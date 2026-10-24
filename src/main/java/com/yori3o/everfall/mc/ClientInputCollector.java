package com.yori3o.everfall.mc;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.yori3o.everfall.core.api.FrameInput;
import com.yori3o.everfall.core.config.ModConfig;
import com.yori3o.everfall.core.config.PixelMode;
import com.yori3o.everfall.mc.compat.ScissorAccess;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractStringWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

public final class ClientInputCollector {

    private final FrameInput input = new FrameInput();
    private final IdentityHashMap<AbstractWidget, Integer> widgetIds = new IdentityHashMap<>();
    private final Set<Integer> widgetsSeenThisFrame = new HashSet<>();
    private float previousCursorX;
    private float previousCursorY;
    private float coordinateScale = 1.0f;
    private boolean hasPreviousCursor;
    private boolean titleScreen;
    private int nextWidgetId = 1;

    public void beginFrame(Screen screen, PixelMode pixelMode) {
        input.obstacles.clear();
        titleScreen = screen instanceof TitleScreen;
        widgetsSeenThisFrame.clear();
        coordinateScale = 1.0f;

        if (pixelMode != PixelMode.GUI && screen.width > 0) {
            int displayWidth = Minecraft.getInstance().getWindow().getWidth();

            if (displayWidth > 0) {
                coordinateScale = displayWidth / (float) screen.width;
            }
        }
    }

    public void captureLogo(int screenWidth, int top, float alpha, Object graphics) {
        if (titleScreen && alpha >= 1.0f / 255.0f && widgetsSeenThisFrame.add(-1)) {
            addClippedObstacle(-1, screenWidth / 2 - 128, top, 256, 44, graphics);
        }
    }

    public void captureWidget(AbstractWidget widget, Object graphics) {
        if (!(widget instanceof AbstractContainerWidget) && !(widget instanceof AbstractStringWidget)
                && widget.visible && widget.getWidth() > 0 && widget.getHeight() > 0) {
            Integer widgetId = widgetIds.get(widget);

            if (widgetId == null) {
                widgetId = nextWidgetId++;
                widgetIds.put(widget, widgetId);
            }

            if (!widgetsSeenThisFrame.add(widgetId)) {
                return;
            }

            addClippedObstacle(widgetId, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), graphics);
        }
    }

    private void addClippedObstacle(int id, int left, int top, int width, int height, Object graphics) {
        int right = left + width;
        int bottom = top + height;
        var scissor = ((ScissorAccess) graphics).everfall$scissor();

        if (scissor != null) {
            left = Math.max(left, scissor.left());
            top = Math.max(top, scissor.top());
            right = Math.min(right, scissor.right());
            bottom = Math.min(bottom, scissor.bottom());
        }

        if (right > left && bottom > top) {
            input.obstacles.add(id, left * coordinateScale, top * coordinateScale,
                    (right - left) * coordinateScale, (bottom - top) * coordinateScale);
        }
    }

    public void finishFrame() {
        Iterator<Map.Entry<AbstractWidget, Integer>> widgets = widgetIds.entrySet().iterator();

        while (widgets.hasNext()) {
            if (!widgetsSeenThisFrame.contains(widgets.next().getValue())) {
                widgets.remove();
            }
        }
    }

    public void resetScreenState() {
        widgetIds.clear();
        widgetsSeenThisFrame.clear();
        nextWidgetId = 1;
        resetCursorTracking();
    }

    public void resetCursorTracking() {
        hasPreviousCursor = false;
        input.cursorPathValid = false;
        input.cursorVelocityX = 0.0f;
        input.cursorVelocityY = 0.0f;
    }

    public FrameInput collect(Screen screen, int mouseX, int mouseY, float deltaSeconds,
            boolean mouseDown, ModConfig config) {
        var client = Minecraft.getInstance();
        var window = client.getWindow();
        // Screen render arguments are rounded GUI coordinates. Preserve subpixel mouse motion instead.
        double guiMouseX = client.mouseHandler.xpos() * screen.width / Math.max(1, window.getScreenWidth());
        double guiMouseY = client.mouseHandler.ypos() * screen.height / Math.max(1, window.getScreenHeight());
        input.coordinateScale = coordinateScale;
        input.interactionScale = coordinateScale * screen.width / Math.max(1.0f, window.getWidth());
        input.screenWidth = Math.round(screen.width * coordinateScale);
        input.screenHeight = Math.round(screen.height * coordinateScale);
        input.deltaSeconds = deltaSeconds;
        input.cursorX = (float) (guiMouseX * coordinateScale);
        input.cursorY = (float) (guiMouseY * coordinateScale);
        input.cursorOnScreen = guiMouseX >= 0 && guiMouseY >= 0 && guiMouseX < screen.width && guiMouseY < screen.height;
        input.cursorPreviousX = previousCursorX;
        input.cursorPreviousY = previousCursorY;
        input.cursorPathValid = hasPreviousCursor && deltaSeconds > 0.0f;

        if (hasPreviousCursor && deltaSeconds > 0.0f) {
            float maxCursorSpeed = 2000.0f * input.interactionScale;
            input.cursorVelocityX = clamp((input.cursorX - previousCursorX) / deltaSeconds, -maxCursorSpeed, maxCursorSpeed);
            input.cursorVelocityY = clamp((input.cursorY - previousCursorY) / deltaSeconds, -maxCursorSpeed, maxCursorSpeed);
        } else {
            input.cursorVelocityX = 0.0f;
            input.cursorVelocityY = 0.0f;
        }

        previousCursorX = input.cursorX;
        previousCursorY = input.cursorY;
        hasPreviousCursor = true;
        input.mouseDown = mouseDown;
        input.inWorld = Minecraft.getInstance().level != null;
        input.config = config;
        return input;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
