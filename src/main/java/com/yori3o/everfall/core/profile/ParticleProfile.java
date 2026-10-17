package com.yori3o.everfall.core.profile;

import java.util.List;

public record ParticleProfile(
        String id,
        List<Texture> textures,
        List<String> sweepSounds,
        List<String> captureSounds,
        Defaults defaults
) {
    public record Texture(String id, int frameSize, int frameCount, float frameTime) {
    }

    public record Defaults(int maxParticles, float wind, float gravity) {
    }
}
