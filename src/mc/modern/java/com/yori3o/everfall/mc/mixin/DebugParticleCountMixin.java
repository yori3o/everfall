package com.yori3o.everfall.mc.mixin;

import java.util.Map;

import com.yori3o.everfall.mc.compat.DebugParticleCount;
import com.yori3o.everfall.mc.compat.Ids;

import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugScreenEntries.class)
public abstract class DebugParticleCountMixin {

    @Shadow
    @Final
    private static Map<Identifier, DebugScreenEntry> ENTRIES_BY_ID;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void everfall$register(CallbackInfo callback) {
        // Missing entries default to NEVER; leave vanilla profiles and saved user statuses untouched.
        ENTRIES_BY_ID.put(Ids.parse("everfall:particle_count"), new DebugParticleCount());
    }
}
