package com.yori3o.everfall.mc;

import dev.isxander.yacl3.api.YetAnotherConfigLib;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ConfigScreenFactory {
    private ConfigScreenFactory() {
    }

    public static Screen create(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("everfall.config.title"))
                .categories(ModConfigBootstrap.handler().generateGui().categories())
                .save(ModConfigBootstrap::save)
                .build()
                .generateScreen(parent);
    }
}
