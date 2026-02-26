# Security Review — Moo of Doom

**Date:** 2026-02-26
**Scope:** Full codebase, build pipeline, supply chain, and gameplay-abuse vectors

---

## Summary

The mod has a clean dependency footprint and solid NeoForge idioms overall.
The findings below are grouped into four categories — **Supply Chain / CI**,
**Server Stability / Abuse**, **Memory & State Management**, and **Code
Hardening** — each with concrete action items your team can pick up
independently.

---

## 1. Supply Chain & CI Hardening

### 1.1 — Add Gradle wrapper SHA-256 verification

**Severity:** High
**File:** `gradle/wrapper/gradle-wrapper.properties`

The wrapper properties enable `validateDistributionUrl` but do **not** include a
`distributionSha256Sum`. A compromised distribution mirror or MITM during
download could inject malicious code into every developer machine and CI run.

**Action:** Add the official SHA-256 checksum for Gradle 9.2.1:

```properties
distributionSha256Sum=<hash from https://gradle.org/release-checksums/>
```

Also consider running `gradle wrapper --gradle-version 9.2.1 --verify` to
verify the wrapper JAR itself.

---

### 1.2 — Pin GitHub Actions to commit SHAs

**Severity:** High
**Files:** `.github/workflows/build.yml`, `.github/workflows/release.yml`

All actions are pinned to major version tags (`@v4`). A compromised upstream
action could push a malicious `v4` tag at any time and your next CI run would
execute it. The `softprops/action-gh-release@v2` action is a third-party action
with `contents: write` permission — the highest-risk combination.

**Action:** Pin every `uses:` to the full commit SHA of the version you've
audited. Add a comment with the human-readable version for maintainability:

```yaml
- uses: actions/checkout@<sha>        # v4.2.2
- uses: softprops/action-gh-release@<sha>  # v2.0.9
```

---

### 1.3 — Add Gradle dependency verification metadata

**Severity:** Medium
**Missing file:** `gradle/verification-metadata.xml`

There is no dependency verification. All transitive dependencies pulled by the
NeoForge plugin, JUnit, and their dependency trees are trusted implicitly.

**Action:** Generate the initial verification file:

```bash
./gradlew --write-verification-metadata sha256 help
```

Commit the resulting `gradle/verification-metadata.xml` and enable
`<verify-metadata>true</verify-metadata>` in the config. Review entries before
committing — large initial diff is expected.

---

### 1.4 — Enable Dependabot or Renovate for automated dependency updates

**Severity:** Medium
**Missing file:** `.github/dependabot.yml`

No automated scanning for vulnerable dependency versions. A CVE in a transitive
dependency (e.g., a logging library pulled by NeoForge) could go unnoticed.

**Action:** Add `.github/dependabot.yml`:

```yaml
version: 2
updates:
  - package-ecosystem: "gradle"
    directory: "/"
    schedule:
      interval: "weekly"
  - package-ecosystem: "github-actions"
    directory: "/"
    schedule:
      interval: "weekly"
```

---

### 1.5 — Restrict workflow permissions to least privilege

**Severity:** Low
**File:** `.github/workflows/release.yml`

The release workflow grants `contents: write` at the **job** level, which is
correct, but there is no top-level `permissions: {}` default in the repository.
If a new workflow is added without explicit permissions it will inherit the
repository default (which may be overly permissive).

**Action:** Add a top-level default to each workflow file:

```yaml
permissions: {}   # deny all by default
```

Then grant the minimum needed per-job.

---

### 1.6 — Add a `SECURITY.md` responsible disclosure policy

**Severity:** Low
**Missing file:** `SECURITY.md`

No documented process for external reporters to disclose vulnerabilities.

**Action:** Create a `SECURITY.md` at the repo root with contact info and
expected response timelines.

---

## 2. Server Stability & Gameplay Abuse

### 2.1 — Unbounded entity search radius via config

**Severity:** High
**Files:** `ModConfig.java`, `AuraHandler.java`, `RebellionHandler.java`,
`GuardianHandler.java`

Several config values control `AABB.inflate()` radii used in
`getEntitiesOfClass()` calls. The maximum allowed values are generous:

| Config               | Max | Used in                          |
|----------------------|-----|----------------------------------|
| `DETECTION_RANGE`    | 64  | `HostileTargetGoal`              |
| `AURA_RANGE`         | 32  | `AuraHandler` (every 40 ticks)   |
| `REBELLION_RANGE`    | 64  | `RebellionHandler` (every 20 ticks) |
| `GUARDIAN_RANGE`     | 64  | `GuardianHandler` (every 20 ticks)  |

On a server with many OP cows and many players, an `inflate(64)` scan of all
entities within a 128-block cube on every tick cycle creates O(cows * entities)
load. A malicious player in `ALL_COWS` mode could breed hundreds of cows in a
small area to create a TPS-killing "lag machine."

**Action:**
- Lower maximum config caps (e.g., 32 max for tick-frequency scans).
- Add a per-chunk or global cap on the number of OP cows processed per tick.
- Consider a cooldown longer than 20 ticks for entity scanning in
  `RebellionHandler` and `GuardianHandler`, or batch processing with entity
  count limits.

---

### 2.2 — Explosion power configurable up to 6.0

**Severity:** Medium
**Files:** `ModConfig.java:128`, `ExplosionHandler.java:26-33`

`EXPLOSION_POWER` is capped at 6.0 (higher than a charged creeper at 6.0, same
as a bed in the Nether). Although `ExplosionInteraction.NONE` prevents block
damage, a power-6 explosion still deals entity damage in a large radius. Combined
with the random trigger (probability `1/EXPLOSION_INTERVAL_TICKS` per tick),
this can harm players and their items unexpectedly.

**Action:**
- Consider lowering the cap to 4.0 or add a separate config for combat vs.
  ambient explosion power.
- Evaluate whether the combat explosion (line 26-33) should exempt friendly
  players within the blast radius.

---

### 2.3 — Doom Apple enables hostile mob army farming

**Severity:** Medium
**Files:** `DoomAppleUseHandler.java`, `MobConversionHandler.java`

Feeding Doom Apples to hostile mobs on the COMMON/UNCOMMON tiers creates
30-second protector mobs with no per-player cooldown or cap. A player with a
stack of Doom Apples can rapidly convert dozens of hostiles into a personal army.
On a multiplayer server this is a griefing vector — amassing converted mobs that
fight other players' mobs and contribute to entity lag.

**Action:**
- Add a per-player cooldown on Doom Apple entity interactions (e.g., 5 seconds).
- Cap the number of active protectors per player (e.g., 5).
- Consider a config option to disable hostile-mob feeding on servers.

---

### 2.4 — Loot multiplier enables duplication-scale item generation

**Severity:** Medium
**Files:** `ModConfig.java:137`, `CombatLootHandler.java:54-81`

`MOOCOW_MULTIPLIER` is configurable up to 100. A 1-hit kill of an OP cow at
max config drops up to 100x beef and 100x leather, plus a rare loot roll. On a
server, this is an economy-breaking loot fountain.

**Action:**
- Lower the max to a more reasonable cap (e.g., 20).
- Or add a hard limit on total items dropped per death event regardless of
  multiplier.

---

### 2.5 — Periodic loot drops have no player-proximity check

**Severity:** Low
**File:** `LootDropHandler.java`

OP cows drop tiered loot periodically even when no player is nearby. In loaded
chunks (e.g., near spawn or in always-loaded chunks), items accumulate on the
ground, contributing to entity count and eventual lag.

**Action:**
- Only drop loot if a player is within a reasonable radius (e.g., 32 blocks).
- Or only drop if the chunk has active player occupants.

---

## 3. Memory & State Management

### 3.1 — Static `HashMap` entries can leak on entity unload

**Severity:** High
**Files:** `CombatLootHandler.java:31`, `CowMorphHandler.java:27`,
`MobConversionHandler.java:35`

Three handlers use static `HashMap` fields keyed by entity ID (`int`) or player
UUID to track state:

```java
// CombatLootHandler
private static final Map<Integer, Integer> hitCounts = new HashMap<>();

// CowMorphHandler
private static final Map<UUID, Integer> morphedPlayers = new HashMap<>();

// MobConversionHandler
private static final Map<Integer, ProtectorData> protectors = new HashMap<>();
```

**Problems:**
1. **Entity ID reuse:** Minecraft reuses entity IDs after entities are removed.
   If a cow is hit, then unloaded (chunk unloads) without dying, its ID stays in
   `hitCounts` forever. When a new entity later gets the same ID, it inherits
   stale hit-count data.
2. **No cleanup on server stop/dimension change:** `morphedPlayers` persists
   across dimension changes. If a morphed player changes dimension, the companion
   cow entity ID in the new dimension is meaningless.
3. **No size bound:** On a long-running server, these maps grow unboundedly as
   entities are hit and unloaded without dying.

**Action:**
- Hook into `EntityLeaveLevelEvent` (or `EntityRemoveEvent`) to clean up entries
  when entities leave the world.
- Add periodic pruning (e.g., every 1200 ticks, remove entries where the entity
  no longer exists).
- For `CowMorphHandler`, handle `PlayerChangedDimensionEvent` to end the morph
  or re-spawn the companion cow.
- Consider using `WeakReference` or NeoForge's entity capability/data attachment
  system instead of static maps keyed by ID.

---

### 3.2 — AI goals accumulate on repeated tag/reload cycles

**Severity:** Medium
**Files:** `OpCowManager.java:56-66`, `RebellionHandler.java:93-96`,
`GuardianHandler.java:66-68`

`addCombatGoals()` is called every time an OP cow joins a level
(`onEntityJoinLevel`). This can happen repeatedly when chunks load/unload. The
method calls `cow.goalSelector.addGoal()` without checking if the goal already
exists. Most Minecraft `GoalSelector` implementations silently add duplicates,
causing the same goal to run multiple times per tick and wasting CPU.

Similarly, `RebellionHandler` and `GuardianHandler` add `MeleeAttackGoal` and
`NearestAttackableTargetGoal` to vanilla cows every time they enter the
rebellion/guardian state, gated only by a tag check — but the tag persists
across chunk reloads.

**Action:**
- Before adding goals, check if the goal type already exists in the selector.
- Or clear goals of that type before re-adding.
- Consider using a tag or persistent data flag to indicate "goals already added."

---

### 3.3 — `RebellionHandler` adds permanent AI goals to vanilla cows

**Severity:** Medium
**File:** `RebellionHandler.java:93-96`

When rebellion ends, the code removes the `REBEL_TAG` and sets
`cow.setTarget(null)`, but the `MeleeAttackGoal` and
`NearestAttackableTargetGoal` added at line 95-96 are **never removed** from
the goal selector. This means vanilla cows permanently retain combat AI after
a single rebellion event. Over time, all cows that have ever been in a rebellion
zone become permanently combat-capable.

**Action:**
- Track and remove the added goals when the rebellion tag is cleared.
- Or use a wrapper goal that checks the tag before executing.

---

## 4. Code Hardening

### 4.1 — Missing `isClientSide()` guard in `MilkProjectile.onHitEntity()`

**Severity:** Medium
**File:** `MilkProjectile.java:34-41`

`onHitEntity()` applies damage and effects without checking `level().isClientSide()`.
While `onHit()` (line 47) does have the guard for `discard()`, the damage/effect
application should also be server-only to avoid double-processing or client-side
desync.

**Action:** Add a client-side early return at the top of `onHitEntity()`:

```java
if (level().isClientSide()) return;
```

---

### 4.2 — Unchecked cast to `ServerLevel` in `MoonJumpHandler`

**Severity:** Low
**Files:** `MoonJumpHandler.java:29`, `MoonJumpHandler.java:56`

The code casts `cow.level()` to `ServerLevel` directly:

```java
ServerLevel level = (ServerLevel) cow.level();
```

While there is an `isClientSide()` guard earlier in the method, if the control
flow is ever refactored such that this line is reachable on the client, it will
throw a `ClassCastException` and crash. The rest of the codebase consistently
uses `instanceof ServerLevel serverLevel` pattern checks.

**Action:** Replace direct casts with `instanceof` pattern matching for
consistency and safety:

```java
if (cow.level() instanceof ServerLevel level) {
    level.sendParticles(...);
}
```

---

### 4.3 — `MobConversionHandler.onMobTick()` has an unchecked cast to `ServerLevel`

**Severity:** Low
**File:** `MobConversionHandler.java:121`

Same pattern as 4.2:

```java
ServerLevel level = (ServerLevel) mob.level();
```

Protected by an earlier `isClientSide()` check, but inconsistent with the rest
of the codebase and fragile under refactoring.

**Action:** Use `instanceof` pattern matching.

---

### 4.4 — No input validation on `DoomAppleUseHandler` for non-Monster Mob entities

**Severity:** Low
**File:** `DoomAppleUseHandler.java:31-35`

The handler checks for `Cow` and `Monster` but not other `Mob` subtypes (e.g.,
`Villager`, `IronGolem`, `Wolf`). A player right-clicking a villager with a Doom
Apple currently does nothing (apple not consumed, no feedback). This is fine
functionally, but a player may be confused about why no interaction occurs.

**Action:** Consider consuming the apple and playing a failure sound for
non-cow, non-hostile mobs, or add a tooltip/advancement hint about valid targets.

---

### 4.5 — Test coverage gaps for server-abuse scenarios

**Severity:** Medium
**Files:** `src/test/java/` (only 2 test files)

Unit test coverage is limited to `TieredRandomTest` and `MoocowMultiplierTest`.
There are no tests for:

- Entity state map cleanup (finding 3.1)
- Goal duplication behavior (finding 3.2)
- Config boundary conditions (e.g., all weights set to 0 in rarity config)
- Edge cases in `CowMorphHandler` (morph during dimension change, death while
  morphed)

**Action:** Add unit tests for:

1. `TieredRandom.roll()` when all weights are 0 (currently throws or returns
   unexpected results).
2. `calculateMultiplier()` edge cases already tested — but add property-based
   tests for config boundary values.
3. Mock-based tests for map cleanup in `CombatLootHandler` and
   `MobConversionHandler`.

---

## Priority Summary

| # | Finding | Severity | Effort |
|---|---------|----------|--------|
| 1.1 | Gradle wrapper SHA-256 | High | Trivial |
| 1.2 | Pin GH Actions to SHAs | High | Low |
| 3.1 | Static HashMap leaks | High | Medium |
| 2.1 | Unbounded entity scan radius | High | Medium |
| 1.3 | Dependency verification metadata | Medium | Low |
| 1.4 | Dependabot / Renovate | Medium | Trivial |
| 3.2 | AI goal duplication | Medium | Medium |
| 3.3 | Permanent rebel AI goals | Medium | Medium |
| 4.1 | Missing `isClientSide` in projectile | Medium | Trivial |
| 2.2 | Explosion power cap | Medium | Trivial |
| 2.3 | Doom Apple army farming | Medium | Low |
| 2.4 | Loot multiplier cap | Medium | Trivial |
| 4.5 | Test coverage gaps | Medium | Medium |
| 2.5 | Loot drops without player proximity | Low | Low |
| 1.5 | Default workflow permissions | Low | Trivial |
| 1.6 | SECURITY.md | Low | Trivial |
| 4.2 | Unchecked ServerLevel cast (MoonJump) | Low | Trivial |
| 4.3 | Unchecked ServerLevel cast (MobConversion) | Low | Trivial |
| 4.4 | Non-target Doom Apple feedback | Low | Trivial |
