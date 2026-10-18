package com.yori3o.everfall.mc;

import com.yori3o.everfall.core.config.ModConfig;
import com.yori3o.everfall.core.config.PixelMode;
import com.yori3o.everfall.core.profile.ParticleProfile;
import com.yori3o.everfall.mc.compat.Ids;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import java.nio.file.Path;

public final class ModConfigBootstrap {
    private static ConfigClassHandler<ModConfig> handler;

    private ModConfigBootstrap() {
    }

    public static void initialize(Path configDirectory) {
        if (handler != null) {
            return;
        }
        ParticleProfileRegistry.load(configDirectory);
        AdditionalProfileRegistry.load(configDirectory);
        handler = ConfigClassHandler.createBuilder(ModConfig.class)
                .id(Ids.create("everfall", "client_config"))
                .serializer(config -> GsonConfigSerializerBuilder.create(config)
                        .setPath(configDirectory.resolve("everfall.json5"))
                        .setJson5(true)
                        .build())
                .build();
        handler.load();
        if (handler.instance().backgroundParticles) handler.instance().interactionEnabled = false;
        if (handler.instance().additionalProfiles == null) {
            handler.instance().additionalProfiles = new java.util.ArrayList<>(java.util.List.of("fireflies"));
        }
        if (handler.instance().profile == null) {
            handler.instance().profile = "autumn";
        }
        handler.instance().profile = handler.instance().profile.toLowerCase(java.util.Locale.ROOT);
        if (handler.instance().pixelMode == null) {
            handler.instance().pixelMode = PixelMode.DISPLAY;
        }
        SeasonalProfileSelector.select(handler.instance());
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
        ModConfig config = handler.instance();
        String selected = SeasonalProfileSelector.select(config);
        return ParticleProfileRegistry.get(selected);
    }

    public static void save() {
        if (handler == null) {
            throw new IllegalStateException("Everfall config has not been initialized");
        }
        ModConfig config = handler.instance();
        SeasonalProfileSelector.select(config);
        if (config.backgroundParticles) config.interactionEnabled = false;
        handler.save();
    }
}
