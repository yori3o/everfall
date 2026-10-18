package com.yori3o.everfall.mc.compat;

import com.yori3o.everfall.mc.ProfileResources;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public final class ProfileResourceReload {
    private static boolean installed;

    private ProfileResourceReload() {}

    public static void install() {
        if (installed) return;
        if (Minecraft.getInstance().getResourceManager() instanceof ReloadableResourceManager manager) {
            manager.registerReloadListener((ResourceManagerReloadListener) ProfileResources::reload);
            installed = true;
            // Client setup may happen after the initial resource reload has already started.
            ProfileResources.reload(manager);
        }
    }
}
