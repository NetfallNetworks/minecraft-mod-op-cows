# Adding New Items to Moo of Doom

Reference guide for creating custom items with textures in Minecraft 1.21.11 NeoForge.

## Pre-Flight Checklist

```bash
# BEFORE making any changes, verify the build is green
export JAVA_HOME="/c/Program Files (x86)/MCA Selector/jre"
./gradlew build
```

If the build is not green, fix existing issues first. Do not pile changes on a broken build.

## What You Need Per Item

Each new item requires **4 files + 1 edit**:

| # | File | Path |
|---|------|------|
| 1 | Texture PNG | `assets/mooofdoom/textures/item/<name>.png` |
| 2 | Item definition JSON | `assets/mooofdoom/items/<name>.json` |
| 3 | Model JSON | `assets/mooofdoom/models/item/<name>.json` |
| 4 | Language entry | `assets/mooofdoom/lang/en_us.json` (edit) |
| 5 | Java registration | `registry/ModItems.java` (edit) |

All `<name>` values must match the item's registry name exactly, in `snake_case`.

## 1. Texture PNG

**Location:** `src/main/resources/assets/mooofdoom/textures/item/<name>.png`

| Property | Requirement |
|----------|-------------|
| Format | PNG (non-interlaced) |
| Dimensions | **16x16 pixels** (standard) or 32x32/64x64 for high-res (must be power of 2, square) |
| Color | sRGB, 8-bit RGBA recommended |
| Transparency | Supported via alpha channel |
| Background | Transparent (alpha = 0) for non-item pixels |

Tips:
- Use a pixel art editor (Aseprite, Piskel, GIMP at 16x16)
- Export as PNG-8 with transparency or PNG-32 (RGBA)
- Keep the style consistent with vanilla Minecraft textures
- No anti-aliasing on edges (pixel art style)

## 2. Item Definition JSON (1.21.11 format)

**Location:** `src/main/resources/assets/mooofdoom/items/<name>.json`

This is **new in 1.21.x** and required. It tells the client how to render the item.

**Simple item:**
```json
{
  "model": {
    "type": "minecraft:model",
    "model": "mooofdoom:item/<name>"
  }
}
```

## 3. Model JSON

**Location:** `src/main/resources/assets/mooofdoom/models/item/<name>.json`

**Flat item (most items):**
```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "mooofdoom:item/<name>"
  }
}
```

**Tool/handheld item:**
```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "layer0": "mooofdoom:item/<name>"
  }
}
```

**Multi-layer item (e.g., tinted overlay):**
```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "mooofdoom:item/<name>",
    "layer1": "mooofdoom:item/<name>_overlay"
  }
}
```

The texture reference `mooofdoom:item/<name>` maps to `assets/mooofdoom/textures/item/<name>.png` automatically.

## 4. Language Entry

**File:** `src/main/resources/assets/mooofdoom/lang/en_us.json`

Add a line:
```json
"item.mooofdoom.<name>": "Display Name"
```

## 5. Java Registration

**File:** `src/main/java/com/github/netfallnetworks/mooofdoom/registry/ModItems.java`

**Simple item (no custom class):**
```java
public static final DeferredItem<Item> MY_ITEM = ITEMS.registerSimpleItem(
        "my_item",
        new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)
);
```

**Custom item class:**
```java
public static final DeferredItem<MyItem> MY_ITEM = ITEMS.registerItem(
        "my_item",
        MyItem::new,
        new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
);
```

## Complete Example: Adding "Cow Crown"

### Texture
Create `src/main/resources/assets/mooofdoom/textures/item/cow_crown.png` (16x16 PNG).

### Item definition
`src/main/resources/assets/mooofdoom/items/cow_crown.json`:
```json
{
  "model": {
    "type": "minecraft:model",
    "model": "mooofdoom:item/cow_crown"
  }
}
```

### Model
`src/main/resources/assets/mooofdoom/models/item/cow_crown.json`:
```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "mooofdoom:item/cow_crown"
  }
}
```

### Language
In `en_us.json`, add:
```json
"item.mooofdoom.cow_crown": "Cow Crown"
```

### Java
In `ModItems.java`, add:
```java
public static final DeferredItem<Item> COW_CROWN = ITEMS.registerSimpleItem(
        "cow_crown",
        new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
);
```

## Post-Change Verification

```bash
# AFTER making changes, verify the build is still green
export JAVA_HOME="/c/Program Files (x86)/MCA Selector/jre"
./gradlew build

# Run tests
./gradlew test

# If adding recipes, verify data generation
./gradlew runData
```

**Do not commit until the build passes.** If the build fails, check:
- JSON syntax (trailing commas, missing quotes)
- File naming matches registry name exactly
- Texture path matches model reference
- Import statements in Java files
