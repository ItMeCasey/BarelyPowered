"""Draws the BarelyPowered icon as 32x32 pixel art and writes it at several sizes.

Run from the repository root: python branding/source/make_icon.py
Writes branding/icon/icon_<size>.png, branding/icon/icon.svg and barelypowered_logo.png (for the jar).

The picture: a battery with one lit segment and a bolt over it, on the studio's night tile.
"""
import math
import random
from pathlib import Path

from PIL import Image

N = 32
OUT = Path(__file__).resolve().parent.parent / "icon"

# Studio palette
BG = (24, 22, 38)
BG_EDGE = (46, 42, 70)
BG_TOP = (58, 54, 88)
GOLD = (242, 179, 61)
GOLD_HI = (255, 226, 140)
GOLD_LO = (178, 116, 32)
# Orange
ORANGE = [(250, 140, 26), (240, 124, 18), (255, 156, 40)]
ORANGE_LO = (196, 90, 12)
ORANGE_HI = (255, 196, 96)
PITH = (255, 236, 176)
PITH_LO = (240, 206, 120)
SEG = (250, 168, 52)
SEG_LINE = (222, 120, 24)
LEAF = (98, 176, 64)
LEAF_LO = (64, 130, 44)
STEM = (96, 62, 34)

random.seed(11)
px = [[None] * N for _ in range(N)]


def put(x, y, c):
    if 0 <= x < N and 0 <= y < N:
        px[y][x] = c


# Background tile with stepped rounded corners.
R = 4
for y in range(N):
    for x in range(N):
        cx = min(max(x, R - 0.5), N - R - 0.5)
        cy = min(max(y, R - 0.5), N - R - 0.5)
        if math.hypot(x - cx, y - cy) <= R:
            px[y][x] = BG
for y in range(N):
    for x in range(N):
        if px[y][x] is None:
            continue
        edge = any(
            not (0 <= x + dx < N and 0 <= y + dy < N) or px[y + dy][x + dx] is None
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
        )
        if edge:
            px[y][x] = BG_TOP if y < N // 2 else BG_EDGE

# A battery with a single lit segment ("barely powered") and a bolt above it.
def rect(x0, y0, x1, y1, c):
    for yy in range(y0, y1 + 1):
        for xx in range(x0, x1 + 1):
            put(xx, yy, c)


# Battery body: gold frame, dark inside, bevel.
rect(3, 15, 26, 26, GOLD_LO)
rect(4, 15, 26, 15, GOLD_HI)
rect(3, 15, 3, 26, GOLD)
rect(4, 16, 25, 25, GOLD)
rect(5, 17, 24, 24, BG)
rect(5, 17, 24, 17, (12, 11, 22))
# Terminal cap.
rect(27, 18, 29, 23, GOLD_LO)
rect(27, 18, 29, 18, GOLD)
rect(27, 18, 27, 23, GOLD)
# Four slots: only the first has charge.
slots = [(6, 9), (11, 14), (16, 19), (21, 24)]
for i, (x0, x1) in enumerate(slots):
    if i == 0:
        rect(x0, 18, x1, 23, (250, 150, 30))
        rect(x0, 18, x1, 19, GOLD_HI)
        rect(x0, 23, x1, 23, GOLD_LO)
    else:
        rect(x0, 18, x1, 23, BG_EDGE)
        rect(x0, 18, x1, 18, (64, 58, 94))

# Bolt above the battery.
bolt = [
    "....XXX",
    "...XXX.",
    "..XXX..",
    ".XXXXXX",
    "..XXXX.",
    "...XX..",
    "..XX...",
    ".XX....",
]
for j, row in enumerate(bolt):
    for i, ch in enumerate(row):
        if ch != "X":
            continue
        left = i == 0 or row[i - 1] == "."
        right = i == len(row) - 1 or row[i + 1] == "."
        put(11 + i, 4 + j, GOLD_HI if left else GOLD_LO if right else GOLD)

# Small sparks.
for x, y, c in ((24, 6, GOLD_HI), (23, 6, GOLD), (25, 6, GOLD), (24, 5, GOLD), (24, 7, GOLD), (7, 9, GOLD_HI), (6, 9, GOLD)):
    put(x, y, c)

img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
for y in range(N):
    for x in range(N):
        if px[y][x] is not None:
            img.putpixel((x, y), px[y][x] + (255,))

OUT.mkdir(parents=True, exist_ok=True)
for size in (32, 64, 128, 256, 512, 1024):
    img.resize((size, size), Image.NEAREST).save(OUT / f"icon_{size}.png", optimize=True)
img.resize((512, 512), Image.NEAREST).save(OUT / "barelypowered_logo.png", optimize=True)

rects = []
for y in range(N):
    for x in range(N):
        c = px[y][x]
        if c is not None:
            rects.append(f'<rect x="{x}" y="{y}" width="1.02" height="1.02" fill="#{c[0]:02x}{c[1]:02x}{c[2]:02x}"/>')
(OUT / "icon.svg").write_text(
    f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {N} {N}" shape-rendering="crispEdges">'
    + "".join(rects)
    + "</svg>\n",
    encoding="utf-8",
)
print("wrote", OUT)
