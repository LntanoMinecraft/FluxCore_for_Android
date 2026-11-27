# 流枢（FluxCore）重构与功能升级工程规范文档

> **文档用途**：本工程文档面向 **Google AI Studio（Gemini 4 Neon）**，请与配套附带的通用规范《Lntano Android 应用统一设计语言规范（Material Design 3 Expressive Edition）》共同执行。
> **项目性质**：基于 MIT 开源项目 [MOUAJMD/FluxCore_for_Android](https://github.com/MOUAJMD/FluxCore_for_Android) 派生的独立维护版本，目标仓库为 [LntanoMinecraft/FluxCore_for_Android](https://github.com/LntanoMinecraft/FluxCore_for_Android)。

---

## 一、项目定位与配套文档适用边界

1. **核心定位**：面向 Root 用户的轻量级 Android 系统级情境自动化引擎，完整继承原仓库（`v1.2.0`）的全部 Root 自动化能力，并升级为现代 Jetpack Compose + Material Design 3 Expressive 架构。
2. **与两份通用工程文档的协同边界**：
   * **《Lntano Android 应用统一设计语言规范（M3 Expressive Edition）》**：**全量强制执行**（色彩、动态取色、7 色色盘、Google Sans Flex `ROND = 100` 与 `tnum`、底部悬浮胶囊导航栏、新版 Google 纯白底柔光渐变图标、设置与关于二级页骨架、去 AI 味文案红线、GitHub Actions CI/CD 约定）。
   * **《LSPosed 系统组件 Hook 模块通用工程开发规范》**：**仅参考工程目录组织、R8 混淆与依赖瘦身、GitHub Actions 双轨云编译与版本号解耦策略**。**FluxCore 是纯 Root（`su`）自动化应用，不是 LSPosed 模块**——严禁引入 `libxposed` 依赖、`META-INF/xposed/` 元数据或任何 Xposed Hook 代码。
3. **开源协议与版权红线**：仓库根目录 `LICENSE` 保留 MIT 协议正文与原作者版权声明 `Copyright (c) 2026 MOUAJMD`，并在其下方追加 `Copyright (c) 2026 LntanoMinecraft`。

---

## 二、应用名称、多语言与图标重构规范

### 1. 应用名称单语显示（严禁双语拼接）

彻底移除当前界面与桌面中「`流枢 · FluxCore`」同时出现的双语拼接写法，全站所有标题、桌面图标名称、系统应用信息页与关于页均严格读取 `R.string.app_name`：

| 语言环境 | 资源目录 | `app_name` 取值 |
| :--- | :--- | :--- |
| **简体中文（默认）** | `res/values/strings.xml`（及 `values-zh`） | `流枢` |
| **英文** | `res/values-en/strings.xml` | `FluxCore` |

* 在「设置 → 外观 / 语言」中提供 `跟随系统 / 简体中文 / English` 切换，基于 Android 13+ 原生 `LocaleManager` 实现即时生效与系统级持久化。

### 2. 新版 Google 风格自适应图标

替换原有深紫色圆底 `F.` 图标，严格对齐 2026 新版 Google 系统图标美学：

* **纯白底板**：`ic_launcher_background.xml` 使用纯净白色 `#FFFFFF`。
* **严格留白安全区**：在 `108dp × 108dp` 自适应画布中，核心前景图形收敛在正中心 `46dp × 46dp`（外接圆直径 ≤ 52dp）内，杜绝贴边裁切。
* **无描边实心柔光渐变几何**：采用 1–2 个大圆角实心几何体表达「流枢（流体与节点枢纽）」意象，使用清透青蓝 `#4DABF7` → 谷歌蓝 `#4285F4` → 柔和紫蓝 `#7C5CFF` 柔光渐变叠层，禁止细描边与脏阴影。
* **完整主题图标与回退资源**：提供负空间纯白单色层 `ic_launcher_monochrome.xml`，在 `mipmap-anydpi-v26/ic_launcher.xml` 与 `ic_launcher_round.xml` 中声明 `<monochrome>` 节点，并同步生成 `mdpi` 至 `xxxhdpi` 5 套方形与圆形 PNG 回退图标。

---

## 三、整体 UI 架构与精简重构

针对现有界面（情境页、弹窗编辑器、控制台、设置页）存在的视觉堆叠与交互局促问题，统一按 M3 Expressive 规范重构：

### 1. 顶部导航与底部悬浮胶囊导航栏

* **精简冗余头部**：取消在每个 Tab 顶部重复堆砌双语大标题与固定状态胶囊的做法。仅在「情境」首页顶部展示单语应用名与精简状态徽章（`Root 就绪` / `监听中`，2–4 字，开启 `tnum`，禁止 `...` 截断）；其他一级页面仅显示清晰简洁的页面大标题（`控制台`、`日志`、`设置`）。
* **标志性底部悬浮胶囊导航栏（Floating Pill Navigation Bar）**：
  * 一级页面设为 4 个 Tab：**情境**、**控制台**、**日志**、**设置**。
  * 未选中 Tab **仅展示图标**；选中时通过 `AnimatedVisibility`（`expandHorizontally` + `fadeIn`）与 M3 Expressive 弹簧曲线（`dampingRatio = 0.62f, stiffness = 380f`）横向弹性展开为「图标 + 名称」胶囊指示器（`secondaryContainer`）。
  * 悬浮于系统手势条上方（`navigationBarsPadding() + 12dp`），所有一级可滚动列表底部强制预留至少 `104dp + navigationBarsPadding()` 安全留白，FAB（`+ 新建情境`）避让悬浮栏不重叠。
  * 进入任意二级子页面（规则编辑器、自定义颜色、深浅色模式、使用教程、关于页）时自动隐藏底部悬浮栏，顶部仅保留带返回键的简洁 TopAppBar。
* **视觉层级与去冗余**：禁用 `1dp` 灰色硬边框，统一使用 `surface` → `surfaceContainerLow` → `surfaceContainer` → `surfaceContainerHigh` 建立层次；统一圆角卡片 `RoundedCornerShape(24.dp)`，精简卡片内重复说明文字。

---

## 四、核心功能保留与新增模块详设

### 1. 完整继承的既有 Root 自动化内核

保留并稳固原项目（`MOUAJMD/FluxCore_for_Android`）的全部核心能力，确保既有配置兼容：

* **事件驱动监听引擎**：硬件、电量、有线耳机、Wi-Fi/蓝牙与屏幕旋转采用系统广播零轮询响应；前台应用切换采用持久化 Root 通道探测系统事件流与活动栈，兼容 Android 13–16+，息屏自动休眠，零额外待机耗电。
* **10 类触发条件（IF）**：仅手动触发、打开/退出应用（进阶模式支持多应用集合）、充电器连接、亮屏/息屏、Wi-Fi 连接（进阶模式支持指定 SSID 与一键读取当前）、蓝牙设备（进阶模式支持名称关键字）、电量阈值（`1%–100%`）、有线耳机插拔、屏幕横竖屏、时间段（支持跨零点）；进阶模式支持多条件 `AND` 组合。
* **系统级执行动作（THEN）与状态快照（Auto-Revert）**：快捷设置磁贴切换（`cmd statusbar click-tile`）、强行停止后台应用（`am force-stop`）、勿扰模式、刷新率锁定（`60Hz/90Hz/120Hz`）、固定亮度（`0–255`）、媒体音量（`0%–100%`）、网络与系统开关（Wi-Fi、移动数据、飞行模式、原生省电、深色主题、自动旋转/锁定竖屏）、触发震动提示、自定义进入与退出 Root Shell 指令；激活前自动保存快照，退出条件或手动关闭时无缝还原。
* **控制台能力**：一键深度后台净化（强停第三方后台应用并释放页面缓存 `drop_caches`）与快捷 Root Shell 终端（实时回显 stdout/stderr）。

### 2. 新增：规则动作流程可视化与滑块拖拽排序

将原有的局促弹窗编辑器重构为**独立全屏二级编辑页（`RuleEditorScreen`）**，并引入可排序的执行流水线：

* **动作流水线数据结构**：将每条情境规则的执行动作由固定字段升级为**有序步骤列表（`List<ActionStep>`）**（保留对旧版 JSON 备份的向后兼容反序列化，自动按默认顺序迁移）。
* **滑块/手柄拖拽上下排序**：
  * 已添加的执行动作（如「强行停止应用」「锁定 120Hz 刷新率」「执行 Root Shell 指令」「切换状态栏磁贴」等）以独立圆角步骤卡片垂直排列，左侧或右侧配备清晰的**拖拽手柄（Drag Handle 图标）**与步骤序号徽章（`1`、`2`、`3`…，开启 `tnum`）。
  * 支持长按或按住手柄**上下往复拖拽排序**，拖拽过程具备跟手抬升阴影、平滑位移动画，且在「触摸反馈引擎」开启时触发细腻的刻度震动反馈。
  * 同步提供辅助「上移 / 下移」快捷操作或删除按钮，兼顾单手操作精度。
* **严格按序执行与安全还原**：触发情境时，引擎严格按照用户排定的顺序依次执行各步骤（支持步骤间可选短延迟或串行等待）；退出情境自动还原时，按逆序执行自定义退出脚本并恢复系统状态快照。

### 3. 新增：Root Shell 自动扫描与本地脚本检索

在「规则编辑器 → 自定义 Root Shell 指令」及「控制台 → 快捷 Root Shell 终端」中，新增**本地 Root Shell 自动搜索与导入**能力：

* **自动扫描范围**：点击「从手机搜索脚本」后，通过已有 Root 通道异步扫描设备常见脚本目录，包括但不限于：
  * `/storage/emulated/0/Download/`、`/storage/emulated/0/Documents/`、`/storage/emulated/0/Scripts/`
  * `/data/adb/service.d/`、`/data/adb/post-fs-data.d/`、`/data/adb/modules/`（限深度检索 `.sh` 文件，排除系统大目录防卡顿）
* **交互与安全确认**：
  * 展示扫描到的 `.sh` 文件列表（显示文件名、所属路径、文件大小与修改时间），支持实时关键词过滤与按目录筛选。
  * 点击任意脚本可先**只读预览脚本文本内容**，用户可选择「直接引用路径执行（`sh /path/to/script.sh`）」或「将脚本内容填入命令输入框」进行二次编辑，避免盲目执行未知脚本。
  * 同时内置少量高频只读/实用系统命令快捷预设（如 `dumpsys battery`、`wm size`、`cmd statusbar` 查询等），支持一键填充。

### 4. 新增：控制中心快捷磁贴（QS Tile）与桌面快捷方式一键触发

支持将任意单条情境规则固定到系统下拉控制中心或主屏幕桌面，点击后**无需打开应用主界面即可直接激活或还原该情境**：

* **触发入口**：在情境首页每条规则卡片的快捷操作菜单（如「更多」按钮）以及规则编辑页顶部/底部，均提供**「添加到控制中心磁贴」**与**「添加到桌面快捷方式」**两项操作。
* **控制中心磁贴（Quick Settings Tile）**：
  * 在 `AndroidManifest.xml` 中预置 **6 个可动态启用的情境磁贴槽位**（`FluxCoreTileSlot1Service` 至 `FluxCoreTileSlot6Service`，默认 `android:enabled="false"`，仅在绑定情境时通过 `PackageManager.setComponentEnabledSetting` 启用，避免系统编辑栏出现大量空白占位磁贴）。
  * 点击「添加到控制中心磁贴」时，自动分配空闲槽位绑定规则 ID；在 Android 13+（API 33+）上调用原生 `StatusBarManager.requestAddTileService(...)` 直接拉起系统添加确认弹窗，一键放入下拉控制中心（低版本则引导用户在控制中心编辑栏中拖入）。
  * **状态实时同步**：磁贴主标签动态显示绑定的情境名称，副标签（`subtitle`）显示「运行中 / 待机」；点击磁贴直接切换该情境的激活与还原状态，磁贴高亮状态（`STATE_ACTIVE` / `STATE_INACTIVE`）与后台引擎实时同步。规则被删除或解绑时自动释放并停用对应槽位。
* **桌面快捷方式（Pinned Home Screen Shortcut）**：
  * 使用 `ShortcutManagerCompat.requestPinShortcut(...)` 拉起系统原生「添加到主屏幕」弹窗，以该情境名称作为默认图标标题（支持确认前自定义短名），采用新版纯白底柔光渐变风格单色/场景图标。
  * **无界面静默中转**：点击桌面快捷方式时，启动透明无界面的中转 Activity（`ShortcutTriggerActivity`，配置 `Theme.Translucent.NoTitleBar` 与 `android:excludeFromRecents="true"`），直接通知引擎执行该情境的激活/还原切换，给出简洁 Toast 提示（并在「触摸反馈引擎」开启时触发短震动）后立即 `finish()`，不打断用户当前页面。
  * **生命周期同步**：情境重命名时同步调用 `ShortcutManagerCompat.updateShortcuts(...)` 更新桌面名称；情境删除时调用 `disableShortcuts(...)` 标记失效并提示用户。

### 5. 新增：实时运行日志页面（`LogScreen`）

将日志能力从设置页的单一导出按钮升级为独立的一级 Tab 页面，方便实时排障：

* **实时日志流**：基于内存环形缓冲区（`StateFlow<List<LogEntry>>`，例如保留最近 2000 条）与持久化日志文件联动，实时展示引擎启动状态、Root 通道状态、前台应用切换探测、规则命中/还原记录、磁贴/快捷方式触发记录、各有序动作执行结果与 Shell 标准输出/报错信息。
* **筛选与阅读体验**：
  * 支持按日志级别筛选芯片（`全部` / `信息 INFO` / `警告 WARN` / `错误 ERROR`），错误与警告使用 M3 语义色（`errorContainer` / `tertiaryContainer`）区分高亮。
  * 支持顶部关键词搜索、时间戳使用等宽数字（`tnum`）、提供「自动滚动到底部 / 暂停滚动」「复制单条或全部日志」「清空日志」操作。
* **一键导出日志**：保留并优化导出 `.txt` 日志功能（支持系统 SAF 选择保存位置或默认保存至 `/storage/emulated/0/Download/FluxCore/`，导出完成后以 Snackbar 提示结果）。

### 6. 新增：全局触摸反馈引擎开关（Haptic Feedback Engine）

* 在「设置」中新增独立开关项：**触摸反馈引擎**（默认开启）。
* 封装全局统一触感管理器（`HapticEngine`），基于 Android 现代线性马达常量（`HapticFeedbackConstants.CONFIRM` / `CLOCK_TICK` / `SEGMENT_TICK` / `GESTURE_START` / `REJECT` 等，按 API 级别优雅降级）。
* **严格联动控制**：
  * **开启状态**：全应用按钮点击、导航栏切换、开关拨动、色盘选择、动作列表拖拽换位均提供克制清脆的线性马达震动反馈。
  * **关闭状态**：全应用交互控件彻底静默，不触发任何触感震动（不影响用户主动在某条情境规则内配置的“规则激活震动提示”动作，二者语义清晰区分）。

---

## 五、设置页面大改与独立二级子页面规划

彻底重构原设置页的 3 张臃肿卡片，改为清晰克制的 M3 Expressive 分组列表，将复杂配置下沉至独立二级页：

### 1. 设置主页（`SettingsScreen`）分组结构

1. **核心与交互分组**：
   * **进阶模式**（Switch 开关）：开启后解锁多应用集合、指定 Wi-Fi/蓝牙及多条件 `AND` 组合。
   * **触摸反馈引擎**（Switch 开关）：控制全局按钮与交互控件的线性马达震动反馈。
2. **外观与个性化分组**：
   * **深浅色模式**（点击进入二级页 `ThemeModeScreen`，右侧显示当前状态如 `跟随系统` / `浅色` / `深色`）。
   * **自定义颜色**（点击进入二级页 `ColorPaletteScreen`，右侧显示当前配色圆点或 `动态取色`）。
   * **应用语言**（点击或分段选择：`跟随系统` / `简体中文` / `English`）。
3. **数据与帮助分组**：
   * **配置备份与恢复**：导出 `.json` / 导入 `.json`（支持「合并追加」与「覆盖恢复」，优先使用 SAF 选择器并兼容 `/storage/emulated/0/Download/FluxCore/`）。
   * **使用教程**（点击进入二级页 `TutorialScreen`）。
4. **关于分组（最底部）**：
   * **关于流枢 / About FluxCore**（左侧信息图标 + 右侧动态版本号 `v${BuildConfig.VERSION_NAME}` 与进入箭头 `>`，点击进入独立二级页 `AboutScreen`）。

### 2. 独立二级页详设

#### A. 深浅色模式页面（`ThemeModeScreen`）
* 顶部提供简洁主题视觉预览卡，展示当前深浅色下的卡片与控件层次。
* 提供三项单选卡片：**跟随系统**（自动随系统深浅色切换）、**浅色模式**、**深色模式**，切换时实时无闪烁生效并持久化保存。

#### B. 自定义颜色页面（`ColorPaletteScreen`）
* **动态取色开关**：支持 Android 12+ Material You 系统壁纸动态取色（默认开启）。
* **7 色圆形色盘选择器**：提供 7 套精心调校的 M3 种子色（如谷歌蓝、靛紫、青绿、琥珀、珊瑚红、鼠尾草绿、暖灰等），选中项带外圆环与弹簧勾号动效；关闭动态取色或点击色盘时立即接管全局主题色，并在页面顶部提供小型组件预览卡实时查看按钮、开关与容器配色效果。

#### C. 使用教程页面（`TutorialScreen`）
* 以清晰的分步卡片形式向新用户与进阶用户说明完整用法（支持中英文双语，杜绝 AI 营销套话）：
  1. **Root 授权与状态确认**：如何允许 `su` 权限并确认顶部状态标签。
  2. **后台保活关键步骤**：如何关闭电池优化（设为「无限制」）并在最近任务中锁定应用，避免被系统墓碑机制查杀。
  3. **创建规则与拖拽排序动作**：讲解 IF 触发条件、进阶模式多条件 `AND` 逻辑，以及如何通过拖拽手柄自定义 THEN 动作的执行顺序与 Auto-Revert 自动还原机制。
  4. **控制中心磁贴与桌面快捷方式**：如何将常用情境一键添加到下拉控制中心或主屏幕，随时免打开应用直接开关。
  5. **Root Shell 脚本检索与调试**：如何自动扫描手机本地 `.sh` 脚本、安全预览并导入规则，以及如何配合「实时日志」排查命令报错。
  6. **配置迁移与覆盖升级**：如何备份/恢复 `.json` 规则及覆盖安装后的重启服务建议。

#### D. 关于页面（`AboutScreen`）
严格遵循《Lntano Android M3 Expressive Design System》第六章标准骨架，同时完整保留上游原作者信息与独立维护者信息：

* **顶部栏**：简洁 TopAppBar + 返回键，隐藏底部悬浮胶囊导航栏。
* **上部/中部垂直居中主视觉区**（`Column` + `weight(1f)` 居中）：
  * `88dp–96dp` 圆角展示新版应用图标；
  * 居中 `headlineMedium` + `Bold` 展示单语应用名称（`流枢` / `FluxCore`）；
  * 居中胶囊展示动态版本号（`v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})`）+ 一句 15 字以内的克制功能描述（如「Root 系统级情境自动化引擎」/ `Root-powered Android automation engine`）；
  * 一行克制的协议与派生标注：`基于 MIT 开源协议 · 上游项目独立维护分支`。
* **页面最底部沉底区**（`navigationBarsPadding() + 20dp` 留白）：
  * 使用 `RoundedCornerShape(24.dp)`、`surfaceContainer` 分组卡片清晰呈现以下 4 条可点击跳转链接（均通过 `Intent.ACTION_VIEW` 打开，**零 `INTERNET` 权限**）：
    1. **当前项目仓库（GitHub）**：`https://github.com/LntanoMinecraft/FluxCore_for_Android`（副标题：`LntanoMinecraft · 独立维护`）
    2. **当前维护者酷安**：`https://www.coolapk.com/u/34054336`（副标题：`Lntano`）
    3. **上游原项目仓库（GitHub）**：`https://github.com/MOUAJMD/FluxCore_for_Android`（副标题：`原作者 MOUAJMD · MIT 协议`）
    4. **原作者酷安主页**：`https://www.coolapk.com/u/39291006`（副标题：`@C_AJMD_W`）

---

## 六、自动化工作流（GitHub Actions）、签名发包与双轨版本号策略

采用生产级双轨 CI/CD 方案，所有配置与规则均自包含、可直接落地，实现 AI Studio 本地预览与 GitHub 云端自动编译、签名、发包的彻底解耦。

### 1. 双轨版本号解耦策略（`app/build.gradle.kts`）

* **SDK 与编译基线**：`compileSdk = 36`、`targetSdk = 36`（统一使用标准整数，避免云端缺失小版本 SDK）；开启 `buildConfig = true` 供设置页与关于页动态读取 `BuildConfig.VERSION_NAME` 与 `BuildConfig.VERSION_CODE`。
* **AI Studio 兼容铁律**：`defaultConfig` 内的 `versionCode` 与 `versionName` 必须保留为干净的静态字面量（如 `versionCode = 10`、`versionName = "2.0.0"`），**严禁在 `defaultConfig` 前插入内联 Lambda**，确保 AI Studio 内置静态解析器正常工作。
* **GitHub Actions 动态覆写**：仅当检测到 `System.getenv("GITHUB_ACTIONS") == "true"` 时，通过底部独立辅助函数或 `androidComponents.onVariants` 动态覆写：
  * **`versionCode`**：`10 + (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1)`（确保单调递增且始终高于原版 `v1.2.0`）。
  * **`versionName`**：
    * **Release 发行版轨道**：读取环境变量 `RELEASE_VERSION`（由工作流从 `github.event.release.tag_name` 去除前缀 `v` 注入，例如标签 `v2.0.0` → `2.0.0`）。
    * **日常开发构建轨道**：默认生成 `dev-<7位短SHA>`（例如 `dev-a1b2c3d`）。

### 2. R8 混淆、依赖瘦身与 Lint 避坑（AGP 9 / Gradle 9）

* **依赖瘦身**：彻底清理 AI Studio 模板中未使用的重型库（如 Firebase、Room、Retrofit、CameraX 等），仅保留不打入 APK 的根目录 `metadata.json`。
* **Release 优化**：`buildTypes.release` 启用 `isMinifyEnabled = true` 与 `isShrinkResources = true`，配置 `proguard-rules.pro` 保留序列化模型与 Manifest 组件类。
* **AGP 9 / Gradle 9 Lint 避坑**：`app/build.gradle.kts` 中配置：

```kotlin
lint {
    checkReleaseBuilds = false
    abortOnError = false
}
```

且云编译命令直接执行 `gradle assembleRelease --no-configuration-cache`，**严禁附加 `-x lintVitalAnalyzeRelease`**（Gradle 9 下该任务未注册，附加会导致 `Task not found` 失败）。

### 3. 签名配置（GitHub Secrets 对接）

在 `app/build.gradle.kts` 的 `signingConfigs` 中创建 `release` 签名配置，仅当环境变量与密钥文件存在时自动启用（未配置 Secrets 时 Debug 构建不受影响）：

| GitHub Secret 名称 | 用途说明 |
| :--- | :--- |
| `SIGNING_KEY_BASE64` | `.jks` 签名密钥库的 Base64 编码字符串 |
| `SIGNING_STORE_PASSWORD` | 密钥库密码（`storePassword`） |
| `SIGNING_KEY_ALIAS` | 密钥别名（默认沿用 `upload`） |
| `SIGNING_KEY_PASSWORD` | 密钥密码（`keyPassword`） |

### 4. GitHub Actions 双轨工作流（`.github/workflows/android-ci.yml`）

完整实现以下两条自动化构建与发包轨道，免依赖 `gradlew` / `gradle-wrapper.jar` 二进制文件：

* **触发条件**：
  * `push`（推送到 `main` 分支）
  * `workflow_dispatch`（支持网页端手动触发）
  * `release`（`types: [published]`，在 GitHub 网页端发布 Release 时自动触发）
* **工具链标准**：使用 `actions/checkout@v4`、`actions/setup-java@v5`（Java 17 / Temurin）与 `gradle/actions/setup-gradle@v4`（配置 `gradle-version: wrapper`，直接执行 `gradle` 命令）。
* **轨道 A（日常 `push` / 手动触发）**：
  * 注入 `RELEASE_VERSION=dev-${GITHUB_SHA::7}`；
  * 执行 `gradle assembleDebug --no-configuration-cache`；
  * 将生成的 APK 重命名为 `FluxCore-dev-<7位SHA>-debug.apk`，通过 `actions/upload-artifact@v4` 上传供随时下载测试。
* **轨道 B（正式 Release 发包）**：
  * 仅在 `github.event_name == 'release'` 时运行（赋予 `permissions: contents: write`）；
  * 提取 `TAG="${GITHUB_REF_NAME}"`，注入 `RELEASE_VERSION="${TAG#v}"`；
  * 将 `SIGNING_KEY_BASE64` 解码写入临时 `.jks` 密钥库文件（编译完成后自动清理）；
  * 执行 `gradle assembleRelease --no-configuration-cache`；
  * 将已签名成品重命名为 `FluxCore-${TAG}-release.apk`，并生成对应 `FluxCore-${TAG}-release.apk.sha256` 校验文件；
  * 使用 `softprops/action-gh-release@v2` 自动将签名 APK 与 SHA-256 文件挂载发布至当前 GitHub Release 附件。

---

## 七、权限边界与交付验收清单

1. **权限最小化**：不申请 `INTERNET` 权限（所有外部链接均通过 `Intent.ACTION_VIEW` 交由系统浏览器或酷安 App 打开）；配置/日志读写优先结合 SAF 与 Root 通道，减少不必要的敏感权限弹窗。
2. **交付验收要点**：
   * 中英文切换时，桌面图标名、TopAppBar、关于页均只显示单语名称（`流枢` 或 `FluxCore`）；
   * 桌面图标为纯白底柔光渐变新版 Google 风格，且在系统主题图标开启时正常渲染单色 `<monochrome>` 层；
   * 规则编辑器内可通过拖拽手柄顺滑上下排序执行动作，保存后引擎按新顺序执行；
   * Root Shell 扫描能列出本地 `.sh` 脚本并支持预览与一键填入；
   * 任意情境支持一键添加到控制中心快捷磁贴（Android 13+ 原生弹窗添加，状态实时同步）或主屏幕快捷方式（无界面透明中转静默触发）；
   * 实时日志页能动态刷新引擎事件与报错信息并正常导出 `.txt`；
   * 设置页的深浅色模式、自定义颜色（动态取色 + 7 色盘）、使用教程、触摸反馈引擎开关与关于页（含维护者及原作者双组链接）均完整可用；
   * `.github/workflows/android-ci.yml` 与 `app/build.gradle.kts` 完整实现双轨版本号解耦、免 `gradlew` 云编译、自动签名与 Release 附件挂载。
