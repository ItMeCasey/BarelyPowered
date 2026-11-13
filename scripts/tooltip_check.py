"""Takes frames of the item tooltips of BarelyPowered and BarelyCables in a real client (no viewer mods).

Usage: python scripts/tooltip_check.py <layer> [<layer> ...]     e.g. neoforge-26.2 forge-26.2

Gives the player a generator, a battery and a cable, opens the inventory and moves the game's own mouse handler over each hotbar slot
(version-26.2 ViewerTour steps give and hover), so the tooltip is drawn. Frames go to docs/frames/tooltips/<layer>_<name>.png.
"""
import shutil
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "scripts"))
from client_tour import OPTIONS, run_isolated  # noqa: E402

SCRIPT = ("give:barelypowered:combustion_generator_iron;give:barelypowered:battery_iron;give:barelycables:energy_cable_iron;"
          "give:barelypowered:wind_turbine_2;wait:20;inv;wait:30;hover:0;wait:10;shot:tooltip_generator;hover:1;wait:10;shot:tooltip_battery;"
          "hover:2;wait:10;shot:tooltip_cable;hover:3;wait:10;shot:tooltip_wind_turbine")


def check(layer):
    from PIL import Image
    folder = ROOT / layer
    run = folder / "run"
    run.mkdir(exist_ok=True)
    (run / "options.txt").write_text(OPTIONS)
    shots = run / "screenshots"
    if shots.exists():
        shutil.rmtree(shots)
    start = time.time()
    log, timed_out = run_isolated(folder, "runClient --no-daemon -q -PdevTools -PwindowSize=1904x1041",
                                  {"BARELYPOWERED_SHOTS": "viewers", "BARELYPOWERED_VIEWER_SCRIPT": SCRIPT}, "BARELYPOWERED-SHOTS DONE")
    (run / "tooltip_check.log").write_text("\n".join(log), encoding="utf-8")
    out = ROOT / "docs" / "frames" / "tooltips"
    out.mkdir(parents=True, exist_ok=True)
    names = []
    for png in sorted(shots.glob("barelypowered_tooltip_*.png")) if shots.exists() else []:
        name = png.name[len("barelypowered_"):]
        im = Image.open(png).convert("RGB")
        if im.width > 1280:
            im = im.resize((1280, round(im.height * 1280 / im.width)), Image.LANCZOS)
        im.quantize(96).save(out / f"{layer}_{name}", optimize=True)
        names.append(name)
    status = "TOOLTIP PASS" if names and not timed_out else "TOOLTIP FAIL"
    return f"{status} {layer} ({len(names)} frames, {int(time.time() - start)}s)"


if __name__ == "__main__":
    bad = 0
    for layer in sys.argv[1:]:
        result = check(layer)
        print(result, flush=True)
        bad += result.startswith("TOOLTIP FAIL")
    sys.exit(1 if bad else 0)
