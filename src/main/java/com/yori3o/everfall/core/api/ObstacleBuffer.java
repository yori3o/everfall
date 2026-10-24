package com.yori3o.everfall.core.api;

public final class ObstacleBuffer {

    private final float[] x;
    private final float[] y;
    private final float[] width;
    private final float[] height;
    private final int[] ids;
    private int count;

    public ObstacleBuffer(int capacity) {
        x = new float[capacity];
        y = new float[capacity];
        width = new float[capacity];
        height = new float[capacity];
        ids = new int[capacity];
    }

    public void clear() {
        count = 0;
    }

    public boolean add(int id, float x, float y, float width, float height) {
        if (count == this.x.length) {
            return false;
        }

        ids[count] = id;
        this.x[count] = x;
        this.y[count] = y;
        this.width[count] = width;
        this.height[count] = height;
        count++;
        return true;
    }

    public int count() {
        return count;
    }

    public int idAt(int index) {
        return ids[index];
    }

    public float xAt(int index) {
        return x[index];
    }

    public float yAt(int index) {
        return y[index];
    }

    public float widthAt(int index) {
        return width[index];
    }

    public float heightAt(int index) {
        return height[index];
    }
}
