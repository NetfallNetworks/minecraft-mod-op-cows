package com.github.netfallnetworks.mooofdoom.rarity;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import net.minecraft.util.RandomSource;

/**
 * Weighted random selection across the 5-tier rarity system.
 * Weights are configurable via ModConfig (default: 50/30/15/4/1).
 */
public class TieredRandom {

    /**
     * Roll a rarity tier using the configured weights.
     */
    public static RarityTier roll(RandomSource random) {
        int common = ModConfig.RARITY_COMMON_WEIGHT.getAsInt();
        int uncommon = ModConfig.RARITY_UNCOMMON_WEIGHT.getAsInt();
        int rare = ModConfig.RARITY_RARE_WEIGHT.getAsInt();
        int legendary = ModConfig.RARITY_LEGENDARY_WEIGHT.getAsInt();
        int mythic = ModConfig.RARITY_MYTHIC_WEIGHT.getAsInt();

        int total = common + uncommon + rare + legendary + mythic;
        int roll = random.nextInt(total);

        if (roll < common) return RarityTier.COMMON;
        roll -= common;
        if (roll < uncommon) return RarityTier.UNCOMMON;
        roll -= uncommon;
        if (roll < rare) return RarityTier.RARE;
        roll -= rare;
        if (roll < legendary) return RarityTier.LEGENDARY;
        return RarityTier.MYTHIC;
    }

    /**
     * Roll using a java.util.Random (for contexts without RandomSource).
     */
    public static RarityTier roll(java.util.Random random) {
        int common = ModConfig.RARITY_COMMON_WEIGHT.getAsInt();
        int uncommon = ModConfig.RARITY_UNCOMMON_WEIGHT.getAsInt();
        int rare = ModConfig.RARITY_RARE_WEIGHT.getAsInt();
        int legendary = ModConfig.RARITY_LEGENDARY_WEIGHT.getAsInt();
        int mythic = ModConfig.RARITY_MYTHIC_WEIGHT.getAsInt();

        int total = common + uncommon + rare + legendary + mythic;
        int roll = random.nextInt(total);

        if (roll < common) return RarityTier.COMMON;
        roll -= common;
        if (roll < uncommon) return RarityTier.UNCOMMON;
        roll -= uncommon;
        if (roll < rare) return RarityTier.RARE;
        roll -= rare;
        if (roll < legendary) return RarityTier.LEGENDARY;
        return RarityTier.MYTHIC;
    }
}
