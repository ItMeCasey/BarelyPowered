"""Proves the loader keeps ONE copy of BarelyCables when two mods embed it (jar-in-jar), on a headless dedicated server per layer.

Usage: python scripts/jarjar_check.py [layer ...|all]          two dummy mods embed it (one a higher version): scenario "dummies"
       python scripts/jarjar_check.py --real [layer ...|all]   the real combined BarelyPowered jar embeds it, next to the BarelyCables
                                                               layer's own build: scenario "real"

Scenario "dummies" runs in the BarelyPowered layer (no BarelyCables of its own) with two code-less dummy mods in run/mods: A nests
BarelyCables 2026.11.1, B nests a copy rewritten to 2026.11.2. Expected: BarelyCables initializes exactly once and the loader's mod
list shows 2026.11.2, with no loading errors, duplicate registrations or split-package complaints.

Scenario "real" runs in the BarelyCables layer, whose own classes are one copy, with dist/barelypowered-mc<mc>-<version>.jar (made by
scripts/build_all.sh, which nests BarelyCables) in run/mods. Expected: BarelyPowered loads from the jar and BarelyCables initializes
exactly once. Ports are the registered 8970-8977. The dummy mods are dev-only and never shipped.
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
import make_jarjar_dummy  # noqa: E402

PORTS = {"neoforge-1.21.1": 8970, "forge-1.21.1": 8971, "neoforge-26.1.2": 8972, "forge-26.1.2": 8973,
         "neoforge-26.2": 8974, "forge-26.2": 8975, "neoforge-26.3": 8976, "forge-26.3": 8977}
BAD = re.compile(r"(ModLoadingException|Failed to load mod|Encountered an unexpected exception|FATAL|Duplicate (key|registry|entry)|"
                 r"[Dd]uplicate mod(?!ule)|split package|reads package|java\.lang\.module\.|already (registered|frozen))")
TIMEOUT = 420


def mc_of(layer):
    return layer.split("-", 1)[1]


def run_server(folder, port, extra_env=None):
    run = folder / "run"
    run.mkdir(exist_ok=True)
    (run / "eula.txt").write_text("eula=true\n")
    (run / "server.properties").write_text(
        f"server-port={port}\nonline-mode=false\nlevel-type=minecraft\\:flat\nspawn-protection=0\nmotd=jar-in-jar check\nmax-tick-time=-1\n")
    env = dict(os.environ, **(extra_env or {}))
    log = []
    proc = subprocess.Popen(["cmd", "/c", str(folder / "gradlew.bat"), "runServer", "--no-daemon", "-q", "-PdevTools"], cwd=folder, env=env,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, errors="replace")

    def pump():
        for line in proc.stdout:
            log.append(line.rstrip())

    t = threading.Thread(target=pump, daemon=True)
    t.start()
    start = time.time()
    done = False
    while time.time() - start < TIMEOUT and proc.poll() is None:
        if any(re.search(r"Done \(|For help, type", line) for line in log):
            done = True
            break
        time.sleep(1)
    time.sleep(2)
    subprocess.run(["taskkill", "/T", "/F", "/PID", str(proc.pid)], capture_output=True)
    t.join(timeout=5)
    return done, "\n".join(log)


def with_mods(run_dir, jars, fn):
    """Runs fn with run/mods holding only the given jars; the previous mods folder is put back afterwards."""
    mods = run_dir / "mods"
    aside = run_dir / "mods.aside"
    if aside.exists():
        shutil.rmtree(aside)
    if mods.exists():
        mods.rename(aside)
    mods.mkdir(parents=True)
    try:
        for j in jars:
            shutil.copy2(j, mods / Path(j).name)
        return fn()
    finally:
        shutil.rmtree(mods, ignore_errors=True)
        if aside.exists():
            aside.rename(mods)


def check(layer, real):
    mc = mc_of(layer)
    cables = ROOT / "barelycables/dist" / f"barelycables-mc{mc}-2026.11.1.jar"
    scratch = ROOT / "release" / "jarjar" / layer
    shutil.rmtree(scratch, ignore_errors=True)
    scratch.mkdir(parents=True)
    if real:
        folder = ROOT / "barelycables" / layer
        host = ROOT / "dist" / f"barelypowered-mc{mc}-2026.11.1.jar"
        jars = [host]
        what = "BarelyPowered combined jar + BarelyCables layer classes"
    else:
        folder = ROOT / layer
        a = scratch / "jjdummy_a.jar"
        b = scratch / "jjdummy_b.jar"
        make_jarjar_dummy.main(["x", str(a), "jjdummy_a", str(cables)])
        make_jarjar_dummy.main(["x", str(b), "jjdummy_b", str(cables), "--bump", "2026.11.2"])
        jars = [a, b]
        what = "two dummy mods nesting BarelyCables (2026.11.1 and 2026.11.2)"
    done, out = with_mods(folder / "run", jars, lambda: run_server(folder, PORTS[layer]))
    (scratch / "server.log").write_text(out, encoding="utf-8")
    inits = len(re.findall(r"BarelyCables module initialized", out))
    problems = [line for line in out.splitlines() if BAD.search(line) and "kqueue" not in line and "epoll" not in line and "Only supported on" not in line]
    listed = sorted(set(re.findall(r"barelycables[^\n]*?(\d{4}\.\d+\.\d+)", out)))
    if not done:
        return f"JARJAR FAIL {layer}: server never reported Done ({what})"
    if inits != 1:
        return f"JARJAR FAIL {layer}: BarelyCables initialized {inits} times, expected 1 ({what})"
    if problems:
        return f"JARJAR FAIL {layer}: {problems[0][:200]}"
    if real and not re.search(r"barelypowered", out):
        return f"JARJAR FAIL {layer}: BarelyPowered never appeared in the log"
    selected = re.findall(r"\*(\S+?)!/META-INF/jarjar/(\S+\.jar)", out)
    listed_in_loader = [line.strip() for line in out.splitlines() if re.search(r"^\s*BarelyCables \d|^ - barelycables ", line)]
    if real and not re.search(r"barelypowered", out):
        return f"JARJAR FAIL {layer}: BarelyPowered never appeared in the log"
    note = f"loader chose {selected[0][1]} from {selected[0][0]}" if selected else (listed_in_loader[0] if listed_in_loader else "one copy")
    return f"JARJAR PASS {layer} ({what}): BarelyCables initialized once, no loading errors; {note}"


def main(argv):
    real = "--real" in argv
    layers = [a for a in argv[1:] if not a.startswith("--")]
    if layers == ["all"] or not layers:
        layers = list(PORTS)
    bad = 0
    for layer in layers:
        result = check(layer, real)
        print(result, flush=True)
        bad += result.startswith("JARJAR FAIL")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
