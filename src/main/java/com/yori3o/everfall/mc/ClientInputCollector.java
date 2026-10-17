package com.yori3o.everfall.mc;

import com.yori3o.everfall.core.api.FrameInput;
import com.yori3o.everfall.core.config.ModConfig;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractStringWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.Minecraft;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class ClientInputCollector {
    private final FrameInput input = new FrameInput();
    private final IdentityHashMap<AbstractWidget, Integer> widgetIds = new IdentityHashMap<>();
    private final Set<Integer> widgetsSeenThisFrame = new HashSet<>();
    private float previousCursorX;
    private float previousCursorY;
    private float coordinateScale = 1.0f;
    private boolean hasPreviousCursor;
    private int nextWidgetId = 1;

    public void beginFrame(Screen screen) {
        input.obstacles.clear();
        widgetsSeenThisFrame.clear();
        if (screen.width > 0) {
            int displayWidth = Minecraft.getInstance().getWindow().getWidth();
            if (displayWidth > 0) {
                coordinateScale = displayWidth / (float) screen.width;
            }
        }
        if (screen instanceof TitleScreen) {
            input.obstacles.add(-1, (screen.width / 2.0f - 128.0f) * coordinateScale,
                    30.0f * coordinateScale, 256.0f * coordinateScale, 44.0f * coordinateScale);
        }
    }

    public void captureWidget(AbstractWidget widget) {
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
            input.obstacles.add(widgetId, widget.getX() * coordinateScale, widget.getY() * coordinateScale,
                    widget.getWidth() * coordinateScale, widget.getHeight() * coordinateScale);
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
        input.coordinateScale = coordinateScale;
        input.screenWidth = Math.round(screen.width * coordinateScale);
        input.screenHeight = Math.round(screen.height * coordinateScale);
        input.deltaSeconds = deltaSeconds;
        input.cursorX = mouseX * coordinateScale;
        input.cursorY = mouseY * coordinateScale;
        input.cursorOnScreen = mouseX >= 0 && mouseY >= 0 && mouseX < screen.width && mouseY < screen.height;
        input.cursorPreviousX = previousCursorX;
        input.cursorPreviousY = previousCursorY;
        input.cursorPathValid = hasPreviousCursor && deltaSeconds > 0.0f;
        if (hasPreviousCursor && deltaSeconds > 0.0f) {
            input.cursorVelocityX = clamp((input.cursorX - previousCursorX) / deltaSeconds, -2000.0f, 2000.0f);
            input.cursorVelocityY = clamp((input.cursorY - previousCursorY) / deltaSeconds, -2000.0f, 2000.0f);
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
