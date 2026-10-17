package com.yori3o.everfall.mc.compat;

import com.yori3o.everfall.core.api.SoundCueBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

public final class SoundCuePlayer {
    private SoundCuePlayer() {
    }

    public static void play(SoundCueBuffer cues) {
        for (int i = 0; i < cues.count(); i++) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(
                    SoundEvent.createVariableRangeEvent(Ids.parse(cues.soundIdAt(i))),
                    cues.volumeAt(i), cues.pitchAt(i)));
        }
    }
}
