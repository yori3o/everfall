package com.yori3o.everfall.mc;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.yori3o.everfall.core.config.ParticleProfileId;
import com.yori3o.everfall.core.config.ModConfig;
import com.yori3o.everfall.core.profile.ParticleProfile;
import com.yori3o.everfall.mc.compat.TextureMetadata;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.server.packs.resources.ResourceManager;

public final class ParticleProfileRegistry {

    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LoggerFactory.getLogger("everfall");
    private static final Map<String, ParticleProfile> PROFILES = new LinkedHashMap<>();
    private static final String[] BUILTIN_IDS = {"spring", "summer", "autumn", "winter"};

    private ParticleProfileRegistry() {
    }

    public static void load(Path configDirectory) {
        load(configDirectory, null);
    }

    public static void load(Path configDirectory, ResourceManager resources) {
        PROFILES.clear();

        for (String id : BUILTIN_IDS) {
            try {
                ParticleProfile profile = loadAsset(id, resources);
                validate(profile, id);
                PROFILES.put(id, profile);
            } catch (RuntimeException exception) {
                LOGGER.warn("[Everfall] Skipping built-in particle profile {}", id, exception);
            }
        }

        ProfileFiles.load(configDirectory, resources, false, (id, reader) -> {
            ParticleProfile profile = convert(GSON.fromJson(reader, ProfileData.class), resources);
            validate(profile, id);
            PROFILES.put(id, profile);
        });

        // Seasonal selection remains usable even when every definition of a season is broken.
        for (String id : BUILTIN_IDS) PROFILES.putIfAbsent(id, empty(id));
    }

    public static List<String> ids() {
        return PROFILES.values().stream().filter(p -> !p.textures().isEmpty()).map(ParticleProfile::id).toList();
    }

    public static java.util.Collection<ParticleProfile> profiles() {
        return List.copyOf(PROFILES.values());
    }

    public static ParticleProfile get(ParticleProfileId id) {
        return get(id.name().toLowerCase(java.util.Locale.ROOT));
    }

    public static ParticleProfile get(String id) {
        return PROFILES.getOrDefault(id, EMPTY_PROFILE);
    }

    private static ParticleProfile empty(String id) {
        return new ParticleProfile(id, List.of(), List.of(), List.of(), new ParticleProfile.Defaults(0, 0, 0));
    }

    private static final ParticleProfile EMPTY_PROFILE = empty("unavailable");

    private static ParticleProfile loadAsset(String id, ResourceManager resources) {
        String resource = "/assets/everfall/profiles/" + id + ".json";

        try (InputStream stream = ParticleProfileRegistry.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("Missing built-in particle profile: " + resource);
            }

            return convert(GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), ProfileData.class), resources);
        } catch (IOException | JsonParseException exception) {
            throw new IllegalStateException("Could not load built-in particle profile: " + resource, exception);
        }
    }

    private static ParticleProfile convert(ProfileData data, ResourceManager resources) {
        if (data == null || data.defaults == null) {
            throw new IllegalStateException("Particle profile or its defaults are missing");
        }

        List<ParticleProfile.Texture> textures = new ArrayList<>();

        if (data.textures != null) {
            for (TextureData texture : data.textures) {
                if (texture == null) {
                    throw new IllegalStateException("Particle profile contains a null texture");
                }

                try {
                    TextureMetadata.Frames dimensions = readTexture(texture.id, texture.frameCount, resources);
                    textures.add(new ParticleProfile.Texture(texture.id, dimensions.width(), dimensions.height(),
                            texture.frameCount, texture.frameTime, texture.weight, texture.scale,
                            readAlternative(texture, resources), dimensions.bounds(), texture.repeat));
                } catch (IOException exception) {
                    LOGGER.warn("[Everfall] Skipping particle texture {}: {}", texture.id, exception.getMessage());
                }
            }
        }

        ParticleProfile.Defaults defaults = new ParticleProfile.Defaults(data.defaults.maxParticles,
                data.defaults.wind, data.defaults.gravity,
                data.defaults.backgroundParticles, data.defaults.interactionEnabled);
        return new ParticleProfile(data.id, List.copyOf(textures),
                copySounds(data.sweepSounds), copySounds(data.captureSounds), defaults, data.flipWithWind,
                copySounds(data.shortSweepSounds), data.sway, data.initialFill, data.offsetSpawnWithWind);
    }

    private static void validate(ParticleProfile profile, String expectedId) {
        if (!expectedId.equals(profile.id())) {
            throw new IllegalStateException("Particle profile id must be '" + expectedId + "'");
        }

        if (profile.textures().isEmpty()) {
            throw new IllegalStateException("Particle profile '" + expectedId + "' must define at least one texture");
        }

        double totalWeight = 0;

        for (ParticleProfile.Texture texture : profile.textures()) {
            if (texture.id() == null || !texture.id().matches("[a-z0-9_.-]+:[a-z0-9_./-]+")
                    || texture.frameWidth() <= 0 || texture.frameHeight() <= 0
                    || texture.frameCount() <= 0 || texture.frameCount() > 128
                    || !Float.isFinite(texture.frameTime()) || texture.frameTime() <= 0.0f
                    || !Double.isFinite(texture.weight()) || texture.weight() < 0
                    || !Float.isFinite(texture.scale()) || texture.scale() <= 0
                    || !Float.isFinite(texture.frameWidth() * texture.scale())
                    || !Float.isFinite(texture.frameHeight() * texture.scale())
                    || !Float.isFinite(texture.frameWidth(true) * texture.scale())
                    || !Float.isFinite(texture.frameHeight(true) * texture.scale())) {
                throw new IllegalStateException("Invalid texture definition in particle profile '" + expectedId + "'");
            }

            totalWeight += texture.weight();
        }

        if (!Double.isFinite(totalWeight) || totalWeight <= 0) {
            throw new IllegalStateException("Particle profile must have a positive total texture weight: " + expectedId);
        }

        validateSounds(profile.sweepSounds(), expectedId);
        validateSounds(profile.shortSweepSounds(), expectedId);
        validateSounds(profile.captureSounds(), expectedId);
        ParticleProfile.Defaults defaults = profile.defaults();

        if (defaults.maxParticles() < 0 || defaults.maxParticles() > ModConfig.MAX_PARTICLES
                || !Float.isFinite(defaults.wind()) || !Float.isFinite(defaults.gravity())) {
            throw new IllegalStateException("Invalid recommended settings in particle profile '" + expectedId + "'");
        }
    }

    private static ParticleProfile.AlternativeAnimation readAlternative(TextureData texture, ResourceManager resources) {
        if (texture.alternativeAnimationId == null) return null;

        if (!texture.alternativeAnimationId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalStateException("Invalid alternative animation texture id");
        }

        ParticleProfile.ConditionType combination;

        try {
            combination = ParticleProfile.ConditionType.valueOf(texture.conditionType.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalStateException("conditionType must be 'both' or 'any'", exception);
        }

        List<ParticleProfile.AnimationCondition> conditions = new ArrayList<>();

        if (texture.conditionsForAlternativeAnimation != null) {
            for (AnimationConditionData condition : texture.conditionsForAlternativeAnimation) {
                if (condition == null || condition.type == null || condition.value == null
                        || !Double.isFinite(condition.value) || condition.value < 0) {
                    throw new IllegalStateException("Invalid alternative animation condition");
                }

                ParticleProfile.AnimationConditionType type;

                try {
                    type = ParticleProfile.AnimationConditionType.valueOf(condition.type.toUpperCase(java.util.Locale.ROOT));
                } catch (IllegalArgumentException exception) {
                    throw new IllegalStateException("Animation condition must be 'chance' or 'cooldown'", exception);
                }

                if (type == ParticleProfile.AnimationConditionType.CHANCE && condition.value > 1) {
                    throw new IllegalStateException("Animation chance must be between 0 and 1");
                }

                for (var existing : conditions) {
                    if (existing.type() == type) throw new IllegalStateException("Duplicate animation condition: " + type);
                }

                conditions.add(new ParticleProfile.AnimationCondition(type, condition.value));
            }
        }

        try {
            TextureMetadata.Frames dimensions = readTexture(texture.alternativeAnimationId, texture.frameCount, resources);
            return new ParticleProfile.AlternativeAnimation(texture.alternativeAnimationId,
                    List.copyOf(conditions), combination, dimensions.width(), dimensions.height(), dimensions.bounds());
        } catch (IOException exception) {
            LOGGER.warn("[Everfall] Skipping alternative animation {}: {}", texture.alternativeAnimationId,
                    exception.getMessage());
            return null;
        }
    }

    private static TextureMetadata.Frames readTexture(String id, int frameCount, ResourceManager resources) throws IOException {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || frameCount <= 0 || frameCount > 128) {
            throw new IOException("Invalid texture id or frame count");
        }

        return TextureMetadata.readFrames(id, frameCount, resources);
    }

    private static void validateSounds(List<String> sounds, String profileId) {
        for (String sound : sounds) {
            if (sound == null || !sound.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
                throw new IllegalStateException("Invalid sound id in particle profile '" + profileId + "'");
            }
        }
    }

    private static List<String> copySounds(List<String> sounds) {
        if (sounds == null) {
            return List.of();
        }

        List<String> copy = new ArrayList<>(sounds.size());

        for (String sound : sounds) {
            if (sound == null) {
                throw new IllegalStateException("Particle profile contains a null sound id");
            }

            copy.add(sound);
        }

        return List.copyOf(copy);
    }

    private static final class ProfileData {

        private String id;
        private boolean flipWithWind;
        private boolean sway = true;
        private boolean initialFill = true;
        private boolean offsetSpawnWithWind;
        private List<TextureData> textures;
        private List<String> sweepSounds;
        private List<String> shortSweepSounds;
        private List<String> captureSounds;
        private DefaultsData defaults;
    }

    private static final class TextureData {

        private String id;
        private int frameCount = 1;
        private float frameTime = 1.0f;
        private double weight = 1.0;
        private float scale = 1.0f;
        private boolean repeat = true;
        private String alternativeAnimationId;
        private List<AnimationConditionData> conditionsForAlternativeAnimation;
        private String conditionType = "both";
    }

    private static final class AnimationConditionData {

        private String type;
        private Double value;
    }

    private static final class DefaultsData {

        private int maxParticles;
        private float wind;
        private float gravity;
        private Boolean backgroundParticles;
        private Boolean interactionEnabled;
    }
}
