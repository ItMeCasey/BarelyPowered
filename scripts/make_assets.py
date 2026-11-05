"""Writes every resource file the mod ships: textures, models, block states, recipes, loot tables, tags and the language file.

Run from the repository root: python scripts/make_assets.py
Shared files go to common/src/main/resources; formats that changed between Minecraft versions go to version-<mc>/src/main/resources.
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import art  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
MODID = "barelypowered"
OLD = ["1.21.1"]
NEW = ["26.1.2", "26.2", "26.3"]


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def ids():
    return [name for name, _ in art.sets()]


NAMES = {
    "hand_crank": "Hand Crank",
    "combustion_generator_stone": "Stone Combustion Generator",
    "combustion_generator_iron": "Iron Combustion Generator",
    "combustion_generator_steel": "Steel Combustion Generator",
    "lava_generator_stone": "Stone Lava Generator",
    "lava_generator_iron": "Iron Lava Generator",
    "lava_generator_steel": "Steel Lava Generator",
    "solar_panel_1": "Solar Panel",
    "solar_panel_2": "Advanced Solar Panel",
    "wind_turbine_1": "Wind Turbine",
    "wind_turbine_2": "Advanced Wind Turbine",
    "water_wheel": "Water Wheel",
    "geothermal_generator": "Geothermal Generator",
}


def lang():
    d = {f"block.{MODID}.{k}": v for k, v in NAMES.items()}
    d[f"item.{MODID}.steel_ingot"] = "Steel Ingot"
    d[f"itemGroup.{MODID}"] = "BarelyPowered"
    d[f"block.{MODID}.test_energy_sink"] = "Test Energy Sink (dev only)"
    d.update({
        f"tooltip.{MODID}.hand_crank": "Right-click to crank. Free power for the first machines.",
        f"tooltip.{MODID}.combustion_generator": "Burns coal, charcoal, logs or planks. Right-click with fuel.",
        f"tooltip.{MODID}.lava_generator": "Burns lava. Right-click with a lava bucket.",
        f"tooltip.{MODID}.solar_panel": "Needs open sky. Strongest at noon, weaker in rain.",
        f"tooltip.{MODID}.wind_turbine": "Needs open sky. Stronger higher up, weaker with blocks or turbines nearby.",
        f"tooltip.{MODID}.water_wheel": "Needs flowing water beside or above it.",
        f"tooltip.{MODID}.geothermal_generator": "Place next to lava or magma. Steady, no fuel.",
        f"tooltip.{MODID}.output": "Output: %s",
        f"message.{MODID}.status": "Output %s FE/t | Stored %s / %s FE",
        f"message.{MODID}.fuel": " | Fuel: %s x%s (%ss left)",
        f"message.{MODID}.lava": " | Lava: %s / %s mB (%ss left)",
        f"message.{MODID}.none": "empty",
    })
    notes = {
        "idle": "", "ok": "", "burning": "", "cranking": " | Cranking",
        "no_fuel": " | No fuel", "no_lava": " | No lava", "full": " | Buffer full, waiting",
        "no_sky": " | No open sky", "night": " | Night", "weather": " | Weather dims the sun",
        "blocked": " | Blocks around the blades", "crowded": " | Other turbines take wind",
        "low": " | Low altitude, build higher", "no_flow": " | No flowing water", "no_heat": " | No lava or magma nearby",
    }
    for k, v in notes.items():
        d[f"message.{MODID}.note.{k}"] = v
    return d


def block_models(root):
    base = root / "assets" / MODID
    for name in ids():
        for suffix, front, top in (("", f"{name}_front", f"{name}_top"), ("_on", f"{name}_front_on", f"{name}_top_on")):
            write(base / "models" / "block" / f"{name}{suffix}.json", {
                "parent": "minecraft:block/orientable",
                "textures": {
                    "top": f"{MODID}:block/{top}",
                    "front": f"{MODID}:block/{front}",
                    "side": f"{MODID}:block/{name}_side",
                },
            })
        variants = {}
        for lit in ("false", "true"):
            for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
                v = {"model": f"{MODID}:block/{name}" + ("_on" if lit == "true" else "")}
                if y:
                    v["y"] = y
                variants[f"facing={facing},lit={lit}"] = v
        write(base / "blockstates" / f"{name}.json", {"variants": variants})
    write(base / "models" / "item" / "steel_ingot.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MODID}:item/steel_ingot"}})
    write(base / "lang" / "en_us.json", lang())


def sink_assets(root, new):
    base = root / "assets" / MODID
    write(base / "blockstates" / "test_energy_sink.json", {"variants": {"": {"model": f"{MODID}:block/test_energy_sink"}}})
    write(base / "models" / "block" / "test_energy_sink.json",
          {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MODID}:block/test_energy_sink"}})
    if new:
        write(base / "items" / "test_energy_sink.json",
              {"model": {"type": "minecraft:model", "model": f"{MODID}:block/test_energy_sink"}})
    else:
        write(base / "models" / "item" / "test_energy_sink.json", {"parent": f"{MODID}:block/test_energy_sink"})


def loot_and_tags(root):
    data = root / "data" / MODID
    for name in ids():
        write(data / "loot_table" / "blocks" / f"{name}.json", {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1,
                "bonus_rolls": 0,
                "entries": [{"type": "minecraft:item", "name": f"{MODID}:{name}"}],
                "conditions": [{"condition": "minecraft:survives_explosion"}],
            }],
            "random_sequence": f"{MODID}:blocks/{name}",
        })
    write(root / "data" / "minecraft" / "tags" / "block" / "mineable" / "pickaxe.json",
          {"replace": False, "values": [f"{MODID}:{n}" for n in ids()]})
    write(root / "data" / "c" / "tags" / "item" / "ingots" / "steel.json",
          {"replace": False, "values": [f"{MODID}:steel_ingot"]})


# ---- Recipes ----
# Ingredient letters used in the patterns below.
ING = {
    "S": "barelypowered:steel_ingot",
    "I": "minecraft:iron_ingot",
    "R": "minecraft:redstone",
    "C": "minecraft:cobblestone",
    "F": "minecraft:furnace",
    "B": "minecraft:bucket",
    "G": "minecraft:glass",
    "L": "minecraft:lapis_lazuli",
    "Q": "minecraft:quartz",
    "D": "minecraft:gold_ingot",
    "M": "minecraft:magma_block",
    "T": "minecraft:stick",
    "P": "#minecraft:planks",
}

SHAPED = {
    "hand_crank": ([" T ", " T ", "PCP"], {}),
    "combustion_generator_stone": (["CCC", "CFC", "CRC"], {}),
    "combustion_generator_iron": (["IRI", "IXI", "III"], {"X": "barelypowered:combustion_generator_stone"}),
    "combustion_generator_steel": (["SRS", "SXS", "SSS"], {"X": "barelypowered:combustion_generator_iron"}),
    "lava_generator_stone": (["CCC", "CBC", "IFI"], {}),
    "lava_generator_iron": (["IRI", "IXI", "III"], {"X": "barelypowered:lava_generator_stone"}),
    "lava_generator_steel": (["SRS", "SXS", "SSS"], {"X": "barelypowered:lava_generator_iron"}),
    "solar_panel_1": (["GGG", "LRL", "III"], {}),
    "solar_panel_2": (["QSQ", "XXX", "DRD"], {"X": "barelypowered:solar_panel_1"}),
    "wind_turbine_1": (["IPI", "PRP", "CCC"], {}),
    "wind_turbine_2": (["SIS", "IXI", "SRS"], {"X": "barelypowered:wind_turbine_1"}),
    "water_wheel": (["PTP", "TIT", "PTP"], {}),
    "geothermal_generator": (["III", "IMI", "SRS"], {}),
}


def ingredient(token, new):
    if token.startswith("#"):
        return token if new else {"tag": token[1:]}
    return token if new else {"item": token}


def recipes(root, new):
    data = root / "data" / MODID / "recipe"
    for name, (pattern, extra) in SHAPED.items():
        letters = {c for row in pattern for c in row if c != " "}
        key = {}
        for c in sorted(letters):
            key[c] = ingredient(extra.get(c) or ING[c], new)
        write(data / f"{name}.json", {
            "type": "minecraft:crafting_shaped",
            "category": "redstone",
            "pattern": pattern,
            "key": key,
            "result": {"id": f"{MODID}:{name}", "count": 1},
        })
    steel = {
        "type": "minecraft:blasting",
        "category": "misc",
        "cookingtime": 400,
        "experience": 0.7,
        "ingredient": ingredient("minecraft:iron_ingot", new),
        "result": {"id": f"{MODID}:steel_ingot"},
    }
    write(data / "steel_ingot_from_blasting.json", steel)


def item_defs(root):
    base = root / "assets" / MODID
    for name in ids():
        write(base / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": f"{MODID}:block/{name}"}})
    write(base / "items" / "steel_ingot.json", {"model": {"type": "minecraft:model", "model": f"{MODID}:item/steel_ingot"}})


def old_item_models(root):
    base = root / "assets" / MODID / "models" / "item"
    for name in ids():
        write(base / f"{name}.json", {"parent": f"{MODID}:block/{name}"})


def main():
    common = ROOT / "common" / "src" / "main" / "resources"
    art.render_all(common / "assets" / MODID / "textures")
    block_models(common)
    loot_and_tags(common)
    for v in OLD:
        res = ROOT / f"version-{v}" / "src" / "main" / "resources"
        recipes(res, False)
        old_item_models(res)
        sink_assets(res, False)
    for v in NEW:
        res = ROOT / f"version-{v}" / "src" / "main" / "resources"
        recipes(res, True)
        item_defs(res)
        sink_assets(res, True)
    print("assets written")


if __name__ == "__main__":
    main()
