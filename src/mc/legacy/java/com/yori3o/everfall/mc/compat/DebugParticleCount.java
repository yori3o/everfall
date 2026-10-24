package com.yori3o.everfall.mc.compat;

import com.yori3o.everfall.mc.ClientRuntime;
import com.yori3o.everfall.mc.ModConfigBootstrap;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class DebugParticleCount {

    private static boolean menuVisible;
    private static boolean f3Down;

    private DebugParticleCount() {
    }

    public static void addLegacyOption(ConfigCategory.Builder category) {
        category.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("everfall.debug.countOption"))
                .description(OptionDescription.createBuilder().text(
                        Component.translatable("everfall.debug.countOption.desc")).build())
                .binding(false, () -> ModConfigBootstrap.config().showParticleCount,
                        value -> ModConfigBootstrap.config().showParticleCount = value)
                .controller(option -> BooleanControllerBuilder.create(option).onOffFormatter())
                .build());
    }

    public static String line() {
        int count = ClientRuntime.particleCount();

        if (count < 0 || !ModConfigBootstrap.config().showParticleCount) return null;

        return Component.translatable("everfall.debug.particles", count).getString();
    }

    public static void renderMenu(Object target) {
        var client = Minecraft.getInstance();

        if (client.level != null) {
            f3Down = false;
            menuVisible = false;
            return;
        }

        boolean pressed = GLFW.glfwGetKey(client.getWindow().getWindow(), GLFW.GLFW_KEY_F3) == GLFW.GLFW_PRESS;

        if (pressed && !f3Down) menuVisible = !menuVisible;
        f3Down = pressed;

        if (!menuVisible) return;

        String line = line();

        if (line == null) return;

        var graphics = (GuiGraphics) target;
        graphics.fill(1, 1, client.font.width(line) + 3, 10, 0x90505050);
        graphics.drawString(client.font, line, 2, 2, 0xffe0e0e0, false);
    }
}
