package com.yori3o.everfall.mc.mixin;

import com.yori3o.everfall.mc.ClientRuntime;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenRenderMixin {

    @Inject(method = "renderWithTooltip", at = @At("HEAD"))
    private void everfall$beginFrame(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
            CallbackInfo callback) {
        ClientRuntime.beginFrame((Screen) (Object) this, mouseX, mouseY);
    }

    @Inject(method = "renderWithTooltip", at = @At("TAIL"))
    private void everfall$render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
            CallbackInfo callback) {
        ClientRuntime.render((Screen) (Object) this, graphics, mouseX, mouseY);
    }

    @Inject(method = "renderBackground", at = @At("TAIL"))
    private void everfall$background(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
            CallbackInfo callback) {
        ClientRuntime.renderBackground((Screen) (Object) this, graphics);
    }

    @Inject(method = "renderTransparentBackground", at = @At("TAIL"))
    private void everfall$transparentBackground(GuiGraphics graphics, CallbackInfo callback) {
        // Container panels are drawn after this full-screen dimming layer.
        ClientRuntime.renderBackground((Screen) (Object) this, graphics);
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            shift = At.Shift.AFTER))
    private void everfall$overriddenBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
            CallbackInfo callback) {
        // Covers overrides such as the title screen's empty renderBackground.
        ClientRuntime.renderBackground((Screen) (Object) this, graphics);
    }
}
