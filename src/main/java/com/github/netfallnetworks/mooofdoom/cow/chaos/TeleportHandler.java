package com.github.netfallnetworks.mooofdoom.cow.chaos;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class TeleportHandler {

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Cow cow)) return;
        if (cow.level().isClientSide()) return;
        if (!OpCowManager.isOpCow(cow)) return;
        if (!ModConfig.TELEPORT_ENABLED.getAsBoolean()) return;

        if (cow.getRandom().nextInt(ModConfig.TELEPORT_INTERVAL_TICKS.getAsInt()) != 0) return;

        int range = ModConfig.TELEPORT_RANGE.getAsInt();
        ServerLevel level = (ServerLevel) cow.level();

        // Try up to 10 times to find a safe position
        for (int i = 0; i < 10; i++) {
            double x = cow.getX() + (cow.getRandom().nextDouble() - 0.5) * 2 * range;
            double z = cow.getZ() + (cow.getRandom().nextDouble() - 0.5) * 2 * range;
            double y = cow.level().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);

            BlockPos pos = BlockPos.containing(x, y, z);
            BlockState below = cow.level().getBlockState(pos.below());
            if (below.isSolid()) {
                // Particles at old position
                level.sendParticles(ParticleTypes.PORTAL,
                        cow.getX(), cow.getY() + 1, cow.getZ(),
                        30, 0.5, 1.0, 0.5, 0.0);

                cow.teleportTo(x, y, z);

                // Particles at new position
                level.sendParticles(ParticleTypes.PORTAL,
                        x, y + 1, z,
                        30, 0.5, 1.0, 0.5, 0.0);
                cow.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                break;
            }
        }
    }
}
