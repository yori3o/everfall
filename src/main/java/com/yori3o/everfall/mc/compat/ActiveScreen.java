package com.yori3o.everfall.mc.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class ActiveScreen {

    private ActiveScreen() {
    }

    public static Screen current() {
        //? if >=26.2 {
        return Minecraft.getInstance().gui.screen();
        //? } else {
        /*return Minecraft.getInstance().screen;
        *///? }
    }
}
