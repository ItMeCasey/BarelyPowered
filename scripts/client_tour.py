"""Runs the dev screenshot tour on a real game client for one layer and collects the screenshots.

Usage: python scripts/client_tour.py <layer> [<layer> ...]     e.g. neoforge-1.21.1 forge-26.2

Starts `gradlew runClient` with BARELYPOWERED_SHOTS set through the studio's isolated launcher (launch-isolated.ps1 in the
user's .claude\\tools folder), which runs the game on a hidden Windows desktop so its window, focus and cursor cannot reach the
person using the PC, and mutes it. Waits for the tour to finish (it quits the game itself), stops anything left over, and
prints the screenshot names. Screenshots land in <layer>/run/screenshots. The game output is read from the launcher's log.
"""
import os
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ISOLATED = r"C:\Users\casey\.claude\tools\launch-isolated.ps1"
TIMEOUT = 300
OPTIONS = (
    "soundCategory_master:0.0\nmusic:0.0\npauseOnLostFocus:false\nonboardAccessibility:false\n"
    "skipMultiplayerWarning:true\njoinedFirstServer:true\ntutorialStep:none\nfullscreen:false\nguiScale:3\nlang:en_us\n"
)


DESKTOP = "BNPTest"
STATE = Path(os.environ.get("LOCALAPPDATA", "")) / "launch-isolated"
GAME_LOG = STATE / f"{DESKTOP}.log"
PRIORITY = STATE / "priority.txt"


def ensure_priority():
    """Puts this project's one-run entry at the head of the launcher's queue file (other lines stay)."""
    entry = f"{ROOT}|once"
    lines = PRIORITY.read_text(encoding="utf-8").splitlines() if PRIORITY.exists() else []
    if not lines or lines[0].strip().lower() != entry.lower():
        PRIORITY.write_text("\n".join([entry] + lines) + "\n", encoding="utf-8")


def run_isolated(folder, gradle_args, extra_env, done_marker, timeout=TIMEOUT, wait_minutes=60):
    """Runs `gradlew <gradle_args>` in folder on the hidden desktop; returns (output lines, timed_out).

    The launcher is started with the project root as its work folder (the queue entry in priority.txt names it), waits for a client
    slot inside the launcher for up to wait_minutes, and the run itself is limited to `timeout` seconds from the moment it starts.
    extra_env is set in the command itself (the launcher runs it with cmd.exe /c), the marker ends the wait, and the launcher's scoped
    -Kill removes whatever is still running afterwards.
    """
    ensure_priority()
    state_file = STATE / f"{DESKTOP}.json"
    if state_file.exists():
        state_file.unlink()
    start = GAME_LOG.stat().st_size if GAME_LOG.exists() else 0
    sets = "".join(f'set "{k}={v}"&& ' for k, v in extra_env.items())
    command = f'cd /d "{folder}" && {sets}.\\gradlew.bat {gradle_args}'
    launcher = subprocess.Popen(["powershell", "-NoProfile", "-File", ISOLATED, "-Run", "-Desktop", DESKTOP, "-WorkDir", str(ROOT),
                                 "-RunDir", str(Path(folder) / "run"), "-Command", command, "-WaitMinutes", str(wait_minutes)],
                                stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

    def lines():
        if not GAME_LOG.exists():
            return []
        data = GAME_LOG.read_bytes()
        offset = start if len(data) >= start else 0
        return data[offset:].decode("utf-8", errors="replace").splitlines()

    waited = time.time()
    while not state_file.exists() and launcher.poll() is None and time.time() - waited < wait_minutes * 60 + 60:
        time.sleep(5)
    began = time.time()
    done = False
    while time.time() - began < timeout:
        time.sleep(2)
        if any(done_marker in line for line in lines()):
            done = True
            break
    if done:
        time.sleep(15)  # the tour quits the game itself; give it a moment before anything is stopped
    out = lines()
    subprocess.run(["powershell", "-NoProfile", "-File", ISOLATED, "-Kill", "-Desktop", DESKTOP, "-WorkDir", str(ROOT)], capture_output=True)
    if launcher.poll() is None:
        launcher.kill()
    return out, not done


def tour(layer):
    folder = ROOT / layer
    run = folder / "run"
    run.mkdir(exist_ok=True)
    (run / "options.txt").write_text(OPTIONS)
    shots = run / "screenshots"
    if shots.exists():
        shutil.rmtree(shots)
    start = time.time()
    log, timed_out = run_isolated(folder, "runClient --no-daemon -q -PdevTools", {"BARELYPOWERED_SHOTS": "1"},
                                  "BARELYPOWERED-SHOTS DONE")
    (run / "client_tour.log").write_text("\n".join(log), encoding="utf-8")
    names = sorted(p.name for p in shots.glob("*.png")) if shots.exists() else []
    done = any("BARELYPOWERED-SHOTS DONE" in line for line in log)
    errors = [l for l in log if re.search(r"barelypowered.*(Exception|Error)|ModLoadingException|Crash report saved", l, re.I)]
    status = "TOUR PASS" if done and not timed_out and not errors else "TOUR FAIL"
    detail = "" if status == "TOUR PASS" else f": done={done} timed_out={timed_out} first_error={errors[:1]}"
    return f"{status} {layer} ({len(names)} screenshots, {int(time.time() - start)}s){detail}"


if __name__ == "__main__":
    bad = 0
    for layer in sys.argv[1:]:
        result = tour(layer)
        print(result, flush=True)
        bad += result.startswith("TOUR FAIL")
    sys.exit(1 if bad else 0)
