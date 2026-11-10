# BarelyCables

Energy cables for Forge Energy (FE), for Forge and NeoForge on Minecraft 1.21.1, 26.1.2, 26.2 and 26.3.

- Three tiers: basic (400 FE per tick), iron (1,600) and steel (6,400), set in `config/barelycables.properties`.
- Cables join each other and any block that offers Forge Energy on that side: generators, batteries, machines, other mods' pipes.
- Right-click an arm with an empty hand to switch that side off or on.
- All cables that touch form one network. One cable (the lowest position) moves the energy for the whole network, a quiet network checks less often, and energy is never created or lost (see `common/src/test`).
- A cable offers its network buffer as ordinary Forge Energy, so other mods' pipes can chain onto it.
- Optional Show Me What This Is plugin.

## A self-contained module

BarelyCables has its own mod id (`barelycables`), Java package (`com.itmecasey.barelycables`), config, language keys, textures, tests and version. It imports nothing from BarelyPowered or AeroFlux. It is built and combined on its own (`scripts/build_all.sh`) and carried inside other mods with the loader's jar-in-jar (`META-INF/jarjar`, group `com.itmecasey`, artifact `barelycables`), so if two mods embed it only one copy loads. Layout: layers `neoforge-<mc>` and `forge-<mc>`, shared `common/` and `version-<mc>/`, resource generator `scripts/make_assets.py`.

Development tools (a test energy source and sink, and a headless self test) are in `src/dev` folders that only build with `-PdevTools`; release jars contain none.

MIT licensed.
