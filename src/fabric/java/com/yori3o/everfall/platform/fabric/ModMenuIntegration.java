package com.yori3o.everfall.platform.fabric;

import com.terraformersmc.modmenu.api.ModMenuApi;

public final class ModMenuIntegration implements ModMenuApi {

    @Override
    public com.terraformersmc.modmenu.api.ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> com.yori3o.everfall.mc.ConfigScreenFactory.create(parent);
    }
}
