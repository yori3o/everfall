package com.yori3o.everfall.mc.compat;

import com.yori3o.everfall.core.api.SpriteBuffer;

public final class SpriteRenderer {
    public void draw(Object graphics, SpriteBuffer sprites) {
        for (int i = 0; i < sprites.count; i++) {
            GuiDraw.fill(graphics, sprites.x[i], sprites.y[i], sprites.width[i], sprites.height[i],
                    sprites.alpha[i], sprites.color[i]);
        }
    }
}
