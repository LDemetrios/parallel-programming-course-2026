#!/usr/bin/env bash
set -euo pipefail
set -x

ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/build/classes"
MAIN="org.ldemetrios.MainKt"

"$ROOT/build.sh"

#echo
#echo "Stage 0: single-thread baseline"
#kotlin -cp "$OUT" "$MAIN" w single 1
#
#echo
#echo "Stage 1: synchronized collector"
#kotlin -cp "$OUT" "$MAIN" w sync 1
#kotlin -cp "$OUT" "$MAIN" w sync 2
#kotlin -cp "$OUT" "$MAIN" w sync 4
#kotlin -cp "$OUT" "$MAIN" w sync 8
#kotlin -cp "$OUT" "$MAIN" w sync 16
#
#echo
#echo "Stage 1: empty synchronized collector"
#kotlin -cp "$OUT" "$MAIN" w no-op 1
#kotlin -cp "$OUT" "$MAIN" w no-op 2
#kotlin -cp "$OUT" "$MAIN" w no-op 4
#kotlin -cp "$OUT" "$MAIN" w no-op 8
#kotlin -cp "$OUT" "$MAIN" w no-op 16
#
#echo
#echo "Stage 2: striped locks"
#kotlin -cp "$OUT" "$MAIN" w striped 1
#kotlin -cp "$OUT" "$MAIN" w striped 2
#kotlin -cp "$OUT" "$MAIN" w striped 4
#kotlin -cp "$OUT" "$MAIN" w striped 8
#kotlin -cp "$OUT" "$MAIN" w striped 16
#
#echo
#echo "Stage 2: striped snapshot consistency"
#kotlin -cp "$OUT" "$MAIN" rw striped 4

#echo
#echo "Stage 3: thread-local collector"
#kotlin -cp "$OUT" "$MAIN" w thread-local 1
#kotlin -cp "$OUT" "$MAIN" w thread-local 2
#kotlin -cp "$OUT" "$MAIN" w thread-local 4
#kotlin -cp "$OUT" "$MAIN" w thread-local 8
#kotlin -cp "$OUT" "$MAIN" w thread-local 16

echo
echo "Stage 3: thread-local snapshot consistency"
kotlin -cp "$OUT" "$MAIN" rw thread-local 4

#echo
#echo "Stage 4: double-buffered collector"
#kotlin -cp "$OUT" "$MAIN" w double-buffered 1
#kotlin -cp "$OUT" "$MAIN" w double-buffered 2
#kotlin -cp "$OUT" "$MAIN" w double-buffered 4
#kotlin -cp "$OUT" "$MAIN" w double-buffered 8
#kotlin -cp "$OUT" "$MAIN" w double-buffered 16
#
#echo
#echo "Stage 4: double-buffered snapshot consistency"
#kotlin -cp "$OUT" "$MAIN" rw double-buffered 4
#
#echo
#echo "Stage 4 experiment: broken-double-buffered collector"
#kotlin -cp "$OUT" "$MAIN" rw broken-double-buffered 4
#
#echo
#echo "Stage 4 experiment: broken-double-buffered collector with no warmup"
#kotlin -cp "$OUT" "$MAIN" rw broken-double-buffered 4 --skip-warmup
