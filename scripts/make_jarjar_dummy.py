"""Builds a tiny dev-only dummy mod that nests the BarelyCables jar, to prove the loader keeps one copy when two mods embed it.

Usage: python scripts/make_jarjar_dummy.py <out.jar> <modid> <barelycables.jar> [--bump 2026.11.2]

The dummy uses the code-less language provider (lowcodefml), so it needs no class and works on Forge and NeoForge from one jar.
With --bump the nested copy is rewritten to that version, to check that the highest version wins. Never shipped.
"""
import argparse
import re
import sys
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from nest_cables import jar_version, nest  # noqa: E402

TOML_FORGE = '''modLoader = "lowcodefml"
loaderVersion = "[0,)"
license = "MIT"

[[mods]]
modId = "{modid}"
version = "1.0.0"
displayName = "Jar-in-jar dummy {modid}"
description = "Dev-only test mod that nests BarelyCables."
'''

TOML_NEO = '''modLoader = "lowcodefml"
loaderVersion = "[1,)"
license = "MIT"

[[mods]]
modId = "{modid}"
version = "1.0.0"
displayName = "Jar-in-jar dummy {modid}"
description = "Dev-only test mod that nests BarelyCables."
'''


def rewritten(src, dest, version):
    """Copy of a mod jar with its version set to {version} in both metadata files."""
    with zipfile.ZipFile(src) as zin, zipfile.ZipFile(dest, "w", zipfile.ZIP_DEFLATED) as zout:
        for info in zin.infolist():
            data = zin.read(info.filename)
            if info.filename in ("META-INF/mods.toml", "META-INF/neoforge.mods.toml"):
                text = re.sub(r'(?m)^(version\s*=\s*)"[^"]+"', lambda m: f'{m.group(1)}"{version}"', data.decode("utf-8"), count=1)
                data = text.encode("utf-8")
            zout.writestr(info, data)


def main(argv):
    ap = argparse.ArgumentParser()
    ap.add_argument("out")
    ap.add_argument("modid")
    ap.add_argument("cables")
    ap.add_argument("--bump")
    a = ap.parse_args(argv[1:])
    out = Path(a.out)
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("META-INF/mods.toml", TOML_FORGE.format(modid=a.modid))
        z.writestr("META-INF/neoforge.mods.toml", TOML_NEO.format(modid=a.modid))
        z.writestr("pack.mcmeta", '{"pack": {"description": "dummy", "pack_format": 15}}')
    nested = Path(a.cables)
    if a.bump:
        nested = out.with_name(f"barelycables-bumped-{a.bump}.jar")
        rewritten(a.cables, nested, a.bump)
    version = a.bump or jar_version(nested)
    nest(out, nested, "com.itmecasey", "barelycables", version, "[2026.11.1,2027.0.0)")
    print(f"wrote {out} nesting barelycables {version}")


if __name__ == "__main__":
    main(sys.argv)
