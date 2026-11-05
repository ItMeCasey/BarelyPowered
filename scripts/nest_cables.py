"""Nests one mod jar inside another with the loader's jar-in-jar (JarJar) layout.

Usage: python scripts/nest_cables.py <host.jar> <nested.jar> [--group G] [--artifact A] [--version V] [--range R]

Both Forge and NeoForge look for META-INF/jarjar/metadata.json in a mod jar and load the jars it lists. When two mods nest the same
group and artifact, the loader keeps one copy (the highest version that satisfies every range). The nested jar should itself be a
combined Forge plus NeoForge jar (see combine_jars.py), so the single metadata file works on both loaders.

Defaults describe BarelyCables: group com.itmecasey, artifact barelycables, version read from the nested jar's mods.toml.
"""
import argparse
import json
import re
import shutil
import sys
import zipfile
from pathlib import Path


def jar_version(path):
    with zipfile.ZipFile(path) as z:
        for name in ("META-INF/neoforge.mods.toml", "META-INF/mods.toml"):
            if name in z.namelist():
                m = re.search(r'^version\s*=\s*"([^"]+)"', z.read(name).decode("utf-8"), re.MULTILINE)
                if m:
                    return m.group(1)
    raise SystemExit(f"no version found in {path}")


def nest(host, nested, group, artifact, version, version_range):
    host = Path(host)
    nested = Path(nested)
    tmp = host.with_suffix(".tmp")
    entry = f"META-INF/jarjar/{nested.name}"
    new = {"identifier": {"group": group, "artifact": artifact},
           "version": {"range": version_range, "artifactVersion": version},
           "path": entry, "isObfuscated": False}
    with zipfile.ZipFile(host) as src, zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as out:
        jars = []
        for info in src.infolist():
            if info.filename == "META-INF/jarjar/metadata.json":
                jars = json.loads(src.read(info.filename).decode("utf-8")).get("jars", [])
                continue
            if info.filename == entry:
                continue
            out.writestr(info, src.read(info.filename))
        jars = [j for j in jars if j.get("identifier") != new["identifier"]] + [new]
        out.writestr(entry, nested.read_bytes())
        out.writestr("META-INF/jarjar/metadata.json", json.dumps({"jars": jars}, indent=2))
    shutil.move(tmp, host)
    return entry


def main(argv):
    ap = argparse.ArgumentParser()
    ap.add_argument("host")
    ap.add_argument("nested")
    ap.add_argument("--group", default="com.itmecasey")
    ap.add_argument("--artifact", default="barelycables")
    ap.add_argument("--version")
    ap.add_argument("--range")
    a = ap.parse_args(argv[1:])
    version = a.version or jar_version(a.nested)
    version_range = a.range or f"[{version},)"
    entry = nest(a.host, a.nested, a.group, a.artifact, version, version_range)
    print(f"nested {entry} ({a.group}:{a.artifact} {version}, range {version_range}) into {a.host}")


if __name__ == "__main__":
    main(sys.argv)
