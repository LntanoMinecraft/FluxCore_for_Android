package com.mouajmd.fluxcore.shortcut

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.mouajmd.fluxcore.R
import com.mouajmd.fluxcore.data.AppLogger
import com.mouajmd.fluxcore.data.RuleStore
import com.mouajmd.fluxcore.engine.ActionExecutor
import com.mouajmd.fluxcore.system.HapticEngine
import com.mouajmd.fluxcore.system.ShortcutHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Translucent no-UI trampoline activity invoked by pinned Home Screen shortcuts.
 * Toggles the target automation profile immediately without opening the main UI.
 */
class ShortcutTriggerActivity : Activity() {

    companion object {
        private val shortcutScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appCtx = applicationContext
        val ruleId = intent?.getStringExtra(ShortcutHelper.EXTRA_RULE_ID)
        val rule = ruleId?.let { RuleStore.findById(appCtx, it) }

        if (rule == null) {
            Toast.makeText(appCtx, getString(R.string.shortcut_missing), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        HapticEngine.shortcutPulseIfEnabled(appCtx)
        AppLogger.log("SHORTCUT", "Home shortcut tapped -> [${rule.name}]")

        shortcutScope.launch {
            val nowActive = ActionExecutor.toggle(appCtx, rule)
            val msg = appCtx.getString(
                if (nowActive) R.string.shortcut_activated else R.string.shortcut_reverted,
                rule.name.ifBlank { appCtx.getString(R.string.app_name) }
            )
            withContext(Dispatchers.Main) {
                Toast.makeText(appCtx, msg, Toast.LENGTH_SHORT).show()
            }
        }

        finish()
    }
}
