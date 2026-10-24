package com.yori3o.everfall.core.engine;

import com.yori3o.everfall.core.api.FrameInput;
import com.yori3o.everfall.core.api.SpriteBuffer;
import com.yori3o.everfall.core.profile.AdditionalProfile;

public interface AdditionalEffectEngine {

    void reset(long seed);
    int particleCount();
    SpriteBuffer step(FrameInput input, AdditionalProfile profile, int particleLimit);
}
