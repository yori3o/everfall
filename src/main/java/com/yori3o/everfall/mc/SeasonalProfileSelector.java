package com.yori3o.everfall.mc;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

import com.yori3o.everfall.core.config.ModConfig;
import com.yori3o.everfall.core.config.ParticleProfileId;
import com.yori3o.everfall.core.season.SeasonResolver;
import com.yori3o.everfall.mc.compat.ClientLanguage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SeasonalProfileSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger("everfall");
    private static final SeasonResolver RESOLVER = loadResolver();
    private static Inputs previousInputs;
    private static String selected;
    private static ParticleProfileId detectedProfile;

    private record Inputs(LocalDate date, ZoneId zone, Locale system, Locale format, Locale display,
            String language, boolean automatic, String manual) {}

    private SeasonalProfileSelector() {
    }

    public static String select(ModConfig config) {
        String language = ClientLanguage.selected();

        if (previousInputs != null
                && previousInputs.automatic() == config.autoSeason
                && (config.autoSeason || java.util.Objects.equals(previousInputs.manual(), config.profile))
                && previousInputs.language().equals(language)) {
            return applyProfile(config);
        }

        ZoneId zone = ZoneId.systemDefault();
        Inputs inputs = new Inputs(LocalDate.now(zone), zone, Locale.getDefault(),
                Locale.getDefault(Locale.Category.FORMAT), Locale.getDefault(Locale.Category.DISPLAY),
                language, config.autoSeason, config.autoSeason ? null : config.profile);

        if (inputs.equals(previousInputs)) return applyProfile(config);

        previousInputs = inputs;

        if (RESOLVER == null) {
            selected = config.profile;
            detectedProfile = ParticleProfileId.AUTUMN;
            LOGGER.info("[Everfall] Seasonal selection unavailable; using manual profile={}", selected);
            return applyProfile(config);
        }

        SeasonResolver.Result result = RESOLVER.resolve(inputs.date(), zone, inputs.system(), inputs.format(),
                inputs.display(), language);
        detectedProfile = result.profile();
        selected = config.autoSeason ? detectedProfile.name().toLowerCase(Locale.ROOT) : config.profile;
        LOGGER.info("[Everfall] Seasonal profile: auto={}, selected={}, date={}, inferredHemisphere={}, "
                        + "northScore={}, southScore={}, northernFallback={}; {}",
                config.autoSeason, selected, inputs.date(), result.hemisphere(), result.northScore(),
                result.southScore(), result.fallback(), result.evidence());
        return applyProfile(config);
    }

    public static String automaticProfile() {
        select(ModConfigBootstrap.config());
        return detectedProfile.name().toLowerCase(Locale.ROOT);
    }

    private static String applyProfile(ModConfig config) {
        if (config.autoSeason && !java.util.Objects.equals(config.profile, selected)) {
            config.profile = selected;
            var profile = ParticleProfileRegistry.get(selected);

            if (!profile.textures().isEmpty()) {
                var defaults = profile.defaults();
                config.maxParticles = defaults.maxParticles();
                config.wind = defaults.wind();
                config.gravity = defaults.gravity();

                if (defaults.backgroundParticles() != null) config.backgroundParticles = defaults.backgroundParticles();

                if (defaults.interactionEnabled() != null) config.interactionEnabled = defaults.interactionEnabled();

                if (config.backgroundParticles) config.interactionEnabled = false;
            }
        }

        return selected;
    }

    private static SeasonResolver loadResolver() {
        try (Reader zones = openData("zone.tab"); Reader links = openData("backward")) {
            return new SeasonResolver(zones, links);
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("[Everfall] Could not load local region data; automatic selection disabled", exception);
            return null;
        }
    }

    private static Reader openData(String name) throws IOException {
        var stream = SeasonalProfileSelector.class.getResourceAsStream("/assets/everfall/data/" + name);

        if (stream == null) throw new IOException("Missing region data: " + name);
        return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }
}
