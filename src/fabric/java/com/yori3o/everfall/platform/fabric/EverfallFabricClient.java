package com.yori3o.everfall.platform.fabric;

import com.yori3o.everfall.mc.ClientRuntime;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class EverfallFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientRuntime.initialize(FabricLoader.getInstance().getConfigDir());
    }
}
