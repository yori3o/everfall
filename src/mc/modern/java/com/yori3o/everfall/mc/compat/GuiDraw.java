package com.yori3o.everfall.mc.compat;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

public final class GuiDraw {
    private GuiDraw() {
    }

    public static void pushDisplayPixelCoordinates(Object target, float coordinateScale) {
        GuiGraphicsExtractor graphics = (GuiGraphicsExtractor) target;
        graphics.pose().pushMatrix();
        float inverseScale = 1.0f / coordinateScale;
        graphics.pose().scale(inverseScale, inverseScale);
    }

    public static void popDisplayPixelCoordinates(Object target) {
        ((GuiGraphicsExtractor) target).pose().popMatrix();
    }

    public static void fill(Object target, float x, float y, float width, float height, float alpha, int color) {
        int tint = ((int) (Math.max(0.0f, Math.min(alpha, 1.0f)) * 255.0f) << 24) | (color & 0x00ffffff);
        int left = Math.round(x);
        int top = Math.round(y);
        ((GuiGraphicsExtractor) target).fill(left, top, left + Math.max(1, Math.round(width)),
                top + Math.max(1, Math.round(height)), tint);
    }

    public static void blit(Object target, String texture, float x, float y, float size, int frameY,
                            int frameSize, int frameCount, float alpha) {
        blit(target, texture, x, y, size, frameY, frameSize, frameCount, alpha, false);
    }

    public static void blit(Object target, String texture, float x, float y, float size, int frameY,
                            int frameSize, int frameCount, float alpha, boolean flipX) {
        float sourceX = flipX ? frameSize : 0.0f;
        int sourceWidth = flipX ? -frameSize : frameSize;
        int tint = ((int) (Math.max(0.0f, Math.min(alpha, 1.0f)) * 255.0f) << 24) | 0x00ffffff;
        int drawSize = Math.max(1, Math.round(size));
        ((GuiGraphicsExtractor) target).blit(RenderPipelines.GUI_TEXTURED, Ids.parse(texture),
                Math.round(x), Math.round(y), sourceX, frameY, drawSize, drawSize, sourceWidth, frameSize,
                frameSize, frameSize * frameCount, tint);
    }
}
