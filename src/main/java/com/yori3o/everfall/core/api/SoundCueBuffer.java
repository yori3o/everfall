package com.yori3o.everfall.core.api;

public final class SoundCueBuffer {
    private final String[] soundIds;
    private final float[] volumes;
    private final float[] pitches;
    private final float[] pans;
    private final byte[] kinds;
    private int count;

    public SoundCueBuffer(int capacity) {
        soundIds = new String[capacity];
        volumes = new float[capacity];
        pitches = new float[capacity];
        pans = new float[capacity];
        kinds = new byte[capacity];
    }

    public void clear() {
        count = 0;
    }

    public void add(String soundId, float volume, float pitch, float pan, byte kind) {
        if (count == soundIds.length) {
            return;
        }

        soundIds[count] = soundId;
        volumes[count] = volume;
        pitches[count] = pitch;
        pans[count] = pan;
        kinds[count] = kind;
        count++;
    }

    public int count() {
        return count;
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
