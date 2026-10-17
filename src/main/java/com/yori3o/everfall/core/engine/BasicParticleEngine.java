package com.yori3o.everfall.core.engine;

import com.yori3o.everfall.core.api.FrameInput;
import com.yori3o.everfall.core.api.FrameOutput;
import com.yori3o.everfall.core.api.ParticleEngine;
import com.yori3o.everfall.core.profile.ParticleProfile;
import java.util.List;

public final class BasicParticleEngine implements ParticleEngine {
    private static final int CAPACITY = 200;
    private static final float FIXED_STEP_SECONDS = 1.0f / 60.0f;
    private static final int MAX_SUBSTEPS = 6;
    private static final byte FRONT_LAYER = 1;
    private static final float CURSOR_RADIUS = 36.0f;
    private static final float LANDING_CHANCE = 0.5f;
    private static final float LANDED_LIFETIME = 12.0f;
    private static final float SWEPT_LIFETIME = 10.0f;
    private static final float DRAGGED_LIFETIME = 60.0f;
    private static final float AIR_RESISTANCE = 0.65f;
    private static final float SWEEP_IMPULSE = 0.18f;
    private static final float GROUNDED_SWEEP_FACTOR = 0.5f;
    private static final float GROUNDED_WIND_RESPONSE = 3.0f;
    private static final float GROUNDED_WIND_SPEED = 0.05f;
    private static final float MIN_SWEEP_SPEED = 160.0f;
    private static final float FULL_SWEEP_SPEED = 320.0f;
    private static final float GUST_AMPLITUDE = 0.4f;
    private static final float WIND_FOR_MAX_SIDE_SPAWN = 80.0f;
    private static final float MAX_SIDE_SPAWN_SHARE = 0.45f;
    private static final float INITIAL_SCREEN_FILL = 0.3f;
    private static final float MAX_SWEEP_AIR_RESISTANCE = 0.4f;
    private static final float SWEEP_RESISTANCE_PER_SPEED = 0.0004f;
    private static final float SWEEP_RESISTANCE_DECAY = 0.8f;
    private static final float MIN_SWAY_AMPLITUDE = 5.0f;
    private static final float MAX_SWAY_AMPLITUDE = 18.0f;
    private static final float MIN_SWAY_FREQUENCY = 1.2f;
    private static final float MAX_SWAY_FREQUENCY = 3.4f;
    private static final float SWAY_SPEED_FACTOR = 0.45f;
    private static final float SWEPT_SWAY_FACTOR = 0.08f;
    private static final float SWAY_TRANSITION_SECONDS = 0.4f;
    private static final float FADE_OUT_SECONDS = 1.5f;
    private static final float DRAG_CAPTURE_RADIUS = 18.0f;

    private final float[] x = new float[CAPACITY];
    private final float[] y = new float[CAPACITY];
    private final float[] velocityX = new float[CAPACITY];
    private final float[] velocityY = new float[CAPACITY];
    private final float[] size = new float[CAPACITY];
    private final float[] lifetime = new float[CAPACITY];
    private final float[] sweepResistance = new float[CAPACITY];
    private final float[] swayAmplitude = new float[CAPACITY];
    private final float[] swayFrequency = new float[CAPACITY];
    private final float[] swayFactor = new float[CAPACITY];
    private final float[] animationTimer = new float[CAPACITY];
    private final int[] textureIndex = new int[CAPACITY];
    private final int[] frameIndex = new int[CAPACITY];
    private final int[] particleIds = new int[CAPACITY];
    private final int[] supportIds = new int[CAPACITY];
    private final float[] phase = new float[CAPACITY];
    private final boolean[] grounded = new boolean[CAPACITY];
    private final FrameOutput output = new FrameOutput();
    private int count;
    private float spawnAccumulator;
    private float simulationAccumulator;
    private long randomState = 1;
    private int nextParticleId;
    private int draggedParticleId = -1;
    private float dragOffsetX;
    private float dragOffsetY;
    private float pendingSweepStartX;
    private float pendingSweepStartY;
    private float pendingSweepEndX;
    private float pendingSweepEndY;
    private float pendingSweepDuration;
    private boolean pendingSweep;
    private boolean previousMouseDown;
    private float gustOffset;
    private float gustStart;
    private float gustTarget;
    private float gustDuration;
    private float gustElapsed;
    private float gustTimeRemaining;
    private String currentProfileId;

    @Override
    public void reset(long seed) {
        count = 0;
        currentProfileId = null;
        spawnAccumulator = 0.0f;
        simulationAccumulator = 0.0f;
        randomState = seed == 0 ? 1 : seed;
        nextParticleId = 0;
        draggedParticleId = -1;
        pendingSweep = false;
        pendingSweepDuration = 0.0f;
        previousMouseDown = false;
        gustOffset = 0.0f;
        gustStart = 0.0f;
        gustTarget = 0.0f;
        gustDuration = 0.0f;
        gustElapsed = 0.0f;
        gustTimeRemaining = 0.0f;
    }

    public void clearGrounded() {
        pendingSweep = false;
        pendingSweepDuration = 0.0f;
        int active = 0;
        for (int i = 0; i < count; i++) {
            if (grounded[i]) {
                continue;
            }
            if (active != i) {
                copy(i, active);
            }
            active++;
        }
        count = active;
    }

    @Override
    public FrameOutput step(FrameInput input) {
        output.clear();
        if (input.config == null || !input.config.enabled || (input.inWorld && !input.config.showInWorld)
                || input.screenWidth <= 0 || input.screenHeight <= 0) {
            pendingSweep = false;
            pendingSweepDuration = 0.0f;
            return output;
        }

        float dt = Math.max(0.0f, Math.min(input.deltaSeconds, 0.1f));
        updatePendingSweep(input, dt);
        updateDragState(input);
        int particleLimit = Math.max(0, Math.min(input.config.maxParticles, CAPACITY));
        boolean profileChanged = !input.profile.id().equals(currentProfileId);
        if (profileChanged) {
            count = 0;
            spawnAccumulator = 0.0f;
            currentProfileId = input.profile.id();
        }
        if ((input.screenChanged || profileChanged) && count == 0) {
            seedInitialScreenParticles(input, particleLimit);
        }
        simulationAccumulator += dt;
        int substeps = 0;
        while (simulationAccumulator >= FIXED_STEP_SECONDS && substeps < MAX_SUBSTEPS) {
            simulate(input, FIXED_STEP_SECONDS, particleLimit, substeps == 0 && pendingSweep);
            if (substeps == 0) {
                pendingSweep = false;
                pendingSweepDuration = 0.0f;
            }
            simulationAccumulator -= FIXED_STEP_SECONDS;
            substeps++;
        }
        if (substeps == MAX_SUBSTEPS && simulationAccumulator >= FIXED_STEP_SECONDS) {
            simulationAccumulator %= FIXED_STEP_SECONDS;
        }
        emit();
        return output;
    }

    private void simulate(FrameInput input, float dt, int particleLimit, boolean applyCursorSweep) {
        float wind = updateGust(input.config.wind, dt);
        spawnAccumulator += dt * Math.max(0.0f, input.config.density) * 6.0f;
        if (count >= particleLimit) {
            spawnAccumulator = Math.min(spawnAccumulator, 1.0f);
        }
        while (spawnAccumulator >= 1.0f && count < particleLimit) {
            spawnAccumulator -= 1.0f;
            spawn(input, input.config.wind);
        }
        float cursorDeltaX = pendingSweepEndX - pendingSweepStartX;
        float cursorDeltaY = pendingSweepEndY - pendingSweepStartY;
        float cursorDistance = (float) Math.sqrt(cursorDeltaX * cursorDeltaX + cursorDeltaY * cursorDeltaY);
        float cursorSpeed = pendingSweepDuration > 0.0f ? cursorDistance / pendingSweepDuration : 0.0f;
        float cursorDirectionX = cursorDistance > 0.0f ? cursorDeltaX / cursorDistance : 0.0f;
        float cursorDirectionY = cursorDistance > 0.0f ? cursorDeltaY / cursorDistance : 0.0f;

        int active = 0;
        boolean sweepSoundAdded = false;
        for (int i = 0; i < count; i++) {
            advanceAnimation(i, dt, input.profile);
            if (particleIds[i] == draggedParticleId) {
                updateSwayFactor(i, 0.0f, dt);
                x[i] = input.cursorX + dragOffsetX - size[i] * 0.5f;
                y[i] = input.cursorY + dragOffsetY - size[i] * 0.5f;
                velocityX[i] = input.cursorVelocityX;
                velocityY[i] = input.cursorVelocityY;
                grounded[i] = false;
                if (active != i) {
                    copy(i, active);
                }
                active++;
                continue;
            }

            if (grounded[i] && !containsObstacle(input, supportIds[i])) {
                continue;
            }
            if (grounded[i]) {
                lifetime[i] -= dt;
                if (lifetime[i] <= 0.0f) {
                    continue;
                }
            }

            float previousDrawX = x[i] + (float) Math.sin(phase[i]) * swayAmplitude[i] * swayFactor[i];
            boolean swept = false;
            if (applyCursorSweep && !input.mouseDown && cursorSpeed > MIN_SWEEP_SPEED && input.cursorOnScreen) {
                float distance = distanceToSegment(x[i] + size[i] * 0.5f, y[i] + size[i] * 0.5f,
                        pendingSweepStartX, pendingSweepStartY, pendingSweepEndX, pendingSweepEndY);
                if (distance < CURSOR_RADIUS) {
                    float falloff = 1.0f - distance / CURSOR_RADIUS;
                    float speedFactor = clamp((cursorSpeed - MIN_SWEEP_SPEED)
                            / (FULL_SWEEP_SPEED - MIN_SWEEP_SPEED), 0.0f, 1.0f);
                    if (!sweepSoundAdded && speedFactor > 0.0f) {
                        addSound(input.profile.sweepSounds(), (byte) 0);
                        sweepSoundAdded = true;
                    }
                    if (grounded[i]) {
                        float sweepImpulse = Math.min(cursorSpeed * SWEEP_IMPULSE, 90.0f)
                                * GROUNDED_SWEEP_FACTOR * falloff * speedFactor;
                        velocityX[i] += cursorDirectionX * sweepImpulse;
                        if (cursorDirectionY < -0.1f) {
                            velocityY[i] += cursorDirectionY * sweepImpulse;
                            grounded[i] = false;
                            swept = true;
                            sweepResistance[i] = Math.max(sweepResistance[i],
                                    Math.min(MAX_SWEEP_AIR_RESISTANCE,
                                            cursorSpeed * SWEEP_RESISTANCE_PER_SPEED));
                        }
                    } else {
                        float sweepImpulse = Math.min(cursorSpeed * SWEEP_IMPULSE, 90.0f)
                                * falloff * speedFactor;
                        velocityX[i] += cursorDirectionX * sweepImpulse;
                        velocityY[i] += cursorDirectionY * sweepImpulse;
                        sweepResistance[i] = Math.max(sweepResistance[i],
                                Math.min(MAX_SWEEP_AIR_RESISTANCE, cursorSpeed * SWEEP_RESISTANCE_PER_SPEED));
                        grounded[i] = false;
                        swept = true;
                    }
                    lifetime[i] = Math.max(lifetime[i], SWEPT_LIFETIME);
                }
            }

            if (grounded[i] && !hasSupport(input, i)) {
                grounded[i] = false;
            }

            float previousY = y[i];
            if (grounded[i]) {
                float friction = (float) Math.exp(-GROUNDED_WIND_RESPONSE * dt);
                float windDrift = clamp(wind * GROUNDED_WIND_SPEED, -2.0f, 2.0f);
                velocityX[i] = velocityX[i] * friction + windDrift * (1.0f - friction);
                velocityY[i] = 0.0f;
            } else {
                float airDrag = (float) Math.exp(-(AIR_RESISTANCE + sweepResistance[i]) * dt);
                sweepResistance[i] *= (float) Math.exp(-SWEEP_RESISTANCE_DECAY * dt);
                velocityX[i] *= airDrag;
                velocityY[i] = velocityY[i] * airDrag + Math.max(0.0f, input.config.gravity) * dt;
            }
            velocityX[i] = clamp(velocityX[i], -180.0f, 180.0f);
            velocityY[i] = clamp(velocityY[i], -120.0f, 120.0f);
            float nextX = x[i] + (velocityX[i] + (grounded[i] ? 0.0f : wind)) * dt;
            float nextY = y[i] + velocityY[i] * dt;
            float nextPhase = phase[i] + dt * swayFrequency[i] * SWAY_SPEED_FACTOR * (0.8f
                    + (float) Math.sqrt(velocityX[i] * velocityX[i]
                    + velocityY[i] * velocityY[i]) * 0.01f);
            float targetSwayFactor = grounded[i] ? 0.0f
                    : sweepResistance[i] > 0.05f ? SWEPT_SWAY_FACTOR : 1.0f;
            updateSwayFactor(i, targetSwayFactor, dt);
            float nextDrawX = nextX + (float) Math.sin(nextPhase) * swayAmplitude[i] * swayFactor[i];
            float landingY = Float.POSITIVE_INFINITY;
            int landingObstacleId = Integer.MIN_VALUE;
            if (!grounded[i] && !swept && velocityY[i] >= 0.0f) {
                for (int obstacle = 0; obstacle < input.obstacles.count(); obstacle++) {
                    float obstacleY = input.obstacles.yAt(obstacle);
                    float obstacleLeft = input.obstacles.xAt(obstacle);
                    float obstacleRight = obstacleLeft + input.obstacles.widthAt(obstacle);
                    if (previousY + size[i] <= obstacleY && nextY + size[i] >= obstacleY
                            && Math.max(previousDrawX, nextDrawX) + size[i] >= obstacleLeft
                            && Math.min(previousDrawX, nextDrawX) <= obstacleRight
                            && obstacleY < landingY) {
                        landingY = obstacleY;
                        landingObstacleId = input.obstacles.idAt(obstacle);
                    }
                }
            }
            boolean landed = landingY < Float.POSITIVE_INFINITY && nextFloat() < LANDING_CHANCE;
            x[i] = landed ? nextDrawX : nextX;
            y[i] = landed ? landingY - size[i] : nextY;
            if (landed) {
                velocityY[i] = 0.0f;
                grounded[i] = true;
                supportIds[i] = landingObstacleId;
                lifetime[i] = Math.max(lifetime[i], LANDED_LIFETIME);
                phase[i] = 0.0f;
            } else if (landingY < Float.POSITIVE_INFINITY) {
                grounded[i] = false;
                supportIds[i] = Integer.MIN_VALUE;
            } else if (grounded[i] && !hasSupport(input, i)) {
                grounded[i] = false;
                supportIds[i] = Integer.MIN_VALUE;
            }
            if (!landed) {
                phase[i] = nextPhase;
            }
            if (y[i] >= input.screenHeight + size[i] || x[i] < -size[i]
                    || x[i] >= input.screenWidth + size[i]) {
                continue;
            }
            if (active != i) {
                copy(i, active);
            }
            active++;
        }
        count = active;
    }

    private void emit() {
        for (int i = 0; i < count; i++) {
            float alpha = clamp(lifetime[i] / FADE_OUT_SECONDS, 0.0f, 1.0f);
            float sway = (float) Math.sin(phase[i]) * swayAmplitude[i] * swayFactor[i];
            output.sprites.add(x[i] + sway, y[i], size[i], size[i], phase[i], alpha, 0xffffffff, FRONT_LAYER,
                    textureIndex[i], frameIndex[i]);
        }
    }

    private void spawn(FrameInput input, float configuredWind) {
        int index = count++;
        initializeParticle(input, index);
        float sideSpawnShare = MAX_SIDE_SPAWN_SHARE
                * clamp(Math.abs(configuredWind) / WIND_FOR_MAX_SIDE_SPAWN, 0.0f, 1.0f);
        if (configuredWind != 0.0f && nextFloat() < sideSpawnShare) {
            x[index] = configuredWind > 0.0f ? -size[index] : input.screenWidth + size[index];
            y[index] = nextFloat() * input.screenHeight;
            velocityX[index] = 0.0f;
            velocityY[index] = 2.0f + nextFloat() * 8.0f;
        } else {
            x[index] = nextFloat() * input.screenWidth;
            y[index] = -size[index] - nextFloat() * 24.0f;
            velocityX[index] = (nextFloat() - 0.5f) * 8.0f;
            velocityY[index] = 4.0f + nextFloat() * 10.0f;
        }
    }

    private void seedInitialScreenParticles(FrameInput input, int particleLimit) {
        int initialCount = Math.min(particleLimit,
                Math.round(particleLimit * INITIAL_SCREEN_FILL
                        * clamp(input.config.density, 0.0f, 1.0f)));
        for (int i = 0; i < initialCount; i++) {
            int index = count++;
            initializeParticle(input, index);
            x[index] = nextFloat() * Math.max(0.0f, input.screenWidth - size[index]);
            y[index] = nextFloat() * Math.max(0.0f, input.screenHeight - size[index]);
            velocityX[index] = (nextFloat() - 0.5f) * 8.0f;
            velocityY[index] = nextFloat() * 12.0f;
        }
    }

    private void initializeParticle(FrameInput input, int index) {
        int textureCount = input.profile.textures().size();
        textureIndex[index] = (int) (nextFloat() * textureCount);
        ParticleProfile.Texture texture = input.profile.textures().get(textureIndex[index]);
        size[index] = texture.frameSize() * input.coordinateScale;
        lifetime[index] = 8.0f + nextFloat() * 6.0f;
        swayAmplitude[index] = MIN_SWAY_AMPLITUDE
                + nextFloat() * (MAX_SWAY_AMPLITUDE - MIN_SWAY_AMPLITUDE);
        swayFactor[index] = 1.0f;
        swayFrequency[index] = MIN_SWAY_FREQUENCY
                + nextFloat() * (MAX_SWAY_FREQUENCY - MIN_SWAY_FREQUENCY);
        sweepResistance[index] = 0.0f;
        animationTimer[index] = nextFloat() * texture.frameTime() / 20.0f;
        frameIndex[index] = (int) (nextFloat() * texture.frameCount());
        particleIds[index] = nextParticleId++;
        phase[index] = nextFloat() * 6.2831855f;
        grounded[index] = false;
        supportIds[index] = Integer.MIN_VALUE;
    }

    private void copy(int from, int to) {
        x[to] = x[from];
        y[to] = y[from];
        velocityX[to] = velocityX[from];
        velocityY[to] = velocityY[from];
        size[to] = size[from];
        lifetime[to] = lifetime[from];
        particleIds[to] = particleIds[from];
        supportIds[to] = supportIds[from];
        phase[to] = phase[from];
        sweepResistance[to] = sweepResistance[from];
        swayAmplitude[to] = swayAmplitude[from];
        swayFrequency[to] = swayFrequency[from];
        swayFactor[to] = swayFactor[from];
        animationTimer[to] = animationTimer[from];
        textureIndex[to] = textureIndex[from];
        frameIndex[to] = frameIndex[from];
        grounded[to] = grounded[from];
    }

    private void updateDragState(FrameInput input) {
        if (!input.mouseDown) {
            if (draggedParticleId >= 0) {
                int index = findParticle(draggedParticleId);
                if (index >= 0) {
                    lifetime[index] = Math.max(lifetime[index], DRAGGED_LIFETIME);
                    velocityX[index] = input.cursorVelocityX;
                    velocityY[index] = input.cursorVelocityY;
                    grounded[index] = false;
                }
                draggedParticleId = -1;
            }
        } else if (!previousMouseDown && input.cursorOnScreen) {
            int index = findParticleToDrag(input);
            if (index >= 0) {
                draggedParticleId = particleIds[index];
                dragOffsetX = x[index] + size[index] * 0.5f - input.cursorX;
                dragOffsetY = y[index] + size[index] * 0.5f - input.cursorY;
                grounded[index] = false;
                addSound(input.profile.captureSounds(), (byte) 1);
            }
        }
        previousMouseDown = input.mouseDown;
    }

    private void updatePendingSweep(FrameInput input, float dt) {
        if (input.mouseDown || !input.cursorOnScreen) {
            pendingSweep = false;
            pendingSweepDuration = 0.0f;
            return;
        }
        if (input.cursorPathValid
                && (input.cursorX != input.cursorPreviousX || input.cursorY != input.cursorPreviousY)) {
            if (!pendingSweep) {
                pendingSweepStartX = input.cursorPreviousX;
                pendingSweepStartY = input.cursorPreviousY;
                pendingSweepDuration = 0.0f;
                pendingSweep = true;
            }
            pendingSweepEndX = input.cursorX;
            pendingSweepEndY = input.cursorY;
            pendingSweepDuration += dt;
        }
    }

    private void addSound(List<String> soundIds, byte kind) {
        if (!soundIds.isEmpty()) {
            int soundIndex = (int) (nextFloat() * soundIds.size());
            output.sounds.add(soundIds.get(soundIndex), 0.7f, 1.0f, 0.0f, kind);
        }
    }

    private void advanceAnimation(int index, float dt, ParticleProfile profile) {
        ParticleProfile.Texture texture = profile.textures().get(textureIndex[index]);
        float frameDuration = texture.frameTime() / 20.0f;
        animationTimer[index] += dt;
        int framesAdvanced = (int) (animationTimer[index] / frameDuration);
        if (framesAdvanced > 0) {
            frameIndex[index] = (frameIndex[index] + framesAdvanced) % texture.frameCount();
            animationTimer[index] -= framesAdvanced * frameDuration;
        }
    }

    private int findParticleToDrag(FrameInput input) {
        int closest = -1;
        float closestDistanceSquared = DRAG_CAPTURE_RADIUS * DRAG_CAPTURE_RADIUS;
        for (int i = 0; i < count; i++) {
            float dx = input.cursorX - (x[i] + size[i] * 0.5f);
            float dy = input.cursorY - (y[i] + size[i] * 0.5f);
            float distanceSquared = dx * dx + dy * dy;
            if (distanceSquared < closestDistanceSquared) {
                closest = i;
                closestDistanceSquared = distanceSquared;
            }
        }
        return closest;
    }

    private int findParticle(int id) {
        for (int i = 0; i < count; i++) {
            if (particleIds[i] == id) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasSupport(FrameInput input, int particle) {
        float bottom = y[particle] + size[particle];
        for (int obstacle = 0; obstacle < input.obstacles.count(); obstacle++) {
            float obstacleY = input.obstacles.yAt(obstacle);
            float obstacleLeft = input.obstacles.xAt(obstacle);
            float obstacleRight = obstacleLeft + input.obstacles.widthAt(obstacle);
            if (Math.abs(bottom - obstacleY) <= 1.5f && x[particle] + size[particle] > obstacleLeft
                    && x[particle] < obstacleRight) {
                return true;
            }
        }
        return false;
    }

    private boolean containsObstacle(FrameInput input, int id) {
        for (int obstacle = 0; obstacle < input.obstacles.count(); obstacle++) {
            if (input.obstacles.idAt(obstacle) == id) {
                return true;
            }
        }
        return false;
    }

    private float updateGust(float baseWind, float dt) {
        if (gustTimeRemaining <= 0.0f) {
            gustStart = gustOffset;
            gustDuration = 0.5f + nextFloat() * 1.5f;
            gustTimeRemaining = gustDuration;
            gustElapsed = 0.0f;
            gustTarget = (nextFloat() * 2.0f - 1.0f) * Math.abs(baseWind) * GUST_AMPLITUDE;
        }
        gustElapsed += dt;
        gustTimeRemaining -= dt;
        float interpolation = clamp(gustElapsed / gustDuration, 0.0f, 1.0f);
        gustOffset = gustStart + (gustTarget - gustStart) * interpolation;
        return baseWind + gustOffset;
    }

    private float distanceToSegment(float pointX, float pointY, float startX, float startY,
                                    float endX, float endY) {
        float segmentX = endX - startX;
        float segmentY = endY - startY;
        float lengthSquared = segmentX * segmentX + segmentY * segmentY;
        float t = lengthSquared == 0.0f ? 0.0f
                : clamp(((pointX - startX) * segmentX + (pointY - startY) * segmentY) / lengthSquared, 0.0f, 1.0f);
        float dx = pointX - (startX + segmentX * t);
        float dy = pointY - (startY + segmentY * t);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void updateSwayFactor(int index, float target, float dt) {
        float blend = 1.0f - (float) Math.exp(-dt / SWAY_TRANSITION_SECONDS);
        swayFactor[index] += (target - swayFactor[index]) * blend;
    }

    private float nextFloat() {
        randomState ^= randomState << 13;
        randomState ^= randomState >>> 7;
        randomState ^= randomState << 17;
        return (randomState >>> 40) / (float) (1 << 24);
    }
}
