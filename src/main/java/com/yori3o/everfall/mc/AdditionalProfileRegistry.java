package com.yori3o.everfall.mc;

import com.google.gson.Gson;
import com.yori3o.everfall.core.profile.AdditionalProfile;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.MonthDay;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AdditionalProfileRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("everfall");
    private static final Gson GSON = new Gson();
    private static List<AdditionalProfile> profiles = List.of();

    private AdditionalProfileRegistry() {}

    public static List<AdditionalProfile> profiles() { return profiles; }

    public static void load(Path configDirectory) {
        load(configDirectory, null);
    }

    public static void load(Path configDirectory, ResourceManager resources) {
        Map<String, AdditionalProfile> loaded = new LinkedHashMap<>();
        try (var stream = AdditionalProfileRegistry.class.getResourceAsStream(
                "/assets/everfall/profiles/additional/fireflies.json")) {
            if (stream == null) throw new IOException("Missing built-in fireflies profile");
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                AdditionalProfile profile = parse(reader, "fireflies");
                loaded.put(profile.id(), profile);
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("[Everfall] Could not load built-in fireflies; skipping this feature", exception);
        }
        ProfileFiles.load(configDirectory, resources, true, (id, reader) -> {
            AdditionalProfile profile = parse(reader, id);
            loaded.put(profile.id(), profile);
        });
        profiles = List.copyOf(loaded.values());
    }

    private static AdditionalProfile parse(Reader reader, String expectedId) {
        Data data = GSON.fromJson(reader, Data.class);
        if (data == null || data.schema != 1 || !expectedId.equals(data.id)
                || !data.id.matches("[a-z0-9_.-]+") || data.maxParticles < 0
                || !Float.isFinite(data.speed) || data.speed < 0
                || !Float.isFinite(data.size) || data.size <= 0 || data.size > 64) {
            throw new IllegalArgumentException("Invalid additional particle profile: " + expectedId);
        }
        Conditions conditions = data.conditions == null ? new Conditions() : data.conditions;
        if (conditions.startHour < 0 || conditions.startHour > 23
                || conditions.endHour < 0 || conditions.endHour > 23
                || (conditions.startDate == null) != (conditions.endDate == null)) {
            throw new IllegalArgumentException("Invalid activation conditions: " + expectedId);
        }
        List<String> seasons = conditions.profiles == null ? List.of() : List.copyOf(conditions.profiles);
        for (String season : seasons) {
            if (!ProfileFiles.validId(season)) {
                throw new IllegalArgumentException("Unknown main profile: " + season);
            }
        }
        AdditionalProfile.Conditions match = new AdditionalProfile.Conditions(seasons,
                conditions.startHour, conditions.endHour, day(conditions.startDate), day(conditions.endDate));
        return new AdditionalProfile(data.id, data.name == null ? data.id : data.name,
                AdditionalProfile.Motion.valueOf(data.motion.toUpperCase(Locale.ROOT)), data.maxParticles,
                data.speed, data.size, data.color, match);
    }

    private static int day(String date) {
        if (date == null) return 0;
        MonthDay day = MonthDay.parse("--" + date);
        return day.getMonthValue() * 100 + day.getDayOfMonth();
    }

    private static final class Data {
        int schema = 1;
        String id;
        String name;
        String motion = "fireflies";
        int maxParticles = 24;
        float speed = 10;
        float size = 1;
        int color = 0xe9ff8a;
        Conditions conditions;
    }

    private static final class Conditions {
        List<String> profiles;
        int startHour;
        int endHour;
        String startDate;
        String endDate;
    }
}
