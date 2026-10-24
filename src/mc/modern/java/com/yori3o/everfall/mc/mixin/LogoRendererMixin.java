package com.yori3o.everfall.mc.mixin;

import com.yori3o.everfall.mc.ClientRuntime;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LogoRenderer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LogoRenderer.class)
public abstract class LogoRendererMixin {

    @Shadow
    @Final
    private boolean keepLogoThroughFade;

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IFI)V", at = @At("TAIL"))
    private void everfall$captureLogo(GuiGraphicsExtractor graphics, int width, float alpha, int top,
            CallbackInfo callback) {
        ClientRuntime.captureLogo(width, top, keepLogoThroughFade ? 1.0f : alpha, graphics);
    }
}
