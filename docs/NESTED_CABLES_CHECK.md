# BarelyCables nested in the BarelyPowered jar, used on its own

Checked on 2026-11-09 in a clean NeoForge 21.1.265 install for Minecraft 1.21.1 (client install from the NeoForge installer, launched from the version JSON with Java 21 on the isolated desktop). The `mods` folder held only `barelypowered-mc1.21.1-2026.11.3.jar`, the jar published on GitHub and CurseForge. The world was a superflat test world with a datapack that places a `barelycables:energy_cable_basic` block at 0 -59 0 and says `CABLE-PLACED` in the log. Quick Play joined the world.

What the game log shows (lines of `logs/latest.log`):

```
[09Nov2026 22:14:34.725] [main/INFO] [cpw.mods.modlauncher.Launcher/MODLAUNCHER]: ModLauncher running: args [--fml.neoForgeVersion, 21.1.265, --fml.fmlVersion, 4.0.45, --fml.mcVersion, 1.21.1, --fml.neoFormVersion, 20240808.144430, --launch
[09Nov2026 22:14:35.945] [main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Found mod file "client-1.21.1-20240808.144430-srg.jar" [locator: production client provider +net.neoforged:neoforge:21.1.265:client]
[09Nov2026 22:14:35.989] [main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Found mod file "neoforge-21.1.265-universal.jar" [locator: PathBasedLocator[name=neoforge, paths=[C:\Users\casey\AppData\Local\imc-installs\ne
[09Nov2026 22:14:36.003] [main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Found mod file "barelypowered-mc1.21.1-2026.11.3.jar" [locator: {mods folder locator at C:\Users\casey\AppData\Local\imc-installs\neoforge-1.2
[09Nov2026 22:14:36.238] [main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Found mod file "barelycables-mc1.21.1-2026.11.2.jar" [parent: barelypowered-mc1.21.1-2026.11.3.jar, locator: jarinjar, reader: mod manifest]
		BarelyCables 2026.11.2 (barelycables)
		BarelyPowered 2026.11.3 (barelypowered)
[09Nov2026 22:14:43.665] [modloading-worker-0/INFO] [BarelyCables/]: BarelyCables module initialized
[09Nov2026 22:14:44.680] [Render thread/INFO] [net.minecraft.server.packs.resources.ReloadableResourceManager/]: Reloading ResourceManager: vanilla, mod_resources, mod/barelypowered, mod/barelycables, mod/neoforge
[09Nov2026 22:14:51.526] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: [Not Secure] [Server] CABLE-PLACED barelycables:energy_cable_basic at 0 -59 0
[09Nov2026 22:14:52.181] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Dev joined the game
```

So the loader found BarelyCables inside BarelyPowered's jar with the `jarinjar` locator (no separate cables jar), BarelyCables 2026.11.2 initialized, its resources were loaded, a cable block was placed in the world, and the player joined.

Not covered: Forge and 26.x installs (the same jar-in-jar layout was proven there with the dummy-mod tests only).
