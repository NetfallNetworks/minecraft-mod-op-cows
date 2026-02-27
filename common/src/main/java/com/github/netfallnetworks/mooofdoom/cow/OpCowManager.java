package com.github.netfallnetworks.mooofdoom.cow;

import com.github.netfallnetworks.mooofdoom.config.ModConfigValues;
import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import com.github.netfallnetworks.mooofdoom.cow.combat.ChargeGoal;
import com.github.netfallnetworks.mooofdoom.cow.combat.HostileTargetGoal;
import com.github.netfallnetworks.mooofdoom.cow.combat.MilkProjectileGoal;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.Level;
import net.minecraft.tags.DamageTypeTags;

public class OpCowManager {

    public static final String OP_TAG = "MooOfDoom";
    private static boolean loggedActivationMode = false;

    public static boolean isOpCow(Cow cow) {
        return cow.getTags().contains(OP_TAG);
    }

    public static void makeOpCow(Cow cow) {
        cow.addTag(OP_TAG);
        boostAttributes(cow);
        cow.setGlowingTag(true);
        addCombatGoals(cow);
    }

    private static void boostAttributes(Cow cow) {
        AttributeInstance healthAttr = cow.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(ModConfigValues.cowHealth);
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

        // Set follow range so targeting goals can detect hostiles at the configured distance
        AttributeInstance followRange = cow.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) {
            followRange.setBaseValue(ModConfigValues.detectionRange);
        }
    }

    public static void addCombatGoals(Cow cow) {
        cow.targetSelector.addGoal(1, new HostileTargetGoal(cow));

        if (ModConfigValues.chargeAttackEnabled) {
            cow.goalSelector.addGoal(2, new ChargeGoal(cow));
        }

        if (ModConfigValues.milkProjectileEnabled) {
            cow.goalSelector.addGoal(3, new MilkProjectileGoal(cow));
        }
    }


    public static void onEntityJoinLevel(Entity entity, Level level) {
        if (!(entity instanceof Cow cow)) return;
        if (level.isClientSide()) return;

        // Log the activation mode once for diagnostics (helps catch stale config files)
        if (!loggedActivationMode) {
            loggedActivationMode = true;
            ModConfigValues.ActivationMode mode = ModConfigValues.activationMode;
            MooOfDoom.LOGGER.info("[Moo of Doom] Activation mode: {}. If this is unexpected, delete " +
                    "config/mooofdoom-common.toml to regenerate with current defaults.", mode);
            if (mode == ModConfigValues.ActivationMode.ALL_COWS) {
                MooOfDoom.LOGGER.warn("[Moo of Doom] ALL_COWS mode is active — every cow will become OP! " +
                        "Set mode = \"ITEM_ACTIVATED\" in mooofdoom-common.toml for normal gameplay.");
            }
        }

        // If already OP, re-apply attributes (they reset on load)
        if (isOpCow(cow)) {
            boostAttributes(cow);
            cow.setGlowingTag(true);
            addCombatGoals(cow);
            return;
        }

        // Determine if this cow should become OP
        ModConfigValues.ActivationMode mode = ModConfigValues.activationMode;
        switch (mode) {
            case ALL_COWS -> makeOpCow(cow);
            case RARE_SPAWN -> {
                if (cow.getRandom().nextDouble() < ModConfigValues.rareSpawnChance) {
                    makeOpCow(cow);
                }
            }
            case ITEM_ACTIVATED -> {
                // Mythic natural spawns: small chance for OP cows regardless of mode
                if (cow.getRandom().nextDouble() < ModConfigValues.mythicSpawnChance) {
                    makeOpCow(cow);
                }
            }
        }
    }


    public static boolean onLivingDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof Cow cow)) return false;
        if (!isOpCow(cow)) return false;

        // Immune to explosion damage
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return true;
        }

        // Immune to fall damage
        if (source.is(DamageTypeTags.IS_FALL)) {
            return true;
        }

        return false;
    }
}
