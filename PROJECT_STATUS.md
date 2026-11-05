# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-05
Summary: Generators-only power mod (FE out, no cables): all eight Forge and NeoForge layers compile, pass unit tests, pass a headless generator self test and were seen working in a real client; GitHub repo, CurseForge project, wiki deploy and the first release are waiting on Casey.
Spend: none

## Foundation
- [x] Layout: shared common/ plus per-version version-<mc>/ and eight loader layers (w:3)
- [x] Gradle builds for NeoForge and Forge on 1.21.1, 26.1.2, 26.2 and 26.3 (w:4)
- [x] Combined jar per Minecraft version (scripts/build_all.sh makes four jars in dist/; not yet loaded as a standalone jar in a launcher) (w:2)
- [ ] GitHub repository (waiting for Casey's public or private choice) (w:1)

## Generators
- [x] Hand crank (self test on all eight layers; seen in a client on all eight) (w:2)
- [x] Combustion generator, stone, iron and steel tiers, coal, charcoal and wood fuel (self test and client, all eight layers) (w:4)
- [x] Lava generator, three tiers, lava bucket fed (self test and client, all eight layers) (w:3)
- [x] Solar tier 1 and 2 with day curve, sky access, weather, night trickle on tier 2 (self test and client, all eight layers) (w:3)
- [x] Wind turbine tier 1 and 2: height, open sky, blocked blades, other turbines, storms (self test and client, all eight layers) (w:4)
- [x] Water wheel from flowing water (self test and client, all eight layers) (w:3)
- [x] Geothermal generator on or near lava and magma (self test and client, all eight layers) (w:3)
- [x] FE output into adjacent acceptors through the loader energy API (checked against a dev energy sink on all eight layers) (w:5)
- [ ] Fluid-fed lava generator through pipes (bucket feeding only for now) (w:2)
- [ ] Hopper and pipe fuel input (w:2)

## Config and balance
- [x] Config file with every rate, tier multiplier and the weather, height and flow rules (w:3)
- [x] Recipes for all generators and the steel ingot (data loads on all eight layers; documented in the wiki) (w:2)
- [ ] Recipes crafted and balance played in a real game (w:2)

## Art and animation
- [x] Original pixel textures drawn by script, animated front faces for fire, lava, blades, wheel, geothermal and crank (w:4)
- [x] Animated art seen in a client on all eight layers (w:3)

## Compatibility
- [~] AeroFlux energy pipes fed by a generator (seen working on NeoForge 1.21.1 only: 17,088 FE through three pipes; other layers and other cable mods not tried) (w:3)
- [x] SMWTI plugin showing output, fuel and the reason for the output (seen on all eight layers) (w:2)

## Testing and release
- [x] Unit tests for the output math (solar curve, wind height, water flow, fuel burn, config), 12 pass on every layer (w:3)
- [~] Unit tests run in CI on every layer (build.yml written, no remote yet so never run) (w:1)
- [x] Headless server load check with the generator self test, ports 8960 to 8967, passes on all eight layers (w:3)
- [x] Client check with screenshots on every layer (scripts/client_tour.py, frames in docs/frames) (w:4)
- [~] build.yml and release.yml with dry_run input, upload only builds whose mod_version equals the tag (written; the upload script dry run resolves all CurseForge ids locally; workflows never run) (w:2)
- [ ] CurseForge project created and id recorded (w:1)
- [ ] First release tagged and uploaded (w:2)

## Documentation and branding
- [x] Icon, banners and section headers drawn in the SMWTI style (w:2)
- [x] README, CHANGELOG, CurseForge description and listing kit (w:2)
- [x] Issue templates (the crash label still has to be created on GitHub) (w:1)
- [x] Wiki pages written for itmecasey.com (15 pages, local files only) (w:2)
- [ ] Wiki deployed and checked live (w:1)
