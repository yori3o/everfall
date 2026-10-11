package com.yori3o.everfall.mc.compat;

import net.minecraft.client.gui.GuiGraphics;

public final class GuiDraw {
    private GuiDraw() {
    }

    public static void fill(Object target, float x, float y, float width, float height, float alpha, int color) {
        int tint = ((int) (Math.max(0.0f, Math.min(alpha, 1.0f)) * 255.0f) << 24) | (color & 0x00ffffff);
        int left = Math.round(x);
        int top = Math.round(y);
        ((GuiGraphics) target).fill(left, top, left + Math.max(1, Math.round(width)),
                top + Math.max(1, Math.round(height)), tint);
    }
}
