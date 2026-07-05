# 同步 Android 玻璃拟态 UI 到 Windows 桌面端 — 剩余计划

## 目标
完成 Phase 5-7：将 BookmarksScreen、FlashcardScreen、DictionariesScreen、SettingsScreen 从旧 Fluent 卡片风格重写为 Android 同款玻璃拟态风格，同步 SearchScreen 字符串资源，适配 DesktopApp 路由。

## 已完成
- Phase 1-4 已完成：公共组件（AcrylicComponents、AmbientBackground、Motion）、图标修复、WordDetailScreen、PronunciationDetailScreen、CollinsDetailScreen。
- SearchScreen 已有玻璃拟态背景和卡片，但有硬编码英文文案。

---

## Phase 5.1：BookmarksScreen 玻璃拟态化

**文件**：`desktop/app/src/main/kotlin/io/github/gdict/ui/screens/BookmarksScreen.kt`

### 改动点
1. **根布局**：`Column` → `Box` + 渐变背景 `bgGradient` + `acrylicAmbientBackground`。
2. **深色模式**：读取 `settingsViewModel.darkMode.collectAsState()`，全页面颜色适配。
3. **列表**：`LazyVerticalGrid` → `LazyColumn` 纵向列表（与 Android 一致）。
4. **BookmarkCard** → `BookmarkItemCard`：
   - 旧：`Card` + `CardDefaults` + 8dp 圆角
   - 新：`Box` + `shadow(4.dp, 24.dp)` + `clip` + `border(1.dp, borderColor, 24.dp)` + `background(glassBg)`
   - 图标容器：`48.dp` 圆形 + `Primary.copy(alpha = 0.12f)` 背景
   - 单词文字 `titleMedium` + `FontWeight.Bold`
5. **空状态**：图标容器改用 `PrimarySoft.copy(alpha = 0.1f)` + `PrimarySoft.copy(alpha = 0.6f)` tint。
6. **FlashcardPromoCard**：新增，玻璃卡片，显示"闪卡"标题 + 描述 + 右箭头，点击 `onFlashcardClick`。
7. **删除对话框**：`RoundedCornerShape(28.dp)` → 使用 `strings` 国际化文案。
8. **字符串**：硬编码 `"My Vocabulary"` → `strings.myVocabulary`，`"No favorites yet"` → `strings.noVocabularyYet`，`"Save words..."` → `strings.addWordsToFavoritesFirst` 等。
9. **新增 `onFlashcardClick` 参数**：从 DesktopApp 传入。

---

## Phase 5.2：FlashcardScreen 玻璃拟态化

**文件**：`desktop/app/src/main/kotlin/io/github/gdict/ui/screens/FlashcardScreen.kt`

### 改动点
1. **根布局**：`Column` → `Box` + 渐变背景 + `acrylicAmbientBackground`。
2. **深色模式**：读取 `settingsViewModel.darkMode.collectAsState()`。
3. **FlashcardStartView**：
   - 统计 `StatChip`：圆角从 8dp → 12dp，去除 border。
   - 开始按钮：`Card` → 玻璃 `Box`（shadow + clip + border + background）。
   - 硬编码 `"No vocabulary yet"` / `"All caught up!"` / `"Ready to review?"` → `strings`。
4. **FlashcardReviewView**：
   - 进度条：`LinearProgressIndicator` → 圆角胶囊 `Box`（12dp 圆角 + `border` + `background` + 渐变填充）。
   - 计数文字 `"Skip"` → `strings.skip`。
   - 闪卡主体：`Card` 8dp → `Box` 32dp 圆角 + `shadow(16.dp)` + `border(1.5.dp)` + `background(glassBg)` + `cameraDistance = 16f`。
   - `FlashcardFront`：`"Tap to reveal"` → `strings.tapToReveal`，增加玻璃胶囊翻转按钮。
   - `FlashcardBack`：柯林斯原生渲染（`CollinsSensesList` + `FrequencyDiamondsBlue`），单词 `headlineMedium` → `headlineMedium` + `FontWeight.Bold`。
5. **RatingButtonsRow**：
   - 旧：透明背景 + 边框
   - 新：玻璃 `Surface` 20dp 圆角顶部 + `glassBg`，按钮 `16dp` 圆角 + 彩色 `background(color.copy(alpha = 0.1f))`。
6. **FlashcardCompleteView**：
   - `"Session Complete!"` → `strings.sessionComplete`，`"Reviewed X of Y"` → `strings.reviewedOf`。
   - "Review Again" → `strings.reviewAgain`，按钮改为玻璃风格。
7. **柯林斯支持**：需要 import `isCollins3rdEntry`、`parseCollinsEntry`、`CollinsSensesList`、`FrequencyDiamondsBlue`（Android 端已有，桌面需确认是否已移植到独立文件）。

---

## Phase 5.3：DictionariesScreen 玻璃拟态化

**文件**：`desktop/app/src/main/kotlin/io/github/gdict/ui/screens/DictionariesScreen.kt`

### 改动点
1. **根布局**：`Column` → 外包 `Box` + 渐变背景 + `acrylicAmbientBackground`。
2. **深色模式**：读取 `settingsViewModel.darkMode.collectAsState()`，传给子组件。
3. **统计卡片**（第 171-205 行）：
   - 旧：`Card` + `CardDefaults` + 8dp 圆角
   - 新：玻璃 `Box`（shadow + 16dp 圆角 + border + glassBg），图标容器 `Primary.copy(alpha = 0.12f)`。
4. **DictionaryItemCard**：
   - 旧：`Card` + 8dp 圆角 + hover
   - 新：玻璃 `Box`（shadow + 16dp 圆角 + border + glassBg）
   - 图标容器 `RoundedCornerShape(12.dp)` + `PrimarySoft.copy(alpha = 0.1f)`
   - Switch 颜色匹配 Android：`checkedTrackColor = PrimarySoft`
5. **空状态**：图标容器改为 `PrimarySoft.copy(alpha = 0.1f)`，`"No dictionaries yet"` / `"Tap to add dictionaries"` 已接入 `strings`。
6. **AddDictionaryDialog**：`Card` 8dp → 玻璃 `Box` 32dp 圆角（参考 Android 的 `shadow(12.dp)` + `border` + `glassBg`）。输入框使用 `RoundedCornerShape(20.dp)`。"Scan Folder" 按钮改为玻璃胶囊样式。
7. **BatchImportDialog**：统一使用 `strings` 文案。
8. **诊断结果对话框**：圆角从 8dp → 24dp。
9. **删除确认对话框**：圆角 → 28dp。

---

## Phase 5.4：SettingsScreen 玻璃拟态化

**文件**：`desktop/app/src/main/kotlin/io/github/gdict/ui/screens/SettingsScreen.kt`

### 改动点
1. **根布局**：外层 `Box` + 渐变背景 + `acrylicAmbientBackground`。
2. **深色模式**：读取 `settingsViewModel.darkMode.collectAsState()`。
3. **ProfileHero**：
   - 旧：56dp 圆形头像
   - 新：72dp 玻璃圆形头像 + `shadow(8.dp)` + `border(1.dp, borderColor, CircleShape)` + `glassBg`
   - 标题 `30.sp`，副标题 `bodyMedium`
4. **SettingsSection**：
   - 旧：`Card` 8dp 圆角
   - 新：玻璃 `Box`（shadow + 28dp 圆角 + border + glassBg），左侧蓝色竖条指示器（4dp × 20dp，`Primary` 色）
5. **SettingsButtonItem**：
   - 新增 48dp 图标容器（`RoundedCornerShape(14.dp)` + `Primary.copy(alpha = 0.12f)` 背景）
   - 标题 `FontWeight.Bold`，描述颜色跟随深色模式
   - 右侧 `KeyboardArrowRight` 图标
6. **新增 SettingsSwitchItem**：用于深色模式开关，左侧图标容器 + Switch。
7. **DonationSection**：`Card` → 玻璃 `Box`（shadow + 28dp 圆角 + border + glassBg）。
8. **LanguageSelectionDialog**：保持 `AlertDialog`，但选中项使用 `PrimaryContainer` 背景。
9. **版本号**：硬编码 `"v1.0.0"` → 动态获取（如有 BuildConfig）。

---

## Phase 6：SearchScreen 字符串收尾

**文件**：`desktop/app/src/main/kotlin/io/github/gdict/ui/screens/SearchScreen.kt`

### 改动点
1. `"Search"` (第 146 行) → `strings.navSearch` 或 `strings.searchHint`
2. `"Search English Dictionary... Enter word or phrase"` (第 343 行) → `strings.searchHint`
3. `"No results for \"$query\""` (第 556 行) → 需新增 string key
4. `"Did you mean:"` (第 563 行) → 需新增 string key
5. `"Recent Searches"` (第 595 行) → `strings.recentSearches`
6. `"Word of the Day"` (第 639 行) → `strings.wordOfTheDay`
7. `"Dismiss"` (第 189 行) → `strings.close`
8. `"Start by adding a dictionary"` / `"Discover new words every day"` (第 651-652 行) → `strings.wordOfTheDayWelcome` / `strings.wordOfTheDayWelcomeDesc` 或 `strings.wordOfTheDayDictionary` / `strings.wordOfTheDayDictionaryDesc`

---

## Phase 7：DesktopApp 路由适配

**文件**：`desktop/app/src/main/kotlin/io/github/gdict/ui/DesktopApp.kt`

### 改动点
1. `BookmarksScreen` 调用处：传入 `onFlashcardClick = { showFlashcard = true }`。
2. `BookmarksScreen` 和 `SearchScreen` 目前未接收 `strings` 参数，需确认是否需要添加（如果 BookmarksScreen 的字符串通过 `strings` 参数传入而非 `stringResource`）。
3. **侧边栏颜色**：当前已使用 `GdictColors` + `MaterialTheme.colorScheme`，无需大改。

---

## 实施顺序

1. **BookmarksScreen**（最简单，先练手）
2. **FlashcardScreen**（中等复杂度，含柯林斯支持）
3. **DictionariesScreen**（中等复杂度，含对话框）
4. **SettingsScreen**（中等复杂度，含新增 SwitchItem）
5. **SearchScreen 字符串收尾**
6. **DesktopApp 路由适配**
7. **编译验证**

## 验证

```bash
./gradlew :desktop:app:compileKotlin
```

- [ ] BookmarksScreen：渐变背景 + 弥散光斑、玻璃卡片列表、FlashcardPromoCard、删除对话框。
- [ ] FlashcardScreen：开始页统计、复习卡片翻转、玻璃进度条、评分按钮、完成页。
- [ ] DictionariesScreen：统计卡片、词典项卡片、添加/批量导入/诊断对话框。
- [ ] SettingsScreen：ProfileHero、玻璃分组、图标容器、深色模式开关、语言对话框。
- [ ] SearchScreen：所有文案使用 strings。
- [ ] 深色模式：所有页面在深色模式下正确显示。
