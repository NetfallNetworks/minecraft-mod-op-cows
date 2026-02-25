# Playtest 1 Feedback — Implementation Plan (FINAL)

All design decisions locked in from playtest discussion.

---

## Core Design Principles

- **Default activation mode: `ITEM_ACTIVATED`** — cows are vanilla until recruited
- **Mythic natural OP cow spawns (~1%)** — rare encounters from the start
- **Doom Apple recipe: Golden Apple + Diamond + Milk Bucket** (mid-game accessible)
- **All cows (vanilla + OP) participate** in Rebellion and Guardian mechanics
- **OP cows are mini-bosses** — 100 HP, combat AI, rare loot

---

## 5-Tier Rarity System

Used across all RNG mechanics in the mod. See `docs/guide-tiered-rng.md` for implementation details.

| Tier | Weight | Approx % |
|------|--------|----------|
| Common | 50 | ~50% |
| Uncommon | 30 | ~30% |
| Rare | 15 | ~15% |
| Legendary | 4 | ~4% |
| Mythic | 1 | ~1% |

For mechanics with fewer than 5 outcomes, fill from the right (Mythic = rarest) back toward Common. Duplicate outcomes across tiers are fine.

### Implementation
- Single `RarityTier` enum + `TieredRandom` utility class
- Configurable weights via `ModConfig`
- Force-tier option for testing (config or command)

---

## Task 0: Foundation — Tiered Rarity System + Config Changes

### Files to Create
- `TieredRandom.java` — weighted random utility
- `RarityTier.java` — enum (COMMON, UNCOMMON, RARE, LEGENDARY, MYTHIC)
- `docs/guide-tiered-rng.md` — developer guide

### Files to Edit
- `ModConfig.java`:
  - Change `ACTIVATION_MODE` default from `ALL_COWS` to `ITEM_ACTIVATED`
  - Add `MYTHIC_SPAWN_CHANCE` (default 0.01 = 1%)
  - Add tier weights (configurable)
- `OpCowManager.java` — support hybrid mode: `ITEM_ACTIVATED` + Mythic natural spawns

### Recipe Change
- `doom_apple.json` — change `minecraft:nether_star` to `minecraft:diamond`

---

## Task 1: Rework Enchanted Milk → "Bucket of Buff" System

**Replace** the single `EnchantedMilkItem` with multiple specific buff bucket items.

### Files to Change
- **DELETE** `EnchantedMilkItem.java`
- **CREATE** `BuffBucketItem.java` — single class parameterized by buff type
- **EDIT** `ModItems.java` — register one item per buff
- **EDIT** `MilkingHandler.java` — give a random specific bucket on milking OP cow
- **EDIT** `ModConfig.java` — config per buff type (enable/disable)
- **CREATE** item definition + model JSONs per bucket (see `docs/guide-new-items.md`)
- **EDIT** `en_us.json` — names for each bucket

### Buff Buckets (Tiered Rarity)

Milking an OP cow rolls on the 5-tier rarity table:

| Tier | Item ID | Display Name | Effect | Duration |
|------|---------|-------------|--------|----------|
| Common (50%) | `bucket_of_speed` | Bucket of Speed | Speed II | 60s |
| Uncommon (30%) | `bucket_of_regeneration` | Bucket of Regeneration | Regeneration II | 30s |
| Rare (15%) | `bucket_of_strength` | Bucket of Strength | Strength II | 60s |
| Legendary (4%) | `bucket_of_fire_resistance` | Bucket of Fire Resistance | Fire Resistance | 60s |
| Mythic (1%) | `bucket_of_luck` | Bucket of Luck | Luck II | 60s |

### Behavior
- Milking an OP cow rolls the tiered table and yields the corresponding bucket
- Consuming clears negative effects (like vanilla milk) then applies the specific buff
- Returns empty bucket on use
- Stack size: 1 (like milk bucket)
- Textures: reuse vanilla milk bucket model for now, custom textures later

---

## Task 2: Adjust Cow Explosion Damage

### Current State
`ExplosionHandler.java` uses `Level.ExplosionInteraction.NONE` (no block damage) but **does damage entities including players**.

### Changes
- **EDIT** `ExplosionHandler.java`:
  - Random (non-combat) explosions: visual + sound only, no entity damage
  - When cow has an active attack target: damaging explosion (combat attack)
- **EDIT** `ModConfig.java` — add `EXPLOSION_COMBAT_ONLY` toggle

---

## Task 3: Rework Doom Apple — Multi-Target Tiered System

The Doom Apple now has different behaviors depending on who receives it, with 5-tier RNG outcomes.

### Player Eats Doom Apple

| Common | Uncommon | Rare | Legendary | Mythic |
|--------|----------|------|-----------|--------|
| God-mode buff (30s) | God-mode buff (30s) | Guardian buff (2min) | God-mode + Guardian (both) | **FULL COW MORPH** (30s) + both buffs + OP cow attributes |

**God-mode buff:** Strength III, Resistance II, Speed II, Glowing, Fire Resistance (30s)

**Full cow morph:** Player model replaced with cow model, cow eye-height, cow movement speed, can't use items/attack, other cows treat player as cow. Receives god-mode + guardian buffs + OP cow attributes (100 HP, knockback resist). Reverts after 30s.

### Feed Doom Apple to Hostile Mob

| Common | Uncommon | Rare | Legendary | Mythic |
|--------|----------|------|-----------|--------|
| Following protector (30s) | Following protector (30s) | Explodes → 0.5x MOOCOW loot | Turns into a cow (permanent) | Turns into an **OP cow** (permanent) |

"Following protector" = mob follows the player and attacks hostile mobs for 30s.

### Feed Doom Apple to Cow

- Non-OP cow: transforms into an OP cow (existing mechanic, no RNG)
- Already-OP cow: wasted apple (no effect, may rebalance later)

### Feed Doom Apple to Already-OP Cow

Wasted — apple is consumed, nothing happens. Revisit in future if needed.

### Files to Change
- **REWRITE** `DoomAppleUseHandler.java` — split into eat vs feed logic
- **EDIT** `ModItems.java` — make Doom Apple a food item (consumable)
- **CREATE** `CowMorphHandler.java` — full player morph system
- **CREATE** `MobConversionHandler.java` — hostile mob side-switch / conversion

---

## Task 4: Implement "Rebellion of the Cows" Debuff

**Custom mob effect** — ALL cows (vanilla + OP) become hostile to the player.

### Triggers (any of these)
1. Player attacks any cow
2. Player kills an OP cow
3. Player kills a vanilla cow within range of an OP cow

**Range:** 16 blocks (1 chunk radius) — same range used for Guardian

### Behavior
- Duration: 2 minutes (2400 ticks)
- All cows within range aggro the debuffed player
- Vanilla cows temporarily gain attack ability: 1-2 base damage with 20% pseudo-crit chance (2x = 2-4 damage). Crit particles on proc. One cow is a nuisance, a herd is deadly.
- OP cows use their full combat AI against the player

### Files to Create
- `RebellionEffect.java` — custom `MobEffect`
- `RebellionHandler.java` — trigger detection + cow aggro logic
- `ModEffects.java` — effect registry

### Files to Edit
- `MooOfDoom.java` — register handler + effect registry
- `ModConfig.java` — `REBELLION_ENABLED`, `REBELLION_DURATION_TICKS`, `REBELLION_RANGE` (default 16)
- `en_us.json` — effect name + description

---

## Task 5: Implement "Guardian of the Cows" Buff

**Custom mob effect** — ALL cows (vanilla + OP) follow and defend the player.

### Trigger
- Player feeds wheat to any cow

### Behavior
- Duration: 2 minutes (2400 ticks)
- All cows within 16 blocks follow the player
- Vanilla cows temporarily gain combat AI (attack hostile mobs)
- OP cows prioritize defending the player over random targeting

### Files to Create
- `GuardianEffect.java` — custom `MobEffect`
- `GuardianHandler.java` — trigger detection + cow follow/defend logic

### Files to Edit
- `MooOfDoom.java` — register handler
- `ModConfig.java` — `GUARDIAN_ENABLED`, `GUARDIAN_DURATION_TICKS`, `GUARDIAN_RANGE` (default 16)
- `en_us.json` — effect name + description

---

## Task 5.5: OP Cow Death Event

When an OP cow is killed, trigger a dramatic "totem pop" style death event.

### Effects
- **Sound:** Explosion sound + cow scream layered together
- **Particles:** Gold/green burst (totem-style particle shower)
- **Audible range:** ~48 blocks (similar to totem pop, not server-wide)
- **Gameplay:** Triggers Rebellion on the killer

### Purpose
Killing an OP cow should turn heads. Nearby players hear the death wail and know something just happened. Then the cows turn hostile.

### Files to Create
- `OpCowDeathHandler.java` — `LivingDeathEvent` handler for OP cows

### Files to Edit
- `MooOfDoom.java` — register handler

---

## Task 6: Dynamic Loot System

### Vanilla Cow Drops (buffed)
- 2x base beef + leather (always, regardless of combat efficiency)

### OP Cow Drops on Death
Two independent drop calculations:

**A) Base drops (beef + leather) — MOOCOW multiplied:**
- MOOCOW_MULTIPLIER = 10 (10x HP = 10x loot)
- 1 hit → 10x multiplier
- 10+ hits → 1x multiplier (base)
- 2-9 hits → linear interpolation, **rounded to nearest integer**

Formula:
```
if hits >= 10: multiplier = 1
if hits == 1: multiplier = MOOCOW_MULTIPLIER (10)
else: multiplier = round(MOOCOW_MULTIPLIER - (hits - 1) * (MOOCOW_MULTIPLIER - 1) / 9)
```

**B) Rare loot — one roll on tiered table (NOT affected by MOOCOW):**

| Tier | Drop |
|------|------|
| Common | Iron Ingot (8x) |
| Uncommon | Gold Ingot (5x), Emerald (3x) |
| Rare | Diamond (1x) |
| Legendary | Netherite Scrap (1x) |
| Mythic | Doom Apple (1x) |

### OP Cow Alive Drops (periodic, ~5 min)
Same tiered table as death drops. This is the "passive loot piñata" — keep your OP cow alive for a steady trickle.

**Design tension:** OP cow alive = steady trickle forever. OP cow dead = burst of MOOCOW-multiplied base drops + one rare roll + Rebellion triggered. Smart play = keep them alive.

### Files to Create
- `CombatLootHandler.java` — hit tracking + death drop calculation

### Files to Edit
- `LootDropHandler.java` — convert to tiered rarity table for alive-drops
- `ModConfig.java` — `MOOCOW_MULTIPLIER` (default 10), `VANILLA_COW_LOOT_MULTIPLIER` (default 2)
- `MooOfDoom.java` — register handler

---

## Task 7: Advancements

Create advancement tab with dad-joke taglines. See `docs/guide-advancements.md` for format.

### Advancement Tree
| ID | Parent | Trigger | Icon | Title | Frame |
|----|--------|---------|------|-------|-------|
| `root` | — | `tick` (auto) | `doom_apple` | "Moo of Doom" | task |
| `doom_apple` | `root` | `inventory_changed` | `doom_apple` | "An Apple a Day..." | task |
| `bucket_of_strength` | `doom_apple` | `inventory_changed` | `bucket_of_strength` | "Udderly Powerful" | task |
| `bucket_of_speed` | `doom_apple` | `inventory_changed` | `bucket_of_speed` | "Fast and the Furry-ous" | task |
| `bucket_of_regeneration` | `doom_apple` | `inventory_changed` | `bucket_of_regeneration` | "Got Milk?" | task |
| `bucket_of_fire_resistance` | `doom_apple` | `inventory_changed` | `bucket_of_fire_resistance` | "Too Hot to Handle" | task |
| `bucket_of_luck` | `doom_apple` | `inventory_changed` | `bucket_of_luck` | "Feeling Lucky, Punk?" | task |

### Files to Create
- Advancement JSONs in `data/mooofdoom/advancement/`
- Optional: 16x16 tab background texture

### Files to Edit
- `en_us.json` — all titles + descriptions

---

## Task 8: Milkable Cow Morph (Circling Back)

When a player is in full cow morph (Mythic Doom Apple roll), other players can milk them.

### Behavior
- Morphed player is treated as a cow by the milking system
- Another player right-clicks with a bucket → rolls the buff bucket tiered table
- Morphed player hears a milk sound and gets a particle effect
- Only works during the 30s morph window

### Design Notes
- This is hilarious and rewards Mythic rolls for multiplayer
- The morphed player is essentially a walking Mythic loot source for 30s
- Combines with their OP attributes — they're powerful AND useful to allies

### Files to Edit
- `MilkingHandler.java` — extend to detect morphed players
- `CowMorphHandler.java` — expose morph state for milking check

---

## Implementation Order

```
 0. Foundation — Tiered RNG system, config defaults, recipe change
 1. Bucket of Buff system (needed before advancements)
 2. Explosion damage fix (small, self-contained)
 3. Doom Apple rework (multi-target + tiered RNG)
 4. Rebellion debuff (uses shared range constant)
 5. Guardian buff (similar pattern to Rebellion)
5.5. OP Cow death event (dramatic death + triggers Rebellion)
 6. Dynamic loot system (uses tiered RNG + MOOCOW multiplier)
 7. Advancements (depends on all items existing)
 8. Milkable cow morph (extends morph + milking systems)
```

**Each task:** `./gradlew build` before → implement → `./gradlew build` after → verify green → commit.

---

## Locked-In Decisions

| Decision | Value |
|----------|-------|
| Activation default | `ITEM_ACTIVATED` |
| Mythic natural OP spawn | ~1% of cow spawns |
| Doom Apple recipe | Golden Apple + Diamond + Milk Bucket |
| MOOCOW multiplier | 10x (1-hit kill on OP cow) |
| Vanilla cow loot buff | 2x base drops |
| Rebellion/Guardian scope | ALL cows (vanilla + OP) |
| Rebellion/Guardian range | 16 blocks (1 chunk) |
| Rebellion triggers | Attack cow, kill OP cow, kill cow near OP cow |
| Rarity tiers | 50/30/15/4/1 weights |
| Player cow morph | Full morph (30s), OP attributes |
| Hostile side-switch | 30s temporary |
| Hostile → cow | Permanent |
| Hostile → OP cow (Mythic) | Permanent |
| Bucket textures | Vanilla milk bucket for now |
| Buff bucket selection | Tiered rarity (Speed=Common → Luck=Mythic) |
| Alive-drop loot table | Tiered (same table as death rare drops) |
| Alive-drop Doom Apple | Mythic tier (~1% per drop cycle) |
| Vanilla cow Rebellion damage | 1-2 base, 20% pseudo-crit (2x) |
| Hostile mob side-switch | Following protector for 30s |
| Feed apple to already-OP cow | Wasted (revisit later) |
| OP cow death event | Totem-pop style (explosion + cow scream, ~48 block range) |
| Milkable cow morph | Yes — other players can milk a morphed player |
