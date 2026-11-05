"""Builds each model of scripts/make_models.py in Blockbench (through its MCP plugin) and saves the results.

Usage: python scripts/send_models.py <specdir> <artdir> <modelsdir> <texturesdir> [name ...]

For every <name>.spec.json in specdir it creates a Java block project, loads <name>.png, places the cubes, saves a screenshot
and the .bbmodel project to artdir, and writes the exported model JSON (with the texture reference pointing at the mod) to
modelsdir. The PNG is copied to texturesdir. Blockbench must be running with the MCP plugin loaded (http://localhost:3000/bb-mcp).
"""
import base64
import json
import re
import shutil
import sys
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from make_models import PANEL_UV, swatch_uv  # noqa: E402

ENDPOINT = "http://localhost:3000/bb-mcp"
MODID = "barelypowered"
FACES = ["north", "south", "east", "west", "up", "down"]


class Mcp:
    def __init__(self):
        self.sid = None
        init = self._post({"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {
            "protocolVersion": "2025-03-26", "capabilities": {}, "clientInfo": {"name": "bp", "version": "1"}}})
        self._post({"jsonrpc": "2.0", "method": "notifications/initialized"})
        self.n = 2

    def _post(self, body):
        headers = {"Content-Type": "application/json", "Accept": "application/json, text/event-stream"}
        if self.sid:
            headers["Mcp-Session-Id"] = self.sid
        req = urllib.request.Request(ENDPOINT, json.dumps(body).encode(), headers)
        with urllib.request.urlopen(req, timeout=60) as r:
            self.sid = self.sid or r.headers.get("mcp-session-id")
            text = r.read().decode()
        line = next((l for l in text.splitlines() if l.startswith("data:")), None)
        return json.loads(line[5:]) if line else (json.loads(text) if text.strip() else None)

    def call(self, tool, args):
        self.n += 1
        res = self._post({"jsonrpc": "2.0", "id": self.n, "method": "tools/call", "params": {"name": tool, "arguments": args}})
        out = res.get("result") or {}
        if out.get("isError") or "error" in res:
            raise RuntimeError(f"{tool} failed: {json.dumps(res)[:600]}")
        return out.get("content", [])


def faces_for(e):
    if e.get("faces"):
        return e["faces"]
    uv = swatch_uv(e["mat"])
    faces = [{"face": f, "uv": uv} for f in FACES]
    if e.get("top") == "panel":
        faces[FACES.index("up")] = {"face": "up", "uv": PANEL_UV}
    return faces


def build(mcp, spec_path, art, models, textures):
    spec = json.loads(spec_path.read_text())
    name = spec["name"]
    main_tex = spec["main"]
    rotor_tex = spec.get("rotor")
    folder = spec_path.resolve().parent
    made = mcp.call("create_project", {"name": name, "format": "java_block"})
    uuid = re.search(r"UUID: ([0-9a-f-]{36})", made[0].get("text", "")).group(1)
    mcp.call("create_texture", {"name": main_tex, "data": str(folder / (main_tex + ".png"))})
    if rotor_tex:
        mcp.call("create_texture", {"name": rotor_tex, "data": str(folder / f"bb_{spec.get('bb_rotor', rotor_tex)}.png")})
    for e in spec["elements"]:
        el = {"name": e["name"], "from": e["from"], "to": e["to"]}
        if "rotation" in e:
            el["rotation"] = e["rotation"]
            el["origin"] = e["origin"]
        if e.get("light_emission"):
            el["light_emission"] = e["light_emission"]
        mcp.call("place_cube", {"elements": [el], "texture": rotor_tex if e.get("tex") == "rotor" else main_tex, "faces": faces_for(e)})
    for c in mcp.call("capture_screenshot", {"project": uuid, "max_size": 900}):
        if c["type"] == "image":
            (art / f"{name}.png").write_bytes(base64.b64decode(c["data"]))
    for c in mcp.call("export_model", {"codec_id": "project", "max_content_length": 2000000}):
        if c["type"] == "text":
            try:
                (art / f"{name}.bbmodel").write_text(json.loads(c["text"])["content"], encoding="utf-8")
            except (ValueError, KeyError, TypeError):
                (art / f"{name}.bbmodel.txt").write_text(c["text"], encoding="utf-8")
    for c in mcp.call("export_model", {"max_content_length": 2000000}):
        if c["type"] == "text":
            try:
                model = json.loads(json.loads(c["text"])["content"])
            except (ValueError, KeyError, TypeError):
                (art / f"{name}.export.txt").write_text(c["text"], encoding="utf-8")
                continue
            model.pop("format_version", None)
            model.pop("credit", None)
            names = {"0": main_tex}
            if rotor_tex:
                names["1"] = rotor_tex
            model["textures"] = {k: f"{MODID}:block/{names.get(k, main_tex)}" for k in model.get("textures", {})}
            model["textures"]["particle"] = f"{MODID}:block/{main_tex}"
            model = {"parent": "minecraft:block/block", **model}
            if rotor_tex:
                model["render_type"] = "minecraft:cutout"
            if name.endswith("_item"):
                # the tower is taller than a block, so it is shown smaller in the inventory, hand and on the ground
                model["display"] = {
                    "gui": {"rotation": [30, 225, 0], "translation": [0, -3.5, 0], "scale": [0.36, 0.36, 0.36]},
                    "ground": {"translation": [0, 0, 0], "scale": [0.28, 0.28, 0.28]},
                    "fixed": {"translation": [0, -3, 0], "scale": [0.36, 0.36, 0.36]},
                    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.24, 0.24, 0.24]},
                    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]},
                }
            (models / f"{name}.json").write_text(json.dumps(model, indent=2) + chr(10), encoding="utf-8")
    print("built", name, flush=True)


def copy_game_textures(specdir, textures):
    for p in specdir.iterdir():
        if p.suffix in (".png", ".mcmeta") and not p.name.startswith("bb_"):
            shutil.copy2(p, textures / p.name)


def main():
    specdir, artdir, modelsdir, texdir = (Path(a) for a in sys.argv[1:5])
    only = set(sys.argv[5:])
    for d in (artdir, modelsdir, texdir):
        d.mkdir(parents=True, exist_ok=True)
    copy_game_textures(specdir, texdir)
    mcp = Mcp()
    for p in sorted(specdir.glob("*.spec.json")):
        if only and p.name.removesuffix(".spec.json") not in only:
            continue
        build(mcp, p, artdir, modelsdir, texdir)


if __name__ == "__main__":
    main()
