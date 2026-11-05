#!/usr/bin/env bash
# Builds and tests every layer, then combines the two loader jars of each Minecraft version into dist/.
# Usage: scripts/build_all.sh [mc-version ...]   (default: all four)
set -u
cd "$(dirname "$0")/.."
VERSIONS=("$@")
[ ${#VERSIONS[@]} -eq 0 ] && VERSIONS=(1.21.1 26.1.2 26.2 26.3)
MODVER=$(grep -h '^mod_version=' neoforge-1.21.1/gradle.properties | cut -d= -f2)
CABVER=$(grep -h '^mod_version=' barelycables/neoforge-1.21.1/gradle.properties | cut -d= -f2)
mkdir -p dist
fail=0
# BarelyCables is its own module: build it first, then nest its combined jar inside each BarelyPowered jar.
if [ -z "${SKIP_CABLES:-}" ]; then
  echo "=== barelycables"
  bash barelycables/scripts/build_all.sh "${VERSIONS[@]}" || fail=1
fi
for v in "${VERSIONS[@]}"; do
  jars=()
  for loader in neoforge forge; do
    echo "=== $loader-$v"
    if ! (cd "$loader-$v" && chmod +x gradlew && ./gradlew build --no-daemon -q 2>&1 | grep -v "Started working\|REUSE\|DONE\|Total runtime\|^WARNING"); then :; fi
    jar=$(ls "$loader-$v"/build/libs/barelypowered-$loader-mc$v-$MODVER.jar 2>/dev/null)
    if [ -z "$jar" ]; then echo "FAILED: no jar for $loader-$v"; fail=1; else jars+=("$jar"); fi
  done
  if [ ${#jars[@]} -eq 2 ]; then
    python scripts/combine_jars.py "dist/barelypowered-mc$v-$MODVER.jar" "${jars[@]}"
    if [ -f "barelycables/dist/barelycables-mc$v-$CABVER.jar" ]; then
      python scripts/nest_cables.py "dist/barelypowered-mc$v-$MODVER.jar" "barelycables/dist/barelycables-mc$v-$CABVER.jar" --range "[$CABVER,)"
    else
      echo "FAILED: no barelycables jar for $v"; fail=1
    fi
  fi
done
exit $fail
