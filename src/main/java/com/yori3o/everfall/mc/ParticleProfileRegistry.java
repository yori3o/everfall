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
    private static final String[] BUILTIN_IDS = {"autumn", "winter"};

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
            PROFILES.put(ParticleProfileId.valueOf(id.toUpperCase()), profile);
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
                        texture.frameTime));
            }
        }
        ParticleProfile.Defaults defaults = new ParticleProfile.Defaults(data.defaults.density,
                data.defaults.maxParticles, data.defaults.wind, data.defaults.gravity);
        return new ParticleProfile(data.id, List.copyOf(textures),
                copySounds(data.sweepSounds), copySounds(data.captureSounds), defaults);
    }

    private static void validate(ParticleProfile profile, String expectedId) {
        if (!expectedId.equals(profile.id())) {
            throw new IllegalStateException("Particle profile id must be '" + expectedId + "'");
        }
        if (profile.textures().isEmpty()) {
            throw new IllegalStateException("Particle profile '" + expectedId + "' must define at least one texture");
        }
        for (ParticleProfile.Texture texture : profile.textures()) {
            if (texture.id() == null || !texture.id().matches("[a-z0-9_.-]+:[a-z0-9_./-]+")
                    || texture.frameSize() <= 0 || texture.frameSize() > 256
                    || texture.frameCount() <= 0 || texture.frameCount() > 128
                    || texture.frameSize() * (long) texture.frameCount() > 32768
                    || !Float.isFinite(texture.frameTime()) || texture.frameTime() <= 0.0f) {
                throw new IllegalStateException("Invalid texture definition in particle profile '" + expectedId + "'");
            }
        }
        validateSounds(profile.sweepSounds(), expectedId);
        validateSounds(profile.captureSounds(), expectedId);
        ParticleProfile.Defaults defaults = profile.defaults();
        if (!Float.isFinite(defaults.density()) || defaults.density() < 0.0f
                || defaults.maxParticles() < 0 || defaults.maxParticles() > 400
                || !Float.isFinite(defaults.wind()) || !Float.isFinite(defaults.gravity())
                || defaults.gravity() < 0.0f) {
            throw new IllegalStateException("Invalid recommended settings in particle profile '" + expectedId + "'");
        }
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
    }

    private static final class DefaultsData {
        private float density;
        private int maxParticles;
        private float wind;
        private float gravity;
    }
}
