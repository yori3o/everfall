package com.yori3o.everfall.mc.compat;

import net.minecraft.resources.ResourceLocation;

public final class Ids {
    private Ids() {
    }

    public static ResourceLocation create(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static ResourceLocation parse(String value) {
        return ResourceLocation.parse(value);
    }
}
