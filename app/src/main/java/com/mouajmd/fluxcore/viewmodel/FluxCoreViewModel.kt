package com.mouajmd.fluxcore.viewmodel

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.PowerManager
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mouajmd.fluxcore.data.AppLogger
import com.mouajmd.fluxcore.data.AppSettings
import com.mouajmd.fluxcore.data.LanguageMode
import com.mouajmd.fluxcore.data.LogEntry
import com.mouajmd.fluxcore.data.RuleStore
import com.mouajmd.fluxcore.data.ScriptRepository
import com.mouajmd.fluxcore.data.SettingsStore
import com.mouajmd.fluxcore.data.ThemeMode
import com.mouajmd.fluxcore.engine.ActionExecutor
import com.mouajmd.fluxcore.model.AutomationRule
import com.mouajmd.fluxcore.model.ShellScriptItem
import com.mouajmd.fluxcore.service.FluxService
import com.mouajmd.fluxcore.system.RootShell
import com.mouajmd.fluxcore.system.ShortcutHelper
import com.mouajmd.fluxcore.system.TileRequestOutcome
import com.mouajmd.fluxcore.system.TileSlotManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Immutable
data class SystemHealthStatus(
    val hasRoot: Boolean = false,
    val batteryUnrestricted: Boolean = false,
    val hasDndAccess: Boolean = false,
    val serviceRunning: Boolean = false,
    val checkedOnce: Boolean = false
) {
    val allReady: Boolean
        get() = hasRoot && batteryUnrestricted && hasDndAccess && serviceRunning
}

class FluxCoreViewModel(application: Application) : AndroidViewModel(application) {
    private val appCtx get() = getApplication<Application>()

    val rules: StateFlow<List<AutomationRule>> = RuleStore.rules
    val activeRuleIds: StateFlow<Set<String>> = ActionExecutor.activeRuleIds
    val settings: StateFlow<AppSettings> = SettingsStore.settings
    val logs: StateFlow<List<LogEntry>> = AppLogger.entries
    val serviceRunning: StateFlow<Boolean> = FluxService.isRunning

    private val _rawHealth = MutableStateFlow(SystemHealthStatus())
    val systemHealth: StateFlow<SystemHealthStatus> = combine(
        _rawHealth,
        FluxService.isRunning
    ) { health, running ->
        health.copy(serviceRunning = running)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SystemHealthStatus(serviceRunning = FluxService.isRunning.value)
    )

    private val _busyRuleIds = MutableStateFlow<Set<String>>(emptySet())
    val busyRuleIds: StateFlow<Set<String>> = _busyRuleIds.asStateFlow()

    private val _isCleaning = MutableStateFlow(false)
    val isCleaning: StateFlow<Boolean> = _isCleaning.asStateFlow()

    private val _shellRunning = MutableStateFlow(false)
    val shellRunning: StateFlow<Boolean> = _shellRunning.asStateFlow()

    private val _shellOutput = MutableStateFlow("")
    val shellOutput: StateFlow<String> = _shellOutput.asStateFlow()

    private val _scannedScripts = MutableStateFlow<List<ShellScriptItem>>(emptyList())
    val scannedScripts: StateFlow<List<ShellScriptItem>> = _scannedScripts.asStateFlow()

    private val _isScanningScripts = MutableStateFlow(false)
    val isScanningScripts: StateFlow<Boolean> = _isScanningScripts.asStateFlow()

    private val _localBackupFiles = MutableStateFlow<List<File>>(emptyList())
    val localBackupFiles: StateFlow<List<File>> = _localBackupFiles.asStateFlow()

    init {
        refreshSystemStatus()
    }

    private fun readSystemHealth(): SystemHealthStatus {
        val rootOk = RootShell.hasRoot()
        val pm = appCtx.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batteryOk = runCatching {
            pm?.isIgnoringBatteryOptimizations(appCtx.packageName) == true
        }.getOrDefault(false)
        val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val dndOk = runCatching {
            nm?.isNotificationPolicyAccessGranted == true
        }.getOrDefault(false)

        if (rootOk && !FluxService.isRunning.value) {
            FluxService.start(appCtx)
        }

        return SystemHealthStatus(
            hasRoot = rootOk,
            batteryUnrestricted = batteryOk,
            hasDndAccess = dndOk,
            serviceRunning = FluxService.isRunning.value,
            checkedOnce = true
        )
    }

    fun refreshSystemStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            _rawHealth.value = readSystemHealth()
        }
    }

    fun tryGrantDndViaRoot(onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            var granted = runCatching {
                nm?.isNotificationPolicyAccessGranted == true
            }.getOrDefault(false)

            if (!granted && RootShell.hasRoot()) {
                val pkg = RootShell.shellQuote(appCtx.packageName)
                val res = RootShell.execDetailed("cmd notification allow_dnd $pkg")
                AppLogger.log(
                    "PERM",
                    "Root allow_dnd (exit=${res.exitCode}): ${res.output.ifBlank { "ok" }}"
                )
                granted = runCatching {
                    nm?.isNotificationPolicyAccessGranted == true
                }.getOrDefault(false)
                if (!granted) {
                    delay(150L)
                    granted = runCatching {
                        nm?.isNotificationPolicyAccessGranted == true
                    }.getOrDefault(false)
                }
            }

            _rawHealth.value = readSystemHealth()
            withContext(Dispatchers.Main) {
                onResult(granted)
            }
        }
    }

    fun startEngineService() {
        FluxService.start(appCtx)
        refreshSystemStatus()
    }

    fun toggleRuleEnabled(rule: AutomationRule, enabled: Boolean) {
        val updated = rule.copy(enabled = enabled)
        viewModelScope.launch(Dispatchers.IO) {
            RuleStore.upsert(appCtx, updated)
            if (!enabled && ActionExecutor.isActive(rule.id)) {
                ActionExecutor.deactivate(appCtx, updated)
            }
        }
    }

    fun toggleRuleActive(rule: AutomationRule) {
        if (rule.id in _busyRuleIds.value) return
        _busyRuleIds.value = _busyRuleIds.value + rule.id
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ActionExecutor.toggle(appCtx, rule)
            } finally {
                _busyRuleIds.value = _busyRuleIds.value - rule.id
            }
        }
    }

    fun saveRule(rule: AutomationRule) {
        viewModelScope.launch(Dispatchers.IO) {
            val old = RuleStore.findById(appCtx, rule.id)
            RuleStore.upsert(appCtx, rule)
            if (old != null && old.name != rule.name) {
                ShortcutHelper.syncRenamedShortcut(appCtx, rule)
            }
            TileSlotManager.syncAllTiles(appCtx)
            AppLogger.log("RULE", "Saved profile [${rule.name}] (${rule.actionSteps.size} steps)")
        }
    }

    fun deleteRule(rule: AutomationRule) {
        viewModelScope.launch(Dispatchers.IO) {
            if (ActionExecutor.isActive(rule.id)) {
                ActionExecutor.deactivate(appCtx, rule)
            }
            RuleStore.delete(appCtx, rule.id)
            TileSlotManager.releaseRuleSlot(appCtx, rule.id)
            ShortcutHelper.disableDeletedShortcut(appCtx, rule.id)
            AppLogger.log("RULE", "Deleted profile [${rule.name}]")
        }
    }

    fun requestAddQsTile(rule: AutomationRule, onOutcome: (TileRequestOutcome) -> Unit) {
        TileSlotManager.bindAndRequestTile(appCtx, rule, onOutcome)
    }

    fun requestPinShortcut(rule: AutomationRule, customLabel: String?): Boolean {
        return ShortcutHelper.requestPinRuleShortcut(appCtx, rule, customLabel)
    }

    fun runDeepCleanup(onDone: () -> Unit) {
        if (_isCleaning.value) return
        _isCleaning.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ActionExecutor.cleanAllBackground(appCtx)
            } finally {
                _isCleaning.value = false
                withContext(Dispatchers.Main) { onDone() }
            }
        }
    }

    fun runQuickShell(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty() || _shellRunning.value) return
        _shellRunning.value = true
        viewModelScope.launch(Dispatchers.IO) {
            AppLogger.log("CONSOLE", "$ $trimmed")
            val res = RootShell.execDetailed(trimmed, timeoutMs = RootShell.EXTENDED_TIMEOUT_MS)
            if (!res.isSuccess) {
                AppLogger.error("CONSOLE", "exit=${res.exitCode}: ${res.output}")
            }
            _shellOutput.value = res.output
            _shellRunning.value = false
        }
    }

    fun clearShellOutput() {
        _shellOutput.value = ""
    }

    fun scanLocalScripts() {
        if (_isScanningScripts.value) return
        _isScanningScripts.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val found = ScriptRepository.scanLocalShellScripts()
            _scannedScripts.value = found
            _isScanningScripts.value = false
            AppLogger.log("SCAN", "Found ${found.size} local .sh scripts")
        }
    }

    suspend fun readScriptPreview(path: String): String = withContext(Dispatchers.IO) {
        ScriptRepository.readScriptPreview(path)
    }

    suspend fun detectCurrentWifi(): String = withContext(Dispatchers.IO) {
        RootShell.queryCurrentWifiSsid()
    }

    suspend fun detectConnectedBluetooth(): List<String> = withContext(Dispatchers.IO) {
        RootShell.parseBtCandidates(RootShell.queryConnectedBtDevices())
    }

    fun refreshLocalBackupFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _localBackupFiles.value = RuleStore.listLocalBackupFiles()
        }
    }

    // Settings
    fun setAdvancedMode(enabled: Boolean) {
        SettingsStore.setAdvancedMode(appCtx, enabled)
        AppLogger.log("SETTINGS", "Advanced mode = $enabled")
    }

    fun setHapticEnabled(enabled: Boolean) {
        SettingsStore.setHapticEnabled(appCtx, enabled)
    }

    fun setHidePersistentNotification(hide: Boolean, onComplete: (appliedViaRoot: Boolean) -> Unit) {
        SettingsStore.setHidePersistentNotification(appCtx, hide)
        FluxService.ensureDaemonChannel(appCtx)
        viewModelScope.launch(Dispatchers.IO) {
            val mode = if (hide) "ignore" else "allow"
            var appliedViaRoot = false
            if (RootShell.hasRoot()) {
                val pkg = RootShell.shellQuote(appCtx.packageName)
                val res = RootShell.execDetailed("cmd appops set $pkg POST_NOTIFICATION $mode")
                appliedViaRoot = res.isSuccess
                AppLogger.log(
                    "PERM",
                    "Root POST_NOTIFICATION $mode (exit=${res.exitCode}): ${res.output.ifBlank { "ok" }}"
                )
            }
            if (appliedViaRoot && FluxService.isRunning.value) {
                FluxService.refreshNotification(appCtx)
            }
            withContext(Dispatchers.Main) {
                onComplete(appliedViaRoot)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        SettingsStore.setThemeMode(appCtx, mode)
    }

    fun setDynamicColor(enabled: Boolean) {
        SettingsStore.setDynamicColor(appCtx, enabled)
    }

    fun setPaletteIndex(index: Int) {
        SettingsStore.setPaletteIndex(appCtx, index)
    }

    fun setLanguageMode(mode: LanguageMode) {
        SettingsStore.setLanguageMode(appCtx, mode)
    }

    // Export / Import
    fun exportConfigToUri(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = RuleStore.exportToUri(appCtx, uri)
            withContext(Dispatchers.Main) { onResult(ok) }
        }
    }

    fun exportConfigToDownload(onResult: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val path = RuleStore.exportToDownload(appCtx)
            if (path != null) {
                _localBackupFiles.value = RuleStore.listLocalBackupFiles()
            }
            withContext(Dispatchers.Main) { onResult(path) }
        }
    }

    fun importConfigFromUri(uri: Uri, overwrite: Boolean, onResult: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = RuleStore.importFromUri(appCtx, uri, overwrite)
            withContext(Dispatchers.Main) { onResult(count) }
        }
    }

    fun importConfigFromFile(file: File, overwrite: Boolean, onResult: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = RuleStore.importFromFile(appCtx, file, overwrite)
            withContext(Dispatchers.Main) { onResult(count) }
        }
    }

    fun exportLogsToUri(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = AppLogger.exportToUri(appCtx, uri)
            withContext(Dispatchers.Main) { onResult(ok) }
        }
    }

    fun exportLogsToDownload(onResult: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val path = AppLogger.exportToDownload(appCtx)
            withContext(Dispatchers.Main) { onResult(path) }
        }
    }

    fun clearLogs() = AppLogger.clear()
}
