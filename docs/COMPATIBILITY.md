# Compatibility with item viewers and in-world info mods

Checked on 2026-11-05 against the compatibility standard in the studio playbook. Every integration is optional: the mod starts and works with none of them installed.

## What BarelyPowered adds to each viewer

- **Item and recipe viewers (BEI, REI, JEI, EMI):** nothing to register. BarelyPowered and BarelyCables add items, blocks and recipes of the vanilla types only (crafting table and one blasting recipe for the steel ingot), so every viewer lists them from the game's own recipe data. There are no custom recipe categories, so no viewer plugin code exists. The item tooltips are plain text. The wind turbine's tower pieces (mast and rotor head) have no item, so they are not listed in any viewer; the viewers show the one turbine item you place.
- **In-world info:** Show Me What This Is (our own, plugins in `compat/`) and Jade (`jade/` package in each NeoForge layer). Generators show output, fuel or lava and the reason for a low output; batteries show their rate and every face that is not set to both; cables show the network rate, size and switched-off sides. Jade draws the energy bar itself from the energy capability. Cable lines live in BarelyCables and import nothing from BarelyPowered.

## Which builds exist upstream (CurseForge lookup, 2026-11-05)

| Mod | NeoForge 1.21.1 | NeoForge 26.1.2 | NeoForge 26.2 | NeoForge 26.3 | Forge 1.21.1 | Forge 26.1.2 / 26.2 / 26.3 |
|---|---|---|---|---|---|---|
| BEI | yes | yes | yes | yes | yes | yes |
| JEI | yes | yes | yes | yes | yes | no build |
| REI | yes | yes | yes | yes | no build | no build |
| EMI | yes | no build | no build | no build | no build | no build |
| Jade | yes | yes | yes | yes | no build | no build |

REI needs Architectury API and Cloth Config. Cloth Config for 26.3 on NeoForge is only tagged `26.3-snapshot`; it worked in the check. JEI and REI files are betas, EMI 1.21.1 and Jade are releases.

## Result per version (what was seen in a real game client)

"Drew" means a client was started with the mod, the viewer's search was set to `@barelypowered` and showed our 18 items, and opening the Hand Crank showed its crafting recipe (for Jade: the look-at box for a combustion generator, a battery, a cable and a lava generator showed the lines above). Frames are in `docs/frames/viewers/<loader>-<mc>/`. A frame with a toast or a warning overlay does not count.

| Viewer | NeoForge 1.21.1 | NeoForge 26.1.2 | NeoForge 26.2 | NeoForge 26.3 | Forge 1.21.1 | Forge 26.1.2 | Forge 26.2 | Forge 26.3 |
|---|---|---|---|---|---|---|---|---|
| BEI | drew | drew | drew | drew | drew | drew | drew | drew |
| JEI | drew | drew | drew | drew | drew | n/a (no build) | n/a (no build) | n/a (no build) |
| REI | drew | drew | drew | drew | n/a (no build) | n/a (no build) | n/a (no build) | n/a (no build) |
| EMI | drew | n/a (no build) | n/a (no build) | n/a (no build) | n/a (no build) | n/a (no build) | n/a (no build) | n/a (no build) |
| Jade | drew | drew | drew | drew | n/a (no build) | n/a (no build) | n/a (no build) | n/a (no build) |
| SMWTI | drew (earlier tour) | drew (earlier tour) | drew (earlier tour) | drew (earlier tour) | drew (earlier tour) | drew (earlier tour) | drew (earlier tour) | drew (earlier tour) |

## Not covered

- Another mod's pipes feeding the generators: AeroFlux's item pipe (chest of coal to a combustion generator) and fluid pipe (tank of lava to a lava generator) were seen in a client on NeoForge and Forge 1.21.1 and 26.1.2; Barely Pipes' item pipe was seen feeding the combustion generator on NeoForge and Forge 26.2 and 26.3 (AeroFlux has no build for 26.2 and 26.3, and Barely Pipes has no tank, so fluids through a third-party pipe were not seen there). BarelyStorage (rack, bus cable, export node with a coal filter) fed the combustion generator on NeoForge 26.1.2 and Forge 26.2. Frames are in `docs/frames/automation`.
- Click-through lookups (uses, catalysts) and bookmarks were not exercised; only search, item list and one crafting recipe were.
- Fuel values (what the combustion generator burns) are not shown in any viewer; they are in the Jade and Show Me What This Is lines and on the wiki. Adding a fuel category would need a plugin for each viewer.
- Each check ran in a fresh superflat world with only BarelyPowered, BarelyCables and the viewer (plus the viewer's required mods) loaded.
- Jade, JEI, REI and EMI were never run together with each other.
- EMI printed a tag warning for `#c:ingots/steel` before the translation key `tag.item.c.ingots.steel` was added; the frames above are from after the fix.
- When a viewer later ships a build for a Minecraft version or loader marked "n/a (no build)", the integration needs a check on that version; nothing in our code is expected to change because no recipe types are custom.

## How the checks were made

`python scripts/viewer_check.py <layer> <mods> --script "..."` starts a dev client with `-Pviewers=<mods>` (dev-only runtime dependencies from `scripts/add_jade.py`) and the dev-only `ViewerTour`, which sends input through the game's own mouse and keyboard handlers (nothing touches the desktop) and saves screenshots. The tooling only builds with `-PdevTools` and never ships in release jars.
