package com.github.netfallnetworks.mooofdoom.registry;

import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import com.github.netfallnetworks.mooofdoom.cow.utility.EnchantedMilkItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.item.Item;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MooOfDoom.MODID);

    public static final DeferredItem<Item> DOOM_APPLE = ITEMS.registerSimpleItem(
            "doom_apple",
            p -> p.stacksTo(16).rarity(net.minecraft.world.item.Rarity.EPIC)
    );

    public static final DeferredItem<EnchantedMilkItem> ENCHANTED_MILK = ITEMS.registerItem(
            "enchanted_milk",
            EnchantedMilkItem::new,
            new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)
    );
}
