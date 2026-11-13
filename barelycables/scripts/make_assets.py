"""Writes every resource file of BarelyCables: textures, models, block states, recipes, loot tables, tags, language file and logo.

Run from the barelycables folder (or anywhere): python scripts/make_assets.py
Shared files go to common/src/main/resources; formats that changed between Minecraft versions go to version-<mc>/src/main/resources.
Everything here is drawn for this mod; nothing is copied from Minecraft or another mod.
"""
import json
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
MODID = "barelycables"
TIERS = ("basic", "iron", "steel")
OLD = ["1.21.1"]
NEW = ["26.1.2", "26.2", "26.3"]
NIGHT = (22, 21, 38)
GOLD = (242, 179, 61)
GOLD_L = (255, 226, 140)
GOLD_D = (178, 116, 32)

PALETTE = {  # base, dark, light
    "basic": ((196, 112, 52), (132, 70, 30), (232, 156, 88)),
    "iron": ((172, 176, 190), (112, 116, 132), (216, 220, 232)),
    "steel": ((78, 90, 116), (48, 56, 78), (112, 126, 156)),
}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def cable_texture(tier, seed):
    base, dark, light = PALETTE[tier]
    rnd = random.Random(seed)
    img = Image.new("RGB", (16, 16), base)
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = shade(base, 1 + rnd.uniform(-0.05, 0.05))
    for y in (0, 15):
        for x in range(16):
            px[x, y] = dark
    for y in range(1, 15):
        px[0, y] = light
        px[15, y] = dark
    for x in range(16):
        px[x, 7] = shade(GOLD, 1.05)
        px[x, 8] = GOLD_D
    for x in (3, 4, 11, 12):
        for y in (3, 4, 11, 12):
            px[x, y] = light
    return img


def logo():
    n = 32
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((0, 0, n - 1, n - 1), radius=4, fill=NIGHT + (255,), outline=(58, 54, 88, 255))
    # a cable with a plug at each end and a bolt of energy in the middle
    d.rectangle((6, 15, 25, 17), fill=PALETTE["basic"][0] + (255,))
    d.rectangle((6, 15, 25, 15), fill=PALETTE["basic"][2] + (255,))
    d.rectangle((3, 12, 7, 20), fill=GOLD_D + (255,))
    d.rectangle((3, 12, 7, 13), fill=GOLD + (255,))
    d.rectangle((24, 12, 28, 20), fill=GOLD_D + (255,))
    d.rectangle((24, 12, 28, 13), fill=GOLD + (255,))
    for x, y in ((14, 6), (15, 6), (13, 7), (14, 7), (12, 8), (13, 8), (14, 8), (15, 8), (15, 9), (14, 9), (13, 10), (14, 11)):
        img.putpixel((x, y), GOLD_L + (255,) if (x + y) % 3 == 0 else GOLD + (255,))
    for x, y in ((8, 24), (9, 24), (22, 24), (23, 24), (15, 24), (16, 24)):
        img.putpixel((x, y), GOLD_D + (255,))
    return img


def box_model(parent_item=False):
    return None


def cube_faces(uvs):
    return {k: {"uv": v, "texture": "#cable"} for k, v in uvs.items()}


def models(root):
    base = root / "assets" / MODID
    for tier in TIERS:
        name = f"energy_cable_{tier}"
        tex = {"cable": f"{MODID}:block/{name}", "particle": f"{MODID}:block/{name}"}
        write(base / "models/block" / f"{name}_core.json", {
            "textures": tex,
            "elements": [{"from": [6, 6, 6], "to": [10, 10, 10], "faces": cube_faces({
                "north": [6, 6, 10, 10], "south": [6, 6, 10, 10], "east": [6, 6, 10, 10],
                "west": [6, 6, 10, 10], "up": [6, 6, 10, 10], "down": [6, 6, 10, 10]})}],
        })
        write(base / "models/block" / f"{name}_arm.json", {
            "textures": tex,
            "elements": [{"from": [6, 6, 0], "to": [10, 10, 6], "faces": cube_faces({
                "north": [6, 6, 10, 10], "south": [6, 6, 10, 10], "east": [0, 6, 6, 10],
                "west": [0, 6, 6, 10], "up": [6, 0, 10, 6], "down": [6, 0, 10, 6]})}],
        })
        write(base / "models/block" / f"{name}_item.json", {
            "parent": "minecraft:block/block",
            "textures": tex,
            "elements": [{"from": [0, 6, 6], "to": [16, 10, 10], "faces": cube_faces({
                "north": [0, 6, 16, 10], "south": [0, 6, 16, 10], "east": [6, 6, 10, 10],
                "west": [6, 6, 10, 10], "up": [0, 6, 16, 10], "down": [0, 6, 16, 10]})}],
            "display": {"gui": {"rotation": [30, 225, 0], "scale": [1.2, 1.2, 1.2]}, "ground": {"scale": [1.0, 1.0, 1.0]},
                        "fixed": {"scale": [1.2, 1.2, 1.2]}, "thirdperson_righthand": {"rotation": [75, 45, 0], "scale": [0.9, 0.9, 0.9]},
                        "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.9, 0.9, 0.9]}},
        })
        arm = f"{MODID}:block/{name}_arm"
        write(base / "blockstates" / f"{name}.json", {"multipart": [
            {"apply": {"model": f"{MODID}:block/{name}_core"}},
            {"when": {"north": "true"}, "apply": {"model": arm}},
            {"when": {"east": "true"}, "apply": {"model": arm, "y": 90}},
            {"when": {"south": "true"}, "apply": {"model": arm, "y": 180}},
            {"when": {"west": "true"}, "apply": {"model": arm, "y": 270}},
            {"when": {"up": "true"}, "apply": {"model": arm, "x": 270}},
            {"when": {"down": "true"}, "apply": {"model": arm, "x": 90}},
        ]})
        (base / "textures/block").mkdir(parents=True, exist_ok=True)
        cable_texture(tier, 5 + TIERS.index(tier)).save(base / "textures/block" / f"{name}.png")
    names = {"basic": "Basic Energy Cable", "iron": "Iron Energy Cable", "steel": "Steel Energy Cable"}
    lang = {f"block.{MODID}.energy_cable_{t}": names[t] for t in TIERS}
    lang.update({
        f"itemGroup.{MODID}": "BarelyCables",
        "tag.item.c.ingots.steel": "Steel Ingots",
        f"tooltip.{MODID}.rate": "A network of these moves up to %s FE per tick",
        f"message.{MODID}.face_off": "%s side: switched off",
        f"message.{MODID}.face_on": "%s side: switched on",
        f"smwti.{MODID}.rate": "Network rate: %s FE/t",
        f"smwti.{MODID}.buffer": "Network buffer: %s / %s FE",
        f"smwti.{MODID}.size": "%s cables, %s connections",
        f"smwti.{MODID}.face_off": "%s side switched off",
    })
    for d in ("down", "up", "north", "south", "west", "east"):
        lang[f"message.{MODID}.face.{d}"] = d.capitalize()
    write(base / "lang" / "en_us.json", lang)
    out = root.parent.parent / "main" / "resources"
    logo().resize((512, 512), Image.NEAREST).save(root / f"{MODID}_logo.png")


ING = {"C": "minecraft:copper_ingot", "R": "minecraft:redstone", "I": "minecraft:iron_ingot", "G": "minecraft:gold_ingot",
       "S": "#c:ingots/steel"}
RECIPES = {
    "energy_cable_basic": (["CCC", "RRR", "CCC"], {}, 6),
    "energy_cable_iron": (["III", "XXX", "III"], {"X": f"{MODID}:energy_cable_basic"}, 4),
    "energy_cable_steel": (["SSS", "XXX", "SSS"], {"X": f"{MODID}:energy_cable_iron"}, 4),
    "energy_cable_steel_from_gold": (["GGG", "XXX", "GGG"], {"X": f"{MODID}:energy_cable_iron"}, 4),
}


def ingredient(token, new):
    if token.startswith("#"):
        return token if new else {"tag": token[1:]}
    return token if new else {"item": token}


def recipes(root, new):
    data = root / "data" / MODID / "recipe"
    for name, (pattern, extra, count) in RECIPES.items():
        key = {}
        for c in sorted({c for row in pattern for c in row if c != " "}):
            key[c] = ingredient(extra.get(c) or ING[c], new)
        result = name if name != "energy_cable_steel_from_gold" else "energy_cable_steel"
        write(data / f"{name}.json", {"type": "minecraft:crafting_shaped", "category": "redstone", "pattern": pattern, "key": key,
                                      "result": {"id": f"{MODID}:{result}", "count": count}})


def loot(root, v):
    data = root / "data" / MODID / "loot_table" / "blocks"
    for tier in TIERS:
        name = f"energy_cable_{tier}"
        entry = {"type": "minecraft:item", "name": f"{MODID}:{name}"}
        if v == "26.3":
            pool = {"rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [entry]}
        else:
            pool = {"rolls": 1, "bonus_rolls": 0, "entries": [entry], "conditions": [{"condition": "minecraft:survives_explosion"}]}
        write(data / f"{name}.json", {"type": "minecraft:block", "pools": [pool], "random_sequence": f"{MODID}:blocks/{name}"})


def items(root, new):
    base = root / "assets" / MODID
    for tier in TIERS:
        name = f"energy_cable_{tier}"
        if new:
            write(base / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": f"{MODID}:block/{name}_item"}})
        else:
            write(base / "models/item" / f"{name}.json", {"parent": f"{MODID}:block/{name}_item"})


def dev(root, new):
    """The dev-only test source and sink blocks go in src/dev/resources, which only builds with -PdevTools."""
    base = root.parent.parent / "dev" / "resources" / "assets" / MODID
    for name, color in (("test_energy_source", (60, 160, 90)), ("test_energy_sink", (120, 70, 170))):
        (base / "textures/block").mkdir(parents=True, exist_ok=True)
        img = Image.new("RGB", (16, 16), color)
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, 15, 15), outline=shade(color, 0.6))
        d.polygon([(9, 2), (5, 8), (8, 8), (6, 14), (11, 6), (8, 6), (10, 2)], fill=GOLD, outline=GOLD_D)
        if name == "test_energy_sink":
            img = img.transpose(Image.FLIP_TOP_BOTTOM)
        img.save(base / "textures/block" / f"{name}.png")
        write(base / "blockstates" / f"{name}.json", {"variants": {"": {"model": f"{MODID}:block/{name}"}}})
        write(base / "models/block" / f"{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MODID}:block/{name}"}})
        if new:
            write(base / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": f"{MODID}:block/{name}"}})
        else:
            write(base / "models/item" / f"{name}.json", {"parent": f"{MODID}:block/{name}"})


def main():
    common = ROOT / "common" / "src" / "main" / "resources"
    models(common)
    write(common / "data" / "minecraft" / "tags" / "block" / "mineable" / "pickaxe.json",
          {"replace": False, "values": [f"{MODID}:energy_cable_{t}" for t in TIERS]})
    # Declared empty so the steel recipe always parses; BarelyPowered or any mod with steel fills it.
    write(common / "data" / "c" / "tags" / "item" / "ingots" / "steel.json", {"replace": False, "values": []})
    for v in OLD + NEW:
        new = v in NEW
        res = ROOT / f"version-{v}" / "src" / "main" / "resources"
        recipes(res, new)
        loot(res, v)
        items(res, new)
        dev(res, new)
    print("barelycables assets written")


if __name__ == "__main__":
    main()
