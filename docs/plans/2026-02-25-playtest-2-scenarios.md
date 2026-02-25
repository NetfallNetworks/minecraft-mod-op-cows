# Playtest 2 — Test Scenarios

**Build:** `8349b1c` (all playtest 1 tasks complete)
**Date:** 2026-02-25

## Setup

1. Fresh world, Survival mode, default config
2. Give yourself materials: `/give @s golden_apple 16`, `/give @s diamond 16`, `/give @s milk_bucket 16`, `/give @s bucket 8`
3. Craft a Doom Apple (shapeless: golden apple + diamond + milk bucket)
4. Find or spawn cows (`/summon cow`)
5. Default activation mode is `ITEM_ACTIVATED` — cows are vanilla until fed a Doom Apple

---

## A. Core Systems (Regressions)

### A1. OP Cow Activation
- [ ] Feed a Doom Apple to a vanilla cow → cow becomes OP (glowing, 100 HP)
- [ ] Check OP cow HP: `/data get entity @e[type=cow,limit=1,sort=nearest] Health` → should be ~100
- [ ] Spawn ~100 cows → roughly 1 should naturally become OP (~1% mythic chance)
- [ ] OP cow persists across world reload (save, quit, rejoin — cow still glowing, still OP)

### A2. OP Cow Combat AI
- [ ] OP cow auto-aggros nearby hostile mobs (zombies, skeletons, etc.)
- [ ] Spawn a zombie ~10 blocks from OP cow → cow should charge and attack
- [ ] Charge attack: cow launches at target from 4-16 blocks away, deals knockback
- [ ] Milk projectile: at > 6 blocks, cow shoots milk at target (white particle trail)
- [ ] Milk projectile applies Slowness II on hit (3 sec)
- [ ] OP cow is immune to explosion damage (test with TNT)
- [ ] OP cow is immune to fall damage (push off a cliff)

### A3. Chaos Features
- [ ] **Explosions:** Wait near OP cow — random visual explosion (particles + sound, NO damage to player) should occur. Confirm no player damage from random explosions
- [ ] **Explosions (combat):** When OP cow is fighting a mob, combat explosions DO deal damage to the target (and bystanders — known behavior)
- [ ] **Size changes:** OP cow randomly changes size (0.5x–3.0x) with poof particles
- [ ] **Moon jumps:** OP cow occasionally launches skyward with firework particles, slow-falls down with end_rod particles, cloud particles on landing

### A4. Passive Aura
- [ ] Stand within 10 blocks of an OP cow → receive Strength I, Regeneration I, Luck I
- [ ] Walk away (> 10 blocks) → buffs stop refreshing after ~3 sec
- [ ] Multiple OP cows in range → effects should refresh (no stacking amplifier)

---

## B. Doom Apple — Player Eating

### B1. Eating Mechanics
- [ ] Doom Apple is edible (right-click in air starts eating animation, 1.6 sec)
- [ ] Can be eaten when not hungry (always edible, like golden apple)
- [ ] Eating animation is the standard food eating animation
- [ ] Gives 4 hunger + 9.6 saturation

### B2. Tiered Outcomes (eat ~20 apples to sample distribution)
- [ ] **Common/Uncommon (~80%):** God-mode for 30s — Strength III, Resistance II, Speed II, Glowing, Fire Resistance
- [ ] **Rare (~15%):** Guardian of the Cows effect (2 min)
- [ ] **Legendary (~4%):** God-mode (30s) + Guardian (2 min) — both active simultaneously
- [ ] **Mythic (~1%):** God-mode (30s) + Guardian (30s) + **Cow Morph** — player turns invisible, companion cow appears

### B3. Mythic Cow Morph (use `/mooofdoom-common.toml` or set mythic weight high to test)
- [ ] Player becomes invisible
- [ ] A cow appears at the player's position
- [ ] Companion cow follows player movement precisely (position + rotation synced)
- [ ] Companion cow is invulnerable (try hitting it — no damage)
- [ ] After 30 seconds, morph ends: cow disappears, player reappears with cloud particles + cow sound
- [ ] During morph, player still has god-mode buffs active

---

## C. Doom Apple — Feed to Entities

### C1. Feed to Vanilla Cow
- [ ] Right-click vanilla cow with Doom Apple → cow transforms to OP cow
- [ ] Totem particles + totem sound on transformation
- [ ] Apple consumed (1 less in hand, or no consumption in creative)

### C2. Feed to Already-OP Cow
- [ ] Right-click OP cow with Doom Apple → apple consumed, VILLAGER_NO sound, nothing else
- [ ] Confirm cow stays the same (no extra health, no crash)

### C3. Feed to Hostile Mob (test with zombies/skeletons)
- [ ] **Common/Uncommon (~80%):** Mob stops attacking player, follows player, attacks other hostile mobs for 30s. Green heart particles + XP orb sound
- [ ] After 30s, protector behavior expires
- [ ] **Rare (~15%):** Mob explodes (visual explosion + death). Check death drops appear
- [ ] **Legendary (~4%):** Mob converts into a vanilla cow permanently. Totem particles
- [ ] **Mythic (~1%):** Mob converts into an OP cow permanently (glowing, 100 HP). Totem particles

### C4. Following Protector Details
- [ ] Protector mob navigates toward player (stays within ~3 blocks)
- [ ] Protector mob targets nearby hostile mobs (not the player)
- [ ] Multiple protectors don't attack each other
- [ ] Protector behavior ends after exactly 30 seconds

---

## D. Milking System

### D1. Milking OP Cow
- [ ] Right-click OP cow with empty bucket → get a random buff bucket (NOT vanilla milk)
- [ ] Bucket consumed, buff bucket appears in inventory
- [ ] Cow moo milk sound plays
- [ ] Repeat 10+ times: distribution should skew toward Bucket of Speed (Common)

### D2. Buff Bucket Consumption
- [ ] Drink Bucket of Speed → Speed II for 60s
- [ ] Drink Bucket of Regeneration → Regen II for 30s
- [ ] Drink Bucket of Strength → Strength II for 60s
- [ ] Drink Bucket of Fire Resistance → Fire Resistance I for 60s
- [ ] Drink Bucket of Luck → Luck II for 60s
- [ ] **Each bucket clears ALL existing effects first** (like vanilla milk) — test: apply god-mode via Doom Apple, then drink a bucket → god-mode should be stripped

### D3. Milking Companion Cow (Morphed Player)
- [ ] Player A morphs (Mythic Doom Apple roll)
- [ ] Player B right-clicks companion cow with bucket → gets buff bucket + extra sparkle particles
- [ ] Works during the 30s morph window only

### D4. Milking Vanilla Cow
- [ ] Right-click vanilla cow with bucket → normal vanilla milk (not buff bucket)

---

## E. Rebellion of the Cows

### E1. Triggers
- [ ] **Attack any cow:** Hit a vanilla cow → player gets "Rebellion of the Cows" debuff (2 min)
- [ ] **Kill OP cow:** Kill an OP cow → Rebellion applied
- [ ] **Kill cow near OP cow:** Kill a vanilla cow within 16 blocks of an OP cow → Rebellion

### E2. Rebellion Behavior
- [ ] Nearby cows (all types) turn hostile toward debuffed player
- [ ] Vanilla cows gain temporary melee attack (1-2 damage)
- [ ] OP cows use full combat AI (charge, milk projectile) against the player
- [ ] Effect icon appears in player HUD (red-orange)
- [ ] Lasts 2 minutes, then cows stop attacking

### E3. Rebellion Cleanup
- [ ] After Rebellion expires, cows near the player stop being hostile
- [ ] Vanilla cows lose their rebel tag and stop chasing
- [ ] Player can safely walk among cows again after expiry

---

## F. Guardian of the Cows

### F1. Trigger
- [ ] Feed wheat to any cow → player gets "Guardian of the Cows" buff (2 min)
- [ ] Effect icon appears in player HUD (blue)

### F2. Guardian Behavior
- [ ] Nearby cows follow the player (walk toward player if > 3 blocks away)
- [ ] Cows attack hostile mobs near the player
- [ ] Vanilla cows gain temporary combat ability against monsters
- [ ] OP cows prioritize defending the player

### F3. Guardian + Combat
- [ ] Spawn zombies near a guardian player → cows should engage the zombies
- [ ] Multiple cows should swarm the closest hostile mob

### F4. Guardian Cleanup
- [ ] After Guardian expires, cows stop following and return to normal behavior

---

## G. OP Cow Death Event

### G1. Death Effects
- [ ] Kill an OP cow → dramatic death event:
  - Golden totem particles (large burst)
  - Explosion particles
  - Loud explosion sound + cow death scream + totem sound (audible ~48 blocks)
- [ ] The death event is LOUD — comparable to a totem pop

### G2. Death Triggers Rebellion
- [ ] Kill OP cow → player immediately gets Rebellion debuff
- [ ] Nearby cows turn hostile

---

## H. Dynamic Loot System

### H1. Vanilla Cow Drops
- [ ] Kill a vanilla cow → drops 2x normal beef + leather (double the usual 1-3 beef, 0-2 leather)
- [ ] Compare to: set `vanillaCowLootMultiplier = 1` in config → drops should return to normal 1x

### H2. OP Cow Death Drops — MOOCOW Multiplier
- [ ] **One-hit kill** (full power swing on low-health cow, or use sharpness V): should drop ~10x beef + leather
- [ ] **10+ hit kill** (punch with fist, many hits): should drop ~1x beef + leather
- [ ] **Mid-range kill** (3-5 hits): should drop a proportional amount between 1x and 10x

### H3. OP Cow Rare Drops on Death
- [ ] Kill OP cow → always get one tiered rare drop in addition to beef/leather:
  - Common: 8x Iron Ingot
  - Uncommon: 5x Gold Ingot + 3x Emerald
  - Rare: 1x Diamond
  - Legendary: 1x Netherite Scrap
  - Mythic: 1x Doom Apple
- [ ] Kill ~20 OP cows: most should drop iron (Common), some gold+emerald, occasional diamond

### H4. Alive Drops (Passive Loot)
- [ ] Keep an OP cow alive for ~5+ minutes → it periodically drops tiered loot items
- [ ] Sparkle particles + XP orb sound when a drop occurs
- [ ] Same tiered table as death rare drops (iron, gold+emerald, diamond, netherite, doom apple)

---

## I. Advancements

### I1. Tab Appears
- [ ] Open advancements screen → "Moo of Doom" tab should appear (auto-granted root)

### I2. Doom Apple Advancement
- [ ] Craft or obtain a Doom Apple → "An Apple a Day..." advancement unlocks (50 XP)

### I3. Buff Bucket Advancements
- [ ] Get a Bucket of Speed → "Fast and the Furry-ous" (20 XP)
- [ ] Get a Bucket of Regeneration → "Got Milk?" (25 XP)
- [ ] Get a Bucket of Strength → "Udderly Powerful" (30 XP)
- [ ] Get a Bucket of Fire Resistance → "Too Hot to Handle" (40 XP)
- [ ] Get a Bucket of Luck → "Feeling Lucky, Punk?" (100 XP, challenge frame)

### I4. Advancement Tree Layout
- [ ] Root → Doom Apple → 5 bucket advancements branching off
- [ ] Bucket of Luck has purple/spiky challenge border

---

## J. Recipe

### J1. Doom Apple Crafting
- [ ] Shapeless recipe: Golden Apple + Diamond + Milk Bucket → Doom Apple
- [ ] Works in crafting table (any slot arrangement)
- [ ] Recipe shows in recipe book

---

## K. Edge Cases & Regression Checks

### K1. Interaction Priority
- [ ] Right-click cow with Doom Apple → feeds cow (does NOT start eating animation)
- [ ] Right-click hostile mob with Doom Apple → feeds mob (does NOT start eating)
- [ ] Right-click empty air with Doom Apple → eating animation starts
- [ ] Right-click OP cow with bucket → buff bucket (does NOT give vanilla milk)
- [ ] Right-click vanilla cow with bucket → vanilla milk (NOT buff bucket)

### K2. Creative Mode
- [ ] All feeding/eating mechanics work in creative mode
- [ ] Items NOT consumed in creative (instabuild check)

### K3. Stacking & Inventory
- [ ] Doom Apple stacks to 16
- [ ] Buff buckets stack to 1
- [ ] Buff bucket returns empty bucket after drinking

### K4. Rebellion + Guardian Simultaneously
- [ ] Feed wheat to cow → get Guardian. Then attack cow → get Rebellion
- [ ] Both effects active — observe behavior: cows may oscillate between following (Guardian) and attacking (Rebellion). Guardian cleanup and Rebellion cleanup should handle this

### K5. Combat Explosion Collateral
- [ ] When OP cow fights a mob, combat explosions damage nearby players/entities
- [ ] OP cow itself is immune to its own explosions
- [ ] This is known/expected — note if it feels too punishing for the player

### K6. Buff Bucket Clears God-Mode
- [ ] Eat Doom Apple → get god-mode. Then drink a buff bucket → all god-mode effects stripped, only bucket effect remains
- [ ] This is by design (vanilla milk behavior) — note if it feels bad

### K7. Config Persistence
- [ ] Modify config (e.g., change `rebellionRange` to 32), restart game → setting persists
- [ ] Set `mode = ALL_COWS` → all cows become OP on join
- [ ] Set `mode = ITEM_ACTIVATED` → cows are vanilla until fed apple (except ~1% mythic)

### K8. Companion Cow in ALL_COWS Mode
- [ ] **POTENTIAL BUG:** Set `mode = ALL_COWS`, then trigger cow morph → companion cow may also become OP and attack the morphed player. Document behavior

### K9. Rebel Cow Tag Persistence
- [ ] Trigger Rebellion, then log off before it expires → log back in
- [ ] Rebel cows may still have `MooOfDoom_Rebel` tag but no target. Should be harmless but note any issues

### K10. Protector Chunk Unload
- [ ] Convert a hostile mob to protector, then walk far away (unload its chunk)
- [ ] Return to the chunk → does protector behavior resume or is it stuck?

### K11. Multiple OP Cows
- [ ] Have 3+ OP cows in the same area → all should independently:
  - Fight mobs, charge, shoot milk
  - Change size randomly
  - Moon jump
  - Drop loot periodically
  - Contribute to Rebellion/Guardian behavior

---

## L. Performance

### L1. Many OP Cows
- [ ] Spawn 20 OP cows in one area → check for lag spikes or TPS drops
- [ ] All tick handlers run per-cow — potential O(n²) for rebellion/guardian range checks

### L2. Many Cows + Rebellion
- [ ] Have 50+ cows nearby, trigger Rebellion → all pathfind toward player simultaneously
- [ ] Note any FPS/TPS impact

---

## Scoring Guide

For each test, mark:
- **PASS** — works as expected
- **FAIL** — broken, wrong behavior
- **AWKWARD** — works but feels wrong / needs balancing
- **CRASH** — game crashes (include log snippet)

Priority bugs: CRASH > FAIL > AWKWARD

---

## Quick Config Overrides for Testing

To force specific rarity rolls for testing rare outcomes:

```toml
# In mooofdoom-common.toml — set weights to force specific tiers:

# Force Mythic only:
[rarity]
commonWeight = 0
uncommonWeight = 0
rareWeight = 0
legendaryWeight = 0
mythicWeight = 1

# Force Legendary only:
[rarity]
commonWeight = 0
uncommonWeight = 0
rareWeight = 0
legendaryWeight = 1
mythicWeight = 0

# Reset to defaults:
[rarity]
commonWeight = 50
uncommonWeight = 30
rareWeight = 15
legendaryWeight = 4
mythicWeight = 1
```

To speed up periodic events for testing:
```toml
[chaos]
sizeChangeIntervalTicks = 200
explosionIntervalTicks = 200
moonJumpIntervalTicks = 200

[utility]
dropIntervalTicks = 200
```
