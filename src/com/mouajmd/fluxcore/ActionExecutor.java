package com.mouajmd.fluxcore;

import android.content.Context;
import android.media.AudioManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ActionExecutor {
    private static class Snapshot {
        String dnd;
        String minHz;
        String peakHz;
        String brightMode;
        String brightVal;
        String wifiOn;
        String dataOn;
        String airplaneOn;
        String lowPower;
        String nightMode;
        String autoRotate;
        int mediaVol = -1;
    }

    private static final Map<String, Snapshot> SNAPSHOTS = new HashMap<>();
    private static final Set<String> ACTIVE_RULES = new HashSet<>();

    public static synchronized boolean isActive(String ruleId) {
        return ACTIVE_RULES.contains(ruleId);
    }

    public static synchronized void activate(Context ctx, Rule r) {
        if (ACTIVE_RULES.contains(r.id)) return;
        ACTIVE_RULES.add(r.id);
        AppLogger.log("EXEC", "触发情境 [" + r.name + "] -> " + r.describeActions());

        if (r.vibrateTrigger) {
            Md3e.vibratePulse(ctx, 55);
        }

        Snapshot snap = new Snapshot();
        if (r.dndMode >= 0) snap.dnd = RootShell.exec("settings get global zen_mode");
        if (r.refreshRate > 0) {
            snap.minHz = RootShell.exec("settings get system min_refresh_rate");
            snap.peakHz = RootShell.exec("settings get system peak_refresh_rate");
        }
        if (r.brightness >= 0) {
            snap.brightMode = RootShell.exec("settings get system screen_brightness_mode");
            snap.brightVal = RootShell.exec("settings get system screen_brightness");
        }
        if (r.wifiAction >= 0) snap.wifiOn = RootShell.exec("settings get global wifi_on");
        if (r.mobileDataAction >= 0) snap.dataOn = RootShell.exec("settings get global mobile_data");
        if (r.airplaneAction >= 0) snap.airplaneOn = RootShell.exec("settings get global airplane_mode_on");
        if (r.batterySaverAction >= 0) snap.lowPower = RootShell.exec("settings get global low_power");
        if (r.darkModeAction >= 0) snap.nightMode = RootShell.exec("settings get secure ui_night_mode");
        if (r.autoRotateAction >= 0) snap.autoRotate = RootShell.exec("settings get system accelerometer_rotation");

        if (r.mediaVolume >= 0) {
            try {
                AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
                if (am != null) {
                    snap.mediaVol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                    int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    int target = Math.round(max * (Math.max(0, Math.min(100, r.mediaVolume)) / 100f));
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0);
                }
            } catch (Throwable ignored) {}
        }
        SNAPSHOTS.put(r.id, snap);

        StringBuilder sb = new StringBuilder();
        if (r.tileComponent != null && !r.tileComponent.trim().isEmpty()) {
            sb.append("cmd statusbar click-tile ").append(r.tileComponent.trim()).append("\n");
        }
        for (String pkg : r.killPkgs) {
            if (!pkg.equals(ctx.getPackageName())) {
                sb.append("am force-stop ").append(pkg).append("\n");
            }
        }
        if (r.dndMode >= 0) {
            sb.append("cmd notification set_dnd ");
            if (r.dndMode == 0) sb.append("off\n");
            else if (r.dndMode == 1) sb.append("priority\n");
            else sb.append("total\n");
        }
        if (r.refreshRate > 0) {
            sb.append("settings put system min_refresh_rate ").append(r.refreshRate).append(".0\n");
            sb.append("settings put system peak_refresh_rate ").append(r.refreshRate).append(".0\n");
        }
        if (r.brightness >= 0) {
            sb.append("settings put system screen_brightness_mode 0\n");
            sb.append("settings put system screen_brightness ").append(r.brightness).append("\n");
        }
        if (r.wifiAction >= 0) {
            sb.append(r.wifiAction == 1 ? "svc wifi enable\n" : "svc wifi disable\n");
        }
        if (r.mobileDataAction >= 0) {
            sb.append(r.mobileDataAction == 1 ? "svc data enable\n" : "svc data disable\n");
        }
        if (r.airplaneAction >= 0) {
            sb.append(r.airplaneAction == 1 ? "cmd connectivity airplane-mode enable\n" : "cmd connectivity airplane-mode disable\n");
        }
        if (r.batterySaverAction >= 0) {
            sb.append("settings put global low_power ").append(r.batterySaverAction).append("\n");
        }
        if (r.darkModeAction >= 0) {
            sb.append(r.darkModeAction == 1 ? "cmd uimode night yes\n" : "cmd uimode night no\n");
        }
        if (r.autoRotateAction >= 0) {
            if (r.autoRotateAction == 1) {
                sb.append("settings put system accelerometer_rotation 1\n");
            } else {
                sb.append("settings put system accelerometer_rotation 0\nsettings put system user_rotation 0\n");
            }
        }
        if (r.customCmd != null && !r.customCmd.trim().isEmpty()) {
            sb.append(r.customCmd.trim()).append("\n");
        }
        if (sb.length() > 0) {
            String out = RootShell.exec(sb.toString());
            if (!out.isEmpty()) AppLogger.log("SHELL", "[" + r.name + "] 输出: " + out);
        }
    }

    public static synchronized void deactivate(Context ctx, Rule r) {
        if (!ACTIVE_RULES.contains(r.id)) return;
        ACTIVE_RULES.remove(r.id);
        Snapshot snap = SNAPSHOTS.remove(r.id);
        if (!r.autoRevert) {
            AppLogger.log("STATE", "情境 [" + r.name + "] 条件已退出（未开启自动还原）");
            return;
        }
        AppLogger.log("REVERT", "还原情境 [" + r.name + "] 系统状态");

        if (snap != null && r.mediaVolume >= 0 && snap.mediaVol >= 0) {
            try {
                AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
                if (am != null) am.setStreamVolume(AudioManager.STREAM_MUSIC, snap.mediaVol, 0);
            } catch (Throwable ignored) {}
        }

        StringBuilder sb = new StringBuilder();
        if (r.tileComponent != null && !r.tileComponent.trim().isEmpty()) {
            sb.append("cmd statusbar click-tile ").append(r.tileComponent.trim()).append("\n");
        }
        if (snap != null) {
            if (r.dndMode >= 0 && isValid(snap.dnd)) {
                sb.append("settings put global zen_mode ").append(snap.dnd).append("\n");
            }
            if (r.refreshRate > 0) {
                if (isValid(snap.minHz)) sb.append("settings put system min_refresh_rate ").append(snap.minHz).append("\n");
                else sb.append("settings delete system min_refresh_rate\n");
                if (isValid(snap.peakHz)) sb.append("settings put system peak_refresh_rate ").append(snap.peakHz).append("\n");
                else sb.append("settings delete system peak_refresh_rate\n");
            }
            if (r.brightness >= 0) {
                if (isValid(snap.brightMode)) sb.append("settings put system screen_brightness_mode ").append(snap.brightMode).append("\n");
                if (isValid(snap.brightVal)) sb.append("settings put system screen_brightness ").append(snap.brightVal).append("\n");
            }
            if (r.wifiAction >= 0 && isValid(snap.wifiOn)) {
                sb.append("1".equals(snap.wifiOn) ? "svc wifi enable\n" : "svc wifi disable\n");
            }
            if (r.mobileDataAction >= 0 && isValid(snap.dataOn)) {
                sb.append("1".equals(snap.dataOn) ? "svc data enable\n" : "svc data disable\n");
            }
            if (r.airplaneAction >= 0 && isValid(snap.airplaneOn)) {
                sb.append("1".equals(snap.airplaneOn) ? "cmd connectivity airplane-mode enable\n" : "cmd connectivity airplane-mode disable\n");
            }
            if (r.batterySaverAction >= 0 && isValid(snap.lowPower)) {
                sb.append("settings put global low_power ").append(snap.lowPower).append("\n");
            }
            if (r.darkModeAction >= 0 && isValid(snap.nightMode)) {
                sb.append("2".equals(snap.nightMode) ? "cmd uimode night yes\n" : "cmd uimode night no\n");
            }
            if (r.autoRotateAction >= 0 && isValid(snap.autoRotate)) {
                sb.append("settings put system accelerometer_rotation ").append(snap.autoRotate).append("\n");
            }
        }
        if (r.revertCmd != null && !r.revertCmd.trim().isEmpty()) {
            sb.append(r.revertCmd.trim()).append("\n");
        }
        if (sb.length() > 0) {
            RootShell.exec(sb.toString());
        }
    }

    private static boolean isValid(String v) {
        return v != null && !v.isEmpty() && !"null".equalsIgnoreCase(v) && !v.startsWith("ERR");
    }

    public static String cleanAllBackground(Context ctx) {
        AppLogger.log("CLEAN", "执行一键深度后台净化");
        String self = ctx.getPackageName();
        String cmd = "for p in $(pm list packages -3 | cut -d: -f2); do " +
                "if [ \"$p\" != \"" + self + "\" ]; then am force-stop \"$p\"; fi; " +
                "done; sync; echo 3 > /proc/sys/vm/drop_caches; echo DONE";
        String res = RootShell.exec(cmd);
        AppLogger.log("CLEAN", "后台净化完成: " + res);
        return res;
    }
}
