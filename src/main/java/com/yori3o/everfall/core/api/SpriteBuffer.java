package com.yori3o.everfall.core.api;

public final class SpriteBuffer {
    public final float[] x;
    public final float[] y;
    public final float[] width;
    public final float[] height;
    public final float[] rotation;
    public final float[] alpha;
    public final int[] color;
    public final byte[] layer;
    public int count;

    public SpriteBuffer(int capacity) {
        x = new float[capacity];
        y = new float[capacity];
        width = new float[capacity];
        height = new float[capacity];
        rotation = new float[capacity];
        alpha = new float[capacity];
        color = new int[capacity];
        layer = new byte[capacity];
    }

    public void clear() {
        count = 0;
    }

    public boolean add(float x, float y, float width, float height, float rotation, float alpha, int color, byte layer) {
        if (count == this.x.length) {
            return false;
        }

        int index = count++;
        this.x[index] = x;
        this.y[index] = y;
        this.width[index] = width;
        this.height[index] = height;
        this.rotation[index] = rotation;
        this.alpha[index] = alpha;
        this.color[index] = color;
        this.layer[index] = layer;
        return true;
    }
}
