# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-09
Summary: Seven generators, three tiers of batteries and a self-contained BarelyCables cable module on all eight Forge and NeoForge layers, released through 2026.11.3 (tall wind towers, bigger tier 2 solar, hopper and pipe feeding, balance fixes); open items are things that need a person, another mod's build, or a launcher install.
Spend: none

## Foundation
- [x] Layout: shared common/ plus per-version version-<mc>/ and eight loader layers (w:3)
- [x] Gradle builds for NeoForge and Forge on 1.21.1, 26.1.2, 26.2 and 26.3 (w:4)
- [x] Combined jar per Minecraft version with BarelyCables nested (scripts/build_all.sh; four jars in dist/) (w:2)
- [x] Development tools (self test, test energy sink, screenshot tour) only build with -PdevTools; release jars checked to hold none (w:2)
- [x] GitHub repository ItMeCasey/BarelyPowered: public, main matches origin (9b241dd when checked 2026-11-09), tags v2026.11.1 to v2026.11.3 and a pre-release with four jars for each (w:1)

## Generators
- [x] Hand crank (self test and client tour on all eight layers) (w:2)
- [x] Combustion generator, stone, iron and steel tiers, coal, charcoal, log and plank fuel, hopper and pipe fed (w:4)
- [x] Lava generator, three tiers, fed by bucket or by pipe (w:3)
- [x] Solar tier 1 and 2 with day curve, sky access, weather, night trickle on tier 2 (w:3)
- [x] Wind turbine tier 1 and 2: height, open sky, blocked blades, other turbines, storms (w:4)
- [x] Wind turbine as a tall multiblock tower with a turning 3D rotor (3 and 5 blocks), solar tier 2 bigger; seen on a client on all eight layers; Casey confirmed the blades turn smoothly (w:4)
- [x] Water wheel from flowing water, 20 FE per unit of flow, 40 FE/t at most (client frame "Output 40 FE/t", neoforge-1.21.1) (w:3)
- [x] Geothermal generator on or near lava and magma (w:3)
- [x] FE output into adjacent acceptors through the loader energy API (checked against a dev energy sink on all eight layers) (w:5)
- [x] Hopper fuel input into the combustion generator through the loader item capability on all eight layers; a real hopper seen feeding it on NeoForge 1.21.1, Forge 1.21.1 and NeoForge 26.3 (w:2)
- [x] Fluid-fed lava generator: the fluid capability is in the server self test on all eight layers, and AeroFlux's fluid pipe moved lava from an AeroFlux tank into it in a real client on NeoForge 1.21.1 ("Lava: 3000 / 4000 mB", docs/frames/automation) (w:2)
- [x] Recipes and balance reviewed on paper: planks no longer beat their log, water wheel lowered to 20 FE per flow, numbers on the wiki Balance page (w:2)
- [x] A third-party item pipe feeding the combustion generator: AeroFlux's item pipe moved coal from a chest into it ("Coal x31", docs/frames/automation, NeoForge 1.21.1) (w:1)
- [x] Third-party pipes feeding the generators on every build that has a pipe mod, seen in real clients (tour scene pipeFeedScene, frames in docs/frames/automation): AeroFlux (v2026.11.10) item and fluid pipes with an AeroFlux tank on all eight builds: NeoForge and Forge on 1.21.1, 26.1.2, 26.2 and 26.3 ("Coal x31" in the combustion generator, "Lava: 3000 / 4000 mB" in the lava generator); Barely Pipes' item pipe also on NeoForge and Forge 26.2 and 26.3 ("Coal x31"; it ships no tank) (w:1)
- [x] BarelyStorage feeding the combustion generator: a rack with a gold drive, rack bus cable and export node (filter coal) with 32 coal in the network put "Fuel: Coal x31" into the generator on NeoForge 26.1.2 and Forge 26.2 in real clients (tour scene storageFeedScene by reflection, frames docs/frames/automation/neoforge-26.1.2_barelystorage_export_node_feeds_combustion.png and forge-26.2_...; BarelyStorage has builds for 26.1.2, 26.2 and 26.3 only, so 1.21.1 is n/a) (w:1)

## Batteries
- [x] Battery blocks in three tiers with capacity and rate from config (w:4)
- [x] Per-face input, output, both or off by empty-hand right-click (w:3)
- [x] Charge kept in the item when broken, shown in the item tooltip and a bar, placed back with the charge (self test on all eight layers) (w:3)
- [x] Charge gauge on the model (eight levels) and redstone comparator output (w:2)
- [x] Battery lines in the SMWTI overlay (w:1)
- [x] Generator to battery to consumer seen in a client on all eight layers (w:2)

## BarelyCables (module in barelycables/)

- [x] Own namespace, package, config, language keys, textures, Gradle layers and tests; imports nothing from BarelyPowered or AeroFlux (w:3)
- [x] Energy cables in three tiers with arms toward neighbors, per-side switches by right-click on an arm (w:4)
- [x] Cheap shared network: one leader per network, quiet networks check less often, energy conserved including neighbors that misreport (11 unit tests, 9-check self test on all eight layers) (w:5)
- [x] Cable lines in the SMWTI overlay (network rate, buffer, size, switched-off sides) (w:1)
- [x] Nested in the BarelyPowered jar with jar-in-jar; the loader keeps one copy when two mods embed it (two dummy mods per layer, and the real combined jar next to the cables layer; both on all eight layers) (w:4)
- [x] The nested copy used on its own: a clean NeoForge 21.1.265 install for Minecraft 1.21.1 with only barelypowered-mc1.21.1-2026.11.3.jar in mods; the log shows the jarinjar locator finding barelycables-mc1.21.1-2026.11.2.jar inside the BarelyPowered jar, BarelyCables 2026.11.2 initialized, a cable block placed by a datapack (CABLE-PLACED) and the player joined (docs/NESTED_CABLES_CHECK.md); Forge and 26.x installs not tried (w:1)
- [x] Generator to cable to battery to consumer seen in a client on all eight layers (w:2)
- [x] AeroFlux energy pipes with an iron battery and with BarelyCables seen in real clients on all eight builds: sink received about 15,400 to 17,000 FE in each scene (Forge 1.21.1 16992 and 16320 FE, NeoForge 26.1.2 15840 and 15456, Forge 26.1.2 15744 and 15360, NeoForge and Forge 26.2 and NeoForge 26.3 15840 and 15456, Forge 26.3 15840 and 15360; frames docs/frames/automation); other cable mods not tried (w:3)

## Config and balance
- [x] Config files: barelypowered.properties for every generator and battery rule, barelycables.properties for cables (w:3)
- [x] Recipes for all generators, batteries, cables and the steel ingot (data loads on all eight layers; documented in the wiki) (w:2)
- [x] Recipes load and craft in the running game: 17 registered, five real crafting layouts and the steel blasting recipe resolve to the right results on all eight layers (server self test, 32 checks) (w:2)
- [ ] Balance played over a whole game by a person (needs a human play session; the numbers were reviewed on paper and the wiki Balance page lists them) (w:2)

## Art and animation
- [x] Original pixel textures drawn by script, animated front faces for fire, lava, blades, wheel, geothermal and crank, gauge for batteries, cable models (w:4)
- [x] Animated art seen in a client on all eight layers (w:3)

## Compatibility
- [x] SMWTI plugins for generators, batteries and cables (seen on all eight layers) (w:2)
- [x] Jade plugins for generators, batteries and cables on NeoForge (frames of all four scenes on 1.21.1, 26.1.2, 26.2 and 26.3; Jade has no Forge build) (w:2)
- [x] Item viewers show our items and the Hand Crank recipe: BEI on all eight layers, JEI on NeoForge x4 and Forge 1.21.1, REI on NeoForge x4, EMI on NeoForge 1.21.1 (frames in docs/frames/viewers; table in docs/COMPATIBILITY.md) (w:4)
- [x] n/a: no JEI build for Forge 26.1.2, 26.2 or 26.3; no REI build for any Forge version; no EMI build for NeoForge 26.1.2, 26.2, 26.3 or any Forge version; no Jade build for any Forge version (CurseForge re-checked 2026-11-09). Every combination that exists was tested in a real client: BEI on all eight layers, JEI on NeoForge 1.21.1, 26.1.2, 26.2, 26.3 and Forge 1.21.1, REI on NeoForge x4, EMI on NeoForge 1.21.1, Jade on NeoForge x4 (w:1)
- [x] Viewer click-through: in JEI on NeoForge 1.21.1 a right click on the steel ingot shows its uses (steel cable, advanced solar panel, advanced wind turbine) and a left click shows the blasting recipe with the blast furnace as catalyst, 20 s (docs/frames/viewers/neoforge-1.21.1/jei_uses_steel.png and jei_recipes_steel.png) (w:1)
- [x] A BarelyPowered fuel category in the viewers: not planned, the vanilla fuel category already lists the same items (coal, charcoal, logs, planks) (w:1)

## Testing and release
- [x] Unit tests: 26 for BarelyPowered (output math, config, battery core, face modes, automation room rules, planks and tiers) and 11 for BarelyCables (network conservation, tier throughput, face switches) on every layer (w:3)
- [x] Unit tests run in CI (build.yml green on main at 9b241dd, run 42174560866: version check, tests, 16 layer builds, combine and nest, no dev files in release jars) (w:1)
- [x] Headless server load check with the self tests (32 checks per BarelyPowered layer, 9 per BarelyCables layer, ports 8960 to 8967 and 8970 to 8977; passed on all eight layers on 2026-11-09) (w:3)
- [x] Client check with screenshots on every layer (scripts/client_tour.py, frames in docs/frames) (w:4)
- [x] build.yml and release.yml with dry_run input, upload only builds whose mod_version equals the tag (release.yml green on tags v2026.11.1, v2026.11.2 and v2026.11.3 and on three manual dry runs, checked with gh run list) (w:2)
- [x] CurseForge project created and id recorded (commit 6b7ccd1) (w:1)
- [x] First release 2026.11.1 tagged and uploaded (GitHub pre-release with four jars; CurseForge file ids 9455478 to 9455481 in the run log) (w:2)
- [x] mod_version bumped to 2026.11.2 in all 16 builds, own commit f99a4d9 (w:1)
- [x] Release 2026.11.2 tagged and uploaded (GitHub pre-release with four jars; CurseForge file ids 9466790 to 9466793 in the run log) (w:2)
- [x] BarelyPowered mod_version bumped to 2026.11.3 in its eight builds (BarelyCables stays at 2026.11.2), own commit b4bffac (w:1)
- [x] Release 2026.11.3 tagged and uploaded, CI green on the tag (run 42175560957; GitHub pre-release with four jars; CurseForge file ids 9469707 to 9469710 in the run log; CurseForge review status not checked) (w:2)

## Documentation and branding
- [x] Icon, banners and section headers drawn in the SMWTI style (w:2)
- [x] README, CHANGELOG, CurseForge description and listing kit (w:2)
- [x] Issue templates (w:1)
- [x] Wiki pages written for itmecasey.com (18 pages including Balance numbers, images wind.png and solar.png for 2026.11.2) (w:2)
- [x] Wiki deployed and checked live: on 2026-11-09 all 18 pages, mod.json and the images were byte-identical to the local draft before the audit fixes (w:1)
- [x] Wiki audit fixes of 2026-11-09 deployed: checked on 2026-11-09 that every page and mod.json on itmecasey.com is identical to the local draft except known-gaps (w:1)
- [x] Wiki known-gaps sentence about the BarelyStorage run deployed (the Project Manager deployed it, 2026-11-09) (w:1)
