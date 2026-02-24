package com.github.netfallnetworks.mooofdoom.registry;

import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.item.Item;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MooOfDoom.MODID);

    public static final DeferredItem<Item> DOOM_APPLE = ITEMS.registerSimpleItem(
            "doom_apple",
            p -> p.stacksTo(16).rarity(net.minecraft.world.item.Rarity.EPIC)
    );

    // Enchanted Milk will be registered in a later task when the custom item class is ready
}
