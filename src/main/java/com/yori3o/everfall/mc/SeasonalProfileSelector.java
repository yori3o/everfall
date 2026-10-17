package com.yori3o.everfall.mc;

import com.yori3o.everfall.core.config.ModConfig;
import com.yori3o.everfall.core.config.ParticleProfileId;
import com.yori3o.everfall.core.season.SeasonResolver;
import com.yori3o.everfall.mc.compat.ClientLanguage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SeasonalProfileSelector {
    private static final Logger LOGGER = LoggerFactory.getLogger("everfall");
    private static final SeasonResolver RESOLVER = loadResolver();
    private static Inputs previousInputs;
    private static ParticleProfileId selected;
    private static ParticleProfileId detectedProfile;
    private static long nextCheckNanos;

    private record Inputs(LocalDate date, ZoneId zone, Locale system, Locale format, Locale display,
                          String language, boolean automatic, ParticleProfileId manual) {}

    private SeasonalProfileSelector() {}

    public static ParticleProfileId select(ModConfig config) {
        long now = System.nanoTime();
        String language = ClientLanguage.selected();
        if (previousInputs != null && now < nextCheckNanos
                && previousInputs.automatic() == config.autoSeason
                && (config.autoSeason || previousInputs.manual() == config.profile)
                && previousInputs.language().equals(language)) {
            return applySelection(config);
        }
        nextCheckNanos = now + 1_000_000_000L;
        ZoneId zone = ZoneId.systemDefault();
        Inputs inputs = new Inputs(LocalDate.now(zone), zone, Locale.getDefault(),
                Locale.getDefault(Locale.Category.FORMAT), Locale.getDefault(Locale.Category.DISPLAY),
                language, config.autoSeason, config.autoSeason ? null : config.profile);
        if (inputs.equals(previousInputs)) return applySelection(config);
        previousInputs = inputs;
        if (RESOLVER == null) {
            selected = config.profile;
            detectedProfile = config.profile;
            LOGGER.info("[Everfall] Seasonal selection unavailable; using manual profile={}", selected);
            return applySelection(config);
        }
        SeasonResolver.Result result = RESOLVER.resolve(inputs.date(), zone, inputs.system(), inputs.format(),
                inputs.display(), language);
        detectedProfile = result.profile();
        selected = config.autoSeason ? detectedProfile : config.profile;
        LOGGER.info("[Everfall] Seasonal profile: auto={}, selected={}, date={}, inferredHemisphere={}, "
                        + "northScore={}, southScore={}, northernFallback={}; {}",
                config.autoSeason, selected, inputs.date(), result.hemisphere(), result.northScore(),
                result.southScore(), result.fallback(), result.evidence());
        return applySelection(config);
    }

    public static ParticleProfileId automaticProfile() {
        select(ModConfigBootstrap.config());
        return detectedProfile;
    }

    private static ParticleProfileId applySelection(ModConfig config) {
        if (config.autoSeason) config.profile = selected;
        return selected;
    }

    private static SeasonResolver loadResolver() {
        try (Reader zones = readData("zone.tab"); Reader links = readData("backward")) {
            return new SeasonResolver(zones, links);
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("[Everfall] Could not load local region data; automatic selection disabled", exception);
            return null;
        }
    }

    private static Reader readData(String name) throws IOException {
        var stream = SeasonalProfileSelector.class.getResourceAsStream("/assets/everfall/data/" + name);
        if (stream == null) throw new IOException("Missing region data: " + name);
        return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }
}
