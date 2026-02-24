package com.github.netfallnetworks.mooofdoom.cow;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public class OpCowManager {

    public static final String OP_TAG = "MooOfDoom";

    public static boolean isOpCow(Cow cow) {
        return cow.getTags().contains(OP_TAG);
    }

    public static void makeOpCow(Cow cow) {
        cow.addTag(OP_TAG);
        boostAttributes(cow);
        cow.setGlowingTag(true);
    }

    private static void boostAttributes(Cow cow) {
        AttributeInstance healthAttr = cow.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(ModConfig.COW_HEALTH.getAsInt());
            cow.setHealth(cow.getMaxHealth());
        }

        AttributeInstance speedAttr = cow.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(0.35);
        }

        AttributeInstance kbAttr = cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kbAttr != null) {
            kbAttr.setBaseValue(0.8);
        }
    }

    // This will be called by later tasks to add combat goals
    public static void addCombatGoals(Cow cow) {
        // Goals will be added in Tasks 6, 7, 8
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Cow cow)) return;
        if (event.getLevel().isClientSide()) return;

        // If already OP, re-apply attributes (they reset on load)
        if (isOpCow(cow)) {
            boostAttributes(cow);
            cow.setGlowingTag(true);
            addCombatGoals(cow);
            return;
        }

        // Determine if this cow should become OP
        ModConfig.ActivationMode mode = ModConfig.ACTIVATION_MODE.get();
        switch (mode) {
            case ALL_COWS -> makeOpCow(cow);
            case RARE_SPAWN -> {
                if (cow.getRandom().nextDouble() < ModConfig.RARE_SPAWN_CHANCE.get()) {
                    makeOpCow(cow);
                }
            }
            case ITEM_ACTIVATED -> {
                // Handled by Doom Apple use, not here
            }
        }
    }
}
