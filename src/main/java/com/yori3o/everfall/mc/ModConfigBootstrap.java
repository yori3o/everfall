package com.yori3o.everfall.mc;

import com.yori3o.everfall.core.config.ModConfig;
import com.yori3o.everfall.core.config.ParticleProfileId;
import com.yori3o.everfall.core.profile.ParticleProfile;
import com.yori3o.everfall.mc.compat.Ids;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import java.nio.file.Path;

public final class ModConfigBootstrap {
    private static ConfigClassHandler<ModConfig> handler;
    private static ParticleProfileId savedProfile;

    private ModConfigBootstrap() {
    }

    public static void initialize(Path configDirectory) {
        if (handler != null) {
            return;
        }
        ParticleProfileRegistry.load(configDirectory);
        handler = ConfigClassHandler.createBuilder(ModConfig.class)
                .id(Ids.create("everfall", "client_config"))
                .serializer(config -> GsonConfigSerializerBuilder.create(config)
                        .setPath(configDirectory.resolve("everfall.json5"))
                        .setJson5(true)
                        .build())
                .build();
        handler.load();
        if (handler.instance().profile == null) {
            handler.instance().profile = ParticleProfileId.AUTUMN;
        }
        savedProfile = handler.instance().profile;
    }

    public static ModConfig config() {
        if (handler == null) {
            throw new IllegalStateException("Everfall config has not been initialized");
        }
        return handler.instance();
    }

    public static ConfigClassHandler<ModConfig> handler() {
        if (handler == null) {
            throw new IllegalStateException("Everfall config has not been initialized");
        }
        return handler;
    }

    public static ParticleProfile profile() {
        if (handler == null) {
            throw new IllegalStateException("Everfall config has not been initialized");
        }
        return ParticleProfileRegistry.get(handler.instance().profile);
    }

    public static void save() {
        if (handler == null) {
            throw new IllegalStateException("Everfall config has not been initialized");
        }
        handler.save();
        ModConfig config = handler.instance();
        if (config.profile != savedProfile) {
            ParticleProfile.Defaults defaults = ParticleProfileRegistry.get(config.profile).defaults();
            config.density = defaults.density();
            config.maxParticles = defaults.maxParticles();
            config.wind = defaults.wind();
            config.gravity = defaults.gravity();
            savedProfile = config.profile;
            handler.save();
        }
    }
}
