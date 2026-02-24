package com.github.netfallnetworks.mooofdoom.cow.chaos;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class ExplosionHandler {

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Cow cow)) return;
        if (cow.level().isClientSide()) return;
        if (!OpCowManager.isOpCow(cow)) return;
        if (!ModConfig.EXPLOSION_ENABLED.getAsBoolean()) return;

        if (cow.getRandom().nextInt(ModConfig.EXPLOSION_INTERVAL_TICKS.getAsInt()) != 0) return;

        // Explosion that does NOT destroy blocks (Level.ExplosionInteraction.NONE)
        cow.level().explode(
                cow,                    // source entity (cow is immune to its own)
                cow.getX(),
                cow.getY() + 0.5,
                cow.getZ(),
                (float) ModConfig.EXPLOSION_POWER.get().doubleValue(),
                Level.ExplosionInteraction.NONE  // No block destruction
        );
    }
}
