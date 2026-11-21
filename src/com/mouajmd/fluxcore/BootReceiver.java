package com.mouajmd.fluxcore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String act = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(act) || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(act)) {
            if (RootShell.hasRoot()) {
                AppLogger.log("BOOT", "检测到开机完成广播，自动拉起 FluxService");
                Intent svc = new Intent(context, FluxService.class);
                if (Build.VERSION.SDK_INT >= 26) {
                    context.startForegroundService(svc);
                } else {
                    context.startService(svc);
                }
            }
        }
    }
}
