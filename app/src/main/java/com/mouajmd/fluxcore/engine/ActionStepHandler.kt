package com.mouajmd.fluxcore.engine

import android.content.Context
import android.media.AudioManager
import com.mouajmd.fluxcore.data.AppLogger
import com.mouajmd.fluxcore.model.ActionStep
import com.mouajmd.fluxcore.model.ActionStepType
import com.mouajmd.fluxcore.system.HapticEngine
import com.mouajmd.fluxcore.system.HardwareDisplayController
import com.mouajmd.fluxcore.system.HbmProbeResult
import com.mouajmd.fluxcore.system.RootShell
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

/**
 * Strategy-style handler for individual automation pipeline steps.
 * Separates step execution and command synthesis from snapshot/lifecycle orchestration.
 */
internal object ActionStepHandler {

    private val safePackageRegex = Regex("^[a-zA-Z0-9._]+$")
    private val safeComponentRegex = Regex("^[a-zA-Z0-9._/$]+$")

    /**
     * Executes a single step. Returns [HbmProbeResult] if an HBM peak brightness override was activated.
     */
    fun executeStep(
        ctx: Context,
        ruleName: String,
        stepNumber: Int,
        step: ActionStep,
        cancelled: AtomicBoolean
    ): HbmProbeResult? {
        if (cancelled.get()) return null

        return when (step.type) {
            ActionStepType.DELAY -> {
                val totalMs = step.intValue.coerceIn(50, 30_000).toLong()
                AppLogger.log("STEP", "[$ruleName #$stepNumber] Delay ${totalMs}ms")
                sleepInterruptibly(totalMs, cancelled)
                null
            }
            ActionStepType.VIBRATE -> {
                val ms = step.intValue.coerceIn(20, 400).toLong()
                HapticEngine.vibrateRulePulse(ctx, ms)
                AppLogger.log("STEP", "[$ruleName #$stepNumber] Vibrate ${ms}ms")
                null
            }
            ActionStepType.MEDIA_VOLUME -> {
                applyMediaVolume(ctx, ruleName, stepNumber, step.intValue)
                null
            }
            ActionStepType.BRIGHTNESS -> {
                if (step.boolValue) {
                    HardwareDisplayController.activateHbmPeakBrightness(ruleName, stepNumber)
                } else {
                    HardwareDisplayController.applyPercentBrightness(ruleName, stepNumber, step.intValue)
                    null
                }
            }
            else -> {
                val cmd = buildShellCommand(ctx, step)
                if (cmd.isNotBlank()) {
                    val timeout = if (step.type == ActionStepType.CUSTOM_SHELL) {
                        RootShell.EXTENDED_TIMEOUT_MS
                    } else {
                        RootShell.DEFAULT_TIMEOUT_MS
                    }
                    val result = RootShell.execDetailed(cmd, timeoutMs = timeout)
                    when {
                        !result.isSuccess -> AppLogger.error(
                            "SHELL",
                            "[$ruleName #$stepNumber ${step.type.key}] exit=${result.exitCode}: ${result.output}"
                        )
                        result.output.isNotEmpty() -> AppLogger.log("SHELL", "[$ruleName #$stepNumber] ${result.output}")
                        else -> AppLogger.log("STEP", "[$ruleName #$stepNumber] ${step.type.key} OK")
                    }
                }
                null
            }
        }
    }

    fun applyMediaVolume(ctx: Context, ruleName: String, stepNumber: Int, percent: Int) {
        runCatching {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (am != null) {
                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val pct = percent.coerceIn(0, 100)
                val target = (max * (pct / 100f)).roundToInt().coerceIn(0, max)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                AppLogger.log("STEP", "[$ruleName #$stepNumber] Media volume -> $pct% ($target/$max)")
            }
        }.onFailure {
            AppLogger.error("STEP", "[$ruleName #$stepNumber] Media volume error: ${it.message}")
        }
    }

    fun buildShellCommand(ctx: Context, step: ActionStep): String = when (step.type) {
        ActionStepType.QS_TILE -> {
            val comp = step.stringValue.trim()
            if (comp.isNotEmpty() && safeComponentRegex.matches(comp)) {
                "cmd statusbar click-tile $comp"
            } else {
                ""
            }
        }
        ActionStepType.KILL_APPS -> {
            val selfPkg = ctx.packageName
            step.stringList
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && it != selfPkg && safePackageRegex.matches(it) }
                .distinct()
                .joinToString("\n") { "am force-stop $it" }
        }
        ActionStepType.DND_MODE -> when (step.intValue) {
            0 -> "cmd notification set_dnd off"
            1 -> "cmd notification set_dnd priority"
            else -> "cmd notification set_dnd total"
        }
        ActionStepType.REFRESH_RATE -> {
            val hz = step.intValue.coerceAtLeast(60)
            "settings put system min_refresh_rate $hz.0\nsettings put system peak_refresh_rate $hz.0"
        }
        ActionStepType.WIFI -> if (step.boolValue) "svc wifi enable" else "svc wifi disable"
        ActionStepType.MOBILE_DATA -> if (step.boolValue) "svc data enable" else "svc data disable"
        ActionStepType.AIRPLANE_MODE -> if (step.boolValue) {
            "cmd connectivity airplane-mode enable"
        } else {
            "cmd connectivity airplane-mode disable"
        }
        ActionStepType.BATTERY_SAVER -> "settings put global low_power ${if (step.boolValue) 1 else 0}"
        ActionStepType.DARK_MODE -> if (step.boolValue) "cmd uimode night yes" else "cmd uimode night no"
        ActionStepType.AUTO_ROTATE -> if (step.boolValue) {
            "settings put system accelerometer_rotation 1"
        } else {
            "settings put system accelerometer_rotation 0\nsettings put system user_rotation 0"
        }
        ActionStepType.CUSTOM_SHELL -> step.stringValue.trim()
        ActionStepType.BRIGHTNESS, ActionStepType.MEDIA_VOLUME, ActionStepType.VIBRATE, ActionStepType.DELAY -> ""
    }

    fun appendStepReverseActions(sb: StringBuilder, steps: List<ActionStep>) {
        for (step in steps.asReversed()) {
            when (step.type) {
                ActionStepType.QS_TILE -> {
                    val comp = step.stringValue.trim()
                    if (comp.isNotEmpty() && safeComponentRegex.matches(comp)) {
                        sb.append("cmd statusbar click-tile ").append(comp).append('\n')
                    }
                }
                ActionStepType.CUSTOM_SHELL -> {
                    val rev = step.secondaryString.trim()
                    if (rev.isNotEmpty()) {
                        sb.append(rev).append('\n')
                    }
                }
                else -> Unit
            }
        }
    }

    private fun sleepInterruptibly(totalMs: Long, cancelled: AtomicBoolean) {
        val deadline = System.currentTimeMillis() + totalMs
        while (!cancelled.get()) {
            val remaining = deadline - System.currentTimeMillis()
            if (remaining <= 0L) break
            try {
                Thread.sleep(minOf(remaining, 80L))
            } catch (_: InterruptedException) {
                break
            }
        }
    }
}
