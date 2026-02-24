package com.github.netfallnetworks.mooofdoom;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(MooOfDoom.MODID)
public class MooOfDoom {
    public static final String MODID = "mooofdoom";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MooOfDoom(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Moo of Doom loading...");
    }
}
