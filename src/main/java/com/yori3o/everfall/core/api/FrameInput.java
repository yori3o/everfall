package com.yori3o.everfall.core.api;

import com.yori3o.everfall.core.config.ModConfig;

public final class FrameInput {
    public int screenWidth;
    public int screenHeight;
    public float deltaSeconds;
    public float cursorX;
    public float cursorY;
    public float cursorVelocityX;
    public float cursorVelocityY;
    public boolean cursorOnScreen;
    public boolean mouseDown;
    public boolean inWorld;
    public final ObstacleBuffer obstacles = new ObstacleBuffer(256);
    public ModConfig config;
}
