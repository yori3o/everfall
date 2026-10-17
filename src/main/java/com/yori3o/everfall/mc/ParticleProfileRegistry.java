package com.yori3o.everfall.mc;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.yori3o.everfall.core.config.ParticleProfileId;
import com.yori3o.everfall.core.profile.ParticleProfile;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ParticleProfileRegistry {
    private static final Gson GSON = new Gson();
    private static final Map<ParticleProfileId, ParticleProfile> PROFILES = new EnumMap<>(ParticleProfileId.class);
    private static final String[] BUILTIN_IDS = {"spring", "summer", "autumn", "winter"};

    private ParticleProfileRegistry() {
    }

    public static void load(Path configDirectory) {
        PROFILES.clear();
        Path overrides = configDirectory.resolve("everfall").resolve("profiles");
        try {
            Files.createDirectories(overrides);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create particle profile override directory: " + overrides,
                    exception);
        }
        for (String id : BUILTIN_IDS) {
            Path override = overrides.resolve(id + ".json");
            ParticleProfile profile = Files.isRegularFile(override)
                    ? loadFile(override)
                    : loadAsset(id);
            validate(profile, id);
            PROFILES.put(ParticleProfileId.valueOf(id.toUpperCase(java.util.Locale.ROOT)), profile);
        }
    }

    public static ParticleProfile get(ParticleProfileId id) {
        ParticleProfile profile = PROFILES.get(id);
        if (profile == null) {
            throw new IllegalStateException("Particle profile has not been loaded: " + id);
        }
        return profile;
    }

    private static ParticleProfile loadAsset(String id) {
        String resource = "/assets/everfall/profiles/" + id + ".json";
        try (InputStream stream = ParticleProfileRegistry.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("Missing built-in particle profile: " + resource);
            }
            return convert(GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), ProfileData.class));
        } catch (IOException | JsonParseException exception) {
            throw new IllegalStateException("Could not load built-in particle profile: " + resource, exception);
        }
    }

    private static ParticleProfile loadFile(Path path) {
        try (InputStream stream = Files.newInputStream(path)) {
            return convert(GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), ProfileData.class));
        } catch (IOException | JsonParseException exception) {
            throw new IllegalStateException("Could not load particle profile override: " + path, exception);
        }
    }

    private static ParticleProfile convert(ProfileData data) {
        if (data == null || data.defaults == null) {
            throw new IllegalStateException("Particle profile or its defaults are missing");
        }
        List<ParticleProfile.Texture> textures = new ArrayList<>();
        if (data.textures != null) {
            for (TextureData texture : data.textures) {
                if (texture == null) {
                    throw new IllegalStateException("Particle profile contains a null texture");
                }
                textures.add(new ParticleProfile.Texture(texture.id, texture.frameSize, texture.frameCount,
                        texture.frameTime, texture.weight, texture.scale, alternative(texture)));
            }
        }
        ParticleProfile.Defaults defaults = new ParticleProfile.Defaults(data.defaults.maxParticles,
                data.defaults.wind, data.defaults.gravity);
        return new ParticleProfile(data.id, List.copyOf(textures),
                copySounds(data.sweepSounds), copySounds(data.captureSounds), defaults, data.flipWithWind);
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
                    || texture.frameSize() <= 0 || texture.frameSize() > 256
                    || texture.frameCount() <= 0 || texture.frameCount() > 128
                    || texture.frameSize() * (long) texture.frameCount() > 32768
                    || !Float.isFinite(texture.frameTime()) || texture.frameTime() <= 0.0f
                    || !Double.isFinite(texture.weight()) || texture.weight() < 0
                    || !Float.isFinite(texture.scale()) || texture.scale() <= 0
                    || !Float.isFinite(texture.frameSize() * texture.scale())) {
                throw new IllegalStateException("Invalid texture definition in particle profile '" + expectedId + "'");
            }
            totalWeight += texture.weight();
        }
        if (!Double.isFinite(totalWeight) || totalWeight <= 0) {
            throw new IllegalStateException("Particle profile must have a positive total texture weight: " + expectedId);
        }
        validateSounds(profile.sweepSounds(), expectedId);
        validateSounds(profile.captureSounds(), expectedId);
        ParticleProfile.Defaults defaults = profile.defaults();
        if (defaults.maxParticles() < 0
                || !Float.isFinite(defaults.wind()) || !Float.isFinite(defaults.gravity())
                || defaults.gravity() < 0.0f) {
            throw new IllegalStateException("Invalid recommended settings in particle profile '" + expectedId + "'");
        }
    }

    private static ParticleProfile.AlternativeAnimation alternative(TextureData texture) {
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
        return new ParticleProfile.AlternativeAnimation(texture.alternativeAnimationId,
                List.copyOf(conditions), combination);
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
        private List<TextureData> textures;
        private List<String> sweepSounds;
        private List<String> captureSounds;
        private DefaultsData defaults;
    }

    private static final class TextureData {
        private String id;
        private int frameSize;
        private int frameCount = 1;
        private float frameTime = 1.0f;
        private double weight = 1.0;
        private float scale = 1.0f;
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
    }
}
