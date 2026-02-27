# Fabric Support via Architectury — Implementation Plan

**Goal:** Restructure Moo of Doom into a multi-loader project supporting both NeoForge and Fabric from a single codebase using Architectury Loom.

**Architecture:** Gradle multi-project with three modules — `common` (shared game logic), `neoforge` (NeoForge-specific wiring), and `fabric` (Fabric-specific wiring). Architectury API provides cross-platform abstractions for registries, events, and networking.

**Tech Stack:** Architectury Loom, Architectury API, Fabric API, Fabric Loader, NeoForge, Gradle 9.x, Java 21

---

## Key Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Multi-loader framework | Architectury Loom + API | Industry standard, active maintenance, handles registry/event abstraction |
| Config on Fabric | Custom JSON config (no extra dep) | Cloth Config is optional; simple JSON keeps dependency count low |
| Project structure | Root + 3 subprojects | Clean separation; `common` is a compile-only dependency for both loaders |
| CI | Separate build tasks per loader | Both JARs built and tested independently, both uploaded as artifacts |
| Existing tests | Stay in `common` module | Tests are pure JUnit 5 with no platform dependencies |

---

## Codebase Audit Summary

**Current file breakdown (30 Java files):**

| Category | Files | % | Migration Effort |
|----------|-------|---|-----------------|
| Pure vanilla (no NeoForge imports) | 6 | 20% | Move to `common/` as-is |
| Mostly vanilla (only `@SubscribeEvent` + event types) | 14 | 47% | Extract logic to `common/`, thin platform wrappers |
| Platform-specific (registries, config, entry point) | 10 | 33% | Rewrite per-platform using Architectury abstractions |

**NeoForge APIs requiring abstraction:**
- `DeferredRegister` / `DeferredItem` / `DeferredHolder` → Architectury `DeferredRegister` (drop-in)
- `@SubscribeEvent` + 6 event types → Architectury event hooks
- `ModConfigSpec` (55 config fields) → Platform-specific config impl behind shared interface
- `@Mod` entry point → `ModInitializer` (Fabric) / `@Mod` (NeoForge)
- `NeoForge.EVENT_BUS.register()` → Architectury event registration
- `@EventBusSubscriber(Dist.CLIENT)` → `@Environment(EnvType.CLIENT)` (Fabric)

**Resources (all vanilla-format, fully shareable):**
- `assets/mooofdoom/` — textures, models, lang files
- `data/mooofdoom/` — advancements, recipes, loot tables

**Platform-specific resources:**
- NeoForge: `META-INF/neoforge.mods.toml` (existing)
- Fabric: `fabric.mod.json` (new)

---

## Task 0: Gradle Multi-Project Scaffolding

**Goal:** Convert from single-project to Architectury multi-project build.

### Step 1: New root `settings.gradle`

```groovy
pluginManagement {
    repositories {
        maven { url "https://maven.architectury.dev/" }
        maven { url "https://maven.fabricmc.net/" }
        maven { url "https://maven.neoforged.net/releases/" }
        gradlePluginPortal()
    }
}

include 'common', 'fabric', 'neoforge'
```

### Step 2: New root `build.gradle`

- Apply `architectury-plugin` at root
- Set `minecraft` version, `architectury.id = "mooofdoom"`
- Define shared repositories (Maven Central, Architectury, Fabric, NeoForge)
- `subprojects {}` block: common Java 21 toolchain, shared dependencies

### Step 3: Module `build.gradle` files

**`common/build.gradle`:**
- Apply `architectury-java` plugin
- Loom with `architectury { common(["neoforge", "fabric"]) }`
- Dependencies: Minecraft, Architectury API (common), JUnit 5

**`neoforge/build.gradle`:**
- Apply `architectury-java` plugin + NeoForge loader
- Loom with `architectury { neoForge() }`
- Dependencies: `common` project, NeoForge, Architectury API (NeoForge)
- `processResources` for `neoforge.mods.toml` template expansion

**`fabric/build.gradle`:**
- Apply `architectury-java` plugin + Fabric loader
- Loom with `architectury { fabric() }`
- Dependencies: `common` project, Fabric Loader, Fabric API, Architectury API (Fabric)
- `processResources` for `fabric.mod.json` template expansion

### Step 4: Move `gradle.properties`

Add new properties:
```properties
architectury_version=<latest>
fabric_loader_version=<latest for 1.21.1>
fabric_api_version=<latest for 1.21.1>
```

Keep existing NeoForge and Minecraft properties.

---

## Task 1: Move Source Files to `common/`

**Goal:** Relocate all platform-independent code to `common/src/main/java/`.

### Pure vanilla files (move as-is):
```
common/src/main/java/com/github/netfallnetworks/mooofdoom/
├── rarity/
│   ├── RarityTier.java
│   └── TieredRandom.java
├── cow/combat/
│   ├── ChargeGoal.java
│   ├── HostileTargetGoal.java
│   └── MilkProjectileGoal.java
└── cow/utility/
    └── BuffBucketItem.java
```

### Handler logic files (move, strip `@SubscribeEvent`):

For each of the 14 handler files, the pattern is:
1. Move the file to `common/`
2. Remove `@SubscribeEvent` annotation
3. Remove NeoForge event imports
4. Change method signature from `onEvent(NeoForgeEvent event)` to accept vanilla parameters
5. The platform modules will call these methods from their event listeners

**Example transformation — `AuraHandler.java`:**

Before (NeoForge):
```java
@SubscribeEvent
public static void onCowTick(EntityTickEvent.Post event) {
    if (!(event.getEntity() instanceof Cow cow)) return;
    // ... aura logic using vanilla APIs ...
}
```

After (common):
```java
public static void onCowTick(Cow cow) {
    // ... identical aura logic ...
}
```

**Files to transform this way:**
- `AuraHandler.java` — strip `EntityTickEvent.Post`
- `CowMorphHandler.java` — strip `EntityTickEvent.Post`
- `DoomAppleUseHandler.java` — strip `PlayerInteractEvent.EntityInteract`
- `ExplosionHandler.java` — strip `EntityTickEvent.Post`
- `GuardianHandler.java` — strip `EntityInteract` + `EntityTickEvent.Post`
- `LootDropHandler.java` — strip `EntityTickEvent.Post`
- `MilkingHandler.java` — strip `EntityInteract`
- `MobConversionHandler.java` — strip `EntityTickEvent.Post`
- `MoonJumpHandler.java` — strip `EntityTickEvent.Post`
- `OpCowDeathHandler.java` — strip `LivingDeathEvent`
- `OpCowManager.java` — strip `EntityJoinLevelEvent` + `LivingIncomingDamageEvent`
- `RebellionHandler.java` — strip `LivingIncomingDamageEvent` + `LivingDeathEvent` + `EntityTickEvent.Post`
- `SizeChangeHandler.java` — strip `EntityTickEvent.Post`
- `CombatLootHandler.java` — strip `LivingIncomingDamageEvent` + `LivingDeathEvent`

### Items (move to common):
- `DoomAppleItem.java` — pure vanilla Item subclass
- `BuffBucketItem.java` — pure vanilla Item subclass

### Move tests to `common/`:
```
common/src/test/java/com/github/netfallnetworks/mooofdoom/
├── cow/utility/MoocowMultiplierTest.java
└── rarity/TieredRandomTest.java
```

---

## Task 2: Config Abstraction

**Goal:** Create a platform-agnostic config interface in `common/`, with NeoForge and Fabric implementations.

### Step 1: Create `common/.../config/ModConfigValues.java`

A simple class with static fields that both platforms populate at startup:

```java
public final class ModConfigValues {
    // Activation
    public static ActivationMode activationMode = ActivationMode.ITEM_ACTIVATED;
    // Combat
    public static int cowHealth = 100;
    public static double cowDamage = 15.0;
    public static boolean enableChargeAttack = true;
    // ... all 55 config fields as plain static fields with defaults ...
}
```

### Step 2: NeoForge config (`neoforge/.../NeoForgeConfig.java`)

Keep existing `ModConfigSpec`-based config. On config load/reload, copy values into `ModConfigValues`:

```java
public static void onConfigLoad() {
    ModConfigValues.cowHealth = COW_HEALTH.getAsInt();
    ModConfigValues.cowDamage = COW_DAMAGE.get();
    // ...
}
```

### Step 3: Fabric config (`fabric/.../FabricConfig.java`)

Simple JSON file config (`config/mooofdoom.json`):
- Read/write with Gson
- Populate `ModConfigValues` on load
- Auto-generate default config file on first run

### Step 4: Update all handler references

Find-replace all `ModConfig.FIELD.get()` / `ModConfig.FIELD.getAsInt()` calls in `common/` with `ModConfigValues.field` static field access.

---

## Task 3: Registry Abstraction

**Goal:** Use Architectury's `DeferredRegister` (cross-platform) or create a thin registration interface.

### Option A: Architectury DeferredRegister (recommended)

Architectury provides its own `DeferredRegister` that works on both platforms:

```java
// common/registry/ModItems.java
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(MooOfDoom.MODID, Registries.ITEM);

    public static final RegistrySupplier<DoomAppleItem> DOOM_APPLE =
        ITEMS.register("doom_apple", () -> new DoomAppleItem(new Item.Properties()...));

    public static void init() { ITEMS.register(); }
}
```

### Files to convert:
- `ModItems.java` — `DeferredRegister.Items` → Architectury `DeferredRegister<Item>`
- `ModEntityTypes.java` — `DeferredRegister<EntityType<?>>` → Architectury equivalent
- `ModEffects.java` — `DeferredRegister<MobEffect>` → Architectury equivalent
- `ModCriteriaTriggers.java` — `DeferredRegister<CriterionTrigger<?>>` → Architectury equivalent
- `ModSimpleTrigger.java` — should work as-is (vanilla `SimpleCriterionTrigger`)

### Step: Move converted registry files to `common/`

All registries live in `common/` using Architectury's cross-platform DeferredRegister.

---

## Task 4: Event Wiring — Platform Modules

**Goal:** Create thin event listener classes in `neoforge/` and `fabric/` that delegate to `common/` handler logic.

### NeoForge event wiring (`neoforge/.../NeoForgeEventHandler.java`)

Preserves current `@SubscribeEvent` pattern, delegates to common:

```java
public class NeoForgeEventHandler {
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Cow cow) {
            AuraHandler.onCowTick(cow);
            CowMorphHandler.onCowTick(cow);
            ExplosionHandler.onCowTick(cow);
            LootDropHandler.onCowTick(cow);
            MobConversionHandler.onCowTick(cow);
            MoonJumpHandler.onCowTick(cow);
            SizeChangeHandler.onCowTick(cow);
        }
        // ... other entity type checks ...
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        DoomAppleUseHandler.onEntityInteract(event.getEntity(), event.getTarget(), event.getHand(), event.getLevel());
        GuardianHandler.onEntityInteract(...);
        MilkingHandler.onEntityInteract(...);
    }

    // ... remaining events ...
}
```

### Fabric event wiring (`fabric/.../FabricEventHandler.java`)

Uses Fabric API callback system:

```java
public class FabricEventHandler {
    public static void init() {
        // Entity tick
        ServerEntityWorldChangeEvents.AFTER_ENTITY_CHANGE_WORLD.register(...);
        // Or use ServerTickEvents + entity iteration

        // Entity interact
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            DoomAppleUseHandler.onEntityInteract(player, entity, hand, world);
            GuardianHandler.onEntityInteract(...);
            MilkingHandler.onEntityInteract(...);
            return ActionResult.PASS;
        });

        // Entity join world
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            OpCowManager.onEntityJoinLevel(entity, world);
        });

        // Living damage — Fabric API's LivingEntityEvents or mixin
        // Living death — Fabric API's ServerLivingEntityEvents.AFTER_DEATH
    }
}
```

### Fabric event gaps (may need mixins):

| NeoForge Event | Fabric Equivalent | Notes |
|---|---|---|
| `EntityTickEvent.Post` | `ServerTickEvents.END_WORLD_TICK` + entity iteration, OR `ServerEntityEvents` | Slightly different pattern |
| `PlayerInteractEvent.EntityInteract` | `UseEntityCallback.EVENT` | Direct equivalent |
| `EntityJoinLevelEvent` | `ServerEntityEvents.ENTITY_LOAD` | Direct equivalent |
| `LivingIncomingDamageEvent` | `FabricLivingEntityEvents` or Mixin on `LivingEntity.hurt()` | May need mixin for cancel/modify |
| `LivingDeathEvent` | `ServerLivingEntityEvents.AFTER_DEATH` | Direct equivalent |
| `EntityRenderersEvent.RegisterRenderers` | `EntityRendererRegistry.register()` | Direct equivalent |

---

## Task 5: Entry Points

### NeoForge (`neoforge/.../NeoForgeMooOfDoom.java`)

```java
@Mod(MooOfDoom.MODID)
public class NeoForgeMooOfDoom {
    public NeoForgeMooOfDoom(IEventBus modEventBus, ModContainer modContainer) {
        MooOfDoom.init();  // common init (registries)
        modContainer.registerConfig(ModConfig.Type.COMMON, NeoForgeConfig.SPEC);
        NeoForge.EVENT_BUS.register(NeoForgeEventHandler.class);
    }
}
```

### Fabric (`fabric/.../FabricMooOfDoom.java`)

```java
public class FabricMooOfDoom implements ModInitializer {
    @Override
    public void onInitialize() {
        MooOfDoom.init();  // common init (registries)
        FabricConfig.load();
        FabricEventHandler.init();
    }
}
```

### Common (`common/.../MooOfDoom.java`)

```java
public class MooOfDoom {
    public static final String MODID = "mooofdoom";

    public static void init() {
        ModItems.init();
        ModEntityTypes.init();
        ModEffects.init();
        ModCriteriaTriggers.init();
    }
}
```

---

## Task 6: Client Setup

### NeoForge (`neoforge/.../client/NeoForgeClientSetup.java`)

Keep existing `@EventBusSubscriber(Dist.CLIENT)` pattern with `EntityRenderersEvent.RegisterRenderers`.

### Fabric (`fabric/.../client/FabricClientSetup.java`)

```java
public class FabricClientSetup implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(
            ModEntityTypes.MILK_PROJECTILE.get(),
            ThrownItemRenderer::new
        );
    }
}
```

Add to `fabric.mod.json`:
```json
"entrypoints": {
    "main": ["com.github.netfallnetworks.mooofdoom.fabric.FabricMooOfDoom"],
    "client": ["com.github.netfallnetworks.mooofdoom.fabric.client.FabricClientSetup"]
}
```

---

## Task 7: Platform-Specific Resources

### Fabric: Create `fabric/src/main/resources/fabric.mod.json`

```json
{
    "schemaVersion": 1,
    "id": "mooofdoom",
    "version": "${version}",
    "name": "Moo of Doom",
    "description": "Makes cows absurdly overpowered.",
    "authors": ["NetfallNetworks"],
    "contact": {},
    "license": "MIT",
    "icon": "assets/mooofdoom/icon.png",
    "environment": "*",
    "entrypoints": {
        "main": ["com.github.netfallnetworks.mooofdoom.fabric.FabricMooOfDoom"],
        "client": ["com.github.netfallnetworks.mooofdoom.fabric.client.FabricClientSetup"]
    },
    "depends": {
        "fabricloader": ">=0.16.0",
        "fabric-api": "*",
        "minecraft": "~1.21.1",
        "java": ">=21",
        "architectury": "*"
    }
}
```

### NeoForge: Move existing `neoforge.mods.toml` to `neoforge/src/main/templates/META-INF/`

### Shared resources: Move to `common/src/main/resources/`
- `assets/mooofdoom/` (textures, models, lang)
- `data/mooofdoom/` (advancements, recipes, loot tables)

---

## Task 8: CI Updates

### Update `build.yml`

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        loader: [neoforge, fabric]
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 }
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 21 }
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew ${{ matrix.loader }}:build --no-configuration-cache --stacktrace
      - uses: actions/upload-artifact@v4
        with:
          name: ${{ matrix.loader }}-jar
          path: ${{ matrix.loader }}/build/libs/*.jar
```

### Update `release.yml`

Build both JARs, attach both to the GitHub Release:
- `moo-of-doom-<version>-neoforge.jar`
- `moo-of-doom-<version>-fabric.jar`

---

## Task 9: Verify & Test

1. `./gradlew common:test --no-configuration-cache` — existing unit tests pass
2. `./gradlew neoforge:build --no-configuration-cache` — NeoForge JAR builds
3. `./gradlew fabric:build --no-configuration-cache` — Fabric JAR builds
4. Manual playtest: load each JAR in respective loader, verify OP cow mechanics work

---

## Final Project Structure

```
moo-of-doom/                            (renamed repo root)
├── settings.gradle                      (includes common, neoforge, fabric)
├── build.gradle                         (architectury plugin, shared config)
├── gradle.properties                    (versions for MC, NeoForge, Fabric, Architectury)
├── common/
│   ├── build.gradle
│   └── src/
│       ├── main/java/.../mooofdoom/
│       │   ├── MooOfDoom.java           (shared init, MODID constant)
│       │   ├── config/
│       │   │   └── ModConfigValues.java (static config fields)
│       │   ├── registry/
│       │   │   ├── ModItems.java        (Architectury DeferredRegister)
│       │   │   ├── ModEntityTypes.java
│       │   │   ├── ModEffects.java
│       │   │   ├── ModCriteriaTriggers.java
│       │   │   └── ModSimpleTrigger.java
│       │   ├── cow/                     (all handler logic, items, goals)
│       │   └── rarity/                  (RarityTier, TieredRandom)
│       ├── main/resources/
│       │   ├── assets/mooofdoom/        (textures, models, lang)
│       │   └── data/mooofdoom/          (advancements, recipes)
│       └── test/java/                   (existing unit tests)
├── neoforge/
│   ├── build.gradle
│   └── src/main/
│       ├── java/.../mooofdoom/neoforge/
│       │   ├── NeoForgeMooOfDoom.java   (@Mod entry point)
│       │   ├── NeoForgeConfig.java      (ModConfigSpec → ModConfigValues)
│       │   ├── NeoForgeEventHandler.java (event delegation)
│       │   └── client/
│       │       └── NeoForgeClientSetup.java
│       └── templates/META-INF/
│           └── neoforge.mods.toml
├── fabric/
│   ├── build.gradle
│   └── src/main/
│       ├── java/.../mooofdoom/fabric/
│       │   ├── FabricMooOfDoom.java     (ModInitializer)
│       │   ├── FabricConfig.java        (JSON → ModConfigValues)
│       │   ├── FabricEventHandler.java  (callback delegation)
│       │   └── client/
│       │       └── FabricClientSetup.java
│       └── resources/
│           └── fabric.mod.json
├── .github/workflows/
│   ├── build.yml                        (matrix: [neoforge, fabric])
│   └── release.yml                      (both JARs in release)
└── docs/
```

---

## Implementation Order

| Phase | Tasks | Estimated Complexity |
|-------|-------|---------------------|
| **Phase 1** | Task 0 (Gradle scaffolding) | High — build system is the hardest part |
| **Phase 2** | Task 1 (move common code) + Task 2 (config) + Task 3 (registries) | Medium — mostly mechanical refactoring |
| **Phase 3** | Task 4 (event wiring) + Task 5 (entry points) + Task 6 (client) | Medium — platform glue code |
| **Phase 4** | Task 7 (resources) + Task 8 (CI) | Low — config files |
| **Phase 5** | Task 9 (verify & test) | Low — but critical |

**Recommendation:** Start with Phase 1 and get `./gradlew build` working with empty subprojects before moving any code. The Gradle multi-project setup with Architectury Loom is the most error-prone step — everything else is straightforward refactoring once the build system works.
