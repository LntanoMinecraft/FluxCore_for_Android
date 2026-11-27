package com.mouajmd.fluxcore.model

import android.content.Context
import androidx.compose.runtime.Immutable
import com.mouajmd.fluxcore.R
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.roundToInt

enum class TriggerType(val code: Int) {
    MANUAL(0),
    APP(1),
    CHARGING(2),
    SCREEN(3),
    WIFI(4),
    BLUETOOTH(5),
    BATTERY(6),
    HEADSET(7),
    ORIENTATION(8),
    TIME(9);

    companion object {
        fun fromCode(code: Int): TriggerType =
            entries.firstOrNull { it.code == code } ?: MANUAL
    }
}

enum class ActionStepType(val key: String) {
    QS_TILE("qs_tile"),
    KILL_APPS("kill_apps"),
    DND_MODE("dnd_mode"),
    REFRESH_RATE("refresh_rate"),
    BRIGHTNESS("brightness"),
    MEDIA_VOLUME("media_volume"),
    WIFI("wifi"),
    MOBILE_DATA("mobile_data"),
    AIRPLANE_MODE("airplane_mode"),
    BATTERY_SAVER("battery_saver"),
    DARK_MODE("dark_mode"),
    AUTO_ROTATE("auto_rotate"),
    VIBRATE("vibrate"),
    CUSTOM_SHELL("custom_shell"),
    DELAY("delay");

    companion object {
        fun fromKey(key: String): ActionStepType? =
            entries.firstOrNull { it.key == key }
    }
}

internal fun describeTriggerCondition(
    ctx: Context,
    type: TriggerType,
    stateOn: Boolean,
    pkgs: List<String>,
    targetName: String,
    threshold: Int,
    singlePkgDetail: Boolean = false
): String {
    val dirOn = ctx.getString(R.string.trigger_state_on)
    val dirOff = ctx.getString(R.string.trigger_state_off)
    return when (type) {
        TriggerType.MANUAL -> ctx.getString(R.string.trigger_manual)
        TriggerType.APP -> {
            if (singlePkgDetail && pkgs.size == 1) {
                val verb = if (stateOn) ctx.getString(R.string.trigger_app_enter) else ctx.getString(R.string.trigger_app_exit)
                "$verb · ${pkgs[0]}"
            } else {
                val verb = if (stateOn) ctx.getString(R.string.trigger_app_enter_multi) else ctx.getString(R.string.trigger_app_exit_multi)
                "$verb (${pkgs.size})"
            }
        }
        TriggerType.CHARGING -> if (stateOn) ctx.getString(R.string.trigger_charging_on) else ctx.getString(R.string.trigger_charging_off)
        TriggerType.SCREEN -> if (stateOn) ctx.getString(R.string.trigger_screen_on) else ctx.getString(R.string.trigger_screen_off)
        TriggerType.WIFI -> {
            val ssid = targetName.trim()
            if (ssid.isNotEmpty()) "Wi-Fi ${if (stateOn) "✓" else "✕"} $ssid"
            else "Wi-Fi ${if (stateOn) dirOn else dirOff}"
        }
        TriggerType.BLUETOOTH -> {
            val bt = targetName.trim()
            val label = ctx.getString(R.string.trigger_bt)
            if (bt.isNotEmpty()) "$label ${if (stateOn) "✓" else "✕"} $bt"
            else "$label ${if (stateOn) dirOn else dirOff}"
        }
        TriggerType.BATTERY -> if (stateOn) {
            ctx.getString(R.string.trigger_battery_above, threshold.coerceIn(1, 100))
        } else {
            ctx.getString(R.string.trigger_battery_below, threshold.coerceIn(1, 100))
        }
        TriggerType.HEADSET -> if (stateOn) ctx.getString(R.string.trigger_headset_on) else ctx.getString(R.string.trigger_headset_off)
        TriggerType.ORIENTATION -> if (stateOn) ctx.getString(R.string.trigger_orient_land) else ctx.getString(R.string.trigger_orient_port)
        TriggerType.TIME -> "${ctx.getString(R.string.trigger_time)} ${targetName.trim().ifBlank { "23:00-07:00" }}"
    }
}

private fun JSONObject.optStringList(key: String): List<String> {
    val arr = optJSONArray(key) ?: return emptyList()
    return buildList(arr.length()) {
        for (i in 0 until arr.length()) {
            val v = arr.optString(i, "").trim()
            if (v.isNotEmpty()) add(v)
        }
    }
}

@Immutable
data class RuleCondition(
    val type: TriggerType = TriggerType.CHARGING,
    val stateOn: Boolean = true,
    val pkgs: List<String> = emptyList(),
    val targetName: String = "",
    val threshold: Int = 20
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("type", type.code)
        put("stateOn", stateOn)
        put("targetName", targetName)
        put("threshold", threshold.coerceIn(1, 100))
        put("pkgs", JSONArray().apply { pkgs.forEach { put(it) } })
    }

    fun describe(ctx: Context): String =
        describeTriggerCondition(ctx, type, stateOn, pkgs, targetName, threshold, singlePkgDetail = false)

    companion object {
        fun fromJson(o: JSONObject): RuleCondition = RuleCondition(
            type = TriggerType.fromCode(o.optInt("type", TriggerType.CHARGING.code)),
            stateOn = o.optBoolean("stateOn", true),
            pkgs = o.optStringList("pkgs"),
            targetName = o.optString("targetName", ""),
            threshold = o.optInt("threshold", 20).coerceIn(1, 100)
        )
    }
}

@Immutable
data class ActionStep(
    val id: String = UUID.randomUUID().toString(),
    val type: ActionStepType,
    val intValue: Int = 0,
    val boolValue: Boolean = true,
    val stringValue: String = "",
    val secondaryString: String = "",
    val stringList: List<String> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type.key)
        put("intValue", intValue)
        put("boolValue", boolValue)
        put("stringValue", stringValue)
        put("secondaryString", secondaryString)
        put("stringList", JSONArray().apply { stringList.forEach { put(it) } })
    }

    fun summary(ctx: Context): String = when (type) {
        ActionStepType.QS_TILE -> {
            val label = secondaryString.ifBlank { stringValue.ifBlank { ctx.getString(R.string.pick_tile_btn) } }
            "${ctx.getString(R.string.step_qs_tile)} · $label"
        }
        ActionStepType.KILL_APPS -> "${ctx.getString(R.string.step_kill_apps)} (${stringList.size})"
        ActionStepType.DND_MODE -> {
            val mode = when (intValue) {
                0 -> ctx.getString(R.string.dnd_off)
                1 -> ctx.getString(R.string.dnd_priority)
                else -> ctx.getString(R.string.dnd_total)
            }
            "${ctx.getString(R.string.step_dnd)} · $mode"
        }
        ActionStepType.REFRESH_RATE -> "${ctx.getString(R.string.step_refresh_rate)} · ${intValue.coerceAtLeast(60)}Hz"
        ActionStepType.BRIGHTNESS -> if (boolValue) {
            "${ctx.getString(R.string.step_brightness)} · ${ctx.getString(R.string.brightness_hbm_badge)}"
        } else {
            "${ctx.getString(R.string.step_brightness)} · ${intValue.coerceIn(0, 100)}%"
        }
        ActionStepType.MEDIA_VOLUME -> "${ctx.getString(R.string.step_media_volume)} · ${intValue.coerceIn(0, 100)}%"
        ActionStepType.WIFI -> "${ctx.getString(R.string.step_wifi)} · ${if (boolValue) ctx.getString(R.string.state_enable) else ctx.getString(R.string.state_disable)}"
        ActionStepType.MOBILE_DATA -> "${ctx.getString(R.string.step_mobile_data)} · ${if (boolValue) ctx.getString(R.string.state_enable) else ctx.getString(R.string.state_disable)}"
        ActionStepType.AIRPLANE_MODE -> "${ctx.getString(R.string.step_airplane)} · ${if (boolValue) ctx.getString(R.string.state_enable) else ctx.getString(R.string.state_disable)}"
        ActionStepType.BATTERY_SAVER -> "${ctx.getString(R.string.step_battery_saver)} · ${if (boolValue) ctx.getString(R.string.state_enable) else ctx.getString(R.string.state_disable)}"
        ActionStepType.DARK_MODE -> "${ctx.getString(R.string.step_dark_mode)} · ${if (boolValue) ctx.getString(R.string.dark_on) else ctx.getString(R.string.dark_off)}"
        ActionStepType.AUTO_ROTATE -> "${ctx.getString(R.string.step_auto_rotate)} · ${if (boolValue) ctx.getString(R.string.rotate_on) else ctx.getString(R.string.rotate_off)}"
        ActionStepType.VIBRATE -> "${ctx.getString(R.string.step_vibrate)} · ${intValue.coerceIn(20, 400)}ms"
        ActionStepType.CUSTOM_SHELL -> {
            val cmdPreview = stringValue.lineSequence().firstOrNull()?.take(36)?.ifBlank { null }
            cmdPreview ?: ctx.getString(R.string.step_custom_shell)
        }
        ActionStepType.DELAY -> "${ctx.getString(R.string.step_delay)} · ${intValue.coerceIn(50, 30_000)}ms"
    }

    companion object {
        const val BRIGHTNESS_UNIT_PCT = "pct"

        fun legacy255ToPercent(value255: Int): Int =
            ((value255.coerceIn(0, 255) * 100f) / 255f).roundToInt().coerceIn(0, 100)

        fun percentToLegacy255(percent: Int): Int =
            ((percent.coerceIn(0, 100) * 255f) / 100f).roundToInt().coerceIn(0, 255)

        fun percentToFloat(percent: Int): Float =
            (percent.coerceIn(0, 100) / 100f).coerceIn(0f, 1f)

        fun defaultFor(type: ActionStepType): ActionStep = when (type) {
            ActionStepType.QS_TILE -> ActionStep(type = type)
            ActionStepType.KILL_APPS -> ActionStep(type = type)
            ActionStepType.DND_MODE -> ActionStep(type = type, intValue = 1)
            ActionStepType.REFRESH_RATE -> ActionStep(type = type, intValue = 120)
            ActionStepType.BRIGHTNESS -> ActionStep(
                type = type,
                intValue = 70,
                boolValue = false,
                stringValue = BRIGHTNESS_UNIT_PCT
            )
            ActionStepType.MEDIA_VOLUME -> ActionStep(type = type, intValue = 50)
            ActionStepType.WIFI -> ActionStep(type = type, boolValue = true)
            ActionStepType.MOBILE_DATA -> ActionStep(type = type, boolValue = true)
            ActionStepType.AIRPLANE_MODE -> ActionStep(type = type, boolValue = false)
            ActionStepType.BATTERY_SAVER -> ActionStep(type = type, boolValue = true)
            ActionStepType.DARK_MODE -> ActionStep(type = type, boolValue = true)
            ActionStepType.AUTO_ROTATE -> ActionStep(type = type, boolValue = false)
            ActionStepType.VIBRATE -> ActionStep(type = type, intValue = 55)
            ActionStepType.CUSTOM_SHELL -> ActionStep(type = type)
            ActionStepType.DELAY -> ActionStep(type = type, intValue = 500)
        }

        fun fromJson(o: JSONObject): ActionStep? {
            val type = ActionStepType.fromKey(o.optString("type", "")) ?: return null
            val stepId = o.optString("id", "").ifBlank { UUID.randomUUID().toString() }
            val list = o.optStringList("stringList")
            if (type == ActionStepType.BRIGHTNESS) {
                val rawStr = o.optString("stringValue", "")
                val rawInt = o.optInt("intValue", 128)
                val isModernPct = rawStr == BRIGHTNESS_UNIT_PCT
                val pct = if (isModernPct) rawInt.coerceIn(0, 100) else legacy255ToPercent(rawInt)
                val hbmEnabled = if (isModernPct) o.optBoolean("boolValue", false) else false
                return ActionStep(
                    id = stepId,
                    type = type,
                    intValue = pct,
                    boolValue = hbmEnabled,
                    stringValue = BRIGHTNESS_UNIT_PCT,
                    secondaryString = o.optString("secondaryString", ""),
                    stringList = list
                )
            }
            val rawInt = o.optInt("intValue", 0)
            val normalizedInt = when (type) {
                ActionStepType.MEDIA_VOLUME -> rawInt.coerceIn(0, 100)
                ActionStepType.VIBRATE -> rawInt.coerceIn(20, 400)
                ActionStepType.DELAY -> rawInt.coerceIn(50, 30_000)
                ActionStepType.REFRESH_RATE -> rawInt.coerceAtLeast(60)
                else -> rawInt
            }
            return ActionStep(
                id = stepId,
                type = type,
                intValue = normalizedInt,
                boolValue = o.optBoolean("boolValue", true),
                stringValue = o.optString("stringValue", ""),
                secondaryString = o.optString("secondaryString", ""),
                stringList = list
            )
        }
    }
}

@Immutable
data class AutomationRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val enabled: Boolean = true,
    val triggerType: TriggerType = TriggerType.MANUAL,
    val triggerPkgs: List<String> = emptyList(),
    val triggerTarget: String = "",
    val triggerThreshold: Int = 20,
    val triggerOn: Boolean = true,
    val extraConditions: List<RuleCondition> = emptyList(),
    val actionSteps: List<ActionStep> = emptyList(),
    val revertCmd: String = "",
    val autoRevert: Boolean = true
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("enabled", enabled)
        put("triggerType", triggerType.code)
        put("triggerTarget", triggerTarget)
        put("triggerThreshold", triggerThreshold.coerceIn(1, 100))
        put("triggerOn", triggerOn)
        put("triggerPkgs", JSONArray().apply { triggerPkgs.forEach { put(it) } })
        put("extraConditions", JSONArray().apply { extraConditions.forEach { put(it.toJson()) } })
        put("actionSteps", JSONArray().apply { actionSteps.forEach { put(it.toJson()) } })
        put("revertCmd", revertCmd)
        put("autoRevert", autoRevert)

        // Mirror legacy v1 fields for clean backward interoperability
        val firstTile = actionSteps.firstOrNull { it.type == ActionStepType.QS_TILE }
        put("tileComponent", firstTile?.stringValue ?: "")
        put("tileLabel", firstTile?.secondaryString ?: "")
        val killAll = actionSteps.filter { it.type == ActionStepType.KILL_APPS }.flatMap { it.stringList }.distinct()
        put("killPkgs", JSONArray().apply { killAll.forEach { put(it) } })
        put("dndMode", actionSteps.lastOrNull { it.type == ActionStepType.DND_MODE }?.intValue ?: -1)
        put("refreshRate", actionSteps.lastOrNull { it.type == ActionStepType.REFRESH_RATE }?.intValue ?: 0)
        val brightMirror = actionSteps.lastOrNull { it.type == ActionStepType.BRIGHTNESS }?.let { step ->
            if (step.boolValue) 255 else ActionStep.percentToLegacy255(step.intValue)
        } ?: -1
        put("brightness", brightMirror)
        put("mediaVolume", actionSteps.lastOrNull { it.type == ActionStepType.MEDIA_VOLUME }?.intValue ?: -1)
        put("wifiAction", actionSteps.lastOrNull { it.type == ActionStepType.WIFI }?.let { if (it.boolValue) 1 else 0 } ?: -1)
        put("mobileDataAction", actionSteps.lastOrNull { it.type == ActionStepType.MOBILE_DATA }?.let { if (it.boolValue) 1 else 0 } ?: -1)
        put("airplaneAction", actionSteps.lastOrNull { it.type == ActionStepType.AIRPLANE_MODE }?.let { if (it.boolValue) 1 else 0 } ?: -1)
        put("batterySaverAction", actionSteps.lastOrNull { it.type == ActionStepType.BATTERY_SAVER }?.let { if (it.boolValue) 1 else 0 } ?: -1)
        put("darkModeAction", actionSteps.lastOrNull { it.type == ActionStepType.DARK_MODE }?.let { if (it.boolValue) 1 else 0 } ?: -1)
        put("autoRotateAction", actionSteps.lastOrNull { it.type == ActionStepType.AUTO_ROTATE }?.let { if (it.boolValue) 1 else 0 } ?: -1)
        put("vibrateTrigger", actionSteps.any { it.type == ActionStepType.VIBRATE })
        put("customCmd", actionSteps.filter { it.type == ActionStepType.CUSTOM_SHELL }.joinToString("\n") { it.stringValue })
    }

    fun describeTrigger(ctx: Context): String {
        val base = describeTriggerCondition(
            ctx = ctx,
            type = triggerType,
            stateOn = triggerOn,
            pkgs = triggerPkgs,
            targetName = triggerTarget,
            threshold = triggerThreshold,
            singlePkgDetail = true
        )
        if (extraConditions.isEmpty()) return base
        return buildString {
            append(base)
            extraConditions.forEach {
                append(" + ")
                append(it.describe(ctx))
            }
        }
    }

    fun describeActions(ctx: Context): String {
        if (actionSteps.isEmpty()) return ctx.getString(R.string.rule_no_actions)
        return actionSteps.joinToString(" → ") { it.summary(ctx) }
    }

    companion object {
        fun fromJson(o: JSONObject): AutomationRule {
            val triggerPkgs = o.optStringList("triggerPkgs").toMutableList()
            val legacyPkg = o.optString("triggerPkg", "").trim()
            if (legacyPkg.isNotEmpty() && legacyPkg !in triggerPkgs) {
                triggerPkgs.add(legacyPkg)
            }

            val extras = mutableListOf<RuleCondition>()
            val cArr = o.optJSONArray("extraConditions")
            if (cArr != null) {
                for (i in 0 until cArr.length()) {
                    cArr.optJSONObject(i)?.let { extras.add(RuleCondition.fromJson(it)) }
                }
            }

            val steps = mutableListOf<ActionStep>()
            val stepsArr = o.optJSONArray("actionSteps")
            if (stepsArr != null) {
                for (i in 0 until stepsArr.length()) {
                    stepsArr.optJSONObject(i)?.let { jo ->
                        ActionStep.fromJson(jo)?.let { steps.add(it) }
                    }
                }
            } else {
                migrateLegacyV1Steps(o, steps)
            }

            return AutomationRule(
                id = o.optString("id", "").ifBlank { UUID.randomUUID().toString() },
                name = o.optString("name", ""),
                enabled = o.optBoolean("enabled", true),
                triggerType = TriggerType.fromCode(o.optInt("triggerType", TriggerType.MANUAL.code)),
                triggerPkgs = triggerPkgs,
                triggerTarget = o.optString("triggerTarget", ""),
                triggerThreshold = o.optInt("triggerThreshold", 20).coerceIn(1, 100),
                triggerOn = o.optBoolean("triggerOn", true),
                extraConditions = extras,
                actionSteps = steps,
                revertCmd = o.optString("revertCmd", ""),
                autoRevert = o.optBoolean("autoRevert", true)
            )
        }

        private fun migrateLegacyV1Steps(o: JSONObject, out: MutableList<ActionStep>) {
            if (o.optBoolean("vibrateTrigger", false)) {
                out.add(ActionStep(type = ActionStepType.VIBRATE, intValue = 55))
            }
            val tileComp = o.optString("tileComponent", "").trim()
            if (tileComp.isNotEmpty()) {
                out.add(
                    ActionStep(
                        type = ActionStepType.QS_TILE,
                        stringValue = tileComp,
                        secondaryString = o.optString("tileLabel", "")
                    )
                )
            }
            val killList = o.optStringList("killPkgs")
            if (killList.isNotEmpty()) {
                out.add(ActionStep(type = ActionStepType.KILL_APPS, stringList = killList))
            }
            val dnd = o.optInt("dndMode", -1)
            if (dnd >= 0) out.add(ActionStep(type = ActionStepType.DND_MODE, intValue = dnd))
            val hz = o.optInt("refreshRate", 0)
            if (hz > 0) out.add(ActionStep(type = ActionStepType.REFRESH_RATE, intValue = hz.coerceAtLeast(60)))
            val bright = o.optInt("brightness", -1)
            if (bright >= 0) {
                out.add(
                    ActionStep(
                        type = ActionStepType.BRIGHTNESS,
                        intValue = ActionStep.legacy255ToPercent(bright),
                        boolValue = false,
                        stringValue = ActionStep.BRIGHTNESS_UNIT_PCT
                    )
                )
            }
            val vol = o.optInt("mediaVolume", -1)
            if (vol >= 0) out.add(ActionStep(type = ActionStepType.MEDIA_VOLUME, intValue = vol.coerceIn(0, 100)))
            val wifi = o.optInt("wifiAction", -1)
            if (wifi >= 0) out.add(ActionStep(type = ActionStepType.WIFI, boolValue = wifi == 1))
            val data = o.optInt("mobileDataAction", -1)
            if (data >= 0) out.add(ActionStep(type = ActionStepType.MOBILE_DATA, boolValue = data == 1))
            val air = o.optInt("airplaneAction", -1)
            if (air >= 0) out.add(ActionStep(type = ActionStepType.AIRPLANE_MODE, boolValue = air == 1))
            val saver = o.optInt("batterySaverAction", -1)
            if (saver >= 0) out.add(ActionStep(type = ActionStepType.BATTERY_SAVER, boolValue = saver == 1))
            val dark = o.optInt("darkModeAction", -1)
            if (dark >= 0) out.add(ActionStep(type = ActionStepType.DARK_MODE, boolValue = dark == 1))
            val rot = o.optInt("autoRotateAction", -1)
            if (rot >= 0) out.add(ActionStep(type = ActionStepType.AUTO_ROTATE, boolValue = rot == 1))
            val custom = o.optString("customCmd", "").trim()
            if (custom.isNotEmpty()) {
                out.add(ActionStep(type = ActionStepType.CUSTOM_SHELL, stringValue = custom))
            }
        }
    }
}

enum class ScriptCategory { STORAGE, ADB }

@Immutable
data class ShellScriptItem(
    val path: String,
    val fileName: String,
    val parentDir: String,
    val category: ScriptCategory,
    val sizeBytes: Long,
    val lastModifiedMs: Long
)

@Immutable
data class QuickCommandPreset(
    val label: String,
    val command: String
) {
    companion object {
        val DEFAULTS = listOf(
            QuickCommandPreset("Battery", "dumpsys battery"),
            QuickCommandPreset("Display Size", "wm size"),
            QuickCommandPreset("Refresh Rate", "settings get system peak_refresh_rate"),
            QuickCommandPreset("Status Bar", "cmd statusbar help | head -n 20"),
            QuickCommandPreset("Uptime", "uptime")
        )
    }
}
