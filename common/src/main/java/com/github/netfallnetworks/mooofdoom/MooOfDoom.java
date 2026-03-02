package com.github.netfallnetworks.mooofdoom;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class MooOfDoom {
    public static final String MODID = "mooofdoom";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        LOGGER.info("Moo of Doom loading...");
    }
}
