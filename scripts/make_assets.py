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


BATTERIES = ["battery_basic", "battery_iron", "battery_steel"]


# The solar panels and wind turbines have hand-built Blockbench models (scripts/make_models.py, art/): their block models and
# textures are not generated here, and the flat textures art.py draws for them are removed again.
CUSTOM_MODELS = ["solar_panel_1", "solar_panel_2", "wind_turbine_1", "wind_turbine_2"]


# Blocks without an item: the mast segments and rotor heads that a wind turbine builds above itself.
TOWERS = ["wind_tower_1", "wind_tower_2"]


def ids():
    return [name for name, _ in art.sets()]


def all_ids():
    return ids() + BATTERIES


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
    "battery_basic": "Basic Battery",
    "battery_iron": "Iron Battery",
    "battery_steel": "Steel Battery",
}


def lang():
    d = {f"block.{MODID}.{k}": v for k, v in NAMES.items()}
    d[f"item.{MODID}.steel_ingot"] = "Steel Ingot"
    d[f"block.{MODID}.wind_tower_1"] = "Wind Turbine Tower"
    d[f"block.{MODID}.wind_tower_2"] = "Advanced Wind Turbine Tower"
    d[f"message.{MODID}.wind_space"] = "Not enough room: this turbine needs %s free blocks above it."
    d["tag.item.c.ingots.steel"] = "Steel Ingots"
    d[f"itemGroup.{MODID}"] = "BarelyPowered"
    d.update({
        f"tooltip.{MODID}.hand_crank": "Right-click to crank. Free power for the first machines.",
        f"tooltip.{MODID}.combustion_generator": "Burns coal, charcoal, logs or planks. Right-click with fuel, or feed it from a hopper or pipe.",
        f"tooltip.{MODID}.lava_generator": "Burns lava. Right-click with a lava bucket, or pipe lava in.",
        f"tooltip.{MODID}.solar_panel": "Needs open sky. Strongest at noon, weaker in rain.",
        f"tooltip.{MODID}.wind_turbine": "Builds a tower: needs 2 free blocks above (4 for the Advanced). Needs open sky; stronger higher up, weaker with blocks or turbines nearby.",
        f"tooltip.{MODID}.water_wheel": "Needs flowing water beside or above it.",
        f"tooltip.{MODID}.geothermal_generator": "Place next to lava or magma. Steady, no fuel.",
        f"tooltip.{MODID}.output": "Output: %s",
        f"tooltip.{MODID}.battery": "Stores energy. Right-click a face with an empty hand to set it to input, output, both or off.",
        f"tooltip.{MODID}.charge": "Charge: %s / %s FE",
        f"tooltip.{MODID}.battery_rate": "Takes in and gives out up to %s FE per tick",
        f"message.{MODID}.battery": "%s: %s | Charge %s / %s FE",
        f"smwti.{MODID}.face": "%s: %s",
        f"smwti.{MODID}.battery_rate": "Rate: %s FE/t",
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
        d[f"smwti.{MODID}.note.{k}"] = v.replace(" | ", "")
    d.update({
        f"smwti.{MODID}.output": "Output: %s FE/t",
        f"smwti.{MODID}.stored": "Stored: %s / %s FE",
        f"smwti.{MODID}.fuel": "Fuel: %s x%s, %ss left",
        f"smwti.{MODID}.no_fuel": "No fuel",
        f"smwti.{MODID}.lava": "Lava: %s / %s mB, %ss left",
    })
    return d


def block_models(root):
    base = root / "assets" / MODID
    for name in ids():
        if name in CUSTOM_MODELS:
            variants = {}
            for lit in ("false", "true"):
                for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
                    v = {"model": f"{MODID}:block/{name}" + ("_on" if lit == "true" and not name.startswith("wind_") else "")}
                    if y:
                        v["y"] = y
                    variants[f"facing={facing},lit={lit}"] = v
            write(base / "blockstates" / f"{name}.json", {"variants": variants})
            continue
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
    for tier in (1, 2):
        # The tower blocks that stand on a wind turbine: a mast segment and, at the top, the head with the rotor.
        variants = {}
        for head in ("false", "true"):
            for lit in ("false", "true"):
                for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
                    model = f"wind_tower_{tier}" if head == "false" else f"wind_head_{tier}" + ("_on" if lit == "true" else "")
                    v = {"model": f"{MODID}:block/{model}"}
                    if y:
                        v["y"] = y
                    variants[f"facing={facing},head={head},lit={lit}"] = v
        write(base / "blockstates" / f"wind_tower_{tier}.json", {"variants": variants})
    for tier in ("basic", "iron", "steel"):
        name = f"battery_{tier}"
        variants = {}
        for level in range(9):
            write(base / "models" / "block" / f"{name}_c{level}.json", {
                "parent": "minecraft:block/cube",
                "textures": {
                    "particle": f"{MODID}:block/{name}_top",
                    "down": f"{MODID}:block/{name}_top",
                    "up": f"{MODID}:block/{name}_top",
                    "north": f"{MODID}:block/{name}_c{level}",
                    "south": f"{MODID}:block/{name}_c{level}",
                    "east": f"{MODID}:block/{name}_c{level}",
                    "west": f"{MODID}:block/{name}_c{level}",
                },
            })
            variants[f"charge={level}"] = {"model": f"{MODID}:block/{name}_c{level}"}
        write(base / "blockstates" / f"{name}.json", {"variants": variants})
    write(base / "models" / "item" / "steel_ingot.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MODID}:item/steel_ingot"}})
    write(base / "lang" / "en_us.json", lang())


def sink_assets(root, new):
    """The dev-only test sink's files go in src/dev/resources, which only builds with -PdevTools."""
    root = root.parent.parent / "dev" / "resources"
    base = root / "assets" / MODID
    (base / "textures" / "block").mkdir(parents=True, exist_ok=True)
    art.sink_texture().save(base / "textures" / "block" / "test_energy_sink.png")
    write(base / "blockstates" / "test_energy_sink.json", {"variants": {"": {"model": f"{MODID}:block/test_energy_sink"}}})
    write(base / "models" / "block" / "test_energy_sink.json",
          {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MODID}:block/test_energy_sink"}})
    if new:
        write(base / "items" / "test_energy_sink.json",
              {"model": {"type": "minecraft:model", "model": f"{MODID}:block/test_energy_sink"}})
    else:
        write(base / "models" / "item" / "test_energy_sink.json", {"parent": f"{MODID}:block/test_energy_sink"})


def loot(root, v):
    """Block loot tables. Minecraft 26.3 changed the layout (pool condition, entry modifier), so each version gets its own."""
    data = root / "data" / MODID / "loot_table" / "blocks"
    new263 = v == "26.3"
    for name in all_ids():
        entry = {"type": "minecraft:item", "name": f"{MODID}:{name}"}
        if name in BATTERIES:
            copy = {"source": "block_entity", "include": ["minecraft:custom_data"]}
            if new263:
                entry["modifier"] = dict(copy, type="minecraft:copy_components")
            else:
                entry["functions"] = [dict(copy, function="minecraft:copy_components")]
        if new263:
            pool = {"rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [entry]}
        else:
            pool = {"rolls": 1, "bonus_rolls": 0, "entries": [entry], "conditions": [{"condition": "minecraft:survives_explosion"}]}
        write(data / f"{name}.json", {"type": "minecraft:block", "pools": [pool], "random_sequence": f"{MODID}:blocks/{name}"})


def loot_and_tags(root):
    data = root / "data" / MODID
    write(root / "data" / "minecraft" / "tags" / "block" / "mineable" / "pickaxe.json",
          {"replace": False, "values": [f"{MODID}:{n}" for n in all_ids() + TOWERS]})
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
    "K": "minecraft:redstone_block",
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
    "battery_basic": (["IRI", "RKR", "IRI"], {}),
    "battery_iron": (["IRI", "KXK", "IRI"], {"X": "barelypowered:battery_basic"}),
    "battery_steel": (["SRS", "KXK", "SRS"], {"X": "barelypowered:battery_iron"}),
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
        model = f"{name}_item" if name.startswith("wind_turbine") else name
        write(base / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": f"{MODID}:block/{model}"}})
    for name in BATTERIES:
        write(base / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": f"{MODID}:block/{name}_c4"}})
    write(base / "items" / "steel_ingot.json", {"model": {"type": "minecraft:model", "model": f"{MODID}:item/steel_ingot"}})


def old_item_models(root):
    base = root / "assets" / MODID / "models" / "item"
    for name in ids():
        model = f"{name}_item" if name.startswith("wind_turbine") else name
        write(base / f"{name}.json", {"parent": f"{MODID}:block/{model}"})
    for name in BATTERIES:
        write(base / f"{name}.json", {"parent": f"{MODID}:block/{name}_c4"})


def main():
    common = ROOT / "common" / "src" / "main" / "resources"
    art.render_all(common / "assets" / MODID / "textures")
    for name in CUSTOM_MODELS:
        for part in ("side", "top", "top_on", "front", "front_on"):
            for ext in (".png", ".png.mcmeta"):
                (common / "assets" / MODID / "textures" / "block" / f"{name}_{part}{ext}").unlink(missing_ok=True)
    block_models(common)
    loot_and_tags(common)
    for v in OLD:
        res = ROOT / f"version-{v}" / "src" / "main" / "resources"
        recipes(res, False)
        loot(res, v)
        old_item_models(res)
        sink_assets(res, False)
    for v in NEW:
        res = ROOT / f"version-{v}" / "src" / "main" / "resources"
        recipes(res, True)
        loot(res, v)
        item_defs(res)
        sink_assets(res, True)
    print("assets written")


if __name__ == "__main__":
    main()
