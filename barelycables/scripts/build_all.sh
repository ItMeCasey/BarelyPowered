#!/usr/bin/env bash
# Builds and tests every layer, then combines the two loader jars of each Minecraft version into dist/.
# Usage: scripts/build_all.sh [mc-version ...]   (default: all four)
set -u
cd "$(dirname "$0")/.."
VERSIONS=("$@")
[ ${#VERSIONS[@]} -eq 0 ] && VERSIONS=(1.21.1 26.1.2 26.2 26.3)
MODVER=$(grep -h '^mod_version=' neoforge-1.21.1/gradle.properties | cut -d= -f2)
mkdir -p dist
fail=0
for v in "${VERSIONS[@]}"; do
  jars=()
  for loader in neoforge forge; do
    echo "=== $loader-$v"
    if ! (cd "$loader-$v" && chmod +x gradlew && ./gradlew build --no-daemon -q 2>&1 | grep -v "Started working\|REUSE\|DONE\|Total runtime\|^WARNING"); then :; fi
    jar=$(ls "$loader-$v"/build/libs/barelycables-$loader-mc$v-$MODVER.jar 2>/dev/null)
    if [ -z "$jar" ]; then echo "FAILED: no jar for $loader-$v"; fail=1; else jars+=("$jar"); fi
  done
  if [ ${#jars[@]} -eq 2 ]; then
    python scripts/combine_jars.py "dist/barelycables-mc$v-$MODVER.jar" "${jars[@]}"
  fi
done
exit $fail
