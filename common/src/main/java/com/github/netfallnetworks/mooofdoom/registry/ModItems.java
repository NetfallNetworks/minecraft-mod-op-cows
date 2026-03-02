package com.github.netfallnetworks.mooofdoom.registry;

import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import com.github.netfallnetworks.mooofdoom.cow.DoomAppleItem;
import com.github.netfallnetworks.mooofdoom.cow.utility.BuffBucketItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumable;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MooOfDoom.MODID);

    public static final DeferredItem<DoomAppleItem> DOOM_APPLE = ITEMS.register(
            "doom_apple",
            id -> new DoomAppleItem(
                    new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, id))
                            .stacksTo(16).rarity(Rarity.EPIC)
                            .food(new FoodProperties(4, 9.6f, true),
                                    Consumable.builder().consumeSeconds(1.6F).build())
            )
    );

    // Buff Buckets — tiered rarity (Common → Mythic)
    public static final DeferredItem<BuffBucketItem> BUCKET_OF_SPEED = ITEMS.register(
            "bucket_of_speed",
            id -> new BuffBucketItem(
                    new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, id))
                            .stacksTo(1).rarity(Rarity.COMMON),
                    MobEffects.SPEED, 1200, 1
            )
    );

    public static final DeferredItem<BuffBucketItem> BUCKET_OF_REGENERATION = ITEMS.register(
            "bucket_of_regeneration",
            id -> new BuffBucketItem(
                    new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, id))
                            .stacksTo(1).rarity(Rarity.UNCOMMON),
                    MobEffects.REGENERATION, 600, 1
            )
    );

    public static final DeferredItem<BuffBucketItem> BUCKET_OF_STRENGTH = ITEMS.register(
            "bucket_of_strength",
            id -> new BuffBucketItem(
                    new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, id))
                            .stacksTo(1).rarity(Rarity.RARE),
                    MobEffects.STRENGTH, 1200, 1
            )
    );

    public static final DeferredItem<BuffBucketItem> BUCKET_OF_FIRE_RESISTANCE = ITEMS.register(
            "bucket_of_fire_resistance",
            id -> new BuffBucketItem(
                    new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, id))
                            .stacksTo(1).rarity(Rarity.EPIC),
                    MobEffects.FIRE_RESISTANCE, 1200, 0
            )
    );

    public static final DeferredItem<BuffBucketItem> BUCKET_OF_LUCK = ITEMS.register(
            "bucket_of_luck",
            id -> new BuffBucketItem(
                    new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, id))
                            .stacksTo(1).rarity(Rarity.EPIC),
                    MobEffects.LUCK, 1200, 1
            )
    );
}
