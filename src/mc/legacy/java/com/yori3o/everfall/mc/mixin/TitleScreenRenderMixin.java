package com.yori3o.everfall.mc.mixin;

import com.yori3o.everfall.mc.ClientRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenRenderMixin {
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/TitleScreen;renderPanorama(Lnet/minecraft/client/gui/GuiGraphics;F)V",
            shift = At.Shift.AFTER))
    private void everfall$background(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
                                      CallbackInfo callback) {
        ClientRuntime.renderBackground((TitleScreen) (Object) this, graphics);
    }
}
