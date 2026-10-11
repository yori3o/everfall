package com.yori3o.everfall.mc;

import com.yori3o.everfall.core.config.ModConfig;
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
        handler = ConfigClassHandler.createBuilder(ModConfig.class)
                .id(Ids.create("everfall", "client_config"))
                .serializer(config -> GsonConfigSerializerBuilder.create(config)
                        .setPath(configDirectory.resolve("everfall.json5"))
                        .setJson5(true)
                        .build())
                .build();
        handler.load();
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
}
