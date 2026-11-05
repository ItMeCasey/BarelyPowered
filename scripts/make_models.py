"""Describes the solar panel and wind turbine models and paints their textures.

Run from the repo root: python scripts/make_models.py <outdir>
Writes into <outdir>:
  <name>.spec.json   cubes in Blockbench terms, placed by scripts/send_models.py through the Blockbench MCP server
  *.png   the game textures (the palette textures)

Models: solar_panel_1/2 (+ _on); wind_turbine_N (base block: foot and lower mast), wind_tower_N (mast segment),
wind_head_N and wind_head_N_on (nacelle and hub; the rotor blades are drawn by the client renderer, version-*/client/WindHeadRenderer), wind_turbine_N_item (a compact
whole turbine for the inventory icon).

UV units follow Blockbench's default 16x16 space, so one UV unit is two pixels of a 32x32 palette texture. Palette swatch i sits
at uv [2*(i%4), 2*(i//4)] and is 2x2 units; a 16x16 pixel region at uv [8,0]-[16,8] holds the solar cell grid.

Tower heights in blocks: tier 1 is 3 (base, one segment, head), tier 2 is 5 (base, three segments, head). Keep in step with
Tiers.windHeight in common.
"""
import json
import sys
from pathlib import Path

from PIL import Image, ImageDraw

SWATCH = {"steel": 0, "steel_mid": 1, "steel_dark": 2, "housing": 3, "accent": 4, "lamp": 5, "blade": 6, "blade_edge": 7,
          "hub": 8, "rubber": 9}


def swatch_uv(name):
    i = SWATCH[name]
    u, v = 2 * (i % 4), 2 * (i // 4)
    return [u, v, u + 2, v + 2]


PANEL_UV = [8, 0, 16, 8]


def palette(tier, lit):
    accent = (196, 120, 64) if tier == 1 else (232, 190, 72)
    steel = (176, 182, 190) if tier == 1 else (150, 158, 170)
    return {
        "steel": steel,
        "steel_mid": tuple(int(c * 0.72) for c in steel),
        "steel_dark": tuple(int(c * 0.45) for c in steel),
        "housing": (46, 50, 58) if tier == 1 else (34, 36, 46),
        "accent": accent,
        "lamp": (255, 214, 96) if lit else (84, 58, 50),
        "blade": (236, 238, 242) if tier == 1 else (216, 222, 236),
        "blade_edge": (186, 192, 202) if tier == 1 else (150, 160, 184),
        "hub": (110, 118, 130) if tier == 1 else (86, 92, 108),
        "rubber": (28, 28, 32),
    }


def paint(tier, lit):
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for name, rgb in palette(tier, lit).items():
        i = SWATCH[name]
        x, y = 4 * (i % 4), 4 * (i // 4)
        d.rectangle([x, y, x + 3, y + 3], fill=rgb + (255,))
        dark = tuple(int(c * 0.8) for c in rgb) + (255,)
        light = tuple(min(255, int(c * 1.12)) for c in rgb) + (255,)
        d.line([x, y + 3, x + 3, y + 3], fill=dark)
        d.line([x + 3, y, x + 3, y + 3], fill=dark)
        d.line([x, y, x + 2, y], fill=light)
    if tier == 1:
        base, line, hi = ((30, 66, 140), (120, 150, 200), (70, 120, 200)) if lit else ((24, 52, 112), (104, 132, 180), (48, 92, 168))
    else:
        base, line, hi = ((46, 40, 120), (232, 190, 72), (96, 86, 190)) if lit else ((30, 26, 84), (200, 160, 60), (62, 54, 140))
    x0, y0 = 16, 0
    d.rectangle([x0, y0, x0 + 15, y0 + 15], fill=base + (255,))
    for gx in range(2):
        for gy in range(2):
            cx, cy = x0 + gx * 8, y0 + gy * 8
            d.rectangle([cx + 1, cy + 1, cx + 6, cy + 6], fill=hi + (255,))
            d.rectangle([cx + 2, cy + 2, cx + 6, cy + 6], fill=base + (255,))
            d.line([cx + 1, cy + 1, cx + 6, cy + 1], fill=hi + (255,))
    for k in (0, 8):
        d.line([x0, y0 + k, x0 + 15, y0 + k], fill=line + (255,))
        d.line([x0 + k, y0, x0 + k, y0 + 15], fill=line + (255,))
    d.line([x0, y0 + 15, x0 + 15, y0 + 15], fill=line + (255,))
    d.line([x0 + 15, y0, x0 + 15, y0 + 15], fill=line + (255,))
    return img


def cube(name, a, b, mat, rotation=None, origin=None, top=None, light=0, tex=None, faces=None):
    e = {"name": name, "from": a, "to": b, "mat": mat}
    if top:
        e["top"] = top
    if rotation:
        e["rotation"] = rotation
        e["origin"] = origin
    if light:
        e["light_emission"] = light
    if tex:
        e["tex"] = tex
    if faces:
        e["faces"] = faces
    return e


def tilted(name, a, b, mat, tilt, origin, top=None):
    return cube(name, a, b, mat, rotation=[tilt, 0, 0], origin=origin, top=top)


def solar(tier, lit):
    """Tier 1: a weighted foot, a dark housing with a lamp, a mast and a tilted panel.
    Tier 2: taller mast, a wide three part panel (about 40 units across) and a braced frame; it needs the whole 3x2 block model space."""
    if tier == 1:
        return [
            cube("foot", [1, 0, 1], [15, 2, 15], "steel_mid"),
            cube("housing", [3, 2, 3], [13, 6, 13], "housing"),
            cube("trim", [2.5, 4, 2.5], [13.5, 5, 13.5], "accent"),
            cube("lamp", [6, 3, 2.5], [10, 4.5, 3], "lamp", light=10 if lit else 0),
            cube("mast", [7, 6, 7], [9, 10, 9], "steel_dark"),
            cube("strut", [7.5, 7, 9], [8.5, 8, 14], "steel_dark", rotation=[-22.5, 0, 0], origin=[8, 7, 9]),
            tilted("panel_frame", [0, 9.5, 0.5], [16, 11, 15.5], "steel", 22.5, [8, 10, 8]),
            tilted("panel_cells", [0.75, 10.9, 1.25], [15.25, 11.3, 14.75], "steel", 22.5, [8, 10, 8], top="panel"),
        ]
    els = [
        cube("foot", [0, 0, 0], [16, 3, 16], "steel_mid"),
        cube("foot_trim", [-0.5, 2, -0.5], [16.5, 3, 16.5], "accent"),
        cube("housing", [2.5, 3, 2.5], [13.5, 9, 13.5], "housing"),
        cube("trim", [2, 6, 2], [14, 7.5, 14], "accent"),
        cube("lamp", [5.5, 4, 2], [10.5, 5.5, 2.5], "lamp", light=10 if lit else 0),
        cube("mast_low", [6.5, 9, 6.5], [9.5, 15, 9.5], "steel"),
        cube("mast_high", [7, 15, 7], [9, 22, 9], "steel_dark"),
        cube("collar", [6, 14.5, 6], [10, 16, 10], "accent"),
        cube("brace_back", [7.5, 14, 9], [8.5, 15, 15], "steel_dark", rotation=[-22.5, 0, 0], origin=[8, 14, 9]),
        cube("brace_front", [7.5, 14, 1], [8.5, 15, 7], "steel_dark", rotation=[22.5, 0, 0], origin=[8, 14, 7]),
        cube("rail", [-12, 21.2, 7.5], [28, 22.2, 8.5], "steel_dark", rotation=[22.5, 0, 0], origin=[8, 22, 8]),
    ]
    # three panels side by side, each with its own frame and cell grid, tilted together about the same axis through the mast top
    for i, (x0, x1) in enumerate(((-12, 0.5), (1.75, 14.25), (15.5, 28))):
        els.append(tilted(f"panel_{i + 1}_frame", [x0, 21.5, -3], [x1, 23, 19], "steel", 22.5, [8, 22, 8]))
        els.append(tilted(f"panel_{i + 1}_cells", [x0 + 0.6, 22.9, -2.4], [x1 - 0.6, 23.3, 18.4], "steel", 22.5, [8, 22, 8],
                          top="panel"))
    return els


def blade(name, angle_from_up, center, length, width, mat="blade"):
    """A 3D blade for the inventory icon: points angle_from_up degrees clockwise from up (seen from the front), in 22.5 degree steps."""
    cx, cy, cz = center
    a = angle_from_up % 360
    if a <= 45 or a >= 315:
        rot = a if a <= 45 else a - 360
        return cube(name, [cx - width / 2, cy, cz], [cx + width / 2, cy + length, cz + 0.8], mat, rotation=[0, 0, -rot], origin=[cx, cy, cz])
    if 135 <= a <= 225:
        rot = a - 180
        return cube(name, [cx - width / 2, cy - length, cz], [cx + width / 2, cy, cz + 0.8], mat, rotation=[0, 0, -rot], origin=[cx, cy, cz])
    if 45 < a < 135:
        rot = a - 90
        return cube(name, [cx, cy - width / 2, cz], [cx + length, cy + width / 2, cz + 0.8], mat, rotation=[0, 0, -rot], origin=[cx, cy, cz])
    rot = a - 270
    return cube(name, [cx - length, cy - width / 2, cz], [cx, cy + width / 2, cz + 0.8], mat, rotation=[0, 0, -rot], origin=[cx, cy, cz])


def wind_item(tier):
    """A compact whole turbine for the inventory, hand and ground icon (one model, 32 units tall)."""
    t2 = tier == 2
    hub = [8, 18, 3.4]
    els = [
        cube("foot", [3, 0, 3], [13, 3, 13], "steel_mid"),
        cube("base_trim", [2.5, 2, 2.5], [13.5, 3, 13.5], "accent"),
        cube("tower_low", [6, 3, 6], [10, 9, 10], "steel"),
        cube("tower_high", [7, 9, 7], [9, 17, 9], "steel"),
        cube("collar", [6.5, 16, 6.5], [9.5, 17, 9.5], "steel_dark"),
        cube("nacelle", [5.5, 16.5, 4], [10.5, 20.5, 13], "housing"),
        cube("nacelle_trim", [5, 18, 4], [11, 19, 13], "accent" if t2 else "steel_mid"),
        cube("tail", [7, 17.5, 13], [9, 19.5, 15], "steel_dark"),
        cube("hub", [6.6, 16.6, 2.6], [9.4, 19.4, 4.4], "hub"),
        cube("spinner", [7.3, 17.3, 1.6], [8.7, 18.7, 2.6], "steel"),
    ]
    for i, ang in enumerate([0, 112.5, 247.5]):
        els.append(blade(f"blade_{i + 1}", ang, hub, 10 if t2 else 9, 2.4 if t2 else 2.0, "blade"))
        els.append(blade(f"blade_{i + 1}_edge", ang, [hub[0], hub[1], hub[2] - 0.2], 10 if t2 else 9, 0.6, "blade_edge"))
    return els


def wind_base(tier):
    """The block the player places: a foot and the first stretch of mast (the energy block entity lives here)."""
    return [
        cube("foot", [2.5, 0, 2.5], [13.5, 3, 13.5], "steel_mid"),
        cube("base_trim", [2, 2, 2], [14, 3.5, 14], "accent"),
        cube("base_collar", [5.5, 3, 5.5], [10.5, 5, 10.5], "steel_dark"),
        cube("mast", [6, 5, 6], [10, 16, 10], "steel"),
        cube("ring", [5.5, 10, 5.5], [10.5, 11, 10.5], "steel_dark"),
    ]


def wind_segment(tier):
    return [
        cube("mast", [6, 0, 6], [10, 16, 10], "steel"),
        cube("ring_low", [5.5, 3, 5.5], [10.5, 4, 10.5], "steel_dark"),
        cube("ring_high", [5.5, 11, 5.5], [10.5, 12, 10.5], "steel_dark"),
        cube("band", [5.75, 7.5, 5.75], [10.25, 8.5, 10.25], "accent"),
    ]


def wind_head(tier, lit):
    """The top block: mast end, nacelle and hub. Faces north. The blades are drawn by WindHeadRenderer, which turns them."""
    return [
        cube("mast", [6, 0, 6], [10, 4, 10], "steel"),
        cube("collar", [5, 3, 5], [11, 5, 11], "steel_dark"),
        cube("nacelle", [4, 5, 3], [12, 13, 15], "housing"),
        cube("nacelle_trim", [3.5, 8, 3], [12.5, 10, 15], "accent" if tier == 2 else "steel_mid"),
        cube("tail", [6.5, 7, 15], [9.5, 11, 18], "steel_dark"),
        cube("lamp", [6.5, 13, 8], [9.5, 13.6, 11], "lamp", light=10 if lit else 0),
        cube("hub", [5.5, 7.5, 1], [10.5, 12.5, 3], "hub"),
        cube("spinner", [6.8, 8.8, -0.4], [9.2, 11.2, 1], "steel"),
    ]


def specs():
    out = []
    for tier in (1, 2):
        for lit in (False, True):
            out.append({"name": f"solar_panel_{tier}" + ("_on" if lit else ""), "main": f"solar_panel_{tier}" + ("_on" if lit else ""),
                        "elements": solar(tier, lit)})
        pal = f"wind_turbine_{tier}"
        out.append({"name": f"wind_turbine_{tier}", "main": pal, "elements": wind_base(tier)})
        out.append({"name": f"wind_tower_{tier}", "main": pal, "elements": wind_segment(tier)})
        out.append({"name": f"wind_head_{tier}", "main": pal, "elements": wind_head(tier, False)})
        out.append({"name": f"wind_head_{tier}_on", "main": f"{pal}_on", "elements": wind_head(tier, True)})
        out.append({"name": f"wind_turbine_{tier}_item", "main": pal, "elements": wind_item(tier)})
    return out


def main(out):
    out = Path(out)
    out.mkdir(parents=True, exist_ok=True)
    for tier in (1, 2):
        for lit in (False, True):
            sfx = "_on" if lit else ""
            paint(tier, lit).save(out / f"solar_panel_{tier}{sfx}.png")
            paint(tier, lit).save(out / f"wind_turbine_{tier}{sfx}.png")
    for spec in specs():
        (out / f"{spec['name']}.spec.json").write_text(json.dumps(spec, indent=1), encoding="utf-8")
        print("wrote", spec["name"])


if __name__ == "__main__":
    main(sys.argv[1])
