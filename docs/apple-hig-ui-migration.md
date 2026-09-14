# Gdict Apple HIG UI Migration

> Scope: Android first. Desktop remains on its current Fluent-oriented shell until the Android design system and core flows pass the gates below. This is an Apple HIG-inspired adaptation for Jetpack Compose, not an attempt to make Android pretend to be iOS.

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

## Phase plan

### Phase 0 — Baseline and governance

- Record current UI architecture and migration rules.
- Add an executable UI policy gate.
- Make screenshot tests part of CI instead of leaving them as an unused test source.
- Remove stale snapshot code before treating Paparazzi as a merge signal.

**Gate:** debug compile + unit tests + UI policy script + Paparazzi task can run in CI.

### Phase 1 — Design foundation

- Convert `GdictColors` from Fluent-specific naming/behavior to semantic neutral backgrounds and glass surfaces while retaining compatibility aliases during migration.
- Replace ambient blue spot backgrounds with a quiet content background.
- Replace Acrylic components with honest glass surfaces; no fake `blurRadius` API.
- Establish a restrained motion policy.

**Gate:** no ambient-blue material in the migrated foundation; no unused blur API; light/dark tokens compile.

### Phase 2 — Core path

- Rebuild the app bottom navigation as a restrained floating glass control layer.
- Search remains the primary affordance.
- Remove result-list pinch zoom, drag reorder and stagger entry.
- Keep brand blue for selection and meaningful actions, not result titles and every control.
- Keep recent searches and suggestions discoverable and clearable.

**Gate:** `SearchScreen.kt` contains none of `detectTransformGestures`, `detectDragGestures`, `DragHandle`, or `staggerEnterAnimation`; no hardcoded RGB colors in Search or app navigation.

### Phase 3 — Content destinations

- Favorites: quiet list/content surfaces, destructive action remains semantically red.
- Learning: preserve meaningful flashcard flip/reveal interaction, remove decorative glass and excess entrance motion.
- Profile: use grouped settings hierarchy; reserve glass for chrome, not every row.

**Gate:** all top-level destinations share the semantic background/surface system; touch controls meet at least 48dp hit targets; user-facing strings are resources.

### Phase 4 — Detail and utility flows

- Word detail, Collins, Pronunciation and Dictionaries adopt the same navigation/content split.
- Remove document-level pinch zoom where it conflicts with standard scrolling/accessibility; provide explicit text sizing later if needed.
- Normalize dialogs, menus, back/share/bookmark controls and dark mode.

**Gate:** no screen-local blue-white background gradients; no ambient background helper usage; no unsupported gesture-only feature without an explicit control alternative.

### Phase 5 — Visual regression and documentation

- Record new Paparazzi goldens after deliberate visual review.
- Refresh README screenshots.
- Run light/dark visual pass and accessibility review.
- Only then update the project-wide rule from Fluent/Acrylic to this system and start Desktop parity work.

**Gate:** CI green, snapshots reviewed, screenshots refreshed, no unapproved UI-policy exceptions.

## Machine gate policy

The executable gate is `scripts/check-apple-hig-ui.sh`. It intentionally starts strict on the migrated core path and expands by phase. CI must run it before build verification.

A gate exception must be documented here with: file, reason, user benefit, accessibility impact and planned removal date. There are currently no exceptions.
