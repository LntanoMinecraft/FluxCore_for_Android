# LSPosed 系统组件 Hook 模块通用工程开发规范（Modern API 102）

> **文档类型**：通用系统级 LSPosed 模块工程标准（Markdown）  
> **适用范围**：Hook `system_server`（`system`）、`com.android.systemui` 及其他 Android 系统核心组件的现代模块  
> **配套文档**：可与《Lntano Android 统一设计语言规范（M3 Expressive Edition）》组合直接输入 AI Studio（Gemini）立项开发

## 1. 标准工程目录结构

```text
ProjectRoot/
├── .github/
│   └── workflows/
│       └── android-ci.yml               # 双轨云编译与 Release 自动发包
├── app/
│   ├── build.gradle.kts                 # compileSdk=36, R8 混淆, check*AarMetadata 兼容与 CI 版本覆写
│   ├── proguard-rules.pro               # libxposed API 102 官方混淆保留规则
│   └── src/main/
│       ├── AndroidManifest.xml          # 零 xposed* meta-data，按最小权限原则声明
│       ├── resources/META-INF/xposed/
│       │   ├── module.prop              # minApiVersion=102, targetApiVersion=102
│       │   ├── java_init.list           # XposedModule 入口类全限定名
│       │   └── scope.list               # 按实际业务声明系统组件作用域
│       └── java/.../
│           ├── hook/                    # 纯 Hook 核心层（严禁引入 Compose/UI/R 资源）
│           ├── provider/                # 严格 UID 校验的本地遥测/日志 ContentProvider（可选）
│           ├── model/                   # 配置模型与 JSON 序列化（Schema 版本化）
│           └── ui/                      # Material Design 3 Expressive 控制端界面
├── gradle/
│   ├── libs.versions.toml               # 仅保留必要依赖，清除未使用重型模板库
│   └── wrapper/
│       └── gradle-wrapper.properties    # 供 setup-gradle wrapper 自动读取版本
├── metadata.json                        # AI Studio 预览元文件（不打入 APK，可保留）
└── settings.gradle.kts
```

## 2. 现代框架接口基线（libxposed API 102）

新项目禁止使用已废弃的 Legacy Xposed API 82（`de.robv.android.xposed.*`、`assets/xposed_init`、`XSharedPreferences / MODE_WORLD_READABLE`），统一采用官方现代接口（参考 [libxposed/api](https://github.com/libxposed/api) 与 [libxposed/service](https://github.com/libxposed/service)）：

### 2.1 Gradle 依赖

```kotlin
dependencies {
    compileOnly("io.github.libxposed:api:102.0.0")
    implementation("io.github.libxposed:service:102.0.1")
}
```

### 2.2 标准元数据模板

**`META-INF/xposed/module.prop`**

```properties
minApiVersion=102
targetApiVersion=102
exceptionMode=protective
# 仅当模块作用域固定（不允许用户随意勾选其他应用）时启用：
# staticScope=true
```

**`META-INF/xposed/scope.list`**（按模块实际目标按行声明）

```text
system
com.android.systemui
```

*注：Modern Xposed API 101/102 中，`system_server` 必须使用虚拟包名 **`system`**（不再是旧版 `android`）；其他系统组件直接填写其真实包名。模块名称与简介统一复用 `AndroidManifest.xml` 中 `<application>` 的 `android:label` 与 `android:description`。*

### 2.3 入口与拦截器规范

* 入口类继承 `io.github.libxposed.api.XposedModule`，提供公开无参构造 `public <init>()`，**构造函数内禁止任何初始化**。
* 系统框架逻辑挂载于 `onSystemServerStarting(...)`；其他系统应用/界面进程逻辑挂载于 `onPackageReady(...)` 并严格校验 `packageName` 与 `processName`（无关进程可调用 `detach()` 释放监听）。
* 使用 OkHttp 风格拦截器链 `hook(method).intercept { chain -> ... chain.proceed() ... }`；对系统框架可能被 ART 内联的关键方法按需调用 `deoptimize(method)`。

## 3. DEX 瘦身、R8 混淆与防 10 秒卡开机红线

Android 对 `SystemUI`、`system_server` 等关键系统进程启动设有 **10 秒超时限制（`PROC_START_TIMEOUT`）**，超出即触发重启循环卡开机。所有系统组件模块必须严格遵守以下红线：

| 风险维度 | 禁止做法（卡开机高危） | 强制规范（主线程耗时压至 `< 10ms`） |
| :--- | :--- | :--- |
| **APK / DEX 体积** | 保留未使用的 Firebase / Room / KSP / Retrofit / CameraX 等模板库 | 彻底清理未用业务依赖（仅保留不打入 APK 的 AI Studio 根目录元文件），Release 开启 `isMinifyEnabled = true` 与 `isShrinkResources = true` |
| **类图解耦** | `hook/` 包静态引用 Compose、Material3、AndroidX UI 或 `R` 类 | `hook/` 包仅依赖标准库与 `io.github.libxposed.api.*`，确保系统进程仅加载极轻纯 Hook 类图 |
| **DEX 扫描与反射** | 启动期引入 DexKit 或运行期全盘 DEX 搜索 | 严禁在系统进程启动期全盘解析 DEX；优先精准 `classLoader.loadClass` + `declaredMethods` 过滤，每类仅挂真正必需的 1–2 个方法 |
| **高频底层类 Hook** | 无差别 Hook `ApplicationPackageManager.getApplicationInfo` 等启动期高频大类或全重载 `hookAllMethods` | 严禁宽泛 Hook 启动期高频底层类，避免引发大面积 ART 反优化（Deoptimization） |
| **系统界面挂载时机** | 在 `com.android.systemui` 的 `onPackageReady` 主线程同步挂载非首帧必需 Hook | 非开机首帧必需的系统界面 Hook，通过 `Handler(Looper.getMainLooper()).postDelayed(..., 12_000L)` 延迟 **12 秒** 后切入 `MIN_PRIORITY` 守护线程挂载，彻底避开 10 秒启动窗口 |
| **启动期 IPC / 读盘** | 在入口回调主线程同步连 `ContentProvider` 或读写磁盘 | 启动结果先纯内存写入 `ConcurrentHashMap`；跨进程遥测上报延迟 **25–30 秒** 且校验 `UserManager.isUserUnlocked()` 后在低优先级单线程池异步执行 |

### 3.1 官方 R8 / ProGuard 规则模板（`proguard-rules.pro`）

```proguard
-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

# 如模块包含本地遥测 ContentProvider 或反射序列化模型，按需保留：
-keep class * extends android.content.ContentProvider { *; }
-keepattributes SourceFile,LineNumberTable
```

## 4. 作用域策略、权限边界与抗混淆原则

1. **按需设计生效范围（不盲目套用黑白名单）**：
   * **全局生效型**（如系统 UI 增强、通话/系统工具功能解锁）：无需强行引入黑白名单，直接针对目标系统组件生效，保持配置简洁；
   * **白名单型（Allowlist）**：当功能涉及**解除系统安全或隐私保护**（如截屏/录屏/音频限制）时采用，确保未勾选的银行、支付与密码管理应用保持原生保护；
   * **黑名单/排除型（Denylist）**：当功能默认全局生效、但需排除少数冲突或高风控应用时采用；
   * **进程隔离原则**：能通过 `system`（`system_server`）或系统服务层完成的策略，优先在系统层按 `packageName` / `UID` 精准控制，避免向强反作弊应用进程直接注入代码；同时慎用整屏级或全局级拦截，避免分屏/小窗场景产生连带副作用。
2. **最小权限原则（Root 与敏感权限按需申请）**：
   * **Root 权限视业务决定**：纯 LSPosed Hook 模块通常无需申请 Root（`su`），与底层 Magisk / KernelSU / APatch 管理器天然解耦；若业务确实需要执行系统级命令（如读写受限文件、写入 Doze 白名单、重启特定系统服务等），可将其设计为**可选 Root 能力**，配合实时 `su` 探针检测与无 Root 降级路径；
   * **克制权限清单**：不申请与核心功能无关的 `INTERNET`、`QUERY_ALL_PACKAGES`、无障碍、悬浮窗与外部存储权限（桌面应用查询优先 `ACTION_MAIN / CATEGORY_LAUNCHER` + 手动输入包名，配置导出/导入优先系统 SAF 选择器）。
3. **对抗原厂 R8 混淆的系统组件（如 Pixel `SystemUI`）**：
   * 原厂系统应用私有方法名与字段名常随月度安全补丁变化；涉及系统弹窗或界面交互自动化时，优先依赖 **Android 公开 SDK API**（如按字段类型匹配 `Dialog`/`AlertDialog`、遍历 View 树操作标准控件），并加严格来源门控与独立 `try-catch (Throwable)` 降级兜底，失败时安全回退为原系统行为，绝不崩溃。

## 5. 配置同步、热路径性能与可观测性

1. **配置同步与热路径缓存（`Remote Preferences`）**：
   * App 端通过 `XposedServiceHelper.registerListener(...)` 获取 `XposedService` 并读写 `getRemotePreferences("...")`（注意 `XposedProvider` 为 Direct Boot Aware，访问凭据加密存储前必须校验 `UserManager.isUserUnlocked()`）。
   * Hook 端通过 `XposedModule.getRemotePreferences("...")` 读取并注册 `OnSharedPreferenceChangeListener` 实时刷新内存 `ConcurrentHashMap`，确保修改配置免重启手机，且 120Hz 高频系统回调**零磁盘 IO**。
2. **状态自检与双轨日志**：
   * 结合 `XposedService` 实时状态（框架名、API 版本、作用域）与 `Settings.Global.BOOT_COUNT` 开机心跳，在首页直观展示模块激活状态与作用域完整性；
   * 关键挂载状态、毫秒级耗时与去重异常同步输出至框架日志与可选本地循环日志页；详细命中日志默认关闭，仅排错时手动开启。

## 6. GitHub Actions 云编译与 AI Studio 避坑清单

1. **免 Gradle Wrapper 二进制依赖**：使用 `actions/setup-java@v5` + `gradle/actions/setup-gradle@v4`（配置 `gradle-version: wrapper`），直接调用 `gradle` 命令，避免因缺少 `gradlew` / `gradle-wrapper.jar` 导致 CI 失败。
2. **SDK 整数化与 `libxposed 102` 的 `minCompileSdk=37` 兼容**：
   * `compileSdk` 与 `targetSdk` 统一使用标准整数（如 `36`），避免云端缺失小版本 SDK（如 `36.1`）。
   * 因 `io.github.libxposed:service:102.x` 与 `interface:102.x` 声明了 `minCompileSdk=37`（实际 `minSdk=26` 且无 API 37 专属编译符号），在 `compileSdk = 36` 的云端环境下必须在 `app/build.gradle.kts` **文件末尾**加入以下规则跳过元数据版本校验（待云端全面支持 API 37 后升级 `compileSdk = 37` 即可移除）：
     ```kotlin
     tasks.configureEach {
         if (name.startsWith("check") && name.endsWith("AarMetadata")) {
             enabled = false
         }
     }
     ```
3. **AGP 9 / Gradle 9 Lint 避坑**：`app/build.gradle.kts` 配置 `lint { checkReleaseBuilds = false; abortOnError = false }` 后，Release 编译命令直接执行 `gradle assembleRelease --no-configuration-cache`，**不得再附加 `-x lintVitalAnalyzeRelease`**（Gradle 9 下未注册该任务会报 `Task not found`）。
4. **AI Studio 与 GitHub Actions 双轨版本号解耦**：
   * AI Studio 的内置静态解析器会扫描并自动递增 `defaultConfig` 中的静态 `versionCode` / `versionName`；
   * 保持 `defaultConfig` 内的 `versionCode` / `versionName` 为干净静态数字供 AI Studio 预览使用，**严禁在 `defaultConfig` 前插入内联 Lambda**；仅在检测到 `System.getenv("GITHUB_ACTIONS") == "true"` 时，通过底部独立辅助逻辑（或变体输出钩子）按 `GITHUB_RUN_NUMBER` 与 `RELEASE_VERSION`（去除 `v` 前缀）动态覆写云端发包版本号，实现本地预览与 GitHub 发包彻底解耦、互不覆盖。
