package com.yori3o.everfall.mc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.yori3o.everfall.mc.compat.Ids;

import net.minecraft.sounds.SoundEvent;

public final class ProfileSoundEvents {

    private static final List<String> PATHS = List.of(
            "autumn.sweep", "autumn.capture", "winter.sweep", "winter.short_sweep", "winter.capture");

    private static final Map<String, SoundEvent> CLIENT_EVENTS = new HashMap<>();

    private ProfileSoundEvents() {
    }

    public static List<String> paths() {
        return PATHS;
    }

    public static void reload() {
        CLIENT_EVENTS.clear();

        for (var profile : ParticleProfileRegistry.profiles()) {
            for (String id : profile.sweepSounds()) get(id);

            for (String id : profile.shortSweepSounds()) get(id);

            for (String id : profile.captureSounds()) get(id);
        }
    }

    // Client-only resource-pack events are direct events, not late writes to the frozen game registry.
    // Minecraft's SoundManager loads their actual definitions/files from sounds.json on each reload.
    public static SoundEvent get(String id) {
        return CLIENT_EVENTS.computeIfAbsent(id, key -> SoundEvent.createVariableRangeEvent(Ids.parse(key)));
    }

    public static SoundEvent create(String path) {
        return SoundEvent.createVariableRangeEvent(Ids.parse("everfall:" + path));
    }
}
