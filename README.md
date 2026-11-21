# 流枢 · FluxCore

[中文说明](#中文说明) | [English](#english)

---

<a id="中文说明"></a>
## 🇨🇳 中文说明

> 面向 Root 用户的超轻量 Android 系统级情境自动化引擎（APK 体积仅 ~60 KB，零第三方依赖，纯原生 Material Design 3 动态取色界面）。

### ✨ 核心特性

* **极致轻量与零依赖**：全套界面基于纯 Java 动态构建（无 XML 布局冗余），内置自研 `Md3e` 引擎，原生支持 Android 12+ 莫奈动态取色（Material You）、自适应单色主题图标以及弹簧物理触摸反馈。
* **系统级事件驱动 + 兼容活动栈探测**：硬件与网络状态通过系统广播零轮询响应；前台应用切换采用持久化 Root 通道探测系统活动栈（`ResumedActivity`）与窗口焦点，完美兼容屏蔽了 `logcat -b events` 缓冲区的国产 ROM 与高版本 Android（Android 13–16+），且息屏自动休眠，零额外待机耗电。
* **状态快照与自动还原（Auto-Revert）**：激活情境前自动记录系统原始状态（勿扰、刷新率、亮度模式与数值、Wi-Fi 开关），在退出触发条件或手动关闭情境时无缝还原，并支持自定义退出还原 Shell 脚本。

### 🛠️ 完整功能清单

#### 1. 触发条件 (IF)
支持单条件触发，或在「设置」中开启**进阶模式**后启用**多应用集合、指定网络/设备匹配**以及**多条件 AND（「且」）组合逻辑**（所有条件同时满足才激活，任一条件退出即自动还原）：

| 触发类型 | 基础模式 | 进阶模式扩展 |
| :--- | :--- | :--- |
| **仅手动触发** | 点击情境卡片按钮主动激活/还原 | — |
| **打开 / 退出应用** | 监控单个指定应用进入或离开前台 | 支持多应用集合（打开任意 / 退出全部） |
| **充电器连接** | 接入电源 / 断开电源 | 可作为附加 `AND` 条件叠加 |
| **亮屏 / 息屏** | 屏幕点亮 / 屏幕熄灭 | 可作为附加 `AND` 条件叠加 |
| **Wi-Fi 连接** | 任意 Wi-Fi 已连接 / 已断开 | 支持匹配指定 **Wi-Fi SSID** 名称 |
| **蓝牙设备** | 任意蓝牙已连接 / 已断开 | 支持匹配指定**蓝牙设备名称**关键字 |

#### 2. 执行动作 (THEN)
每条规则可自由组合以下多项系统级控制动作：
* **强行停止后台应用**：多选目标应用，触发时自动执行 `am force-stop` 彻底清理进程。
* **勿扰模式（DND）控制**：切换「关闭勿扰」、「仅允许重要打扰」或「完全静音勿扰」。
* **屏幕刷新率锁定**：在 `60Hz` / `90Hz` / `120Hz` 之间动态切换。
* **固定屏幕亮度**：自动关闭自适应亮度并锁定至 `0–255` 指定亮度值。
* **Wi-Fi 开关控制**：自动开启或关闭 Wi-Fi。
* **自定义 Root Shell 指令**：支持配置**进入情境时执行**的 `su` 指令与**退出还原时执行**的 `su` 指令（可用于调节 CPU 调度、温控策略、分辨率/DPI、系统省电开关等）。

#### 3. 开箱即用的内置预设
首次启动自动加载三套实用情境模板（完全保留用户已有自定义规则，支持一键切换或二次编辑）：
* **游戏沉浸模式**：开启完全静音勿扰 + 锁定 120Hz 高刷 + 锁定亮度 200（可自行绑定对应游戏包名及后台清理列表）。
* **深度睡眠模式**：开启完全静音勿扰 + 降为 60Hz + 极低亮度 15 + 关闭 Wi-Fi。
* **极致省电模式**：降为 60Hz + 亮度 45 + 通过 Root 指令联动开启系统原生省电模式（`low_power 1`），退出时自动还原。

#### 4. 控制台与工具箱
* **一键深度后台净化**：强行停止所有第三方后台应用并释放系统页面缓存（`drop_caches`）。
* **快捷 Root Shell 终端**：内置轻量 `su` 命令执行器，实时回显标准输出，方便调试系统参数。
* **实时引擎日志**：记录系统事件捕获、应用切换轨迹、规则匹配与 Shell 执行状态，便于排查自动化流程。

### 📋 使用与保活指南

1. **授予 Root 权限**：安装并打开 FluxCore，在弹出的 Root 授权框中允许 `su` 权限，确认顶部状态标签显示 **`● Root 已就绪`** 与 **`● 后台监听中`**。
2. **后台保活设置**：为避免系统墓碑机制或省电策略休眠后台服务，建议在系统设置中将 FluxCore 设为**电池优化「无限制」**，并在最近任务界面将其**锁定**。
3. **覆盖升级提示**：若从旧版本覆盖安装升级，建议在系统应用信息中「强行停止」一次 FluxCore 后重新打开，以确保加载最新的后台监听服务。

---

<a id="english"></a>
## 🇺🇸 English

> An ultra-lightweight, system-level context automation engine for rooted Android devices (~60 KB APK, zero third-party dependencies, built with a pure programmatic Material Design 3 dynamic color UI).

### ✨ Key Highlights

* **Ultra-Lightweight & Zero Dependencies**: Built entirely with programmatic Java layouts (no XML overhead) powered by a custom `Md3e` UI toolkit—featuring Android 12+ Material You dynamic colors, adaptive monochrome themed icons, and spring-physics touch feedback.
* **Event-Driven Hardware Listeners + Resilient Activity Stack Probing**: Hardware and connectivity states are captured via zero-polling system broadcasts. Foreground app detection uses a persistent Root shell channel querying `ResumedActivity` and window focus directly—working reliably even on OEM ROMs and Android 13–16+ builds where `logcat -b events` is stripped or disabled, while automatically sleeping when the screen is off for zero idle battery drain.
* **State Snapshot & Auto-Revert**: Automatically snapshots original system states (Do Not Disturb, refresh rate, brightness mode & level, Wi-Fi state) before activating a rule and seamlessly restores them when exit conditions are met, alongside custom exit shell scripts.

### 🛠️ Full Feature Set

#### 1. Triggers (IF)
Supports single-condition triggers or **Advanced Mode** (toggleable in Settings) for multi-app sets, specific network/device matching, and **multi-condition `AND` logic** (activates only when all conditions are satisfied simultaneously; reverts as soon as any condition exits):

| Trigger Type | Standard Mode | Advanced Mode Extensions |
| :--- | :--- | :--- |
| **Manual Trigger** | Manually toggle active/revert state from the rule card | — |
| **App Launch / Exit** | Monitors when a target app enters or leaves the foreground | Supports multi-app sets (enter any / exit all) |
| **Charger Connection** | Power connected / disconnected | Can be stacked as an additional `AND` condition |
| **Screen On / Off** | Screen turned on / turned off | Can be stacked as an additional `AND` condition |
| **Wi-Fi Connection** | Any Wi-Fi connected / disconnected | Match specific **Wi-Fi SSID** |
| **Bluetooth Device** | Any Bluetooth device connected / disconnected | Match specific **Bluetooth device name** keyword |

#### 2. Actions (THEN)
Combine multiple system-level actions within any automation rule:
* **Force-Stop Background Apps**: Select multiple target apps to terminate via `am force-stop` upon activation.
* **Do Not Disturb (DND) Control**: Switch between Off, Priority Only, or Total Silence.
* **Display Refresh Rate Lock**: Dynamically switch between `60Hz`, `90Hz`, and `120Hz`.
* **Fixed Screen Brightness**: Disables adaptive brightness and locks the level to a fixed value (`0–255`).
* **Wi-Fi Toggle**: Automatically enable or disable Wi-Fi.
* **Custom Root Shell Commands**: Execute custom `su` commands upon entering a profile and dedicated `su` commands upon exiting/reverting (ideal for CPU governors, thermal profiles, DPI tweaks, or system battery saver toggles).

#### 3. Built-in Out-of-the-Box Presets
Automatically appended on first launch without overwriting existing user rules:
* **Gaming Immersion Mode**: Total Silence DND + 120Hz refresh rate + 200 brightness (ready to bind to specific games and background kill lists).
* **Deep Sleep Mode**: Total Silence DND + 60Hz refresh rate + ultra-low 15 brightness + Wi-Fi off.
* **Extreme Power Saving**: 60Hz refresh rate + 45 brightness + enables native Android Battery Saver (`settings put global low_power 1`) with automatic restoration on exit.

#### 4. Console & Utilities
* **One-Tap Deep Background Cleanup**: Force-stops all third-party background processes and drops system page caches (`drop_caches`).
* **Quick Root Shell Terminal**: Built-in lightweight `su` command runner with live stdout feedback.
* **Real-Time Engine Logs**: Tracks system events, foreground app transitions, rule evaluations, and shell outputs for effortless debugging.

### 📋 Setup & Persistence Tips

1. **Grant Root Access**: Launch FluxCore and grant `su` permission when prompted. Verify that the top status chips display **`● Root 已就绪` (Root Ready)** and **`● 后台监听中` (Monitoring Active)**.
2. **Prevent Background Killing**: Set FluxCore's battery optimization to **Unrestricted** in system settings and lock it in the Recents overview so the background engine stays active.
3. **Upgrading Over an Existing Install**: Force-stop FluxCore once in App Info after installing an update, then reopen it to ensure the updated background service is loaded.
