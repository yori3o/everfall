# Resource-pack and config profiles

Main profiles are discovered in `assets/everfall/profiles/*.json` inside active
resource packs and `config/everfall/profiles/*.json`. Additional profiles are
independently discovered in the `additional/` subfolder at both locations.
Files are directly inside their respective folder. Their filename (without `.json`)
must match `id`, using lowercase letters, digits, underscores, dots, or hyphens.

Priority, lowest to highest:

1. Bundled profiles.
2. Active resource packs, in Minecraft's pack priority order.
3. Files in `config/everfall/profiles/` (and its `additional/` folder).

A profile replaces the whole definition with that ID; fields are not merged.
Invalid definitions are logged with their source and skipped, retaining the next
valid lower-priority definition when available. An unavailable seasonal profile
produces no main particles. A removed custom profile also produces no main particles
if its ID remains selected; selecting another valid profile restores the effect.

Profiles and config overrides reload when resource packs reload (F3+T, applying the
pack selection, or startup). Particle state resets to use the new definitions and
texture dimensions safely. Reopen the settings screen after a reload to refresh
its choices and additional-effect switches.

## Main profile example

Resource-pack structure (plus a normal `pack.mcmeta` for your Minecraft version):

```text
assets/
  everfall/
    profiles/
      cherry.json
      additional/
        golden_fireflies.json
    textures/particle/cherry/petal.png
    sounds/cherry/sweep_1.ogg
    sounds.json
```

`assets/everfall/profiles/cherry.json`:

```json
{
  "id": "cherry",
  "textures": [
    {
      "id": "everfall:textures/particle/cherry/petal.png",
      "frameCount": 8,
      "frameTime": 2,
      "weight": 1,
      "scale": 1
    }
  ],
  "sweepSounds": ["everfall:cherry.sweep"],
  "defaults": { "maxParticles": 200, "wind": -50, "gravity": 40 }
}
```

Textures can also use another namespace. Their frame dimensions are read from the
actual resource-pack images; see [texture animations](TEXTURE_ANIMATIONS.md).
Sound lists are optional, including `shortSweepSounds` and `captureSounds`.

New main profiles appear in the profile selector. Disable automatic seasonal
selection to choose one. Automatic selection still resolves to `spring`, `summer`,
`autumn`, or `winter`; replacing those IDs changes the automatic seasonal themes.
Choosing a custom profile stages its count/wind/gravity defaults just like a bundled
profile. Pending edits can still be saved or canceled normally.

Optional `defaults.backgroundParticles` and `defaults.interactionEnabled` also stage
the corresponding settings on manual profile selection. Omitted/null flags preserve
the current settings, so existing profiles keep their previous behavior. For example:

```json
"defaults": {
  "maxParticles": 200,
  "wind": -50,
  "gravity": 40,
  "backgroundParticles": true,
  "interactionEnabled": false
}
```

Background mode always forces interaction off and disables its control, even if a
profile requests both flags as true. To enable interaction in a profile, explicitly
set `backgroundParticles` to false and `interactionEnabled` to true. Enabling automatic selection stages the detected profile defaults, even when its ID
already matches. Automatic season changes also apply defaults once; repeated checks
of the same season preserve subsequent user adjustments.

The config `profile` is now a string ID. Existing uppercase seasonal values such as
`"AUTUMN"` are normalized to `"autumn"` on load.

## Sounds

The resource pack defines sounds normally. For the example above, `sounds.json`
contains:

```json
{
  "cherry.sweep": {
    "sounds": ["everfall:cherry/sweep_1"]
  }
}
```

Minecraft's sound reload discovers `sounds.json` and OGG files. Everfall builds
client sound events for every referenced sweep/short-sweep/capture ID at profile
reload. This supports arbitrary namespaces without trying to modify Minecraft's
frozen sound registry after startup. Existing limits, cooldowns, and volume rules
apply to custom sounds too. A config profile can reference assets provided by any
active resource pack; JSON files in the config directory do not themselves supply
textures or audio.

## Additional profiles

See [additional particle profiles](ADDITIONAL_PROFILES.md). Their existing format
is unchanged (`motion: "fireflies"` is the currently supported motion model).
`conditions.profiles` can reference custom main profile IDs such as `"cherry"`;
an empty list matches all main profiles. Multiple additional profiles can be enabled
at once, and they share the configured particle budget with the main profile.
