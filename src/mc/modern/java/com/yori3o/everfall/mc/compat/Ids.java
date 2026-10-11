package com.yori3o.everfall.mc.compat;

import net.minecraft.resources.Identifier;

public final class Ids {
    private Ids() {
    }

    public static Identifier create(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
