package com.yori3o.everfall.platform.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.minecraft.core.registries.Registries;
import com.yori3o.everfall.mc.ProfileSoundEvents;
import com.yori3o.everfall.mc.compat.Ids;

@Mod("everfall")
public final class EverfallNeoForge {
    public EverfallNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(EverfallNeoForge::registerSounds);
        modEventBus.addListener((FMLClientSetupEvent event) ->
                event.enqueueWork(() -> EverfallNeoForgeClient.initialize(modContainer)));
    }

    private static void registerSounds(RegisterEvent event) {
        event.register(Registries.SOUND_EVENT, helper -> {
            for (String path : ProfileSoundEvents.paths()) {
                helper.register(Ids.parse("everfall:" + path), ProfileSoundEvents.create(path));
            }
        });
    }
}
