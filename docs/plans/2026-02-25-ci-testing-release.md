# CI, Testing & Release Pipeline — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build CI confidence with automated builds on PRs, unit tests for core logic, and auto-release on merge to main.

**Architecture:** GitHub Actions for CI/CD. Plain JUnit 5 for unit-testable logic (refactored to decouple from ModConfig). Tag-triggered release workflow that builds the JAR and publishes a GitHub Release. Branch protection enforced after first successful CI run.

**Tech Stack:** GitHub Actions, Gradle 9.2.1, Java 21 (Temurin), JUnit 5, ModDevGradle 2.0.140, `softprops/action-gh-release@v2`

---

## Key Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Test framework | JUnit 5 (plain, no NeoForge boot) | Fast, simple, tests pure logic. NeoForge GameTests can come later |
| Config decoupling | Refactor `TieredRandom` to accept raw weights | Makes it testable without booting Minecraft |
| Version format | `MC_VERSION-MOD_VERSION` (e.g., `1.21.11-1.0.1`) | Standard mod convention, clear at a glance |
| Release trigger | Git tag `v*.*.*` | Explicit, auditable, no accidental releases |
| Branch protection | Require `Build` check to pass on PRs to `main` | Prevents broken code from merging |

---

### Task 0: GitHub Actions — Build on Push/PR

**Files:**
- Create: `.github/workflows/build.yml`

**Step 1: Create the workflow file**

```yaml
# .github/workflows/build.yml
name: Build

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    name: Build
    runs-on: ubuntu-latest
    permissions:
      contents: read

    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
          fetch-tags: true

      - name: Setup JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Build
        run: ./gradlew build --no-configuration-cache

      - name: Upload JAR
        uses: actions/upload-artifact@v4
        with:
          name: mooofdoom-${{ github.sha }}
          path: build/libs/*.jar
          if-no-files-found: error
```

**Step 2: Push and verify**

```bash
git add .github/workflows/build.yml
git commit -m "ci: add GitHub Actions build workflow"
git push
```

Expected: Go to GitHub → Actions tab → "Build" workflow runs → green checkmark.

**Step 3: Verify JAR artifact**

After the workflow passes, click the run → "Artifacts" section → `mooofdoom-<sha>` download should contain the JAR.

---

### Task 1: Add JUnit 5 to the Build

**Files:**
- Modify: `build.gradle` (add test dependencies)

**Step 1: Add JUnit 5 dependencies and test task**

Add to `build.gradle` inside the `dependencies { }` block:

```groovy
dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter:5.11.0'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

Add after the `dependencies` block:

```groovy
test {
    useJUnitPlatform()
}
```

**Step 2: Verify build still passes**

```bash
./gradlew build
```

Expected: `BUILD SUCCESSFUL`. `test` task should now say `NO-SOURCE` (not yet an error — no test files exist yet).

**Step 3: Commit**

```bash
git add build.gradle
git commit -m "build: add JUnit 5 test dependencies"
```

---

### Task 2: Refactor TieredRandom for Testability

**Files:**
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/rarity/TieredRandom.java`

**Problem:** Both `roll()` overloads call `ModConfig.RARITY_*_WEIGHT.getAsInt()` which requires the NeoForge mod loading system. Can't unit test without booting Minecraft.

**Solution:** Extract a pure `roll(Random, int, int, int, int, int)` method. Existing methods delegate to it.

**Step 1: Refactor TieredRandom.java**

Replace the entire file with:

```java
package com.github.netfallnetworks.mooofdoom.rarity;

import com.github.netfallnetworks.mooofdoom.ModConfig;
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
        return roll(random.fork().nextInt(getTotal()),
                ModConfig.RARITY_COMMON_WEIGHT.getAsInt(),
                ModConfig.RARITY_UNCOMMON_WEIGHT.getAsInt(),
                ModConfig.RARITY_RARE_WEIGHT.getAsInt(),
                ModConfig.RARITY_LEGENDARY_WEIGHT.getAsInt(),
                ModConfig.RARITY_MYTHIC_WEIGHT.getAsInt());
    }

    /**
     * Roll using a java.util.Random (for contexts without RandomSource).
     */
    public static RarityTier roll(java.util.Random random) {
        return roll(random.nextInt(getTotal()),
                ModConfig.RARITY_COMMON_WEIGHT.getAsInt(),
                ModConfig.RARITY_UNCOMMON_WEIGHT.getAsInt(),
                ModConfig.RARITY_RARE_WEIGHT.getAsInt(),
                ModConfig.RARITY_LEGENDARY_WEIGHT.getAsInt(),
                ModConfig.RARITY_MYTHIC_WEIGHT.getAsInt());
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

    private static int getTotal() {
        return ModConfig.RARITY_COMMON_WEIGHT.getAsInt()
                + ModConfig.RARITY_UNCOMMON_WEIGHT.getAsInt()
                + ModConfig.RARITY_RARE_WEIGHT.getAsInt()
                + ModConfig.RARITY_LEGENDARY_WEIGHT.getAsInt()
                + ModConfig.RARITY_MYTHIC_WEIGHT.getAsInt();
    }
}
```

**Step 2: Verify build passes**

```bash
./gradlew build
```

Expected: `BUILD SUCCESSFUL`. No callers change — the existing `roll(RandomSource)` and `roll(Random)` signatures are preserved.

**Step 3: Commit**

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/rarity/TieredRandom.java
git commit -m "refactor: extract pure roll method from TieredRandom for testability"
```

---

### Task 3: Write Unit Tests — TieredRandom

**Files:**
- Create: `src/test/java/com/github/netfallnetworks/mooofdoom/rarity/TieredRandomTest.java`

**Step 1: Create the test directory**

```bash
mkdir -p src/test/java/com/github/netfallnetworks/mooofdoom/rarity
```

**Step 2: Write TieredRandomTest.java**

```java
package com.github.netfallnetworks.mooofdoom.rarity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TieredRandomTest {

    // Default weights: 50/30/15/4/1 = 100 total
    private static final int W_COMMON = 50;
    private static final int W_UNCOMMON = 30;
    private static final int W_RARE = 15;
    private static final int W_LEGENDARY = 4;
    private static final int W_MYTHIC = 1;

    // --- Boundary tests (deterministic, using rollValue directly) ---

    @Test
    void rollValue0ReturnsCommon() {
        assertEquals(RarityTier.COMMON,
                TieredRandom.roll(0, W_COMMON, W_UNCOMMON, W_RARE, W_LEGENDARY, W_MYTHIC));
    }

    @Test
    void rollValue49ReturnsCommon() {
        // Last value in Common range (0-49)
        assertEquals(RarityTier.COMMON,
                TieredRandom.roll(49, W_COMMON, W_UNCOMMON, W_RARE, W_LEGENDARY, W_MYTHIC));
    }

    @Test
    void rollValue50ReturnsUncommon() {
        // First value in Uncommon range (50-79)
        assertEquals(RarityTier.UNCOMMON,
                TieredRandom.roll(50, W_COMMON, W_UNCOMMON, W_RARE, W_LEGENDARY, W_MYTHIC));
    }

    @Test
    void rollValue79ReturnsUncommon() {
        assertEquals(RarityTier.UNCOMMON,
                TieredRandom.roll(79, W_COMMON, W_UNCOMMON, W_RARE, W_LEGENDARY, W_MYTHIC));
    }

    @Test
    void rollValue80ReturnsRare() {
        // First value in Rare range (80-94)
        assertEquals(RarityTier.RARE,
                TieredRandom.roll(80, W_COMMON, W_UNCOMMON, W_RARE, W_LEGENDARY, W_MYTHIC));
    }

    @Test
    void rollValue94ReturnsRare() {
        assertEquals(RarityTier.RARE,
                TieredRandom.roll(94, W_COMMON, W_UNCOMMON, W_RARE, W_LEGENDARY, W_MYTHIC));
    }

    @Test
    void rollValue95ReturnsLegendary() {
        // First value in Legendary range (95-98)
        assertEquals(RarityTier.LEGENDARY,
                TieredRandom.roll(95, W_COMMON, W_UNCOMMON, W_RARE, W_LEGENDARY, W_MYTHIC));
    }

    @Test
    void rollValue98ReturnsLegendary() {
        assertEquals(RarityTier.LEGENDARY,
                TieredRandom.roll(98, W_COMMON, W_UNCOMMON, W_RARE, W_LEGENDARY, W_MYTHIC));
    }

    @Test
    void rollValue99ReturnsMythic() {
        // Only value in Mythic range (99)
        assertEquals(RarityTier.MYTHIC,
                TieredRandom.roll(99, W_COMMON, W_UNCOMMON, W_RARE, W_LEGENDARY, W_MYTHIC));
    }

    // --- Zero-weight tests ---

    @Test
    void zeroWeightMythicNeverReturnsMythic() {
        Random rng = new Random(42L);
        for (int i = 0; i < 10_000; i++) {
            RarityTier tier = TieredRandom.roll(rng, 50, 30, 15, 5, 0);
            assertNotEquals(RarityTier.MYTHIC, tier);
        }
    }

    @Test
    void zeroWeightCommonNeverReturnsCommon() {
        Random rng = new Random(42L);
        for (int i = 0; i < 10_000; i++) {
            RarityTier tier = TieredRandom.roll(rng, 0, 30, 15, 4, 1);
            assertNotEquals(RarityTier.COMMON, tier);
        }
    }

    @Test
    void singleTierAlwaysReturnsThatTier() {
        Random rng = new Random();
        for (int i = 0; i < 100; i++) {
            assertEquals(RarityTier.LEGENDARY,
                    TieredRandom.roll(rng, 0, 0, 0, 1, 0));
        }
    }

    // --- Distribution test ---

    @RepeatedTest(3)
    void distributionMatchesWeightsWithinTolerance() {
        Random rng = new Random();
        Map<RarityTier, Integer> counts = new EnumMap<>(RarityTier.class);
        for (RarityTier t : RarityTier.values()) counts.put(t, 0);

        int trials = 100_000;
        for (int i = 0; i < trials; i++) {
            RarityTier t = TieredRandom.roll(rng, W_COMMON, W_UNCOMMON, W_RARE, W_LEGENDARY, W_MYTHIC);
            counts.merge(t, 1, Integer::sum);
        }

        // Within 3% absolute tolerance
        assertWithinTolerance("COMMON", counts.get(RarityTier.COMMON), trials, 0.50, 0.03);
        assertWithinTolerance("UNCOMMON", counts.get(RarityTier.UNCOMMON), trials, 0.30, 0.03);
        assertWithinTolerance("RARE", counts.get(RarityTier.RARE), trials, 0.15, 0.03);
        assertWithinTolerance("LEGENDARY", counts.get(RarityTier.LEGENDARY), trials, 0.04, 0.02);
        assertWithinTolerance("MYTHIC", counts.get(RarityTier.MYTHIC), trials, 0.01, 0.01);
    }

    // --- All enum values covered ---

    @Test
    void allTiersReachableWithDefaultWeights() {
        Random rng = new Random(99L);
        Map<RarityTier, Boolean> seen = new EnumMap<>(RarityTier.class);

        for (int i = 0; i < 100_000; i++) {
            seen.put(TieredRandom.roll(rng, W_COMMON, W_UNCOMMON, W_RARE, W_LEGENDARY, W_MYTHIC), true);
            if (seen.size() == RarityTier.values().length) break;
        }

        for (RarityTier tier : RarityTier.values()) {
            assertTrue(seen.containsKey(tier),
                    tier + " was never rolled in 100k trials");
        }
    }

    private void assertWithinTolerance(String name, int count, int total, double expected, double tolerance) {
        double actual = count / (double) total;
        assertTrue(actual > expected - tolerance && actual < expected + tolerance,
                name + " ratio should be ~" + expected + " (±" + tolerance + "), got: " + actual);
    }
}
```

**Step 3: Run the tests**

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL`, all tests pass. Report at `build/reports/tests/test/index.html`.

**Step 4: Commit**

```bash
git add src/test/java/com/github/netfallnetworks/mooofdoom/rarity/TieredRandomTest.java
git commit -m "test: add unit tests for TieredRandom weighted rarity system"
```

---

### Task 4: Write Unit Tests — MOOCOW Multiplier Formula

The MOOCOW formula in `CombatLootHandler` is pure math that deserves its own tests. Extract it for testability.

**Files:**
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/utility/CombatLootHandler.java`
- Create: `src/test/java/com/github/netfallnetworks/mooofdoom/cow/utility/MoocowMultiplierTest.java`

**Step 1: Extract the multiplier formula to a package-visible static method**

Add this method to `CombatLootHandler.java`:

```java
/**
 * Calculate the MOOCOW loot multiplier based on hit count.
 * 1-hit kill = max multiplier, 10+ hits = 1x, linear interpolation between.
 */
static int calculateMultiplier(int hits, int moocowMax) {
    if (hits >= 10) return 1;
    if (hits <= 1) return moocowMax;
    return (int) Math.round(moocowMax - (hits - 1.0) * (moocowMax - 1.0) / 9.0);
}
```

Then update `handleOpCowDeath` to call it:

```java
int multiplier = calculateMultiplier(hits, moocow);
```

Replace the inline formula block:
```java
// REMOVE these lines:
int multiplier;
if (hits >= 10) {
    multiplier = 1;
} else if (hits == 1) {
    multiplier = moocow;
} else {
    multiplier = (int) Math.round(moocow - (hits - 1.0) * (moocow - 1.0) / 9.0);
}

// REPLACE with:
int multiplier = calculateMultiplier(hits, moocow);
```

**Step 2: Verify build passes**

```bash
./gradlew build
```

**Step 3: Write the test**

```java
package com.github.netfallnetworks.mooofdoom.cow.utility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class MoocowMultiplierTest {

    // Default MOOCOW_MULTIPLIER = 10

    @Test
    void oneHitKillGivesMaxMultiplier() {
        assertEquals(10, CombatLootHandler.calculateMultiplier(1, 10));
    }

    @Test
    void tenHitsGivesMinMultiplier() {
        assertEquals(1, CombatLootHandler.calculateMultiplier(10, 10));
    }

    @Test
    void moreThanTenHitsStillGivesOne() {
        assertEquals(1, CombatLootHandler.calculateMultiplier(15, 10));
        assertEquals(1, CombatLootHandler.calculateMultiplier(100, 10));
    }

    @Test
    void zeroHitsTreatedAsOneHit() {
        // Edge case: if somehow hits = 0, should still give max
        assertEquals(10, CombatLootHandler.calculateMultiplier(0, 10));
    }

    @ParameterizedTest
    @CsvSource({
            "1,  10, 10",
            "2,  10, 9",
            "3,  10, 8",
            "4,  10, 7",
            "5,  10, 6",
            "6,  10, 5",
            "7,  10, 4",
            "8,  10, 3",
            "9,  10, 2",
            "10, 10, 1"
    })
    void linearInterpolationWithDefault10(int hits, int moocow, int expected) {
        assertEquals(expected, CombatLootHandler.calculateMultiplier(hits, moocow),
                "hits=" + hits + " moocow=" + moocow);
    }

    @ParameterizedTest
    @CsvSource({
            "1,  20, 20",
            "5,  20, 12",
            "10, 20, 1"
    })
    void worksWithCustomMultiplier(int hits, int moocow, int expected) {
        assertEquals(expected, CombatLootHandler.calculateMultiplier(hits, moocow),
                "hits=" + hits + " moocow=" + moocow);
    }

    @Test
    void multiplierAlwaysAtLeastOne() {
        for (int hits = 1; hits <= 20; hits++) {
            for (int moocow = 1; moocow <= 100; moocow++) {
                int result = CombatLootHandler.calculateMultiplier(hits, moocow);
                assertTrue(result >= 1,
                        "Multiplier must be >= 1, got " + result + " for hits=" + hits + " moocow=" + moocow);
            }
        }
    }

    @Test
    void multiplierNeverExceedsMax() {
        for (int hits = 1; hits <= 20; hits++) {
            int moocow = 10;
            int result = CombatLootHandler.calculateMultiplier(hits, moocow);
            assertTrue(result <= moocow,
                    "Multiplier must be <= moocow, got " + result + " for hits=" + hits);
        }
    }

    @Test
    void multiplierDecreasesMonotonicallyWithHits() {
        int moocow = 10;
        int prev = CombatLootHandler.calculateMultiplier(1, moocow);
        for (int hits = 2; hits <= 10; hits++) {
            int current = CombatLootHandler.calculateMultiplier(hits, moocow);
            assertTrue(current <= prev,
                    "Multiplier should decrease: hits=" + hits + " gave " + current + " > prev " + prev);
            prev = current;
        }
    }
}
```

**Step 4: Run the tests**

```bash
./gradlew test
```

Expected: All tests pass (including Task 3's tests).

**Step 5: Commit**

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/utility/CombatLootHandler.java \
        src/test/java/com/github/netfallnetworks/mooofdoom/cow/utility/MoocowMultiplierTest.java
git commit -m "test: add unit tests for MOOCOW multiplier formula"
```

---

### Task 5: Update CI Workflow to Run Tests

**Files:**
- Modify: `.github/workflows/build.yml`

**Step 1: Add test step to workflow**

The `./gradlew build` task already runs `test` as part of the standard lifecycle, so tests run automatically. But to make test failures clearly visible in CI, add an explicit test report upload:

```yaml
      - name: Build and Test
        run: ./gradlew build --no-configuration-cache

      - name: Upload Test Report
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: test-report-${{ github.sha }}
          path: build/reports/tests/
```

Replace the existing `Build` step with `Build and Test`, and add the test report upload step after it (before the JAR upload step).

**Step 2: Commit and push**

```bash
git add .github/workflows/build.yml
git commit -m "ci: add test report upload on failure"
git push
```

Expected: CI runs, tests execute, green checkmark.

---

### Task 6: Release Workflow — Auto-Publish on Tag

**Files:**
- Create: `.github/workflows/release.yml`
- Modify: `build.gradle` (version format)

**Step 1: Update version format in build.gradle**

Change the version line to include the Minecraft version:

```groovy
version = "${minecraft_version}-${mod_version}"
```

This produces `mooofdoom-1.21.11-1.0.0.jar`.

**Step 2: Verify build**

```bash
./gradlew build
ls build/libs/
```

Expected: `mooofdoom-1.21.11-1.0.0.jar`

**Step 3: Create release workflow**

```yaml
# .github/workflows/release.yml
name: Release

on:
  push:
    tags:
      - 'v*.*.*'

jobs:
  release:
    name: Build and Release
    runs-on: ubuntu-latest
    permissions:
      contents: write

    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
          fetch-tags: true

      - name: Setup JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Build and Test
        run: ./gradlew build --no-configuration-cache

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          files: build/libs/*.jar
          generate_release_notes: true
          draft: false
          prerelease: false
```

**Step 4: Commit**

```bash
git add build.gradle .github/workflows/release.yml
git commit -m "ci: add release workflow, include MC version in JAR name"
```

**Step 5: Test the release pipeline**

```bash
git push
git tag v1.0.0
git push origin v1.0.0
```

Expected: GitHub Actions → "Release" workflow runs → GitHub Releases page shows `v1.0.0` with `mooofdoom-1.21.11-1.0.0.jar` attached and auto-generated release notes from commit history.

---

### Task 7: Enable Branch Protection

This task is manual — performed in the GitHub UI after the build workflow has run at least once.

**Step 1: Verify the build workflow has run successfully at least once**

Go to GitHub → Actions → confirm "Build" workflow has a green run.

**Step 2: Add branch protection rule**

Go to: **Settings → Branches → Add branch ruleset** (or classic protection rule) for `main`:

- **Require a pull request before merging:** Yes
  - Required approvals: 0 (solo project) or 1 (team)
  - Dismiss stale reviews on new push: Yes
- **Require status checks to pass:** Yes
  - Add required check: `Build` (the job name, not the file name)
  - Require branches to be up to date: Yes
- **Allow administrators to bypass:** Yes (for hotfixes)

**Step 3: Test it**

```bash
git checkout -b test/branch-protection
echo "# test" >> README.md
git add README.md
git commit -m "test: verify branch protection"
git push -u origin test/branch-protection
```

Expected: PR required to merge. CI runs on the PR. Cannot merge until CI passes.

Clean up after testing:
```bash
git checkout main
git branch -d test/branch-protection
git push origin --delete test/branch-protection
```

---

### Task 8: Document the Release Process

**Files:**
- Modify: `docs/plans/2026-02-25-ci-testing-release.md` (this file — append a cheat sheet at the bottom)

**Append to the bottom of this plan file after implementation:**

````markdown
---

## Release Cheat Sheet

### Cutting a New Release

```bash
# 1. Update version in gradle.properties
#    mod_version=1.0.1

# 2. Commit the version bump
git add gradle.properties
git commit -m "release: bump version to 1.0.1"

# 3. Tag and push
git tag v1.0.1
git push && git push origin v1.0.1
```

The release workflow builds the JAR, runs tests, and publishes a GitHub Release automatically.

### PR Workflow

```bash
# 1. Create a branch
git checkout -b feat/my-feature

# 2. Make changes, commit, push
git push -u origin feat/my-feature

# 3. Open PR on GitHub (or use gh cli)
gh pr create --title "feat: my feature" --body "Description"

# 4. CI runs automatically — merge when green
```
````

---

## Implementation Order

```
0. GitHub Actions build workflow (instant CI on push/PR)
1. Add JUnit 5 to the build (no tests yet, just deps)
2. Refactor TieredRandom for testability (decouple from ModConfig)
3. Unit tests for TieredRandom (boundary, distribution, zero-weight)
4. Unit tests for MOOCOW multiplier (extract, parameterized tests)
5. Update CI to surface test reports on failure
6. Release workflow + version format (tag → GitHub Release with JAR)
7. Branch protection (manual, GitHub UI)
8. Document the release process
```

**Each task:** build → implement → build → verify green → commit.
