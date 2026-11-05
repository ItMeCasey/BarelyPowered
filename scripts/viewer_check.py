"""Runs a real game client with optional mods (item and recipe viewers, Jade) and takes screenshots of them showing BarelyPowered.

Usage: python scripts/viewer_check.py <layer> <mods> [--script "inv;wait:40;shot:name"] [--scene]
       e.g. python scripts/viewer_check.py neoforge-1.21.1 jade,jei --scene

<mods> is a comma list of jade, jei, rei, emi, bei (what each layer has is in the layer's build.gradle). The client starts with
BARELYPOWERED_SHOTS=viewers, so the dev tour runs ViewerTour instead of the generator tour: --scene builds a generator, battery,
cable and lava generator and looks at each (for Jade); --script is the list of steps (see ViewerTour). The BarelyCables jar must be
in <layer>/run/mods for the cable frames. The window is moved to DISPLAY6; screenshots land in <layer>/run/screenshots and are
copied to docs/frames/viewers/<layer>/ with the mods' names in the file names.
"""
import os
import re
import shutil
import subprocess
import sys
import threading
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "scripts"))
from client_tour import OPTIONS, PLACE, TIMEOUT  # noqa: E402


def check(layer, mods, script, scene):
    # AeroFlux is not part of this check and its recipe viewer warnings (EMI dev mode) would sit in every frame, so its jar waits aside.
    mods_dir = ROOT / layer / "run" / "mods"
    aside = ROOT / layer / "run" / "mods.aeroflux"
    moved = []
    if mods_dir.exists():
        aside.mkdir(exist_ok=True)
        # SMWTI draws its own overlay over the same block, so it also waits aside when Jade is what is being checked.
        patterns = ["aeroflux*.jar"] + (["smwti*.jar"] if "jade" in mods else [])
        for pattern in patterns:
            for jar in mods_dir.glob(pattern):
                shutil.move(str(jar), str(aside / jar.name))
                moved.append(jar.name)
    try:
        return _check(layer, mods, script, scene)
    finally:
        for name in moved:
            shutil.move(str(aside / name), str(mods_dir / name))


def _check(layer, mods, script, scene):
    folder = ROOT / layer
    run = folder / "run"
    run.mkdir(exist_ok=True)
    (run / "options.txt").write_text(OPTIONS)
    shots = run / "screenshots"
    if shots.exists():
        shutil.rmtree(shots)
    env = dict(os.environ, BARELYPOWERED_SHOTS="viewers", BARELYPOWERED_VIEWER_SCRIPT=script)
    if scene:
        env["BARELYPOWERED_VIEWER_SCENE"] = "1"
    log = []
    proc = subprocess.Popen(["cmd", "/c", str(folder / "gradlew.bat"), "runClient", "--no-daemon", "-q", "-PdevTools", f"-Pviewers={mods}", "-PwindowSize=1904x1041"],
                            cwd=folder, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, errors="replace")

    def pump():
        for line in proc.stdout:
            log.append(line.rstrip())

    threading.Thread(target=pump, daemon=True).start()
    def place():
        # Some versions recreate or resize the window while loading, so the window is moved to DISPLAY6 again every few seconds.
        while proc.poll() is None:
            subprocess.run(["powershell", "-NoProfile", "-File", PLACE, "-ProcessName", "java*", "-Match", "Minecraft", "-Wait", "5"],
                           capture_output=True, text=True)
            time.sleep(4)

    threading.Thread(target=place, daemon=True).start()
    start = time.time()
    while proc.poll() is None and time.time() - start < TIMEOUT:
        time.sleep(2)
    timed_out = proc.poll() is None
    if timed_out:
        subprocess.run(["taskkill", "/T", "/F", "/PID", str(proc.pid)], capture_output=True)
    tag = mods.replace(",", "+")
    (run / f"viewer_check_{tag}.log").write_text("\n".join(log), encoding="utf-8")
    names = sorted(p.name for p in shots.glob("*.png")) if shots.exists() else []
    out = ROOT / "docs" / "frames" / "viewers" / layer
    out.mkdir(parents=True, exist_ok=True)
    for n in names:
        shutil.copy2(shots / n, out / n.replace("barelypowered_", f"{tag}_", 1))
    done = any("BARELYPOWERED-SHOTS DONE" in line for line in log)
    errors = [l for l in log if re.search(r"(barelypowered|barelycables|jade|jei|emi|roughlyenoughitems|bei).*(Exception|ERROR)|ModLoadingException|Crash report saved|Failed to load", l, re.I) and "[mixin/]" not in l and "Untranslated tag" not in l]
    status = "VIEWER PASS" if done and not timed_out and not errors else "VIEWER FAIL"
    detail = "" if status == "VIEWER PASS" else f": done={done} timed_out={timed_out} first_error={errors[:2]}"
    return f"{status} {layer} [{mods}] ({len(names)} screenshots, {int(time.time() - start)}s){detail}"


if __name__ == "__main__":
    args = sys.argv[1:]
    scene = "--scene" in args
    script = "inv;wait:40;shot:inventory"
    if "--script" in args:
        script = args[args.index("--script") + 1]
    positional = [a for i, a in enumerate(args) if not a.startswith("--") and (i == 0 or args[i - 1] != "--script")]
    result = check(positional[0], positional[1], script, scene)
    print(result, flush=True)
    sys.exit(1 if result.startswith("VIEWER FAIL") else 0)
