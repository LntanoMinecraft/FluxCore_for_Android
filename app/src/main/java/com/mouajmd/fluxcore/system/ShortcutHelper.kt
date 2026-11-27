package com.mouajmd.fluxcore.system

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.mouajmd.fluxcore.R
import com.mouajmd.fluxcore.data.AppLogger
import com.mouajmd.fluxcore.model.AutomationRule
import com.mouajmd.fluxcore.shortcut.ShortcutTriggerActivity

object ShortcutHelper {
    const val EXTRA_RULE_ID = "com.mouajmd.fluxcore.EXTRA_RULE_ID"

    private fun shortcutId(ruleId: String) = "flux_rule_$ruleId"

    fun requestPinRuleShortcut(context: Context, rule: AutomationRule, customLabel: String? = null): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            return false
        }
        val label = (customLabel?.trim().takeUnless { it.isNullOrEmpty() }
            ?: rule.name.ifBlank { context.getString(R.string.app_name) }).take(24)

        val info = buildShortcutInfo(context, rule.id, label)
        val ok = ShortcutManagerCompat.requestPinShortcut(context, info, null)
        if (ok) {
            AppLogger.log("SHORTCUT", "Requested home shortcut for [$label]")
        }
        return ok
    }

    fun syncRenamedShortcut(context: Context, rule: AutomationRule) {
        runCatching {
            val label = rule.name.ifBlank { context.getString(R.string.app_name) }.take(24)
            val info = buildShortcutInfo(context, rule.id, label)
            ShortcutManagerCompat.updateShortcuts(context, listOf(info))
        }
    }

    fun disableDeletedShortcut(context: Context, ruleId: String) {
        runCatching {
            ShortcutManagerCompat.disableShortcuts(
                context,
                listOf(shortcutId(ruleId)),
                context.getString(R.string.shortcut_missing)
            )
            AppLogger.log("SHORTCUT", "Disabled shortcut for deleted rule $ruleId")
        }
    }

    private fun buildShortcutInfo(context: Context, ruleId: String, label: String): ShortcutInfoCompat {
        val intent = Intent(context, ShortcutTriggerActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_RULE_ID, ruleId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return ShortcutInfoCompat.Builder(context, shortcutId(ruleId))
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(intent)
            .build()
    }
}
