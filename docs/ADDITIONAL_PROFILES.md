# Additional particle profiles

Additional profiles are independent layers. Any number can be selected together in
the **Features** settings category; the main seasonal profile stays active.
Checking a feature permits automatic activation when its conditions match. It does
not force the effect outside its conditions. Fireflies are selected by default.

The built-in example is `assets/everfall/profiles/additional/fireflies.json`.
Add `.json` files under that same path in a resource pack, or override/add them in
`config/everfall/profiles/additional/`. Reload resource packs (F3+T or the pack selection
screen) to reload definitions, including config overrides. Each filename must match its `id`.
Invalid files are logged and skipped; an invalid override retains the built-in.
New custom profiles appear as independent switches, initially off. Several can be
enabled together; no fixed slot count or exclusive feature selector is used.

```json
{
  "schema": 1,
  "id": "golden_fireflies",
  "name": "Golden fireflies",
  "motion": "fireflies",
  "maxParticles": 24,
  "speed": 10.0,
  "size": 1.0,
  "color": 16773000,
  "conditions": {
    "profiles": ["spring", "summer"],
    "startHour": 21,
    "endHour": 5
  }
}
```

- `motion`: currently `fireflies`. The engine factory is separate from conditions
  and selection so other motion models can be added without changing those systems.
- `maxParticles`: the layer's population weight, relative to the main profile's
  recommended count. All active layers share the configured **Particle count**
  limit; enabling features does not exceed it. Zero disables this population.
- `speed`: movement units per second in the selected display/GUI coordinate mode.
- `size`: core size in GUI pixels. The procedural pixel glow extends to 5x this
  size. No Minecraft textures, image files, audio, widget landing, or mouse capture
  are used for fireflies. The full glow stays in the bottom third of the screen.
- `color`: RGB as a decimal integer; fireflies pulse smoothly at independent phases.
- `conditions.profiles`: main profile IDs; an empty/omitted list matches every main profile, including custom IDs.
- `startHour`, `endHour`: local device time, start inclusive and end exclusive.
  Wrapping midnight is supported; equal values mean all day. Built-in fireflies
  match `[21:00, 05:00)` only in spring/summer, including manually chosen seasons.
- Optional `startDate`, `endDate`: paired `MM-DD` dates, both inclusive, for annual
  holiday windows. Year wrapping is supported (e.g. `12-24` through `01-07`).
  Omit both for the whole year. Dates, hours, and profile conditions combine.

Example holiday conditions:

```json
"conditions": {
  "profiles": [],
  "startHour": 0,
  "endHour": 0,
  "startDate": "12-24",
  "endDate": "01-07"
}
```

Selected IDs are stored in `additionalProfiles` in `everfall.json5`. Conditions use
the local device clock, never in-world Minecraft time. Time is sampled once per
second. Effects also obey the global enable switch, screen/world visibility,
coordinate mode, resizing/fullscreen resets, and zero particle count. Motion and
pulsing update on every rendered frame. Instances persist across screen changes.
