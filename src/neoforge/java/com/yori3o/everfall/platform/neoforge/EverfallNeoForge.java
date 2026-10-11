package com.yori3o.everfall.platform.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod("everfall")
public final class EverfallNeoForge {
    public EverfallNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener((FMLClientSetupEvent event) ->
                event.enqueueWork(() -> EverfallNeoForgeClient.initialize(modContainer)));
    }
}
