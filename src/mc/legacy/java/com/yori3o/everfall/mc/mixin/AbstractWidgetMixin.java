package com.yori3o.everfall.mc.mixin;

import com.yori3o.everfall.mc.ClientRuntime;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void everfall$captureWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
                                        CallbackInfo callback) {
        ClientRuntime.captureWidget((AbstractWidget) (Object) this);
    }
}
