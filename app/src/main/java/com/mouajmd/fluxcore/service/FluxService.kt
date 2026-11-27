package com.mouajmd.fluxcore.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.mouajmd.fluxcore.BuildConfig
import com.mouajmd.fluxcore.MainActivity
import com.mouajmd.fluxcore.R
import com.mouajmd.fluxcore.data.AppLogger
import com.mouajmd.fluxcore.data.RuleStore
import com.mouajmd.fluxcore.engine.ActionExecutor
import com.mouajmd.fluxcore.engine.RuleMatcher
import com.mouajmd.fluxcore.engine.SystemEnvironmentState
import com.mouajmd.fluxcore.model.TriggerType
import com.mouajmd.fluxcore.system.RootShell
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import kotlin.math.min
import kotlin.math.roundToInt

class FluxService : Service() {

    companion object {
        private const val REVERT_DEBOUNCE_MS = 1_500L
        private const val FOCUS_POLL_MS = 850L
        private const val BACKOFF_INITIAL_MS = 2_000L
        private const val BACKOFF_MAX_MS = 15_000L

        private val EVENT_RESUME_PATTERN: Pattern = Pattern.compile(
            "(?:am_on_resume_called|wm_on_resume_called|am_focused_activity|wm_set_resumed_activity|am_resumed):\\s*\\[[^\\]]*?([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+)[,/\\]]"
        )
        private val FOCUS_PKG_PATTERN: Pattern = Pattern.compile(
            "([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+)/"
        )

        const val DAEMON_CHANNEL_ID = "fluxcore_daemon_channel"
        const val DAEMON_NOTIF_ID = 101
        private const val LEGACY_CHANNEL_ID = "fluxcore_engine"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context) {
            runCatching {
                val intent = Intent(context.applicationContext, FluxService::class.java)
                context.applicationContext.startForegroundService(intent)
            }.onFailure {
                AppLogger.warn("ENGINE", "Service start skipped: ${it.message}")
            }
        }

        fun ensureDaemonChannel(context: Context) {
            val appCtx = context.applicationContext
            val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            runCatching { nm.deleteNotificationChannel(LEGACY_CHANNEL_ID) }
            val channel = NotificationChannel(
                DAEMON_CHANNEL_ID,
                appCtx.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
            }
            nm.createNotificationChannel(channel)
        }

        fun buildDaemonNotification(context: Context): Notification {
            val appCtx = context.applicationContext
            ensureDaemonChannel(appCtx)
            val openIntent = Intent(appCtx, MainActivity::class.java)
            val pending = PendingIntent.getActivity(
                appCtx,
                0,
                openIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            return Notification.Builder(appCtx, DAEMON_CHANNEL_ID)
                .setContentTitle(appCtx.getString(R.string.notif_title))
                .setContentText(appCtx.getString(R.string.notif_body))
                .setSmallIcon(R.drawable.ic_qs_flux)
                .setContentIntent(pending)
                .setOngoing(true)
                .build()
        }

        fun refreshNotification(context: Context) {
            if (!_isRunning.value) return
            val appCtx = context.applicationContext
            runCatching {
                val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.notify(DAEMON_NOTIF_ID, buildDaemonNotification(appCtx))
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val evalLock = Any()
    private val pendingRevertJobs = ConcurrentHashMap<String, Job>()
    private val screenOnFlow = MutableStateFlow(true)

    @Volatile private var logcatProc: Process? = null
    @Volatile private var focusWatcherProc: Process? = null

    @Volatile private var currentFgPkg = ""
    @Volatile private var isCharging = false
    @Volatile private var batteryLevel = 50
    @Volatile private var isScreenOn = true
    @Volatile private var isWifiConnected = false
    @Volatile private var currentWifiSsid = ""
    @Volatile private var isBtConnected = false
    @Volatile private var connectedBtDump = ""
    @Volatile private var isHeadsetPlugged = false
    @Volatile private var isLandscape = false

    private val sysReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val act = intent.action ?: return
            when (act) {
                Intent.ACTION_POWER_CONNECTED -> {
                    isCharging = true
                    AppLogger.log("EVENT", "Power connected")
                    scheduleEvaluateAllRules()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    isCharging = false
                    AppLogger.log("EVENT", "Power disconnected")
                    scheduleEvaluateAllRules()
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val lvl = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                    if (lvl >= 0 && scale > 0) {
                        val pct = ((lvl * 100f) / scale).roundToInt().coerceIn(0, 100)
                        if (pct != batteryLevel) {
                            batteryLevel = pct
                            scheduleEvaluateAllRules()
                        }
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    screenOnFlow.value = true
                    AppLogger.log("EVENT", "Screen turned on")
                    scheduleEvaluateAllRules()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    screenOnFlow.value = false
                    AppLogger.log("EVENT", "Screen turned off")
                    // Immediately tear down focus watcher shell while sleeping to avoid wakeups
                    destroyProcessQuietly(focusWatcherProc)
                    focusWatcherProc = null
                    serviceScope.launch {
                        ActionExecutor.onScreenOffSafetyRevert(applicationContext)
                        evaluateAllRules()
                    }
                }
                AudioManager.ACTION_HEADSET_PLUG -> {
                    val plugged = intent.getIntExtra("state", 0) == 1
                    if (plugged != isHeadsetPlugged) {
                        isHeadsetPlugged = plugged
                        AppLogger.log("EVENT", "Wired headset ${if (plugged) "plugged in" else "unplugged"}")
                        scheduleEvaluateAllRules()
                    }
                }
                Intent.ACTION_CONFIGURATION_CHANGED -> {
                    val land = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    if (land != isLandscape) {
                        isLandscape = land
                        AppLogger.log("EVENT", "Orientation -> ${if (land) "Landscape" else "Portrait"}")
                        scheduleEvaluateAllRules()
                    }
                }
                Intent.ACTION_TIME_TICK -> scheduleEvaluateAllRules()
                @Suppress("DEPRECATION")
                ConnectivityManager.CONNECTIVITY_ACTION -> refreshNetworkAndBtAsync("Network changed")
                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothDevice.ACTION_ACL_DISCONNECTED,
                BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
                    refreshNetworkAndBtAsync("Bluetooth changed")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        _isRunning.value = true
        AppLogger.log("ENGINE", "FluxCore v${BuildConfig.VERSION_NAME} engine started")
        startForegroundCompat()
        initInitialStates()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction(Intent.ACTION_CONFIGURATION_CHANGED)
            addAction(Intent.ACTION_TIME_TICK)
            @Suppress("DEPRECATION")
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(sysReceiver, filter, RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(sysReceiver, filter)
        }

        launchEventStreamWatcher()
        launchFocusWatcher()
    }

    private fun initInitialStates() {
        runCatching {
            val bat = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (bat != null) {
                isCharging = bat.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
                val lvl = bat.getIntExtra(BatteryManager.EXTRA_LEVEL, 50)
                val scale = bat.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                if (scale > 0) batteryLevel = ((lvl * 100f) / scale).roundToInt().coerceIn(0, 100)
            }
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm != null) {
                isScreenOn = pm.isInteractive
                screenOnFlow.value = isScreenOn
            }
            isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }
        refreshNetworkAndBtAsync("Initial hardware probe")
    }

    private fun startForegroundCompat() {
        startForeground(DAEMON_NOTIF_ID, buildDaemonNotification(this))
    }

    private fun onForegroundPackageDetected(pkg: String) {
        if (pkg.isBlank() || pkg == "com.android.systemui") return
        if (pkg != currentFgPkg) {
            currentFgPkg = pkg
            AppLogger.log("APP", "Foreground -> $pkg")
            evaluateAllRules()
        }
    }

    private fun refreshNetworkAndBtAsync(reason: String) {
        serviceScope.launch {
            runCatching {
                val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
                val wifiOk = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                isWifiConnected = wifiOk
                currentWifiSsid = if (wifiOk) RootShell.queryCurrentWifiSsid() else ""

                val btDump = RootShell.queryConnectedBtDevices()
                connectedBtDump = btDump
                val lower = btDump.lowercase()
                var btOn = lower.contains("connected") && !lower.contains("disconnected")
                if (btDump.contains("mActiveDevice=") && !btDump.contains("mActiveDevice=null")) {
                    btOn = true
                }
                isBtConnected = btOn

                AppLogger.log(
                    "STATE",
                    "$reason -> Wi-Fi=$isWifiConnected${if (currentWifiSsid.isNotEmpty()) "($currentWifiSsid)" else ""}, BT=$isBtConnected, Bat=$batteryLevel%"
                )
                evaluateAllRules()
            }
        }
    }

    private fun launchEventStreamWatcher() {
        serviceScope.launch {
            var backoffMs = BACKOFF_INITIAL_MS
            while (isActive) {
                // Suspend cleanly with zero wakeups while display is off
                if (!screenOnFlow.value) {
                    screenOnFlow.first { it }
                    backoffMs = BACKOFF_INITIAL_MS
                }
                var proc: Process? = null
                try {
                    proc = ProcessBuilder("su").redirectErrorStream(true).start()
                    logcatProc = proc
                    val os = DataOutputStream(proc.outputStream)
                    os.writeBytes("logcat -b events -T 1 -v brief *:I\n")
                    os.flush()

                    BufferedReader(InputStreamReader(proc.inputStream, StandardCharsets.UTF_8)).use { br ->
                        var line: String? = null
                        while (isActive && br.readLine().also { line = it } != null) {
                            backoffMs = BACKOFF_INITIAL_MS
                            val m = EVENT_RESUME_PATTERN.matcher(line.orEmpty())
                            if (m.find()) {
                                m.group(1)?.let { onForegroundPackageDetected(it) }
                            }
                        }
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (_: Throwable) {
                    // Exponential backoff handles missing Root or transient logcat resets gracefully
                } finally {
                    destroyProcessQuietly(proc)
                    if (logcatProc === proc) logcatProc = null
                }

                if (isActive) {
                    delay(backoffMs)
                    backoffMs = min(backoffMs * 2, BACKOFF_MAX_MS)
                }
            }
        }
    }

    private fun launchFocusWatcher() {
        serviceScope.launch {
            var backoffMs = BACKOFF_INITIAL_MS
            while (isActive) {
                // Zero-wakeup suspension when screen is OFF
                if (!screenOnFlow.value) {
                    screenOnFlow.first { it }
                    backoffMs = BACKOFF_INITIAL_MS
                }
                var proc: Process? = null
                try {
                    proc = ProcessBuilder("su").redirectErrorStream(true).start()
                    focusWatcherProc = proc
                    val os = DataOutputStream(proc.outputStream)
                    val br = BufferedReader(InputStreamReader(proc.inputStream, StandardCharsets.UTF_8))

                    while (isActive && screenOnFlow.value) {
                        os.writeBytes("dumpsys window 2>/dev/null | grep -E 'mCurrentFocus|mFocusedApp' | tail -n 2; echo __END__\n")
                        os.flush()

                        var line: String? = null
                        var detected = ""
                        var gotSentinel = false
                        while (isActive && br.readLine().also { line = it } != null) {
                            val trimmed = line?.trim().orEmpty()
                            if (trimmed == "__END__") {
                                gotSentinel = true
                                break
                            }
                            val m = FOCUS_PKG_PATTERN.matcher(trimmed)
                            while (m.find()) {
                                detected = m.group(1).orEmpty()
                            }
                        }
                        if (!gotSentinel) break
                        backoffMs = BACKOFF_INITIAL_MS
                        if (detected.isNotEmpty()) {
                            onForegroundPackageDetected(detected)
                        }
                        delay(FOCUS_POLL_MS)
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (_: Throwable) {
                    // Exponential backoff handles missing Root or broken pipe
                } finally {
                    destroyProcessQuietly(proc)
                    if (focusWatcherProc === proc) focusWatcherProc = null
                }

                if (isActive && screenOnFlow.value) {
                    delay(backoffMs)
                    backoffMs = min(backoffMs * 2, BACKOFF_MAX_MS)
                }
            }
        }
    }

    private fun currentEnvironmentState(): SystemEnvironmentState = SystemEnvironmentState(
        currentFgPkg = currentFgPkg,
        isCharging = isCharging,
        batteryLevel = batteryLevel,
        isScreenOn = isScreenOn,
        isWifiConnected = isWifiConnected,
        currentWifiSsid = currentWifiSsid,
        isBtConnected = isBtConnected,
        connectedBtDump = connectedBtDump,
        isHeadsetPlugged = isHeadsetPlugged,
        isLandscape = isLandscape
    )

    private fun scheduleEvaluateAllRules() {
        serviceScope.launch { evaluateAllRules() }
    }

    private fun evaluateAllRules() = synchronized(evalLock) {
        val rules = RuleStore.getSnapshot(this)
        val env = currentEnvironmentState()
        for (rule in rules) {
            if (!rule.enabled || rule.triggerType == TriggerType.MANUAL) continue

            if (RuleMatcher.isRuleMatched(rule, env)) {
                pendingRevertJobs.remove(rule.id)?.cancel()
                ActionExecutor.activate(this, rule)
            } else if (ActionExecutor.isActive(rule.id)) {
                if (rule.triggerType == TriggerType.APP) {
                    if (!pendingRevertJobs.containsKey(rule.id)) {
                        val ruleId = rule.id
                        val job = serviceScope.launch {
                            delay(REVERT_DEBOUNCE_MS)
                            pendingRevertJobs.remove(ruleId)
                            val top = RootShell.queryTopPackageViaDumpsys()
                            if (top.isNotEmpty() && top != "com.android.systemui") {
                                currentFgPkg = top
                            }
                            val latestRule = RuleStore.findById(this@FluxService, ruleId) ?: rule
                            if (!RuleMatcher.isRuleMatched(latestRule, currentEnvironmentState())) {
                                ActionExecutor.deactivate(this@FluxService, latestRule)
                            }
                        }
                        pendingRevertJobs[ruleId] = job
                    }
                } else {
                    ActionExecutor.deactivate(this, rule)
                }
            }
        }
    }

    private fun destroyProcessQuietly(proc: Process?) {
        if (proc == null) return
        runCatching { proc.outputStream.close() }
        runCatching { proc.inputStream.close() }
        runCatching { proc.errorStream.close() }
        runCatching { proc.destroyForcibly() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        _isRunning.value = false
        AppLogger.log("ENGINE", "FluxCore engine stopped")
        runCatching { unregisterReceiver(sysReceiver) }
        pendingRevertJobs.values.forEach { it.cancel() }
        pendingRevertJobs.clear()
        destroyProcessQuietly(logcatProc)
        destroyProcessQuietly(focusWatcherProc)
        logcatProc = null
        focusWatcherProc = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
