package com.github.netfallnetworks.mooofdoom.cow.utility;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import com.github.netfallnetworks.mooofdoom.rarity.RarityTier;
import com.github.netfallnetworks.mooofdoom.rarity.TieredRandom;
import com.github.netfallnetworks.mooofdoom.registry.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class MilkingHandler {

    @SubscribeEvent
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!ModConfig.ENCHANTED_MILK_ENABLED.getAsBoolean()) return;
        if (!(event.getTarget() instanceof Cow cow)) return;
        if (!OpCowManager.isOpCow(cow)) return;
        if (!event.getItemStack().is(Items.BUCKET)) return;

        // Replace normal milking with tiered buff bucket
        event.setCanceled(true);

        Item bucketItem = rollBuffBucket(cow);

        if (!event.getEntity().getAbilities().instabuild) {
            event.getItemStack().shrink(1);
        }
        event.getEntity().getInventory().add(new ItemStack(bucketItem));
        cow.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
    }

    private static Item rollBuffBucket(Cow cow) {
        RarityTier tier = TieredRandom.roll(cow.getRandom());
        return switch (tier) {
            case COMMON -> ModItems.BUCKET_OF_SPEED.get();
            case UNCOMMON -> ModItems.BUCKET_OF_REGENERATION.get();
            case RARE -> ModItems.BUCKET_OF_STRENGTH.get();
            case LEGENDARY -> ModItems.BUCKET_OF_FIRE_RESISTANCE.get();
            case MYTHIC -> ModItems.BUCKET_OF_LUCK.get();
        };
    }
}
