# Background particles and interaction

General settings include two independent switches:

- `backgroundParticles` (default `false`): draw the main profile and all additional effects after the menu backdrop, before interface content.
- `interactionEnabled` (default `true`): allow landing on widgets, cursor sweeping, dragging, and their sounds.

Enabling background particles sets interaction to false and disables its YACL control, including while editing pending values. Leaving background mode unlocks the control; interaction can then be enabled manually. Loading or saving a background configuration also forces interaction off. The engine additionally checks both settings, so a manually edited contradictory configuration cannot enable background interaction.

Disabling interaction releases captured and grounded particles without clearing the particle field. It disables obstacle collisions and cursor impulses; wind, gravity, animation, and additional effects continue. Re-enabling interaction while a mouse button remains held does not capture another particle until a new press.

Rendering uses one simulation step and draw per frame. Minecraft 26.x draws the background layer after the screen background, using separate GUI strata; the title screen instead draws it after its panorama. Minecraft 1.21.1 draws after the menu background or full-screen dimming layer, before container panels; the title screen also uses a panorama hook. A guard prevents duplicate draws when multiple hooks run. Foreground rendering occurs at the end of the screen wrapper.

## Manual checks

1. On the title screen, enable background particles: the logo, splash, text, buttons, and tooltips cover particles; fireflies also move behind them.
2. Visit options, YACL, inventory, and a container (enable `showInWorld` as needed): full-screen backdrops stay behind particles, while interface panels and images cover them.
3. Enable background mode with interaction enabled: the interaction switch immediately turns off and becomes unavailable. Save, reopen, and verify the state persists.
4. Disable background mode: the interaction control unlocks and stays off until enabled. Canceling configuration edits preserves the saved settings.
5. Disable interaction while dragging or while particles rest on buttons: particles are released, pass through widgets, and no new interaction sounds play. With interaction disabled, stationary or moving clicks cannot capture or sweep particles.
6. Restore interaction: sweeping, capture, and landing work again. Check both pixel modes, window resize, and screen changes.
