#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
UI="$ROOT/android/app/src/main/java/io/github/gdict/ui"
SEARCH="$UI/screens/SearchScreen.kt"
APP="$UI/GdictApp.kt"
AMBIENT="$UI/components/AmbientBackground.kt"
ACRYLIC="$UI/components/AcrylicComponents.kt"
LIQUID="$UI/components/LiquidGlass.kt"
PRON="$UI/screens/PronunciationDetailScreen.kt"
BUILD="$ROOT/android/app/build.gradle.kts"

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

# Liquid-glass v3 safety rails. Match executable APIs/dependencies, not prose comments.
if [[ -f "$LIQUID" ]]; then
  if grep -Eq '^import dev\.chrisbanes\.haze|HazeState|\.hazeChild\(|\.haze\(' "$APP" "$LIQUID" || \
     grep -Fq 'dev.chrisbanes.haze:haze:' "$BUILD"; then
    fail "Liquid glass must not reintroduce Haze; real-device testing showed a compositing seam"
  fi
  grep -Fq 'rememberGraphicsLayer' "$APP" ||
    fail "App backdrop must be recorded with Compose rememberGraphicsLayer()"
  grep -Fq 'backdropLayer.record' "$APP" ||
    fail "App backdrop GraphicsLayer must record the actual page draw commands"
  grep -Fq 'drawLayer(backdropLayer)' "$LIQUID" ||
    fail "Liquid glass must sample the recorded app backdrop layer"
  grep -Fq 'uniform shader backdrop' "$LIQUID" ||
    fail "Android 13+ refraction shader must sample the real backdrop texture"
  grep -Fq 'createRuntimeShaderEffect' "$LIQUID" ||
    fail "Android 13+ liquid glass must use RuntimeShader as a RenderEffect"
  grep -Fq 'Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU' "$LIQUID" ||
    fail "RuntimeShader refraction must remain guarded to Android 13+"
  grep -Fq 'AndroidShader.TileMode.CLAMP' "$LIQUID" ||
    fail "Backdrop blur must clamp edge samples to avoid window-edge seams"
  grep -Fq 'GdictColors.GlassSurface' "$LIQUID" ||
    fail "Liquid glass must preserve the stable light opaque fallback"
  grep -Fq 'GdictColors.DarkGlassSurface' "$LIQUID" ||
    fail "Liquid glass must preserve the stable dark opaque fallback"
  if grep -Eq 'noiseFactor[[:space:]]*=|float[[:space:]]+streak[[:space:]]*=' "$LIQUID"; then
    fail "Liquid glass must remain optically clean: no grain or full-width streak effect"
  fi
  grep -Fq 'drawContent()' "$LIQUID" ||
    fail "Liquid glass foreground content must be drawn after the optical backdrop"
  grep -Fq 'compose-bom:2024.09.02' "$BUILD" ||
    fail "GraphicsLayer experiment requires the pinned Compose 1.7-generation BOM"
  grep -Fq 'bottom = 18.dp' "$APP" ||
    fail "Floating glass bottom bar must keep breathing room from the window edge"
fi

echo "Apple HIG UI policy gate passed."
