package com.mouajmd.fluxcore.engine

import com.mouajmd.fluxcore.model.AutomationRule
import com.mouajmd.fluxcore.model.TriggerType
import java.util.Calendar

/**
 * Immutable snapshot of current device runtime signals used for deterministic rule evaluation.
 */
data class SystemEnvironmentState(
    val currentFgPkg: String = "",
    val isCharging: Boolean = false,
    val batteryLevel: Int = 50,
    val isScreenOn: Boolean = true,
    val isWifiConnected: Boolean = false,
    val currentWifiSsid: String = "",
    val isBtConnected: Boolean = false,
    val connectedBtDump: String = "",
    val isHeadsetPlugged: Boolean = false,
    val isLandscape: Boolean = false
)

/**
 * Stateless rule & compound AND-condition evaluator.
 */
object RuleMatcher {

    fun isRuleMatched(
        rule: AutomationRule,
        env: SystemEnvironmentState,
        nowMinutesOfDay: Int = currentMinutesOfDay()
    ): Boolean {
        if (!checkConditionMatch(
                type = rule.triggerType,
                expectedOn = rule.triggerOn,
                pkgs = rule.triggerPkgs,
                targetName = rule.triggerTarget,
                threshold = rule.triggerThreshold,
                env = env,
                nowMinutesOfDay = nowMinutesOfDay
            )
        ) {
            return false
        }
        for (cond in rule.extraConditions) {
            if (!checkConditionMatch(
                    type = cond.type,
                    expectedOn = cond.stateOn,
                    pkgs = cond.pkgs,
                    targetName = cond.targetName,
                    threshold = cond.threshold,
                    env = env,
                    nowMinutesOfDay = nowMinutesOfDay
                )
            ) {
                return false
            }
        }
        return true
    }

    fun checkConditionMatch(
        type: TriggerType,
        expectedOn: Boolean,
        pkgs: List<String>,
        targetName: String,
        threshold: Int,
        env: SystemEnvironmentState,
        nowMinutesOfDay: Int = currentMinutesOfDay()
    ): Boolean = when (type) {
        TriggerType.APP -> {
            if (pkgs.isEmpty() || env.currentFgPkg.isEmpty()) false
            else {
                val inList = env.currentFgPkg in pkgs
                if (expectedOn) inList else !inList
            }
        }
        TriggerType.CHARGING -> env.isCharging == expectedOn
        TriggerType.SCREEN -> env.isScreenOn == expectedOn
        TriggerType.WIFI -> {
            val kw = targetName.trim()
            if (kw.isEmpty()) env.isWifiConnected == expectedOn
            else {
                val matched = env.isWifiConnected && env.currentWifiSsid.equals(kw, ignoreCase = true)
                if (expectedOn) matched else !matched
            }
        }
        TriggerType.BLUETOOTH -> {
            val kw = targetName.trim().lowercase()
            if (kw.isEmpty()) env.isBtConnected == expectedOn
            else {
                val matched = env.isBtConnected && env.connectedBtDump.lowercase().contains(kw)
                if (expectedOn) matched else !matched
            }
        }
        TriggerType.BATTERY -> {
            val safeThreshold = threshold.coerceIn(1, 100)
            if (expectedOn) env.batteryLevel >= safeThreshold else env.batteryLevel <= safeThreshold
        }
        TriggerType.HEADSET -> env.isHeadsetPlugged == expectedOn
        TriggerType.ORIENTATION -> env.isLandscape == expectedOn
        TriggerType.TIME -> isInTimeWindow(targetName, nowMinutesOfDay)
        TriggerType.MANUAL -> false
    }

    fun isInTimeWindow(spec: String?, nowMinutesOfDay: Int = currentMinutesOfDay()): Boolean {
        val raw = spec?.trim().takeUnless { it.isNullOrEmpty() } ?: "23:00-07:00"
        return runCatching {
            val parts = raw.split("-")
            if (parts.size != 2) return false
            val p1 = parts[0].trim().split(":")
            val p2 = parts[1].trim().split(":")
            if (p1.size != 2 || p2.size != 2) return false
            val h1 = p1[0].toInt().coerceIn(0, 23)
            val m1 = p1[1].toInt().coerceIn(0, 59)
            val h2 = p2[0].toInt().coerceIn(0, 23)
            val m2 = p2[1].toInt().coerceIn(0, 59)
            val startMin = h1 * 60 + m1
            val endMin = h2 * 60 + m2
            if (startMin <= endMin) {
                nowMinutesOfDay in startMin until endMin
            } else {
                nowMinutesOfDay >= startMin || nowMinutesOfDay < endMin
            }
        }.getOrDefault(false)
    }

    private fun currentMinutesOfDay(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }
}
