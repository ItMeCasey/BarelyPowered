# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-05
Summary: Generators-only power mod (FE out, no cables): all eight Forge and NeoForge layers compile and load on a server with the generator self test passing; NeoForge 1.21.1 has also been seen working in a real client, the rest of the client tours, CI, listing and wiki are in progress.
Spend: none

## Foundation
- [x] Layout: shared common/ plus per-version version-<mc>/ and eight loader layers (w:3)
- [x] Gradle builds for NeoForge and Forge on 1.21.1, 26.1.2, 26.2 and 26.3 (w:4)
- [~] Combined jar per Minecraft version (scripts/combine_jars.py, scripts/build_all.sh written, not run yet) (w:2)
- [ ] GitHub repository (w:1)

## Generators
- [x] Hand crank (self test passes on all eight layers; seen in a NeoForge 1.21.1 client) (w:2)
- [x] Combustion generator, stone, iron and steel tiers, coal, charcoal and wood fuel (server self test on all eight layers; NeoForge 1.21.1 seen in a client) (w:4)
- [x] Lava generator, three tiers, lava bucket fed (server self test on all eight layers; NeoForge 1.21.1 seen in a client) (w:3)
- [x] Solar tier 1 and 2 with day curve, sky access, weather, night trickle on tier 2 (server self test; NeoForge 1.21.1 seen in a client) (w:3)
- [x] Wind turbine tier 1 and 2: height, open sky, blocked blades, other turbines, storms (server self test; NeoForge 1.21.1 seen in a client) (w:4)
- [x] Water wheel from flowing water (server self test; NeoForge 1.21.1 seen in a client) (w:3)
- [x] Geothermal generator on or near lava and magma (server self test; NeoForge 1.21.1 seen in a client) (w:3)
- [x] FE output into adjacent acceptors through the loader energy API (checked against a dev energy sink on all eight layers) (w:5)
- [ ] Fluid-fed lava generator through pipes (bucket feeding only for now) (w:2)
- [ ] Item and fluid automation (hopper fuel input) (w:2)

## Config and balance
- [x] Config file with every rate, tier multiplier and the weather, height and flow rules (w:3)
- [x] Recipes for all generators and the steel ingot (data loads on all eight layers) (w:2)
- [ ] Recipes crafted and balance played in a real game (w:2)

## Art and animation
- [x] Original pixel textures drawn by script, animated front faces for fire, lava, blades, wheel, geothermal and crank (w:4)
- [~] Animated art checked in a client on every layer (NeoForge 1.21.1 seen; the other seven not yet) (w:3)

## Compatibility
- [ ] AeroFlux energy pipe fed by a generator, seen in a client (w:3)
- [ ] SMWTI plugin showing output and fuel (w:2)

## Testing and release
- [x] Unit tests for the output math (solar curve, wind height, water flow, fuel burn, config) (w:3)
- [~] Unit tests run in CI on every layer (workflow not written yet) (w:1)
- [~] Headless server load check with the generator self test (passes on all eight layers) (w:3)
- [~] Client check with screenshots on every layer (NeoForge 1.21.1 done) (w:4)
- [ ] build.yml and release.yml with dry_run input (w:2)
- [ ] CurseForge project created and id recorded (w:1)
- [ ] First release tagged and uploaded (w:2)

## Documentation and branding
- [ ] Icon, banners and section headers drawn in the SMWTI style (w:2)
- [ ] README, CHANGELOG, CurseForge description and listing kit (w:2)
- [ ] Issue templates (w:1)
- [ ] Wiki pages written for itmecasey.com (w:2)
- [ ] Wiki deployed and checked live (w:1)
