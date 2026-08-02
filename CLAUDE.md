# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EverlaArtifacts is a Minecraft Forge 1.20.1 mod (modId: `everlaartifacts`) that adds custom items, music discs, enchantments, mob effects, a difficulty system, and various "random ideas." Partially MCreator-generated, partially hand-written.

- **Group**: `net.everla` / **Package**: `net.everla.everlaartifacts`
- **Java**: 17 (toolchain locked) / **Forge**: 1.20.1-47.4.21 / **Mappings**: official
- **Dependencies**: JEI 15.2.0.27 (compileOnly for API, runtimeOnly for full jar)
- **Mixin**: 0.8.5 (annotation processor, refmap auto-generated)

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
- **`client/`** — Client-only: GUI screen overlays, particle rendering, entity renderers, model layer definitions, client-side event handlers, language detection. Entry point is `ClientModEvents`.
- **`server/`** — Server-only: network packet definitions (`server/network/`), server-side event handlers, performance metrics tracking, enchantment flight management.

### Event Handler Pattern

Most handlers use `@Mod.EventBusSubscriber(modid = "everlaartifacts")` on the class with `@SubscribeEvent` static methods. Some are registered manually in the main mod constructor via `MinecraftForge.EVENT_BUS.register(...)`. Handlers are organized by domain:

- `common/handlers/` — enchantment behavior, data-driven item systems (everlasting, rainbow name/lore)
- `client/handlers/` — visual effects, tooltips, particles, overlays, language detection (organized by item/effect name)
- `server/handlers/` — item ability logic, block behavior, difficulty mechanics, command handling, enchantment flight (organized by item/effect name)

### Item Class Hierarchy

Basic items extend vanilla types (`SwordItem`, `RecordItem`, etc.) with custom tiers and properties defined inline. Music discs extend `BaseRecordItem`, a shared base class that resolves `SoundEvent` by registry name and supports translatable description lines. Complex items (Homa Staff, Brackets Blade, Venus Shell, Procedure Sword) have their ability logic in corresponding `server/handlers/items/<name>/` handler classes.

### Mixin Architecture

All Mixin classes are in `net.everla.everlaartifacts.mixin` and registered in `src/main/resources/mixins.everlaartifacts.json`. Three categories:

| Category | Mixin JSON key | Description |
|---|---|---|
| Common (client+server) | `mixins` | Game logic mixins |
| Client-only | `client` | Rendering, particles, client events |
| Server-only | `server` | (currently empty) |

**Existing Mixins:**

| Class | Target | Purpose |
|---|---|---|
| `ItemStackEverlastingMixin` | `ItemStack` | Cancel durability consumption for everlasting-tagged items |
| `ItemEntityEverlastingMixin` | `ItemEntity` | Make everlasting items immune to fire/explosions |
| `ImpalingEnchantmentMixin` | `TridentImpalerEnchantment` | New damage formula (L1=+2, Ln=+2.5), all mob types |
| `PlayerAttackImpalingMixin` | `Player` | Water/rain/lava condition check + suppress enchanted_hit particles |
| `ThrownTridentImpalingMixin` | `ThrownTrident` | Water/rain/lava condition for thrown tridents |
| `LivingEntityLayeredBufferMixin` | `LivingEntity` | Absorb damage at HEAD of `hurt()` — highest priority |
| `AbstractArrowAccessor` | `AbstractArrow` | `@Invoker` for protected `getPickupItem()` |
| `LivingEntityAccessor` | `LivingEntity` | `@Invoker` for protected `dropFromLootTable()` |
| `AbilitiesAccessor` | `Abilities` | `@Accessor` for private `flyingSpeed` field |
| `ItemTooltipOrderMixin` | `ItemStack` | Reorder mod tooltips before F3+H advanced section |
| `LocalPlayerSprintMixin` | `LocalPlayer` | Client sprint behavior |
| `ItemRendererMixin` | `ItemRenderer` | Custom item rendering |

**Mixin patterns:**
- `@Inject` with `cancellable = true` at `HEAD` → intercept before any processing (highest priority)
- `@Redirect` at `INVOKE` → replace method calls at specific call sites. Use `remap = false` for Forge classes
- `@Invoker` interface → expose protected methods (prefer over Java reflection)
- `@Accessor` interface → expose private fields (prefer over Access Transformers)
- `@ModifyVariable` at `HEAD` with `argsOnly` → modify method parameters before use
- Cross-package communication: make target methods `public` and call directly (e.g. LayeredBufferHandler ↔ Mixin)

### Network Packets

Defined in `server/network/`. SimpleChannel-based with a sequential message ID counter. Packets handle: performance score reporting, difficulty changes/sync, blood blossom entity effects, language sync. New packets are registered in `EverlaartifactsMod.registerNetworkPackets()` via `addNetworkMessage()`.

**C→S packet pattern:**
1. Client handler detects event (e.g. `ClientPlayerNetworkEvent.LoggingIn`)
2. Calls `EverlaartifactsMod.PACKET_HANDLER.sendToServer(packet)`
3. Server-side `handle()` reads `ctx.get().getSender()` to get the player

### Config

`EverlaArtifactsConfig` uses Forge's `ForgeConfigSpec` system, registered as `ModConfig.Type.COMMON`. Covers: red packet drop rates, wither special attacks, ender dragon crystal respawn, true damage boss blacklist, layered buffer enchantment behavior, performance debug mode, impaling enhancement.

Config values are accessed via static getter methods (e.g. `EverlaArtifactsConfig.isEnhanceImpaling()`). Mixin handlers read config directly since `ConfigValue.get()` is a cheap field read.

## Key Mod Features

### Custom Enchantments

| Enchantment | Class | Max Level | Notes |
|---|---|---|---|
| Layered Buffer | `LayeredBufferEnchantment` | 10 | Absorbs hits; Mixin at HEAD of `hurt()` for highest priority |
| Steadfast | `SteadfastEnchantment` | 3 | Shield bash + damage boost |
| Live Wire | `LiveWireEnchantment` | — | Lightning strikes |
| Money Burners Creed | `MoneyBurnersCreedEnchantment` | — | XP-based damage |
| TPAura | `TPAuraEnchantment` | — | Teleport attack |
| Wild Hunt | `WildHuntEnchantment` | — | Max-health damage absorption |
| Deutsch | `DeutschEnchantment` | — | — |
| Scrapyard Scrounger | `ScrapyardScroungerEnchantment` | — | Performance-based damage |
| Death Sprint | `DeathSprintEnchantment` | — | Sprint using health when hungry |
| Chinese Can Fly | `ChineseCanFlyEnchantment` | 1 | Flight for zh-language users at half creative speed |

### Impaling Enchantment Rework

Vanilla `TridentImpalerEnchantment` modified via Mixin:
- **Damage formula**: Level 1 = +2.0, each additional level = +2.5
- **Condition**: Any mob in water, rain, or lava (was: aquatic mobs only)
- **Particle suppression**: `enchanted_hit` particles suppressed when bonus not active
- **Config toggle**: `EnhanceImpaling.enhanceImpaling` (default: `true`)
- **Architecture**: `ImpalingEnchantmentMixin` (formula) + `PlayerAttackImpalingMixin` (melee condition) + `ThrownTridentImpalingMixin` (ranged condition)

### Layered Buffer Protection

Two-layer architecture:
- **Primary**: `LivingEntityLayeredBufferMixin` — `@Inject` at `HEAD` of `LivingEntity.hurt()`. Runs before any Forge event. Cancels damage by returning `false`.
- **Secondary**: `LayeredBufferHandler.onLivingHurt` — Forge `LivingHurtEvent` at `HIGHEST` priority. Catches damage re-dispatched after Mixin interception.
- **Death prevention**: `LayeredBufferHandler.onLivingDeath` — Forge `LivingDeathEvent`.

### Chinese Can Fly (Language-Based Flight)

Client detects language on login → sends `LanguageSyncPacket` to server → server stores in `ConcurrentHashMap<UUID, String>` → every 20 ticks + on equipment change, checks zh-language + chest enchantment → enables `mayfly` at `flyingSpeed = 0.025` (half creative default).

### Custom music discs

~30 discs with jukebox song definitions; many reference anime/game music.

### Mob effects with screen overlays

Blood Blossom, American Style Cut, Waaooo, Genshin Start, Homa Active/Passive, Venus Shell, Bedmic Destruction, Blitzkrieg, Cognitive Disorder, Lethal Poison, Nuclear Water Radiation.

### Difficulty system

Custom `DifficultyLevel` enum extends vanilla with `LUNATIC` and `EXTRA` levels. Controlled via game rules.

### Other features

- **Projectiles**: Angolmois Doom (custom model/renderer), Firecracker
- **Fluid**: Nuclear Waste Water (Create mod compat)
- **Performance tracking**: Client reports hardware specs; server calculates performance score
- **Everlasting tag**: Items tagged `everlaartifacts:everlasting` are unbreakable via Mixin
- **Brackets Blade**: Sword damage scales with `「」` bracket pairs in custom name
- **Procedure Sword**: Instant-kill weapon using Mixin `@Invoker` for loot drops instead of fragile Java reflection

### Data-Driven Systems

- **Item tags** at `data/everlaartifacts/tags/items/`: `everlasting.json` (auto-unbreakable), `rainbow_lore.json`, `rainbow_name.json` — handlers in `common/handlers/data_driven/` and `client/handlers/data_driven/` react to these tags.
- **Weapon attributes** at `data/everlaartifacts/weapon_attributes/`: JSON configs for special weapon mechanics.
- **Damage types** at `data/everlaartifacts/damage_type/`: custom damage types.
- **Recipes** at `data/everlaartifacts/recipes/`: standard and Create mod compat recipes.

## Coding Conventions

- Follow surrounding code style: match indentation (tabs), naming, and comment patterns
- Mixin handler method names use the `everlaartifacts$` prefix
- Use `@Invoker`/`@Accessor` Mixins instead of Java reflection or Access Transformers
- Forge event handlers use `EventPriority.HIGHEST` for protective/cancelling behavior
- Language keys: `enchantment.everlaartifacts.<name>` and `enchantment.everlaartifacts.<name>.desc`
- Forge classes in `@At` targets require `remap = false`
- `@Local` capture in `@Inject` uses `LocalCapture.CAPTURE_FAILSOFT` with null-check fallback
- Cross-package shared methods: make `public` and document the dependency
