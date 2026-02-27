package com.github.netfallnetworks.mooofdoom.neoforge;

import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import com.github.netfallnetworks.mooofdoom.registry.ModCriteriaTriggers;
import com.github.netfallnetworks.mooofdoom.registry.ModEffects;
import com.github.netfallnetworks.mooofdoom.registry.ModEntityTypes;
import com.github.netfallnetworks.mooofdoom.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

@Mod(MooOfDoom.MODID)
public class NeoForgeMooOfDoom {

    public NeoForgeMooOfDoom(IEventBus modEventBus, ModContainer modContainer) {
        MooOfDoom.init();

        // Register deferred registers on the mod event bus
        ModItems.ITEMS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModEffects.MOB_EFFECTS.register(modEventBus);
        ModCriteriaTriggers.TRIGGERS.register(modEventBus);

        // Register mod configuration and sync on load/reload
        modContainer.registerConfig(ModConfig.Type.COMMON, NeoForgeConfig.SPEC);
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReload);
    }

    private void onConfigLoad(ModConfigEvent.Loading event) {
        NeoForgeConfig.syncToCommon();
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        NeoForgeConfig.syncToCommon();
    }
}
