# Smart Zombie Mod — Minecraft 1.20.1 (Forge)

A Minecraft Forge mod that adds the **Smart Zombie** — a half-human, half-zombie hybrid that retained its intelligence and combat instincts.

---

## Features

### Smart Zombie Behavior
- **Picks up & uses weapons** — swords and bows found on the ground
- **Melee mode (sword):** Closes in, strikes, then immediately backs away to avoid retaliation
- **Ranged mode (bow):** Maintains distance, backs up if the player/villager gets too close, strafes sideways to dodge projectiles, fires charged arrows
- **Targets:** Players, Villagers, Iron Golems
- **Does NOT burn in sunlight** — its human half grants resistance
- **30 HP**, slightly more armor than a normal zombie
- **Loot:** Rotten flesh + chance of iron ingot, gold nuggets, or arrows

### Weapon Behavior
| Weapon | Behavior |
|--------|----------|
| Stone/Iron/Diamond Sword | Approach → Strike → Retreat ~5-6 blocks → Repeat |
| Bow | Stay 7-20 blocks away, back up if player closes in, strafe, fire charged arrows |

---

## Installation

### Requirements
- **Minecraft 1.20.1**
- **Forge 47.2.0+** — [Download Forge](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)
- **Java 17**

### Steps
1. Install Forge 47.2.x for Minecraft 1.20.1
2. Place `smartzombie-1.0.0.jar` into your `.minecraft/mods/` folder
3. Launch Minecraft with the Forge profile

---

## Building from Source

### Prerequisites
- Java 17 JDK
- Git

### Steps

```bash
# 1. Clone / place the mod source
cd smartzombie/

# 2. Set up Forge workspace (downloads Minecraft assets — takes a few minutes)
./gradlew genEclipseRuns   # for Eclipse
# or
./gradlew genIntellijRuns  # for IntelliJ IDEA

# 3. Build the mod JAR
./gradlew build

# Output: build/libs/smartzombie-1.0.0.jar
```

The compiled JAR will be at `build/libs/smartzombie-1.0.0.jar`.

---

## Adding a Custom Texture

The mod ships without a texture file so you can provide your own.

1. Create a **64x64 PNG** that looks like a half-human, half-zombie
   - Left half of face: normal human skin
   - Right half: zombie green/rotten skin
   - Use vanilla zombie UV layout (same as `zombie.png` from vanilla)
2. Save it as:
   ```
   src/main/resources/assets/smartzombie/textures/entity/smart_zombie.png
   ```
3. Rebuild the mod

**UV Layout Reference** (same as vanilla zombie):
- Head: top-left 8x8 region
- Body, Arms, Legs: follow standard humanoid layout

If no texture is provided, the game will show the magenta/black "missing texture" checkerboard — the mob will still work perfectly.

---

## Spawning

Smart Zombies spawn naturally at night in the same conditions as regular zombies.

**Spawn via command:**
```
/summon smartzombie:smart_zombie
```

**Spawn egg** (creative mode): Search "Smart Zombie" in the creative menu.

---

## File Structure

```
smartzombie/
├── build.gradle
├── gradle.properties
└── src/main/
    ├── java/com/smartzombie/
    │   ├── SmartZombieMod.java              ← Main mod class, registration
    │   ├── init/
    │   │   └── ModEntities.java             ← Entity type registration
    │   ├── entity/
    │   │   ├── SmartZombieEntity.java        ← Entity class, attributes, weapon logic
    │   │   └── ai/
    │   │       ├── SmartMeleeAttackGoal.java ← Hit-and-run sword AI
    │   │       ├── SmartRangedAttackGoal.java← Backpedal bow AI
    │   │       └── SmartZombieRetreatGoal.java← Movement during retreat
    │   └── client/
    │       ├── ClientSetup.java             ← Renderer registration
    │       └── renderer/
    │           └── SmartZombieRenderer.java ← Visual renderer
    └── resources/
        ├── pack.mcmeta
        ├── META-INF/mods.toml               ← Mod metadata
        ├── assets/smartzombie/
        │   ├── lang/en_us.json              ← Display name
        │   ├── models/entity/               ← (optional custom model)
        │   └── textures/entity/             ← Place smart_zombie.png here
        └── data/smartzombie/
            └── loot_tables/entities/
                └── smart_zombie.json        ← Drop table
```

---

## Customization / Tuning

All behavior constants are at the top of each AI goal class:

**`SmartMeleeAttackGoal.java`**
```java
private static final int ATTACK_COOLDOWN = 30;   // ticks between strikes (20 ticks = 1 second)
private static final int RETREAT_DURATION = 25;  // how long to retreat after hitting
```

**`SmartRangedAttackGoal.java`**
```java
private static final double IDEAL_RANGE = 12.0;  // preferred shooting distance
private static final double MIN_RANGE   = 7.0;   // backs up if target gets closer than this
private static final double MAX_RANGE   = 20.0;  // closes in if target is farther than this
private static final int    CHARGE_TIME = 20;    // ticks to charge arrow (= 1 second)
private static final int    SHOOT_COOLDOWN = 40; // ticks between shots (= 2 seconds)
```

**`SmartZombieEntity.java` — `createAttributes()`**
```java
.add(Attributes.MAX_HEALTH, 30.0D)      // HP
.add(Attributes.MOVEMENT_SPEED, 0.28D) // Speed (normal zombie = 0.23)
.add(Attributes.ATTACK_DAMAGE, 5.0D)   // Melee damage
.add(Attributes.FOLLOW_RANGE, 40.0D)   // Detection range
.add(Attributes.ARMOR, 2.0D)           // Armor points
```

---

## License
MIT — free to use, modify, and redistribute.
