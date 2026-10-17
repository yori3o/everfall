package com.yori3o.everfall.mc;

import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import java.util.ArrayList;
import com.yori3o.everfall.core.config.ParticleProfileId;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ConfigScreenFactory {
    private ConfigScreenFactory() {
    }

    public static Screen create(Screen parent) {
        ModConfigBootstrap.profile();
        YetAnotherConfigLib generated = ModConfigBootstrap.handler().generateGui();
        Option<Boolean> automatic = findOption(generated, "autoSeason", Boolean.class);
        Option<ParticleProfileId> profile = findOption(generated, "profile", ParticleProfileId.class);
        automatic.addListener((option, enabled) -> updateProfileOption(profile, enabled));
        updateProfileOption(profile, automatic.pendingValue());
        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("everfall.config.title"))
                .categories(generated.categories())
                .category(additionalProfiles())
                .save(ModConfigBootstrap::save)
                .build()
                .generateScreen(parent);
    }

    private static ConfigCategory additionalProfiles() {
        var category = ConfigCategory.createBuilder()
                .name(Component.translatable("everfall.config.features"));
        for (var feature : AdditionalProfileRegistry.profiles()) {
            boolean builtIn = feature.id().equals("fireflies");
            Component name = builtIn ? Component.translatable("everfall.feature.fireflies")
                    : Component.literal(feature.name());
            Component description = builtIn ? Component.translatable("everfall.feature.fireflies.desc")
                    : Component.translatable("everfall.feature.custom.desc");
            category.option(Option.<Boolean>createBuilder()
                    .name(name)
                    .description(OptionDescription.createBuilder().text(description).build())
                    .binding(builtIn, () -> ModConfigBootstrap.config().additionalProfiles.contains(feature.id()),
                            enabled -> {
                                var selected = new ArrayList<>(ModConfigBootstrap.config().additionalProfiles);
                                selected.removeIf(feature.id()::equals);
                                if (enabled) selected.add(feature.id());
                                ModConfigBootstrap.config().additionalProfiles = selected;
                            })
                    .controller(option -> BooleanControllerBuilder.create(option).onOffFormatter().coloured(true))
                    .build());
        }
        return category.build();
    }

    private static void updateProfileOption(Option<ParticleProfileId> profile, boolean automatic) {
        if (automatic) profile.requestSet(SeasonalProfileSelector.automaticProfile());
        profile.setAvailable(!automatic);
    }

    @SuppressWarnings("unchecked")
    private static <T> Option<T> findOption(YetAnotherConfigLib gui, String field, Class<T> type) {
        Component name = Component.translatable("yacl3.config.everfall:client_config." + field);
        for (var category : gui.categories()) {
            for (var group : category.groups()) {
                for (Option<?> option : group.options()) {
                    if (option.name().equals(name) && type.isInstance(option.pendingValue())) {
                        return (Option<T>) option;
                    }
                }
            }
        }
        throw new IllegalStateException("Missing generated Everfall option: " + field);
    }
}
