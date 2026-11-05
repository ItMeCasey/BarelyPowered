"""Builds a contact sheet from tour screenshots: each scene's status frame, cropped around the block and the action bar.

Usage: python scripts/crop_shots.py <layer> <out.png> [suffix]    (suffix defaults to status)
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw

layer = sys.argv[1]
out = sys.argv[2]
suffix = sys.argv[3] if len(sys.argv) > 3 else "status"
shots = sorted((Path(__file__).resolve().parent.parent / layer / "run" / "screenshots").glob(f"barelypowered_*_{suffix}.png"))
cols = 3
tiles = []
for f in shots:
    im = Image.open(f).convert("RGB")
    w, h = im.size
    crop = im.crop((int(w * 0.22), int(h * 0.30), int(w * 0.78), int(h * 0.86)))
    crop = crop.resize((560, 560 * crop.height // crop.width))
    d = ImageDraw.Draw(crop)
    d.rectangle((0, 0, 330, 14), fill=(0, 0, 0))
    d.text((3, 1), f.stem.replace("barelypowered_", "").replace("_" + suffix, ""), fill=(255, 255, 255))
    tiles.append(crop)
rows = (len(tiles) + cols - 1) // cols
th = max(t.height for t in tiles)
sheet = Image.new("RGB", (cols * 560, rows * th), (30, 30, 30))
for i, t in enumerate(tiles):
    sheet.paste(t, ((i % cols) * 560, (i // cols) * th))
sheet.save(out)
print(out, sheet.size, len(tiles))
