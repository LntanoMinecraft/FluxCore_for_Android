# 流枢 · FluxCore

面向 Root 用户的超轻量 Android 系统级情境自动化引擎——APK 体积仅约 60 KB，零第三方依赖，纯原生 Material Design 3 动态取色界面。

> **项目与版权声明：** 本项目是上游开源项目 [MOUAJMD/FluxCore_for_Android](https://github.com/MOUAJMD/FluxCore_for_Android) 的衍生分支，现由 [LntanoMinecraft](https://github.com/LntanoMinecraft) 独立维护与迭代。原项目版权归原作者 [MOUAJMD](https://github.com/MOUAJMD) 所有，本项目完整保留原作者版权声明，并继续遵循 **MIT 开源协议**。

---

## 🇨🇳 中文说明

### 核心特性

* **极致轻量与零依赖：** 全套界面基于纯 Java 动态构建（无 XML 布局冗余），内置自研 `Md3e` 引擎，原生支持 Android 12+ 莫奈动态取色（Material You）、Android 13–16+ 自适应单色主题图标（`<monochrome>`）、边到边沉浸适配与全局线性马达弹簧触感反馈。
* **事件驱动 + 兼容活动栈探测：** 硬件、电量、耳机、网络与屏幕旋转状态通过系统广播零轮询响应；前台应用切换采用持久化 Root 通道探测系统事件流与活动栈，兼容高版本 Android（Android 13–16+），息屏自动休眠，零额外待机耗电。
* **状态快照与自动还原（Auto-Revert）：** 激活情境前自动记录系统原始状态（磁贴状态、勿扰、刷新率、亮度模式与数值、媒体音量、Wi-Fi、移动数据、飞行模式、省电、深色主题、屏幕旋转），在退出触发条件或手动关闭情境时无缝还原，并支持自定义退出还原 Shell 脚本。

### 完整功能清单

#### 1. 触发条件（IF）

支持单条件触发；在「设置」中开启**进阶模式**后，可解锁**多应用集合**、**指定网络/蓝牙匹配**以及**多条件 `AND`（「且」）组合逻辑**（所有条件同时满足才激活，任一条件退出即自动还原）：

| 触发类型 | 基础模式 | 进阶模式扩展 |
| :--- | :--- | :--- |
| **仅手动触发** | 点击情境卡片按钮主动激活或还原 | — |
| **打开 / 退出应用** | 监控单个指定应用进入或离开前台 | 支持多应用集合（打开任意 / 退出全部） |
| **充电器连接** | 接入电源 / 断开电源 | 可作为附加 `AND` 条件叠加 |
| **亮屏 / 息屏** | 屏幕点亮 / 屏幕熄灭 | 可作为附加 `AND` 条件叠加 |
| **Wi-Fi 连接** | 任意 Wi-Fi 已连接 / 已断开 | 匹配指定 **Wi-Fi SSID**（支持一键读取当前） |
| **蓝牙设备** | 任意蓝牙已连接 / 已断开 | 匹配指定**蓝牙设备名称**关键字 |
| **电量阈值** | 电量 ≥ 或 ≤ 指定百分比（`1%–100%`） | 可作为附加 `AND` 条件叠加 |
| **有线耳机插拔** | 插入有线耳机 / 拔出有线耳机 | 可作为附加 `AND` 条件叠加 |
| **屏幕横竖屏** | 屏幕旋转为横屏 / 恢复为竖屏 | 可作为附加 `AND` 条件叠加 |
| **时间段触发** | 在指定时间区间内激活（支持跨零点，如 `23:00–07:00`） | 可作为附加 `AND` 条件叠加 |

#### 2. 执行动作（THEN）

每条规则可自由组合多项系统级控制动作：

| 动作类别 | 支持能力 |
| :--- | :--- |
| **快捷设置磁贴（QS Tile）** | 内置磁贴选择器，自动扫描系统与第三方应用的快捷设置磁贴，触发时通过 `cmd statusbar click-tile` 切换状态，退出自动还原 |
| **强行停止后台应用** | 多选目标应用（已选应用置顶），触发时执行 `am force-stop` 彻底清理进程 |
| **显示与声音控制** | 屏幕刷新率动态锁定（`60Hz` / `90Hz` / `120Hz`）；关闭自适应亮度并固定至 `0–255` 亮度值；媒体音量调节（`0%–100%`，退出自动还原）；勿扰模式（DND）切换（关闭 / 仅重要打扰 / 完全静音） |
| **网络与系统开关** | 独立控制 **Wi-Fi**、**移动数据**、**飞行模式**、**系统原生省电模式**、**深色主题**与**屏幕自动旋转 / 锁定竖屏** |
| **触感反馈** | 情境激活时触发短震提示，无需亮屏即可感知规则生效 |
| **自定义 Root Shell 指令** | 分别配置**进入情境**与**退出还原**时执行的 `su` 指令（可用于调节 CPU 调度、温控策略、分辨率/DPI 等） |

#### 3. 控制台与设置中心

* **一键深度后台净化（控制台）：** 强行停止所有第三方后台应用并释放系统页面缓存（`drop_caches`）。
* **快捷 Root Shell 终端（控制台）：** 内置轻量 `su` 命令执行器，实时回显标准输出，方便调试系统参数。
* **进阶模式开关（设置）：** 默认关闭以保持界面极简清爽，开启后解锁多应用触发、指定网络/蓝牙及多条件 `AND` 组合。
* **配置备份与恢复（设置）：** 将全部规则导出为 `.json` 至 `/storage/emulated/0/Download/FluxCore/`，支持可视化扫描导入（「合并追加」或「覆盖恢复」）。
* **运行日志导出（设置）：** 一键导出引擎事件流、规则匹配与 Shell 执行记录（`.txt`）至 `/storage/emulated/0/Download/FluxCore/`。

### 使用与保活指南

1. **授予 Root 权限：** 安装并打开 FluxCore，在 Root 授权弹窗中允许 `su` 权限，确认顶部状态标签显示 **`• Root 已就绪`** 与 **`• 后台监听中`**。
2. **后台保活设置：** 为避免系统墓碑机制或省电策略休眠后台服务，请在系统设置中将 FluxCore 电池优化设为**「无限制」**，并在最近任务界面将其**锁定**。
3. **覆盖升级提示：** 从旧版本覆盖安装会完整保留原有配置；升级后建议在系统应用信息中「强行停止」一次 FluxCore 再重新打开，以确保加载最新后台监听服务。

### 开源协议与版权声明

* **上游项目：** [MOUAJMD/FluxCore_for_Android](https://github.com/MOUAJMD/FluxCore_for_Android)（原作者：MOUAJMD）
* **独立维护：** [LntanoMinecraft/FluxCore_for_Android](https://github.com/LntanoMinecraft/FluxCore_for_Android)（维护者：LntanoMinecraft）
* **开源协议：** 本项目遵循 **MIT License**，完整协议与版权声明详见仓库根目录的 `LICENSE` 文件。

---

## 🇺🇸 English

An ultra-lightweight, system-level context automation engine for rooted Android devices (~60 KB APK, zero third-party dependencies, pure programmatic Material Design 3 dynamic color UI).

> **Project & Copyright Notice:** This repository is an independently maintained fork of [MOUAJMD/FluxCore_for_Android](https://github.com/MOUAJMD/FluxCore_for_Android), maintained by [LntanoMinecraft](https://github.com/LntanoMinecraft). Original copyright belongs to [MOUAJMD](https://github.com/MOUAJMD). All original copyright notices are retained in `LICENSE` under the **MIT License**.

### Key Highlights

* **Ultra-Lightweight & Zero Dependencies:** Built entirely with programmatic Java layouts (no XML overhead) powered by a custom `Md3e` UI toolkit—supporting Android 12+ Material You dynamic colors, Android 13–16+ adaptive monochrome themed icons (`<monochrome>`), edge-to-edge layout insets, and global linear-motor haptic feedback.
* **Event-Driven Hardware Listeners + Resilient Activity Stack Probing:** Hardware, battery, headset, connectivity, and orientation states use zero-polling system broadcasts. Foreground app detection runs through a persistent Root shell channel compatible with Android 13–16+, automatically sleeping when the screen is off for zero idle battery drain.
* **State Snapshot & Auto-Revert:** Automatically snapshots original system states (QS Tiles, Do Not Disturb, refresh rate, brightness mode & level, media volume, Wi-Fi, mobile data, airplane mode, battery saver, dark mode, auto-rotation) before activating a rule and restores them seamlessly on exit, alongside custom exit shell scripts.

### Full Feature Set

#### 1. Triggers (IF)

Supports single-condition triggers or **Advanced Mode** (toggleable in Settings) for multi-app sets, specific network/device matching, and **multi-condition `AND` logic** (activates only when all conditions are met simultaneously; reverts as soon as any condition exits):

| Trigger Type | Standard Mode | Advanced Mode Extensions |
| :--- | :--- | :--- |
| **Manual Trigger** | Manually toggle active or revert state from the rule card | — |
| **App Launch / Exit** | Monitors when a target app enters or leaves the foreground | Multi-app sets (enter any / exit all) |
| **Charger Connection** | Power connected / disconnected | Stackable as an additional `AND` condition |
| **Screen On / Off** | Screen turned on / turned off | Stackable as an additional `AND` condition |
| **Wi-Fi Connection** | Any Wi-Fi connected / disconnected | Match specific **Wi-Fi SSID** (with one-tap current SSID fetch) |
| **Bluetooth Device** | Any Bluetooth device connected / disconnected | Match specific **Bluetooth device name** keyword |
| **Battery Threshold** | Battery level ≥ or ≤ target percentage (`1%–100%`) | Stackable as an additional `AND` condition |
| **Wired Headset** | Wired headset plugged in / unplugged | Stackable as an additional `AND` condition |
| **Screen Orientation** | Screen rotated to landscape / portrait | Stackable as an additional `AND` condition |
| **Time Window** | Active within a scheduled time range (supports overnight spans such as `23:00–07:00`) | Stackable as an additional `AND` condition |

#### 2. Actions (THEN)

Combine multiple system-level actions within any automation rule:

| Category | Capabilities |
| :--- | :--- |
| **Quick Settings (QS) Tile Toggle** | Built-in QS Tile picker scanning system and third-party tiles; toggles the target tile via `cmd statusbar click-tile` on entry and reverts on exit |
| **Force-Stop Background Apps** | Select multiple target apps (selected items pinned to the top) to terminate via `am force-stop` upon activation |
| **Display & Audio Control** | Dynamic refresh rate lock (`60Hz` / `90Hz` / `120Hz`); disable adaptive brightness and lock fixed brightness (`0–255`); media volume (`0%–100%`, auto-restored on exit); Do Not Disturb (Off / Priority Only / Total Silence) |
| **System & Connectivity Toggles** | Independently control **Wi-Fi**, **Mobile Data**, **Airplane Mode**, **Native Battery Saver**, **Dark Theme**, and **Auto-Rotation / Portrait Lock** |
| **Trigger Vibration Pulse** | Tactile confirmation when a profile activates silently in the background |
| **Custom Root Shell Commands** | Run dedicated `su` commands on profile entry and exit/revert (ideal for CPU governors, thermal profiles, DPI tweaks, etc.) |

#### 3. Console & Settings

* **One-Tap Deep Background Cleanup (Console):** Force-stops all third-party background processes and drops system page caches (`drop_caches`).
* **Quick Root Shell Terminal (Console):** Built-in lightweight `su` command runner with live stdout feedback.
* **Advanced Mode Toggle (Settings):** Disabled by default for a clean UI; enables multi-app triggers, specific SSID/Bluetooth matching, and multi-condition `AND` rules.
* **Config Backup & Restore (Settings):** Export all rules to `.json` in `/storage/emulated/0/Download/FluxCore/` and import backups with **Merge** or **Overwrite** modes.
* **Engine Log Export (Settings):** One-tap export of engine event streams, rule evaluations, and shell outputs (`.txt`) to `/storage/emulated/0/Download/FluxCore/`.

### Setup & Persistence Tips

1. **Grant Root Access:** Launch FluxCore and grant `su` permission when prompted. Verify that the top status chips show **`• Root 已就绪` (Root Ready)** and **`• 后台监听中` (Monitoring Active)**.
2. **Prevent Background Killing:** Set FluxCore's battery optimization to **Unrestricted** in system settings and lock it in the Recents overview to keep the background engine active.
3. **Upgrading Over an Existing Install:** Upgrading preserves all existing rules; force-stop FluxCore once in App Info after installing an update, then reopen it to load the latest background service.

### License & Attribution

* **Upstream Repository:** [MOUAJMD/FluxCore_for_Android](https://github.com/MOUAJMD/FluxCore_for_Android) (Original Author: MOUAJMD)
* **Independent Maintenance:** [LntanoMinecraft/FluxCore_for_Android](https://github.com/LntanoMinecraft/FluxCore_for_Android) (Maintainer: LntanoMinecraft)
* **License:** Distributed under the **MIT License**. See `LICENSE` for full terms and copyright notices.
