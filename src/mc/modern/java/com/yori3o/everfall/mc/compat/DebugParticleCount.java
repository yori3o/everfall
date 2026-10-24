package com.yori3o.everfall.mc.compat;

import com.yori3o.everfall.mc.ClientRuntime;

import dev.isxander.yacl3.api.ConfigCategory;

import net.minecraft.client.gui.components.debug.DebugEntryCategory;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

public final class DebugParticleCount implements DebugScreenEntry {

    private static final DebugEntryCategory CATEGORY = new DebugEntryCategory(Component.literal("Everfall"), 2.0f);

    @Override
    public void display(DebugScreenDisplayer output, Level level, LevelChunk clientChunk, LevelChunk serverChunk) {
        int count = ClientRuntime.particleCount();

        if (count >= 0) output.addLine(Component.translatable("everfall.debug.particles", count).getString());
    }

    @Override
    public boolean isAllowed(boolean reducedInfo) {
        return true;
    }

    @Override
    public DebugEntryCategory category() {
        return CATEGORY;
    }

    public static void addLegacyOption(ConfigCategory.Builder category) {}
    public static void renderMenu(Object graphics) {}
}
