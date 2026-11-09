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
ISOLATED_LOG = Path(os.environ.get("LOCALAPPDATA", "")) / "launch-isolated" / "MCTest.log"
TIMEOUT = 300
OPTIONS = (
    "soundCategory_master:0.0\nmusic:0.0\npauseOnLostFocus:false\nonboardAccessibility:false\n"
    "skipMultiplayerWarning:true\njoinedFirstServer:true\ntutorialStep:none\nfullscreen:false\nguiScale:3\nlang:en_us\n"
)


def run_isolated(folder, gradle_args, extra_env, done_marker, timeout=TIMEOUT):
    """Runs `gradlew <gradle_args>` in folder on the hidden desktop; returns (output lines, timed_out).

    extra_env is set in the command itself (the launcher runs it with cmd.exe /c), the marker ends the wait, and the launcher's
    -Kill removes whatever is still running afterwards.
    """
    start = ISOLATED_LOG.stat().st_size if ISOLATED_LOG.exists() else 0
    sets = "".join(f'set "{k}={v}"&& ' for k, v in extra_env.items())
    command = f".\\gradlew.bat {gradle_args}"
    command = sets + command
    subprocess.run(["powershell", "-NoProfile", "-File", ISOLATED, "-Run", "-WorkDir", str(folder), "-Command", command],
                   check=True, capture_output=True)

    def lines():
        if not ISOLATED_LOG.exists():
            return []
        data = ISOLATED_LOG.read_bytes()
        offset = start if len(data) >= start else 0
        return data[offset:].decode("utf-8", errors="replace").splitlines()

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
    subprocess.run(["powershell", "-NoProfile", "-File", ISOLATED, "-Kill"], capture_output=True)
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
