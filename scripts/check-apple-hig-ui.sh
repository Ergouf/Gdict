#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SEARCH="$ROOT/android/app/src/main/java/io/github/gdict/ui/screens/SearchScreen.kt"
APP="$ROOT/android/app/src/main/java/io/github/gdict/ui/GdictApp.kt"
AMBIENT="$ROOT/android/app/src/main/java/io/github/gdict/ui/components/AmbientBackground.kt"
ACRYLIC="$ROOT/android/app/src/main/java/io/github/gdict/ui/components/AcrylicComponents.kt"

fail() {
  echo "UI gate failed: $1" >&2
  exit 1
}

for pattern in detectTransformGestures detectDragGestures DragHandle staggerEnterAnimation; do
  if grep -Fq "$pattern" "$SEARCH"; then
    fail "SearchScreen reintroduced nonstandard gesture/decorative motion: $pattern"
  fi
done

if grep -Eq 'Color\(0x[0-9A-Fa-f]+' "$SEARCH"; then
  fail "SearchScreen contains a hardcoded RGB color; use semantic GdictColors tokens"
fi

if grep -Eq 'Color\(0x[0-9A-Fa-f]+' "$APP"; then
  fail "GdictApp navigation contains a hardcoded RGB color; use semantic GdictColors tokens"
fi

if grep -Fq '.blur(' "$ACRYLIC" || grep -Fq 'blurRadius' "$ACRYLIC"; then
  fail "Glass components must not pretend Modifier.blur is backdrop blur"
fi

if grep -Fq 'radialGradient' "$AMBIENT"; then
  fail "Ambient blue radial material is not allowed in the migrated foundation"
fi

echo "Apple HIG UI policy gate passed."
