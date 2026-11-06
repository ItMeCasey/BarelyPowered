# BarelyPowered

![BarelyPowered](branding/banners/banner.png)

Power for Forge and NeoForge in Forge Energy (FE): seven generators, batteries in three tiers, and energy cables that ship as a separate module (BarelyCables) inside the jar. There are no machines. Each generator pushes power into any block next to it that accepts FE, and any cable mod can pull it out.

| Generator | Power from |
|---|---|
| Hand crank | Right-clicks, free and tiny |
| Combustion generator (stone, iron, steel) | Coal, charcoal, logs, planks; by hand or from a hopper or pipe |
| Lava generator (stone, iron, steel) | Lava, by bucket or by pipe |
| Solar panel (tier 1, tier 2) | Daylight, sky access, weather; tier 2 is a taller, wider three-panel array and keeps a trickle at night and in rain |
| Wind turbine (tier 1, tier 2) | A tall tower (3 blocks, or 5 for tier 2) with a turning 3D rotor; build height, open sky, storms; nearby blocks and other turbines reduce it |
| Water wheel | Flowing water beside or above it |
| Geothermal generator | Lava and magma next to it, steady, no fuel |
| Battery (basic, iron, steel) | Stores 100,000 to 1,600,000 FE; each face can be input, output, both or off; keeps its charge when broken |
| Energy cable (basic, iron, steel), from BarelyCables | Joins generators, batteries and machines; 400 to 6,400 FE per tick; each side can be switched off |

Right-click a generator with an empty hand to see its output, stored energy, fuel and why the output is what it is. Right-click a battery face to cycle it through input and output, input only, output only and off. Right-click a cable arm to switch that side off. Optional plugins for Show Me What This Is and Jade (NeoForge) show the same information when you look at a block, and the item viewers BEI, JEI, REI and EMI list the items and recipes. See [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) for what was checked on each version and loader. Every rate, tier multiplier and weather, height and flow rule is in `config/barelypowered.properties`.

## Versions

One jar per Minecraft version, each holding both the Forge and the NeoForge layer.

| Minecraft | Loaders | Java |
|---|---|---|
| 1.21.1 | NeoForge, Forge | 21 |
| 26.1.2 | NeoForge, Forge | 25 |
| 26.2 | NeoForge, Forge | 25 |
| 26.3 | NeoForge, Forge | 25 |

Fabric is out of scope. Install: put the jar for your Minecraft version in `mods`. Needs nothing else; install it on the server and the clients.

## BarelyCables: a separable module

The energy cables are not part of BarelyPowered's code. They live in [`barelycables/`](barelycables/): their own mod id (`barelycables`), Java package (`com.itmecasey.barelycables`), config file (`barelycables.properties`), language keys, textures, Gradle layers, tests and version number. The module imports nothing from BarelyPowered or AeroFlux, and BarelyPowered's generators and batteries talk to it only through Forge Energy, like any other mod would.

Each BarelyPowered jar carries the combined BarelyCables jar for the same Minecraft version inside `META-INF/jarjar` (the loader's jar-in-jar). When two mods carry the same BarelyCables, the loader keeps one copy, so there is only one set of cable blocks, recipes and config. The plan is to publish BarelyCables as its own mod and make it an optional dependency of BarelyPowered; the folder is laid out so that is a folder move to its own repository.

## Documentation

The wiki is at https://itmecasey.com/wiki/barelypowered/ (generators, recipes, config, known gaps). The pages are kept in the website repository.

## What was verified

Honest status, per layer, is on the wiki's *Versions and known gaps* page and in `PROJECT_STATUS.md`. In short: every layer compiles and passes a headless server self test that places each generator beside an energy sink and checks the energy delivered; the scripted client tour (`scripts/client_tour.py`) shows each generator running. The item viewers and Jade were checked in a real client where they have a build (table in `docs/COMPATIBILITY.md`). The wind turbine tower with its turning rotor and the taller tier 2 solar panel were seen in a real client on every layer (frames in `docs/frames/models`). Hopper feeding was seen in a client on NeoForge 1.21.1, Forge 1.21.1 and NeoForge 26.3, and the item and fluid inputs are in the server self test on every layer (frames in `docs/frames/automation`). Balance has not been play-tested over a full game, and only AeroFlux energy pipes were tried among cable mods.

## Building

Each layer is its own Gradle build (`forge-<mc>`, `neoforge-<mc>`), compiling the shared `common/` (output math, config) and `version-<mc>/` (blocks, block entities) sources.

```
cd neoforge-1.21.1
./gradlew build
```

`scripts/build_all.sh` builds and tests every layer of BarelyCables and BarelyPowered, combines the two loader jars of each Minecraft version, and nests BarelyCables inside the BarelyPowered jar in `dist/`. Development tools (the generator self test, the test energy sink and the screenshot tour) live in `src/dev` folders that only build with `-PdevTools`, so the release jars never contain them. Resource files (textures, models, recipes, loot tables, language file) are generated by `python scripts/make_assets.py` (needs Pillow).

Checks that need no display: `python scripts/server_check.py all` (server load and generator and battery self test, ports 8960 to 8967) and `python barelycables/scripts/server_check.py all` (cable self test, ports 8970 to 8977). With a display: `python scripts/client_tour.py neoforge-1.21.1` runs the screenshot tour (put the BarelyCables jar in `<layer>/run/mods` for the cable frames).

## License

MIT. See [LICENSE](LICENSE).
