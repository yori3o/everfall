package com.yori3o.everfall.core.engine;

import com.yori3o.everfall.core.api.FrameInput;
import com.yori3o.everfall.core.api.SpriteBuffer;
import com.yori3o.everfall.core.profile.AdditionalProfile;
import java.util.Arrays;

/** Slow independent wandering, confined to the lower third. No widget/cursor collisions. */
public final class FireflyParticleEngine implements AdditionalEffectEngine {
    private final SpriteBuffer sprites = new SpriteBuffer(0);
    private float[] x = new float[0], y = new float[0], heading = new float[0], turn = new float[0],
            targetTurn = new float[0], turnTimer = new float[0], pulse = new float[0];
    private int count;
    private long randomState = 1;

    @Override
    public void reset(long seed) {
        count = 0;
        randomState = seed == 0 ? 1 : seed;
        sprites.clear();
    }

    @Override
    public SpriteBuffer step(FrameInput input, AdditionalProfile profile, int particleLimit) {
        sprites.clear();
        int limit = Math.max(0, particleLimit);
        if (input.screenWidth <= 0 || input.screenHeight <= 0 || limit == 0) {
            count = 0;
            return sprites;
        }
        ensureCapacity(limit);
        count = Math.min(count, limit);
        float size = Math.min(profile.size() * input.coordinateScale,
                Math.min(input.screenWidth / 5.0f, input.screenHeight / 15.0f));
        float radius = size * 2.5f;
        float left = radius;
        float right = Math.max(left, input.screenWidth - radius);
        float top = input.screenHeight * (2.0f / 3.0f) + radius;
        float bottom = Math.max(top, input.screenHeight - radius);
        while (count < limit) {
            int i = count++;
            x[i] = left + nextFloat() * (right - left);
            y[i] = top + nextFloat() * (bottom - top);
            heading[i] = nextFloat() * 6.2831855f;
            turn[i] = 0;
            targetTurn[i] = 0;
            turnTimer[i] = 0;
            pulse[i] = nextFloat() * 6.2831855f;
        }
        float dt = Math.max(0, Math.min(input.deltaSeconds, 0.1f));
        float smoothing = 1.0f - (float) Math.exp(-dt * 1.5f);
        for (int i = 0; i < count; i++) {
            turnTimer[i] -= dt;
            if (turnTimer[i] <= 0) {
                targetTurn[i] = (nextFloat() * 2 - 1) * 1.6f;
                turnTimer[i] = 1.0f + nextFloat() * 3.0f;
            }
            turn[i] += (targetTurn[i] - turn[i]) * smoothing;
            heading[i] += turn[i] * dt;
            float dx = (float) Math.cos(heading[i]);
            float dy = (float) Math.sin(heading[i]);
            x[i] += dx * profile.speed() * dt;
            y[i] += dy * profile.speed() * dt;
            if ((x[i] < left && dx < 0) || (x[i] > right && dx > 0)) {
                heading[i] = (float) Math.PI - heading[i];
            }
            if ((y[i] < top && dy < 0) || (y[i] > bottom && dy > 0)) heading[i] = -heading[i];
            x[i] = Math.max(left, Math.min(right, x[i]));
            y[i] = Math.max(top, Math.min(bottom, y[i]));
            pulse[i] += dt * (0.9f + (i % 5) * 0.13f);
            float glow = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(pulse[i]));
            addGlow(i, size * 5, glow * 0.06f, profile.color());
            addGlow(i, size * 3, glow * 0.18f, profile.color());
            addGlow(i, size, glow, profile.color());
        }
        return sprites;
    }

    private void addGlow(int i, float size, float alpha, int color) {
        sprites.add(x[i] - size * 0.5f, y[i] - size * 0.5f, size, size,
                0, alpha, color, (byte) 1, -1, 0);
    }

    private void ensureCapacity(int capacity) {
        if (capacity <= x.length) return;
        x = Arrays.copyOf(x, capacity);
        y = Arrays.copyOf(y, capacity);
        heading = Arrays.copyOf(heading, capacity);
        turn = Arrays.copyOf(turn, capacity);
        targetTurn = Arrays.copyOf(targetTurn, capacity);
        turnTimer = Arrays.copyOf(turnTimer, capacity);
        pulse = Arrays.copyOf(pulse, capacity);
        sprites.ensureCapacity(Math.multiplyExact(capacity, 3));
    }

    private float nextFloat() {
        randomState ^= randomState << 13;
        randomState ^= randomState >>> 7;
        randomState ^= randomState << 17;
        return (randomState >>> 40) / (float) (1 << 24);
    }
}
