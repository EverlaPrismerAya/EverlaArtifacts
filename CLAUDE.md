# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EverlaArtifacts is a Minecraft Forge 1.20.1 mod (modId: `everlaartifacts`) that adds custom items, enchantments, mob effects, a difficulty system, and various "random ideas." Partially MCreator-generated, partially hand-written. Two related mods are split out of it: the data-driven tweaks live in the **EverlaTweaker** Jar-in-Jar subproject (`src/JarJar/EverlaTweaker`, modId `everlatweaker`, bundled inside this mod), and the music discs live in a separate standalone mod **EverlaDiscs** (modId `everladiscs`) — see "Related Mods" below.

- **Group**: `net.everla` / **Package**: `net.everla.everlaartifacts`
- **Java**: 17 (toolchain locked) / **Forge**: 1.20.1-47.4.21 / **Mappings**: official
- **Dependencies**: EverlaTweaker 1.0.1 (Jar-in-Jar subproject, `mandatory=true` in mods.toml, bundled into the `-all` jar); JEI 15.2.0.27 (compileOnly for API, runtimeOnly for full jar); Curios API `5.14.1+1.20.1` (compileOnly `:api` classifier + runtimeOnly, optional dependency declared in mods.toml)
- **Mixin**: 0.8.5 (annotation processor, refmap auto-generated)

## Build Commands

```bash
./gradlew build                      # Build the mod jar (auto-builds + bundles the EverlaTweaker subproject)
./gradlew :everlatweaker:build       # Build only the EverlaTweaker subproject
./gradlew runClient                  # Launch Minecraft client with the mod
./gradlew runServer                  # Launch dedicated server with the mod
```

The mod version is read from `src/main/resources/META-INF/mods.toml` (the `version=` field) at build time — update it there, not in `build.gradle`. The EverlaTweaker subproject does the same for its own version (from `src/JarJar/EverlaTweaker/src/main/resources/META-INF/mods.toml`), which drives the `EverlaTweaker-<version>.jar` artifact name and the name of the jar bundled under `META-INF/jarjar/`.

`build/libs/` produces two jars: `EverlaArtifacts-<version>-forge-1.20.1-all.jar` (the **distribution jar**, with EverlaTweaker bundled under `META-INF/jarjar/` via `jarJar.enable()`) and the plain jar without it — distribute the `-all` jar. EverlaTweaker is wired in as a Gradle subproject (`settings.gradle`: `include 'everlatweaker'`, `projectDir = src/JarJar/EverlaTweaker`) and consumed via `jarJar(project(':everlatweaker')) { transitive = false; jarJar.ranged(it, '[1.0,)') }` + `runtimeOnly(project(':everlatweaker'))`. The root `jarJar` task has `dependsOn ':everlatweaker:reobfJar'` so the bundled jar is SRG-reobf'd *before* embedding — the embedded jar is flagged `isObfuscated: true` and FML loads it as-is (no runtime remap). Do not remove that `dependsOn`: without it the embedded jar ships dev-mapped and crashes in the modpack with `NoSuchFieldError` (SRG runtime).

## Related Mods

### EverlaTweaker (Jar-in-Jar subproject)

The data-driven systems — **rainbow name** (`everlatweaker:rainbow_name`), **rainbow lore** (`everlatweaker:rainbow_lore`), **fire resistance** (`everlatweaker:fire_resistant`), **explosion resistance** (`everlatweaker:explosion_resistant`), and **everlasting/unbreakable** (`everlatweaker:everlasting`) — live in the Jar-in-Jar subproject **EverlaTweaker** (modId `everlatweaker`, package `net.everla.everlatweaker`, source at `src/JarJar/EverlaTweaker`).

- It is a **Gradle subproject** of this build (not a separate project) and is automatically bundled into the `-all` distribution jar — no separate install needed.
- Handlers: `EverlastingItemHandler` (tag constant + tooltip `tooltip.everlatweaker.everlasting`), `ProtectiveTagsHandler` (fire/explosion tag checks), `EverlaRainbowHandler`/`RainbowNameHandler`/`RainbowLoreHandler` (client rainbow rendering), plus mixins `ItemStackEverlastingMixin` + `ItemEntityEverlastingMixin` (`mixins.everlatweaker.json`).
- Tags live at `src/JarJar/EverlaTweaker/src/main/resources/data/everlatweaker/tags/items/` and still reference this mod's items (`everlaartifacts:`). Tag IDs changed from `everlaartifacts:` to `everlatweaker:`.
- This mod has **no compile-time reference** to EverlaTweaker — the mixins/handlers were moved wholesale. `WitherEssenceDropHandler` here keeps only the wither-essence drop logic.

### EverlaDiscs (standalone)

The music discs (item classes, jukebox song data, sounds, disc recipes, the `everla_discs` creative tab, and the "penis music" jukebox-explosion handler) live in a separate Forge mod: **EverlaDiscs**, modId `everladiscs`, package `net.everla.everladiscs`, source at `F:\备份区\minecraft\工作区\EverlaDiscs`. It uses the same Minecraft/Forge version and the same `2.1.5` mod version.

- Discs use the `everladiscs:` namespace; the old `everlaartifacts:<disc>` IDs no longer exist (discs already in saves become invalid items after the split).
- The two mods have **no forced dependency** on each other. Cross-mod access is done with string-based `ForgeRegistries` lookups + null checks rather than compile-time imports:
  - `NilkItem` (this mod) plays `everladiscs:nilk` and grants `everladiscs:music_disc_nilk`.
  - `BadAppleSoundHandler` (this mod) grants `everladiscs:worst_apple`.
  - `EverlaDiscs.PenisMusicExplosionHandler` soft-references this mod's `everlaartifacts:waaooo_overlay` effect and `everlaartifacts:deltarune_explosion` sound (it inlines a copy of `EverlaKillHandler`'s kill routine to avoid a dependency on this mod).
- Some disc recipes in EverlaDiscs still reference this mod's items (`auric_scrap`, `firecracker`, `three_interwined_fate`, `tokyo_ticket`), so those recipes only load when both mods are installed.

## Architecture

### Registration Layer (`init/`)

All game objects are registered via Forge's `DeferredRegister` pattern in the `init/` package. Each file (e.g., `EverlaartifactsModItems`, `EverlaartifactsModBlocks`) holds a `public static final DeferredRegister` and `RegistryObject<>` fields for every registered object. The main mod class registers all of them on the mod event bus in its constructor. Advancement triggers are the exception — 1.20.1 has no trigger registry, so `EverlaartifactsModTriggerTypes.register()` calls `CriteriaTriggers.register(...)` directly (trigger classes live in `common/advancements/`, e.g. `SonicBoomWardenKillTrigger`).

When adding a new item/block/effect/etc., register it in the corresponding `init/` class and, if it's an item, add it to the appropriate creative tab in `EverlaartifactsModTabs`.

### Side-Separated Code

- **`common/`** — Code shared between client and server: item classes, block classes, mob effects, enchantments, config, difficulty enum, game rules, and shared event handlers.
- **`client/`** — Client-only: GUI screen overlays, particle rendering, entity renderers, model layer definitions, client-side event handlers, language detection. Entry point is `ClientModEvents`.
- **`server/`** — Server-only: network packet definitions (`server/network/`), server-side event handlers, performance metrics tracking, enchantment flight management.

### Event Handler Pattern

Most handlers use `@Mod.EventBusSubscriber(modid = "everlaartifacts")` on the class with `@SubscribeEvent` static methods. Some are registered manually in the main mod constructor via `MinecraftForge.EVENT_BUS.register(...)`. Handlers are organized by domain:

- `common/handlers/` — enchantment behavior (the data-driven everlasting/rainbow systems moved to the EverlaTweaker subproject)
- `client/handlers/` — visual effects, tooltips, particles, overlays, language detection (organized by item/effect name)
- `server/handlers/` — item ability logic, block behavior, difficulty mechanics, command handling, enchantment flight (organized by item/effect name)

### Item Class Hierarchy

Basic items extend vanilla types (`SwordItem`, etc.) with custom tiers and properties defined inline. Complex items (Homa Staff, Brackets Blade, Venus Shell, Procedure Sword) have their ability logic in corresponding `server/handlers/items/<name>/` handler classes. Music discs are no longer defined here — they live in the sibling EverlaDiscs mod.

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
| `ImpalingEnchantmentMixin` | `TridentImpalerEnchantment` | New damage formula (L1=+2, Ln=+2.5), all mob types |
| `PlayerAttackImpalingMixin` | `Player` | Water/rain/lava condition check + suppress enchanted_hit particles |
| `ThrownTridentImpalingMixin` | `ThrownTrident` | Water/rain/lava condition for thrown tridents |
| `LivingEntityLayeredBufferMixin` | `LivingEntity` | Absorb damage at HEAD of `hurt()` — highest priority |
| `AbstractArrowAccessor` | `AbstractArrow` | `@Invoker` for protected `getPickupItem()` |
| `LivingEntityAccessor` | `LivingEntity` | `@Invoker` for protected `dropFromLootTable()` |
| `AbilitiesAccessor` | `Abilities` | `@Accessor` for private `flyingSpeed` field |
| `ItemTooltipOrderMixin` | `ItemStack` | Reorder mod tooltips before F3+H advanced section |
| `CrossbowItemQuickChargeMixin` | `CrossbowItem` | Quick Charge >5 auto-fires full-charge volleys (right-click + hold) at 120rpm/level, max 720rpm; shared rate gate caps clicks so spam can't exceed rpm |
| `AbstractArrowQuickChargeMixin` | `AbstractArrow` | qc>5 crossbow arrows bypass target i-frames; damage reduced 12%/level above 5 (max 48%) |
| `CrossbowItemAccessor` | `CrossbowItem` | `@Invoker` for private static `isCharged`/`setCharged`/`tryLoadProjectiles`/`performShooting`/`getShootingPower` |
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

`EverlaArtifactsConfig` uses Forge's `ForgeConfigSpec` system, registered as `ModConfig.Type.COMMON`. Covers: red packet drop rates, wither special attacks, ender dragon crystal respawn, true damage boss blacklist, layered buffer enchantment behavior, performance debug mode, impaling enhancement, quick charge enhancement.

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

### Trinkets: Curios-compatible with off-hand fallback

Trinket items (ATM Ring, Deep Seek, Gigabyte Memory Ring, Glasses, Gaming Cattle, Commoner Necklace) are Curios-capable but keep working without Curios via a fallback slot: **off-hand** for rings/necklaces, **head** for helmet-style items. To add a new trinket:

1. **Item class** (`common/item/<Name>Item.java`) — extends `Item` (or `ArmorItem` for helmet-style). Pure calculation lives in `public static` methods (e.g. `calculateDamageMultiplier(...)`) shared by the server handler and the client tooltip. `appendHoverText` adds `description_N` lines; hardware-dependent lines render only when `level.isClientSide()`.
2. **Server effect handler** (`server/handlers/items/<name>/<Name>Handler.java`, `@Mod.EventBusSubscriber(FORGE)`):
   - `isCuriosLoaded()`: lazily cache `ModList.get().isLoaded("curios")` once.
   - `hasXxxEquipped(Player)`: Curios loaded → `CuriosApi.getCuriosInventory(player)...isEquipped(item)`; otherwise → `player.getOffhandItem().getItem() == <item>` (head slot for helmet-style).
   - Attribute effects: `addTransientModifier(new AttributeModifier(FIXED_UUID, name, amount, Operation.MULTIPLY_BASE))` on `Attributes.ATTACK_DAMAGE`, re-checked every 20 ticks in `TickEvent.PlayerTickEvent`; always `removeModifier(FIXED_UUID)` first so unequip/zero-bonus clears it.
   - One-shot effects (e.g. damage-taken multipliers) apply directly in `LivingDamageEvent`/`LivingHurtEvent`.
3. **Curios capability handler** (`common/handlers/items/<name>/<Name>CuriosHandler.java`) — attaches `CuriosCapability.ITEM` on `AttachCapabilitiesEvent<ItemStack>`, guarded by `isCuriosLoaded()`. Because the JVM resolves classes lazily, Curios types are only touched when Curios is actually installed; the mod compiles and runs without it.
4. **Registration** — `RegistryObject` in `EverlaartifactsModItems`, `tabData.accept(...)` in `EverlaartifactsModTabs.EVERLA_TWEAKER`, item model JSON in `assets/everlaartifacts/models/item/<name>.json`.
5. **Lang keys** in all four files (`en_us`, `zh_cn`, `lzh`, `zh_meme`).

Hardware-based trinkets read the wearer's reported hardware: the client reports RAM/VRAM at login via `ClientHardwareInfoPacket`, stored per-player by `PerformanceMetrics.setPlayerHardwareInfo`; the server handler reads `PerformanceMetrics.getPlayerVramMB(player)`; tooltips read the local client cache (`getCachedVramMB()`, `getClientGpuName()`).

For real-time per-frame trinkets (Glasses, Deep Seek, Gaming Cattle) the client computes the attribute result **locally** and sends **one packet per trinket carrying only the result to apply — never raw hardware** (FPS/CPU/window). `ClientPerformanceHandler` gates each send: only when that trinket is equipped (head/off-hand + Curios via `hasTrinketEquipped`) AND the computed result changed since last send (caches `lastSent*`; reset on logout). Packets: `ClientGlassesBonusPacket`/`ClientDeepSeekBonusPacket` (the `double` MULTIPLY_BASE bonus) and `ClientGamingCattleEffectPacket` (an `int` effect mask via `GamingCattleItem.targetEffectMask`/`effectsFromMask`, shared by both sides so thresholds stay in sync). The server handlers apply the stored result directly (`PerformanceMetrics.getPlayerGlassesBonus`/`getPlayerDeepSeekBonus`/`getPlayerGamingCattleMask`).

### Custom music discs

Moved to the sibling **EverlaDiscs** mod. This mod keeps only cross-mod soft references to a few discs (`NilkItem`, `BadAppleSoundHandler`); the `everlatweaker:penis_music` tag that powers the Manbo-disc explosion is defined in EverlaDiscs.

### Mob effects with screen overlays

Blood Blossom, American Style Cut, Waaooo, Genshin Start, Homa Active/Passive, Venus Shell, Bedmic Destruction, Blitzkrieg, Cognitive Disorder, Lethal Poison, Nuclear Water Radiation.

### Difficulty system

Custom `DifficultyLevel` enum extends vanilla with `LUNATIC` and `EXTRA` levels. Controlled via game rules.

### Other features

- **Projectiles**: Angolmois Doom (custom model/renderer), Firecracker
- **Fluid**: Nuclear Waste Water (Create mod compat)
- **Performance tracking**: Client reports hardware specs; server calculates performance score
- **Everlasting tag**: Items tagged `everlatweaker:everlasting` are unbreakable (the mixin now lives in the EverlaTweaker subproject)
- **Brackets Blade**: Sword damage scales with `「」` bracket pairs in custom name
- **Procedure Sword**: Instant-kill weapon using Mixin `@Invoker` for loot drops instead of fragile Java reflection

### Data-Driven Systems

- The **data-driven tweak tags** (everlasting / fire_resistant / explosion_resistant / rainbow_name / rainbow_lore) and their handlers + mixins were moved to the **EverlaTweaker** subproject (`src/JarJar/EverlaTweaker`), under the `everlatweaker:` namespace. This mod's own data folders that remain:
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
