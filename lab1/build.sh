#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/build/classes"
SOURCE_ROOTS=("$ROOT/src/main/java" "$ROOT/src/main/kotlin")

rm -rf "$OUT"
mkdir -p "$OUT"

JAVA_FILES=()
KOTLIN_FILES=()

for src in "${SOURCE_ROOTS[@]}"; do
  while IFS= read -r -d '' file; do
    JAVA_FILES+=("$file")
  done < <(find "$src" -name '*.java' -print0 2>/dev/null || true)

  while IFS= read -r -d '' file; do
    KOTLIN_FILES+=("$file")
  done < <(find "$src" -name '*.kt' -print0 2>/dev/null || true)
done

if [[ ${#KOTLIN_FILES[@]} -gt 0 ]]; then
  SOURCES=("${KOTLIN_FILES[@]}")
  if [[ ${#JAVA_FILES[@]} -gt 0 ]]; then
    SOURCES+=("${JAVA_FILES[@]}")
  fi
  kotlinc -jvm-target 17 -d "$OUT" "${SOURCES[@]}"
fi

if [[ ${#JAVA_FILES[@]} -gt 0 ]]; then
  javac -encoding UTF-8 -cp "$OUT" -d "$OUT" "${JAVA_FILES[@]}"
fi
