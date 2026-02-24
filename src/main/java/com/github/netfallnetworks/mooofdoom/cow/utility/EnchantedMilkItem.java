package com.github.netfallnetworks.mooofdoom.cow.utility;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class EnchantedMilkItem extends Item {

    private static final List<MobEffectInstance> POSSIBLE_EFFECTS = List.of(
            new MobEffectInstance(MobEffects.STRENGTH, 1200, 1),        // Strength II for 60s
            new MobEffectInstance(MobEffects.REGENERATION, 600, 1),     // Regen II for 30s
            new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0), // Fire Res for 60s
            new MobEffectInstance(MobEffects.SPEED, 1200, 1)            // Speed II for 60s
    );

    public EnchantedMilkItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player player) {
            // Clear negative effects like regular milk
            player.removeAllEffects();

            // Grant 1-2 random positive effects
            int count = 1 + level.getRandom().nextInt(2);
            List<MobEffectInstance> shuffled = new ArrayList<>(POSSIBLE_EFFECTS);
            Collections.shuffle(shuffled, new Random(level.getRandom().nextLong()));
            for (int i = 0; i < Math.min(count, shuffled.size()); i++) {
                MobEffectInstance template = shuffled.get(i);
                player.addEffect(new MobEffectInstance(template));
            }

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                player.getInventory().add(new ItemStack(Items.BUCKET));
            }
        }
        return stack;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }
}
