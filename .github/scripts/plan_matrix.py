#!/usr/bin/env python3
"""Pick the builds a workflow run needs, so unchanged Minecraft versions and loaders are never built.

  plan_matrix.py build   [--before SHA] [--sha SHA] [--files PATH...]   modules whose code changed in the push
  plan_matrix.py release --tag vYYYY.M.N                                modules a release of that version needs

Writes these lines to $GITHUB_OUTPUT (and prints them):
  matrix=<json>       {"include": [{"project": ..., "layer": ..., "dir": ..., "java": ...}]}
  any=true|false      at least one module is picked
  count=<n>           number of picked modules
  test_matrix=<json>  {"include": [{"dir": ...}]}: the unit-test jobs to run (the 1.21.1 NeoForge modules, when picked)
  test_any=true|false
  combine=<list>      Minecraft versions whose four layers (BarelyPowered and BarelyCables, both loaders) are all picked,
                      so a combined jar with the nested cables can be made (space separated)

A module is "<project>-<layer>" (barelypowered-forge-26.2, barelycables-neoforge-1.21.1); BarelyCables lives in barelycables/.
How the modules share code (read from every build.gradle):
  common/src/main, common/src/test           compiled into every BarelyPowered module
  version-<mc>/src/main                      compiled into both BarelyPowered loaders of that Minecraft version
  libs/smwti-api-<mc>.jar                    compileOnly in both BarelyPowered loaders of that version
  barelycables/common, barelycables/version-<mc>, barelycables/libs   the same three, for the BarelyCables modules only
  <layer>/src/main, barelycables/<layer>/src/main                      the module itself
  src/dev folders                            only with -PdevTools (self test, tours); the CI never passes it
  scripts/combine_jars.py, scripts/nest_cables.py   make the combined jars the release uploads
BarelyCables is never uploaded alone; it is nested in the BarelyPowered jar. A release builds the BarelyPowered modules whose
mod_version equals the tag, plus the BarelyCables modules of those Minecraft versions (at whatever version they carry).
Anything not understood selects every module.
"""
import argparse
import json
import os
import re
import subprocess

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

LAYERS = [
    ("neoforge-1.21.1", 21),
    ("forge-1.21.1", 21),
    ("neoforge-26.1.2", 25),
    ("forge-26.1.2", 25),
    ("neoforge-26.2", 25),
    ("forge-26.2", 25),
    ("neoforge-26.3", 25),
    ("forge-26.3", 25),
]
PROJECTS = ["barelypowered", "barelycables"]
JAVA = dict(LAYERS)
MCS = ["1.21.1", "26.1.2", "26.2", "26.3"]
# Same modules and order as the project x layer matrix in build.yml and release.yml.
NAMES = [f"{p}-{layer}" for p in PROJECTS for layer, _ in LAYERS]
TEST_LAYER = "neoforge-1.21.1"  # runs `gradlew test` in the test job, for both projects
ZERO = "0" * 40

MODULE_NOISE = re.compile(r"^(build|run|runs|run-data|\.gradle|logs|crash-reports)(/|$)")
NONE_TOP = re.compile(r"^(art|branding|curseforge|dist|docs|logs|release|crash-reports|run[^/]*|runs|[^/]*\.log)(/|$)")
NONE_FILES = {"PROJECT_STATUS.md", "CHANGELOG.md", "README.md", "PLAN.md", "LICENSE", ".gitignore"}
NONE_GITHUB = re.compile(r"^\.github/(workflows|scripts|ISSUE_TEMPLATE)/")
# Generators, tours and dev checks run by hand; no gradle file and no workflow step uses them (checked).
NONE_SCRIPTS = {
    "scripts/add_jade.py", "scripts/art.py", "scripts/build_all.sh", "scripts/client_tour.py", "scripts/crop_shots.py",
    "scripts/jarjar_check.py", "scripts/make_assets.py", "scripts/make_jarjar_dummy.py", "scripts/make_models.py",
    "scripts/send_models.py", "scripts/server_check.py", "scripts/viewer_check.py",
}
NONE_SCRIPT_DIRS = ("scripts/templates/", "scripts/__pycache__/")
ALL_FILES = {"scripts/combine_jars.py", "scripts/nest_cables.py"}
# BarelyCables' own top-level files: docs, its own scripts (the workflows use the root ones), its old dist folder.
NONE_CABLES = re.compile(r"^(dist|logs|release)(/|$)|^(README\.md|LICENSE)$|^scripts/")

LAYER_RE = r"(forge|neoforge)-(\d[\w.]*)"
LAYER_DIR = re.compile(rf"^{LAYER_RE}/(.*)$")
VERSION_DIR = re.compile(r"^version-(\d[\w.]*)/(.*)$")
LIB = re.compile(r"^libs/smwti-api-(\d[\w.]*)\.jar$")


def module(project, layer):
    return f"{project}-{layer}"


def project_modules(project, mc=None):
    return {m for m in NAMES if m.startswith(project + "-") and (mc is None or m.endswith("-" + mc))}


def modules_in(project, path):
    """The modules of one project (paths relative to that project's root) a changed path can change."""
    m = LAYER_DIR.match(path)
    if m:
        name = module(project, f"{m.group(1)}-{m.group(2)}")
        rest = m.group(3)
        if name not in NAMES:
            return set(NAMES)  # a module folder this script does not know: fail safe
        if MODULE_NOISE.match(rest) or rest.startswith("src/dev/"):
            return set()
        return {name}
    m = VERSION_DIR.match(path)
    if m:
        mc, rest = m.group(1), m.group(2)
        if mc not in MCS:
            return set(NAMES)
        if rest.startswith("src/dev/"):
            return set()
        return project_modules(project, mc)
    if path.startswith("common/"):
        return project_modules(project)
    m = LIB.match(path)
    if m:
        return project_modules(project, m.group(1)) if m.group(1) in MCS else set(NAMES)
    return None  # not a shared folder of this project


def modules_for(path):
    """The set of module names a changed path can change the jar or the test result of."""
    path = path.replace("\\", "/")
    while path.startswith("./"):
        path = path[2:]
    if path.startswith("barelycables/"):
        rest = path[len("barelycables/"):]
        found = modules_in("barelycables", rest)
        if found is not None:
            return found
        if NONE_CABLES.match(rest):
            return set()
        return set(NAMES)
    found = modules_in("barelypowered", path)
    if found is not None:
        return found
    if path in ALL_FILES:
        return set(NAMES)
    if path in NONE_FILES or path in NONE_SCRIPTS or path.startswith(NONE_SCRIPT_DIRS):
        return set()
    if NONE_TOP.match(path) or NONE_GITHUB.match(path):
        return set()
    return set(NAMES)  # unknown path


def changed_files(before, sha, cwd=None):
    """Paths changed between two commits, or None when git cannot tell (the caller then builds everything)."""
    if not before or before == ZERO:
        before = sha + "^"  # first push of a branch or a manual run: compare with the parent commit
    try:
        out = subprocess.run(["git", "diff", "--name-only", "--no-renames", before, sha], capture_output=True, text=True, cwd=cwd or ROOT)
    except OSError:
        return None
    if out.returncode != 0:
        return None
    return [line.strip() for line in out.stdout.splitlines() if line.strip()]


def module_dir(name):
    project, layer = name.split("-", 1)
    return f"barelycables/{layer}" if project == "barelycables" else layer


def module_version(name, root=None):
    try:
        with open(os.path.join(root or ROOT, module_dir(name), "gradle.properties"), encoding="utf-8") as fh:
            text = fh.read()
    except OSError:
        return None
    m = re.search(r"^mod_version=(\S+)", text, re.M)
    return m.group(1) if m else None


def newest_version(root=None):
    found = {module_version(m, root) for m in NAMES if m.startswith("barelypowered-")} - {None}
    keyed = [v for v in found if re.match(r"^\d+(\.\d+)*$", v)]
    return sorted(keyed, key=lambda v: [int(x) for x in v.split(".")])[-1] if keyed else ""


def pick_build(files):
    if files is None:
        return list(NAMES)  # cannot tell: build everything
    picked = set()
    for f in files:
        picked |= modules_for(f)
    return [m for m in NAMES if m in picked]


def pick_release(tag, root=None):
    """BarelyPowered modules at the tag's version, plus the BarelyCables modules of those Minecraft versions."""
    version = (tag or "").lstrip("v")
    if not re.match(r"^\d+\.\d+\.\d+$", version):
        version = newest_version(root)  # a manual run on a branch: release the newest version found
    powered = [m for m in NAMES if m.startswith("barelypowered-") and module_version(m, root) == version]
    mcs = {m.rsplit("-", 1)[1] for m in powered}
    cables = [m for m in NAMES if m.startswith("barelycables-") and m.rsplit("-", 1)[1] in mcs]
    return [m for m in NAMES if m in powered or m in cables], version


def outputs(picked):
    picked = set(picked)
    include = []
    for name in NAMES:
        if name in picked:
            project, layer = name.split("-", 1)
            include.append({"project": project, "layer": layer, "dir": module_dir(name), "java": JAVA[layer]})
    tests = [{"dir": module_dir(module(p, TEST_LAYER))} for p in PROJECTS if module(p, TEST_LAYER) in picked]
    combine = [mc for mc in MCS if (project_modules("barelypowered", mc) | project_modules("barelycables", mc)) <= picked]
    return {
        "matrix": json.dumps({"include": include}),
        "any": "true" if picked else "false",
        "count": str(len(picked)),
        "test_matrix": json.dumps({"include": tests}),
        "test_any": "true" if tests else "false",
        "combine": " ".join(combine),
    }


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("mode", choices=["build", "release"])
    ap.add_argument("--before")
    ap.add_argument("--sha", default="HEAD")
    ap.add_argument("--tag")
    ap.add_argument("--files", nargs="*", default=None, help="changed paths, instead of asking git (for tests)")
    args = ap.parse_args(argv)

    if args.mode == "release":
        picked, version = pick_release(args.tag)
        print("release version:", version or "none")
    else:
        files = args.files if args.files is not None else changed_files(args.before, args.sha)
        picked = pick_build(files)
        if files is None:
            print("could not compute the changed files: building everything")

    out = outputs(picked)
    lines = [f"{k}={v}" for k, v in out.items()]
    print("\n".join(lines))
    print("builds:", ", ".join(picked) or "none")
    target = os.environ.get("GITHUB_OUTPUT")
    if target:
        with open(target, "a", encoding="utf-8") as fh:
            fh.write("\n".join(lines) + "\n")


if __name__ == "__main__":
    main()
