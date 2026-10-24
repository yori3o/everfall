package com.yori3o.everfall.mc;

import java.util.ArrayList;
import java.util.List;

import com.yori3o.everfall.mc.compat.DebugParticleCount;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionEventListener;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.CyclingListControllerBuilder;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ConfigScreenFactory {

    private ConfigScreenFactory() {
    }

    public static Screen create(Screen parent) {
        ModConfigBootstrap.profile();
        YetAnotherConfigLib generated = ModConfigBootstrap.handler().generateGui();
        Option<Boolean> automatic = findOption(generated, "autoSeason", Boolean.class);
        List<String> choices = new ArrayList<>(ParticleProfileRegistry.ids());
        String current = ModConfigBootstrap.config().profile;

        if (!choices.contains(current)) choices.add(current);

        String detected = SeasonalProfileSelector.automaticProfile();

        if (!choices.contains(detected)) choices.add(detected);

        Option<String> profile = Option.<String>createBuilder()
                .name(Component.translatable("yacl3.config.everfall:client_config.profile"))
                .description(OptionDescription.createBuilder().text(Component.translatable(
                        "yacl3.config.everfall:client_config.profile.desc")).build())
                .binding(choices.contains("autumn") ? "autumn" : choices.getFirst(),
                        () -> ModConfigBootstrap.config().profile,
                        value -> ModConfigBootstrap.config().profile = value)
                .controller(option -> CyclingListControllerBuilder.create(option).values(choices)
                        .valueFormatter(ConfigScreenFactory::profileName))
                .build();

        Option<Integer> count = findOption(generated, "maxParticles", Integer.class);
        Option<Float> wind = findOption(generated, "wind", Float.class);
        Option<Float> gravity = findOption(generated, "gravity", Float.class);
        Option<Boolean> background = findOption(generated, "backgroundParticles", Boolean.class);
        Option<Boolean> interaction = findOption(generated, "interactionEnabled", Boolean.class);

        Runnable applyDefaults = () -> {
            var selected = ParticleProfileRegistry.get(profile.pendingValue());

            if (selected.textures().isEmpty()) return;

            var defaults = selected.defaults();
            count.requestSet(defaults.maxParticles());
            wind.requestSet(defaults.wind());
            gravity.requestSet(defaults.gravity());

            if (defaults.backgroundParticles() != null) background.requestSet(defaults.backgroundParticles());

            if (defaults.interactionEnabled() != null) interaction.requestSet(
                    !background.pendingValue() && defaults.interactionEnabled());

            updateInteractionOption(interaction, background.pendingValue());
        };

        profile.addEventListener((option, event) -> {
            if (event == OptionEventListener.Event.STATE_CHANGE) applyDefaults.run();
        });

        automatic.addEventListener((option, event) -> {
            if (event != OptionEventListener.Event.STATE_CHANGE) return;

            String previous = profile.pendingValue();
            updateProfileOption(profile, option.pendingValue());

            // Enabling auto also restores defaults when the detected profile already matches.
            if (option.pendingValue() && previous.equals(profile.pendingValue())) applyDefaults.run();
        });

        updateProfileOption(profile, automatic.pendingValue());
        background.addListener((option, enabled) -> updateInteractionOption(interaction, enabled));
        interaction.addListener((option, enabled) -> {
            if (enabled && background.pendingValue()) interaction.requestSet(false);
        });

        updateInteractionOption(interaction, background.pendingValue());

        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("everfall.config.title"))
                .categories(withProfileOption(generated, profile))
                .category(featuresCategory())
                .save(ModConfigBootstrap::save)
                .build()
                .generateScreen(parent);
    }

    private static Component profileName(String id) {
        return Component.translatableWithFallback("yacl3.config.enum.ParticleProfileId." + id, id);
    }

    private static List<ConfigCategory> withProfileOption(YetAnotherConfigLib generated, Option<String> profile) {
        List<ConfigCategory> categories = new ArrayList<>(generated.categories());
        var original = categories.get(0);
        var builder = ConfigCategory.createBuilder().name(original.name()).tooltip(original.tooltip());

        for (var group : original.groups()) {
            if (group.isRoot()) builder.options(group.options());
            else builder.group(group);
        }

        builder.option(profile);
        DebugParticleCount.addLegacyOption(builder);
        categories.set(0, builder.build());
        return categories;
    }

    private static ConfigCategory featuresCategory() {
        var category = ConfigCategory.createBuilder()
                .name(Component.translatable("everfall.config.features"));

        for (var feature : AdditionalProfileRegistry.profiles()) {
            boolean builtIn = feature.id().equals("fireflies");
            Component name = Component.translatableWithFallback("everfall.feature." + feature.id(), feature.id());
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

    private static void updateProfileOption(Option<String> profile, boolean automatic) {
        if (automatic) profile.requestSet(SeasonalProfileSelector.automaticProfile());
        profile.setAvailable(!automatic);
    }

    private static void updateInteractionOption(Option<Boolean> interaction, boolean background) {
        if (background) interaction.requestSet(false);
        interaction.setAvailable(!background);
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
