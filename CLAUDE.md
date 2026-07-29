# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EverlaArtifacts is a Minecraft Forge 1.20.1 mod (modId: `everlaartifacts`) that adds custom items, music discs, enchantments, mob effects, a difficulty system, and various "random ideas." Partially MCreator-generated, partially hand-written.

- **Group**: `net.everla` / **Package**: `net.everla.everlaartifacts`
- **Java**: 17 (toolchain locked) / **Forge**: 1.20.1-47.4.0 / **Mappings**: official
- **Dependencies**: JEI 15.2.0.27 (compileOnly for API, runtimeOnly for full jar)

## Build Commands

```bash
./gradlew build          # Build the mod jar
./gradlew runClient      # Launch Minecraft client with the mod
./gradlew runServer      # Launch dedicated server with the mod
```

The mod version is read from `src/main/resources/META-INF/mods.toml` (the `version=` field) at build time — update it there, not in `build.gradle`.

## Architecture

### Registration Layer (`init/`)

All game objects are registered via Forge's `DeferredRegister` pattern in the `init/` package. Each file (e.g., `EverlaartifactsModItems`, `EverlaartifactsModBlocks`) holds a `public static final DeferredRegister` and `RegistryObject<>` fields for every registered object. The main mod class registers all of them on the mod event bus in its constructor.

When adding a new item/block/effect/etc., register it in the corresponding `init/` class and, if it's an item, add it to the appropriate creative tab in `EverlaartifactsModTabs`.

### Side-Separated Code

- **`common/`** — Code shared between client and server: item classes, block classes, mob effects, enchantments, config, difficulty enum, game rules, and shared event handlers.
- **`client/`** — Client-only: GUI screen overlays, particle rendering, entity renderers, model layer definitions, and client-side event handlers. Entry point is `ClientModEvents`.
- **`server/`** — Server-only: network packet definitions (`server/network/`), server-side event handlers, performance metrics tracking.

### Event Handler Pattern

Most handlers use `@Mod.EventBusSubscriber(modid = "everlaartifacts")` on the class with `@SubscribeEvent` static methods. Some are registered manually in the main mod constructor via `MinecraftForge.EVENT_BUS.register(...)`. Handlers are organized by domain:

- `common/handlers/` — enchantment behavior, data-driven item systems (everlasting, rainbow name/lore)
- `client/handlers/` — visual effects, tooltips, particles, overlays (organized by item/effect name)
- `server/handlers/` — item ability logic, block behavior, difficulty mechanics, command handling (organized by item/effect name)

### Item Class Hierarchy

Basic items extend vanilla types (`SwordItem`, `RecordItem`, etc.) with custom tiers and properties defined inline. Music discs extend `BaseRecordItem`, a shared base class that resolves `SoundEvent` by registry name and supports translatable description lines. Complex items (Homa Staff, Brackets Blade, Venus Shell) have their ability logic in corresponding `server/handlers/items/<name>/` handler classes.

### Data-Driven Systems

- **Item tags** at `data/everlaartifacts/tags/items/`: `everlasting.json` (auto-unbreakable), `rainbow_lore.json`, `rainbow_name.json` — handlers in `common/handlers/data_driven/` and `client/handlers/data_driven/` react to these tags.
- **Weapon attributes** at `data/everlaartifacts/weapon_attributes/`: JSON configs for special weapon mechanics (e.g., `homa_staff.json`, `venus_shell.json`).
- **Damage types** at `data/everlaartifacts/damage_type/`: custom damage types (`destiny_kill.json`, `homa_overburn.json`).
- **Recipes** at `data/everlaartifacts/recipes/`: standard and Create mod compat recipes.

### Difficulty System

Custom `DifficultyLevel` enum in `common/difficulty/` extends vanilla with `LUNATIC` and `EXTRA` levels. Controlled via game rules (`EnableLunaticMode`, `ForceUseTruePerformance`). Difficulty-specific handlers are in `common/difficulty/` (shared logic) and `server/handlers/difficulty/` (sync, boss modifications). Network packets sync difficulty state from server to client.

### Network Packets

Defined in `server/network/`. SimpleChannel-based with a sequential message ID counter. Packets handle: performance score reporting, difficulty changes/sync, and blood blossom entity effects. New packets are registered in `EverlaartifactsMod.registerNetworkPackets()`.

### Config

`EverlaArtifactsConfig` uses Forge's `ForgeConfigSpec` system, registered as `ModConfig.Type.COMMON`. Covers: red packet drop rates, wither special attacks, ender dragon crystal respawn, true damage boss blacklist, layered buffer enchantment behavior, and performance debug mode.

## Key Mod Features

- **Custom music discs**: ~30 discs with jukebox song definitions; many reference anime/game music
- **Custom enchantments**: LayeredBuffer (absorption shield), LiveWire, MoneyBurnersCreed, Steadfast, TPAura, WildHunt, Deutsch, ScrapyardScrounger
- **Mob effects with screen overlays**: Blood Blossom, American Style Cut, Waaooo, Genshin Start, Homa Active/Passive, Venus Shell, Bedmic Destruction, Blitzkrieg, Cognitive Disorder, Lethal Poison, Nuclear Water Radiation
- **Difficulty system**: Lunatic mode enhances bosses (wither, ender dragon) and mob behavior; Extra mode adds special mechanics
- **Projectiles**: Angolmois Doom (with custom model/renderer), Firecracker
- **Fluid**: Nuclear Waste Water (with Create mod compat for fluid interactions)
- **Performance tracking**: Client reports hardware specs; server calculates a performance score used by enchantments (e.g., PerformanceBasedThingsHandler)
- **Everlasting tag**: Items tagged `everlaartifacts:everlasting` automatically get `Unbreakable:1b` NBT
- **Brackets Blade**: Sword whose damage scales with the number of `「」` bracket pairs in its custom name; special behavior in Extra difficulty