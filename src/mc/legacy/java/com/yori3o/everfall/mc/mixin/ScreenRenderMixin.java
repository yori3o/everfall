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
        ClientRuntime.beginFrame((Screen) (Object) this);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void everfall$render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
                                 CallbackInfo callback) {
        ClientRuntime.render((Screen) (Object) this, graphics, mouseX, mouseY);
    }
}
