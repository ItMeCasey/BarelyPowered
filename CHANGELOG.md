# Changelog

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
