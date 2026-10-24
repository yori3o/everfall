package com.yori3o.everfall.mc;

import java.nio.file.Path;

import net.minecraft.server.packs.resources.ResourceManager;

/** Called on the client apply executor after resource preparation. */
public final class ProfileResources {

    private static Path configDirectory;

    private ProfileResources() {
    }

    public static void initialize(Path directory) {
        configDirectory = directory;
    }

    public static void reload(ResourceManager resources) {
        if (configDirectory == null) return;

        ParticleProfileRegistry.load(configDirectory, resources);
        AdditionalProfileRegistry.load(configDirectory, resources);
        ProfileSoundEvents.reload();
        ClientRuntime.profilesReloaded();
    }
}
