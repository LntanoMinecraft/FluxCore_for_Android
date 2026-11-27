# Lntano Android 应用统一设计语言规范（Material Design 3 Expressive Edition）

> **适用范围**：本规范沉淀自 **云签助手（MhyCloud-Checkin）**、**录屏策略（ScreenPolicy）** 与 **PixelLoop** 三款项目的实战打磨，可直接作为通用设计文档附带给 AI Studio（Gemini）或开发工具，确保后续所有自研 Android 应用保持统一的 Google Pixel 原生级高级质感。

---

## 一、设计定位与核心原则

1. **原生旗舰质感**：严格遵循 **Google Material Design 3 Expressive** 与 Jetpack Compose 现代范式，视觉体验向 Pixel 原生系统工具（设置、文档、信息、录音机）看齐。
2. **极简克制、去 AI 味**：界面只保留高频必要元素与短促直白的文案，杜绝信息重复堆叠、花哨渐变边框与营销式套话。
3. **家族一致性**：所有应用共享同一套悬浮胶囊导航栏、Google Sans Flex 圆角字体、新版 Google 纯白底柔光渐变图标与标准「关于」二级页骨架。

---

## 二、色彩、主题与沉浸式规范

| 规范项 | 具体标准 |
| :--- | :--- |
| **动态取色** | 默认开启 **Material You 动态取色（Dynamic Color）**，支持在设置中一键开关 |
| **深浅色模式** | 采用 3 段式分段选择器：**`跟随系统 / 浅色 / 深色`** |
| **内置色盘** | 提供 **7 色圆形色盘选择器**（动态取色关闭或手动覆写时生效），选中项带外圆环与弹簧勾号动效 |
| **容器层级** | **禁用生硬的 `1dp` 灰色外框描边**；统一依靠 `surface` → `surfaceContainerLow` → `surfaceContainer` → `surfaceContainerHigh` 的色调明暗建立层级 |
| **沉浸式布局** | 全局启用 **Edge-to-Edge** 透出状态栏与底部手势条，所有可滚动列表必须正确处理 `statusBarsPadding()` 与 `navigationBarsPadding()` |

---

## 三、字体与排版体系（Typography）

* **英文、品牌名与数字**：优先加载系统内置的 **Google Sans Flex**（或免额外权限回退），并在 `FontVariation.Settings` 中显式开启其特有的**圆角可变轴：`FontVariation.Setting("ROND", 100f)`**，呈现柔和饱满的表现力。
* **中文排版**：自然回退至系统原生清晰黑体，确保中英混排视觉基线对齐、行高舒展。
* **等宽数字铁律**：所有时间码、进度、帧率、分辨率、计数徽章与版本号必须开启 **`fontFeatureSettings = "tnum"`**，防止数值实时刷新时发生左右抖动。

| 文本角色 | 推荐样式与字重 | 颜色与细节 |
| :--- | :--- | :--- |
| **页面大标题** | `headlineMedium` / `titleLarge` + `FontWeight.Bold` | `onSurface`，微紧字距 `-0.5sp` |
| **分组小标题** | `labelLarge` + `FontWeight.SemiBold` | `MaterialTheme.colorScheme.primary` |
| **卡片主标题** | `titleMedium` + `FontWeight.SemiBold` | `onSurface`，简洁不重复副标题内容 |
| **辅助说明 / 包名** | `bodySmall` + `FontWeight.Normal` | `onSurfaceVariant`，保持克制字数 |
| **状态胶囊 / 徽章** | `labelMedium` + `FontWeight.Medium` | 配合 `tnum`，字数严格控制在 2–4 字内，**严禁出现 `...` 截断** |

---

## 四、核心组件与动效规范

### 1. 标志性底部悬浮胶囊导航栏（Floating Pill Navigation Bar）

* **容器造型**：宽幅大圆角悬浮胶囊（`CircleShape` / `RoundedCornerShape(50)`），悬浮于系统手势条上方（`navigationBarsPadding() + 12dp`），表面采用 `surfaceContainerHigh` 配极淡边界（`outlineVariant.copy(alpha = 0.22f)`）与柔和抬升阴影。
* **标志性交互（未选中仅图标 → 选中弹性展开）**：
  * 未选中标签页**仅展示图标**；
  * 点击切换至某个标签页时，该项通过 `AnimatedVisibility`（`expandHorizontally` + `fadeIn`）**横向弹性展开为「图标 + 名称」的胶囊指示器**（`secondaryContainer`），其余项平滑收缩回纯图标。
* **弹簧物理参数**：导航栏容器尺寸变化（`animateContentSize`）、指示器展开、图标微缩放及底部抽屉展开统一使用富有弹性的 M3 Expressive 弹簧曲线（参考：`spring(dampingRatio = 0.62f, stiffness = 380f)`）。
* **底部安全留白铁律**：所有带悬浮导航栏的可滚动页面，列表底部必须统一预留至少 **`104dp + navigationBarsPadding()`** 的 `contentPadding`，确保最底部卡片或参数选项完整露出，绝不被悬浮栏遮挡。

### 2. 分组卡片与横向控件

* **分组圆角卡片**：统一采用大圆角（`RoundedCornerShape(24.dp)` 或 `28.dp`），内边距 `16dp–20dp`，无硬边框。
* **横向芯片行防截断**：帧率、分辨率、筛选标签等横向芯片行必须支持贯穿卡片内边距的 Edge-to-Edge 横向滚动（左右 `contentPadding = 16dp`）或改用 `FlowRow` 自适应换行，**禁止文字在卡片右边缘被生硬切掉半截**。
* **宽屏自适应**：平板或横屏等宽屏场景下，底部悬浮导航栏自动切换为左侧 `NavigationRail`。

### 3. 双语本地化（可选标准配置）

* 默认简体中文（`values/strings.xml`），同步提供地道克制的英文资源（`values-en/strings.xml`），全站严禁硬编码界面字符串。
* 在「设置 → 外观」提供 `跟随系统 / 简体中文 / English` 分段选择器，基于 Android 13+ 原生 `LocaleManager` 实现即时切换与系统级持久化。

---

## 五、新版 Google 风格自适应图标规范

所有自研应用图标统一遵循 **2026 新版 Google 系统图标美学**（对齐新版文档、信息、通讯录、我的 Pixel）：

1. **纯白底板**：`ic_launcher_background.xml` 统一采用纯净白色（`#FFFFFF`），确保在深色抽屉与浅色桌面上轮廓同样利落通透，不再使用易在黑底隐身的深色底板。
2. **严格留白安全区**：在 `108dp × 108dp` 自适应画布中，前景核心图形严格收敛在正中心 **`46dp × 46dp`（外接圆直径 ≤ 52dp）** 内，保证圆形/方圆形裁切后四周留白与 Google 原生图标完全一致，绝不贴边。
3. **无描边实心柔光渐变叠层**：
   * 禁用生硬细描边和脏阴影，使用 **1–2 个大圆角实心几何元素**，靠同色系柔光渐变与微透明交叠表现层次；
   * 推荐主色系：清透亮蓝 / 青蓝 → 谷歌蓝 → 柔和靛蓝/紫蓝渐变（例如 `#4DABF7` → `#4285F4` → `#7C5CFF`），不同应用通过几何主体造型做出清晰差异化。
4. **完整主题图标与回退资源**：
   * 必须提供清晰的负空间纯白单色层 **`ic_launcher_monochrome.xml`**；
   * 在 `mipmap-anydpi-v26/ic_launcher.xml` 与 `ic_launcher_round.xml` 中显式声明 `<monochrome>` 节点，完美适配系统**动态取色主题图标（Themed Icons）**，并同步生成 5 套分辨率（`mdpi`–`xxxhdpi`）的方形与圆形 PNG 回退图标。

---

## 六、标准「设置」与「关于」二级页骨架

1. **设置页结构**：
   * 分组清晰排列：核心业务配置 → **外观**（深浅色模式、语言、动态取色开关、7 色色盘）→ 配置管理（如有，优先采用系统 SAF 选择器免存储权限）→ 最底部放置 **「关于 `<应用名>`」入口卡片**（左侧信息图标 + 右侧动态版本号 `v${BuildConfig.VERSION_NAME}` 与进入箭头 `>`）。
2. **独立「关于」二级页（`AboutScreen`）**：
   * 点击入口平滑进入，自动隐藏底部悬浮导航栏，顶部仅保留带返回键的简洁 TopAppBar。
   * **上部/中部垂直居中主视觉区**（`Column` + `weight(1f)` 居中）：
     * `88dp–96dp` 圆角展示当前应用图标；
     * 居中 `headlineMedium` + `Bold` 展示应用名称；
     * 居中胶囊展示动态版本号（`v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})`）+ 一句 15 字以内的克制功能描述。
   * **页面最底部沉底区**（`navigationBarsPadding() + 20dp` 留白）：
     * 使用一个 `RoundedCornerShape(24.dp)`、`surfaceContainer` 分组卡片承载两条标准链接（均通过 `Intent.ACTION_VIEW` 打开，**无需申请 `INTERNET` 权限**）：
       1. **项目主页（GitHub）**：`https://github.com/LntanoMinecraft/<仓库名>`
       2. **开发者酷安**：`https://www.coolapk.com/u/34054336`（优先系统默认唤起酷安 App，无酷安时自动回退浏览器）。

---

## 七、去 AI 味文案红线

* **禁用词汇**：严禁在界面、副标题、空状态和关于页中使用「智能赋能、全方位守护、一键畅享、极致体验、无缝衔接、强大引擎」等 AI 宣传词。
* **信息零重复**：同一张卡片的主标题与副标题禁止重复展示相同信息（例如主标题已写应用名/游戏名，副标题只放脱敏账号或包名）。
* **克制短句**：按钮与开关标题控制在 2–6 字，辅助说明控制在 1 行以内，能靠视觉层级与图标表达的绝不堆字。

---

## 八、附：云编译与发包工程约定（CI/CD 速查）

新建项目接入 GitHub Actions（`.github/workflows/android-ci.yml`）时统一复用已验证的最佳实践：
* **免 Wrapper 二进制依赖**：使用 `actions/setup-java@v5` + `gradle/actions/setup-gradle@v4`（配置 `gradle-version: wrapper`），直接调用 `gradle` 命令，避免 AI Studio 导出缺少 `gradlew` / `gradle-wrapper.jar` 导致构建失败。
* **双轨版本号**：`versionCode` 自动读取 `GITHUB_RUN_NUMBER` 递增；`versionName` 日常 `push` 编 Debug 包用 `dev-<7位SHA>`，网页端发布 Release（`release: types: [published]`）时自动读取 `tag_name`（去除前缀 `v`）编译签名 Release APK 并挂载至 Release 附件。
* **Release Lint 避坑**：`app/build.gradle.kts` 配置 `lint { checkReleaseBuilds = false; abortOnError = false }`，编译命令附带 `-x lintVitalAnalyzeRelease --no-configuration-cache`。
