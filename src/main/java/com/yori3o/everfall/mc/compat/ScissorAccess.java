package com.yori3o.everfall.mc.compat;

import net.minecraft.client.gui.navigation.ScreenRectangle;

/** The intersection of active GUI scissor rectangles, or null when clipping is disabled. */
public interface ScissorAccess {

    ScreenRectangle everfall$scissor();
}
