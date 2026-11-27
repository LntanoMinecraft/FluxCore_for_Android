package com.mouajmd.fluxcore.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mouajmd.fluxcore.data.AppLogger
import com.mouajmd.fluxcore.system.RootShell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            val appCtx = context.applicationContext
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (RootShell.hasRoot()) {
                        AppLogger.log("BOOT", "Boot completed -> starting FluxService")
                        FluxService.start(appCtx)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
