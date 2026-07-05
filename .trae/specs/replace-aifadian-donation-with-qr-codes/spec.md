# 替换爱发电捐助为支付宝/微信二维码 Spec

## Why
用户要求取消现有的爱发电捐助入口，改为在桌面端和安卓端设置页直接展示支付宝、微信收款二维码，并支持点击放大查看。同时修复柯林斯/发音词典详情页的若干 UI/交互 Bug。

## What Changes
- 桌面端 `SettingsScreen` 移除“爱发电赞助”按钮，改为展示支付宝、微信二维码卡片。
- 安卓端 `SettingsScreen` 新增与桌面端视觉一致的捐助二维码区域。
- 新增/复制用户提供的二维码图片到两端资源目录，并在捐助卡片中加载。
- 点击二维码缩略图弹出居中放大对话框，支持关闭/返回键/点击外部关闭。
- 移除桌面端 `AfdianClient` 的使用，并删除其源码与相关字符串。
- 修复柯林斯释义列表中序号圆圈与词性徽标不在同一水平线的问题。
- 修复按钮内部出现白色区域的问题。
- 修复发音词典（Cambridge EPD）详情页没有国旗、点击不发音的问题。
- 修改 `HtmlContentBuilder`，对非 Cambridge EPD / 柯林斯原生拦截的“其他词典”不再注入覆盖性 CSS，仅保持 body 背景与应用背景一致。

## Impact
- Affected specs: 设置页、捐助交互、词典详情页渲染、跨平台资源管理。
- Affected code:
  - 桌面：`SettingsScreen.kt`, `StringResources.kt`, `ZhCnStrings.kt`, `EnStrings.kt`, `AfdianClient.kt`, resources；`CollinsDetailScreen.kt`, `PronunciationDetailScreen.kt`, `WordDetailScreen.kt`, `MdxWebView.kt`。
  - 安卓：`SettingsScreen.kt`, `strings.xml`, resources；`CollinsDetailScreen.kt`, `PronunciationDetailScreen.kt`, `WordDetailScreen.kt`, `MdxWebView.kt`。
  - 共享：`HtmlContentBuilder.kt`, `DictionaryRenderer.kt`。

## ADDED Requirements

### Requirement: 捐助二维码展示
The system SHALL provide a donation section on both Android and Desktop Settings screens showing two QR code thumbnails (Alipay and WeChat).

#### Scenario: 桌面端展示
- **WHEN** 用户打开桌面端设置页
- **THEN** “支持开发者”区域显示支付宝、微信两个二维码缩略图及标签，不再有爱发电按钮

#### Scenario: 安卓端展示
- **WHEN** 用户打开安卓端设置页
- **THEN** 同桌面端一致的捐助卡片出现在设置页合适位置（建议在“关于”区域之前或作为独立区域）

#### Scenario: 点击放大
- **WHEN** 用户点击任一二维码缩略图
- **THEN** 弹出居中对话框，展示放大后的二维码，并提供关闭按钮；点击对话框外部或返回键可关闭

### Requirement: 二维码资源
The system SHALL bundle the two user-provided QR images with the app.

#### Scenario: 资源路径
- **GIVEN** 用户提供 `d:\workspace\Gdict\1783150248538.jpg`（支付宝）和 `d:\workspace\Gdict\mm_facetoface_collect_qrcode_1783150236572.png`（微信）
- **THEN** 桌面端资源复制到 `desktop/app/src/main/resources/donation/alipay_qr.jpg` 与 `desktop/app/src/main/resources/donation/wechat_qr.png`
- **THEN** 安卓端资源复制到 `android/app/src/main/res/drawable-nodpi/donation_alipay.jpg` 与 `android/app/src/main/res/drawable-nodpi/donation_wechat.png`（或同等合理的 drawable 目录）

## MODIFIED Requirements

### Requirement: 移除爱发电捐助
**Reason**: 用户取消原有爱发电方式。
**Migration**: 删除桌面端 `AfdianClient` 及引用；移除 `SettingsScreen` 中 `AfdianClient.openSponsorPage()` 调用；字符串中移除/替换相关文案。

### Requirement: 柯林斯序号与词性对齐
The system SHALL render Collins sense number badge and POS badge with their vertical centers on the same line.

#### Scenario: 桌面端修复
- **GIVEN** 桌面端 `CollinsSensesList` 的序号 Box 与词性 Box
- **THEN** 两者均设置 `contentAlignment = Alignment.Center`，`Row` 使用 `Alignment.CenterVertically`，确保视觉中心对齐

#### Scenario: 安卓端复核
- **GIVEN** 安卓端 `CollinsSensesList`
- **THEN** 复核并修复同样问题（当前缺少 contentAlignment 时补齐）

### Requirement: 按钮背景无多余白色
The system SHALL render action buttons with a single uniform glass background and no internal white stripe/area.

#### Scenario: 桌面端与安卓端
- **GIVEN** `PronActionButton` / `ActionButton`
- **THEN** 仅外层 `Box` 使用 `glassBg` 作为背景
- **THEN** 内部 `Row` 不设置任何背景色，不使用 `Surface` 或额外白色 Box 包裹
- **THEN** 阴影、边框、背景顺序正确，避免透明层叠加产生白条

### Requirement: 发音词典国旗与发音
The system SHALL display UK/US flags and play audio when the speaker icon is tapped in Cambridge EPD (pronunciation) dictionary detail.

#### Scenario: 桌面端
- **GIVEN** 桌面端 `PronunciationDetailContent`
- **THEN** 在 `PronunciationRows` 中每行渲染国旗徽标（复用/移植 `FlagBadge`）
- **THEN** 每行提供喇叭按钮（复用/移植 `SpeakerButton`）
- **THEN** 点击喇叭调用与 `WordDetailScreen` 一致的音频播放链路（MDD 资源 → Edge TTS）
- **THEN** `PronunciationDetailContent` 新增 `playAudio: (String?, String) -> Unit` 参数，`WordDetailScreen` 负责注入

#### Scenario: 安卓端
- **GIVEN** 安卓端已存在国旗与发音
- **THEN** 检查并修复可能的国旗不显示或点击无音问题（如回调未传递、解析失败）

### Requirement: 其他词典背景一致
The system SHALL not override CSS for dictionaries other than Cambridge EPD / Collins native-rendered ones, only keeping the page background consistent with the app background.

#### Scenario: HtmlContentBuilder 行为
- **GIVEN** 非特殊词典
- **THEN** `HtmlContentBuilder` 不注入 `BASE_CSS`
- **THEN** 不调用 `fixInlineStyles()`
- **THEN** 不删除原始 `<link rel="stylesheet">`
- **THEN** 仅注入一段最小 CSS 设置 `body { background: transparent; margin:0; padding:6px 10px; }`（移动端 WebView 已透明，桌面端背景由应用背景承担）
- **THEN** 资源前缀重写、`entry://` / `sound://` 协议链接仍保留

## REMOVED Requirements

### Requirement: 爱发电按钮
**Reason**: 用户取消原捐助方式。
**Migration**: 替换为支付宝/微信二维码入口。原有 `AfdianClient` 删除。
