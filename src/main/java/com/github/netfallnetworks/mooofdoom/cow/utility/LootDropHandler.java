package com.github.netfallnetworks.mooofdoom.cow.utility;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public class LootDropHandler {

    private static final List<ItemStack> LOOT_TABLE = List.of(
            new ItemStack(Items.DIAMOND),
            new ItemStack(Items.EMERALD, 3),
            new ItemStack(Items.NETHERITE_SCRAP),
            new ItemStack(Items.GOLD_INGOT, 5),
            new ItemStack(Items.IRON_INGOT, 8),
            new ItemStack(Items.LAPIS_LAZULI, 10)
    );

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Cow cow)) return;
        if (cow.level().isClientSide()) return;
        if (!OpCowManager.isOpCow(cow)) return;
        if (!ModConfig.RARE_DROPS_ENABLED.getAsBoolean()) return;

        // Random chance each tick based on configured interval
        if (cow.getRandom().nextInt(ModConfig.DROP_INTERVAL_TICKS.getAsInt()) != 0) return;

        // Pick random loot
        ItemStack loot = LOOT_TABLE.get(cow.getRandom().nextInt(LOOT_TABLE.size())).copy();
        ItemEntity itemEntity = new ItemEntity(cow.level(),
                cow.getX(), cow.getY() + 0.5, cow.getZ(), loot);
        cow.level().addFreshEntity(itemEntity);

        // Sparkle effect
        ServerLevel serverLevel = (ServerLevel) cow.level();
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                cow.getX(), cow.getY() + 1, cow.getZ(),
                10, 0.5, 0.5, 0.5, 0.0);
        cow.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5F, 1.0F);
    }
}
