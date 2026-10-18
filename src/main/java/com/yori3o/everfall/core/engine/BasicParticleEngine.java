package com.yori3o.everfall.core.engine;

import com.yori3o.everfall.core.api.FrameInput;
import com.yori3o.everfall.core.api.FrameOutput;
import com.yori3o.everfall.core.api.ParticleEngine;
import com.yori3o.everfall.core.api.SoundCueBuffer;
import com.yori3o.everfall.core.profile.ParticleProfile;
import java.util.Arrays;
import java.util.List;

public final class BasicParticleEngine implements ParticleEngine {
    private static final byte FRONT_LAYER = 1;
    private static final float CURSOR_RADIUS = 36.0f;
    private static final float LANDING_CHANCE = 0.5f;
    private static final float LANDED_LIFETIME = 12.0f;
    private static final float SWEPT_LIFETIME = 10.0f;
    private static final float DRAGGED_LIFETIME = 60.0f;
    private static final float AIR_RESISTANCE = 0.65f;
    private static final float SWEEP_IMPULSE_PER_SECOND = 10.8f;
    private static final float MAX_SWEEP_IMPULSE_PER_SECOND = 5400.0f;
    private static final float GROUNDED_SWEEP_FACTOR = 0.5f;
    private static final float GROUNDED_WIND_RESPONSE = 3.0f;
    private static final float GROUNDED_WIND_SPEED = 0.05f;
    private static final float WIND_RELEASE_INTERVAL = 2.0f;
    private static final float WIND_LIFT_SPEED = 18.0f;
    private static final float MIN_SWEEP_SPEED = 160.0f;
    private static final float FULL_SWEEP_SPEED = 320.0f;
    private static final float GUST_AMPLITUDE = 0.4f;
    private static final float WIND_FOR_MAX_SIDE_SPAWN = 80.0f;
    private static final float MAX_SIDE_SPAWN_SHARE = 0.45f;
    private static final float INITIAL_SCREEN_FILL = 0.5f;
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
    private static final float FADE_IN_SECONDS = 0.75f;
    private static final float EDGE_FADE_DISTANCE = 48.0f;
    private static final float DRAG_CAPTURE_RADIUS = 18.0f;

    private float[] x = new float[0];
    private float[] y = new float[0];
    private float[] velocityX = new float[0];
    private float[] velocityY = new float[0];
    private float[] width = new float[0];
    private float[] height = new float[0];
    private float[] lifetime = new float[0];
    private float[] fadeInProgress = new float[0];
    private double[] windReleaseTimer = new double[0];
    private float[] sweepResistance = new float[0];
    private float[] swayAmplitude = new float[0];
    private float[] swayFrequency = new float[0];
    private float[] swayFactor = new float[0];
    private double[] animationTimer = new double[0];
    private double[] alternativeCooldownAge = new double[0];
    private boolean[] alternativeAnimation = new boolean[0];
    private int[] textureIndex = new int[0];
    private int[] frameIndex = new int[0];
    private long[] particleIds = new long[0];
    private int[] supportIds = new int[0];
    private float[] phase = new float[0];
    private boolean[] grounded = new boolean[0];
    private final FrameOutput output = new FrameOutput();
    private int count;
    private float spawnAccumulator;
    private long randomState = 1;
    private long nextParticleId;
    private long draggedParticleId = -1;
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
        randomState = seed == 0 ? 1 : seed;
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
        if (input.profile != null && input.profile.textures().isEmpty()) {
            // A profile rejected during loading must not spawn particles or retain its old interaction state.
            count = 0;
            spawnAccumulator = 0.0f;
            currentProfileId = null;
            draggedParticleId = -1;
            previousMouseDown = input.mouseDown;
            pendingSweep = false;
            pendingSweepDuration = 0.0f;
            return output;
        }
        if (input.config == null || !input.config.enabled || (input.inWorld && !input.config.showInWorld)
                || input.screenWidth <= 0 || input.screenHeight <= 0) {
            pendingSweep = false;
            pendingSweepDuration = 0.0f;
            return output;
        }

        float dt = Math.max(0.0f, Math.min(input.deltaSeconds, 0.1f));
        boolean interactive = input.config.canInteract();
        if (interactive) {
            updatePendingSweep(input, dt);
        } else {
            pendingSweep = false;
            pendingSweepDuration = 0.0f;
            draggedParticleId = -1;
            previousMouseDown = input.mouseDown;
            Arrays.fill(grounded, 0, count, false);
        }
        int particleLimit = Math.max(0, input.config.maxParticles);
        if (input.particleLimit >= 0) particleLimit = Math.min(particleLimit, input.particleLimit);
        ensureCapacity(particleLimit);
        count = Math.min(count, particleLimit);
        boolean profileChanged = !input.profile.id().equals(currentProfileId);
        if (profileChanged) {
            count = 0;
            spawnAccumulator = 0.0f;
            currentProfileId = input.profile.id();
            draggedParticleId = -1;
        }
        if ((input.screenChanged || profileChanged) && count == 0) {
            seedInitialScreenParticles(input, particleLimit);
        }
        if (interactive) updateDragState(input);
        simulate(input, dt, particleLimit, pendingSweep);
        pendingSweep = false;
        pendingSweepDuration = 0.0f;
        output.sprites.flipX = input.profile.flipWithWind() && input.config.wind < 0;
        emit(input);
        return output;
    }

    private void simulate(FrameInput input, float dt, int particleLimit, boolean applyCursorSweep) {
        float wind = updateGust(input.config.wind, dt);
        boolean invertedGravity = input.config.gravity < 0.0f;
        float gravityDirection = invertedGravity ? -1.0f : 1.0f;
        float windReleaseChance = windReleaseChance(input.config.wind);
        spawnAccumulator += dt * particleLimit * 0.03f;
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
        for (int i = 0; i < count; i++) {
            fadeInProgress[i] = Math.min(1.0f, fadeInProgress[i] + dt / FADE_IN_SECONDS);
            if (particleIds[i] == draggedParticleId) {
                windReleaseTimer[i] = 0;
                alternativeCooldownAge[i] += dt;
                updateSwayFactor(i, 0.0f, dt);
                x[i] = input.cursorX + dragOffsetX - width[i] * 0.5f;
                y[i] = input.cursorY + dragOffsetY - height[i] * 0.5f;
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
            boolean windReleased = false;
            if (grounded[i] && windReleaseChance > 0.0f
                    && lifetime[i] - dt > Math.max(FADE_OUT_SECONDS, WIND_RELEASE_INTERVAL)) {
                windReleaseTimer[i] += dt;
                if (windReleaseTimer[i] >= WIND_RELEASE_INTERVAL) {
                    windReleaseTimer[i] -= WIND_RELEASE_INTERVAL;
                    if (nextFloat() < windReleaseChance) {
                        grounded[i] = false;
                        supportIds[i] = Integer.MIN_VALUE;
                        velocityY[i] = -gravityDirection * WIND_LIFT_SPEED;
                        lifetime[i] = Math.max(lifetime[i], LANDED_LIFETIME);
                        windReleased = true;
                    }
                }
            } else {
                windReleaseTimer[i] = 0;
            }
            if (grounded[i]) {
                lifetime[i] -= dt;
                if (lifetime[i] <= 0.0f) {
                    continue;
                }
            }

            float previousDrawX = x[i] + (float) Math.sin(phase[i]) * swayAmplitude[i] * swayFactor[i];
            boolean swept = windReleased;
            if (applyCursorSweep && cursorSpeed > MIN_SWEEP_SPEED && input.cursorOnScreen) {
                float distance = distanceToSegment(x[i] + width[i] * 0.5f, y[i] + height[i] * 0.5f,
                        pendingSweepStartX, pendingSweepStartY, pendingSweepEndX, pendingSweepEndY);
                if (distance < CURSOR_RADIUS) {
                    float falloff = 1.0f - distance / CURSOR_RADIUS;
                    float speedFactor = clamp((cursorSpeed - MIN_SWEEP_SPEED)
                            / (FULL_SWEEP_SPEED - MIN_SWEEP_SPEED), 0.0f, 1.0f);
                    if (speedFactor > 0.0f) {
                        float volume = grounded[i] ? 0.5f : 0.2f;
                        if (input.profile.shortSweepSounds().isEmpty()) {
                            addSound(input.profile.sweepSounds(), SoundCueBuffer.SWEEP, particleIds[i], volume);
                        } else {
                            addSound(input.profile.shortSweepSounds(), SoundCueBuffer.SHORT_SWEEP, particleIds[i], volume);
                            if (grounded[i]) {
                                addSound(input.profile.sweepSounds(), SoundCueBuffer.SWEEP, particleIds[i], volume);
                            }
                        }
                    }
                    if (grounded[i]) {
                        float sweepImpulse = Math.min(cursorSpeed * SWEEP_IMPULSE_PER_SECOND,
                                MAX_SWEEP_IMPULSE_PER_SECOND) * dt
                                * GROUNDED_SWEEP_FACTOR * falloff * speedFactor;
                        velocityX[i] += cursorDirectionX * sweepImpulse;
                        if (cursorDirectionY * gravityDirection < -0.1f) {
                            velocityY[i] += cursorDirectionY * sweepImpulse;
                            grounded[i] = false;
                            swept = true;
                            sweepResistance[i] = Math.max(sweepResistance[i],
                                    Math.min(MAX_SWEEP_AIR_RESISTANCE,
                                            cursorSpeed * SWEEP_RESISTANCE_PER_SPEED));
                        }
                    } else {
                        float sweepImpulse = Math.min(cursorSpeed * SWEEP_IMPULSE_PER_SECOND,
                                MAX_SWEEP_IMPULSE_PER_SECOND) * dt
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
                velocityY[i] = velocityY[i] * airDrag + input.config.gravity * dt;
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
            if (input.config.canInteract() && !grounded[i] && !swept
                    && velocityY[i] * gravityDirection >= 0.0f) {
                for (int obstacle = 0; obstacle < input.obstacles.count(); obstacle++) {
                    // Project the leading particle edge and widget face onto the gravity direction.
                    float obstacleY = gravityDirection * (input.obstacles.yAt(obstacle)
                            + (invertedGravity ? input.obstacles.heightAt(obstacle) : 0.0f));
                    float obstacleLeft = input.obstacles.xAt(obstacle);
                    float obstacleRight = obstacleLeft + input.obstacles.widthAt(obstacle);
                    float leadingOffset = invertedGravity ? 0.0f : height[i];
                    if (gravityDirection * (previousY + leadingOffset) <= obstacleY
                            && gravityDirection * (nextY + leadingOffset) >= obstacleY
                            && Math.max(previousDrawX, nextDrawX) + width[i] >= obstacleLeft
                            && Math.min(previousDrawX, nextDrawX) <= obstacleRight
                            && obstacleY < landingY) {
                        landingY = obstacleY;
                        landingObstacleId = input.obstacles.idAt(obstacle);
                    }
                }
            }
            boolean landed = landingY < Float.POSITIVE_INFINITY && nextFloat() < LANDING_CHANCE;
            x[i] = landed ? nextDrawX : nextX;
            y[i] = landed ? gravityDirection * landingY - (invertedGravity ? 0.0f : height[i]) : nextY;
            if (landed) {
                velocityY[i] = 0.0f;
                grounded[i] = true;
                supportIds[i] = landingObstacleId;
                lifetime[i] = Math.max(lifetime[i], LANDED_LIFETIME);
                windReleaseTimer[i] = 0;
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
            if (!grounded[i]) {
                advanceAnimation(i, dt, input.profile);
                updateDimensions(i, input.profile.textures().get(textureIndex[i]), input.coordinateScale);
            } else {
                alternativeCooldownAge[i] += dt;
            }
            if ((!invertedGravity && y[i] >= input.screenHeight + height[i]) || x[i] < -width[i]
                    || x[i] >= input.screenWidth + width[i]
                    || (input.config.gravity <= 0.0f && y[i] < -height[i])) {
                continue;
            }
            if (active != i) {
                copy(i, active);
            }
            active++;
        }
        count = active;
    }

    private void ensureCapacity(int capacity) {
        if (capacity <= x.length) {
            return;
        }
        x = Arrays.copyOf(x, capacity);
        y = Arrays.copyOf(y, capacity);
        velocityX = Arrays.copyOf(velocityX, capacity);
        velocityY = Arrays.copyOf(velocityY, capacity);
        width = Arrays.copyOf(width, capacity);
        height = Arrays.copyOf(height, capacity);
        lifetime = Arrays.copyOf(lifetime, capacity);
        fadeInProgress = Arrays.copyOf(fadeInProgress, capacity);
        windReleaseTimer = Arrays.copyOf(windReleaseTimer, capacity);
        sweepResistance = Arrays.copyOf(sweepResistance, capacity);
        swayAmplitude = Arrays.copyOf(swayAmplitude, capacity);
        swayFrequency = Arrays.copyOf(swayFrequency, capacity);
        swayFactor = Arrays.copyOf(swayFactor, capacity);
        animationTimer = Arrays.copyOf(animationTimer, capacity);
        alternativeCooldownAge = Arrays.copyOf(alternativeCooldownAge, capacity);
        alternativeAnimation = Arrays.copyOf(alternativeAnimation, capacity);
        textureIndex = Arrays.copyOf(textureIndex, capacity);
        frameIndex = Arrays.copyOf(frameIndex, capacity);
        particleIds = Arrays.copyOf(particleIds, capacity);
        supportIds = Arrays.copyOf(supportIds, capacity);
        phase = Arrays.copyOf(phase, capacity);
        grounded = Arrays.copyOf(grounded, capacity);
        output.sprites.ensureCapacity(capacity);
        output.sounds.ensureCapacity(Math.multiplyExact(capacity, 2));
    }

    private void emit(FrameInput input) {
        boolean zeroGravity = input.config.gravity == 0.0f;
        float edgeFadeDistance = Math.min(EDGE_FADE_DISTANCE,
                Math.min(input.screenWidth, input.screenHeight) * 0.1f);
        for (int i = 0; i < count; i++) {
            float alpha = clamp(lifetime[i] / FADE_OUT_SECONDS, 0.0f, 1.0f);
            float sway = (float) Math.sin(phase[i]) * swayAmplitude[i] * swayFactor[i];
            float fadeIn = fadeInProgress[i];
            alpha *= fadeIn * fadeIn * (3.0f - 2.0f * fadeIn);
            if (zeroGravity) {
                float centerX = x[i] + sway + width[i] * 0.5f;
                float centerY = y[i] + height[i] * 0.5f;
                float distance = Math.min(Math.min(centerX, input.screenWidth - centerX),
                        Math.min(centerY, input.screenHeight - centerY));
                float edge = clamp(distance / edgeFadeDistance, 0.0f, 1.0f);
                alpha *= edge * edge * (3.0f - 2.0f * edge);
            }
            output.sprites.add(x[i] + sway, y[i], width[i], height[i], phase[i], alpha, 0xffffffff, FRONT_LAYER,
                    textureIndex[i], frameIndex[i], alternativeAnimation[i]);
        }
    }

    private void spawn(FrameInput input, float configuredWind) {
        int index = count++;
        initializeParticle(input, index);
        if (input.config.gravity == 0.0f) {
            x[index] = nextFloat() * Math.max(0.0f, input.screenWidth - width[index]);
            y[index] = nextFloat() * Math.max(0.0f, input.screenHeight - height[index]);
            velocityX[index] = (nextFloat() - 0.5f) * 8.0f;
            velocityY[index] = (nextFloat() - 0.5f) * 8.0f;
            return;
        }
        float sideSpawnShare = MAX_SIDE_SPAWN_SHARE
                * clamp(Math.abs(configuredWind) / WIND_FOR_MAX_SIDE_SPAWN, 0.0f, 1.0f);
        float gravityDirection = input.config.gravity < 0.0f ? -1.0f : 1.0f;
        if (configuredWind != 0.0f && nextFloat() < sideSpawnShare) {
            x[index] = configuredWind > 0.0f ? -width[index] : input.screenWidth + width[index];
            y[index] = nextFloat() * input.screenHeight;
            velocityX[index] = 0.0f;
            velocityY[index] = gravityDirection * (2.0f + nextFloat() * 8.0f);
        } else {
            x[index] = nextFloat() * input.screenWidth;
            float offset = nextFloat() * 24.0f;
            y[index] = gravityDirection < 0.0f ? input.screenHeight + offset : -height[index] - offset;
            velocityX[index] = (nextFloat() - 0.5f) * 8.0f;
            velocityY[index] = gravityDirection * (4.0f + nextFloat() * 10.0f);
        }
    }

    private void seedInitialScreenParticles(FrameInput input, int particleLimit) {
        int initialCount = Math.round(particleLimit * INITIAL_SCREEN_FILL);
        for (int i = 0; i < initialCount; i++) {
            int index = count++;
            initializeParticle(input, index);
            x[index] = nextFloat() * Math.max(0.0f, input.screenWidth - width[index]);
            y[index] = nextFloat() * Math.max(0.0f, input.screenHeight - height[index]);
            velocityX[index] = (nextFloat() - 0.5f) * 8.0f;
            velocityY[index] = input.config.gravity == 0.0f
                    ? (nextFloat() - 0.5f) * 8.0f
                    : (input.config.gravity < 0.0f ? -1.0f : 1.0f) * nextFloat() * 12.0f;
        }
    }

    private void initializeParticle(FrameInput input, int index) {
        textureIndex[index] = input.profile.chooseTexture(nextDouble());
        ParticleProfile.Texture texture = input.profile.textures().get(textureIndex[index]);
        width[index] = texture.frameWidth() * texture.scale() * input.coordinateScale;
        height[index] = texture.frameHeight() * texture.scale() * input.coordinateScale;
        lifetime[index] = 8.0f + nextFloat() * 6.0f;
        fadeInProgress[index] = input.config.gravity == 0.0f ? 0.0f : 1.0f;
        windReleaseTimer[index] = 0;
        swayAmplitude[index] = MIN_SWAY_AMPLITUDE
                + nextFloat() * (MAX_SWAY_AMPLITUDE - MIN_SWAY_AMPLITUDE);
        swayFactor[index] = 1.0f;
        swayFrequency[index] = MIN_SWAY_FREQUENCY
                + nextFloat() * (MAX_SWAY_FREQUENCY - MIN_SWAY_FREQUENCY);
        sweepResistance[index] = 0.0f;
        animationTimer[index] = nextFloat() * texture.frameTime() / 20.0;
        alternativeCooldownAge[index] = 0;
        alternativeAnimation[index] = false;
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
        width[to] = width[from];
        height[to] = height[from];
        lifetime[to] = lifetime[from];
        fadeInProgress[to] = fadeInProgress[from];
        windReleaseTimer[to] = windReleaseTimer[from];
        particleIds[to] = particleIds[from];
        supportIds[to] = supportIds[from];
        phase[to] = phase[from];
        sweepResistance[to] = sweepResistance[from];
        swayAmplitude[to] = swayAmplitude[from];
        swayFrequency[to] = swayFrequency[from];
        swayFactor[to] = swayFactor[from];
        animationTimer[to] = animationTimer[from];
        alternativeCooldownAge[to] = alternativeCooldownAge[from];
        alternativeAnimation[to] = alternativeAnimation[from];
        textureIndex[to] = textureIndex[from];
        frameIndex[to] = frameIndex[from];
        grounded[to] = grounded[from];
    }

    private static float windReleaseChance(float configuredWind) {
        float speed = Math.abs(configuredWind);
        if (speed <= 100.0f) return 0.0f;
        return Math.min(1.0f, 0.1f + 0.1f * (float) Math.floor((speed - 100.0f) / 50.0f));
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
                dragOffsetX = x[index] + width[index] * 0.5f - input.cursorX;
                dragOffsetY = y[index] + height[index] * 0.5f - input.cursorY;
                grounded[index] = false;
                addSound(input.profile.captureSounds(), SoundCueBuffer.CAPTURE, particleIds[index], 0.55f);
            }
        }
        previousMouseDown = input.mouseDown;
    }

    private void updatePendingSweep(FrameInput input, float dt) {
        if (!input.cursorOnScreen) {
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

    private void addSound(List<String> soundIds, byte kind, long particleId, float volume) {
        if (!soundIds.isEmpty()) {
            int soundIndex = (int) (nextFloat() * soundIds.size());
            output.sounds.add(soundIds.get(soundIndex), volume, 1.0f, 0.0f, kind, particleId);
        }
    }

    private void updateDimensions(int index, ParticleProfile.Texture texture, float coordinateScale) {
        float nextWidth = texture.frameWidth(alternativeAnimation[index]) * texture.scale() * coordinateScale;
        float nextHeight = texture.frameHeight(alternativeAnimation[index]) * texture.scale() * coordinateScale;
        // A different alternative sheet may have a different aspect ratio; keep its center stable.
        x[index] += (width[index] - nextWidth) * 0.5f;
        y[index] += (height[index] - nextHeight) * 0.5f;
        width[index] = nextWidth;
        height[index] = nextHeight;
    }

    private void advanceAnimation(int index, float dt, ParticleProfile profile) {
        ParticleProfile.Texture texture = profile.textures().get(textureIndex[index]);
        double frameDuration = texture.frameTime() / 20.0;
        if (texture.alternativeAnimation() == null) {
            double position = (frameIndex[index] * frameDuration + animationTimer[index] + dt)
                    % (texture.frameCount() * frameDuration);
            setAnimationPosition(index, texture, frameDuration, position);
            alternativeCooldownAge[index] += dt;
            return;
        }
        double remaining = dt;
        // Bound work for pathological profiles with extremely short frame durations.
        int transitions = 0;
        while (remaining > 0 && transitions < 64) {
            double untilEnd = (texture.frameCount() - frameIndex[index]) * frameDuration - animationTimer[index];
            if (remaining < untilEnd) {
                setAnimationPosition(index, texture, frameDuration,
                        frameIndex[index] * frameDuration + animationTimer[index] + remaining);
                alternativeCooldownAge[index] += remaining;
                return;
            }
            remaining -= Math.max(0, untilEnd);
            alternativeCooldownAge[index] += Math.max(0, untilEnd);
            frameIndex[index] = 0;
            animationTimer[index] = 0;
            if (alternativeAnimation[index]) {
                alternativeAnimation[index] = false;
                alternativeCooldownAge[index] = 0;
            } else if (texture.alternativeAnimation() != null && alternativeMatches(index, texture.alternativeAnimation())) {
                alternativeAnimation[index] = true;
            }
            transitions++;
        }
        if (remaining > 0) {
            alternativeAnimation[index] = false;
            alternativeCooldownAge[index] += remaining;
            setAnimationPosition(index, texture, frameDuration,
                    remaining % (texture.frameCount() * frameDuration));
        }
    }

    private void setAnimationPosition(int index, ParticleProfile.Texture texture, double frameDuration, double position) {
        frameIndex[index] = Math.min(texture.frameCount() - 1, (int) (position / frameDuration));
        animationTimer[index] = position - frameIndex[index] * frameDuration;
    }

    private boolean alternativeMatches(int index, ParticleProfile.AlternativeAnimation animation) {
        boolean both = animation.conditionType() == ParticleProfile.ConditionType.BOTH;
        for (int i = 0; i < animation.conditions().size(); i++) {
            ParticleProfile.AnimationCondition condition = animation.conditions().get(i);
            boolean matches = switch (condition.type()) {
                case CHANCE -> nextDouble() < condition.value();
                case COOLDOWN -> alternativeCooldownAge[index] >= condition.value();
            };
            if (both && !matches) return false;
            if (!both && matches) return true;
        }
        return both || animation.conditions().isEmpty();
    }

    private int findParticleToDrag(FrameInput input) {
        int closest = -1;
        float closestDistanceSquared = DRAG_CAPTURE_RADIUS * DRAG_CAPTURE_RADIUS;
        for (int i = 0; i < count; i++) {
            float dx = input.cursorX - (x[i] + width[i] * 0.5f);
            float dy = input.cursorY - (y[i] + height[i] * 0.5f);
            float distanceSquared = dx * dx + dy * dy;
            if (distanceSquared < closestDistanceSquared) {
                closest = i;
                closestDistanceSquared = distanceSquared;
            }
        }
        return closest;
    }

    private int findParticle(long id) {
        for (int i = 0; i < count; i++) {
            if (particleIds[i] == id) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasSupport(FrameInput input, int particle) {
        boolean invertedGravity = input.config.gravity < 0.0f;
        float edge = y[particle] + (invertedGravity ? 0.0f : height[particle]);
        for (int obstacle = 0; obstacle < input.obstacles.count(); obstacle++) {
            float obstacleY = input.obstacles.yAt(obstacle)
                    + (invertedGravity ? input.obstacles.heightAt(obstacle) : 0.0f);
            float obstacleLeft = input.obstacles.xAt(obstacle);
            float obstacleRight = obstacleLeft + input.obstacles.widthAt(obstacle);
            if (Math.abs(edge - obstacleY) <= 1.5f && x[particle] + width[particle] > obstacleLeft
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

    private double nextDouble() {
        randomState ^= randomState << 13;
        randomState ^= randomState >>> 7;
        randomState ^= randomState << 17;
        return (randomState >>> 11) * 0x1.0p-53;
    }

    private float nextFloat() {
        randomState ^= randomState << 13;
        randomState ^= randomState >>> 7;
        randomState ^= randomState << 17;
        return (randomState >>> 40) / (float) (1 << 24);
    }
}
