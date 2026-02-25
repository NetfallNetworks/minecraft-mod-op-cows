package com.github.netfallnetworks.mooofdoom.registry;

import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import com.github.netfallnetworks.mooofdoom.cow.utility.BuffBucketItem;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MooOfDoom.MODID);

    public static final DeferredItem<Item> DOOM_APPLE = ITEMS.registerSimpleItem(
            "doom_apple",
            p -> p.stacksTo(16).rarity(Rarity.EPIC)
    );

    // Buff Buckets — tiered rarity (Common → Mythic)
    public static final DeferredItem<BuffBucketItem> BUCKET_OF_SPEED = ITEMS.registerItem(
            "bucket_of_speed",
            p -> new BuffBucketItem(p, MobEffects.SPEED, 1200, 1),
            new Item.Properties().stacksTo(1).rarity(Rarity.COMMON)
    );

    public static final DeferredItem<BuffBucketItem> BUCKET_OF_REGENERATION = ITEMS.registerItem(
            "bucket_of_regeneration",
            p -> new BuffBucketItem(p, MobEffects.REGENERATION, 600, 1),
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
    );

    public static final DeferredItem<BuffBucketItem> BUCKET_OF_STRENGTH = ITEMS.registerItem(
            "bucket_of_strength",
            p -> new BuffBucketItem(p, MobEffects.STRENGTH, 1200, 1),
            new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
    );

    public static final DeferredItem<BuffBucketItem> BUCKET_OF_FIRE_RESISTANCE = ITEMS.registerItem(
            "bucket_of_fire_resistance",
            p -> new BuffBucketItem(p, MobEffects.FIRE_RESISTANCE, 1200, 0),
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
    );

    public static final DeferredItem<BuffBucketItem> BUCKET_OF_LUCK = ITEMS.registerItem(
            "bucket_of_luck",
            p -> new BuffBucketItem(p, MobEffects.LUCK, 1200, 1),
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
    );
}
