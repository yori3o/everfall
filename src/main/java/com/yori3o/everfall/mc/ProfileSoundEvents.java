package com.yori3o.everfall.mc;

import com.yori3o.everfall.mc.compat.Ids;
import java.util.List;
import net.minecraft.sounds.SoundEvent;

public final class ProfileSoundEvents {
    private static final List<String> PATHS = List.of(
            "autumn.sweep", "autumn.capture", "winter.sweep", "winter.capture");

    private ProfileSoundEvents() {
    }

    public static List<String> paths() {
        return PATHS;
    }

    public static SoundEvent create(String path) {
        return SoundEvent.createVariableRangeEvent(Ids.parse("everfall:" + path));
    }
}
