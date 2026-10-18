package com.yori3o.everfall.core.api;

import java.util.Arrays;

public final class SoundCueBuffer {
    public static final byte SWEEP = 0;
    public static final byte CAPTURE = 1;
    public static final byte SHORT_SWEEP = 2;

    private String[] soundIds;
    private long[] particleIds;
    private float[] volumes;
    private float[] pitches;
    private float[] pans;
    private byte[] kinds;
    private int count;

    public SoundCueBuffer(int capacity) {
        soundIds = new String[capacity];
        particleIds = new long[capacity];
        volumes = new float[capacity];
        pitches = new float[capacity];
        pans = new float[capacity];
        kinds = new byte[capacity];
    }

    public void ensureCapacity(int capacity) {
        if (capacity <= soundIds.length) {
            return;
        }
        soundIds = Arrays.copyOf(soundIds, capacity);
        particleIds = Arrays.copyOf(particleIds, capacity);
        volumes = Arrays.copyOf(volumes, capacity);
        pitches = Arrays.copyOf(pitches, capacity);
        pans = Arrays.copyOf(pans, capacity);
        kinds = Arrays.copyOf(kinds, capacity);
    }

    public void clear() {
        count = 0;
    }

    public void add(String soundId, float volume, float pitch, float pan, byte kind, long particleId) {
        if (count == soundIds.length) {
            return;
        }

        soundIds[count] = soundId;
        particleIds[count] = particleId;
        volumes[count] = volume;
        pitches[count] = pitch;
        pans[count] = pan;
        kinds[count] = kind;
        count++;
    }

    public int count() {
        return count;
    }

    public long particleIdAt(int index) {
        return particleIds[index];
    }

    public String soundIdAt(int index) {
        return soundIds[index];
    }

    public float volumeAt(int index) {
        return volumes[index];
    }

    public float pitchAt(int index) {
        return pitches[index];
    }

    public float panAt(int index) {
        return pans[index];
    }

    public byte kindAt(int index) {
        return kinds[index];
    }
}
