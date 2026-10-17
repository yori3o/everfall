package com.yori3o.everfall.mc;

import com.yori3o.everfall.core.api.FrameInput;
import com.yori3o.everfall.core.api.FrameOutput;
import com.yori3o.everfall.core.engine.BasicParticleEngine;
import com.yori3o.everfall.core.engine.AdditionalParticleSystem;
import com.yori3o.everfall.core.config.PixelMode;
import com.yori3o.everfall.mc.compat.SpriteRenderer;
import com.yori3o.everfall.mc.compat.SoundCuePlayer;
import com.yori3o.everfall.mc.compat.WindowMode;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import java.nio.file.Path;
import java.time.LocalDateTime;

public final class ClientRuntime {
    private static final ClientInputCollector INPUT_COLLECTOR = new ClientInputCollector();
    private static final BasicParticleEngine ENGINE = new BasicParticleEngine();
    private static final SpriteRenderer RENDERER = new SpriteRenderer();
    private static AdditionalParticleSystem additionalParticles;
    private static LocalDateTime localTime;
    private static long nextTimeCheckNanos;
    private static Screen previousScreen;
    private static long previousFrameNanos;
    private static boolean mouseDown;
    private static int mouseButtonMask;
    private static boolean screenChanged;
    private static boolean initialized;
    private static int previousScreenWidth = -1;
    private static int previousScreenHeight = -1;
    private static int previousWindowWidth = -1;
    private static int previousWindowHeight = -1;
    private static boolean previousFullscreen;
    private static PixelMode previousPixelMode = PixelMode.DISPLAY;

    private ClientRuntime() {
    }

    public static void initialize(Path configDirectory) {
        if (initialized) {
            return;
        }
        ModConfigBootstrap.initialize(configDirectory);
        additionalParticles = new AdditionalParticleSystem(AdditionalProfileRegistry.profiles());
        resetParticles();
        previousFrameNanos = System.nanoTime();
        initialized = true;
    }

    public static void mouseButtonChanged(int button, int action) {
        if (button < 0 || button >= Integer.SIZE) {
            return;
        }
        int buttonMask = 1 << button;
        if (action == 1) {
            mouseButtonMask |= buttonMask;
        } else if (action == 0) {
            mouseButtonMask &= ~buttonMask;
        }
        mouseDown = mouseButtonMask != 0;
    }

    public static void beginFrame(Screen screen) {
        if (!initialized || screen == null) {
            return;
        }
        Window window = Minecraft.getInstance().getWindow();
        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();
        boolean fullscreen = WindowMode.isFullscreen();
        PixelMode pixelMode = ModConfigBootstrap.config().pixelMode;
        boolean pixelModeChanged = previousPixelMode != pixelMode;
        if (previousScreen != screen || pixelModeChanged) {
            screenChanged = true;
            if (pixelModeChanged) {
                resetParticles();
            } else {
                ENGINE.clearGrounded();
            }
            INPUT_COLLECTOR.resetScreenState();
            mouseButtonMask = 0;
            mouseDown = false;
            previousFrameNanos = System.nanoTime();
        } else if (previousScreenWidth >= 0 && (screen.width != previousScreenWidth
                || screen.height != previousScreenHeight || windowWidth != previousWindowWidth
                || windowHeight != previousWindowHeight || fullscreen != previousFullscreen)) {
            resetParticles();
            INPUT_COLLECTOR.resetScreenState();
            mouseButtonMask = 0;
            mouseDown = false;
            previousFrameNanos = System.nanoTime();
        }
        previousScreen = screen;
        previousScreenWidth = screen.width;
        previousScreenHeight = screen.height;
        previousWindowWidth = windowWidth;
        previousWindowHeight = windowHeight;
        previousFullscreen = fullscreen;
        previousPixelMode = pixelMode;
        INPUT_COLLECTOR.beginFrame(screen, pixelMode);
    }

    public static void captureWidget(AbstractWidget widget) {
        if (initialized) {
            INPUT_COLLECTOR.captureWidget(widget);
        }
    }

    public static void render(Screen screen, Object graphics, int mouseX, int mouseY) {
        if (!initialized || screen == null) {
            return;
        }
        long now = System.nanoTime();
        float deltaSeconds = Math.min((now - previousFrameNanos) / 1_000_000_000.0f, 0.1f);
        previousFrameNanos = now;
        FrameInput input = INPUT_COLLECTOR.collect(screen, mouseX, mouseY, deltaSeconds, mouseDown,
                ModConfigBootstrap.config());
        input.profile = ModConfigBootstrap.profile();
        input.screenChanged = screenChanged;
        screenChanged = false;
        INPUT_COLLECTOR.finishFrame();
        if (localTime == null || now >= nextTimeCheckNanos) {
            localTime = LocalDateTime.now();
            nextTimeCheckNanos = now + 1_000_000_000L;
        }
        additionalParticles.step(input, localTime);
        FrameOutput output = ENGINE.step(input);
        RENDERER.draw(graphics, output.sprites, input.profile, input.coordinateScale);
        for (int i = 0; i < additionalParticles.layers().size(); i++) {
            var layer = additionalParticles.layers().get(i);
            if (layer.active()) {
                RENDERER.draw(graphics, layer.sprites(), input.profile, input.coordinateScale);
            }
        }
        SoundCuePlayer.play(output.sounds, input.config.masterVolume / 100.0f);
    }

    private static void resetParticles() {
        ENGINE.reset(0x4556455246414c4cL);
        additionalParticles.reset(0x46495245464c4945L);
    }
}
