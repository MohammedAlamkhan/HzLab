#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAVA_HOME="${JAVA_HOME:-$ROOT/../tools/jdk}"
JAVAC="$JAVA_HOME/bin/javac"
JAVA="$JAVA_HOME/bin/java"
OUT="$ROOT/test/classes"
rm -rf "$OUT"; mkdir -p "$OUT"
SRC="$ROOT/app/src/main/java/io/github/mohammedalamkhan/hzlab"
"$JAVAC" -d "$OUT" \
  "$SRC/Notes.java" "$SRC/PitchResult.java" "$SRC/PitchDetector.java" \
  "$ROOT/test/PitchDetectorTest.java" "$ROOT/test/HardPitchTest.java" \
  "$ROOT/test/ChordScoreTest.java" "$ROOT/test/BassChromaTest.java" \
  "$ROOT/test/BassSingle.java" "$ROOT/test/ChromaTest.java"
"$JAVA" -cp "$OUT" PitchDetectorTest
"$JAVA" -cp "$OUT" HardPitchTest
"$JAVA" -cp "$OUT" ChordScoreTest
echo "DSP tests completed."
