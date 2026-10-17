# Texture weights and alternative animation

Existing profile JSON is unchanged: missing `weight` means 1, and without
`alternativeAnimationId` the texture loops exactly as before.

```json
"textures": [
  {
    "id": "everfall:textures/particle/autumn/leaf_01.png",
    "weight": 1,
    "scale": 1,
    "frameSize": 16,
    "frameCount": 8,
    "frameTime": 4,
    "alternativeAnimationId": "everfall:textures/particle/autumn/leaf_01_alternative.png",
    "conditionsForAlternativeAnimation": [
      { "type": "chance", "value": 0.25 },
      { "type": "cooldown", "value": 5 }
    ],
    "conditionType": "both"
  }
]
```

## Weight

`weight` is a finite nonnegative relative weight, chosen once when a particle
spawns. Weights 9 and 1 mean 90% and 10%; weights 999 and 1 mean 99.9% and 0.1%.
Fractional weights are supported, including very small weights for rare sprites.
Weight 0 excludes that texture. At least one texture must have positive weight.
The selection uses the engine's seeded RNG, with 53-bit precision.

## Scale

`scale` is a positive finite size multiplier, default 1. For an 8x8 frame,
`scale: 2` draws a 16x16 particle; `scale: 0.5` draws it at 4x4 GUI pixels.
It applies equally to main and alternative animation, and to particle landing bounds
and cursor-interaction centers. `frameSize` remains the source frame size: changing
`scale` never changes UVs, frame count, frame timing, or movement speed. GUI scale
is applied afterward as before. Zero, negative, and nonfinite scales are rejected;
use weight 0 to exclude a texture instead.

## Alternative animation

The alternative uses a different texture sheet with the **same** `frameSize`,
`frameCount`, and `frameTime`. Both sheets contain vertically stacked frames.
`frameTime` retains its existing unit: twentieths of a second (4 means 0.2 s/frame).

Conditions are evaluated only when the main animation reaches the end of a cycle.
If they pass, the alternative starts at frame 0, plays one complete cycle, and
returns to main frame 0. The particle keeps its identity, size, physics, and sound
state. Time remaining in a rendered frame carries into the next animation.

- `chance`: probability from 0 to 1, rolled independently for each eligible main
  cycle and particle. 0 never passes; 1 always passes.
- `cooldown`: nonnegative seconds **per particle**, starting at its creation, then
  restarting when its alternative animation finishes. Failed checks do not restart
  it. This is not a shared cooldown for all particles using the texture.
- `conditionType`: `both` (default) requires all listed conditions; `any` requires
  at least one. An empty or omitted condition list allows the alternative after
  every main cycle. Unsupported conditions, duplicates, invalid values, and
  unsupported combination modes are rejected during profile validation.

With the example above, the first alternative is possible at the first main cycle
boundary at or after 5 seconds of particle existence, with a 25% chance. After the
alternative finishes, another 5 seconds must pass before it can qualify again.
The main animation keeps looping while conditions fail; requests are not queued.

Both animations pause on resting and mouse-held particles and resume from the
same frame and fractional frame time. Cooldown seconds continue accumulating
while a particle rests or is held, but animation conditions are not checked until
an airborne main animation completes a cycle. All timing uses the engine's frame
time, including its existing stall clamp. A work cap limits pathological animation
definitions to 64 cycle transitions per rendered frame; excess time skips main
cycles without further condition rolls.

The implementation does not rewrite profile JSON or create texture sheets. Supply
the alternative sheet and fields in a built-in profile or normal profile override.

## Wind mirroring

`flipWithWind` is a **profile-level** boolean, default `false`:

```json
{
  "id": "autumn",
  "flipWithWind": true,
  "textures": []
}
```

The snippet shows the placement only; retain the profile's actual textures and
other fields. When enabled, negative configured wind mirrors every particle
horizontally; positive or zero wind uses the original orientation. Existing
particles update immediately when the wind direction changes. Both main and
alternative sheets are mirrored by reversing their horizontal UVs, preserving
position, scale, animation progress, and collision bounds. When false, wind never
changes the texture's orientation. Procedural additional effects are unaffected.
