# Adding Advancements to Moo of Doom

Reference guide for creating custom advancements in Minecraft 1.21.11 NeoForge.

## Pre-Flight Checklist

```bash
# BEFORE making any changes, verify the build is green
export JAVA_HOME="/c/Program Files (x86)/MCA Selector/jre"
./gradlew build
```

If the build is not green, fix existing issues first. Do not pile changes on a broken build.

## File Location

```
src/main/resources/data/mooofdoom/advancement/
```

Subdirectories are optional but recommended for organization:
```
data/mooofdoom/advancement/
  root.json                  <- Tab root (no parent)
  activate_cow.json
  combat/
    first_kill.json
    milk_sniper.json
  chaos/
    moon_launch.json
```

## Advancement JSON Schema

```json
{
  "parent": "mooofdoom:root",
  "display": {
    "icon": {
      "id": "mooofdoom:doom_apple",
      "count": 1
    },
    "title": {
      "translate": "advancement.mooofdoom.my_advancement.title"
    },
    "description": {
      "translate": "advancement.mooofdoom.my_advancement.description"
    },
    "frame": "task",
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": false
  },
  "criteria": {
    "requirement_name": {
      "trigger": "minecraft:inventory_changed",
      "conditions": {
        "items": [
          { "items": "mooofdoom:doom_apple" }
        ]
      }
    }
  },
  "requirements": [
    ["requirement_name"]
  ],
  "rewards": {
    "experience": 50
  }
}
```

## Field Reference

### `parent`
- Identifier of the parent advancement (e.g., `"mooofdoom:root"`)
- **Omit for root advancements** (these create new tabs)
- Child advancements display connected to their parent in the tree

### `display`

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `icon` | ItemStack | required | Item to show as icon (see below) |
| `title` | Text component | required | Name shown in UI and toast |
| `description` | Text component | required | Description shown on hover |
| `frame` | String | `"task"` | Visual frame style (see below) |
| `background` | Identifier | none | Custom tab background (root only) |
| `show_toast` | Boolean | `true` | Show popup notification when earned |
| `announce_to_chat` | Boolean | `true` | Send chat message when earned |
| `hidden` | Boolean | `false` | Hide from UI until earned |

### Icon Format

Icons are **item stacks**, not texture files. Use any registered item:

```json
"icon": {
  "id": "mooofdoom:doom_apple",
  "count": 1
}
```

Or vanilla items:
```json
"icon": {
  "id": "minecraft:diamond_sword",
  "count": 1
}
```

### Frame Types

| Frame | Visual | Use For |
|-------|--------|---------|
| `"task"` | Standard green border | Normal progression |
| `"goal"` | Rounded green border | Milestones |
| `"challenge"` | Purple/spiky border | Difficult achievements |

### Text Components

**Translatable (recommended):**
```json
{"translate": "advancement.mooofdoom.my_advancement.title"}
```
Then add to `assets/mooofdoom/lang/en_us.json`:
```json
"advancement.mooofdoom.my_advancement.title": "My Advancement",
"advancement.mooofdoom.my_advancement.description": "Do the thing"
```

**Literal (quick and dirty):**
```json
{"text": "My Advancement"}
```

### Requirements (AND/OR Logic)

Each sub-array is OR'd together, then AND'd across arrays:
```json
"requirements": [["A", "B"], ["C"]]
```
Means: `(A OR B) AND C`

Simple case (all criteria required):
```json
"requirements": [["criterion_a"], ["criterion_b"]]
```

### Rewards

All fields optional:
```json
"rewards": {
  "experience": 100,
  "recipes": ["mooofdoom:doom_apple"],
  "loot": ["mooofdoom:special_loot"],
  "function": "mooofdoom:grant_effect"
}
```

## Custom Tab Backgrounds

Only applies to **root advancements** (no parent).

### Background Image Specs

| Property | Requirement |
|----------|-------------|
| Format | PNG |
| Dimensions | 16x16 pixels (tiled automatically) |
| Location | `assets/mooofdoom/textures/gui/advancements/backgrounds/<name>.png` |
| JSON reference | `"mooofdoom:gui/advancements/backgrounds/<name>"` |

The texture path auto-resolves: `"mooofdoom:gui/advancements/backgrounds/combat"` maps to `assets/mooofdoom/textures/gui/advancements/backgrounds/combat.png`.

### Root Advancement with Custom Background

```json
{
  "display": {
    "icon": { "id": "mooofdoom:doom_apple", "count": 1 },
    "title": {"translate": "advancement.mooofdoom.root.title"},
    "description": {"translate": "advancement.mooofdoom.root.description"},
    "background": "mooofdoom:gui/advancements/backgrounds/moo_of_doom",
    "frame": "task",
    "show_toast": false,
    "announce_to_chat": false
  },
  "criteria": {
    "tick": {
      "trigger": "minecraft:tick"
    }
  }
}
```

## Common Triggers

| Trigger | Use Case | Key Conditions |
|---------|----------|----------------|
| `minecraft:tick` | Auto-grant (root advancements) | none |
| `minecraft:inventory_changed` | Player obtains item | `items` |
| `minecraft:consume_item` | Player eats/drinks item | `item` |
| `minecraft:player_killed_entity` | Player kills mob | `entity`, `killing_blow` |
| `minecraft:player_hurt_entity` | Player damages mob | `entity`, `damage` |
| `minecraft:entity_hurt_player` | Player takes damage | `damage` |
| `minecraft:player_interacted_with_entity` | Right-click entity | `item`, `entity` |
| `minecraft:recipe_crafted` | Player crafts recipe | `recipe_id` |
| `minecraft:bred_animals` | Player breeds animals | `parent`, `partner`, `child` |
| `minecraft:tame_animal` | Player tames animal | `entity` |
| `minecraft:changed_dimension` | Enter dimension | `from`, `to` |
| `minecraft:location` | Be in a location | `biome`, `position` |
| `minecraft:impossible` | Never auto-triggers | none (grant via code) |

### Trigger with Entity Condition Example

```json
"criteria": {
  "kill_zombie_near_cow": {
    "trigger": "minecraft:player_killed_entity",
    "conditions": {
      "entity": [
        {
          "condition": "minecraft:entity_properties",
          "entity": "this",
          "predicate": {
            "type": "minecraft:zombie"
          }
        }
      ]
    }
  }
}
```

### Trigger with Item Condition Example

```json
"criteria": {
  "got_doom_apple": {
    "trigger": "minecraft:inventory_changed",
    "conditions": {
      "items": [
        { "items": "mooofdoom:doom_apple" }
      ]
    }
  }
}
```

## Granting Advancements via Code

For custom triggers that don't map to vanilla criteria, use `minecraft:impossible` and grant manually:

```java
// In an event handler
ServerPlayer player = ...;
AdvancementHolder advancement = player.server().getAdvancements()
    .get(Identifier.fromNamespaceAndPath("mooofdoom", "combat/milk_sniper"));
if (advancement != null) {
    player.getAdvancements().award(advancement, "impossible_criterion");
}
```

## Complete Example: "Moo of Doom" Advancement Tab

### Directory Structure

```
data/mooofdoom/advancement/
  root.json
  feed_the_beast.json
  combat/first_blood.json

assets/mooofdoom/textures/gui/advancements/backgrounds/
  moo_of_doom.png              (16x16 tiled background)

assets/mooofdoom/lang/en_us.json
  (add translation keys)
```

### root.json (Tab Root)

```json
{
  "display": {
    "icon": { "id": "mooofdoom:doom_apple", "count": 1 },
    "title": {"translate": "advancement.mooofdoom.root.title"},
    "description": {"translate": "advancement.mooofdoom.root.description"},
    "background": "mooofdoom:gui/advancements/backgrounds/moo_of_doom",
    "show_toast": false,
    "announce_to_chat": false
  },
  "criteria": {
    "tick": { "trigger": "minecraft:tick" }
  }
}
```

### feed_the_beast.json (Child)

```json
{
  "parent": "mooofdoom:root",
  "display": {
    "icon": { "id": "mooofdoom:doom_apple", "count": 1 },
    "title": {"translate": "advancement.mooofdoom.feed_the_beast.title"},
    "description": {"translate": "advancement.mooofdoom.feed_the_beast.description"},
    "frame": "task"
  },
  "criteria": {
    "use_doom_apple": {
      "trigger": "minecraft:consume_item",
      "conditions": {
        "item": { "items": "mooofdoom:doom_apple" }
      }
    }
  },
  "rewards": {
    "experience": 50
  }
}
```

### combat/first_blood.json (Nested Child)

```json
{
  "parent": "mooofdoom:feed_the_beast",
  "display": {
    "icon": { "id": "minecraft:iron_sword", "count": 1 },
    "title": {"translate": "advancement.mooofdoom.first_blood.title"},
    "description": {"translate": "advancement.mooofdoom.first_blood.description"},
    "frame": "goal"
  },
  "criteria": {
    "kill_mob": {
      "trigger": "minecraft:player_killed_entity",
      "conditions": {}
    }
  },
  "rewards": {
    "experience": 100
  }
}
```

### Language Entries

```json
"advancement.mooofdoom.root.title": "Moo of Doom",
"advancement.mooofdoom.root.description": "The cows are watching...",
"advancement.mooofdoom.feed_the_beast.title": "Feed the Beast",
"advancement.mooofdoom.feed_the_beast.description": "Use a Doom Apple to activate an OP cow",
"advancement.mooofdoom.first_blood.title": "First Blood",
"advancement.mooofdoom.first_blood.description": "Watch your OP cow defeat its first enemy"
```

## Post-Change Verification

```bash
# AFTER making changes, verify the build is still green
export JAVA_HOME="/c/Program Files (x86)/MCA Selector/jre"
./gradlew build

# Run tests
./gradlew test
```

**Do not commit until the build passes.** If the build fails, check:
- JSON syntax (trailing commas, missing quotes, wrong bracket types)
- Parent advancement identifiers are correct (`mooofdoom:path/name`)
- Criteria trigger names are valid (`minecraft:trigger_name`)
- Item IDs in icons and conditions are valid
- All criteria referenced in `requirements` exist in `criteria`
- Language keys match between advancement JSON and `en_us.json`
