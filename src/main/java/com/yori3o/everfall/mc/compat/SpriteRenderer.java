package com.yori3o.everfall.mc.compat;

import com.yori3o.everfall.core.api.SpriteBuffer;
import com.yori3o.everfall.core.profile.ParticleProfile;

public final class SpriteRenderer {
    public void draw(Object graphics, SpriteBuffer sprites, ParticleProfile profile, float coordinateScale) {
        GuiDraw.pushDisplayPixelCoordinates(graphics, coordinateScale);
        try {
            for (int i = 0; i < sprites.count; i++) {
                if (sprites.textureIndex[i] < 0) {
                    GuiDraw.fill(graphics, sprites.x[i], sprites.y[i], sprites.width[i], sprites.height[i],
                            sprites.alpha[i], sprites.color[i]);
                    continue;
                }
                ParticleProfile.Texture texture = profile.textures().get(sprites.textureIndex[i]);
                String textureId = sprites.alternativeAnimation[i] && texture.alternativeAnimation() != null
                        ? texture.alternativeAnimation().id() : texture.id();
                GuiDraw.blit(graphics, textureId, sprites.x[i], sprites.y[i], sprites.width[i],
                        sprites.frameIndex[i] * texture.frameSize(), texture.frameSize(), texture.frameCount(),
                        sprites.alpha[i], sprites.flipX);
            }
        } finally {
            GuiDraw.popDisplayPixelCoordinates(graphics);
        }
    }
}
