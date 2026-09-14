# Gdict Apple HIG UI Migration

> Scope: Android first. Desktop remains on its current Fluent-oriented shell until a separate parity phase. This is an Apple HIG-inspired adaptation for Jetpack Compose, not an attempt to make Android pretend to be iOS.

## Status

Android phases 0–5 are implemented on `codex/apple-hig-ui` / PR #4.

- Phase 0: governance, CI and executable UI policy gate — complete.
- Phase 1: semantic colors, quiet backgrounds, honest glass and restrained motion — complete.
- Phase 2: bottom navigation and primary Search flow — complete.
- Phase 3: Favorites, Learning and Profile/Settings — complete.
- Phase 4: generic Word Detail, Collins, Pronunciation and Dictionaries — complete.
- Phase 5: gate expansion, project rules and Paparazzi semantic smoke fixtures — complete.

The Paparazzi suite is intentionally a renderer/semantic-layout smoke layer rather than a claim of pixel-identical Apple UI. Physical-device screenshot assets in `screenshots/` remain release documentation and should only be replaced from a reviewed device build; they are not generated blindly in CI.

## Official references

- Apple Design: https://developer.apple.com/design/
- Human Interface Guidelines: https://developer.apple.com/design/human-interface-guidelines/
- Liquid Glass: https://developer.apple.com/documentation/technologyoverviews/liquid-glass
- Searching: https://developer.apple.com/design/human-interface-guidelines/searching
- Toolbars: https://developer.apple.com/design/human-interface-guidelines/toolbars
- Color: https://developer.apple.com/design/human-interface-guidelines/color
- Accessibility: https://developer.apple.com/design/human-interface-guidelines/accessibility

## Design decisions

1. **Content first.** Dictionary content and readable text are the visual foreground. Glass belongs primarily to navigation and interactive chrome, not every content card.
2. **Neutral glass by default.** Liquid Glass is not a blue material. Gdict brand blue is reserved for primary actions, links, selection and meaningful state.
3. **Familiar interactions.** Search is the primary task. Standard scrolling and tapping win over decorative pinch-to-zoom, drag-reorder and stagger choreography.
4. **One semantic system.** Background, surface, glass, separator, label and accent colors live in `GdictColors`; screens do not invent their own gradients.
5. **Legibility before translucency.** Android cannot reproduce Apple's system Liquid Glass compositor exactly. Compose surfaces therefore use adaptive translucent fills, separators and restrained elevation without pretending that `Modifier.blur()` is backdrop blur.
6. **Motion explains state.** Keep short state transitions; remove motion that exists only to decorate list entry or press feedback.
7. **Platform-respectful adaptation.** Navigation, touch targets and accessibility behavior remain appropriate for Android while following HIG principles of familiarity, simplicity, agency and clarity.

## Implemented phases and gates

### Phase 0 — Baseline and governance

- Recorded UI architecture and migration rules.
- Added `scripts/check-apple-hig-ui.sh`.
- Added the UI policy gate and Android unit/Paparazzi task to CI.

**Gate:** debug build + unit/Paparazzi smoke + UI policy script + shared core tests + Desktop compile.

### Phase 1 — Design foundation

- Converted Android colors to semantic neutral background/surface/glass roles.
- Removed global blue radial ambient material.
- Replaced fake Acrylic blur semantics with honest translucent surfaces.
- Global page/stagger/press decoration helpers no longer animate automatically.

**Gate:** no ambient radial foundation, fake blur API or page-level decorative motion on migrated surfaces.

### Phase 2 — Core path

- Rebuilt bottom navigation as restrained interaction chrome.
- Made Search the clear primary affordance.
- Removed Search pinch zoom, drag reorder and stagger entrance choreography.
- Kept recent searches, suggestions, clear history and meaningful brand-blue state.

**Gate:** Search forbids `detectTransformGestures`, `detectDragGestures`, `DragHandle` and `staggerEnterAnimation`; Search and app navigation use semantic colors.

### Phase 3 — Content destinations

- Favorites uses quiet list surfaces and semantic destructive actions.
- Learning preserves meaningful click-to-flip/reveal and rating feedback but removes swipe-only navigation, blue gradients and decorative glass.
- Profile/Settings uses a grouped system-settings hierarchy with explicit controls and separators.

**Gate:** migrated top-level screens use semantic surfaces, visible controls and approximately 48dp minimum interaction targets.

### Phase 4 — Detail and utility flows

- Generic Word Detail, Collins and Pronunciation share a neutral navigation/content hierarchy.
- Removed document-wide pinch zoom and decorative ambient/pulse material.
- Kept dictionary parsing, MDD audio, WebView fallback, cross-reference hooks and FSRS outside the visual refactor boundary.
- Dictionaries now uses a standard list, explicit switches/FAB, and standard dialogs instead of animated Fluent cards.

**Gate:** migrated screens cannot reintroduce hardcoded RGB colors, page-enter animation or detail-level transform gestures.

### Phase 5 — Regression and governance closure

- Expanded the UI gate across all migrated Android surfaces.
- Updated `.trae/rules/project_rules.md` so the HIG-inspired Android system is now authoritative.
- Replaced stale Fluent/Acrylic Paparazzi fixtures with semantic light/dark fixtures covering Search, Detail, Favorites, Learning, Dictionaries and Settings.
- CI remains the merge signal; screenshots used for release documentation require reviewed device capture rather than automatic replacement.

**Gate:** policy gate, Android unit/Paparazzi smoke, debug APK build, shared core tests and Desktop compile must all pass on the final head.

## Machine gate policy

The executable gate is `scripts/check-apple-hig-ui.sh`. It now covers the complete migrated Android surface set.

A gate exception must be documented here with: file, reason, user benefit, accessibility impact and planned removal date. There are currently no exceptions.
