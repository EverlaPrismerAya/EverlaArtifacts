# EverlaArtifacts

A Forge 1.20.1 mod born from the author's wildest musings.

## Building

Requires **Java 17**. Run the following from the project root:

```bash
./gradlew build                  # Build the mod jar (automatically builds and bundles EverlaTweaker)
./gradlew :everlatweaker:build   # Build EverlaTweaker only
./gradlew runClient              # Launch the Minecraft client
./gradlew runServer              # Launch a test server
```

Build artifacts are located in `build/libs/`. **For distribution, use the jar with the `-all` suffix** (which bundles its Jar-in-Jar dependencies):

| Artifact | Description |
|---|---|
| `EverlaArtifacts-<version>-forge-1.20.1-all.jar` | **Distribution jar** (contains the embedded mod) |
| `EverlaArtifacts-<version>-forge-1.20.1.jar` | Slim jar without the embedded mod |

### Jar-in-Jar Subproject: EverlaTweaker

The data-driven systems (rainbow names, rainbow item lore, fire resistance, explosion resistance, and unbreakable items) live in the Jar-in-Jar subproject
`src/JarJar/EverlaTweaker` (modId `everlatweaker`). It is built together with the root project and automatically bundled into the `-all` jar,
**no separate build or install required**; installing EverlaArtifacts alone is enough to get every feature.

To build the subproject on its own: `./gradlew :everlatweaker:build` (artifacts in `src/JarJar/EverlaTweaker/build/libs/`).

> Note: after changing EverlaTweaker's source, just run `./gradlew build` again — the build will automatically recompile and bundle the latest code.

### Standalone Mod: EverlaDiscs

The music discs (about 29) are a **standalone mod** `EverlaDiscs` (modId `everladiscs`), licensed All Rights Reserved,
and are **not embedded**. The two mods have no forced dependency on each other.

### Dependencies

- **EverlaTweaker** — required (bundled via Jar-in-Jar, provided automatically)
- **JEI** — optional
- **Curios API** — optional

## Open-Source Notice

This project uses the following open-source software/code:

https://github.com/Nova-Committee/Re-Avaritia , licensed under the MIT License.

Copyright (c) 2024-2026 Nova-Committee