package com.github.netfallnetworks.mooofdoom.rarity;

import com.github.netfallnetworks.mooofdoom.config.ModConfigValues;
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
        int common = ModConfigValues.rarityCommonWeight;
        int uncommon = ModConfigValues.rarityUncommonWeight;
        int rare = ModConfigValues.rarityRareWeight;
        int legendary = ModConfigValues.rarityLegendaryWeight;
        int mythic = ModConfigValues.rarityMythicWeight;

        int total = common + uncommon + rare + legendary + mythic;
        return roll(random.nextInt(total), common, uncommon, rare, legendary, mythic);
    }

    /**
     * Roll using a java.util.Random (for contexts without RandomSource).
     */
    public static RarityTier roll(java.util.Random random) {
        int common = ModConfigValues.rarityCommonWeight;
        int uncommon = ModConfigValues.rarityUncommonWeight;
        int rare = ModConfigValues.rarityRareWeight;
        int legendary = ModConfigValues.rarityLegendaryWeight;
        int mythic = ModConfigValues.rarityMythicWeight;

        int total = common + uncommon + rare + legendary + mythic;
        return roll(random.nextInt(total), common, uncommon, rare, legendary, mythic);
    }

    /**
     * Pure logic: roll a tier from explicit weights. Testable without ModConfig.
     */
    public static RarityTier roll(int rollValue, int common, int uncommon, int rare, int legendary, int mythic) {
        if (rollValue < common) return RarityTier.COMMON;
        rollValue -= common;
        if (rollValue < uncommon) return RarityTier.UNCOMMON;
        rollValue -= uncommon;
        if (rollValue < rare) return RarityTier.RARE;
        rollValue -= rare;
        if (rollValue < legendary) return RarityTier.LEGENDARY;
        return RarityTier.MYTHIC;
    }

    /**
     * Pure logic: roll a tier using a Random and explicit weights.
     */
    public static RarityTier roll(java.util.Random random, int common, int uncommon, int rare, int legendary, int mythic) {
        int total = common + uncommon + rare + legendary + mythic;
        return roll(random.nextInt(total), common, uncommon, rare, legendary, mythic);
    }
}
