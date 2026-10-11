# Everfall

Everfall is a client-side Minecraft mod scaffold for decorative, seasonal particles on GUI screens. The first implementation renders a small, configurable field of falling particles; airborne particles persist until they leave the screen, while settled particles can fade out and expire. Settled particles are cleared when screens change, and all particles are cleared when window dimensions change. Wind and gravity are configurable; particles fall from above and join from the upwind screen edge, cursor movement sweeps nearby particles, and click-dragging carries a particle with cursor inertia on release.

## Supported builds

| Minecraft | Fabric | NeoForge |
|---|---:|---:|
| 1.21.1 | yes | yes |
| 26.1 | yes | yes |
| 26.2 | yes | yes |
| 26.3 | yes | yes |

## Development

- Use Java 21 for Minecraft 1.21.1 and Java 25 for 26.x.
- Build a specific variant with `gradlew :<version>-<loader>:build`, for example `gradlew :26.3-fabric:build`.
- Build all variants by invoking `build` for each version/loader project.

The shared particle engine and frame contracts are under `src/main/java/com/yori3o/everfall/core`. Loader entrypoints are kept in `src/fabric/java` and `src/neoforge/java`; Minecraft-version-sensitive rendering and identifiers are in `src/mc/legacy/java` and `src/mc/modern/java`.

Settings are stored in `config/everfall.json5` through YACL. Fabric users can open them through ModMenu; NeoForge registers the same YACL screen in its mod config UI.
