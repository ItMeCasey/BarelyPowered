# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-05
Summary: Seven generators, three tiers of batteries and a separable BarelyCables cable module (nested in the jar) on all eight Forge and NeoForge layers: each compiles, passes unit tests, a headless self test and a client tour; GitHub is pushed by the Project Manager, and the CurseForge project, wiki deploy and first release are waiting on Casey.
Spend: none

## Foundation
- [x] Layout: shared common/ plus per-version version-<mc>/ and eight loader layers (w:3)
- [x] Gradle builds for NeoForge and Forge on 1.21.1, 26.1.2, 26.2 and 26.3 (w:4)
- [x] Combined jar per Minecraft version with BarelyCables nested (scripts/build_all.sh; four jars in dist/) (w:2)
- [x] Development tools (self test, test energy sink, screenshot tour) only build with -PdevTools; release jars checked to hold none (w:2)
- [~] GitHub repository (public repo exists and the Project Manager pushed the first 11 commits; later commits are local until pushed) (w:1)

## Generators
- [x] Hand crank (self test and client tour on all eight layers) (w:2)
- [x] Combustion generator, stone, iron and steel tiers, coal, charcoal and wood fuel (w:4)
- [x] Lava generator, three tiers, lava bucket fed (w:3)
- [x] Solar tier 1 and 2 with day curve, sky access, weather, night trickle on tier 2 (w:3)
- [x] Wind turbine tier 1 and 2: height, open sky, blocked blades, other turbines, storms (w:4)
- [x] Water wheel from flowing water (w:3)
- [x] Geothermal generator on or near lava and magma (w:3)
- [x] FE output into adjacent acceptors through the loader energy API (checked against a dev energy sink on all eight layers) (w:5)
- [ ] Fluid-fed lava generator through pipes (bucket feeding only for now) (w:2)
- [ ] Hopper and pipe fuel input (w:2)

## Batteries
- [x] Battery blocks in three tiers with capacity and rate from config (w:4)
- [x] Per-face input, output, both or off by empty-hand right-click (w:3)
- [x] Charge kept in the item when broken, shown in the item tooltip and a bar, placed back with the charge (self test on all eight layers) (w:3)
- [x] Charge gauge on the model (eight levels) and redstone comparator output (w:2)
- [x] Battery lines in the SMWTI overlay (w:1)
- [x] Generator to battery to consumer seen in a client on all eight layers (w:2)

## BarelyCables (separable module in barelycables/)
- [x] Own namespace, package, config, language keys, textures, Gradle layers and tests; imports nothing from BarelyPowered or AeroFlux (w:3)
- [x] Energy cables in three tiers with arms toward neighbors, per-side switches by right-click on an arm (w:4)
- [x] Cheap shared network: one leader per network, quiet networks check less often, energy conserved including neighbors that misreport (11 unit tests, 9-check self test on all eight layers) (w:5)
- [x] Cable lines in the SMWTI overlay (network rate, buffer, size, switched-off sides) (w:1)
- [x] Nested in the BarelyPowered jar with jar-in-jar; the loader keeps one copy when two mods embed it (two dummy mods per layer, and the real combined jar next to the cables layer; both on all eight layers) (w:4)
- [ ] The nested copy used on its own, in a launcher with only the BarelyPowered jar (not tested; the dev runs always load the cables layer or dummy mods) (w:1)
- [ ] BarelyCables published as its own mod with an optional dependency from BarelyPowered (planned; the folder move is the only code change) (w:3)
- [x] Generator to cable to battery to consumer seen in a client on all eight layers (w:2)
- [~] AeroFlux energy pipes with batteries and with BarelyCables (seen working on NeoForge 1.21.1 only; other layers and other cable mods not tried) (w:3)

## Config and balance
- [x] Config files: barelypowered.properties for every generator and battery rule, barelycables.properties for cables (w:3)
- [x] Recipes for all generators, batteries, cables and the steel ingot (data loads on all eight layers; documented in the wiki) (w:2)
- [ ] Recipes crafted and balance played in a real game (w:2)

## Art and animation
- [x] Original pixel textures drawn by script, animated front faces for fire, lava, blades, wheel, geothermal and crank, gauge for batteries, cable models (w:4)
- [x] Animated art seen in a client on all eight layers (w:3)

## Compatibility
- [x] SMWTI plugins for generators, batteries and cables (seen on all eight layers) (w:2)

## Testing and release
- [x] Unit tests: 20 for BarelyPowered (output math, config, battery core, face modes) and 11 for BarelyCables (network conservation, tier throughput, face switches) on every layer (w:3)
- [~] Unit tests run in CI on every layer (build.yml written for both projects, never run on GitHub yet) (w:1)
- [x] Headless server load check with the self tests (17 checks per BarelyPowered layer, 9 per BarelyCables layer, ports 8960 to 8967 and 8970 to 8977) (w:3)
- [x] Client check with screenshots on every layer (scripts/client_tour.py, frames in docs/frames) (w:4)
- [~] build.yml and release.yml with dry_run input, upload only builds whose mod_version equals the tag (written for the two-project build; the upload script dry run resolves all CurseForge ids locally; workflows never run) (w:2)
- [ ] CurseForge project created and id recorded (w:1)
- [ ] First release tagged and uploaded (w:2)

## Documentation and branding
- [x] Icon, banners and section headers drawn in the SMWTI style (w:2)
- [x] README, CHANGELOG, CurseForge description and listing kit (w:2)
- [x] Issue templates (w:1)
- [x] Wiki pages written for itmecasey.com (17 pages, local files only) (w:2)
- [ ] Wiki deployed and checked live (w:1)
