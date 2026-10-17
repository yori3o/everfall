package com.yori3o.everfall.mc.compat;

import net.minecraft.client.Minecraft;

public final class ClientLanguage {
    private ClientLanguage() {}

    public static String selected() {
        var minecraft = Minecraft.getInstance();
        return minecraft == null || minecraft.options == null || minecraft.options.languageCode == null
                ? "" : minecraft.options.languageCode;
    }
}
