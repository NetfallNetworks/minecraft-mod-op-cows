package com.github.netfallnetworks.mooofdoom.neoforge;

import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import com.github.netfallnetworks.mooofdoom.cow.CowMorphHandler;
import com.github.netfallnetworks.mooofdoom.cow.DoomAppleUseHandler;
import com.github.netfallnetworks.mooofdoom.cow.MobConversionHandler;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import com.github.netfallnetworks.mooofdoom.cow.chaos.ExplosionHandler;
import com.github.netfallnetworks.mooofdoom.cow.chaos.MoonJumpHandler;
import com.github.netfallnetworks.mooofdoom.cow.chaos.SizeChangeHandler;
import com.github.netfallnetworks.mooofdoom.cow.effects.GuardianHandler;
import com.github.netfallnetworks.mooofdoom.cow.effects.OpCowDeathHandler;
import com.github.netfallnetworks.mooofdoom.cow.effects.RebellionHandler;
import com.github.netfallnetworks.mooofdoom.cow.utility.AuraHandler;
import com.github.netfallnetworks.mooofdoom.cow.utility.CombatLootHandler;
import com.github.netfallnetworks.mooofdoom.cow.utility.LootDropHandler;
import com.github.netfallnetworks.mooofdoom.cow.utility.MilkingHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Single dispatcher that bridges NeoForge events to platform-agnostic handlers in common/.
 * Each @SubscribeEvent method unwraps the NeoForge event and delegates to common handlers.
 */
@EventBusSubscriber(modid = MooOfDoom.MODID)
public class NeoForgeEventHandler {

    // --- Entity Tick (EntityTickEvent.Post) ---

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        var entity = event.getEntity();

        // Cow tick handlers (chaos, utility, loot)
        SizeChangeHandler.onEntityTick(entity);
        ExplosionHandler.onEntityTick(entity);
        MoonJumpHandler.onEntityTick(entity);
        AuraHandler.onEntityTick(entity);
        LootDropHandler.onEntityTick(entity);

        // Player tick handlers (effects, morph)
        RebellionHandler.onPlayerTick(entity);
        GuardianHandler.onPlayerTick(entity);
        CowMorphHandler.onPlayerTick(entity);

        // Mob tick handler (protector follow)
        MobConversionHandler.onMobTick(entity);
    }

    // --- Player Interact Entity (PlayerInteractEvent.EntityInteract) ---

    @SubscribeEvent
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        var player = event.getEntity();
        var target = event.getTarget();
        var hand = event.getHand();
        var level = event.getLevel();

        DoomAppleUseHandler.onPlayerInteractEntity(player, target, hand, level);
        MilkingHandler.onPlayerInteractEntity(player, target, hand, level);
        GuardianHandler.onFeedCow(player, target, hand, level);
    }

    // --- Living Incoming Damage (LivingIncomingDamageEvent) ---

    @SubscribeEvent
    public static void onLivingDamage(LivingIncomingDamageEvent event) {
        var entity = event.getEntity();
        var source = event.getSource();
        var amount = event.getAmount();

        // OP cow damage immunity (cancel if handler returns true)
        if (OpCowManager.onLivingDamage(entity, source, amount)) {
            event.setCanceled(true);
            return;
        }

        // Track hits for MOOCOW loot multiplier
        CombatLootHandler.onCowHit(entity);

        // Rebellion: cow hurt triggers rebellion
        RebellionHandler.onCowHurt(entity, source);
    }

    // --- Living Death (LivingDeathEvent) ---

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        var entity = event.getEntity();
        var source = event.getSource();

        OpCowDeathHandler.onOpCowDeath(entity, source);
        CombatLootHandler.onCowDeath(entity, source);
        RebellionHandler.onCowDeath(entity, source);
    }

    // --- Entity Join Level (EntityJoinLevelEvent) ---

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        OpCowManager.onEntityJoinLevel(event.getEntity(), event.getLevel());
    }
}
