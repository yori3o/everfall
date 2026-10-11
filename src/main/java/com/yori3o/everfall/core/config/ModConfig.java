package com.yori3o.everfall.core.config;

import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.autogen.AutoGen;
import dev.isxander.yacl3.config.v2.api.autogen.FloatSlider;
import dev.isxander.yacl3.config.v2.api.autogen.IntSlider;
import dev.isxander.yacl3.config.v2.api.autogen.Boolean;

public final class ModConfig {
    @AutoGen(category = "general")
    @Boolean
    @SerialEntry
    public boolean enabled = true;

    @AutoGen(category = "general")
    @Boolean
    @SerialEntry
    public boolean showInWorld = false;

    @AutoGen(category = "particles")
    @FloatSlider(min = 0.0f, max = 2.0f, step = 0.05f)
    @SerialEntry
    public float density = 1.0f;

    @AutoGen(category = "particles")
    @IntSlider(min = 0, max = 200, step = 1)
    @SerialEntry
    public int maxParticles = 120;

    @AutoGen(category = "particles")
    @FloatSlider(min = -40.0f, max = 40.0f, step = 1.0f)
    @SerialEntry
    public float wind = 0.0f;

    @AutoGen(category = "particles")
    @FloatSlider(min = 0.0f, max = 40.0f, step = 0.5f)
    @SerialEntry
    public float gravity = 8.0f;
}
