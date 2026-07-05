# Tasks

- [x] Task 1: 整理二维码资源并移除爱发电相关代码
  - [x] SubTask 1.1: 复制支付宝、微信二维码图片到桌面端 `resources/donation/` 与安卓端 `res/drawable-nodpi/`
  - [x] SubTask 1.2: 删除桌面端 `AfdianClient.kt` 及 `SettingsScreen` 中的引用
  - [x] SubTask 1.3: 清理字符串资源中不再使用的爱发电文案（桌面 `ZhCnStrings.kt` / `EnStrings.kt`，安卓 `strings.xml`）

- [x] Task 2: 桌面端 SettingsScreen 捐助卡片改用二维码
  - [x] SubTask 2.1: 重新设计 `DonationSection`，展示两个二维码缩略图及标签
  - [x] SubTask 2.2: 实现点击放大对话框（居中、带关闭按钮、点击外部关闭）
  - [x] SubTask 2.3: 使用 `StringResources` 中已有的 `donationAlipay` / `donationWechat` 标签

- [x] Task 3: 安卓端 SettingsScreen 同步新增捐助二维码
  - [x] SubTask 3.1: 在设置页合适位置插入 `DonationSection`
  - [x] SubTask 3.2: 使用 `Image` / `painterResource` 加载 drawable 资源并支持点击放大
  - [x] SubTask 3.3: 添加/复用必要的字符串资源

- [x] Task 4: 修复柯林斯序号与词性对齐
  - [x] SubTask 4.1: 桌面端 `CollinsSensesList` 序号 Box 与 POS Box 添加 `contentAlignment = Alignment.Center`
  - [x] SubTask 4.2: 安卓端 `CollinsSensesList` 复核并修复同样问题

- [x] Task 5: 修复按钮内部白色区域
  - [x] SubTask 5.1: 检查桌面端 `PronActionButton` / `ActionButton`，移除内部多余背景/白色层
  - [x] SubTask 5.2: 检查安卓端 `PronActionButton` / `ActionButton`，修复同样问题
  - [x] SubTask 5.3: 将收藏/分享按钮外层 Box 背景从 `glassBg` 改为 `Color.Transparent`，保留边框与阴影

- [x] Task 6: 修复发音词典国旗与发音
  - [x] SubTask 6.1: 桌面端 `PronunciationDetailContent` 新增 `playAudio: (String?, String) -> Unit` 参数，`WordDetailScreen` 注入音频播放链路
  - [x] SubTask 6.2: 桌面端 `PronunciationRows` 渲染 `FlagBadge` 与 `SpeakerButton`
  - [x] SubTask 6.3: 移植/复用 `FlagBadge`、`SpeakerButton` 到桌面端
  - [x] SubTask 6.4: 安卓端检查发音国旗和点击回调是否正常工作
  - [x] SubTask 6.5: 桌面端 `CollinsDetailContent` 新增 `playAudio` 参数并注入 `WordDetailScreen`，修复 `SpeakerButton` 图标为 VolumeUp

- [x] Task 7: 其他词典背景一致
  - [x] SubTask 7.1: 修改 `HtmlContentBuilder`，对 `DefaultRenderer` 不注入 `BASE_CSS` 与 `fixInlineStyles`
  - [x] SubTask 7.2: 保持 `body` 背景透明/与应用一致，保留资源前缀与协议链接重写
  - [x] SubTask 7.3: 验证 Android 与桌面端 WebView 透明背景正常

- [x] Task 8: 柯林斯高级词典原生渲染（桌面端 + 安卓端）
  - [x] SubTask 8.1: 扩展 `isCollinsEntry` 识别柯林斯 Advanced 结构（class="hom" / sensenum / id="collins_english_dictionary"）
  - [x] SubTask 8.2: 新增 `parseCollinsAdvancedEntry` 解析器，提取词频、词形、发音、释义、例句
  - [x] SubTask 8.3: 在 `WordDetailScreen` / `FlashcardScreen` 中使用新的 `isCollinsEntry` 判断
  - [x] SubTask 8.4: 两端编译验证

- [x] Task 9: 修复柯林斯3rd序号与词性垂直居中
  - [x] SubTask 9.1: 桌面端 `CollinsSensesList` 序号/词性 Text 设置 `lineHeight = fontSize`
  - [x] SubTask 9.2: 安卓端 `CollinsSensesList` 同步设置 `lineHeight = fontSize`

# Task Dependencies
- Task 2 依赖 Task 1
- Task 3 依赖 Task 1
- Task 6 可并行于 Task 4 / Task 5
- Task 7 可并行于 Task 2 / Task 3
