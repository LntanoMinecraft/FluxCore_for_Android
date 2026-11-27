package com.mouajmd.fluxcore.system

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import com.mouajmd.fluxcore.data.SettingsStore

object HapticEngine {

    private fun canVibrateUi(): Boolean = SettingsStore.isHapticEnabled

    private fun performUiHaptic(view: View?, constant: Int) {
        if (!canVibrateUi() || view == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            view.performHapticFeedback(constant)
        } else {
            @Suppress("DEPRECATION")
            view.performHapticFeedback(constant, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
        }
    }

    fun click(view: View?) {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CLOCK_TICK
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
        performUiHaptic(view, constant)
    }

    fun tabSwitch(view: View?) {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.SEGMENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        performUiHaptic(view, constant)
    }

    fun toggle(view: View?, enabled: Boolean) {
        val constant = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                if (enabled) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> HapticFeedbackConstants.CONFIRM
            else -> HapticFeedbackConstants.VIRTUAL_KEY
        }
        performUiHaptic(view, constant)
    }

    fun dragStart(view: View?) {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.GESTURE_START
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        performUiHaptic(view, constant)
    }

    fun dragTick(view: View?) {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        performUiHaptic(view, constant)
    }

    fun sliderTick(view: View?, isEndpoint: Boolean = false) {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (isEndpoint) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        performUiHaptic(view, constant)
    }

    fun confirm(view: View?) {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
        performUiHaptic(view, constant)
    }

    fun reject(view: View?) {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        performUiHaptic(view, constant)
    }

    fun shortcutPulseIfEnabled(context: Context) {
        if (!canVibrateUi()) return
        vibratePulse(context, 28L)
    }

    /**
     * Independent vibration pulse explicitly configured inside an automation rule.
     * Runs regardless of the UI Haptic Feedback Engine toggle.
     */
    fun vibrateRulePulse(context: Context, durationMs: Long = 55L) {
        vibratePulse(context, durationMs.coerceIn(20L, 400L))
    }

    private fun vibratePulse(context: Context, durationMs: Long) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.vibrate(
                VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        }
    }
}
