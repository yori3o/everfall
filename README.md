# Everfall

Everfall is a client-side Minecraft mod scaffold for decorative, seasonal particles on GUI screens. The first implementation renders a small, configurable field of falling particles; airborne particles persist until they leave the screen, while settled particles can fade out and expire. Settled particles are cleared when screens change, and all particles are cleared when window dimensions change. Wind and gravity are configurable; particles fall from above and join from the upwind screen edge, cursor movement sweeps nearby particles, and click-dragging carries a particle with cursor inertia on release.

By default, particle motion is simulated in display-pixel coordinates so changing the GUI scale does not change its screen-space speed or make movement jump by multiple display pixels. The movement pixel mode setting can switch to GUI pixels, where motion follows the interface grid and scales with GUI scale. Switching mode resets particles; particle size continues to follow the GUI scale in both modes. Sprite animation pauses while a particle rests on a widget or is held with the mouse, then resumes from the same point. Stronger configured wind shifts more of the unchanged spawn rate from the top edge to the upwind side edge; at maximum wind, the target split is 40% top and 60% side.

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

Input, particle physics, and drawing update on every rendered frame using elapsed frame time (with a 0.1-second stall clamp). The configured maximum is the only particle-count limit; buffers grow as needed. Sweep audio allows one active sweep sound per particle and up to six active sweep sounds overall. Any sweep starts a randomized 150-450 ms cooldown for all sweep sounds and a separate 600-1800 ms cooldown for the same audio file. Each duration is sampled once per playback. Requests during either cooldown are skipped. Sweep volume is 40% for resting particles and 70% for airborne particles, with no sound below the interaction speed threshold. Capture sounds play at 70% volume, bypass these limits, and are not tracked.

Seasonal profiles switch automatically by default, using the local date and hemisphere inferred from time zone, distinct system locale regions, and Minecraft language. Geographic time zones have the strongest weight; default English has only a weak vote. With no usable evidence, the northern hemisphere is used. The local IANA data requires no network access. Startup logs report all hints and the selection; date and language changes are detected during play. Disable automatic selection in General settings to use the manual profile. Automatic switching preserves physics settings. Spring and summer are initially copies of winter and share its textures and sounds.

Automatic selection displays the current seasonal profile and disables its manual selector until automatic mode is turned off. The single Particle count setting controls both the live limit and spawn rate (200: 6 spawns/s; 400: 12 spawns/s). Zero disables particles. The existing maxParticles config key is retained; the old density field is ignored.

General settings include a master volume slider (0-100%) for all Everfall sounds. Zero mutes the mod; the default 100% preserves existing sound levels.

Independent additional profiles can run together with the main seasonal effect. Features settings enable each layer separately. Built-in fireflies slowly wander and pulse in the lower third in spring and summer between local 22:00 and 04:00. All layers share the configured particle count. See [additional profile definitions](docs/ADDITIONAL_PROFILES.md) for custom layers, overrides, and annual holiday conditions.

Profile textures support relative spawn weights and optional one-cycle alternative animations triggered by chance and cooldown conditions. Existing profiles remain compatible. See [texture animation format](docs/TEXTURE_ANIMATIONS.md) for details.
