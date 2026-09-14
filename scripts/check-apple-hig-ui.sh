#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
UI="$ROOT/android/app/src/main/java/io/github/gdict/ui"
SEARCH="$UI/screens/SearchScreen.kt"
APP="$UI/GdictApp.kt"
AMBIENT="$UI/components/AmbientBackground.kt"
ACRYLIC="$UI/components/AcrylicComponents.kt"
PRON="$UI/screens/PronunciationDetailScreen.kt"

MIGRATED_SCREENS=(
  "$UI/screens/SearchScreen.kt"
  "$UI/screens/BookmarksScreen.kt"
  "$UI/screens/FlashcardScreen.kt"
  "$UI/screens/SettingsScreen.kt"
  "$UI/screens/WordDetailScreen.kt"
  "$UI/screens/PronunciationDetailScreen.kt"
  "$UI/screens/CollinsDetailScreen.kt"
  "$UI/screens/DictionariesScreen.kt"
)

fail() {
  echo "UI gate failed: $1" >&2
  exit 1
}

for pattern in detectTransformGestures detectDragGestures DragHandle staggerEnterAnimation; do
  if grep -Fq "$pattern" "$SEARCH"; then
    fail "SearchScreen reintroduced nonstandard gesture/decorative motion: $pattern"
  fi
done

if grep -Fq 'detectHorizontalDragGestures' "$UI/screens/FlashcardScreen.kt"; then
  fail "Learning reintroduced swipe-only card navigation; review must remain explicit and accessible"
fi

for file in "$UI/screens/WordDetailScreen.kt" "$UI/screens/PronunciationDetailScreen.kt" "$UI/screens/CollinsDetailScreen.kt"; do
  if grep -Fq 'detectTransformGestures' "$file"; then
    fail "Detail screens must not use document-wide pinch zoom: ${file##*/}"
  fi
done

for file in "${MIGRATED_SCREENS[@]}" "$APP"; do
  if grep -Eq 'Color\(0x[0-9A-Fa-f]+' "$file"; then
    fail "Migrated UI contains a hardcoded RGB color; use semantic GdictColors tokens: ${file##*/}"
  fi
  if grep -Fq 'pageEnterAnimation' "$file"; then
    fail "Migrated UI reintroduced automatic decorative page motion: ${file##*/}"
  fi
done

if grep -Fq 'import androidx.compose.ui.draw.blur' "$ACRYLIC" || grep -Eq '^[[:space:]]*blurRadius[[:space:]]*:' "$ACRYLIC"; then
  fail "Glass components must not pretend Modifier.blur is backdrop blur"
fi

if grep -Eq 'Brush\.radialGradient|radialGradient\(' "$AMBIENT" "$PRON"; then
  fail "Decorative radial ambient lighting is not allowed in the migrated foundation"
fi

if grep -Eq 'rememberInfiniteTransition|infiniteRepeatable' "$PRON"; then
  fail "Pronunciation controls must not pulse indefinitely for decoration"
fi

echo "Apple HIG UI policy gate passed."
