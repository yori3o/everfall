package com.yori3o.everfall.mc.compat;

import com.yori3o.everfall.core.api.SpriteBuffer;
import com.yori3o.everfall.core.profile.ParticleProfile;

public final class SpriteRenderer {
    public void draw(Object graphics, SpriteBuffer sprites, ParticleProfile profile, float coordinateScale) {
        GuiDraw.pushDisplayPixelCoordinates(graphics, coordinateScale);
        try {
            for (int i = 0; i < sprites.count; i++) {
                ParticleProfile.Texture texture = profile.textures().get(sprites.textureIndex[i]);
                GuiDraw.blit(graphics, texture.id(), sprites.x[i], sprites.y[i], sprites.width[i],
                        sprites.frameIndex[i] * texture.frameSize(), texture.frameSize(), texture.frameCount(),
                        sprites.alpha[i]);
            }
        } finally {
            GuiDraw.popDisplayPixelCoordinates(graphics);
        }
    }
}
