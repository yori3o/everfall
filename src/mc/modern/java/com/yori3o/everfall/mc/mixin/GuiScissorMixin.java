package com.yori3o.everfall.mc.mixin;

import java.util.ArrayDeque;
import java.util.Deque;

import com.yori3o.everfall.mc.compat.ScissorAccess;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiScissorMixin implements ScissorAccess {

    @Unique
    private final Deque<ScreenRectangle> everfall$scissors = new ArrayDeque<>();

    @Inject(method = "enableScissor", at = @At("TAIL"))
    private void everfall$pushScissor(int left, int top, int right, int bottom, CallbackInfo callback) {
        ScreenRectangle rectangle = new ScreenRectangle(left, top, right - left, bottom - top)
                .transformAxisAligned(((GuiGraphicsExtractor) (Object) this).pose());
        ScreenRectangle previous = everfall$scissors.peekLast();

        if (previous != null) {
            ScreenRectangle intersection = rectangle.intersection(previous);
            rectangle = intersection == null ? ScreenRectangle.empty() : intersection;
        }

        everfall$scissors.addLast(rectangle);
    }

    @Inject(method = "disableScissor", at = @At("TAIL"))
    private void everfall$popScissor(CallbackInfo callback) {
        everfall$scissors.removeLast();
    }

    @Override
    public ScreenRectangle everfall$scissor() {
        return everfall$scissors.peekLast();
    }
}
