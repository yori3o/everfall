package com.yori3o.everfall.mc.compat;

import net.minecraft.client.gui.GuiGraphics;

public final class GuiDraw {
    private GuiDraw() {
    }

    public static void pushDisplayPixelCoordinates(Object target, float coordinateScale) {
        GuiGraphics graphics = (GuiGraphics) target;
        graphics.pose().pushPose();
        float inverseScale = 1.0f / coordinateScale;
        graphics.pose().scale(inverseScale, inverseScale, 1.0f);
    }

    public static void popDisplayPixelCoordinates(Object target) {
        ((GuiGraphics) target).pose().popPose();
    }

    public static void fill(Object target, float x, float y, float width, float height, float alpha, int color) {
        int tint = ((int) (Math.max(0.0f, Math.min(alpha, 1.0f)) * 255.0f) << 24) | (color & 0x00ffffff);
        int left = Math.round(x);
        int top = Math.round(y);
        ((GuiGraphics) target).fill(left, top, left + Math.max(1, Math.round(width)),
                top + Math.max(1, Math.round(height)), tint);
    }

    public static void blit(Object target, String texture, float x, float y, float size, int frameY,
                            int frameSize, int frameCount, float alpha) {
        GuiGraphics graphics = (GuiGraphics) target;
        graphics.setColor(1.0f, 1.0f, 1.0f, Math.max(0.0f, Math.min(alpha, 1.0f)));
        try {
            graphics.blit(Ids.parse(texture), Math.round(x), Math.round(y), 0, 0.0f, frameY,
                    Math.max(1, Math.round(size)), Math.max(1, Math.round(size)), frameSize,
                    frameSize * frameCount);
        } finally {
            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }
}
