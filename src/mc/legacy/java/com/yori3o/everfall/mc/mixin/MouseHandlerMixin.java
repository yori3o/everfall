package com.yori3o.everfall.mc.mixin;

import com.yori3o.everfall.mc.ClientRuntime;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onPress", at = @At("HEAD"))
    private void everfall$mouseButton(long window, int button, int action, int modifiers, CallbackInfo callback) {
        ClientRuntime.mouseButtonChanged(button, action);
    }
}
