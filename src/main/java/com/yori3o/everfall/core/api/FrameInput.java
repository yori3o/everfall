package com.yori3o.everfall.core.api;

import com.yori3o.everfall.core.config.ModConfig;
import com.yori3o.everfall.core.profile.ParticleProfile;

public final class FrameInput {

    public int screenWidth;
    public int screenHeight;
    public float coordinateScale = 1.0f;
    /** Simulation units per display pixel; interaction tuning uses display pixels. */
    public float interactionScale = 1.0f;
    public float deltaSeconds;
    public float cursorX;
    public float cursorY;
    public float cursorPreviousX;
    public float cursorPreviousY;
    public float cursorVelocityX;
    public float cursorVelocityY;
    public boolean cursorPathValid;
    public boolean cursorOnScreen;
    public boolean mouseDown;
    public boolean screenChanged;
    public boolean inWorld;
    public final ObstacleBuffer obstacles = new ObstacleBuffer(256);
    public ModConfig config;
    public ParticleProfile profile;
    /** Scene allocation within the configured global limit; -1 means no additional layers. */
    public int particleLimit = -1;
}
