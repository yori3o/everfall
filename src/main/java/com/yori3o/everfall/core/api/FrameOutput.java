package com.yori3o.everfall.core.api;

public final class FrameOutput {
    public final SpriteBuffer sprites = new SpriteBuffer(256);
    public final SoundCueBuffer sounds = new SoundCueBuffer(16);

    public void clear() {
        sprites.clear();
        sounds.clear();
    }
}
