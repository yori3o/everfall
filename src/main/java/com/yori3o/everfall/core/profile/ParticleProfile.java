package com.yori3o.everfall.core.profile;

import java.util.List;

public record ParticleProfile(
        String id,
        List<Texture> textures,
        List<String> sweepSounds,
        List<String> captureSounds,
        Defaults defaults,
        boolean flipWithWind,
        List<String> shortSweepSounds,
        boolean sway,
        boolean initialFill,
        boolean offsetSpawnWithWind
        ) {
    public ParticleProfile(String id, List<Texture> textures, List<String> sweepSounds,
            List<String> captureSounds, Defaults defaults, boolean flipWithWind,
            List<String> shortSweepSounds, boolean sway, boolean initialFill) {
        this(id, textures, sweepSounds, captureSounds, defaults, flipWithWind, shortSweepSounds,
                sway, initialFill, false);
    }

    public ParticleProfile(String id, List<Texture> textures, List<String> sweepSounds,
            List<String> captureSounds, Defaults defaults, boolean flipWithWind,
            List<String> shortSweepSounds, boolean sway) {
        this(id, textures, sweepSounds, captureSounds, defaults, flipWithWind, shortSweepSounds, sway, true);
    }

    public ParticleProfile(String id, List<Texture> textures, List<String> sweepSounds,
            List<String> captureSounds, Defaults defaults, boolean flipWithWind,
            List<String> shortSweepSounds) {
        this(id, textures, sweepSounds, captureSounds, defaults, flipWithWind, shortSweepSounds, true);
    }

    public ParticleProfile(String id, List<Texture> textures, List<String> sweepSounds,
            List<String> captureSounds, Defaults defaults) {
        this(id, textures, sweepSounds, captureSounds, defaults, false, List.of());
    }

    public ParticleProfile(String id, List<Texture> textures, List<String> sweepSounds,
            List<String> captureSounds, Defaults defaults, boolean flipWithWind) {
        this(id, textures, sweepSounds, captureSounds, defaults, flipWithWind, List.of());
    }

    public record Texture(String id, int frameWidth, int frameHeight, int frameCount, float frameTime,
            double weight, float scale, AlternativeAnimation alternativeAnimation,
            List<FrameBounds> frameBounds, boolean repeat) {
        public Texture(String id, int frameWidth, int frameHeight, int frameCount, float frameTime,
                double weight, float scale, AlternativeAnimation alternativeAnimation,
                List<FrameBounds> frameBounds) {
            this(id, frameWidth, frameHeight, frameCount, frameTime, weight, scale, alternativeAnimation, frameBounds, true);
        }

        public Texture(String id, int frameWidth, int frameHeight, int frameCount, float frameTime,
                double weight, float scale, AlternativeAnimation alternativeAnimation) {
            this(id, frameWidth, frameHeight, frameCount, frameTime, weight, scale, alternativeAnimation, List.of());
        }

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

        private FrameBounds bounds(boolean alternative, int frame) {
            List<FrameBounds> bounds = alternative && alternativeAnimation != null
                    ? alternativeAnimation.frameBounds() : frameBounds;
            return frame >= 0 && frame < bounds.size() ? bounds.get(frame) : null;
        }

        public boolean hasVisiblePixels(boolean alternative, int frame) {
            FrameBounds bounds = bounds(alternative, frame);
            return bounds == null || bounds.bottom() > bounds.top();
        }

        public int contactEdge(boolean alternative, int frame, boolean invertedGravity) {
            FrameBounds bounds = bounds(alternative, frame);
            return bounds == null ? (invertedGravity ? 0 : frameHeight(alternative))
                    : (invertedGravity ? bounds.top() : bounds.bottom());
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

    /** First occupied row and exclusive last occupied row, in source-frame pixels. */
    public record FrameBounds(int top, int bottom) {}

    public enum ConditionType {
        BOTH, ANY
    }

    public enum AnimationConditionType {
        CHANCE, COOLDOWN
    }

    public record AnimationCondition(AnimationConditionType type, double value) {}

    public record AlternativeAnimation(String id, List<AnimationCondition> conditions, ConditionType conditionType,
            int frameWidth, int frameHeight, List<FrameBounds> frameBounds) {
        public AlternativeAnimation(String id, List<AnimationCondition> conditions, ConditionType conditionType,
                int frameWidth, int frameHeight) {
            this(id, conditions, conditionType, frameWidth, frameHeight, List.of());
        }

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
