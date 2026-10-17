package com.yori3o.everfall.core.profile;

import java.util.List;

public record ParticleProfile(
        String id,
        List<Texture> textures,
        List<String> sweepSounds,
        List<String> captureSounds,
        Defaults defaults,
        boolean flipWithWind
) {
    public ParticleProfile(String id, List<Texture> textures, List<String> sweepSounds,
                           List<String> captureSounds, Defaults defaults) {
        this(id, textures, sweepSounds, captureSounds, defaults, false);
    }

    public record Texture(String id, int frameSize, int frameCount, float frameTime,
                          double weight, float scale, AlternativeAnimation alternativeAnimation) {
        public Texture(String id, int frameSize, int frameCount, float frameTime) {
            this(id, frameSize, frameCount, frameTime, 1.0, 1.0f, null);
        }

        public Texture(String id, int frameSize, int frameCount, float frameTime,
                       double weight, AlternativeAnimation alternativeAnimation) {
            this(id, frameSize, frameCount, frameTime, weight, 1.0f, alternativeAnimation);
        }
    }

    public enum ConditionType { BOTH, ANY }
    public enum AnimationConditionType { CHANCE, COOLDOWN }
    public record AnimationCondition(AnimationConditionType type, double value) {}
    public record AlternativeAnimation(String id, List<AnimationCondition> conditions, ConditionType conditionType) {}

    /** Weights are relative; zero-weight sprites are never spawned. */
    public int chooseTexture(double random) {
        double total = 0;
        for (int i = 0; i < textures.size(); i++) total += textures.get(i).weight();
        double target = random * total;
        int lastPositive = 0;
        for (int i = 0; i < textures.size(); i++) {
            double weight = textures.get(i).weight();
            if (weight <= 0) continue;
            lastPositive = i;
            if (target < weight) return i;
            target -= weight;
        }
        return lastPositive;
    }

    public record Defaults(int maxParticles, float wind, float gravity) {
    }
}
