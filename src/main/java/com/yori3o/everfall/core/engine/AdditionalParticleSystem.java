package com.yori3o.everfall.core.engine;

import java.time.LocalDateTime;
import java.util.List;

import com.yori3o.everfall.core.api.FrameInput;
import com.yori3o.everfall.core.api.SpriteBuffer;
import com.yori3o.everfall.core.config.ModConfig;
import com.yori3o.everfall.core.profile.AdditionalProfile;

public final class AdditionalParticleSystem {

    public static final class Layer {

        private final AdditionalProfile profile;
        private final AdditionalEffectEngine engine;
        private boolean active;
        private SpriteBuffer sprites;

        private Layer(AdditionalProfile profile) {
            this.profile = profile;
            this.engine = switch (profile.motion()) {
                case FIREFLIES -> new FireflyParticleEngine();
            };
        }

        public AdditionalProfile profile() {
            return profile;
        }

        public boolean active() {
            return active;
        }

        public SpriteBuffer sprites() {
            return sprites;
        }
    }

    private final List<Layer> layers;

    public AdditionalParticleSystem(List<AdditionalProfile> profiles) {
        layers = profiles.stream().map(Layer::new).toList();
    }

    public List<Layer> layers() {
        return layers;
    }

    public int particleCount() {
        int total = 0;

        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);

            if (layer.active) total += layer.engine.particleCount();
        }

        return total;
    }

    public void reset(long seed) {
        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            layer.engine.reset(seed ^ layer.profile.id().hashCode());
            layer.active = false;
        }
    }

    /** Reserve each active layer's share of the one global particle budget. */
    public void step(FrameInput input, LocalDateTime now) {
        int total = ModConfig.clampParticleCount(input.config.maxParticles);
        boolean enabled = input.config.enabled && (!input.inWorld || input.config.showInWorld)
                && input.screenWidth > 0 && input.screenHeight > 0 && total > 0;
        double weight = Math.max(1, input.profile.defaults().maxParticles());

        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            boolean active = enabled && input.config.additionalProfiles != null
                    && input.config.additionalProfiles.contains(layer.profile.id())
                    && layer.profile.conditions().matches(input.profile.id(), now);

            if (layer.active && !active) layer.engine.reset(layer.profile.id().hashCode());
            layer.active = active;

            if (active) weight += layer.profile.maxParticles();
        }

        int remaining = total;

        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);

            if (!layer.active) continue;

            int limit = Math.min(remaining, (int) (total * (double) layer.profile.maxParticles() / weight));
            remaining -= limit;
            layer.sprites = layer.engine.step(input, layer.profile, limit);
        }

        input.particleLimit = remaining;
    }
}
