package com.mouajmd.fluxcore;

import android.content.Context;
import android.content.Intent;

public class Engine {
    public static final String ACTION_STATE_CHANGED = "com.mouajmd.fluxcore.STATE_CHANGED";

    public static void apply(final Context ctx, final Rule r, final Runnable onDone) {
        StringBuilder cmd = new StringBuilder();
        for (String pkg : r.killPkgs) {
            if (pkg != null && !pkg.isEmpty() && !pkg.equals(ctx.getPackageName())) {
                cmd.append("am force-stop ").append(pkg).append(";\n");
            }
        }
        if (r.dndMode == 0) cmd.append("cmd notification set_dnd off;\n");
        else if (r.dndMode == 1) cmd.append("cmd notification set_dnd priority;\n");
        else if (r.dndMode == 2) cmd.append("cmd notification set_dnd total;\n");

        if (r.brightness >= 0) {
            cmd.append("settings put system screen_brightness_mode 0;\n");
            cmd.append("settings put system screen_brightness ").append(r.brightness).append(";\n");
        }
        if (r.refreshRate > 0) {
            cmd.append("settings put system min_refresh_rate ").append(r.refreshRate).append(";\n");
            cmd.append("settings put system peak_refresh_rate ").append(r.refreshRate).append(";\n");
        }
        if (r.wifiAction == 1) cmd.append("svc wifi enable;\n");
        else if (r.wifiAction == 0) cmd.append("svc wifi disable;\n");

        if (r.customCmd != null && !r.customCmd.trim().isEmpty()) {
            cmd.append(r.customCmd.trim()).append(";\n");
        }

        if (r.hasStatefulAction()) {
            r.active = true;
            RuleStore.upsert(ctx, r);
        }

        final String finalCmd = cmd.toString();
        if (finalCmd.isEmpty()) {
            notifyChanged(ctx);
            if (onDone != null) onDone.run();
            return;
        }
        RootShell.execAsync(finalCmd, new RootShell.Callback() {
            @Override
            public void onResult(boolean success, String output) {
                notifyChanged(ctx);
                if (onDone != null) onDone.run();
            }
        });
    }

    public static void revert(final Context ctx, final Rule r, final Runnable onDone) {
        StringBuilder cmd = new StringBuilder();
        if (r.dndMode > 0) {
            cmd.append("cmd notification set_dnd off;\n");
        }
        if (r.brightness >= 0) {
            cmd.append("settings put system screen_brightness_mode 1;\n");
        }
        if (r.refreshRate > 0) {
            cmd.append("settings delete system min_refresh_rate;\n");
            cmd.append("settings delete system peak_refresh_rate;\n");
        }
        if (r.wifiAction == 0) cmd.append("svc wifi enable;\n");
        else if (r.wifiAction == 1) cmd.append("svc wifi disable;\n");

        if (r.revertCmd != null && !r.revertCmd.trim().isEmpty()) {
            cmd.append(r.revertCmd.trim()).append(";\n");
        }

        r.active = false;
        RuleStore.upsert(ctx, r);

        final String finalCmd = cmd.toString();
        if (finalCmd.isEmpty()) {
            notifyChanged(ctx);
            if (onDone != null) onDone.run();
            return;
        }
        RootShell.execAsync(finalCmd, new RootShell.Callback() {
            @Override
            public void onResult(boolean success, String output) {
                notifyChanged(ctx);
                if (onDone != null) onDone.run();
            }
        });
    }

    private static void notifyChanged(Context ctx) {
        Intent i = new Intent(ACTION_STATE_CHANGED);
        i.setPackage(ctx.getPackageName());
        ctx.sendBroadcast(i);
    }
}
