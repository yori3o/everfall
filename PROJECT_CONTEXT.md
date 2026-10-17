# Everfall — Project Context

> **Audience:** coding agents (and the human owner). Read this whole file before touching code.
> **Status:** early implementation. The architecture and roadmap below describe the target. Current code includes the particle engine, four seasonal profiles, interaction sounds, and basic YACL settings. Explicit current decisions below supersede the original plan.
> **Working title:** `Everfall`. Placeholder mod id: `everfall`. Package: `com.yori3o.everfall`.
> **License: MIT** (code and built-in assets, unless a file says otherwise).
> There is **no deadline**. The mod is released only when it is complete and polished.
> Facts marked **[VERIFY]** came from secondary sources or memory. Check them against official docs/sources before relying on them.

---

## 1. One-paragraph summary

A **client-side** Minecraft mod that draws decorative, seasonal, physically-playful particles over GUI screens (primarily the main menu, also pause, options, inventory, etc.): falling leaves, snow, rain, drops, frost, insects, petals, and so on. The scene is chosen **automatically** from the device date, time zone and locale (so the player never has to switch it), but is fully overridable. Particles **interact with the GUI**: they land on buttons and can be swept off with the mouse cursor. Soft ambient **sounds** accompany them. Rare **easter eggs** exist (e.g. a snowflake shaped like a creeper face). Visual styles are **data-driven** (JSON + textures in `assets`, overridable by resource packs, and overridable per-option from the user config). The config is **large and deep** (performance, season/style, features, volume, ...). It works on **Fabric and NeoForge**, Minecraft **1.21.1, 26.1, 26.2, 26.3**, from a single Stonecutter-managed source tree, with only **two third-party dependencies**: ModMenu (Fabric only) and YACL.

The main product value is **polish and the interaction feel**, not particle count. Existing mods draw falling snow in menus; none (as far as we know) combine multi-version support, automatic seasons, widget physics, sound, and deep configuration.

---

## 2. Goals and non-goals

### Goals (in priority order)
1. **Feels great in a screenshot/GIF:** leaf lands on a button, cursor sweeps it away.
2. **Zero-effort for the player:** correct season/scene automatically; sane defaults; never annoying.
3. **Cheap to maintain across Minecraft versions:** new MC version = rewrite a few adapter functions, not the mod.
4. **Cheap to run:** negligible FPS impact at default settings; scalable presets.
5. **Deeply configurable** but with a clean default experience: basic knobs and switches in the GUI, precise parameters in the config file.
6. **Plug-and-play for players, extensible for modpack authors:** a built-in universal style pack works with zero setup; modpack makers can ship their own resource packs and configs following a small public wiki (§6.8).

### Non-goals (do NOT build these)
- No gameplay changes, no server component, no networking, no new blocks/items/entities.
- No in-world weather/particle replacement (other mods do this). Scope is **GUI screens** only.
- No network requests, no geolocation APIs, no telemetry. Region is inferred **only** from local time zone + locale.
- No third-party libraries beyond ModMenu and YACL. No Architectury API. No Kotlin.
- Forge (non-Neo) is out of scope.

---

## 3. Targets

| Minecraft | Fabric | NeoForge | Java | Build style | Window/input backend |
|---|---|---|---|---|---|
| 1.21.1 | yes | yes | 21 | obfuscated → *remapping* Loom | GLFW |
| 26.1 | yes | yes | 25 | unobfuscated → *non-remapping* Loom | GLFW |
| 26.2 | yes | yes | 25 | unobfuscated | GLFW |
| 26.3 | yes | yes | 25 | unobfuscated | SDL3 |

Known facts (from Fabric/NeoForge docs):
- 26.1 is the first unobfuscated release. Mods for 1.21.11 and older do not work on it even as compile-only dependencies; Fabric uses the plugin `net.fabricmc.fabric-loom` there (no `modImplementation`/`remapJar`, use `implementation`/`jar`).
- Fabric API renamed several things to Mojang names in 26.1 (e.g. `HudRenderCallback` removed in favor of `HudElementRegistry`, keybinding → keymapping naming). Do not rely on remembered Fabric API names; check the version-specific docs/sources.
- Mappings policy (**decision**): use **Mojang official mappings** on all versions, including 1.21.1, so names stay as close as possible across targets. Yarn is not used.
- **[VERIFY]** NeoForge builds for the newest MC versions may be beta or lag behind Fabric. Maintain `docs/COMPAT_MATRIX.md` with the exact loader/MC versions that are actually built and tested.

Primary development target (**decision, revisit at M0**): the newest version where **both** loaders have a usable build. Port downward; do 1.21.1 last (largest API delta).

---

## 4. Golden rules for the agent

1. **Read before writing.** Never guess Minecraft/Fabric/NeoForge/YACL API names. With unobfuscated 26.x and Mojang mappings on 1.21.1 you can read the actual sources in the dev environment. When unsure, open the source or docs; do not invent.
2. **Dependency direction is one-way:** `platform → adapter → core`. Core never imports adapter or platform.
3. **`core` is Minecraft-free.** Allowed imports in `core`: JDK (`java.*`), Gson, SLF4J, and — **only inside `core.config`** — YACL's annotation/config-class API for the config POJO (see §6.7). Anything `net.minecraft.*`, `net.fabricmc.*`, `net.neoforged.*`, `com.mojang.*`, GLFW/SDL classes is forbidden in core. This is a convention reviewed by humans, not enforced by tooling.
4. **All Minecraft-version differences live in `mc/compat/`.** Stonecutter conditionals (`//? if ...`) are allowed **only** in: `mc/compat/**`, loader modules, build scripts, and metadata templates. If you need a conditional elsewhere, the abstraction is wrong: add a method to a compat class instead.
5. **Direct GLFW / SDL3 calls are allowed where necessary, but only through abstract utilities** in `mc/compat/` (e.g. `WindowInput`; see §9.2). No other class may touch GLFW/SDL directly, and no class outside `mc/compat` may know which backend a version uses.
6. **No per-frame allocation in the hot path** (particle step and render submission). Use preallocated arrays, pools, primitive buffers.
7. **Everything user-visible is configurable and can be disabled.** Sounds, easter eggs, interactions, each effect type, each screen.
8. **Fail soft.** A broken style JSON, missing texture, or compat error must log once and degrade (skip the effect / use fallback), never crash the game or spam logs.
9. **Seedable core.** All randomness goes through an injected RNG that can be seeded (useful for reproducing visual bugs).
10. **Don't ship Mojang assets.** All textures/sounds are original. Easter-egg sprites are original pixel art *inspired by* Minecraft, not extracted game textures.
11. **Small, reviewable changes.** One feature/subsystem per commit. Keep this file updated when decisions change.

---

## 5. Architecture

### 5.1 Layers

```
┌─────────────────────────────────────────────────────────────┐
│ platform/fabric, platform/neoforge   (loader glue only)     │
│  - entrypoints, metadata, screen hooks, resource reload,    │
│    config-screen registration, mod-loaded checks            │
├─────────────────────────────────────────────────────────────┤
│ mc  (adapter: Minecraft ↔ core)                             │
│  - InputCollector  : Screen → FrameInput                    │
│  - SpriteRenderer  : SpriteBuffer → pixels on screen        │
│  - AudioBridge     : SoundCue → Minecraft sound system      │
│  - AssetBridge     : ResourceManager → core AssetReader     │
│  - ConfigBootstrap : YACL ConfigClassHandler + screen       │
│  - mc/compat/**    : the ONLY place with version conditionals│
│                      and direct GLFW/SDL3 access            │
├─────────────────────────────────────────────────────────────┤
│ core  (pure Java)                                           │
│  - ParticleEngine, physics, interaction                     │
│  - SeasonResolver, regions/holidays data                    │
│  - StyleRegistry (JSON → typed styles)                      │
│  - config POJO (core.config, YACL-annotated)                │
│  - AudioDirector (decides cues, no playback)                │
│  - EasterEggDirector                                        │
│  - util: math, RNG, pools, rect/geometry, logging facade    │
└─────────────────────────────────────────────────────────────┘
```

The owner's stated preference: core and Minecraft need not be *religiously* separated, but **platform / GUI input-output / core must be separated**. The rules above implement exactly that boundary and nothing stricter.

### 5.2 Repository layout (logical; adapt to the actual Stonecutter template)

```
/
├─ PROJECT_CONTEXT.md            ← this file
├─ settings.gradle.kts           ← registers versions × loaders
├─ stonecutter.gradle.kts        ← Stonecutter controller
├─ gradle.properties
├─ versions/<mc>/gradle.properties   ← per-version deps (loader, Fabric API, YACL, ModMenu versions)
├─ common/                       ← shared source set used by every loader/version
│  └─ src/main/
│     ├─ java/<base>/core/...
│     ├─ java/<base>/mc/...
│     ├─ java/<base>/mc/compat/...        ← version conditionals + GLFW/SDL3 live here
│     └─ resources/assets/<modid>/{styles,data,textures,sounds,lang}/...
├─ fabric/                       ← fabric entrypoints, fabric.mod.json template, ModMenu entry
├─ neoforge/                     ← neoforge entrypoints, neoforge.mods.toml template
└─ docs/
   ├─ COMPAT_MATRIX.md
   ├─ MANUAL_CHECKLIST.md        ← §11
   ├─ wiki/                      ← small public docs for modpack makers / pack authors (§6.8)
   │  ├─ STYLE_FORMAT.md
   │  ├─ CONFIG.md
   │  └─ MODPACK_GUIDE.md
   └─ DECISIONS.md               ← append-only log of architecture decisions
```

Start from the official `stonecutter-template-multiloader` (Fabric + NeoForge, Java). Learn only what is needed: the version list, `//? if` conditionals, `chiseledBuild`, and switching the active version. Do not restructure the template's build logic without a reason recorded in `docs/DECISIONS.md`.

### 5.3 Per-frame data flow

Triggered from a **post-render screen hook**, for screens that pass the screen filter (§6.7):

```
loader event(screen, mouseX, mouseY, partialTick)
   → adapter.InputCollector.collect(...)  : FrameInput
   → core.ParticleEngine.step(FrameInput) : FrameOutput
   → adapter.SpriteRenderer.draw(out.sprites)
   → adapter.AudioBridge.apply(out.sounds)
```

- **One engine instance for the whole session**, not one per screen: leaves keep falling while the player moves title → options → back. When the screen changes, obstacles change; resting particles re-evaluate and fall if their support is gone.
- **Coordinates (current decision):** simulation defaults to **display pixels**, origin top-left, +y down, for smoother movement independent of GUI scale. The adapter converts cursor/widget coordinates into display pixels and rendering back into GUI coordinates; sprite sizes still follow GUI scale. `pixelMode = DISPLAY | GUI` in YACL/config selects display pixels (default) or GUI pixels for motion, input, obstacles, and rendering. GUI mode follows GUI scaling; sprite size remains unchanged on screen. Switching mode resets particles and cursor tracking to avoid mixing coordinate systems.
- **Time (current decision):** input, physics, and output update together on **every rendered frame**, using real frame `dt` in seconds (clamped to ≤ 0.1 s to avoid jumps after a stall). There is no fixed 60 Hz simulation. Actual cadence follows Minecraft rendering/FPS and VSync settings; cursor sweep impulses scale with `dt`.

### 5.4 Core contract (sketch — refine, but keep the shape)

```java
// core/api
public record Rect(float x, float y, float w, float h) {}

public record FrameInput(
    int screenW, int screenH,          // GUI-scaled
    float dt,                          // seconds
    float cursorX, float cursorY,      // GUI-scaled
    boolean cursorOnScreen,
    boolean mousePressed,
    ObstacleList obstacles,            // widgets as rects (+ flags), reusable, no per-frame alloc
    boolean inWorld,                   // a world is loaded behind this screen (false on title/menus)
    ModConfig config) {}               // YACL-annotated POJO, see §6.7

public interface ParticleEngine {
    void reset(long seed);
    FrameOutput step(FrameInput in);   // returns a reused FrameOutput instance
}

public final class FrameOutput {
    public final SpriteBuffer sprites; // parallel primitive arrays
    public final SoundCueList sounds;  // reused list
}

public final class SpriteBuffer {
    public float[] x, y, rotation, scaleX, scaleY, alpha;
    public int[]   spriteId;           // index into the style's sprite table
    public byte[]  layer;              // BACK=0, FRONT=1 (see §9.4)
    public int count;
}
```

Core-defined ports implemented by the adapter/platform (kept tiny):

```java
public interface AssetReader {              // adapter wraps ResourceManager
    Optional<String> readText(String path);
    List<String> list(String directory);
}
public interface Environment {              // adapter/platform wraps system info
    LocalDate today();  ZoneId zone();  Locale locale();
    boolean isModLoaded(String modId);
}
```

(There is no config-storage port: YACL loads/saves the config itself, see §6.7.)

Rendering and audio *playback* are **not** core ports. Core only emits data (`SpriteBuffer`, `SoundCue`); the adapter consumes it. This keeps the core independent of how things are drawn.

---

## 6. Subsystems

### 6.1 Particle engine (core)

**Current additional-profile system:** `AdditionalParticleSystem` composes independent feature engines with the main `BasicParticleEngine`. Each additional JSON profile has its own motion parameters, population weight, and season/hour/annual-date conditions. Multiple selected IDs in `additionalProfiles` may run simultaneously. YACL Features contains one independent switch per definition. Built-in fireflies are selected by default and active only in spring/summer during local `[22:00,04:00)`; clock sampling is cached for one second, and animation/motion update each frame. The wandering, pulsing procedural firefly glow stays entirely in the lower third. Feature particles do not land, sweep, capture, or produce audio. One configured `maxParticles` budget is shared between main and active additional layers, by their profile count weights; disabled/unmatched layers reserve nothing. `FrameInput.particleLimit` carries the main layer's allocation within this global limit. Definitions support config-directory overrides and new profiles with soft failure. See `docs/ADDITIONAL_PROFILES.md` for the complete current format and holiday examples. Resource-pack discovery/reload remains future work.


- **Storage (current decision):** structure-of-arrays; simulation and output buffers grow when configured `maxParticles` requires more capacity. There is no additional engine/render cap. Lowering the config limit immediately trims the live count. No objects per particle; no allocation at unchanged capacity.
- **Particle state machine:** `FALLING → RESTING (on a rect) → (SWEPT | MELTING/FADING | FALLING again)`; plus type-specific states (e.g. insects: `HOVERING/FLYING`, frost: `GROWING → MELTING`).
- **Animation (current decision):** sprite frame and fractional animation time pause while grounded or mouse-dragged; animation resumes from that exact point when airborne again. Profile-level `flipWithWind` defaults to false; when enabled, negative configured wind mirrors all particle UVs on X, with original orientation for positive/zero wind, including alternative animation. Per-texture `scale` defaults to 1 and scales particle size/collision bounds for both animations without changing source frame geometry or speed. Texture weights default to 1 (0 disables a texture); selection is seeded and weighted at spawn. Optional `alternativeAnimationId`, `conditionsForAlternativeAnimation` (chance/cooldown), and `conditionType` (both/any) choose a one-cycle alternative at main-cycle boundaries. Both sheets use the same layout/timing. Cooldown is per particle, from spawn then the end of its last alternative; it continues during resting/dragging while animation is paused. No built-in profile definitions were changed. See `docs/TEXTURE_ANIMATIONS.md` for the exact format and defaults.
- **Motion model per effect (data-driven):** fall speed range, wind (global + per-particle gust noise), sway (amplitude/frequency), rotation speed, size/alpha ranges, lifetime, spawn rate and spawn area, depth factor (size/alpha/speed scaling to fake parallax).
- **Current spawn-rate decision:** `maxParticles` is the single particle-count option, controlling both the hard live limit and `spawnRate = maxParticles * 0.03` per second. Initial fill is 30% of that count. There is no separate density field in config or profile defaults; legacy density fields are ignored. A count of 200 produces 6 particles/s; 400 produces 12 particles/s. Zero clears particles and disables spawning.
- **Spawning:** accumulator-based spawn rate, scaled by season intensity × performance preset × config density multiplier.
- **Cull:** particles leaving the screen are recycled immediately.

### 6.2 Interaction / physics (core)

- **Obstacles:** list of axis-aligned rects (from widgets). Number is small (≈ 5–40): brute-force tests are fine; add a grid only if profiling says so.
- **Landing:** a falling particle crossing the *top edge* of an obstacle rect has a `landing_chance` (per effect) to land. Landed particles store `supportId` + relative x. They follow the obstacle if it moves (animated/hover-shifted widgets) and drop if the obstacle disappears or the screen changes.
- **Cursor:** modeled as a moving circle with velocity. Particles within the radius receive an impulse proportional to cursor speed and inverse mass; resting particles are knocked loose ("swept") when the cursor passes over them. Optional light wind-push for airborne particles. Mouse press may optionally trigger bursts/puffs (configurable).
- **Accumulation (stretch goal):** snow depth per obstacle top edge in coarse buckets, melting when hovered/over time; leaves pile slightly. Must stay cheap and be optional.
- **Hidden/transparent widgets:** the adapter should exclude widgets that are not visible or fully transparent (`active/visible` flags, alpha where accessible) so particles don't land on invisible buttons.
- Keep all tunables (mass, bounce, friction, sweep strength) in styles/config, not hardcoded.

### 6.3 Season resolution (core)

**Current implementation:** `autoSeason` defaults to true; the selected season is mirrored into `profile`; its YACL selector is visibly disabled while auto mode is on. Toggling auto mode updates the selector immediately without committing unsaved changes. Disabling auto mode unlocks manual `profile`. Spring and summer initially copy winter's assets and defaults. Meteorological seasons are reversed for the southern hemisphere. Bundled public-domain IANA `zone.tab` + `backward` supply geographic time-zone/country hemisphere hints. The time zone contributes weight 8, each distinct system-locale country (default/FORMAT/DISPLAY) contributes 2, and the Minecraft language's country contributes 1. Mixed-hemisphere countries and fictional Pirate/upside-down locales supply no vote. Ties/no evidence fall back to northern seasons. These scores are heuristic evidence, not probabilities or geolocation. Startup and changed inputs are logged with date, zone, source hints, scores, hemisphere, fallback, auto/manual mode, and selected profile. A cached check once per second catches local date/zone/locale changes; language/config changes apply immediately. Automatic selection preserves user physics settings; manual profile changes retain the existing recommended-default behavior. No network access occurs at runtime. Climate/holiday selection and smooth transitions below remain planned.


Inputs: `Environment` (date, zone, locale) + config overrides. Output: a **scene selection** = chosen style id + a set of effect intensities (e.g. `leaf: 0.8, rain: 0.2`).

1. **Hemisphere & climate profile** are looked up from `assets/<modid>/data/regions.json`, which maps time-zone IDs/prefixes (and locale country as a fallback) → `{hemisphere, climate}`. Climates: `temperate`, `tropical`, `arid`, `polar` (extend by data only).
2. **Season** from date + hemisphere (meteorological seasons: N: Dec–Feb winter, Mar–May spring, Jun–Aug summer, Sep–Nov autumn; S is mirrored). **Smooth transitions:** within ±N days of a boundary, mix both seasons' intensities so there's no hard switch.
3. **Climate adaptation:** e.g. `tropical` has no snow by default (wet/dry-season rain, petals, fireflies instead); `polar` skews to snow/frost. Manual override can always force anything (including snow in the tropics).
4. **Holidays** from `data/holidays.json` (date windows, optionally per-hemisphere/region): e.g. Halloween (bats, dry leaves), winter holidays/New Year (snow + lights/sparkles), spring (petals). Holiday style can take priority over the plain season style if the config allows.
5. **Overrides:** `season = AUTO | SPRING | SUMMER | AUTUMN | WINTER | OFF`, forced hemisphere/climate, fake date (debug), disable holidays.
6. **Style selection:** each style declares `match` tags (`season`, `holiday`, `climate`, `priority`). Resolver picks the best match; config can pin a style id.

No network, no GPS. Mention this in the mod description (privacy-friendly point).

### 6.4 Styles — data-driven (core + assets)

- Location: `assets/<namespace>/<modid>/styles/*.json` (final folder name decided when implemented; document in `docs/wiki/STYLE_FORMAT.md`). **Resource packs can add/replace styles.**
- A style = metadata + sprite table (texture + UV region, so one atlas per style) + list of effect definitions + ambient audio mapping + easter-egg references.
- Parsing is in `core` (Gson). Loading goes through `AssetReader`; reload on resource reload (loader glue registers the reload listener).
- **Validation:** every field has a default or a clear error; unknown fields are ignored with one warning; invalid style is skipped, a built-in minimal fallback style always exists.
- **Schema versioning:** `"schema": 1`; keep a migration path.

Example (shape, not final):

```json
{
  "schema": 1,
  "id": "menuseasons:autumn_default",
  "match": { "season": ["autumn"], "climate": ["temperate"], "priority": 10 },
  "atlas": "menuseasons:textures/particle/autumn.png",
  "sprites": {
    "leaf_oak":   { "uv": [0, 0, 16, 16] },
    "leaf_maple": { "uv": [16, 0, 16, 16] }
  },
  "effects": [
    {
      "id": "leaf",
      "sprites": [ { "id": "leaf_oak", "weight": 3 }, { "id": "leaf_maple", "weight": 2 } ],
      "spawn":   { "rate": 14, "max": 160, "area": "top" },
      "motion":  { "fall_speed": [28, 60], "wind": [-6, 12],
                   "sway": { "amp": [6, 14], "freq": [0.6, 1.4] },
                   "rotation_speed": [-90, 90], "depth": [0.5, 1.0] },
      "look":    { "size": [0.6, 1.1], "alpha": [0.75, 1.0] },
      "interaction": { "lands_on_widgets": true, "landing_chance": 0.35,
                       "rest_time": [4, 12], "sweepable": true, "mass": 0.4 },
      "sounds":  { "land": "menuseasons:leaf_land", "sweep": "menuseasons:leaf_sweep" }
    }
  ],
  "ambient": [ { "sound": "menuseasons:amb_autumn_wind", "volume": 0.4 } ],
  "easter_eggs": [ "menuseasons:creeper_leaf" ]
}
```
Units: GUI px, px/s, seconds, degrees.

**Config overrides on top of styles:** effective value = `style default` ← overridden by `user config override` if present. Core exposes an `EffectiveStyle` that already merges both; the engine only sees the merged result. **Decision:** precise per-effect parameters are edited **in the config file only** (the `advanced` block, §6.7); the GUI exposes only basic knobs and switches.

### 6.5 Audio (core decides, adapter plays)

**Current master-volume setting:** `masterVolume` is a YACL slider in General, 0-100 percent, default 100. It multiplies every sweep/capture cue at playback, retaining their relative state volumes. Zero skips playback without consuming sweep slots or cooldowns. This setting is independent of seasonal profile defaults.

- **Current interaction-audio decision:** sweep/capture cues carry a session-unique particle ID. Playback limits apply **only to sweep**: one active sweep sound per particle and at most **six** active sweep sounds overall. New sweep cues are skipped while the particle or all six slots are busy. Capture sounds bypass the limiter entirely and do not occupy slots. Any sweep playback starts independently randomized cooldowns with uniform +/-50% spread: global sweep 150-450 ms (300 ms base), and per resolved audio file 600-1800 ms (1.2 s base), measured with monotonic time. Durations are sampled once after playback and are not rerolled or extended by ignored requests; duplicate cues are dropped, never queued. The random variant is resolved once and retained for playback. Sweep volume is 0.4 for grounded particles and 0.7 for airborne particles; capture volume is 0.7, measured before the interaction changes their state. Sweep audio uses the same cursor-speed gate as physics (currently >160 motion pixels/s); speed gates playback rather than scaling volume. Slots are released according to Minecraft sound playback state, rather than a guessed clip duration.
- `AudioDirector` (core) emits `SoundCue { soundId, volume, pitch, pan, kind }` and loop-control cues. It handles rate limiting (max N one-shots/sec, per-sound cooldown), randomization of pitch/volume, and ambient layer crossfades when the scene changes.
- One-shots: leaf lands, sweep swish, snow soft tick, raindrop patter, easter-egg sounds. Ambient loops: wind, rain bed, insects, etc.
- Volume model: **master mod volume**, per-category sliders (ambient / interaction / easter eggs), and per-screen mute. Final volume also respects Minecraft's own volume settings — **decision to confirm when implemented:** which Minecraft sound category the mod's sounds use, and whether sounds can play on screens where the sound engine/menu music is paused or not yet ready.
- Assets: original `.ogg`, authored by the owner. Guidelines: very quiet by default, seamless loops, no clipping, consistent loudness across files, short one-shots (< 1.5 s). Registered through `sounds.json` (`assets/<modid>/sounds.json`).
- Everything default-on but gentle; one global switch mutes the whole mod's audio.

### 6.6 Easter eggs (core)

- Defined as **data** (`data/easter_eggs/*.json` or inside styles): `id`, sprite/variant to use, `spawn_chance_per_minute`, conditions (season/holiday/screen), optional click behavior, sound.
- Example: a snowflake sprite shaped like a creeper face, rare (about one per several minutes at most); clicking/hovering may trigger a tiny harmless "pop" with a soft hiss sound. Must never block UI, never obscure buttons, never live longer than a normal particle.
- Global toggle + "frequency" slider (0 = off) in config.

### 6.7 Config — YACL end to end

YACL is used for **everything config-related**, not only the GUI: config class definition, JSON (de)serialization, load/save, and the settings screen. Do **not** write a custom schema system, config store, or serializer.

- **Config POJO** `ModConfig` lives in `core.config` and is annotated with YACL's config-class API (serial entries, auto-gen option annotations) so that fields are saved and the GUI can be generated from it. This is the one allowed YACL import location in `core`. Core reads values straight from this object each frame. **[VERIFY]** exact annotation names and packages for the YACL version on each target (YACL differs between 1.21.1 and 26.x builds).
- **Handler/bootstrap** (`ConfigClassHandler`, serializer, config file path, identifier) is created in `mc/` (`ConfigBootstrap`), since it needs Minecraft types. Any YACL API differences between versions go in `mc/compat/`.
- **GUI:** use YACL's auto-generation. The GUI contains only basic knobs and switches (see below), so everything should map onto annotated fields; use the manual builder only for something auto-gen truly can't express. Translations through lang files following YACL's key conventions **[VERIFY]**.
- **Entry points:** Fabric → ModMenu API entrypoint in `fabric/`; NeoForge → its own config-screen extension registration in `neoforge/`. Both open the same YACL screen.
- **Categories (initial):**
  - **General:** master enable, screen filter (mode + list, see "Screen filtering" below).
  - **Season & Style:** `AUTO/manual season`, hemisphere/climate override, fake date, holidays on/off, pinned style id, per-effect enable, global multipliers (density, speed, size, opacity, wind). Precise per-effect parameters are **not** in the GUI (see "Basic vs advanced options").
  - **Interaction:** landing on widgets, sweep strength, cursor radius, bursts on click, accumulation, ignore hidden widgets.
  - **Audio:** master, ambient, interaction, easter eggs, mute per screen.
  - **Performance:** preset (`OFF/LOW/MEDIUM/HIGH/ULTRA/CUSTOM`), max particles, simulation rate/substeps cap, auto-throttle when frametime exceeds threshold.
  - **Easter eggs:** enable, frequency.
  - **Compatibility:** placeholder category (see §7), empty at first.
  - **Debug:** overlay (particle count, step time), draw obstacle rects, fixed seed, log level.
- Config changes apply live where possible (core reads the POJO each frame; style-affecting options trigger a style re-resolve).

#### Basic vs advanced options
- **GUI (basic):** enable, season mode, pinned style, per-effect on/off, global multipliers (density, speed, size, opacity, wind), performance preset, volumes, interaction switches, easter-egg frequency, screen-filter mode/list.
- **File only (advanced):** precise per-effect parameters live in the same config JSON, in an `advanced` block: sparse entries keyed by `effectId.path` (e.g. `leaf.motion.fall_speed`). No GUI for them. Invalid or unknown entries are ignored with one warning. Documented in `docs/wiki/CONFIG.md`.
- **Precedence:** `style value` ← replaced by `advanced override` if present ← then multiplied by the basic global multipliers.
- **[VERIFY]** that a map-like `advanced` field serializes cleanly through YACL's Gson serializer and how externally edited files get reloaded. Fallback if awkward: a separate hand-edited JSON read by core.

#### Screen filtering
- Logic is based on Minecraft's abstract `Screen` base class: **every** screen is a candidate; there is no hardcoded list of supported screens.
- Config: `screenFilter.mode = BLACKLIST | WHITELIST` and `screenFilter.entries = [...]`. Entry kinds: a **logical id** for vanilla screens (e.g. `title`, `pause`, `options`, `inventory`), a **fully qualified class name** for modded screens, or a `prefix.*` wildcard.
- Why logical ids: runtime class names of vanilla screens may differ by version/loader (e.g. obfuscated runtime names on Fabric 1.21.1 **[VERIFY]**). `mc/compat/ScreenIds` maps known vanilla screens, via compile-time class references that the build remaps automatically, to stable ids; unknown screens fall back to their class name.
- The decision is evaluated once per screen change and cached; a disallowed screen costs ~nothing.
- The debug overlay shows the current screen's id, so filling the list is easy.
- **Default behavior (decision):** particles are shown on all screens **when no world is loaded** — i.e. every main-menu screen and every settings/sub-screen opened from the main menu (title, world select, multiplayer, options, mods lists, etc.). Screens opened over a loaded world (pause, inventory, chat, signs, anvil, options opened from the pause menu, ...) are **off by default**.
- This is controlled by `FrameInput.inWorld` (a level is loaded behind the screen) and two basic config options: `showOverWorld` (default **false**) and `overWorldDensity` (multiplier applied only when `showOverWorld` is on; default TBD). The black/white-list is applied on top of this in both cases. The default filter list is empty (BLACKLIST mode) except for any transient/loading screens found to look bad during testing (§14).


### 6.8 Built-in pack and extensibility for modpack authors

- **Built-in universal style pack** ships in the jar: all four seasons, holidays, regions/holidays data, sounds. Download and play, zero configuration.
- **The built-in pack must use only the public style format** (no private fields or special cases), so it doubles as the reference example for pack authors.
- **Modpack makers** can: (a) ship a resource pack that adds/replaces styles, textures, sounds, and `regions.json` / `holidays.json`; (b) ship a preconfigured config file in the modpack's `config/`; (c) pin a style or season through config.
- **Small public wiki** in `docs/wiki/`: `STYLE_FORMAT.md` (every field, units, valid examples), `CONFIG.md` (every option, the `advanced` block, screen filter), `MODPACK_GUIDE.md` (resource-pack structure, recipes: pin a theme, disable on some screens, recommended presets). Keep it short and accurate; update it in the same commit as any format/config change.
- **Art (built-in pack):** original hand-drawn **pixel art in the Minecraft style**, made by the owner. Guidelines: small base sprites (e.g. 8×8 or 16×16), limited palette per sprite, one atlas per style, crisp **nearest-neighbor** sampling (no smoothing/blur) **[VERIFY how the GUI draw path samples textures on each version]**. Sprite on-screen size is defined in GUI pixels so it scales with the GUI scale setting. Rotation of pixel art can look jagged: decide visually in M1 whether sprites rotate freely, rotate in coarse steps, or use pre-drawn frames, and record the decision in `docs/DECISIONS.md`.
- **Licensing:** MIT. Modpack authors may include the mod and the built-in pack freely under its terms. Keep a top-level `LICENSE` file and credit third-party content if any is ever added.
- **Format stability:** style files carry `"schema": N`. Backward-compatible additions keep N. A breaking change bumps N, the old version is still read (with a warning) for at least one release, and the change is noted in the changelog.

---

## 7. Compatibility with other mods (stub)

Not a development priority. The design simply avoids closing the door, and real compatibility testing happens in the **last items of the manual checklist** (§11).

What the architecture already gives for free (do not break it):
- Drawing happens from a **post-render screen hook**, independent of the screen's own layout.
- Obstacles are read from the **live screen every frame**, so layouts that move or restyle buttons keep working.
- `Environment.isModLoaded(id)` exists, so a compatibility profile (e.g. for FancyMenu) can be added later without an API dependency.
- Config has a screen filter (black/white list).

Explicitly **not** planned for now: calling FancyMenu API, reflection into other mods, support for non-widget custom elements. If compatibility problems are found during the final checklist pass, handle them then (manual obstacle rects, compat profile, etc.).

---

## 8. Performance requirements

- Default preset: ≤ ~200 live particles; CPU cost of `step + submit` targeted at **well under 1 ms/frame** on a mid-range machine (measure; record numbers in `docs/DECISIONS.md`).
- Hot path: no allocation, no streams, no boxing, no string ops, no `Optional`/lambda churn.
- Renderer: batch by texture/atlas; one atlas per style; minimize state changes.
- Presets scale: max particles, spawn multipliers, interaction detail (accumulation off at LOW), sound polyphony.
- **Auto-throttle:** if smoothed frametime of our own work exceeds budget, reduce spawn rate/cap temporarily (config-controlled).
- Disabled screens/effects must cost ~nothing (early exit before collecting input).

---

## 9. Platform & rendering notes

### 9.1 Loader glue (`platform/fabric`, `platform/neoforge`)
Only: mod entrypoint(s), registering the client screen hooks (init + post-render) where they are loader events, registering the resource reload listener, config-screen registration (ModMenu on Fabric; NeoForge's mechanism), `isModLoaded`, config dir path, mod metadata. Keep each file tiny; they should be nearly copy-paste mirrors. Logic belongs in `mc`/`core`.

### 9.2 Version compat (`mc/compat`)
One small class per concern, each with a stable API used by the rest of `mc`, with Stonecutter-conditional bodies:
- `GuiDraw` — draw a textured quad (UV region, rotation, scale, alpha, tint) in the GUI. **This is the single most version-sensitive function** (GUI rendering changed significantly between 1.21.1 and 26.x).
- `WindowInput` — everything that needs the window/input backend directly: cursor position and whether it is inside the window, window focus, window/framebuffer size and content scale, and any other low-level queries. Implemented over **GLFW on 1.21.1–26.2 and SDL3 on 26.3+**; callers never know which. If a new low-level need appears, add a method here instead of calling GLFW/SDL elsewhere.
- `ScreenAccess` — get a screen's widgets/children and their bounds/visibility/alpha; whether a world is loaded behind the screen.
- `ScreenIds` — stable logical ids for vanilla screens (class → id), class-name fallback for everything else (see §6.7).
- `Ids` — create resource identifiers/paths (naming changes across versions).
- `SoundBridge` — play/stop sounds and loops.
- `ConfigUi` — any YACL API differences between versions.
When a new MC version breaks the build, the fix should be confined to these files.

### 9.3 What not to do
No GLFW/SDL/OpenGL calls outside `mc/compat/`. No reaching into `RenderSystem` internals from `core` or from `mc` outside `GuiDraw`.

### 9.4 Layers (BACK vs FRONT)
Ideal look: far particles drawn *behind* buttons, near/landed particles *in front*. A reliable "behind widgets" hook may not exist uniformly across versions/screens (the screen background is drawn inside the screen's own render). **Initial approach: draw everything in the FRONT layer** (post-render), using depth scaling/alpha for parallax. `SpriteBuffer.layer` exists in the contract from day one so a BACK layer can be added later per version **[VERIFY feasibility per version]**.

---

## 10. Build & tooling

- **Stonecutter + official multiloader template.** Targets: `{1.21.1, 26.1, 26.2, 26.3} × {fabric, neoforge}` = 8 build nodes.
- Per-version `gradle.properties`: Minecraft version, loader versions, Fabric API, YACL, ModMenu (Fabric only) versions, Java version. Remember: **Java 21 for 1.21.1, Java 25 for 26.x**; make sure toolchains are configured.
- Dev loop: work on the single *active* Stonecutter version; run `./gradlew chiseledBuild` before every merge to ensure all nodes compile; use `runClient` on the active version, and run the manual checklist on the others before releases.
- **Dependencies (hard limit):** YACL (all targets), ModMenu (Fabric only, optional at runtime — do not hard-require it; **[VERIFY]** declare as suggested/optional in `fabric.mod.json`), plus the loader's own API (Fabric API on Fabric). Nothing else.
- **Publishing:** Modrinth and CurseForge. Jar naming: `<modid>-<loader>-<mc>-<version>.jar`. Semver; each MC version/loader is a separate artifact.

### 10.1 Naming/conventions
- `common` compiles at Java 21 language level (lowest target); 26.x nodes run on Java 25.
- Packages: `core.*`, `mc.*`, `mc.compat.*`, `platform.fabric.*`, `platform.neoforge.*`.
- Classes in `core` never contain the words Minecraft/Screen/Widget; use `Obstacle`, `Surface`, `FrameInput`.
- Logging through a small facade (SLF4J) with a "log once" helper for recurring errors.

---

## 11. Manual verification

`docs/MANUAL_CHECKLIST.md` — run per release, per version, per loader. Order matters: the compatibility items are deliberately **last**.

1. Screen filter works in both modes: title, options, pause, inventory, creative, world list, multiplayer list, chat, and a modded screen behave as configured; with defaults, screens opened over a world show nothing, while main-menu screens (and options opened from the main menu) show particles; `showOverWorld` enables the rest; wildcard and class-name entries match; debug overlay shows the screen id.
2. GUI scale 1 / 2 / 3 / 4 / auto; window resize; fullscreen toggle; cursor mapping stays correct.
3. Particles land on buttons and are swept by the cursor; they fall when the screen changes.
4. Season auto-detection (change system date/time zone to test N/S hemisphere, tropics, boundaries, holidays); manual overrides work.
5. Resource pack can override a texture/sound and add a style.
6. Config: every option applies live; defaults sane; presets work; ModMenu (Fabric) and NeoForge config entry both open the same screen.
7. Audio: master/category volumes, per-screen mute, global mute, no clipping, no repeating spam.
8. Easter eggs: appear rarely, are harmless, can be disabled.
9. Performance: FPS impact at each preset; low-FPS behavior (auto-throttle).
10. Logs: no spam, no errors on a clean launch.
11. **Compatibility pass (last):** FancyMenu with a custom layout; one popular menu/panorama replacement mod; note problems in `docs/DECISIONS.md` and decide whether to act.

---

## 12. Roadmap (order only, no dates)

| Step | Content |
|---|---|
| **M0** Build skeleton | Template adapted; empty mod loads on all 8 nodes; compat matrix doc |
| **M1** Core + first adapter | Particle engine, interaction physics, input collection, `GuiDraw`, event wiring on the active version; leaves land on title-screen buttons and get swept |
| **M2** Seasons & styles | Region/holiday data, resolver, style JSON loader, resource reload, styles for all four seasons |
| **M3** Config | YACL config class, handler, generated screen, `advanced` block, screen filter, ModMenu + NeoForge entries, presets |
| **M4** Port | Same adapter on the remaining versions; `mc/compat` filled in; `WindowInput` GLFW/SDL3 implementations |
| **M5** Audio | AudioDirector, bridge, ambient + one-shots, volume model |
| **M6** Easter eggs & extra effects | Eggs, frost, drop sprites, insects, petals, accumulation, performance pass |
| **M7** Polish & release | Full manual checklist incl. compatibility pass, wiki (`docs/wiki`), store pages, release |

The mod is released **complete**, when M7 is done.

Future ideas (unscheduled): real BACK layer; manual obstacle rects for non-widget elements; optional deeper integration with menu-customization mods; more regions/holidays/styles; style-pack documentation for pack authors.

---

## 13. Definition of done (any feature)

- Works on the active version with both loaders; compiles on all nodes (`chiseledBuild`).
- Has a config switch (or is covered by an existing one) and sensible defaults.
- No new third-party dependency, no new conditional or GLFW/SDL access outside `mc/compat`, no new allocation in the hot path.
- Relevant manual-checklist items pass.
- `docs/` and this file updated if a decision or format changed.

---

## 14. Open questions / [VERIFY] checklist

- Exact loader/Fabric API/YACL/ModMenu versions available for each of the 8 nodes (fill `docs/COMPAT_MATRIX.md`).
- Whether NeoForge stable builds exist for all targeted 26.x versions.
- Best screen post-render hook per loader/version and its exact semantics (draw order vs. other mods).
- Which `Screen`/widget accessors expose visibility and alpha across versions.
- Minecraft sound category/behavior for menu sounds; ability to loop ambient sounds on screens.
- GUI draw API for rotated/tinted textured quads in 1.21.1 vs 26.x; batching options.
- YACL: exact config-class annotations on each target; whether the map-like `advanced` field serializes cleanly (fallback: separate hand-edited JSON).
- ModMenu optionality declaration in `fabric.mod.json`.
- Whether any transient screens without a world (connecting, level loading/progress, disconnected) should be blacklisted by default; decide after trying them.
- Final mod name, modid, repository host.
