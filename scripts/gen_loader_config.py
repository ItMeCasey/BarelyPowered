"""Writes the per-loader config classes (NeoForge ModConfigSpec, Forge ForgeConfigSpec) and patches the mod entry classes.

Run once from the repository root: python scripts/gen_loader_config.py
The generated files are ordinary source; this script exists so the sixteen copies stay identical.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
VERSIONS = ["1.21.1", "26.1.2", "26.2", "26.3"]

MODS = {
    "powered": dict(dir=ROOT, pkg="com.itmecasey.barelypowered", main="BarelyPowered", settings="Settings",
                    neo_entry="NeoForgeBarelyPowered", forge_entry="ForgeBarelyPowered"),
    "cables": dict(dir=ROOT / "barelycables", pkg="com.itmecasey.barelycables", main="BarelyCables", settings="CableSettings",
                   neo_entry="NeoForgeBarelyCables", forge_entry="ForgeBarelyCables"),
}

NEO = """package {pkg}.neoforge;

import {pkg}.{main};
import {pkg}.{settings};
import {pkg}.config.ConfigSchema;
import java.util.Map;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The mod's config file (config/<modid>-common.toml), built from the one list of settings, and the push of its values into the mod. */
final class NeoForgeConfig {{
    private static ModConfigSpec spec;
    private static Map<String, Supplier<Object>> readers;

    private NeoForgeConfig() {{
    }}

    static void register(IEventBus modBus, ModContainer container) {{
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        readers = ConfigSchema.define({settings}.defs(), new ConfigSchema.Adapter() {{
            @Override
            public Supplier<Object> integer(String key, int fallback, int min, int max, String comment) {{
                ModConfigSpec.IntValue value = builder.comment(comment).defineInRange(key, fallback, min, max);
                return value::get;
            }}

            @Override
            public Supplier<Object> decimal(String key, double fallback, double min, double max, String comment) {{
                ModConfigSpec.DoubleValue value = builder.comment(comment).defineInRange(key, fallback, min, max);
                return value::get;
            }}

            @Override
            public Supplier<Object> bool(String key, boolean fallback, String comment) {{
                ModConfigSpec.BooleanValue value = builder.comment(comment).define(key, fallback);
                return value::get;
            }}
        }});
        spec = builder.build();
        container.registerConfig(ModConfig.Type.{ctype}, spec, {main}.CONFIG_FILE);
        modBus.addListener(NeoForgeConfig::loaded);
        modBus.addListener(NeoForgeConfig::reloaded);
    }}

    private static void loaded(ModConfigEvent.Loading event) {{
        push(event.getConfig());
    }}

    private static void reloaded(ModConfigEvent.Reloading event) {{
        push(event.getConfig());
    }}

    private static void push(ModConfig config) {{
        if (config.getSpec() == spec) {{
            {settings}.apply(ConfigSchema.read(readers));
        }}
    }}
}}
"""

NEO_SCREEN = """package {pkg}.neoforge;

import {pkg}.{main};
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only: the in-game config screen (Mods, then the mod, then Config). Never loaded on a dedicated server. */
@Mod(value = {main}.MODID, dist = Dist.CLIENT)
public final class NeoForgeConfigScreen {{
    public NeoForgeConfigScreen(ModContainer container) {{
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }}
}}
"""

FORGE = """package {pkg}.forge;

import {pkg}.{main};
import {pkg}.{settings};
import {pkg}.config.ConfigSchema;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** The mod's config file (config/<modid>-common.toml), built from the one list of settings, and the push of its values into the mod. */
final class ForgeConfig {{
    private static ForgeConfigSpec spec;
    private static Map<String, Supplier<Object>> readers;

    private ForgeConfig() {{
    }}

    static void register(FMLJavaModLoadingContext context) {{
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        readers = ConfigSchema.define({settings}.defs(), new ConfigSchema.Adapter() {{
            @Override
            public Supplier<Object> integer(String key, int fallback, int min, int max, String comment) {{
                ForgeConfigSpec.IntValue value = builder.comment(comment).defineInRange(key, fallback, min, max);
                return value::get;
            }}

            @Override
            public Supplier<Object> decimal(String key, double fallback, double min, double max, String comment) {{
                ForgeConfigSpec.DoubleValue value = builder.comment(comment).defineInRange(key, fallback, min, max);
                return value::get;
            }}

            @Override
            public Supplier<Object> bool(String key, boolean fallback, String comment) {{
                ForgeConfigSpec.BooleanValue value = builder.comment(comment).define(key, fallback);
                return value::get;
            }}
        }});
        spec = builder.build();
        context.registerConfig(ModConfig.Type.COMMON, spec, {main}.CONFIG_FILE);
{listeners}
    }}

    private static void loaded(ModConfigEvent.Loading event) {{
        push(event.getConfig());
    }}

    private static void reloaded(ModConfigEvent.Reloading event) {{
        push(event.getConfig());
    }}

    private static void push(ModConfig config) {{
        if (config.getSpec() == spec) {{
            {settings}.apply(ConfigSchema.read(readers));
        }}
    }}
}}
"""

FORGE_OLD_BUS = """        context.getModEventBus().addListener(ForgeConfig::loaded);
        context.getModEventBus().addListener(ForgeConfig::reloaded);"""
FORGE_NEW_BUS = """        ModConfigEvent.Loading.getBus(context.getModBusGroup()).addListener(ForgeConfig::loaded);
        ModConfigEvent.Reloading.getBus(context.getModBusGroup()).addListener(ForgeConfig::reloaded);"""


def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(text.encode("utf-8"))


def patch(path, edits):
    raw = path.read_bytes().decode("utf-8")
    nl = "\r\n" if "\r\n" in raw else "\n"
    s = raw.replace("\r\n", "\n")
    for old, new in edits:
        if new in s:
            continue
        assert old in s, (path, old)
        s = s.replace(old, new, 1)
    path.write_bytes(s.replace("\n", nl).encode("utf-8"))


for name, m in MODS.items():
    fields = dict(pkg=m["pkg"], main=m["main"], settings=m["settings"])
    pkgdir = m["pkg"].replace(".", "/")
    for v in VERSIONS:
        neo = m["dir"] / f"neoforge-{v}" / "src/main/java" / pkgdir / "neoforge"
        # FML 12 (NeoForge 26.3) renamed the COMMON config type to LOCAL
        write(neo / "NeoForgeConfig.java", NEO.format(ctype="LOCAL" if v == "26.3" else "COMMON", **fields))
        write(neo / "NeoForgeConfigScreen.java", NEO_SCREEN.format(**fields))
        patch(neo / f"{m['neo_entry']}.java", [
            ("(IEventBus modBus) {", "(IEventBus modBus, net.neoforged.fml.ModContainer container) {"),
            (f"{m['main']}.init(FMLPaths.CONFIGDIR::get);",
             f"{m['main']}.init(FMLPaths.CONFIGDIR::get);\n        NeoForgeConfig.register(modBus, container);"),
        ])
        forge = m["dir"] / f"forge-{v}" / "src/main/java" / pkgdir / "forge"
        listeners = FORGE_OLD_BUS if v == "1.21.1" else FORGE_NEW_BUS
        write(forge / "ForgeConfig.java", FORGE.format(listeners=listeners, **fields))
        patch(forge / f"{m['forge_entry']}.java", [
            (f"{m['main']}.init(FMLPaths.CONFIGDIR::get);",
             f"{m['main']}.init(FMLPaths.CONFIGDIR::get);\n        ForgeConfig.register(context);"),
        ])
print("ok")
