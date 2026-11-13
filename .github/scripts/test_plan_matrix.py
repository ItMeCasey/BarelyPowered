#!/usr/bin/env python3
"""Tests for plan_matrix.py. Run from the repo root: python -I -m unittest discover -s .github/scripts
or: python -I .github/scripts/test_plan_matrix.py

Synthetic file lists always run; the cases built from real commits are skipped when the commit is not in the clone.
"""
import json
import os
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import plan_matrix as pm  # noqa: E402

ALL = set(pm.NAMES)
POWERED = {m for m in pm.NAMES if m.startswith("barelypowered-")}
CABLES = {m for m in pm.NAMES if m.startswith("barelycables-")}


def mc(version, project=None):
    return {m for m in pm.NAMES if m.endswith("-" + version) and (project is None or m.startswith(project + "-"))}


def git_files(*args):
    """Paths of a real commit range, or None when git or the commit is not available."""
    try:
        out = subprocess.run(["git", "diff", "--name-only", "--no-renames", *args], capture_output=True, text=True, cwd=pm.ROOT)
    except OSError:
        return None
    return out.stdout.split() if out.returncode == 0 else None


class SyntheticBuild(unittest.TestCase):
    def pick(self, *files):
        return set(pm.pick_build(list(files)))

    def test_there_are_sixteen_modules(self):
        self.assertEqual(len(ALL), 16)
        self.assertEqual((len(POWERED), len(CABLES)), (8, 8))

    def test_docs_and_status_pick_nothing(self):
        self.assertEqual(self.pick("PROJECT_STATUS.md", "README.md", "CHANGELOG.md", "docs/COMPATIBILITY.md", "docs/frames/a.png",
                                   "art/solar_panel_1.bbmodel", "branding/banners/banner.png", "curseforge/DESCRIPTION.md",
                                   "LICENSE", ".gitignore", "dist/x.jar", "release/jarjar/x.jar", "run/options.txt",
                                   "barelycables/README.md", "barelycables/LICENSE", "barelycables/dist/x.jar"), set())

    def test_workflow_and_ci_script_changes_pick_nothing(self):
        self.assertEqual(self.pick(".github/workflows/build.yml", ".github/workflows/release.yml",
                                   ".github/scripts/plan_matrix.py", ".github/scripts/publish_curseforge.py",
                                   ".github/ISSUE_TEMPLATE/config.yml"), set())

    def test_common_picks_every_barelypowered_module_only(self):
        self.assertEqual(self.pick("common/src/main/java/com/itmecasey/barelypowered/Fuel.java"), POWERED)
        self.assertEqual(self.pick("common/src/main/resources/assets/barelypowered/lang/en_us.json"), POWERED)
        self.assertEqual(self.pick("common/src/test/java/com/itmecasey/barelypowered/AutomationTest.java"), POWERED)

    def test_barelycables_common_picks_every_cables_module_only(self):
        self.assertEqual(self.pick("barelycables/common/src/main/resources/assets/barelycables/lang/en_us.json"), CABLES)
        self.assertEqual(self.pick("barelycables/common/src/test/java/x/ATest.java"), CABLES)

    def test_version_folders_pick_one_minecraft_version_of_their_own_project(self):
        self.assertEqual(self.pick("version-26.2/src/main/java/x/A.java"), mc("26.2", "barelypowered"))
        self.assertEqual(self.pick("barelycables/version-1.21.1/src/main/resources/a.json"), mc("1.21.1", "barelycables"))

    def test_dev_tools_are_not_built_by_ci(self):
        self.assertEqual(self.pick("version-26.2/src/dev/java/x/ShotTour.java", "neoforge-26.3/src/dev/java/x/Dev.java",
                                   "barelycables/forge-26.1.2/src/dev/java/x/ForgeDev.java",
                                   "barelycables/version-26.2/src/dev/resources/x"), set())

    def test_single_module_change_picks_exactly_that_module(self):
        self.assertEqual(self.pick("neoforge-26.3/src/main/java/x/A.java"), {"barelypowered-neoforge-26.3"})
        self.assertEqual(self.pick("forge-1.21.1/gradle.properties"), {"barelypowered-forge-1.21.1"})
        self.assertEqual(self.pick("barelycables/neoforge-26.1.2/build.gradle"), {"barelycables-neoforge-26.1.2"})
        self.assertEqual(self.pick("barelycables/forge-26.2/gradle.properties"), {"barelycables-forge-26.2"})

    def test_libs_pick_the_minecraft_version_of_their_own_project(self):
        self.assertEqual(self.pick("libs/smwti-api-26.2.jar"), mc("26.2", "barelypowered"))
        self.assertEqual(self.pick("barelycables/libs/smwti-api-26.3.jar"), mc("26.3", "barelycables"))
        self.assertEqual(self.pick("libs/new-thing.jar"), ALL)

    def test_module_build_and_run_folders_pick_nothing(self):
        self.assertEqual(self.pick("forge-26.2/build/libs/x.jar", "neoforge-26.2/run/options.txt",
                                   "barelycables/forge-26.2/runs/a", "barelycables/neoforge-26.2/build/x"), set())

    def test_combine_and_nest_scripts_pick_everything_and_dev_scripts_nothing(self):
        self.assertEqual(self.pick("scripts/combine_jars.py"), ALL)
        self.assertEqual(self.pick("scripts/nest_cables.py"), ALL)
        self.assertEqual(self.pick("scripts/client_tour.py", "scripts/make_assets.py", "scripts/server_check.py",
                                   "scripts/templates/BarelyPoweredJadePlugin.java.tpl", "scripts/add_jade.py",
                                   "barelycables/scripts/combine_jars.py", "barelycables/scripts/server_check.py"), set())
        self.assertEqual(self.pick("scripts/brand_new_tool.py"), ALL)

    def test_unknown_path_picks_everything(self):
        self.assertEqual(self.pick("something/new.txt"), ALL)
        self.assertEqual(self.pick("build.gradle"), ALL)
        self.assertEqual(self.pick(".gitattributes"), ALL)
        self.assertEqual(self.pick("forge-9.9/build.gradle"), ALL)
        self.assertEqual(self.pick("barelycables/something.txt"), ALL)
        self.assertEqual(self.pick("barelycables/version-9.9/src/main/java/A.java"), ALL)

    def test_mixed_change_is_the_union(self):
        self.assertEqual(self.pick("PROJECT_STATUS.md", "forge-26.2/gradle.properties", "barelycables/neoforge-26.3/gradle.properties"),
                         {"barelypowered-forge-26.2", "barelycables-neoforge-26.3"})

    def test_backslash_and_dot_slash_paths(self):
        self.assertEqual(self.pick("barelycables\\forge-26.2\\src\\main\\java\\A.java"), {"barelycables-forge-26.2"})
        self.assertEqual(self.pick("./.github/workflows/build.yml"), set())

    def test_diff_failure_picks_everything(self):
        self.assertEqual(set(pm.pick_build(None)), ALL)
        self.assertIsNone(pm.changed_files("0123456789abcdef0123456789abcdef01234567", "fedcba9876543210fedcba9876543210fedcba98"))

    def test_outputs(self):
        out = pm.outputs(["barelypowered-neoforge-1.21.1", "barelycables-forge-26.2"])
        self.assertEqual((out["any"], out["count"], out["test_any"], out["combine"]), ("true", "2", "true", ""))
        self.assertEqual(json.loads(out["matrix"])["include"], [
            {"project": "barelypowered", "layer": "neoforge-1.21.1", "dir": "neoforge-1.21.1", "java": 21},
            {"project": "barelycables", "layer": "forge-26.2", "dir": "barelycables/forge-26.2", "java": 25},
        ])
        self.assertEqual(json.loads(out["test_matrix"]), {"include": [{"dir": "neoforge-1.21.1"}]})
        both = json.loads(pm.outputs(["barelypowered-neoforge-1.21.1", "barelycables-neoforge-1.21.1"])["test_matrix"])
        self.assertEqual(both, {"include": [{"dir": "neoforge-1.21.1"}, {"dir": "barelycables/neoforge-1.21.1"}]})
        nothing = pm.outputs([])
        self.assertEqual((nothing["any"], nothing["test_any"], nothing["count"], nothing["combine"], nothing["matrix"]),
                         ("false", "false", "0", "", '{"include": []}'))
        self.assertEqual(pm.outputs(pm.NAMES)["combine"], "1.21.1 26.1.2 26.2 26.3")
        self.assertEqual(pm.outputs(sorted(POWERED))["combine"], "")  # no cables built: nothing to nest
        self.assertEqual(pm.outputs(sorted(mc("26.2")))["combine"], "26.2")


class RealCommits(unittest.TestCase):
    def check(self, a, b, expected):
        files = git_files(a, b)
        if files is None:
            self.skipTest(f"commit {a} or {b} is not in this clone")
        self.assertEqual(set(pm.pick_build(files)), expected)

    def test_status_only_commit(self):
        self.check("a7dbfe6^", "a7dbfe6", set())

    def test_docs_and_frames_commit(self):
        self.check("ad74082^", "ad74082", set())

    def test_docs_only_commit(self):
        self.check("c5cb3d9^", "c5cb3d9", set())

    def test_common_change_commit(self):
        self.check("60d7f6f^", "60d7f6f", POWERED)

    def test_barelypowered_version_bump_commit(self):
        self.check("b4bffac^", "b4bffac", POWERED)

    def test_barelycables_workflow_and_sources_commit(self):
        self.check("90ad2c7^", "90ad2c7", CABLES)


class Release(unittest.TestCase):
    def make_root(self, powered, cables="2026.11.2"):
        root = tempfile.mkdtemp()
        for name in pm.NAMES:
            folder = os.path.join(root, pm.module_dir(name))
            os.makedirs(folder)
            version = powered.get(name, "2026.11.3") if name.startswith("barelypowered-") else cables
            with open(os.path.join(folder, "gradle.properties"), "w", encoding="utf-8") as fh:
                fh.write(f"mod_id=x\nmod_version={version}\n")
        return root

    def test_modules_at_the_tag_version_and_the_cables_of_their_minecraft_versions(self):
        root = self.make_root({m: "2026.11.4" for m in mc("26.3", "barelypowered")})
        picked, version = pm.pick_release("v2026.11.4", root)
        self.assertEqual(version, "2026.11.4")
        self.assertEqual(set(picked), mc("26.3"))  # both projects, only 26.3

    def test_all_modules_at_the_tag_version(self):
        root = self.make_root({})
        self.assertEqual(set(pm.pick_release("v2026.11.3", root)[0]), ALL)

    def test_cables_version_is_never_the_tag(self):
        root = self.make_root({})
        self.assertEqual(pm.pick_release("v2026.11.2", root)[0], [])  # the cables carry 2026.11.2 but are not released alone

    def test_tag_nobody_carries_picks_nothing(self):
        self.assertEqual(pm.pick_release("v2026.11.9", self.make_root({}))[0], [])

    def test_manual_run_uses_the_newest_barelypowered_version(self):
        root = self.make_root({m: "2026.11.10" for m in mc("26.2", "barelypowered")}, cables="2026.12.1")
        picked, version = pm.pick_release("main", root)
        self.assertEqual((version, set(picked)), ("2026.11.10", mc("26.2")))

    def test_real_repo_versions(self):
        version = pm.newest_version()
        if not version:
            self.skipTest("no mod_version found")
        picked, _ = pm.pick_release("v" + version)
        self.assertTrue(picked)
        for name in picked:
            if name.startswith("barelypowered-"):
                self.assertEqual(pm.module_version(name), version)


if __name__ == "__main__":
    unittest.main()
