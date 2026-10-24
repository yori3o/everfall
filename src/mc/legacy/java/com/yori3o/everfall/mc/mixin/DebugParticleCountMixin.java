package com.yori3o.everfall.mc.mixin;

import java.util.ArrayList;
import java.util.List;

import com.yori3o.everfall.mc.compat.DebugParticleCount;

import net.minecraft.client.gui.components.DebugScreenOverlay;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugParticleCountMixin {

    @Inject(method = "getGameInformation", at = @At("RETURN"), cancellable = true)
    private void everfall$counter(CallbackInfoReturnable<List<String>> callback) {
        String line = DebugParticleCount.line();

        if (line == null) return;

        List<String> lines = new ArrayList<>(callback.getReturnValue());
        lines.add(line);
        callback.setReturnValue(lines);
    }
}
