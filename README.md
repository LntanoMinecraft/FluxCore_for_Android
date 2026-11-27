# 流枢 · FluxCore

面向 Root 用户的现代 Android 系统级情境自动化引擎——基于 Kotlin、Jetpack Compose 与 Material Design 3 Expressive 全面重构，支持可视化动作流水线排序、屏幕峰值亮度（HBM）激发、本地 Shell 脚本检索、快捷设置磁贴、桌面快捷方式与实时日志排障。

> **项目来源、版权与 AI 构建声明：**
> * 本仓库为上游开源项目 [MOUAJMD/FluxCore_for_Android](https://github.com/MOUAJMD/FluxCore_for_Android) 的衍生分支，现由 [LntanoMinecraft](https://github.com/LntanoMinecraft) 独立维护与持续迭代。
> * 原项目版权归原作者 [MOUAJMD](https://github.com/MOUAJMD)（酷安：[@C_AJMD_W](https://www.coolapk.com/u/39291006)）所有，本项目完整保留原作者版权声明，并继续遵循 **MIT 开源协议**。
> * **AI 编写说明：** 原上游项目由 **Gemini 4 Hydrogen** 编写；当前独立维护分支由维护者借助 **Google AI Studio** 中的 **Gemini 4 Neon** 完成架构重构与功能开发。

> **安装须知（同包名提醒）：** 本分支沿用了与原上游项目相同的应用包名（`com.mouajmd.fluxcore`），但使用独立签名密钥签发。**若设备已安装原上游版本，请先在原版导出 `.json` 规则备份并卸载原软件，再安装本仓库发行版**（导入旧版备份可自动迁移规则与 `0–255` 亮度数值）。

---

## 🇨🇳 中文说明

### 核心升级与特性

* **Material Design 3 Expressive 原生界面：** 全面移除传统硬边框与旧版 View 弹窗，采用无边框色调层级（Tonal Elevation）大圆角卡片、悬浮胶囊底栏（`FloatingPillNavBar`，大屏自适应切换为 `NavigationRail`）、Google Sans Flex 正文与 JetBrains Mono 终端字体，以及全站真 Edge-to-Edge 透明导航栏（覆盖所有一二级页面与弹窗）。
* **单语应用名与新版自适应图标：** 中文环境显示「流枢」，英文环境显示「FluxCore」；图标升级为纯白底柔光渐变自适应图标，完整适配系统单色主题图标（`<monochrome>`）。
* **主页实时权限与运行状态检测：** 主页顶部内置状态检测卡，实时监控 Root 权限、电池优化白名单、勿扰权限与后台守护服务；权限缺失时以红色警示容器提供一键授权或直达入口，就绪后返回前台自动过渡回主题色。
* **可视化动作流水线与拖拽排序：** 将原有固定表单升级为有序动作步骤列表（`ActionStep`），支持长按手柄丝滑上下拖拽排序、独立删改、步骤延时编排与「启动应用」动作（Root 优先突破后台启动限制）；应用列表支持显示真实 App 图标，退出情境时结合状态快照安全还原（Auto-Revert）。
* **百分比滑块与屏幕峰值亮度（HBM）：** 媒体音量（`0%–100%`）与屏幕亮度（`0%–100%`）统一采用宽轨圆角百分比滑块（等宽数字 `tnum` 防抖动）；内置 `HardwareDisplayController` 支持突破系统亮度上限激发 HBM 峰值亮度（自动探测主流高通、三星、小米、一加、vivo 等设备的 sysfs HBM 与 `max_brightness` 节点），退出情境或息屏时强制还原原始亮度与自适应亮度状态。
* **本地 Root Shell 脚本扫描器：** 内置 `ScriptRepository` 与 `ScriptScannerSheet`，自动扫描本地存储目录与 `/data/adb` 下的 `.sh` 脚本，支持分类筛选、只读预览，一键引用路径执行或填入脚本正文。
* **控制中心磁贴与桌面快捷方式：** 内置多槽位快捷设置磁贴（`FluxCoreTileService` / `TileSlotManager`）与无界面透明中转桌面快捷方式（`ShortcutHelper` / `ShortcutTriggerActivity`），无需打开主界面即可一键切换情境。
* **交互式控制台与可检索实时日志：** 提供支持快捷命令预设与实时回显的 Root 终端（`ConsoleScreen`），以及支持关键字搜索、按级别筛选、一键清空与导出的实时日志页（`LogScreen`）。
* **外观定制、触感引擎与无通知后台模式：** 支持深浅色模式（跟随系统 / 浅色 / 深色）、Material You 动态壁纸取色、7 色双层圆点主题色盘、独立「使用教程」与「关于」二级页、可开关的全局线性马达触摸反馈引擎（`HapticEngine`），以及可隐藏状态栏常驻运行通知的后台运行模式。
* **后台健壮性与旧版配置兼容：** 重构持久化 `RootShell` 长连接，加入超时保护、异常捕获、进程清理与自动重连，严禁主线程同步 Shell/IO；息屏自动挂起轮询降低功耗，并自动兼容迁移原版规则 JSON 与 `0–255` 亮度数值。

### 触发条件与执行能力概览

| 维度 | 支持能力 |
| :--- | :--- |
| **触发条件（IF）** | 手动触发、打开/退出应用（进阶模式支持多应用集合）、充电器插拔、亮屏/息屏、Wi-Fi 连接（进阶模式匹配指定 SSID）、蓝牙设备（进阶模式匹配名称关键字）、电量阈值（`1%–100%`）、有线耳机插拔、横竖屏切换、时间段（支持跨零点）；进阶模式支持多条件 `AND` 组合 |
| **有序动作（THEN）** | 快捷设置磁贴切换、强行停止后台应用、启动指定应用、勿扰模式、刷新率锁定（`60Hz/90Hz/120Hz`）、百分比亮度与 HBM 峰值亮度激发、百分比媒体音量、Wi-Fi / 移动数据 / 飞行模式 / 省电 / 深色主题 / 屏幕旋转开关、步骤延时、触发震动提示、自定义进入与退出 Root Shell 指令 |
| **快捷触发与工具** | 控制中心多槽位磁贴、主屏幕无感快捷方式、一键深度后台净化（`drop_caches`）、快捷 Root 终端、本地 `.sh` 脚本扫描、JSON 配置备份恢复、实时可检索日志 |

### 技术栈与工程架构

| 技术维度 | 实现方案 |
| :--- | :--- |
| **语言与并发** | Kotlin `2.2.21`、Kotlin Coroutines、`StateFlow` 单向数据流 |
| **构建与编译** | Gradle Kotlin DSL（AGP `9.1.1`、`libs.versions.toml`、`compileSdk` / `targetSdk 36`、`minSdk 26`、Java 17），Release 开启 R8 代码压缩与资源缩减 |
| **UI 与字体** | Jetpack Compose（BOM `2025.05.00`）、Material Design 3 Expressive、Material Symbols Extended、本地打包 `Google Sans Flex` 与 `JetBrains Mono` |
| **导航与持久化** | Navigation Compose `2.8.9`（`@Serializable` 类型安全路由）、`kotlinx.serialization 1.8.0`、`RuleStore` / `SettingsStore` / `AppLogger` |
| **分层架构** | MVVM 清晰分层：`model/`、`data/`、`engine/`、`system/`、`service/`、`viewmodel/`、`ui/` |

### 安装、使用与保活指南

1. **卸载旧版（如曾安装原上游版本）：** 因本分支与原项目使用相同包名（`com.mouajmd.fluxcore`）但签名不同，请先备份原版规则后卸载原软件，再安装本仓库发布的 APK。
2. **授予 Root 与相关权限：** 打开「流枢」，允许 `su` 权限后确认主页顶部状态卡片转为主题色就绪状态；按提示完成勿扰权限与电池优化设置。
3. **后台保活设置：** 在系统应用信息中将电池优化设为**「无限制」**，并在最近任务中锁定应用；如希望状态栏更清爽，可在设置中开启「隐藏常驻通知」。
4. **规则编排与快捷入口：** 在编辑页通过手柄拖拽调整动作顺序；如需一键切换，可在规则菜单将其固定到控制中心磁贴或桌面快捷方式。

### 开源协议与版权信息

* **当前独立维护仓库：** [LntanoMinecraft/FluxCore_for_Android](https://github.com/LntanoMinecraft/FluxCore_for_Android)（维护者：[Lntano](https://www.google.com/search?q=https://www.coolapk.com/u/34054336) · 使用 Google AI Studio **Gemini 4 Neon** 重构与维护）
* **原上游项目仓库：** [MOUAJMD/FluxCore_for_Android](https://github.com/MOUAJMD/FluxCore_for_Android)（原作者：[MOUAJMD / @C_AJMD_W](https://www.coolapk.com/u/39291006) · 使用 **Gemini 4 Hydrogen** 编写）
* **开源协议：** 本项目遵循 **MIT License**，完整协议正文与版权声明见仓库根目录 `LICENSE` 文件。

---

## 🇺🇸 English

A modern, system-level context automation engine for rooted Android devices—rebuilt from the ground up with Kotlin, Jetpack Compose, and Material Design 3 Expressive, featuring a drag-to-reorder action pipeline, hardware peak brightness (HBM) boost, local `.sh` script scanning, Quick Settings tiles, pinned home shortcuts, and real-time searchable logs.

> **Upstream Attribution, Copyright & AI Authorship Notice:**
> * This repository is an independently maintained fork of [MOUAJMD/FluxCore_for_Android](https://github.com/MOUAJMD/FluxCore_for_Android), maintained by [LntanoMinecraft](https://github.com/LntanoMinecraft).
> * Original copyright belongs to [MOUAJMD](https://github.com/MOUAJMD) (CoolApk: [@C_AJMD_W](https://www.coolapk.com/u/39291006)). All original copyright notices are retained in `LICENSE` under the **MIT License**.
> * **AI Development Credit:** The original upstream project was written with **Gemini 4 Hydrogen**; this independently maintained fork is refactored and maintained using **Gemini 4 Neon** in **Google AI Studio**.

> **Installation Note (Identical Package Name):** This fork intentionally retains the same Android package name (`com.mouajmd.fluxcore`) as the upstream project, but is signed with an independent release key. **If you currently have the original upstream build installed, export your `.json` rules first and uninstall the original app before installing releases from this repository** (legacy backups and `0–255` brightness values are migrated automatically).

### What's New & Upgraded

* **Material Design 3 Expressive Redesign:** Replaces legacy programmatic Java Views, dialogs, and harsh borders with borderless tonal-elevation cards, a floating pill navigation bar (`FloatingPillNavBar`, adapting to `NavigationRail` on large screens), bundled `Google Sans Flex` and `JetBrains Mono` typography, and true global Edge-to-Edge transparent navigation bars across every screen, bottom sheet, and dialog.
* **Localized Single-Language App Name & Adaptive Icon:** Displays `流枢` in Chinese and `FluxCore` in English, paired with a white-background soft blue-violet gradient adaptive icon supporting `<monochrome>` themed icons.
* **Live Permission & Engine Status Card:** A prominent home status card continuously checks Root (`su`) availability, battery optimization exemption, Do Not Disturb access, and background service health—highlighting missing permissions in M3 error containers with direct actions and transitioning back to theme colors on resume.
* **Visual Drag-to-Reorder Action Pipeline:** Upgrades fixed action forms into an ordered `ActionStep` workflow supporting smooth drag-handle reordering, per-step editing/deletion, delay steps, **Launch App** (Root-first to bypass background launch restrictions), real installed app icons in pickers, and consistent Auto-Revert state restoration.
* **Percentage Sliders & Hardware HBM Peak Brightness:** Media volume (`0%–100%`) and brightness (`0%–100%`) use wide-track M3 Expressive percentage sliders with tabular numerals (`tnum`). `HardwareDisplayController` probes device-specific sysfs HBM and `max_brightness` nodes (Qualcomm, Samsung, Xiaomi, OnePlus, vivo, etc.) to unlock peak brightness beyond system caps, and automatically restores prior brightness and adaptive brightness on rule exit or screen-off.
* **Local Root Shell Script Scanner:** `ScriptRepository` and `ScriptScannerSheet` scan local storage and `/data/adb` for `.sh` scripts with category filtering, read-only preview, and one-tap path reference or inline insertion.
* **Quick Settings Tiles & Home Screen Shortcuts:** Multi-slot `FluxCoreTileService` (`TileSlotManager`) and translucent no-UI pinned shortcuts (`ShortcutHelper` / `ShortcutTriggerActivity`) let you toggle profiles directly without opening the app.
* **Built-In Root Console & Searchable Real-Time Logs:** Interactive `ConsoleScreen` with command presets and live output, paired with `LogScreen` featuring keyword search, level filtering, one-tap clear, and export.
* **Appearance, Haptic Engine & Notification-Free Background Mode:** Includes theme mode switching (Follow System / Light / Dark), Material You Dynamic Color, 7-color palette picker, dedicated Tutorial and About screens, a toggleable global linear-motor `HapticEngine`, and an option to hide the persistent running notification.
* **Hardened Root Channel & Legacy Compatibility:** Rebuilt persistent `RootShell` with command timeouts, exception isolation, process cleanup, auto-reconnect, zero main-thread Shell/IO, screen-off polling suspension, and automatic migration for legacy rule JSON files and `0–255` brightness values.

### Tech Stack & Architecture

| Area | Stack |
| :--- | :--- |
| **Language & Async** | Kotlin `2.2.21`, Kotlin Coroutines, unidirectional `StateFlow` |
| **Build System** | Gradle Kotlin DSL (AGP `9.1.1`, `libs.versions.toml`, `compileSdk` / `targetSdk 36`, `minSdk 26`, Java 17), Release R8 shrinking & resource shrinking |
| **UI & Typography** | Jetpack Compose (BOM `2025.05.00`), Material Design 3 Expressive, Material Symbols Extended, bundled `Google Sans Flex` & `JetBrains Mono` |
| **Navigation & Storage** | Navigation Compose `2.8.9` (`@Serializable` type-safe routes), `kotlinx.serialization 1.8.0`, `RuleStore` / `SettingsStore` / `AppLogger` |
| **Architecture** | Layered MVVM: `model/`, `data/`, `engine/`, `system/`, `service/`, `viewmodel/`, `ui/` |

### License & Attribution

* **Maintained Fork:** [LntanoMinecraft/FluxCore_for_Android](https://github.com/LntanoMinecraft/FluxCore_for_Android) (Maintainer: [Lntano](https://www.google.com/search?q=https://www.coolapk.com/u/34054336) · Built with **Gemini 4 Neon** in Google AI Studio)
* **Original Upstream:** [MOUAJMD/FluxCore_for_Android](https://github.com/MOUAJMD/FluxCore_for_Android) (Original Author: [MOUAJMD / @C_AJMD_W](https://www.coolapk.com/u/39291006) · Built with **Gemini 4 Hydrogen**)
* **License:** Distributed under the **MIT License**. See `LICENSE` in the repository root for full terms and copyright notices.
