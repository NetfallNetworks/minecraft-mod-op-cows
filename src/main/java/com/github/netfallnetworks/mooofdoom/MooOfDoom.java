package com.github.netfallnetworks.mooofdoom;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import com.github.netfallnetworks.mooofdoom.cow.DoomAppleUseHandler;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import com.github.netfallnetworks.mooofdoom.registry.ModEntityTypes;
import com.github.netfallnetworks.mooofdoom.registry.ModItems;
import net.neoforged.neoforge.common.NeoForge;

@Mod(MooOfDoom.MODID)
public class MooOfDoom {
    public static final String MODID = "mooofdoom";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MooOfDoom(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Moo of Doom loading...");

        // Register deferred registers
        ModItems.ITEMS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);

        // Register mod configuration
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, ModConfig.SPEC);

        // Register game event handlers
        NeoForge.EVENT_BUS.register(OpCowManager.class);
        NeoForge.EVENT_BUS.register(DoomAppleUseHandler.class);
    }
}
