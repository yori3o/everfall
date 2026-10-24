package com.yori3o.everfall.mc.compat;

import net.minecraft.client.Minecraft;

public final class WindowMode {

    private WindowMode() {
    }

    public static boolean isFullscreen() {
        return Minecraft.getInstance().options.fullscreen().get();
    }
}
