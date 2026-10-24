package com.yori3o.everfall.platform.neoforge;

import com.yori3o.everfall.mc.ClientRuntime;
import com.yori3o.everfall.mc.ConfigScreenFactory;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class EverfallNeoForgeClient {

    private EverfallNeoForgeClient() {
    }

    public static void initialize(ModContainer modContainer) {
        ClientRuntime.initialize(FMLPaths.CONFIGDIR.get());
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> ConfigScreenFactory.create(parent));
    }
}
