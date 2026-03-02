package com.github.netfallnetworks.mooofdoom.cow.chaos;

import com.github.netfallnetworks.mooofdoom.config.ModConfigValues;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.Level;

public class ExplosionHandler {


    public static void onEntityTick(Entity entity) {
        if (!(entity instanceof Cow cow)) return;
        if (cow.level().isClientSide()) return;
        if (!OpCowManager.isOpCow(cow)) return;
        if (!ModConfigValues.explosionEnabled) return;

        if (cow.getRandom().nextInt(ModConfigValues.explosionIntervalTicks) != 0) return;

        if (cow.getTarget() != null && cow.getTarget().isAlive()) {
            // Combat explosion: deals damage to the target
            cow.level().explode(
                    cow,
                    cow.getX(),
                    cow.getY() + 0.5,
                    cow.getZ(),
                    (float) ModConfigValues.explosionPower,
                    Level.ExplosionInteraction.NONE
            );
        } else {
            // Random explosion: visual + sound only, no damage
            if (cow.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        cow.getX(), cow.getY() + 0.5, cow.getZ(),
                        1, 0, 0, 0, 0);
                cow.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.0F,
                        0.8F + cow.getRandom().nextFloat() * 0.4F);
            }
        }
    }
}
