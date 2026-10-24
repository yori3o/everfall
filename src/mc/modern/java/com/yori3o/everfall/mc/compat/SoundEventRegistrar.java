package com.yori3o.everfall.mc.compat;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class SoundEventRegistrar {

    private SoundEventRegistrar() {
    }

    public static void registerFabric() {
        for (String path : com.yori3o.everfall.mc.ProfileSoundEvents.paths()) {
            Registry.register(BuiltInRegistries.SOUND_EVENT, Ids.parse("everfall:" + path),
                    com.yori3o.everfall.mc.ProfileSoundEvents.create(path));
        }
    }
}
