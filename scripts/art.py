"""Pixel art for BarelyPowered, drawn by script (original, 16x16 tiles).

`render_all(out_textures_dir)` writes every block and item texture plus animation metadata. Drawn in the same palette as the
studio branding: night blue, orange-gold trim, steel grays.
"""
import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

GOLD = (242, 179, 61)
GOLD_L = (255, 226, 140)
GOLD_D = (178, 116, 32)
NIGHT = (22, 21, 38)
NIGHT_L = (46, 42, 70)

BODIES = {
    "stone": ((140, 140, 148), (96, 96, 106), (176, 176, 186)),
    "iron": ((201, 204, 214), (144, 148, 162), (236, 238, 245)),
    "steel": ((86, 98, 122), (56, 66, 90), (122, 136, 166)),
    "solar": ((176, 184, 200), (110, 118, 140), (220, 226, 238)),
    "wind": ((214, 218, 228), (150, 156, 172), (244, 246, 250)),
    "wood": ((150, 112, 70), (104, 74, 44), (190, 146, 94)),
    "basalt": ((62, 58, 68), (40, 37, 46), (92, 86, 100)),
    "crank": ((120, 124, 134), (80, 84, 96), (170, 174, 186)),
}


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3])


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def body_tile(body, seed):
    base, dark, light = body
    rnd = random.Random(seed)
    img = Image.new("RGB", (16, 16), base)
    px = img.load()
    for y in range(16):
        for x in range(16):
            f = 1.0 + rnd.uniform(-0.05, 0.05)
            px[x, y] = shade(base, f)
    for i in range(16):
        px[i, 0] = light
        px[0, i] = light
        px[i, 15] = dark
        px[15, i] = dark
    px[0, 15] = dark
    px[15, 0] = light
    return img


def rivets(img, body):
    px = img.load()
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px[x, y] = body[2]
        px[x + (1 if x < 8 else -1), y] = body[1]


def side_tile(body, seed, trim=GOLD):
    img = body_tile(body, seed)
    px = img.load()
    for x in range(1, 15):
        px[x, 7] = shade(trim, 1.1)
        px[x, 8] = trim
        px[x, 9] = shade(trim, 0.7)
    rivets(img, body)
    return img


def top_tile(body, seed, trim=GOLD):
    img = body_tile(body, seed)
    d = ImageDraw.Draw(img)
    d.rectangle((3, 3, 12, 12), outline=body[1])
    d.rectangle((4, 4, 11, 11), outline=trim)
    px = img.load()
    for i in range(6, 10):
        px[i, 7] = body[1]
        px[7, i] = body[1]
    return img


def frame_with_panel(body, seed):
    """A front tile with a dark inset window at (3,3)-(12,12) for the device art."""
    img = body_tile(body, seed)
    d = ImageDraw.Draw(img)
    d.rectangle((2, 2, 13, 13), fill=body[1])
    d.rectangle((3, 3, 12, 12), fill=NIGHT)
    d.rectangle((3, 3, 12, 3), fill=NIGHT_L)
    px = img.load()
    for x in range(2, 14):
        px[x, 14] = GOLD_D
    rivets(img, body)
    return img


def strip(frames):
    out = Image.new("RGBA", (16, 16 * len(frames)))
    for i, f in enumerate(frames):
        out.paste(f.convert("RGBA"), (0, 16 * i))
    return out


# ---- Fire, lava, glow ----

FLAME = [(255, 247, 190), (255, 214, 90), (255, 150, 40), (214, 70, 24)]


def fire_frame(body, seed, frame, intensity=1.0):
    img = frame_with_panel(body, seed)
    d = ImageDraw.Draw(img)
    px = img.load()
    # grate
    for x in range(4, 12):
        px[x, 11] = (70, 66, 80)
    rnd = random.Random(frame * 97 + 5)
    for col in range(4, 12):
        h = 3 + int(rnd.random() * 5 * intensity) + (2 if col in (6, 7, 8, 9) else 0)
        for k in range(h):
            y = 10 - k
            if y < 4:
                break
            t = k / max(1, h - 1)
            c = FLAME[min(3, int(t * 3.4))] if t < 0.95 else FLAME[3]
            if k == 0:
                c = FLAME[1]
            px[col, y] = c
    return img


def cold_fire(body, seed):
    img = frame_with_panel(body, seed)
    px = img.load()
    for x in range(4, 12):
        px[x, 11] = (70, 66, 80)
        px[x, 10] = (58, 52, 60) if x % 2 else (48, 44, 52)
    for x in (6, 9):
        px[x, 10] = (120, 52, 34)
    return img


def lava_frame(body, seed, frame, lit=True):
    img = frame_with_panel(body, seed)
    px = img.load()
    rnd = random.Random(seed)
    for y in range(4, 12):
        for x in range(4, 12):
            wave = math.sin((x + frame * 1.5) * 0.9 + y * 0.5) * 0.5 + 0.5
            c = mix((200, 60, 18), (255, 190, 60), wave * (0.4 + 0.6 * ((y - 4) / 8)))
            px[x, y] = c if lit else shade(c, 0.28)
    if lit:
        bx = 5 + (frame * 3) % 6
        by = 9 - (frame * 2) % 5
        px[bx, by] = (255, 240, 170)
    # tank window frame
    d = ImageDraw.Draw(img)
    d.rectangle((3, 3, 12, 12), outline=NIGHT_L)
    return img


# ---- Rotating things ----


def rot(cx, cy, r, ang):
    return cx + r * math.cos(ang), cy + r * math.sin(ang)


def blade_frame(body, seed, frame, blades, steps, lit=True, hub=GOLD):
    img = frame_with_panel(body, seed)
    d = ImageDraw.Draw(img)
    px = img.load()
    for y in range(4, 12):
        for x in range(4, 12):
            px[x, y] = mix((52, 78, 132), (110, 150, 205), (y - 4) / 8)
    cx = cy = 7.5
    ang0 = 2 * math.pi / blades * frame / steps
    col = (250, 250, 255) if lit else (160, 164, 176)
    edge = (176, 188, 214) if lit else (110, 114, 126)
    for b in range(blades):
        a = ang0 + 2 * math.pi * b / blades
        tip = rot(cx, cy, 5.2, a)
        left = rot(cx, cy, 2.4, a - 0.42)
        right = rot(cx, cy, 2.4, a + 0.42)
        d.polygon([(cx, cy), left, tip, right], fill=col, outline=edge)
    d.ellipse((6.4, 6.4, 8.6, 8.6), fill=hub)
    px[7, 7] = GOLD_L
    d.rectangle((3, 3, 12, 12), outline=NIGHT_L)
    return img


def wheel_frame(body, seed, frame, steps, lit=True):
    img = frame_with_panel(body, seed)
    px = img.load()
    for y in range(4, 12):
        for x in range(4, 12):
            px[x, y] = mix((20, 52, 96), (30, 100, 170), (y - 4) / 8) if lit else (20, 36, 60)
    d = ImageDraw.Draw(img)
    cx = cy = 7.5
    ang0 = (2 * math.pi / 6) * frame / steps
    wood = (170, 124, 72) if lit else (110, 80, 50)
    for s in range(6):
        a = ang0 + 2 * math.pi * s / 6
        tip = rot(cx, cy, 4.4, a)
        d.line([(cx, cy), tip], fill=wood)
        pa = rot(cx, cy, 4.4, a)
        pb = rot(cx, cy, 4.4, a + 0.5)
        d.line([pa, pb], fill=(214, 168, 100) if lit else (130, 100, 64))
    d.ellipse((6, 6, 9, 9), fill=GOLD)
    px[7, 7] = GOLD_L
    if lit:
        for i, (x, y) in enumerate(((4, 11), (11, 10), (6, 4), (10, 5))):
            if (i + frame) % 3 == 0:
                px[x, y] = (200, 230, 255)
    d.rectangle((3, 3, 12, 12), outline=NIGHT_L)
    return img


def crank_frame(body, seed, frame, steps, lit=True):
    img = frame_with_panel(body, seed)
    d = ImageDraw.Draw(img)
    cx = cy = 7.5
    a = 2 * math.pi * frame / steps
    tip = rot(cx, cy, 4.2, a)
    d.line([(cx, cy), tip], fill=(210, 214, 224) if lit else (140, 144, 156), width=2)
    d.ellipse((tip[0] - 1.4, tip[1] - 1.4, tip[0] + 1.4, tip[1] + 1.4), fill=GOLD if lit else GOLD_D)
    d.ellipse((6, 6, 9, 9), fill=GOLD_D)
    d.rectangle((3, 3, 12, 12), outline=NIGHT_L)
    return img


def geo_frame(body, seed, frame, steps, lit=True):
    img = body_tile(body, seed)
    px = img.load()
    k = 0.55 + 0.45 * math.sin(2 * math.pi * frame / steps)
    glow = mix((150, 50, 20), (255, 170, 50), k) if lit else (88, 34, 22)
    cracks = [(3, 2), (3, 3), (4, 4), (5, 5), (6, 6), (7, 7), (7, 8), (8, 9), (9, 10), (10, 11), (11, 12), (12, 13),
              (12, 5), (11, 6), (10, 7), (9, 7), (4, 10), (5, 10), (6, 9), (13, 3), (2, 12), (3, 12)]
    for x, y in cracks:
        px[x, y] = glow
        if lit and k > 0.7:
            for dx, dy in ((1, 0), (0, 1)):
                nx, ny = x + dx, y + dy
                if 0 < nx < 15 and 0 < ny < 15 and px[nx, ny] not in cracks:
                    px[nx, ny] = mix(px[nx, ny], glow, 0.25)
    for x in range(1, 15):
        px[x, 14] = GOLD_D
    return img


def solar_top(lit, seed):
    img = Image.new("RGB", (16, 16), BODIES["solar"][0])
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 15), fill=BODIES["solar"][1])
    d.rectangle((1, 1, 14, 14), fill=(40, 62, 130) if lit else (28, 42, 88))
    px = img.load()
    for y in range(2, 14):
        for x in range(2, 14):
            if (x - 2) % 4 == 3 or (y - 2) % 4 == 3:
                px[x, y] = (150, 170, 210) if lit else (96, 110, 150)
            else:
                f = 0.85 + 0.25 * ((x + y) % 5) / 5
                base = (52, 88, 176) if lit else (34, 52, 104)
                px[x, y] = shade(base, f)
    if lit:
        for i in range(4):
            px[3 + i, 3 + i] = (210, 230, 255)
    for x, y in ((0, 0), (15, 0), (0, 15), (15, 15)):
        px[x, y] = GOLD
    return img


def solar_top2(lit, seed):
    img = solar_top(lit, seed)
    d = ImageDraw.Draw(img)
    d.rectangle((1, 1, 14, 14), outline=GOLD if lit else GOLD_D)
    px = img.load()
    for x, y in ((7, 7), (8, 8), (7, 8), (8, 7)):
        px[x, y] = (255, 240, 170) if lit else (120, 100, 70)
    return img


def solar_front(body, seed, advanced, lit):
    img = frame_with_panel(body, seed)
    px = img.load()
    for y in range(4, 12):
        for x in range(4, 12):
            if (x - 4) % 4 == 3 or (y - 4) % 4 == 3:
                px[x, y] = (150, 170, 210) if lit else (90, 104, 140)
            else:
                px[x, y] = (58, 94, 184) if lit else (34, 52, 104)
    if lit:
        px[5, 5] = (220, 236, 255)
        px[6, 6] = (190, 214, 250)
    if advanced:
        for x in range(3, 13):
            px[x, 3] = GOLD if lit else GOLD_D
    return img


def sink_texture():
    img = Image.new("RGB", (16, 16), (70, 40, 110))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 15), outline=(40, 22, 70))
    d.rectangle((2, 2, 13, 13), outline=(120, 80, 170))
    bolt = [(9, 2), (5, 8), (8, 8), (6, 14), (11, 6), (8, 6), (10, 2)]
    d.polygon(bolt, fill=GOLD, outline=GOLD_D)
    return img


BATTERY_BODIES = {"basic": ((96, 98, 112), (62, 64, 78), (140, 142, 158)), "iron": BODIES["iron"], "steel": BODIES["steel"]}
GAUGE = [(214, 70, 24), (232, 100, 28), (244, 130, 34), (250, 156, 40), (252, 180, 52), (255, 204, 74), (255, 224, 110), (255, 240, 160)]


def battery_side(tier, level, seed):
    """A battery's side: a vertical gauge of 8 cells, the lowest `level` of them lit."""
    body = BATTERY_BODIES[tier]
    img = side_tile(body, seed, trim=GOLD_D)
    px = img.load()
    d = ImageDraw.Draw(img)
    d.rectangle((5, 2, 10, 13), fill=NIGHT)
    d.rectangle((5, 2, 10, 13), outline=body[1])
    for i in range(8):
        y = 11 - i
        for x in range(6, 10):
            if i < level:
                px[x, y] = GAUGE[i]
            else:
                px[x, y] = (40, 36, 58) if (i % 2) else (34, 31, 50)
    px[6, 3] = (80, 74, 110)
    return img


def battery_top(tier, seed):
    body = BATTERY_BODIES[tier]
    img = top_tile(body, seed, trim=GOLD_D)
    d = ImageDraw.Draw(img)
    d.rectangle((6, 6, 9, 9), fill=GOLD)
    d.rectangle((7, 7, 8, 8), fill=GOLD_L)
    return img


def steel_ingot():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    top = (146, 160, 190, 255)
    mid = (96, 110, 142, 255)
    dark = (60, 70, 96, 255)
    d.polygon([(3, 10), (9, 4), (14, 4), (14, 8), (8, 13), (3, 13)], fill=mid)
    d.polygon([(3, 10), (9, 4), (14, 4), (8, 10)], fill=top)
    d.polygon([(8, 10), (14, 4), (14, 8), (8, 13)], fill=dark)
    d.line([(3, 13), (8, 13)], fill=(40, 46, 66, 255))
    img.putpixel((10, 5), (220, 230, 250, 255))
    img.putpixel((9, 6), (200, 212, 238, 255))
    img.putpixel((13, 7), GOLD + (255,))
    return img


# ---- Per-block sets ----


def sets():
    """(block id, spec) for every generator; spec says how its faces are drawn."""
    out = []
    for i, tier in enumerate(("stone", "iron", "steel")):
        out.append((f"combustion_generator_{tier}", dict(body=tier, kind="fire", seed=10 + i)))
        out.append((f"lava_generator_{tier}", dict(body=tier, kind="lava", seed=20 + i)))
    out.append(("solar_panel_1", dict(body="solar", kind="solar1", seed=30)))
    out.append(("solar_panel_2", dict(body="steel", kind="solar2", seed=31)))
    out.append(("wind_turbine_1", dict(body="wind", kind="wind1", seed=40)))
    out.append(("wind_turbine_2", dict(body="steel", kind="wind2", seed=41)))
    out.append(("water_wheel", dict(body="wood", kind="wheel", seed=50)))
    out.append(("geothermal_generator", dict(body="basalt", kind="geo", seed=60)))
    out.append(("hand_crank", dict(body="crank", kind="crank", seed=70)))
    return out


ANIM = {"fire": (4, 3), "lava": (4, 5), "wind1": (8, 2), "wind2": (6, 2), "wheel": (6, 3), "geo": (8, 5), "crank": (8, 1),
        "solar1": (1, 1), "solar2": (1, 1)}


def draw_block(name, spec, out):
    body = BODIES[spec["body"]]
    kind = spec["kind"]
    seed = spec["seed"]
    n, ft = ANIM[kind]
    side = side_tile(body, seed + 100)
    top = top_tile(body, seed + 200)
    if kind in ("solar1", "solar2"):
        maker = solar_top if kind == "solar1" else solar_top2
        top, top_on = maker(False, seed), maker(True, seed)
        front = solar_front(BODIES["solar" if kind == "solar1" else "steel"], seed + 300, kind == "solar2", False)
        frames_on = [solar_front(BODIES["solar" if kind == "solar1" else "steel"], seed + 300, kind == "solar2", True)]
    else:
        top_on = top
        if kind == "fire":
            frames_on = [fire_frame(body, seed, f) for f in range(n)]
            front = cold_fire(body, seed)
        elif kind == "lava":
            frames_on = [lava_frame(body, seed, f) for f in range(n)]
            front = lava_frame(body, seed, 0, lit=False)
        elif kind in ("wind1", "wind2"):
            blades = 3 if kind == "wind1" else 4
            frames_on = [blade_frame(body, seed, f, blades, n) for f in range(n)]
            front = blade_frame(body, seed, 0, blades, n, lit=False)
        elif kind == "wheel":
            frames_on = [wheel_frame(body, seed, f, n) for f in range(n)]
            front = wheel_frame(body, seed, 0, n, lit=False)
        elif kind == "geo":
            frames_on = [geo_frame(body, seed, f, n) for f in range(n)]
            front = geo_frame(body, seed, 0, n, lit=False)
        else:
            frames_on = [crank_frame(body, seed, f, n) for f in range(n)]
            front = crank_frame(body, seed, 0, n, lit=False)
    block = out / "block"
    block.mkdir(parents=True, exist_ok=True)
    side.save(block / f"{name}_side.png")
    top.save(block / f"{name}_top.png")
    if top_on is not top:
        top_on.save(block / f"{name}_top_on.png")
    else:
        top.save(block / f"{name}_top_on.png")
    front.save(block / f"{name}_front.png")
    if len(frames_on) > 1:
        strip(frames_on).save(block / f"{name}_front_on.png")
        (block / f"{name}_front_on.png.mcmeta").write_text(json.dumps({"animation": {"frametime": ft}}), encoding="utf-8")
    else:
        frames_on[0].save(block / f"{name}_front_on.png")


def render_all(out):
    out = Path(out)
    for name, spec in sets():
        draw_block(name, spec, out)
    for i, tier in enumerate(("basic", "iron", "steel")):
        battery_top(tier, 400 + i).save(out / "block" / f"battery_{tier}_top.png")
        for level in range(9):
            battery_side(tier, level, 410 + i).save(out / "block" / f"battery_{tier}_c{level}.png")
    item = out / "item"
    item.mkdir(parents=True, exist_ok=True)
    steel_ingot().save(item / "steel_ingot.png")


if __name__ == "__main__":
    import sys

    render_all(sys.argv[1] if len(sys.argv) > 1 else "art_out")
