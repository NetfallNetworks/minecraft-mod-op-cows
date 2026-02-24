# Moo of Doom Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a NeoForge mod for Minecraft 1.21.11 that transforms cows into OP combat/utility/chaos entities.

**Architecture:** Vanilla cow behavior is modified at runtime via NeoForge events (`EntityJoinLevelEvent`, `EntityTickEvent`). Custom AI goals are injected into the cow's `goalSelector` and `targetSelector`. OP state is persisted via NBT data. All features are toggleable via NeoForge's `ModConfigSpec`. Two custom items (Doom Apple, Enchanted Milk) and one custom entity (Milk Projectile) are registered via `DeferredRegister`.

**Tech Stack:** Java 21, NeoForge 21.11.38-beta, ModDevGradle 2.0.140, Gradle, Minecraft 1.21.11

**Important 1.21.11 API note:** `ResourceLocation` has been renamed to `Identifier` in 1.21.11. Use `Identifier.fromNamespaceAndPath(namespace, path)` instead of `ResourceLocation.fromNamespaceAndPath()`. Entity save/load uses `ValueInput`/`ValueOutput` instead of `CompoundTag` for `readAdditionalSaveData`/`addAdditionalSaveData`.

---

### Task 1: Scaffold the NeoForge project from MDK template

**Files:**
- Create: `build.gradle`
- Create: `settings.gradle`
- Create: `gradle.properties`
- Create: `gradlew`, `gradlew.bat`, `gradle/wrapper/*`
- Create: `.gitignore`
- Create: `LICENSE`
- Create: `src/main/templates/META-INF/neoforge.mods.toml`
- Create: `src/main/resources/assets/mooofdoom/lang/en_us.json`
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java`

**Step 1: Initialize Gradle wrapper**

Download the NeoForge MDK 1.21.11 template files. The fastest way: clone the MDK repo as a starting point then replace the example mod files.

```bash
cd /c/Users/mxm58/OneDrive/Documents/code/minecraft/mod-op-cows
# Download the MDK template
git clone --depth 1 https://github.com/NeoForgeMDKs/MDK-1.21.11-ModDevGradle.git temp-mdk
# Copy Gradle wrapper and scripts
cp -r temp-mdk/gradle .
cp temp-mdk/gradlew temp-mdk/gradlew.bat .
cp temp-mdk/.gitattributes .
# Clean up
rm -rf temp-mdk
```

**Step 2: Create `.gitignore`**

```
# eclipse
bin
*.launch
.settings
.metadata
.classpath
.project

# idea
out
*.ipr
*.iws
*.iml
.idea

# gradle
build
.gradle

# other
eclipse
run
runs
run-data
repo

# OS
.DS_Store
Thumbs.db

# temp
temp-mdk
```

**Step 3: Create `settings.gradle`**

```gradle
pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '1.0.0'
}
```

**Step 4: Create `gradle.properties`**

```properties
# Gradle
org.gradle.jvmargs=-Xmx1G
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true

# Parchment mappings
parchment_minecraft_version=1.21.11
parchment_mappings_version=2025.12.20

# Minecraft / NeoForge
minecraft_version=1.21.11
minecraft_version_range=[1.21.11]
neo_version=21.11.38-beta

# Mod properties
mod_id=mooofdoom
mod_name=Moo of Doom
mod_license=MIT
mod_version=1.0.0
mod_group_id=com.github.netfallnetworks.mooofdoom
```

**Step 5: Create `build.gradle`**

```gradle
plugins {
    id 'java-library'
    id 'maven-publish'
    id 'net.neoforged.moddev' version '2.0.140'
    id 'idea'
}

tasks.named('wrapper', Wrapper).configure {
    distributionType = Wrapper.DistributionType.BIN
}

version = mod_version
group = mod_group_id

repositories {
}

base {
    archivesName = mod_id
}

// Minecraft 1.21.11 requires Java 21
java.toolchain.languageVersion = JavaLanguageVersion.of(21)

neoForge {
    version = project.neo_version

    parchment {
        mappingsVersion = project.parchment_mappings_version
        minecraftVersion = project.parchment_minecraft_version
    }

    runs {
        client {
            client()
            systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
        }

        server {
            server()
            programArgument '--nogui'
            systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
        }

        gameTestServer {
            type = "gameTestServer"
            systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
        }

        data {
            clientData()
            programArguments.addAll '--mod', project.mod_id, '--all', '--output', file('src/generated/resources/').getAbsolutePath(), '--existing', file('src/main/resources/').getAbsolutePath()
        }

        configureEach {
            systemProperty 'forge.logging.markers', 'REGISTRIES'
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        "${mod_id}" {
            sourceSet(sourceSets.main)
        }
    }
}

sourceSets.main.resources { srcDir 'src/generated/resources' }

configurations {
    runtimeClasspath.extendsFrom localRuntime
}

dependencies {
}

var generateModMetadata = tasks.register("generateModMetadata", ProcessResources) {
    var replaceProperties = [
            minecraft_version      : minecraft_version,
            minecraft_version_range: minecraft_version_range,
            neo_version            : neo_version,
            mod_id                 : mod_id,
            mod_name               : mod_name,
            mod_license            : mod_license,
            mod_version            : mod_version,
    ]
    inputs.properties replaceProperties
    expand replaceProperties
    from "src/main/templates"
    into "build/generated/sources/modMetadata"
}
sourceSets.main.resources.srcDir generateModMetadata
neoForge.ideSyncTask generateModMetadata

publishing {
    publications {
        register('mavenJava', MavenPublication) {
            from components.java
        }
    }
    repositories {
        maven {
            url "file://${project.projectDir}/repo"
        }
    }
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

idea {
    module {
        downloadSources = true
        downloadJavadoc = true
    }
}
```

**Step 6: Create `src/main/templates/META-INF/neoforge.mods.toml`**

```toml
license="${mod_license}"

[[mods]]
modId="${mod_id}"
version="${mod_version}"
displayName="${mod_name}"
description='''
Moo of Doom transforms ordinary cows into absurdly overpowered entities.
OP cows fight hostile mobs, drop rare loot, grant aura buffs, and exhibit
chaotic behaviors like teleportation, random explosions, and moon jumps.
'''

[[dependencies.${mod_id}]]
    modId="neoforge"
    type="required"
    versionRange="[${neo_version},)"
    ordering="NONE"
    side="BOTH"

[[dependencies.${mod_id}]]
    modId="minecraft"
    type="required"
    versionRange="${minecraft_version_range}"
    ordering="NONE"
    side="BOTH"
```

**Step 7: Create the main mod class**

Create `src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java`:

```java
package com.github.netfallnetworks.mooofdoom;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(MooOfDoom.MODID)
public class MooOfDoom {
    public static final String MODID = "mooofdoom";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MooOfDoom(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Moo of Doom loading...");
    }
}
```

**Step 8: Create `src/main/resources/assets/mooofdoom/lang/en_us.json`**

```json
{
}
```

**Step 9: Create MIT `LICENSE` file**

Standard MIT license with year 2026 and "NetfallNetworks".

**Step 10: Verify the build compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL (first build downloads dependencies, may take a few minutes)

**Step 11: Commit**

```bash
git add .gitignore .gitattributes LICENSE build.gradle settings.gradle gradle.properties \
  gradlew gradlew.bat gradle/ \
  src/main/templates/META-INF/neoforge.mods.toml \
  src/main/resources/assets/mooofdoom/lang/en_us.json \
  src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java
git commit -m "feat: scaffold NeoForge 1.21.11 mod project"
```

---

### Task 2: Add mod configuration system

**Files:**
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/ModConfig.java`
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java`

**Step 1: Create the config class**

Create `src/main/java/com/github/netfallnetworks/mooofdoom/ModConfig.java`:

```java
package com.github.netfallnetworks.mooofdoom;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ModConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // --- Activation ---
    public static final ModConfigSpec.EnumValue<ActivationMode> ACTIVATION_MODE;
    public static final ModConfigSpec.DoubleValue RARE_SPAWN_CHANCE;

    // --- Combat ---
    public static final ModConfigSpec.BooleanValue CHARGE_ATTACK_ENABLED;
    public static final ModConfigSpec.BooleanValue MILK_PROJECTILE_ENABLED;
    public static final ModConfigSpec.IntValue COW_HEALTH;
    public static final ModConfigSpec.IntValue COW_ATTACK_DAMAGE;
    public static final ModConfigSpec.IntValue DETECTION_RANGE;
    public static final ModConfigSpec.IntValue CHARGE_COOLDOWN_TICKS;

    // --- Utility ---
    public static final ModConfigSpec.BooleanValue ENCHANTED_MILK_ENABLED;
    public static final ModConfigSpec.BooleanValue RARE_DROPS_ENABLED;
    public static final ModConfigSpec.BooleanValue PASSIVE_AURA_ENABLED;
    public static final ModConfigSpec.IntValue AURA_RANGE;
    public static final ModConfigSpec.IntValue DROP_INTERVAL_TICKS;

    // --- Chaos ---
    public static final ModConfigSpec.BooleanValue TELEPORT_ENABLED;
    public static final ModConfigSpec.BooleanValue SIZE_CHANGE_ENABLED;
    public static final ModConfigSpec.BooleanValue EXPLOSION_ENABLED;
    public static final ModConfigSpec.BooleanValue MOON_JUMP_ENABLED;
    public static final ModConfigSpec.IntValue TELEPORT_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue TELEPORT_RANGE;
    public static final ModConfigSpec.IntValue SIZE_CHANGE_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue EXPLOSION_INTERVAL_TICKS;
    public static final ModConfigSpec.DoubleValue EXPLOSION_POWER;
    public static final ModConfigSpec.IntValue MOON_JUMP_INTERVAL_TICKS;

    static final ModConfigSpec SPEC;

    static {
        BUILDER.push("activation");
        ACTIVATION_MODE = BUILDER
                .comment("How cows become OP: ALL_COWS, ITEM_ACTIVATED, or RARE_SPAWN")
                .defineEnum("mode", ActivationMode.ALL_COWS);
        RARE_SPAWN_CHANCE = BUILDER
                .comment("Chance (0.0-1.0) for a cow to spawn as OP in RARE_SPAWN mode")
                .defineInRange("rareSpawnChance", 0.05, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("combat");
        CHARGE_ATTACK_ENABLED = BUILDER
                .comment("Enable charge attack")
                .define("chargeAttackEnabled", true);
        MILK_PROJECTILE_ENABLED = BUILDER
                .comment("Enable ranged milk projectile attack")
                .define("milkProjectileEnabled", true);
        COW_HEALTH = BUILDER
                .comment("OP cow max health (vanilla cow is 10)")
                .defineInRange("health", 100, 10, 1000);
        COW_ATTACK_DAMAGE = BUILDER
                .comment("OP cow melee attack damage")
                .defineInRange("attackDamage", 10, 1, 100);
        DETECTION_RANGE = BUILDER
                .comment("Range in blocks to detect hostile mobs")
                .defineInRange("detectionRange", 24, 8, 64);
        CHARGE_COOLDOWN_TICKS = BUILDER
                .comment("Cooldown between charge attacks in ticks (20 ticks = 1 second)")
                .defineInRange("chargeCooldownTicks", 100, 20, 600);
        BUILDER.pop();

        BUILDER.push("utility");
        ENCHANTED_MILK_ENABLED = BUILDER
                .comment("Enable enchanted milk from OP cows")
                .define("enchantedMilkEnabled", true);
        RARE_DROPS_ENABLED = BUILDER
                .comment("Enable periodic rare item drops")
                .define("rareDropsEnabled", true);
        PASSIVE_AURA_ENABLED = BUILDER
                .comment("Enable passive aura buffs for nearby players")
                .define("passiveAuraEnabled", true);
        AURA_RANGE = BUILDER
                .comment("Range in blocks for passive aura buffs")
                .defineInRange("auraRange", 10, 3, 32);
        DROP_INTERVAL_TICKS = BUILDER
                .comment("Average ticks between rare item drops (6000 = ~5 minutes)")
                .defineInRange("dropIntervalTicks", 6000, 200, 72000);
        BUILDER.pop();

        BUILDER.push("chaos");
        TELEPORT_ENABLED = BUILDER
                .comment("Enable random teleportation")
                .define("teleportEnabled", true);
        SIZE_CHANGE_ENABLED = BUILDER
                .comment("Enable random size changes")
                .define("sizeChangeEnabled", true);
        EXPLOSION_ENABLED = BUILDER
                .comment("Enable random explosions")
                .define("explosionEnabled", true);
        MOON_JUMP_ENABLED = BUILDER
                .comment("Enable random moon jumps")
                .define("moonJumpEnabled", true);
        TELEPORT_INTERVAL_TICKS = BUILDER
                .comment("Average ticks between teleports (1200 = ~60 seconds)")
                .defineInRange("teleportIntervalTicks", 1200, 100, 12000);
        TELEPORT_RANGE = BUILDER
                .comment("Max teleport range in blocks")
                .defineInRange("teleportRange", 15, 3, 32);
        SIZE_CHANGE_INTERVAL_TICKS = BUILDER
                .comment("Average ticks between size changes (2400 = ~2 minutes)")
                .defineInRange("sizeChangeIntervalTicks", 2400, 200, 12000);
        EXPLOSION_INTERVAL_TICKS = BUILDER
                .comment("Average ticks between random explosions (3600 = ~3 minutes)")
                .defineInRange("explosionIntervalTicks", 3600, 200, 72000);
        EXPLOSION_POWER = BUILDER
                .comment("Explosion power (TNT is 4.0, creeper is 3.0)")
                .defineInRange("explosionPower", 2.0, 0.5, 6.0);
        MOON_JUMP_INTERVAL_TICKS = BUILDER
                .comment("Average ticks between moon jumps (2400 = ~2 minutes)")
                .defineInRange("moonJumpIntervalTicks", 2400, 200, 12000);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public enum ActivationMode {
        ALL_COWS,
        ITEM_ACTIVATED,
        RARE_SPAWN
    }
}
```

**Step 2: Register config in main mod class**

Update `MooOfDoom.java` constructor to register the config:

```java
public MooOfDoom(IEventBus modEventBus, ModContainer modContainer) {
    LOGGER.info("Moo of Doom loading...");
    modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, ModConfig.SPEC);
}
```

**Step 3: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/ModConfig.java \
  src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java
git commit -m "feat: add mod configuration with all toggles and tunables"
```

---

### Task 3: Add item and entity type registries

**Files:**
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/registry/ModItems.java`
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/registry/ModEntityTypes.java`
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java`

**Step 1: Create item registry**

Create `src/main/java/com/github/netfallnetworks/mooofdoom/registry/ModItems.java`:

```java
package com.github.netfallnetworks.mooofdoom.registry;

import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.item.Item;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MooOfDoom.MODID);

    public static final DeferredItem<Item> DOOM_APPLE = ITEMS.registerSimpleItem(
            "doom_apple",
            p -> p.stacksTo(16).rarity(net.minecraft.world.item.Rarity.EPIC)
    );

    // Enchanted Milk will be registered in a later task when the custom item class is ready
}
```

**Step 2: Create entity type registry**

Create `src/main/java/com/github/netfallnetworks/mooofdoom/registry/ModEntityTypes.java`:

```java
package com.github.netfallnetworks.mooofdoom.registry;

import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MooOfDoom.MODID);

    // Milk Projectile entity will be registered in a later task when the entity class is ready
}
```

**Step 3: Register deferred registers in main mod class**

Update `MooOfDoom.java`:

```java
package com.github.netfallnetworks.mooofdoom;

import com.github.netfallnetworks.mooofdoom.registry.ModEntityTypes;
import com.github.netfallnetworks.mooofdoom.registry.ModItems;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(MooOfDoom.MODID)
public class MooOfDoom {
    public static final String MODID = "mooofdoom";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MooOfDoom(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Moo of Doom loading...");
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, ModConfig.SPEC);

        ModItems.ITEMS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
    }
}
```

**Step 4: Update en_us.json with item names**

Update `src/main/resources/assets/mooofdoom/lang/en_us.json`:

```json
{
    "item.mooofdoom.doom_apple": "Doom Apple",
    "item.mooofdoom.enchanted_milk": "Enchanted Milk",
    "entity.mooofdoom.milk_projectile": "Milk Projectile"
}
```

**Step 5: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/registry/ \
  src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java \
  src/main/resources/assets/mooofdoom/lang/en_us.json
git commit -m "feat: add item and entity type registries"
```

---

### Task 4: Implement OP cow activation and attribute boosting

**Files:**
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/OpCowManager.java`
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java`

**Step 1: Create OpCowManager**

This is the core class that handles making cows OP. It listens for `EntityJoinLevelEvent` to activate cows and boost their attributes.

Create `src/main/java/com/github/netfallnetworks/mooofdoom/cow/OpCowManager.java`:

```java
package com.github.netfallnetworks.mooofdoom.cow;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public class OpCowManager {

    public static final String OP_TAG = "MooOfDoom";

    /**
     * Check if a cow is already tagged as OP.
     */
    public static boolean isOpCow(Cow cow) {
        return cow.getTags().contains(OP_TAG);
    }

    /**
     * Tag a cow as OP and boost its attributes.
     */
    public static void makeOpCow(Cow cow) {
        cow.addTag(OP_TAG);
        boostAttributes(cow);
        cow.setGlowingTag(true);
    }

    private static void boostAttributes(Cow cow) {
        // Health
        AttributeInstance healthAttr = cow.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(ModConfig.COW_HEALTH.getAsInt());
            cow.setHealth(cow.getMaxHealth()); // Heal to full
        }

        // Movement speed (vanilla cow is 0.2)
        AttributeInstance speedAttr = cow.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(0.35);
        }

        // Knockback resistance
        AttributeInstance kbAttr = cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kbAttr != null) {
            kbAttr.setBaseValue(0.8);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Cow cow)) return;
        if (event.getLevel().isClientSide()) return;

        // If already OP, just re-apply attributes (they reset on load)
        if (isOpCow(cow)) {
            boostAttributes(cow);
            cow.setGlowingTag(true);
            return;
        }

        // Determine if this cow should become OP
        ModConfig.ActivationMode mode = ModConfig.ACTIVATION_MODE.get();
        switch (mode) {
            case ALL_COWS -> makeOpCow(cow);
            case RARE_SPAWN -> {
                if (cow.getRandom().nextDouble() < ModConfig.RARE_SPAWN_CHANCE.get()) {
                    makeOpCow(cow);
                }
            }
            case ITEM_ACTIVATED -> {
                // Handled by item use, not here
            }
        }
    }
}
```

**Step 2: Register the event handler in MooOfDoom.java**

Add to the `MooOfDoom` constructor:

```java
NeoForge.EVENT_BUS.register(OpCowManager.class);
```

Add the import:
```java
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import net.neoforged.neoforge.common.NeoForge;
```

**Step 3: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 4: Manual test**

Run: `./gradlew runClient`
Expected: In-game, spawn cows with `/summon cow` - they should have glowing effect and 100 HP (check with F3 + entity targeting or damage testing).

**Step 5: Commit**

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/OpCowManager.java \
  src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java
git commit -m "feat: add OP cow activation system with attribute boosting"
```

---

### Task 5: Implement Doom Apple item activation

**Files:**
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/DoomAppleUseHandler.java`
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java`
- Create: `src/main/resources/data/mooofdoom/recipe/doom_apple.json`

**Step 1: Create interaction handler**

Create `src/main/java/com/github/netfallnetworks/mooofdoom/cow/DoomAppleUseHandler.java`:

```java
package com.github.netfallnetworks.mooofdoom.cow;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.Cow;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class DoomAppleUseHandler {

    @SubscribeEvent
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (ModConfig.ACTIVATION_MODE.get() != ModConfig.ActivationMode.ITEM_ACTIVATED) return;
        if (!(event.getTarget() instanceof Cow cow)) return;
        if (!event.getItemStack().is(ModItems.DOOM_APPLE.get())) return;
        if (OpCowManager.isOpCow(cow)) return;

        // Consume the item
        if (!event.getEntity().getAbilities().instabuild) {
            event.getItemStack().shrink(1);
        }

        // Transform the cow
        OpCowManager.makeOpCow(cow);

        // Effects
        ServerLevel serverLevel = (ServerLevel) event.getLevel();
        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                cow.getX(), cow.getY() + 1, cow.getZ(),
                50, 0.5, 1.0, 0.5, 0.2);
        cow.playSound(SoundEvents.TOTEM_OF_UNDYING_ACTIVATE, 1.0F, 1.0F);
    }
}
```

**Step 2: Register the event handler**

Add to `MooOfDoom` constructor:

```java
NeoForge.EVENT_BUS.register(DoomAppleUseHandler.class);
```

Add the import:
```java
import com.github.netfallnetworks.mooofdoom.cow.DoomAppleUseHandler;
```

**Step 3: Create crafting recipe**

Create `src/main/resources/data/mooofdoom/recipe/doom_apple.json`:

```json
{
    "type": "minecraft:crafting_shapeless",
    "ingredients": [
        { "item": "minecraft:golden_apple" },
        { "item": "minecraft:nether_star" },
        { "item": "minecraft:milk_bucket" }
    ],
    "result": {
        "id": "mooofdoom:doom_apple",
        "count": 1
    }
}
```

**Step 4: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/DoomAppleUseHandler.java \
  src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java \
  src/main/resources/data/mooofdoom/recipe/doom_apple.json
git commit -m "feat: add Doom Apple item to activate OP cows"
```

---

### Task 6: Implement auto-aggression targeting

**Files:**
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/combat/HostileTargetGoal.java`
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/OpCowManager.java`

**Step 1: Create the targeting goal**

Create `src/main/java/com/github/netfallnetworks/mooofdoom/cow/combat/HostileTargetGoal.java`:

```java
package com.github.netfallnetworks.mooofdoom.cow.combat;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;

/**
 * Makes OP cows target hostile mobs within detection range.
 */
public class HostileTargetGoal extends NearestAttackableTargetGoal<Monster> {

    public HostileTargetGoal(Mob mob) {
        super(mob, Monster.class, ModConfig.DETECTION_RANGE.getAsInt(), true, false, null);
    }
}
```

**Step 2: Inject goals into OP cows**

Add a method to `OpCowManager` that injects combat goals when a cow becomes OP. Modify `OpCowManager.java` to add goals after `makeOpCow`:

Add this method:

```java
public static void addCombatGoals(Cow cow) {
    // Targeting: find nearest hostile mob
    cow.targetSelector.addGoal(1, new HostileTargetGoal(cow));
}
```

Call `addCombatGoals(cow)` at the end of `makeOpCow()` and in the re-apply path of `onEntityJoinLevel`.

Add the import:
```java
import com.github.netfallnetworks.mooofdoom.cow.combat.HostileTargetGoal;
```

**Step 3: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/combat/HostileTargetGoal.java \
  src/main/java/com/github/netfallnetworks/mooofdoom/cow/OpCowManager.java
git commit -m "feat: add auto-aggression targeting for OP cows"
```

---

### Task 7: Implement charge attack

**Files:**
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/combat/ChargeGoal.java`
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/OpCowManager.java`

**Step 1: Create the charge attack goal**

Create `src/main/java/com/github/netfallnetworks/mooofdoom/cow/combat/ChargeGoal.java`:

```java
package com.github.netfallnetworks.mooofdoom.cow.combat;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Cow charges at its target, dealing damage and heavy knockback on impact.
 */
public class ChargeGoal extends Goal {
    private final Cow cow;
    private LivingEntity target;
    private int cooldownTicks;
    private boolean charging;

    public ChargeGoal(Cow cow) {
        this.cow = cow;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!ModConfig.CHARGE_ATTACK_ENABLED.getAsBoolean()) return false;
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }
        target = cow.getTarget();
        if (target == null || !target.isAlive()) return false;
        double dist = cow.distanceToSqr(target);
        // Charge when between 4 and 16 blocks away
        return dist >= 16.0 && dist <= 256.0;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && charging && cow.distanceToSqr(target) > 2.0;
    }

    @Override
    public void start() {
        charging = true;
    }

    @Override
    public void stop() {
        charging = false;
        cooldownTicks = ModConfig.CHARGE_COOLDOWN_TICKS.getAsInt();
        target = null;
    }

    @Override
    public void tick() {
        if (target == null) return;

        cow.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // Sprint toward target at boosted speed
        Vec3 direction = target.position().subtract(cow.position()).normalize();
        cow.setDeltaMovement(direction.x * 0.8, cow.getDeltaMovement().y, direction.z * 0.8);

        // Check for impact (within 2 blocks)
        if (cow.distanceToSqr(target) < 4.0) {
            target.hurt(cow.damageSources().mobAttack(cow), ModConfig.COW_ATTACK_DAMAGE.getAsInt());
            // Heavy knockback
            Vec3 kb = direction.scale(3.0);
            target.push(kb.x, 0.5, kb.z);
            charging = false;
        }
    }
}
```

**Step 2: Register the goal**

Add to `OpCowManager.addCombatGoals()`:

```java
if (ModConfig.CHARGE_ATTACK_ENABLED.getAsBoolean()) {
    cow.goalSelector.addGoal(2, new ChargeGoal(cow));
}
```

Add the import:
```java
import com.github.netfallnetworks.mooofdoom.cow.combat.ChargeGoal;
```

**Step 3: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/combat/ChargeGoal.java \
  src/main/java/com/github/netfallnetworks/mooofdoom/cow/OpCowManager.java
git commit -m "feat: add charge attack combat goal"
```

---

### Task 8: Implement milk projectile entity and ranged attack

**Files:**
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/combat/MilkProjectile.java`
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/combat/MilkProjectileGoal.java`
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/registry/ModEntityTypes.java`
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/OpCowManager.java`

**Step 1: Register the entity type**

Update `ModEntityTypes.java` to add the milk projectile registration:

```java
// Add after the existing DeferredRegister declaration
public static final Supplier<EntityType<MilkProjectile>> MILK_PROJECTILE =
        ENTITY_TYPES.register("milk_projectile",
                () -> EntityType.Builder.<MilkProjectile>of(MilkProjectile::new, MobCategory.MISC)
                        .sized(0.25F, 0.25F)
                        .clientTrackingRange(4)
                        .updateInterval(10)
                        .build(ResourceKey.create(Registries.ENTITY_TYPE,
                                Identifier.fromNamespaceAndPath(MooOfDoom.MODID, "milk_projectile"))));
```

Add the necessary imports for `MilkProjectile`, `Supplier`, `ResourceKey`, `Registries`, `Identifier`.

**Step 2: Create the milk projectile entity**

Create `src/main/java/com/github/netfallnetworks/mooofdoom/cow/combat/MilkProjectile.java`:

```java
package com.github.netfallnetworks.mooofdoom.cow.combat;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.registry.ModEntityTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class MilkProjectile extends ThrowableProjectile {

    public MilkProjectile(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    public MilkProjectile(Level level, LivingEntity shooter) {
        super(ModEntityTypes.MILK_PROJECTILE.get(), shooter, level);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (result.getEntity() instanceof LivingEntity target) {
            target.hurt(damageSources().mobProjectile(this, (LivingEntity) getOwner()),
                    ModConfig.COW_ATTACK_DAMAGE.getAsInt() / 2.0F);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }
        discard();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide()) {
            discard();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            level().addParticle(ParticleTypes.DRIPPING_WATER,
                    getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // No extra synched data needed
    }
}
```

**Step 3: Create the ranged attack goal**

Create `src/main/java/com/github/netfallnetworks/mooofdoom/cow/combat/MilkProjectileGoal.java`:

```java
package com.github.netfallnetworks.mooofdoom.cow.combat;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cow;

import java.util.EnumSet;

/**
 * Cow shoots milk projectiles at targets that are too far for melee.
 */
public class MilkProjectileGoal extends Goal {
    private final Cow cow;
    private int cooldown;

    public MilkProjectileGoal(Cow cow) {
        this.cow = cow;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!ModConfig.MILK_PROJECTILE_ENABLED.getAsBoolean()) return false;
        LivingEntity target = cow.getTarget();
        if (target == null || !target.isAlive()) return false;
        // Use ranged when target is 6+ blocks away
        return cow.distanceToSqr(target) > 36.0;
    }

    @Override
    public void tick() {
        LivingEntity target = cow.getTarget();
        if (target == null) return;

        cow.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (cooldown <= 0) {
            MilkProjectile projectile = new MilkProjectile(cow.level(), cow);
            double dx = target.getX() - cow.getX();
            double dy = target.getEyeY() - cow.getEyeY();
            double dz = target.getZ() - cow.getZ();
            projectile.shoot(dx, dy, dz, 1.5F, 2.0F);
            cow.level().addFreshEntity(projectile);
            cooldown = 40; // 2 second cooldown
        } else {
            cooldown--;
        }
    }
}
```

**Step 4: Register goals in OpCowManager**

Add to `addCombatGoals()`:

```java
if (ModConfig.MILK_PROJECTILE_ENABLED.getAsBoolean()) {
    cow.goalSelector.addGoal(3, new MilkProjectileGoal(cow));
}
```

Add the import:
```java
import com.github.netfallnetworks.mooofdoom.cow.combat.MilkProjectileGoal;
```

**Step 5: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/combat/MilkProjectile.java \
  src/main/java/com/github/netfallnetworks/mooofdoom/cow/combat/MilkProjectileGoal.java \
  src/main/java/com/github/netfallnetworks/mooofdoom/registry/ModEntityTypes.java \
  src/main/java/com/github/netfallnetworks/mooofdoom/cow/OpCowManager.java
git commit -m "feat: add milk projectile entity and ranged attack goal"
```

---

### Task 9: Implement enchanted milk item

**Files:**
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/utility/EnchantedMilkItem.java`
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/utility/MilkingHandler.java`
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/registry/ModItems.java`
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java`

**Step 1: Create EnchantedMilkItem**

Create `src/main/java/com/github/netfallnetworks/mooofdoom/cow/utility/EnchantedMilkItem.java`:

```java
package com.github.netfallnetworks.mooofdoom.cow.utility;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

public class EnchantedMilkItem extends Item {

    private static final List<MobEffectInstance> POSSIBLE_EFFECTS = List.of(
            new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 1),      // Strength II for 60s
            new MobEffectInstance(MobEffects.REGENERATION, 600, 1),       // Regen II for 30s
            new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0),   // Fire Res for 60s
            new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 1)     // Speed II for 60s
    );

    public EnchantedMilkItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player player) {
            // Clear negative effects like regular milk
            player.removeAllEffects();

            // Grant 1-2 random positive effects
            int count = 1 + level.getRandom().nextInt(2);
            List<MobEffectInstance> shuffled = new java.util.ArrayList<>(POSSIBLE_EFFECTS);
            java.util.Collections.shuffle(shuffled, new java.util.Random(level.getRandom().nextLong()));
            for (int i = 0; i < Math.min(count, shuffled.size()); i++) {
                MobEffectInstance template = shuffled.get(i);
                player.addEffect(new MobEffectInstance(template));
            }

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                player.getInventory().add(new ItemStack(Items.BUCKET));
            }
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32; // Same as milk bucket
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public net.minecraft.world.InteractionResult use(Level level, Player player,
            net.minecraft.world.InteractionHand hand) {
        player.startUsingItem(hand);
        return net.minecraft.world.InteractionResult.CONSUME;
    }
}
```

**Step 2: Register the item in ModItems**

Add to `ModItems.java`:

```java
public static final DeferredItem<EnchantedMilkItem> ENCHANTED_MILK = ITEMS.registerItem(
        "enchanted_milk",
        EnchantedMilkItem::new,
        new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)
);
```

Add the import:
```java
import com.github.netfallnetworks.mooofdoom.cow.utility.EnchantedMilkItem;
import net.minecraft.world.item.Item;
```

**Step 3: Create milking handler**

Create `src/main/java/com/github/netfallnetworks/mooofdoom/cow/utility/MilkingHandler.java`:

```java
package com.github.netfallnetworks.mooofdoom.cow.utility;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import com.github.netfallnetworks.mooofdoom.registry.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class MilkingHandler {

    @SubscribeEvent
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!ModConfig.ENCHANTED_MILK_ENABLED.getAsBoolean()) return;
        if (!(event.getTarget() instanceof Cow cow)) return;
        if (!OpCowManager.isOpCow(cow)) return;
        if (!event.getItemStack().is(Items.BUCKET)) return;

        // Replace normal milking with enchanted milk
        event.setCanceled(true);

        if (!event.getEntity().getAbilities().instabuild) {
            event.getItemStack().shrink(1);
        }
        event.getEntity().getInventory().add(new ItemStack(ModItems.ENCHANTED_MILK.get()));
        cow.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
    }
}
```

**Step 4: Register the handler in MooOfDoom.java**

Add to constructor:
```java
NeoForge.EVENT_BUS.register(MilkingHandler.class);
```

Add import:
```java
import com.github.netfallnetworks.mooofdoom.cow.utility.MilkingHandler;
```

**Step 5: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/utility/ \
  src/main/java/com/github/netfallnetworks/mooofdoom/registry/ModItems.java \
  src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java
git commit -m "feat: add enchanted milk item and milking handler"
```

---

### Task 10: Implement rare loot drops

**Files:**
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/utility/LootDropHandler.java`
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java`

**Step 1: Create loot drop handler**

Create `src/main/java/com/github/netfallnetworks/mooofdoom/cow/utility/LootDropHandler.java`:

```java
package com.github.netfallnetworks.mooofdoom.cow.utility;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public class LootDropHandler {

    private static final List<ItemStack> LOOT_TABLE = List.of(
            new ItemStack(Items.DIAMOND),
            new ItemStack(Items.EMERALD, 3),
            new ItemStack(Items.NETHERITE_SCRAP),
            new ItemStack(Items.GOLD_INGOT, 5),
            new ItemStack(Items.IRON_INGOT, 8),
            new ItemStack(Items.LAPIS_LAZULI, 10)
    );

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Cow cow)) return;
        if (cow.level().isClientSide()) return;
        if (!OpCowManager.isOpCow(cow)) return;
        if (!ModConfig.RARE_DROPS_ENABLED.getAsBoolean()) return;

        // Random chance each tick based on configured interval
        if (cow.getRandom().nextInt(ModConfig.DROP_INTERVAL_TICKS.getAsInt()) != 0) return;

        // Pick random loot
        ItemStack loot = LOOT_TABLE.get(cow.getRandom().nextInt(LOOT_TABLE.size())).copy();
        ItemEntity itemEntity = new ItemEntity(cow.level(),
                cow.getX(), cow.getY() + 0.5, cow.getZ(), loot);
        cow.level().addFreshEntity(itemEntity);

        // Sparkle effect
        ServerLevel serverLevel = (ServerLevel) cow.level();
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                cow.getX(), cow.getY() + 1, cow.getZ(),
                10, 0.5, 0.5, 0.5, 0.0);
        cow.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5F, 1.0F);
    }
}
```

**Step 2: Register the handler**

Add to `MooOfDoom` constructor:
```java
NeoForge.EVENT_BUS.register(LootDropHandler.class);
```

Add import:
```java
import com.github.netfallnetworks.mooofdoom.cow.utility.LootDropHandler;
```

**Step 3: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/utility/LootDropHandler.java \
  src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java
git commit -m "feat: add periodic rare loot drops from OP cows"
```

---

### Task 11: Implement passive aura buffs

**Files:**
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/utility/AuraHandler.java`
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java`

**Step 1: Create aura handler**

Create `src/main/java/com/github/netfallnetworks/mooofdoom/cow/utility/AuraHandler.java`:

```java
package com.github.netfallnetworks.mooofdoom.cow.utility;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public class AuraHandler {

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Cow cow)) return;
        if (cow.level().isClientSide()) return;
        if (!OpCowManager.isOpCow(cow)) return;
        if (!ModConfig.PASSIVE_AURA_ENABLED.getAsBoolean()) return;

        // Only check every 40 ticks (2 seconds) for performance
        if (cow.tickCount % 40 != 0) return;

        int range = ModConfig.AURA_RANGE.getAsInt();
        AABB auraBox = cow.getBoundingBox().inflate(range);
        List<Player> nearbyPlayers = cow.level().getEntitiesOfClass(Player.class, auraBox);

        for (Player player : nearbyPlayers) {
            // Duration slightly longer than check interval to avoid flickering
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 60, 0, true, true));
        }
    }
}
```

**Step 2: Register the handler**

Add to `MooOfDoom` constructor:
```java
NeoForge.EVENT_BUS.register(AuraHandler.class);
```

Add import:
```java
import com.github.netfallnetworks.mooofdoom.cow.utility.AuraHandler;
```

**Step 3: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/utility/AuraHandler.java \
  src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java
git commit -m "feat: add passive aura buffs for players near OP cows"
```

---

### Task 12: Implement random teleportation (chaos)

**Files:**
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/chaos/TeleportHandler.java`
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java`

**Step 1: Create teleport handler**

Create `src/main/java/com/github/netfallnetworks/mooofdoom/cow/chaos/TeleportHandler.java`:

```java
package com.github.netfallnetworks.mooofdoom.cow.chaos;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class TeleportHandler {

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Cow cow)) return;
        if (cow.level().isClientSide()) return;
        if (!OpCowManager.isOpCow(cow)) return;
        if (!ModConfig.TELEPORT_ENABLED.getAsBoolean()) return;

        if (cow.getRandom().nextInt(ModConfig.TELEPORT_INTERVAL_TICKS.getAsInt()) != 0) return;

        int range = ModConfig.TELEPORT_RANGE.getAsInt();
        ServerLevel level = (ServerLevel) cow.level();

        // Try up to 10 times to find a safe position
        for (int i = 0; i < 10; i++) {
            double x = cow.getX() + (cow.getRandom().nextDouble() - 0.5) * 2 * range;
            double z = cow.getZ() + (cow.getRandom().nextDouble() - 0.5) * 2 * range;
            double y = cow.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    (int) x, (int) z);

            BlockPos pos = BlockPos.containing(x, y, z);
            BlockState below = cow.level().getBlockState(pos.below());
            if (below.isSolid()) {
                // Particles at old position
                level.sendParticles(ParticleTypes.PORTAL,
                        cow.getX(), cow.getY() + 1, cow.getZ(),
                        30, 0.5, 1.0, 0.5, 0.0);

                cow.teleportTo(x, y, z);

                // Particles at new position
                level.sendParticles(ParticleTypes.PORTAL,
                        x, y + 1, z,
                        30, 0.5, 1.0, 0.5, 0.0);
                cow.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                break;
            }
        }
    }
}
```

**Step 2: Register the handler**

Add to `MooOfDoom` constructor:
```java
NeoForge.EVENT_BUS.register(TeleportHandler.class);
```

Add import:
```java
import com.github.netfallnetworks.mooofdoom.cow.chaos.TeleportHandler;
```

**Step 3: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/chaos/TeleportHandler.java \
  src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java
git commit -m "feat: add random teleportation chaos behavior"
```

---

### Task 13: Implement random size changes (chaos)

**Files:**
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/chaos/SizeChangeHandler.java`
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java`

**Step 1: Create size change handler**

Create `src/main/java/com/github/netfallnetworks/mooofdoom/cow/chaos/SizeChangeHandler.java`:

```java
package com.github.netfallnetworks.mooofdoom.cow.chaos;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class SizeChangeHandler {

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Cow cow)) return;
        if (cow.level().isClientSide()) return;
        if (!OpCowManager.isOpCow(cow)) return;
        if (!ModConfig.SIZE_CHANGE_ENABLED.getAsBoolean()) return;

        if (cow.getRandom().nextInt(ModConfig.SIZE_CHANGE_INTERVAL_TICKS.getAsInt()) != 0) return;

        // Random scale between 0.5 and 3.0
        float newScale = 0.5F + cow.getRandom().nextFloat() * 2.5F;

        // Use the SCALE attribute (available in 1.21+)
        AttributeInstance scaleAttr = cow.getAttribute(Attributes.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(newScale);
        }

        ServerLevel level = (ServerLevel) cow.level();
        level.sendParticles(ParticleTypes.POOF,
                cow.getX(), cow.getY() + 0.5, cow.getZ(),
                15, 0.5, 0.5, 0.5, 0.05);
        cow.playSound(SoundEvents.PUFFER_FISH_BLOW_UP, 1.0F,
                newScale > 1.5F ? 0.5F : 1.5F);
    }
}
```

**Step 2: Register the handler**

Add to `MooOfDoom` constructor:
```java
NeoForge.EVENT_BUS.register(SizeChangeHandler.class);
```

Add import:
```java
import com.github.netfallnetworks.mooofdoom.cow.chaos.SizeChangeHandler;
```

**Step 3: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/chaos/SizeChangeHandler.java \
  src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java
git commit -m "feat: add random size changes chaos behavior"
```

---

### Task 14: Implement random explosions (chaos)

**Files:**
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/chaos/ExplosionHandler.java`
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java`

**Step 1: Create explosion handler**

Create `src/main/java/com/github/netfallnetworks/mooofdoom/cow/chaos/ExplosionHandler.java`:

```java
package com.github.netfallnetworks.mooofdoom.cow.chaos;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class ExplosionHandler {

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Cow cow)) return;
        if (cow.level().isClientSide()) return;
        if (!OpCowManager.isOpCow(cow)) return;
        if (!ModConfig.EXPLOSION_ENABLED.getAsBoolean()) return;

        if (cow.getRandom().nextInt(ModConfig.EXPLOSION_INTERVAL_TICKS.getAsInt()) != 0) return;

        // Explosion that does NOT destroy blocks (Level.ExplosionInteraction.NONE)
        cow.level().explode(
                cow,                    // source entity (cow is immune to its own)
                cow.getX(),
                cow.getY() + 0.5,
                cow.getZ(),
                (float) ModConfig.EXPLOSION_POWER.get().doubleValue(),
                Level.ExplosionInteraction.NONE  // No block destruction
        );
    }
}
```

**Step 2: Make cow immune to its own explosions**

Add to `OpCowManager`, a handler for incoming damage:

```java
@SubscribeEvent
public static void onLivingDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
    if (!(event.getEntity() instanceof Cow cow)) return;
    if (!isOpCow(cow)) return;

    // Immune to explosion damage
    if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
        event.setCanceled(true);
    }
}
```

**Step 3: Register the handler**

Add to `MooOfDoom` constructor:
```java
NeoForge.EVENT_BUS.register(ExplosionHandler.class);
```

Add import:
```java
import com.github.netfallnetworks.mooofdoom.cow.chaos.ExplosionHandler;
```

**Step 4: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/chaos/ExplosionHandler.java \
  src/main/java/com/github/netfallnetworks/mooofdoom/cow/OpCowManager.java \
  src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java
git commit -m "feat: add random explosions chaos behavior with self-immunity"
```

---

### Task 15: Implement moon jumps (chaos)

**Files:**
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/chaos/MoonJumpHandler.java`
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java`

**Step 1: Create moon jump handler**

Create `src/main/java/com/github/netfallnetworks/mooofdoom/cow/chaos/MoonJumpHandler.java`:

```java
package com.github.netfallnetworks.mooofdoom.cow.chaos;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.cow.OpCowManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class MoonJumpHandler {

    private static final String MOON_JUMP_TAG = "MooOfDoom_MoonJump";

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Cow cow)) return;
        if (cow.level().isClientSide()) return;
        if (!OpCowManager.isOpCow(cow)) return;
        if (!ModConfig.MOON_JUMP_ENABLED.getAsBoolean()) return;

        // If currently in a moon jump, apply slow fall
        if (cow.getTags().contains(MOON_JUMP_TAG)) {
            if (cow.onGround()) {
                // Landed
                cow.removeTag(MOON_JUMP_TAG);
                ServerLevel level = (ServerLevel) cow.level();
                level.sendParticles(ParticleTypes.CLOUD,
                        cow.getX(), cow.getY(), cow.getZ(),
                        20, 1.0, 0.2, 1.0, 0.0);
            } else {
                // Slow fall + particles
                Vec3 motion = cow.getDeltaMovement();
                if (motion.y < -0.1) {
                    cow.setDeltaMovement(motion.x * 0.95, -0.1, motion.z * 0.95);
                }
                cow.fallDistance = 0;
                if (cow.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.END_ROD,
                            cow.getX(), cow.getY(), cow.getZ(),
                            2, 0.3, 0.0, 0.3, 0.02);
                }
            }
            return;
        }

        // Random chance to start a moon jump
        if (cow.getRandom().nextInt(ModConfig.MOON_JUMP_INTERVAL_TICKS.getAsInt()) != 0) return;

        cow.addTag(MOON_JUMP_TAG);
        cow.setDeltaMovement(cow.getDeltaMovement().add(0, 2.0, 0));
        cow.playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0F, 0.5F);

        ServerLevel level = (ServerLevel) cow.level();
        level.sendParticles(ParticleTypes.FIREWORK,
                cow.getX(), cow.getY(), cow.getZ(),
                15, 0.3, 0.1, 0.3, 0.1);
    }
}
```

**Step 2: Add fall damage immunity in OpCowManager**

Add to the `onLivingDamage` method:

```java
// Immune to fall damage
if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
    event.setCanceled(true);
}
```

**Step 3: Register the handler**

Add to `MooOfDoom` constructor:
```java
NeoForge.EVENT_BUS.register(MoonJumpHandler.class);
```

Add import:
```java
import com.github.netfallnetworks.mooofdoom.cow.chaos.MoonJumpHandler;
```

**Step 4: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/chaos/MoonJumpHandler.java \
  src/main/java/com/github/netfallnetworks/mooofdoom/cow/OpCowManager.java \
  src/main/java/com/github/netfallnetworks/mooofdoom/MooOfDoom.java
git commit -m "feat: add moon jumps chaos behavior with fall damage immunity"
```

---

### Task 16: Integration testing and polish

**Files:**
- All files (no new files)

**Step 1: Run a full build**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL

**Step 2: Launch test client**

Run: `./gradlew runClient`

**Step 3: In-game test checklist**

Test each feature manually:

1. **Activation (ALL_COWS mode):**
   - Spawn a cow with `/summon cow` - verify it glows
   - Check health with F3 - should show much higher HP

2. **Combat:**
   - Spawn a zombie near the cow - cow should target and engage
   - Watch for charge attack behavior (cow rushes toward zombie)
   - Watch for milk projectile (when target is far away)

3. **Utility:**
   - Right-click OP cow with bucket - should get Enchanted Milk
   - Drink Enchanted Milk - should get random potion effects
   - Stand near cow - should get Strength, Regen, Luck buffs
   - Wait near cow - should see rare items drop

4. **Chaos:**
   - Wait and watch cow - should teleport occasionally (portal particles)
   - Watch for size changes (cow grows/shrinks)
   - Watch for random explosions (no block damage)
   - Watch for moon jumps (cow launches up, floats down)

5. **Config (ITEM_ACTIVATED mode):**
   - Change config to ITEM_ACTIVATED
   - Restart client
   - Spawn cow - should NOT be OP
   - Craft Doom Apple (golden apple + nether star + milk bucket)
   - Right-click cow with Doom Apple - should become OP with totem particles

**Step 4: Fix any issues found**

Address any bugs, crashes, or unexpected behavior found during testing.

**Step 5: Final commit**

```bash
git add -A
git commit -m "fix: address integration test findings"
```

---

### Task 17: Prepare for distribution

**Files:**
- No new source files

**Step 1: Build the release JAR**

Run: `./gradlew clean build`
Verify: Check `build/libs/mooofdoom-1.0.0.jar` exists

**Step 2: Push to GitHub**

```bash
git remote add origin https://github.com/NetfallNetworks/mod-op-cows.git
git branch -M main
git push -u origin main
```

**Step 3: Create GitHub release**

```bash
gh release create v1.0.0 build/libs/mooofdoom-1.0.0.jar \
  --title "Moo of Doom v1.0.0" \
  --notes "Initial release for Minecraft 1.21.11 (NeoForge 21.11+)"
```

**Step 4: Upload to CurseForge**

- Go to https://www.curseforge.com/minecraft/mc-mods
- Create new project "Moo of Doom"
- Upload `build/libs/mooofdoom-1.0.0.jar`
- Set game version to 1.21.11
- Set mod loader to NeoForge
- Set license to MIT
- Publish

---

## Summary

| Task | Description | Dependencies |
|------|-------------|--------------|
| 1 | Scaffold NeoForge project | None |
| 2 | Add configuration system | Task 1 |
| 3 | Add item and entity registries | Task 1 |
| 4 | OP cow activation + attributes | Tasks 2, 3 |
| 5 | Doom Apple item activation | Task 4 |
| 6 | Auto-aggression targeting | Task 4 |
| 7 | Charge attack goal | Task 6 |
| 8 | Milk projectile + ranged attack | Tasks 3, 6 |
| 9 | Enchanted milk item | Tasks 3, 4 |
| 10 | Rare loot drops | Task 4 |
| 11 | Passive aura buffs | Task 4 |
| 12 | Random teleportation | Task 4 |
| 13 | Random size changes | Task 4 |
| 14 | Random explosions | Task 4 |
| 15 | Moon jumps | Task 4 |
| 16 | Integration testing | Tasks 5-15 |
| 17 | Distribution | Task 16 |
