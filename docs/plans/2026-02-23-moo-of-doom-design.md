# Moo of Doom - Design Document

**Date:** 2026-02-23
**Minecraft Version:** 1.21.11
**Mod Loader:** NeoForge (21.11.38-beta+)
**Java Version:** 21
**Mod ID:** `mooofdoom`
**License:** MIT

## Overview

Moo of Doom is a Minecraft mod that transforms ordinary cows into absurdly overpowered entities. OP cows are powerful combat allies that hunt hostile mobs, provide valuable resources and buffs, and exhibit chaotic comedic behaviors like random teleportation, size changes, explosions, and moon jumps.

## Activation Modes (Configurable)

The mod supports three mutually exclusive activation modes, selectable via config:

| Mode | Behavior |
|------|----------|
| `ALL_COWS` | Every cow in the world becomes OP on spawn |
| `ITEM_ACTIVATED` | Players craft a "Doom Apple" and right-click a cow to upgrade it |
| `RARE_SPAWN` | Configurable % chance (default 5%) that a spawning cow is OP |

OP cows are tagged with persistent NBT data (`MooOfDoom:true`) to survive save/load cycles. They have a glowing effect and custom particles as a visual indicator.

## Combat System

### Boosted Attributes
- Health: 100 HP (vanilla cow: 10 HP)
- Attack damage: +10
- Knockback resistance: high
- Movement speed: boosted

### Charge Attack (ChargeGoal)
- Targets nearest hostile mob within detection range
- Lowers head, sprints at high speed toward target
- Deals heavy damage + massive knockback on impact
- Cooldown between charges (configurable)

### Milk Projectile (MilkProjectileGoal)
- Ranged attack using a custom milk projectile entity
- Deals damage on hit + applies Slowness effect
- Based on vanilla snowball mechanics
- Used when target is outside melee range

### Auto-Aggression (NearestHostileTargetGoal)
- Automatically targets hostile mobs: zombies, skeletons, creepers, spiders, etc.
- Configurable detection range
- Added to cow's `targetSelector` via `EntityJoinLevelEvent`

## Utility System

### Enchanted Milk
- Right-click OP cow with a bucket to receive "Enchanted Milk" (custom item)
- Consuming grants random beneficial potion effects:
  - Strength, Regeneration, Fire Resistance, Speed
- Registered as a custom item via NeoForge item registry

### Rare Item Drops
- OP cows periodically drop random valuable items while alive:
  - Diamonds, emeralds, enchanted books, netherite scraps
- Driven by a timed loot table, not on-death drops
- Drop frequency configurable

### Passive Aura Buffs
- Players within 10 blocks (configurable) receive:
  - Strength I, Regeneration I, Luck
- Applied via `EntityTickEvent`

## Chaos System

All chaos behaviors have configurable frequency and can be individually toggled.

### Random Teleportation
- Every ~60 seconds (randomized), cow teleports 5-15 blocks in random direction
- Enderman-style particles and sound effects
- Checks for safe landing position

### Size Changes
- Cow randomly scales between 0.5x and 3x normal size
- Affects hitbox and visual model
- Changes every few minutes

### Random Explosions
- Cow occasionally creates a small explosion
- Does NOT destroy blocks (configurable)
- Pure knockback + particles + comedy
- Cow is immune to its own explosions

### Moon Jumps
- Cow randomly launches 10+ blocks into the air
- Slow floating descent with particle trails
- Cow takes no fall damage

## Configuration

Uses NeoForge's built-in config system (`ModConfigSpec`).
Config file: `.minecraft/config/mooofdoom-common.toml`

### Config Options
- **Activation mode:** ALL_COWS / ITEM_ACTIVATED / RARE_SPAWN
- **Rare spawn chance:** 0.0 - 1.0 (default 0.05)
- **Combat toggles:** charge attack, milk projectile (each on/off)
- **Combat tunables:** health, damage, detection range, charge cooldown
- **Utility toggles:** enchanted milk, rare drops, passive aura (each on/off)
- **Utility tunables:** aura range, drop frequency, drop table
- **Chaos toggles:** teleportation, size changes, explosions, moon jumps (each on/off)
- **Chaos tunables:** frequency for each behavior, explosion power, teleport range

## Custom Items

| Item | Recipe/Source | Purpose |
|------|--------------|---------|
| Doom Apple | Golden Apple + Nether Star + Milk Bucket (shapeless) | Upgrades a cow to OP (ITEM_ACTIVATED mode) |
| Enchanted Milk | Right-click OP cow with bucket | Consumable granting random potion effects |

## Custom Entities

| Entity | Base | Purpose |
|--------|------|---------|
| Milk Projectile | Snowball/ThrowableProjectile | Ranged cow attack projectile |

## Project Structure

```
src/main/java/com/github/<org>/mooofdoom/
  MooOfDoom.java              -- Main mod class, event bus registration
  ModConfig.java              -- Config definitions
  cow/
    OpCowManager.java         -- Activation, NBT tagging, attribute boosting
    combat/
      ChargeGoal.java         -- Charge attack AI goal
      MilkProjectileGoal.java -- Ranged milk attack AI goal
      MilkProjectile.java     -- Custom projectile entity
    utility/
      EnchantedMilkItem.java  -- Custom milk item with potion effects
      LootDropHandler.java    -- Periodic rare item drops
      AuraHandler.java        -- Passive buff aura
    chaos/
      TeleportHandler.java    -- Random teleportation
      SizeChangeHandler.java  -- Random size scaling
      ExplosionHandler.java   -- Random explosions
      MoonJumpHandler.java    -- Random sky launches
  registry/
    ModItems.java             -- Item registration (Enchanted Milk, Doom Apple)
    ModEntityTypes.java       -- Entity type registration (Milk Projectile)

src/main/resources/
  META-INF/
    neoforge.mods.toml        -- Mod metadata
  assets/mooofdoom/
    lang/
      en_us.json              -- Localization strings
    textures/item/
      doom_apple.png           -- Doom Apple texture
      enchanted_milk.png       -- Enchanted Milk texture
  data/mooofdoom/
    recipe/
      doom_apple.json          -- Doom Apple crafting recipe
```

## Scope Exclusions (YAGNI)

- No custom cow model or texture (vanilla cow + glowing effect)
- No GUIs or menus
- No custom networking packets (server-side logic, auto-synced)
- No advancement/achievement system
- No cow variants or tier system
- No custom sounds (use vanilla sound events)

## Distribution

- Build with `gradlew build`, output JAR in `build/libs/`
- Publish to CurseForge
- Source code on GitHub under MIT license
