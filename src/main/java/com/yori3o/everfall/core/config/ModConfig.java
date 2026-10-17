package com.yori3o.everfall.core.config;

import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.autogen.AutoGen;
import dev.isxander.yacl3.config.v2.api.autogen.FloatSlider;
import dev.isxander.yacl3.config.v2.api.autogen.IntSlider;
import dev.isxander.yacl3.config.v2.api.autogen.Boolean;
import dev.isxander.yacl3.config.v2.api.autogen.EnumCycler;

public final class ModConfig {
    @AutoGen(category = "general")
    @Boolean
    @SerialEntry
    public boolean enabled = true;

    @AutoGen(category = "general")
    @Boolean
    @SerialEntry
    public boolean showInWorld = false;

    @AutoGen(category = "general")
    @EnumCycler
    @SerialEntry
    public ParticleProfileId profile = ParticleProfileId.AUTUMN;

    @AutoGen(category = "particles")
    @FloatSlider(min = 0.0f, max = 5.0f, step = 0.1f, format = "%.1fx")
    @SerialEntry
    public float density = 1.0f;

    @AutoGen(category = "particles")
    @IntSlider(min = 0, max = 400, step = 10)
    @SerialEntry
    public int maxParticles = 200;

    @AutoGen(category = "particles")
    @FloatSlider(min = -300.0f, max = 300.0f, step = 5.0f, format = "%.0f px/s")
    @SerialEntry
    public float wind = -40.0f;

    @AutoGen(category = "particles")
    @FloatSlider(min = 0.0f, max = 100.0f, step = 1.0f, format = "%.0f px/s^2")
    @SerialEntry
    public float gravity = 50.0f;
}
