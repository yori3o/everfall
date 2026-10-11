package com.yori3o.everfall.core.api;

public interface ParticleEngine {
    void reset(long seed);

    FrameOutput step(FrameInput input);
}
