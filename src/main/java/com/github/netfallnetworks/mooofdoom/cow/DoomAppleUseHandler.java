package com.github.netfallnetworks.mooofdoom.cow;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.cow.Cow;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class DoomAppleUseHandler {

    @SubscribeEvent
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (ModConfig.ACTIVATION_MODE.get() != ModConfig.ActivationMode.ITEM_ACTIVATED) return;
        if (!(event.getTarget() instanceof Cow cow)) return;
        if (!event.getItemStack().is(ModItems.DOOM_APPLE.get())) return;
        if (OpCowManager.isOpCow(cow)) return;

        // Consume the item
        if (!event.getEntity().getAbilities().instabuild) {
            event.getItemStack().shrink(1);
        }

        // Transform the cow
        OpCowManager.makeOpCow(cow);

        // Effects
        ServerLevel serverLevel = (ServerLevel) event.getLevel();
        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                cow.getX(), cow.getY() + 1, cow.getZ(),
                50, 0.5, 1.0, 0.5, 0.2);
        cow.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);
    }
}
