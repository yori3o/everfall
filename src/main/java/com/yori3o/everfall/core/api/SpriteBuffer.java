package com.yori3o.everfall.core.api;

import java.util.Arrays;

public final class SpriteBuffer {
    public float[] x;
    public float[] y;
    public float[] width;
    public float[] height;
    public float[] rotation;
    public float[] alpha;
    public int[] color;
    public byte[] layer;
    /** Negative indices represent untextured colored quads (e.g. procedural firefly glow). */
    public int[] textureIndex;
    public int[] frameIndex;
    public boolean[] alternativeAnimation;
    public int count;
    public boolean flipX;

    public SpriteBuffer(int capacity) {
        x = new float[capacity];
        y = new float[capacity];
        width = new float[capacity];
        height = new float[capacity];
        rotation = new float[capacity];
        alpha = new float[capacity];
        color = new int[capacity];
        layer = new byte[capacity];
        textureIndex = new int[capacity];
        frameIndex = new int[capacity];
        alternativeAnimation = new boolean[capacity];
    }

    public void ensureCapacity(int capacity) {
        if (capacity <= x.length) {
            return;
        }
        x = Arrays.copyOf(x, capacity);
        y = Arrays.copyOf(y, capacity);
        width = Arrays.copyOf(width, capacity);
        height = Arrays.copyOf(height, capacity);
        rotation = Arrays.copyOf(rotation, capacity);
        alpha = Arrays.copyOf(alpha, capacity);
        color = Arrays.copyOf(color, capacity);
        layer = Arrays.copyOf(layer, capacity);
        textureIndex = Arrays.copyOf(textureIndex, capacity);
        frameIndex = Arrays.copyOf(frameIndex, capacity);
        alternativeAnimation = Arrays.copyOf(alternativeAnimation, capacity);
    }

    public void clear() {
        count = 0;
        flipX = false;
    }

    public boolean add(float x, float y, float width, float height, float rotation, float alpha, int color,
                       byte layer, int textureIndex, int frameIndex) {
        return add(x, y, width, height, rotation, alpha, color, layer, textureIndex, frameIndex, false);
    }

    public boolean add(float x, float y, float width, float height, float rotation, float alpha, int color,
                       byte layer, int textureIndex, int frameIndex, boolean alternativeAnimation) {
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
        this.textureIndex[index] = textureIndex;
        this.frameIndex[index] = frameIndex;
        this.alternativeAnimation[index] = alternativeAnimation;
        return true;
    }
}
