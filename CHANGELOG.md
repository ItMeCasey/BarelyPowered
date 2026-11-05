# Changelog

## 2026.11.2

- New hand-built models for the solar panels and wind turbines, made in Blockbench (sources in `art/`).
- Wind turbines are now tall towers: placing one builds a mast and a head above it (3 blocks tall for tier 1, 5 for tier 2, so it needs 2 or 4 free blocks above). The three-blade rotor is drawn in 3D and turns smoothly while the turbine makes power, and slows down when it stops. Breaking any part of the tower breaks the turbine and drops it.
- The tier 2 solar panel is taller and wider: a three-panel array on a high mast.
- The wind sky check now reads from the top of the tower, and the blocked-blades check from the rotor height.
- Optional Jade plugins on NeoForge for generators, batteries and cables (the cable lines come from BarelyCables).
- Translation for the `c:ingots/steel` tag, so EMI lists it without a warning.
- Checked in BEI, JEI, REI and EMI wherever they have a build (BEI on every layer, JEI on NeoForge and Forge 1.21.1, REI on NeoForge, EMI on NeoForge 1.21.1); see `docs/COMPATIBILITY.md`. No code was needed: the mod only adds vanilla recipe types, so the viewers list everything on their own.
- Wiki pages for the wind turbine and solar panel updated, and the known gaps now list the wind tower and solar array limits.

## 2026.11.1

First version.

- Seven generators that output Forge Energy: hand crank, combustion generator (stone, iron, steel), lava generator (stone, iron, steel), solar panel (two tiers), wind turbine (two tiers), water wheel and geothermal generator.
- Generators push energy into adjacent acceptors and can be drawn from by cables on any side.
- Output rules: fuel and tier for combustion and lava; daylight curve, sky access and weather for solar; height, open sky, blocked blades, other turbines and storms for wind; flow for the water wheel; heat from lava and magma for geothermal.
- Right-click with an empty hand shows output, stored energy, fuel and the reason for the current output.
- Optional Show Me What This Is plugin: output, fuel and the reason for a low output.
- Batteries in three tiers (basic, iron, steel) with per-face input and output, a charge gauge on the block, a comparator output, and the charge kept in the item when the block is broken.
- BarelyCables, a separate cable module carried inside the jar: three tiers of energy cables with per-side switches and a cheap shared network that works with generators, batteries, machines and other mods' pipes.
- Config file `config/barelypowered.properties` with every rate, tier multiplier and rule.
- Recipes for every generator and a steel ingot made in a blast furnace.
- Animated front faces (flames, lava, blades, wheel, crank, geothermal glow), drawn for this mod.
- Forge and NeoForge on Minecraft 1.21.1, 26.1.2, 26.2 and 26.3, one jar per Minecraft version.
