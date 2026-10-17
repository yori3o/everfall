package com.yori3o.everfall.mc.compat;

import com.yori3o.everfall.core.api.SoundCueBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.WeighedSoundEvents;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class SoundCuePlayer {
    private static final int MAX_SWEEP_SOUNDS = 6;
    private static final long SWEEP_COOLDOWN_NANOS = 500_000_000L;
    private static final long GLOBAL_SWEEP_COOLDOWN_NANOS = 100_000_000L;
    private static Cooldown globalSweepCooldown;
    private static final Map<String, Cooldown> sweepCooldowns = new HashMap<>();
    private static final SimpleSoundInstance[] activeSweepSounds = new SimpleSoundInstance[MAX_SWEEP_SOUNDS];
    private static final long[] particleIds = new long[MAX_SWEEP_SOUNDS];

    private SoundCuePlayer() {
    }

    public static void play(SoundCueBuffer cues, float masterVolume) {
        masterVolume = Math.max(0.0f, Math.min(masterVolume, 1.0f));
        if (masterVolume <= 0.0f) return;
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        for (int slot = 0; slot < MAX_SWEEP_SOUNDS; slot++) {
            if (activeSweepSounds[slot] != null && !manager.isActive(activeSweepSounds[slot])) {
                activeSweepSounds[slot] = null;
            }
        }
        for (int i = 0; i < cues.count(); i++) {
            if (cues.kindAt(i) != 0) {
                // Capture sounds bypass sweep limits and never occupy a sweep slot.
                manager.play(createSound(cues, i, masterVolume));
                continue;
            }
            long now = System.nanoTime();
            if (globalSweepCooldown != null && globalSweepCooldown.active(now)) {
                continue;
            }
            int freeSlot = -1;
            boolean particleBusy = false;
            for (int slot = 0; slot < MAX_SWEEP_SOUNDS; slot++) {
                if (activeSweepSounds[slot] == null) {
                    if (freeSlot < 0) {
                        freeSlot = slot;
                    }
                } else if (particleIds[slot] == cues.particleIdAt(i)) {
                    particleBusy = true;
                    break;
                }
            }
            if (particleBusy || freeSlot < 0) {
                continue;
            }
            ResolvedSweepSound sound = new ResolvedSweepSound(cues, i, masterVolume);
            if (sound.resolve(manager) == null || sound.getSound() == SoundManager.EMPTY_SOUND) {
                continue;
            }
            String file = sound.getSound().getPath().toString();
            Cooldown previous = sweepCooldowns.get(file);
            if (previous != null && previous.active(now)) {
                continue;
            }
            manager.play(sound);
            sweepCooldowns.put(file, randomCooldown(now, SWEEP_COOLDOWN_NANOS));
            globalSweepCooldown = randomCooldown(now, GLOBAL_SWEEP_COOLDOWN_NANOS);
            activeSweepSounds[freeSlot] = sound;
            particleIds[freeSlot] = cues.particleIdAt(i);
        }
    }

    private record Cooldown(long startedAt, long durationNanos) {
        boolean active(long now) {
            return now - startedAt < durationNanos;
        }
    }

    private static Cooldown randomCooldown(long now, long baseNanos) {
        long half = baseNanos / 2;
        long duration = ThreadLocalRandom.current().nextLong(baseNanos - half, baseNanos + half + 1);
        return new Cooldown(now, duration);
    }

    private static SimpleSoundInstance createSound(SoundCueBuffer cues, int index, float masterVolume) {
        // forUI takes pitch before volume.
        return SimpleSoundInstance.forUI(
                SoundEvent.createVariableRangeEvent(Ids.parse(cues.soundIdAt(index))),
                cues.pitchAt(index), cues.volumeAt(index) * masterVolume);
    }

    // Resolve once so cooldown checks and playback use the exact same random file.
    private static final class ResolvedSweepSound extends SimpleSoundInstance {
        private final String eventId;
        private boolean resolved;
        private WeighedSoundEvents resolvedEvent;

        private ResolvedSweepSound(SoundCueBuffer cues, int index, float masterVolume) {
            super(Ids.parse(cues.soundIdAt(index)), SoundSource.MASTER,
                    cues.volumeAt(index) * masterVolume, cues.pitchAt(index), SoundInstance.createUnseededRandom(),
                    false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
            eventId = cues.soundIdAt(index);
        }

        // Minecraft <=26.2 calls resolve; 26.3 calls getOrResolve.
        public WeighedSoundEvents resolve(SoundManager manager) {
            if (!resolved) {
                resolvedEvent = manager.getSoundEvent(Ids.parse(eventId));
                sound = resolvedEvent == null ? SoundManager.EMPTY_SOUND : resolvedEvent.getSound(random);
                resolved = true;
            }
            return resolvedEvent;
        }

        public WeighedSoundEvents getOrResolve(SoundManager manager) {
            return resolve(manager);
        }

        public WeighedSoundEvents getSoundEvent() {
            return resolvedEvent;
        }
    }
}
