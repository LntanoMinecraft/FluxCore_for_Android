package com.mouajmd.fluxcore.engine

import android.content.Context
import android.media.AudioManager
import com.mouajmd.fluxcore.data.AppLogger
import com.mouajmd.fluxcore.model.ActionStepType
import com.mouajmd.fluxcore.model.AutomationRule
import com.mouajmd.fluxcore.system.BrightnessBaseline
import com.mouajmd.fluxcore.system.HardwareDisplayController
import com.mouajmd.fluxcore.system.HbmProbeResult
import com.mouajmd.fluxcore.system.RootShell
import com.mouajmd.fluxcore.system.ShellResult
import com.mouajmd.fluxcore.system.TileSlotManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Concurrency-safe automation execution engine with:
 * - Reference-counted shared system baselines so overlapping rules never clobber each other's state.
 * - Per-rule cancellation tokens so deactivating mid-DELAY aborts remaining steps immediately.
 * - Mandatory atomic HBM peak-brightness safety revert on rule exit or display screen-off.
 */
object ActionExecutor {

    private enum class ManagedResource {
        DND,
        REFRESH_RATE,
        BRIGHTNESS,
        MEDIA_VOLUME,
        WIFI,
        MOBILE_DATA,
        AIRPLANE_MODE,
        BATTERY_SAVER,
        DARK_MODE,
        AUTO_ROTATE
    }

    private data class CapturedBaselines(
        var dnd: String? = null,
        var minHz: String? = null,
        var peakHz: String? = null,
        var brightness: BrightnessBaseline? = null,
        var hbmProbe: HbmProbeResult? = null,
        var mediaVol: Int = -1,
        var wifiOn: String? = null,
        var dataOn: String? = null,
        var airplaneOn: String? = null,
        var lowPower: String? = null,
        var nightMode: String? = null,
        var autoRotate: String? = null
    )

    private val stateLock = Any()
    private val baselines = CapturedBaselines()
    private val resourceOwners = HashMap<ManagedResource, LinkedHashSet<String>>()
    private val activeRules = LinkedHashMap<String, AutomationRule>()
    private val runningTokens = HashMap<String, AtomicBoolean>()
    private val hbmActiveRuleIds = LinkedHashSet<String>()

    private val _activeRuleIds = MutableStateFlow<Set<String>>(emptySet())
    val activeRuleIds: StateFlow<Set<String>> = _activeRuleIds.asStateFlow()

    fun isActive(ruleId: String): Boolean = synchronized(stateLock) {
        activeRules.containsKey(ruleId)
    }

    fun toggle(context: Context, rule: AutomationRule): Boolean {
        val nowActive = synchronized(stateLock) { activeRules.containsKey(rule.id) }
        return if (nowActive) {
            deactivate(context, rule)
            false
        } else {
            activate(context, rule)
            true
        }
    }

    fun activate(context: Context, rule: AutomationRule) {
        val token: AtomicBoolean
        val resourcesToCapture = mutableSetOf<ManagedResource>()

        synchronized(stateLock) {
            if (activeRules.containsKey(rule.id)) return
            activeRules[rule.id] = rule
            token = AtomicBoolean(false)
            runningTokens[rule.id] = token
            _activeRuleIds.value = activeRules.keys.toSet()

            val claimed = resourcesClaimedBy(rule)
            for (res in claimed) {
                val owners = resourceOwners.getOrPut(res) { LinkedHashSet() }
                if (owners.isEmpty()) {
                    resourcesToCapture.add(res)
                }
                owners.add(rule.id)
            }
        }

        val appCtx = context.applicationContext
        AppLogger.log("EXEC", "Activate [${rule.name}] (${rule.actionSteps.size} steps)")

        // Capture true original system baselines ONLY for newly claimed resources
        captureMissingBaselines(appCtx, resourcesToCapture)

        // Execute user-ordered pipeline steps with cancellation checks
        for ((index, step) in rule.actionSteps.withIndex()) {
            if (token.get()) {
                AppLogger.log("EXEC", "[${rule.name}] Aborted remaining steps (rule deactivated)")
                break
            }
            val hbmProbe = ActionStepHandler.executeStep(appCtx, rule.name, index + 1, step, token)
            if (hbmProbe != null) {
                var mustImmediatelyRevertHbm = false
                synchronized(stateLock) {
                    if (token.get() || !activeRules.containsKey(rule.id)) {
                        mustImmediatelyRevertHbm = true
                    } else {
                        hbmActiveRuleIds.add(rule.id)
                        if (baselines.hbmProbe == null || hbmProbe.hbmNodePath != null) {
                            baselines.hbmProbe = hbmProbe
                        }
                    }
                }
                if (mustImmediatelyRevertHbm) {
                    val revertSb = StringBuilder()
                    val baseBright = synchronized(stateLock) { baselines.brightness }
                    HardwareDisplayController.appendRevertCommands(revertSb, baseBright, hbmProbe)
                    if (revertSb.isNotEmpty()) {
                        RootShell.execDetailed(revertSb.toString())
                        AppLogger.warn("HBM", "[${rule.name}] Race guard: immediately reverted HBM after cancel")
                    }
                }
            }
        }

        synchronized(stateLock) {
            if (runningTokens[rule.id] === token) {
                runningTokens.remove(rule.id)
            }
        }

        TileSlotManager.syncAllTiles(appCtx)
    }

    fun deactivate(context: Context, rule: AutomationRule) {
        val appCtx = context.applicationContext
        val releasedLastOwner = mutableSetOf<ManagedResource>()
        val handoffResources = HashMap<ManagedResource, AutomationRule>()
        val hadHbm: Boolean
        val shouldSafetyRevertHbm: Boolean
        val capturedSnapshot: CapturedBaselines

        synchronized(stateLock) {
            val activeRule = activeRules.remove(rule.id) ?: return
            runningTokens.remove(rule.id)?.set(true)
            _activeRuleIds.value = activeRules.keys.toSet()

            hadHbm = hbmActiveRuleIds.remove(rule.id)
            shouldSafetyRevertHbm = hadHbm && hbmActiveRuleIds.isEmpty()

            val claimed = resourcesClaimedBy(activeRule)
            for (res in claimed) {
                val owners = resourceOwners[res] ?: continue
                owners.remove(rule.id)
                if (owners.isEmpty()) {
                    resourceOwners.remove(res)
                    releasedLastOwner.add(res)
                } else {
                    val nextOwnerId = owners.lastOrNull()
                    val nextOwnerRule = nextOwnerId?.let { activeRules[it] }
                    if (nextOwnerRule != null) {
                        handoffResources[res] = nextOwnerRule
                    }
                }
            }

            // Copy baseline values for safe out-of-lock reversion
            capturedSnapshot = baselines.copy()

            // Clear baseline fields that are fully released now
            clearReleasedBaselinesLocked(releasedLastOwner, clearHbm = shouldSafetyRevertHbm)
        }

        if (!rule.autoRevert) {
            // Mandatory HBM safety revert even when rule.autoRevert is disabled
            if (shouldSafetyRevertHbm) {
                val hbmSb = StringBuilder()
                HardwareDisplayController.appendRevertCommands(hbmSb, capturedSnapshot.brightness, capturedSnapshot.hbmProbe)
                if (hbmSb.isNotEmpty()) {
                    val res = RootShell.execDetailed(hbmSb.toString())
                    if (res.isSuccess) {
                        AppLogger.warn(
                            "HBM",
                            "[${rule.name}] Mandatory HBM safety revert executed (node=${capturedSnapshot.hbmProbe?.hbmNodePath ?: "fallback"})"
                        )
                    } else {
                        AppLogger.error("HBM", "[${rule.name}] Safety HBM revert failed: ${res.output}")
                    }
                }
            }
            AppLogger.log("STATE", "Exited [${rule.name}] (auto-revert disabled)")
            TileSlotManager.syncAllTiles(appCtx)
            return
        }

        AppLogger.log("REVERT", "Reverting [${rule.name}] in reverse order")

        // 1. Restore media volume if this rule was the last owner; otherwise hand off to remaining active rule
        if (ManagedResource.MEDIA_VOLUME in releasedLastOwner && capturedSnapshot.mediaVol >= 0) {
            runCatching {
                val am = appCtx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                am?.setStreamVolume(AudioManager.STREAM_MUSIC, capturedSnapshot.mediaVol, 0)
            }
        } else {
            handoffResources[ManagedResource.MEDIA_VOLUME]?.let { owner ->
                owner.actionSteps.lastOrNull { it.type == ActionStepType.MEDIA_VOLUME }?.let { step ->
                    ActionStepHandler.applyMediaVolume(appCtx, owner.name, 0, step.intValue)
                }
            }
        }

        // 2. Build batched Root shell revert script
        val sb = StringBuilder()
        ActionStepHandler.appendStepReverseActions(sb, rule.actionSteps)

        appendReleasedBaselineReverts(sb, releasedLastOwner, shouldSafetyRevertHbm, capturedSnapshot)

        // Re-apply overlapping rules' settings for any shared resource still owned by another active rule
        handoffResources.forEach { (res, owner) ->
            if (res == ManagedResource.MEDIA_VOLUME) return@forEach
            val stepType = res.toActionStepType()
            val step = owner.actionSteps.lastOrNull { it.type == stepType } ?: return@forEach
            if (step.type == ActionStepType.BRIGHTNESS) {
                if (!step.boolValue) {
                    HardwareDisplayController.applyPercentBrightness(owner.name, 0, step.intValue)
                }
            } else {
                val cmd = ActionStepHandler.buildShellCommand(appCtx, step)
                if (cmd.isNotBlank()) sb.append(cmd).append('\n')
            }
        }

        if (rule.revertCmd.trim().isNotEmpty()) {
            sb.append(rule.revertCmd.trim()).append('\n')
        }

        if (sb.isNotEmpty()) {
            val res = RootShell.execDetailed(sb.toString(), timeoutMs = RootShell.EXTENDED_TIMEOUT_MS)
            if (!res.isSuccess) {
                AppLogger.warn("REVERT", "[${rule.name}] revert exit=${res.exitCode}: ${res.output}")
            } else if (shouldSafetyRevertHbm) {
                AppLogger.log(
                    "HBM",
                    "[${rule.name}] HBM OFF & brightness restored (node=${capturedSnapshot.hbmProbe?.hbmNodePath ?: "fallback"})"
                )
            }
        }

        TileSlotManager.syncAllTiles(appCtx)
    }

    /**
     * Immediately turns off any active HBM peak brightness state and restores system brightness
     * when the display turns off, preventing burn-in or thermal runaway.
     */
    fun onScreenOffSafetyRevert(context: Context) {
        val hbmProbe: HbmProbeResult?
        val brightnessBase: BrightnessBaseline?
        val affectedRuleNames: List<String>

        synchronized(stateLock) {
            if (hbmActiveRuleIds.isEmpty()) return
            affectedRuleNames = hbmActiveRuleIds.mapNotNull { activeRules[it]?.name }
            hbmActiveRuleIds.clear()
            hbmProbe = baselines.hbmProbe
            brightnessBase = baselines.brightness
            baselines.hbmProbe = null
        }

        val sb = StringBuilder()
        HardwareDisplayController.appendRevertCommands(sb, brightnessBase, hbmProbe)
        if (sb.isNotEmpty()) {
            val res = RootShell.execDetailed(sb.toString())
            val names = affectedRuleNames.joinToString(", ").ifBlank { "HBM" }
            if (res.isSuccess) {
                AppLogger.warn(
                    "HBM",
                    "[$names] Screen OFF safety revert -> HBM disabled & brightness restored (node=${hbmProbe?.hbmNodePath ?: "fallback"})"
                )
            } else {
                AppLogger.error("HBM", "[$names] Screen OFF HBM safety revert error: ${res.output}")
            }
        }
        TileSlotManager.syncAllTiles(context.applicationContext)
    }

    fun cleanAllBackground(context: Context): ShellResult {
        AppLogger.log("CLEAN", "Running deep background cleanup")
        val self = RootShell.shellQuote(context.packageName)
        val cmd = "for p in \$(pm list packages -3 | cut -d: -f2); do " +
            "if [ \"\$p\" != $self ]; then am force-stop \"\$p\"; fi; " +
            "done; sync; echo 3 > /proc/sys/vm/drop_caches; echo DONE"
        val res = RootShell.execDetailed(cmd, timeoutMs = RootShell.EXTENDED_TIMEOUT_MS)
        if (res.isSuccess) {
            AppLogger.log("CLEAN", "Cleanup done: ${res.output}")
        } else {
            AppLogger.error("CLEAN", "Cleanup error: ${res.output}")
        }
        return res
    }

    private fun captureMissingBaselines(appCtx: Context, resources: Set<ManagedResource>) {
        if (resources.isEmpty()) return
        val dnd = if (ManagedResource.DND in resources) RootShell.exec("settings get global zen_mode") else null
        val minHz = if (ManagedResource.REFRESH_RATE in resources) RootShell.exec("settings get system min_refresh_rate") else null
        val peakHz = if (ManagedResource.REFRESH_RATE in resources) RootShell.exec("settings get system peak_refresh_rate") else null
        val bright = if (ManagedResource.BRIGHTNESS in resources) HardwareDisplayController.captureBaseline() else null
        val wifi = if (ManagedResource.WIFI in resources) RootShell.exec("settings get global wifi_on") else null
        val data = if (ManagedResource.MOBILE_DATA in resources) RootShell.exec("settings get global mobile_data") else null
        val air = if (ManagedResource.AIRPLANE_MODE in resources) RootShell.exec("settings get global airplane_mode_on") else null
        val saver = if (ManagedResource.BATTERY_SAVER in resources) RootShell.exec("settings get global low_power") else null
        val night = if (ManagedResource.DARK_MODE in resources) RootShell.exec("settings get secure ui_night_mode") else null
        val rot = if (ManagedResource.AUTO_ROTATE in resources) RootShell.exec("settings get system accelerometer_rotation") else null
        val vol = if (ManagedResource.MEDIA_VOLUME in resources) {
            runCatching {
                val am = appCtx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: -1
            }.getOrDefault(-1)
        } else {
            -1
        }

        synchronized(stateLock) {
            if (ManagedResource.DND in resources) baselines.dnd = dnd
            if (ManagedResource.REFRESH_RATE in resources) {
                baselines.minHz = minHz
                baselines.peakHz = peakHz
            }
            if (ManagedResource.BRIGHTNESS in resources) baselines.brightness = bright
            if (ManagedResource.WIFI in resources) baselines.wifiOn = wifi
            if (ManagedResource.MOBILE_DATA in resources) baselines.dataOn = data
            if (ManagedResource.AIRPLANE_MODE in resources) baselines.airplaneOn = air
            if (ManagedResource.BATTERY_SAVER in resources) baselines.lowPower = saver
            if (ManagedResource.DARK_MODE in resources) baselines.nightMode = night
            if (ManagedResource.AUTO_ROTATE in resources) baselines.autoRotate = rot
            if (ManagedResource.MEDIA_VOLUME in resources) baselines.mediaVol = vol
        }
    }

    private fun clearReleasedBaselinesLocked(released: Set<ManagedResource>, clearHbm: Boolean) {
        if (clearHbm) baselines.hbmProbe = null
        for (res in released) {
            when (res) {
                ManagedResource.DND -> baselines.dnd = null
                ManagedResource.REFRESH_RATE -> {
                    baselines.minHz = null
                    baselines.peakHz = null
                }
                ManagedResource.BRIGHTNESS -> {
                    baselines.brightness = null
                    baselines.hbmProbe = null
                }
                ManagedResource.MEDIA_VOLUME -> baselines.mediaVol = -1
                ManagedResource.WIFI -> baselines.wifiOn = null
                ManagedResource.MOBILE_DATA -> baselines.dataOn = null
                ManagedResource.AIRPLANE_MODE -> baselines.airplaneOn = null
                ManagedResource.BATTERY_SAVER -> baselines.lowPower = null
                ManagedResource.DARK_MODE -> baselines.nightMode = null
                ManagedResource.AUTO_ROTATE -> baselines.autoRotate = null
            }
        }
    }

    private fun appendReleasedBaselineReverts(
        sb: StringBuilder,
        released: Set<ManagedResource>,
        shouldRevertHbm: Boolean,
        snap: CapturedBaselines
    ) {
        if (ManagedResource.DND in released && HardwareDisplayController.isValidSetting(snap.dnd)) {
            sb.append("settings put global zen_mode ").append(snap.dnd!!.trim()).append('\n')
        }
        if (ManagedResource.REFRESH_RATE in released) {
            if (HardwareDisplayController.isValidSetting(snap.minHz)) {
                sb.append("settings put system min_refresh_rate ").append(snap.minHz!!.trim()).append('\n')
            } else {
                sb.append("settings delete system min_refresh_rate\n")
            }
            if (HardwareDisplayController.isValidSetting(snap.peakHz)) {
                sb.append("settings put system peak_refresh_rate ").append(snap.peakHz!!.trim()).append('\n')
            } else {
                sb.append("settings delete system peak_refresh_rate\n")
            }
        }
        if (ManagedResource.BRIGHTNESS in released || shouldRevertHbm) {
            HardwareDisplayController.appendRevertCommands(
                sb = sb,
                baseline = if (ManagedResource.BRIGHTNESS in released) snap.brightness else null,
                hbmProbe = if (shouldRevertHbm) snap.hbmProbe else null
            )
        }
        if (ManagedResource.WIFI in released && HardwareDisplayController.isValidSetting(snap.wifiOn)) {
            sb.append(if (snap.wifiOn!!.trim() == "1") "svc wifi enable\n" else "svc wifi disable\n")
        }
        if (ManagedResource.MOBILE_DATA in released && HardwareDisplayController.isValidSetting(snap.dataOn)) {
            sb.append(if (snap.dataOn!!.trim() == "1") "svc data enable\n" else "svc data disable\n")
        }
        if (ManagedResource.AIRPLANE_MODE in released && HardwareDisplayController.isValidSetting(snap.airplaneOn)) {
            sb.append(
                if (snap.airplaneOn!!.trim() == "1") "cmd connectivity airplane-mode enable\n"
                else "cmd connectivity airplane-mode disable\n"
            )
        }
        if (ManagedResource.BATTERY_SAVER in released && HardwareDisplayController.isValidSetting(snap.lowPower)) {
            sb.append("settings put global low_power ").append(snap.lowPower!!.trim()).append('\n')
        }
        if (ManagedResource.DARK_MODE in released && HardwareDisplayController.isValidSetting(snap.nightMode)) {
            sb.append(if (snap.nightMode!!.trim() == "2") "cmd uimode night yes\n" else "cmd uimode night no\n")
        }
        if (ManagedResource.AUTO_ROTATE in released && HardwareDisplayController.isValidSetting(snap.autoRotate)) {
            sb.append("settings put system accelerometer_rotation ").append(snap.autoRotate!!.trim()).append('\n')
        }
    }

    private fun resourcesClaimedBy(rule: AutomationRule): Set<ManagedResource> =
        rule.actionSteps.mapNotNull { it.type.toManagedResource() }.toSet()

    private fun ActionStepType.toManagedResource(): ManagedResource? = when (this) {
        ActionStepType.DND_MODE -> ManagedResource.DND
        ActionStepType.REFRESH_RATE -> ManagedResource.REFRESH_RATE
        ActionStepType.BRIGHTNESS -> ManagedResource.BRIGHTNESS
        ActionStepType.MEDIA_VOLUME -> ManagedResource.MEDIA_VOLUME
        ActionStepType.WIFI -> ManagedResource.WIFI
        ActionStepType.MOBILE_DATA -> ManagedResource.MOBILE_DATA
        ActionStepType.AIRPLANE_MODE -> ManagedResource.AIRPLANE_MODE
        ActionStepType.BATTERY_SAVER -> ManagedResource.BATTERY_SAVER
        ActionStepType.DARK_MODE -> ManagedResource.DARK_MODE
        ActionStepType.AUTO_ROTATE -> ManagedResource.AUTO_ROTATE
        ActionStepType.QS_TILE,
        ActionStepType.KILL_APPS,
        ActionStepType.LAUNCH_APP,
        ActionStepType.VIBRATE,
        ActionStepType.CUSTOM_SHELL,
        ActionStepType.DELAY -> null
    }

    private fun ManagedResource.toActionStepType(): ActionStepType = when (this) {
        ManagedResource.DND -> ActionStepType.DND_MODE
        ManagedResource.REFRESH_RATE -> ActionStepType.REFRESH_RATE
        ManagedResource.BRIGHTNESS -> ActionStepType.BRIGHTNESS
        ManagedResource.MEDIA_VOLUME -> ActionStepType.MEDIA_VOLUME
        ManagedResource.WIFI -> ActionStepType.WIFI
        ManagedResource.MOBILE_DATA -> ActionStepType.MOBILE_DATA
        ManagedResource.AIRPLANE_MODE -> ActionStepType.AIRPLANE_MODE
        ManagedResource.BATTERY_SAVER -> ActionStepType.BATTERY_SAVER
        ManagedResource.DARK_MODE -> ActionStepType.DARK_MODE
        ManagedResource.AUTO_ROTATE -> ActionStepType.AUTO_ROTATE
    }
}
