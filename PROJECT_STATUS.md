# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-09
Summary: Seven generators, three tiers of batteries and a separable BarelyCables cable module on all eight Forge and NeoForge layers, released through 2026.11.3 (tall wind towers, bigger tier 2 solar, hopper and pipe feeding, balance fixes); open items are things that need a person, another mod's build, or a launcher install.
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
- [ ] Third-party pipes feeding the generators on Forge and on 26.x, and BarelyStorage nodes (jars exist in AeroFlux/dist and BarelyPipes/dist; the tour scene exists only for NeoForge 1.21.1; only the vanilla hopper and the self test cover the rest) (w:1)

## Batteries
- [x] Battery blocks in three tiers with capacity and rate from config (w:4)
- [x] Per-face input, output, both or off by empty-hand right-click (w:3)
- [x] Charge kept in the item when broken, shown in the item tooltip and a bar, placed back with the charge (self test on all eight layers) (w:3)
- [x] Charge gauge on the model (eight levels) and redstone comparator output (w:2)
- [x] Battery lines in the SMWTI overlay (w:1)
- [x] Generator to battery to consumer seen in a client on all eight layers (w:2)

## BarelyCables (separable module in barelycables/)

Decision (Casey, 2026-11-09): BarelyCables stays part of BarelyPowered and is NOT published as its own mod. Item and fluid transport is the job of Barely Pipes.
- [x] Own namespace, package, config, language keys, textures, Gradle layers and tests; imports nothing from BarelyPowered or AeroFlux (w:3)
- [x] Energy cables in three tiers with arms toward neighbors, per-side switches by right-click on an arm (w:4)
- [x] Cheap shared network: one leader per network, quiet networks check less often, energy conserved including neighbors that misreport (11 unit tests, 9-check self test on all eight layers) (w:5)
- [x] Cable lines in the SMWTI overlay (network rate, buffer, size, switched-off sides) (w:1)
- [x] Nested in the BarelyPowered jar with jar-in-jar; the loader keeps one copy when two mods embed it (two dummy mods per layer, and the real combined jar next to the cables layer; both on all eight layers) (w:4)
- [x] The nested copy used on its own: a clean NeoForge 21.1.265 install for Minecraft 1.21.1 with only barelypowered-mc1.21.1-2026.11.3.jar in mods; the log shows the jarinjar locator finding barelycables-mc1.21.1-2026.11.2.jar inside the BarelyPowered jar, BarelyCables 2026.11.2 initialized, a cable block placed by a datapack (CABLE-PLACED) and the player joined (docs/NESTED_CABLES_CHECK.md); Forge and 26.x installs not tried (w:1)
eoforge-1.21.1\game with only the released barelypowered-mc1.21.1-2026.11.3.jar in mods and a tipworld copy with a datapack that places a barelycables cable; the launch did not run on 2026-11-09: a launch from the PowerShell tool could not see the install's libraries folder, and a launch from the Bash tool never got a client slot in 18 minutes because other sessions held it; the real jar's META-INF/jarjar/metadata.json lists barelycables, and the dummy-mod and next-to-the-cables-layer tests pass on all eight layers) (w:1)
- [x] Generator to cable to battery to consumer seen in a client on all eight layers (w:2)
- [~] AeroFlux energy pipes with batteries and with BarelyCables (energy pipes seen on NeoForge 1.21.1 only; AeroFlux item and fluid pipes feeding the generators also seen there; other layers and other cable mods not tried) (w:3)

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
- [ ] A BarelyPowered fuel category in the viewers (the vanilla fuel category already lists coal, charcoal, logs and planks; a category with our burn times and energy needs one plugin per viewer) (w:1)

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
- [ ] Wiki audit fixes of 2026-11-09 deployed (home, FAQ, recipes, balance, getting started and known-gaps text corrected in the local draft; the Project Manager deploys) (w:1)
