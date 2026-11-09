![BarelyPowered](https://raw.githubusercontent.com/ItMeCasey/BarelyPowered/main/branding/banners/banner.png)

**Seven ways to make Forge Energy, batteries to store it, and cables to move it. No machines.**

BarelyPowered adds power sources, batteries and energy cables, and no machines. Each generator pushes its energy into any block beside it that accepts Forge Energy, and cables from any mod can draw from it, so it slots into the pack you already have. Every source is tied to the world: the sun, the wind, a stream, a pool of lava.

![The generators](https://raw.githubusercontent.com/ItMeCasey/BarelyPowered/main/branding/banners/h_generators.png)

- **Hand crank:** free and tiny. Right-click to turn it. Enough to start the first machine.
- **Combustion generator:** coal, charcoal, logs or planks, by hand or from a hopper or pipe. Stone, iron and steel tiers: hotter, faster, and more energy from each item.
- **Lava generator:** lava by bucket or by pipe, a lot of power. Three tiers.
- **Solar panel:** follows the day, needs open sky, dims in rain. Tier 2 is a taller, wider three-panel array and keeps a small output at night and in storms.
- **Wind turbine:** a tall tower with a turning 3D rotor (3 blocks, or 5 for tier 2; it needs that much free space above it). Output rises with build height. Open sky and clear blades matter, other turbines nearby steal wind, storms help. Two tiers.
- **Water wheel:** needs flowing water beside it or falling onto it. Output follows the flow.
- **Geothermal generator:** steady power from lava and magma touching it. No fuel.
- **Batteries:** basic, iron and steel, from 100,000 to 1,600,000 FE. Each face can take in, give out, do both or be off. A broken battery keeps its charge, shows a gauge on the block and drives a comparator.
- **Energy cables:** three tiers from 400 to 6,400 FE per tick, with a switch for every side. They come from BarelyCables, a separate module carried inside the jar, and work with any mod that speaks Forge Energy.

Right-click any generator with an empty hand to see its output, the energy stored, the fuel left and why the output is what it is ("Low altitude, build higher", "Weather dims the sun").

![How output is worked out](https://raw.githubusercontent.com/ItMeCasey/BarelyPowered/main/branding/banners/output.png)

![Config](https://raw.githubusercontent.com/ItMeCasey/BarelyPowered/main/branding/banners/h_config.png)

Every output rate, tier multiplier and the weather, height and flow rules are in one file, `config/barelypowered.properties`. Recipes and the numbers behind them are on the wiki.

![Compatibility](https://raw.githubusercontent.com/ItMeCasey/BarelyPowered/main/branding/banners/h_compat.png)

- Outputs plain Forge Energy through the loader's energy interface on Forge and NeoForge.
- Works against any block that accepts FE. A generator feeding AeroFlux energy pipes into a test block was checked on NeoForge 1.21.1.
- BarelyCables is its own mod inside the jar. If another mod carries the same cables, only one copy loads.
- Optional info plugins: Show Me What This Is and Jade (NeoForge) show a generator's output, fuel and the reason for a low output, a battery's rate and face settings, and a cable network's rate and size. Nothing else is required.
- Shows up in the item viewers BEI, JEI, REI and EMI wherever they have a build for your version and loader (items and crafting recipes; no extra recipe categories).

The pictures on this page are illustrations drawn for the listing, not game screenshots.

**Tested:** every layer (Forge and NeoForge on 1.21.1, 26.1.2, 26.2 and 26.3) compiles, passes its unit tests and loads on a dedicated server, where self tests check that each generator delivers the expected energy, that batteries pass it on, keep it when broken and obey their face settings, and that cables carry it at the tier rate without losing any. Each layer was also run in a real game client with a scripted tour that photographs every generator, a generator feeding a battery and a chain of cables into an energy-accepting test block, with the Show Me What This Is plugin lines on screen. On every layer, two test mods that carry BarelyCables inside them were loaded together and only one copy of the cables loaded. AeroFlux energy pipes were tried with batteries and with the cables on NeoForge 1.21.1 only. Other cable mods, hoppers and pipes feeding fuel, the cables used on their own from the nested jar, and recipe balance over a full game have not been tried yet. The viewers were checked in a real client (search for the mod, open the Hand Crank recipe): BEI on Forge and NeoForge for every version; JEI on NeoForge for every version and on Forge 1.21.1; REI on NeoForge for every version; EMI on NeoForge 1.21.1; Jade on NeoForge for every version. Jade, JEI, REI and EMI have no build for the Forge versions and Minecraft versions not listed, so nothing was checked there. Clicking through to uses and catalysts was not tried. The wind turbine tower with its turning 3D rotor and the taller advanced solar panel were seen in a real client on every layer. Feeding the combustion and lava generators through the item and fluid connections is checked by the server self test on every layer (counts, limits, refusals and the energy that comes out), and a real hopper feeding an iron combustion generator was seen in a client on NeoForge 1.21.1, Forge 1.21.1 and NeoForge 26.3; AeroFlux's item pipe was seen moving coal from a chest into a combustion generator and its fluid pipe moving lava from a tank into a lava generator on NeoForge 1.21.1; other loaders and versions have the self test and the hopper check only.

![FAQ](https://raw.githubusercontent.com/ItMeCasey/BarelyPowered/main/branding/banners/h_faq.png)

**Does it need to be on the server?** Yes, on the server and on every client.

**Does it add cables or machines?** Cables yes (BarelyCables, a separate module in the jar), machines no. Any other cable mod and any machines work too.

**Can I change the numbers?** Yes, all of them, in the config file.

**Fabric?** Not at this time.

**Can I use it in my modpack?** Yes. It's MIT licensed.

Made by ItMeCasey Studios. Source and issues on [GitHub](https://github.com/ItMeCasey/BarelyPowered).
