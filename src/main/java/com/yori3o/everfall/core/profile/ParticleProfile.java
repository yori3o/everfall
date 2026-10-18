package com.yori3o.everfall.core.profile;

import java.util.List;

public record ParticleProfile(
        String id,
        List<Texture> textures,
        List<String> sweepSounds,
        List<String> captureSounds,
        Defaults defaults,
        boolean flipWithWind,
        List<String> shortSweepSounds
) {
    public ParticleProfile(String id, List<Texture> textures, List<String> sweepSounds,
                           List<String> captureSounds, Defaults defaults) {
        this(id, textures, sweepSounds, captureSounds, defaults, false, List.of());
    }

    public ParticleProfile(String id, List<Texture> textures, List<String> sweepSounds,
                           List<String> captureSounds, Defaults defaults, boolean flipWithWind) {
        this(id, textures, sweepSounds, captureSounds, defaults, flipWithWind, List.of());
    }

    public record Texture(String id, int frameWidth, int frameHeight, int frameCount, float frameTime,
                          double weight, float scale, AlternativeAnimation alternativeAnimation) {
        public Texture(String id, int side, int frameCount, float frameTime) {
            this(id, side, side, frameCount, frameTime, 1.0, 1.0f, null);
        }

        public Texture(String id, int side, int frameCount, float frameTime,
                       double weight, AlternativeAnimation alternativeAnimation) {
            this(id, side, side, frameCount, frameTime, weight, 1.0f, alternativeAnimation);
        }

        public Texture(String id, int side, int frameCount, float frameTime,
                       double weight, float scale, AlternativeAnimation alternativeAnimation) {
            this(id, side, side, frameCount, frameTime, weight, scale, alternativeAnimation);
        }

        public int frameWidth(boolean alternative) {
            return alternative && alternativeAnimation != null && alternativeAnimation.frameWidth() > 0
                    ? alternativeAnimation.frameWidth() : frameWidth;
        }

        public int frameHeight(boolean alternative) {
            return alternative && alternativeAnimation != null && alternativeAnimation.frameHeight() > 0
                    ? alternativeAnimation.frameHeight() : frameHeight;
        }
    }

    public enum ConditionType { BOTH, ANY }
    public enum AnimationConditionType { CHANCE, COOLDOWN }
    public record AnimationCondition(AnimationConditionType type, double value) {}
    public record AlternativeAnimation(String id, List<AnimationCondition> conditions, ConditionType conditionType,
                                       int frameWidth, int frameHeight) {
        public AlternativeAnimation(String id, List<AnimationCondition> conditions, ConditionType conditionType) {
            this(id, conditions, conditionType, 0, 0);
        }
    }

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

    public record Defaults(int maxParticles, float wind, float gravity,
                           Boolean backgroundParticles, Boolean interactionEnabled) {
        public Defaults(int maxParticles, float wind, float gravity) {
            this(maxParticles, wind, gravity, null, null);
        }
    }
}
