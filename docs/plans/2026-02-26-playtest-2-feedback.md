# Playtest 2 Feedback — Bug Reports, Improvements & Expansion Ideas

**Date:** 2026-02-26
**Build:** Post-playtest-1 implementation (all 8 tasks complete)
**Test Scenarios:** `docs/plans/2026-02-25-playtest-2-scenarios.md`

---

## Bugs, Fixes & Improvements

### 1. Rebellion Effect Missing Icon

**Issue:** The "Rebellion of the Cows" debuff has no icon in the player HUD. It shows as a blank/default effect slot, making it hard to notice when it's active.

**Desired:** A custom 18x18 icon — thematically a cow with a sword (angry cow motif).

**Root Cause:** `ModEffects.java` registers the Rebellion effect with color `0xCC4400` but no icon texture exists. NeoForge custom effects require a texture at `assets/mooofdoom/textures/mob_effect/rebellion.png` to render in the HUD. Without it, the effect slot appears blank or uses a missing-texture placeholder.

**Fix:**
- Create a 18x18 pixel icon texture at `src/main/resources/assets/mooofdoom/textures/mob_effect/rebellion.png`
- Art direction: red/orange-tinted cow silhouette with a sword — conveys "the cows are angry at you"
- The texture file name must match the effect's registry name (`rebellion`)

---

### 2. Guardian Effect Missing Icon

**Issue:** The "Guardian of the Cows" buff has no icon in the player HUD. Same blank slot problem as Rebellion.

**Desired:** A custom 18x18 icon — thematically a cow with a heart (friendly cow motif).

**Root Cause:** Same as Rebellion. `ModEffects.java` registers the Guardian effect with color `0x3399FF` but no texture at the expected path. Needs `assets/mooofdoom/textures/mob_effect/guardian.png`.

**Fix:**
- Create a 18x18 pixel icon texture at `src/main/resources/assets/mooofdoom/textures/mob_effect/guardian.png`
- Art direction: blue-tinted cow silhouette with a heart — conveys "the cows are protecting you"
- The texture file name must match the effect's registry name (`guardian`)

---

### 3. Advancements UI Missing Background Texture

**Issue:** Opening the advancements screen shows the "Moo of Doom" tab, but the advancement tree panel has no background texture — it's either black or uses a jarring default.

**Root Cause:** The root advancement (`data/mooofdoom/advancement/root.json`) is missing the `"background"` field in its `"display"` block. In Minecraft, the root advancement of a tab is responsible for defining the background texture that tiles behind the entire advancement tree. Without it, the tab has no background.

Current `root.json` display block:
```json
"display": {
    "icon": { "id": "mooofdoom:doom_apple", "count": 1 },
    "title": {"translate": "advancement.mooofdoom.root.title"},
    "description": {"translate": "advancement.mooofdoom.root.description"},
    "show_toast": false,
    "announce_to_chat": false
}
```

**Fix:** Add a `"background"` field to the root advancement's display. Use a vanilla texture for now; a custom texture can come later.

Recommended options (pick one):
- `"minecraft:textures/block/green_concrete.json"` — clean green, fits the pastoral cow theme
- `"minecraft:textures/block/moss_block.json"` — organic, nature feel
- `"minecraft:textures/block/hay_block_side.json"` — hay = cows, very thematic
- `"minecraft:textures/gui/advancements/backgrounds/stone.png"` — safe vanilla default

The hay block side texture is probably the most on-brand. Example fix:
```json
"display": {
    "icon": { "id": "mooofdoom:doom_apple", "count": 1 },
    "title": {"translate": "advancement.mooofdoom.root.title"},
    "description": {"translate": "advancement.mooofdoom.root.description"},
    "background": "minecraft:textures/block/hay_block_side.png",
    "show_toast": false,
    "announce_to_chat": false
}
```

---

### 4. All Cows Still Spawning as OP

**Issue:** During playtest, all cows were becoming OP on spawn, even though the default activation mode should be `ITEM_ACTIVATED`.

**Root Cause (likely — config persistence):** The code default for `ACTIVATION_MODE` was changed from `ALL_COWS` to `ITEM_ACTIVATED` after playtest 1 (in `ModConfig.java` line 62). However, NeoForge config files are written once and then persist. If the tester's `mooofdoom-common.toml` was generated while the old default was `ALL_COWS`, the saved config still has `mode = "ALL_COWS"` and won't pick up the new code default.

**How to verify:** Check the tester's `.minecraft/config/mooofdoom-common.toml` file. If it says `mode = "ALL_COWS"`, that confirms the stale config theory.

**Fix options:**
1. **Tester action:** Delete or edit `mooofdoom-common.toml` to regenerate with the new default
2. **Code improvement:** Add a config migration or version check that detects an outdated config — though this is non-trivial and may be overkill for a mod in active development
3. **Documentation:** Add a note to playtest instructions: "Delete your config file between playtest builds to pick up new defaults"

**If NOT a config issue:** The mythic natural spawn chance (`MYTHIC_SPAWN_CHANCE = 0.01`) fires on every cow join event in `ITEM_ACTIVATED` mode. At 1%, this should only affect ~1 in 100 cows. If significantly more are becoming OP, check whether the config value got corrupted or whether there's a chunk-reload loop causing `EntityJoinLevelEvent` to fire repeatedly for the same cow (the OP check on line 75 should prevent re-conversion, but it's worth logging to confirm).

---

### 5. OP Cow Death Explosion Missing Animal Dying Scream

**Issue:** When an OP cow dies, you hear the kaboom explosion sound but not the cow death scream. The death should feel layered — explosion + cow wail + totem sound.

**Root Cause:** The death handler (`OpCowDeathHandler.java`) does play `SoundEvents.COW_DEATH` (line 43), but two factors likely make it inaudible:
1. **Low pitch (0.5F):** The cow death sound is pitched down to half speed, making it a deep rumble that blends into the explosion sound. At 0.5F pitch, the distinctive "moo-wail" character of the cow death sound is lost.
2. **Volume competition:** The explosion plays at volume 4.0F while the cow death plays at 3.0F. Combined with the low pitch, the cow sound gets drowned out.
3. **Timing:** All three sounds (explosion, cow death, totem) fire on the same tick. The explosion is perceptually dominant because it has the highest volume and the most immediate attack.

**Fix suggestions:**
- Increase cow death sound volume to 4.0F (match the explosion)
- Raise pitch to 0.8F–1.0F so the cow scream is recognizable as a cow dying
- Consider adding a small delay (2-3 ticks) between the explosion and the cow scream so they don't overlap — this could be done with a scheduled task or by splitting the death event processing

Example quick fix in `OpCowDeathHandler.java`:
```java
// Cow death sound — louder and higher pitch so it's audible over the explosion
serverLevel.playSound(null, cow.getX(), cow.getY(), cow.getZ(),
        SoundEvents.COW_DEATH, SoundSource.HOSTILE,
        4.0F, 0.8F);  // was 3.0F, 0.5F
```

---

### 6. More Advancements Needed

**Issue:** The current advancement tree only covers item acquisition (root, doom apple, 5 buff buckets = 7 total). There are no advancements for the mod's core gameplay mechanics — combat, rebellion, guardian, loot, feeding, etc. This makes the advancement tab feel sparse and doesn't guide players through the mod's features.

**Suggested new advancements:**

| ID | Parent | Trigger | Title Idea | Description |
|----|--------|---------|------------|-------------|
| `first_op_cow` | `doom_apple` | Feed doom apple to cow (custom trigger or `player_interacted_with_entity`) | "Rise of the Moo-chine" | Create your first OP cow |
| `rebellion` | `first_op_cow` | Get Rebellion debuff (custom trigger) | "You've Made a Moo-stake" | Anger the cows and face their wrath |
| `guardian` | `first_op_cow` | Get Guardian buff (custom trigger) | "Herd Mentality" | Earn the cows' loyalty |
| `kill_op_cow` | `first_op_cow` | Kill an OP cow (entity_killed_player check or custom) | "Holy Cow!" | Defeat an OP cow |
| `cow_morph` | `doom_apple` | Trigger mythic cow morph (custom trigger) | "Udderly Transformed" | Experience the mythic cow morph |
| `convert_hostile` | `doom_apple` | Feed doom apple to hostile mob (custom trigger) | "Change of Heart" | Convert a hostile mob with a Doom Apple |
| `collect_all_buckets` | `doom_apple` | Have all 5 buff buckets (inventory_changed with all 5 items) | "The Full Dairy" | Collect every type of buff bucket |

**Notes:**
- Some of these need custom advancement triggers (NeoForge `CriterionTrigger` implementations), since vanilla triggers don't cover mod-specific events
- Custom triggers are a moderate amount of work per trigger but make the advancement tree much richer
- Consider a "challenge" frame for difficult ones like `cow_morph` (mythic = 1% chance) and `collect_all_buckets`

---

### 7. Cow Attacks Have Bad Aim

**Issue:** OP cow milk projectile attacks seem to miss the target frequently. We don't want aimbot accuracy, but they should feel competent.

**Root Cause analysis:**

**Milk Projectile (`MilkProjectileGoal.java`):**
- Line 43: `projectile.shoot(dx, dy, dz, 1.5F, 2.0F)` — the last parameter (`2.0F`) is the **inaccuracy** (random spread). For reference:
  - Skeleton (Hard difficulty): `14.0 - difficulty*4 = 2.0` inaccuracy — so the cow matches a hard-mode skeleton
  - Llama spit: `10.0` inaccuracy (very sloppy)
  - Blaze fireball: `0.0` inaccuracy (perfect aim)
- The speed parameter (`1.5F`) is moderate. Vanilla snowballs use `1.5F`, skeletons use `1.6F`.
- **No target leading:** The projectile aims at where the target IS, not where it will be. Against a moving player/mob, especially at 6+ block range, the projectile will consistently trail behind.
- At 6+ blocks with 2.0 inaccuracy and no leading, misses against moving targets are expected.

**Charge Attack (`ChargeGoal.java`):**
- Uses direct vector-to-target each tick, so it tracks moving targets well
- Should rarely miss unless target dodges at the last moment
- Probably not the source of complaints

**Fix suggestions:**
- Reduce milk projectile inaccuracy from `2.0F` to `1.0F` — makes shots tighter without being aimbot
- Increase projectile speed from `1.5F` to `1.8F` — less travel time = less drift from target movement
- Optionally add basic target leading: offset the aim point by `target.getDeltaMovement() * estimatedTravelTime` to predict where the target will be when the projectile arrives
- Keep some inaccuracy (never go below `0.5F`) — perfect aim feels unfair and unfun

---

## Expansion Ideas

### A. Taming Mechanics (Flute, Loyalty, etc.)

**Concept:** Introduce a "Cow Flute" item that lets players build a persistent bond with OP cows. Tamed cows follow their owner, respond to flute commands (stay, follow, attack target), and persist their loyalty across sessions via NBT.

**How it works:**
- **Cow Flute:** Crafted from gold ingots + note block + leather. Right-click an OP cow repeatedly (3-5 times) while holding wheat to begin a taming process. Each feeding has a % chance to succeed (similar to wolf/horse taming).
- **Loyalty levels:** Tamed cows start at Loyalty I and can be leveled up (see Cow Leveling below). Higher loyalty = faster response, larger follow range, and access to more flute commands.
- **Flute commands:** Right-click air with flute to cycle modes — Follow (default), Stay, Guard Area, Patrol (walk between two set points). Sneak+right-click to whistle all tamed cows in range to come.
- **Persistence:** Tamed cow stores owner UUID in NBT. Only the owner can command it. Tamed cows don't despawn and respawn in the same chunk on world load.

**Benefits for the mod:**
- **Player investment:** Taming creates emotional attachment to specific cows. Players name them, protect them, build bases around them. This transforms OP cows from disposable novelties into long-term companions.
- **Progression depth:** Taming adds a mid-to-late-game goal beyond "feed doom apple, receive OP cow." The flute introduces a crafting goal, and loyalty levels give players something to work toward over multiple play sessions.
- **Multiplayer dynamics:** "That's MY cow" moments. Players can show off their high-loyalty named cows. Creates social status and trading opportunities.
- **Content longevity:** Each taming mechanic (flute crafting, loyalty grinding, command mastery) adds hours of gameplay per cow. A player with 5 tamed cows at max loyalty has invested significantly in the mod.

---

### B. Cow Leveling System

**Concept:** OP cows can be leveled up by feeding them diamonds (or other valuable items). Each level increases their stats and unlocks better loot drops. Visual indicators show cow level (particle effects, aura color changes).

**How it works:**
- **Leveling:** Feed diamonds to an OP cow. Each diamond gives XP toward the next level. Levels 1-5, with exponentially increasing XP requirements (1 diamond for L2, 3 for L3, 8 for L4, 20 for L5).
- **Stat scaling per level:**
  - Health: 100 → 150 → 200 → 300 → 500
  - Attack damage: 10 → 15 → 20 → 30 → 50
  - Aura range: 10 → 12 → 15 → 20 → 30 blocks
  - Aura buffs: Higher levels add new effects (L3: Haste I, L4: Jump Boost I, L5: Absorption II)
- **Loot scaling:** Higher level cows have better rarity weights on their alive-drop and milking tables. A Level 5 cow might have 40/25/20/10/5 weights instead of 50/30/15/4/1, dramatically increasing rare+ drops.
- **Visual indicators:** Level shown as colored particles — L1 white, L2 yellow, L3 cyan, L4 purple, L5 rainbow. Higher level cows literally glow more impressively.

**Benefits for the mod:**
- **Resource sink:** Diamonds become a meaningful currency beyond tools/armor. Players face interesting choices — "Do I make a diamond pickaxe or level my cow?" This creates economic tension that extends playtime.
- **Sense of growth:** The mod currently has a flat power curve — an OP cow at minute 5 is the same as one at hour 50. Leveling creates visible, satisfying progression that keeps players engaged long-term.
- **Emergent difficulty scaling:** Higher level cows drop better loot, which funds more cow leveling, which creates stronger cows. This positive feedback loop is addictive in the same way villager trading and beacon construction are.
- **Endgame content:** Level 5 cows become prestige items. Getting one is a significant achievement that gives veteran players bragging rights and tangible in-game benefits.

---

### C. Cow Trading Mechanics

**Concept:** OP cows respond to players holding diamonds (or other valuable items) in their hand. Standing near an OP cow while holding a diamond triggers a "trade" — the cow consumes the diamond and produces a special item or enhanced loot drop.

**How it works:**
- **Passive trading:** Hold a diamond near an OP cow (within 3 blocks). The cow walks toward the player, makes a unique "interested" sound, and after a short channel (2-3 seconds), consumes the diamond and drops a reward.
- **Tiered trade items:** Different items yield different rewards:
  - Diamond → random buff bucket (guaranteed, no milking needed)
  - Gold ingot → random rare loot table roll
  - Emerald → guaranteed enchanted book (random enchantment)
  - Netherite scrap → exclusive "Cow Charm" trinket items (new item category)
- **Cooldown:** Each cow has a per-trade cooldown (5 minutes per cow) to prevent spam and encourage having multiple OP cows.
- **Trade memory:** Cows track how many trades they've done with a player. Higher trade count = slightly better odds on the tiered table (loyalty bonus).

**Benefits for the mod:**
- **Active engagement:** Trading gives players something to DO with their OP cows beyond passive farming. It's a direct, intentional interaction that feels rewarding.
- **Alternative progression path:** Players who prefer peaceful play over combat get a way to access rare materials. Not everyone wants to kill their OP cows for MOOCOW loot — trading lets them keep their cows alive and still progress.
- **Economy integration:** Integrating emeralds and gold into cow trading bridges the mod's economy with vanilla mechanics (villager trading, gold farms, piglin bartering). The mod feels less isolated and more part of the overall Minecraft progression.
- **Multiplayer economy:** Cows become shared resources in multiplayer. Groups can pool diamonds to trade with high-level cows, creating cooperative gameplay around cow management.

---

### D. Loot Drop Tuning (Netherite Upgrades)

**Concept:** Rebalance the OP cow loot tables to include more endgame-relevant drops, particularly netherite upgrade templates and smithing templates. This makes OP cows a viable alternative path to endgame gear.

**How it works:**
- **Expanded Legendary tier:** Add Netherite Upgrade Smithing Template to the Legendary drop pool alongside Netherite Scrap. This is significant because smithing templates are normally found only in bastion remnants.
- **New Mythic drops:** Rotate the Mythic drop between Doom Apple and other ultra-rare items — Enchanted Golden Apple (Notch Apple), Totem of Undying, or exclusive mod-only items.
- **Level-scaled loot (pairs with cow leveling):** Higher level cows unlock access to higher-tier drop tables. Level 1-2 cows max out at Rare tier drops. Level 3+ cows can roll Legendary. Level 5 cows can roll Mythic.
- **Alive-drop improvements:** Current alive-drops use the same table as death drops. Consider a separate alive-drop table that favors consumables and renewable resources, while death drops favor permanent upgrades.

**Benefits for the mod:**
- **Endgame relevance:** Right now the mod's loot peaks at "1x Netherite Scrap" (Legendary) and "1x Doom Apple" (Mythic). Smithing templates and notch apples make the mod relevant for players who have already reached the netherite tier.
- **Alternative progression:** Finding bastions and ancient cities for smithing templates is a major bottleneck in vanilla progression. Offering these through OP cows (at appropriately low drop rates) gives the mod strategic value beyond novelty.
- **Replayability:** More diverse loot tables mean each OP cow encounter feels different. Players don't know exactly what they'll get, which keeps farming engaging.
- **Balance lever:** Loot tables are the easiest thing to tune via config. Having more items in the tables gives modpack makers and server admins flexibility to customize the experience for their community.

---

### E. OP Cow Trait Propagation (Viral Conversion)

**Concept:** A single OP cow in a group can, over time, passively convert nearby vanilla cows into OP cows. This creates an organic "infection" mechanic where OP-ness spreads through cow herds.

**How it works:**
- **Proximity conversion:** Every ~10 minutes (configurable), an OP cow has a small chance (5-10%) to convert one vanilla cow within 8 blocks into an OP cow.
- **Conversion event:** When a vanilla cow is converted, it plays totem particles, a moo sound, and gains the OP tag + attributes. Similar visual to feeding a doom apple but happens passively.
- **Propagation limits:**
  - Max converted cows per OP cow: 3 (configurable). An OP cow that has converted 3 cows stops propagating.
  - "Naturally converted" cows have a `MooOfDoom_Converted` tag and cannot themselves propagate (only doom-apple-created or naturally-spawned OP cows can spread)
  - This prevents exponential explosion — it's a slow, bounded spread
- **Visual telegraph:** OP cows near vanilla cows occasionally emit particle trails toward the nearest vanilla cow (like enchanting table particles toward bookshelves). This signals that propagation is possible and gives players visual feedback.
- **Config toggle:** `PROPAGATION_ENABLED` (default true), `PROPAGATION_INTERVAL_TICKS`, `PROPAGATION_CHANCE`, `MAX_CONVERSIONS_PER_COW`

**Benefits for the mod:**
- **Living world feel:** The mod currently treats OP cows as static entities — they're OP or they're not. Propagation makes them feel alive and dynamic. A single OP cow discovered in the wild becomes the seed of a growing herd, creating emergent storytelling.
- **Discovery mechanic:** Players who find a naturally-spawned mythic OP cow (1% chance) now have a reason to pen it with vanilla cows and wait. This transforms a lucky find into a long-term investment, bridging early-game discovery with mid-game cow farming.
- **Risk/reward tension:** More OP cows = more loot and buffs, but also more Rebellion danger if something goes wrong. Players must balance herd growth with the risk of a full cow uprising. This creates genuine strategic decisions.
- **Passive gameplay loop:** Not all players want to actively grind doom apples. Propagation rewards patient players who set up cow pens and check back periodically. This "farming" loop is deeply Minecraft-native and feels right for a cow mod.
- **Server dynamics:** On multiplayer servers, an uncontrolled OP cow herd near spawn becomes a community event. Players either work together to manage it or suffer collective Rebellions. This creates memorable shared experiences.

---

## Priority Assessment

### Bugs/Fixes — Suggested Implementation Order

| Priority | Issue | Effort | Impact |
|----------|-------|--------|--------|
| **P1** | #4 All cows spawning as OP | Low (config/docs fix) | Blocks normal gameplay |
| **P2** | #5 Death explosion missing cow scream | Low (tweak 2 numbers) | Feels broken during key moment |
| **P2** | #3 Advancements missing background | Low (add 1 JSON field) | Visual polish |
| **P3** | #1 Rebellion icon missing | Medium (needs pixel art) | HUD polish |
| **P3** | #2 Guardian icon missing | Medium (needs pixel art) | HUD polish |
| **P3** | #7 Cow attack aim | Low-Medium (tweak constants, optional leading) | Combat feel |
| **P4** | #6 More advancements | Medium-High (needs custom triggers) | Content depth |

### Expansion Ideas — Suggested Development Order

| Order | Feature | Pairs With | Why This Order |
|-------|---------|------------|----------------|
| 1st | **Loot Drop Tuning** | Existing loot system | Lowest effort, highest immediate impact. Enriches existing tables without new systems. |
| 2nd | **Cow Leveling** | Loot tuning | Creates the progression backbone that other features plug into. Level-gated loot tables reward investment. |
| 3rd | **Taming Mechanics** | Cow leveling | Builds on leveling — loyalty becomes another axis of cow progression. Needs leveling in place first. |
| 4th | **Cow Trading** | Taming + leveling | Most impactful when cows have levels and loyalty. Trade rewards scale with cow investment. |
| 5th | **Trait Propagation** | All of the above | The capstone feature — makes OP cows feel like a living ecosystem. Most interesting when leveling, taming, and trading are all in play. |
