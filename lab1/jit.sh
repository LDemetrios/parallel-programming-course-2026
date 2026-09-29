#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/build/classes"
JIT_OUT="$ROOT/jit-output"
MAIN="org.ldemetrios.MainKt"

"$ROOT/build.sh"
mkdir -p "$JIT_OUT"

javap -classpath "$OUT" -c -p org.ldemetrios.collectors.DoubleBufferedCollector  > "$JIT_OUT/double-buffered-bytecode.txt"
javap -classpath "$OUT" -c -p org.ldemetrios.collectors.BrokenDoubleBufferedCollector > "$JIT_OUT/broken-double-buffered-bytecode.txt"

kotlin \
  -J-Xbatch \
  -J-XX:+UnlockDiagnosticVMOptions \
  -J-XX:+PrintCompilation \
  -J-XX:+PrintInlining \
  -J-XX:CompileCommand=print,org/ldemetrios/collectors/DoubleBufferedCollector.record \
  -cp "$OUT" "$MAIN" rw double-buffered 4 \
  > "$JIT_OUT/double-buffered-jit.txt" 2>&1

kotlin \
  -J-Xbatch \
  -J-XX:+UnlockDiagnosticVMOptions \
  -J-XX:+PrintCompilation \
  -J-XX:+PrintInlining \
  -J-XX:CompileCommand=print,org/ldemetrios/collectors/BrokenDoubleBufferedCollector.record \
  -cp "$OUT" "$MAIN" rw broken-double-buffered 4 \
  > "$JIT_OUT/broken-double-buffered-jit.txt" 2>&1
