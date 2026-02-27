package com.github.netfallnetworks.mooofdoom.cow.utility;

/**
 * Pure-math loot multiplier calculation.
 * Extracted to a standalone class so unit tests don't trigger class loading
 * of Minecraft-dependent types via CombatLootHandler.
 *
 * MOOCOW formula: 1-hit kill = max multiplier, 10+ hits = 1x,
 * linear interpolation in between.
 */
public class MoocowMultiplier {

    /**
     * Calculate the MOOCOW loot multiplier based on hit count.
     * 1-hit kill = max multiplier, 10+ hits = 1x, linear interpolation between.
     */
    public static int calculate(int hits, int moocowMax) {
        if (hits >= 10) return 1;
        if (hits <= 1) return moocowMax;
        return (int) Math.round(moocowMax - (hits - 1.0) * (moocowMax - 1.0) / 9.0);
    }

    private MoocowMultiplier() {}
}
