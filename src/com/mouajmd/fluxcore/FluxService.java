package com.mouajmd.fluxcore;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FluxService extends Service {
    public static volatile boolean isRunning = false;
    private volatile boolean stopFlag = false;
    private Process logcatProc;

    private volatile String currentFgPkg = "";
    private volatile boolean isCharging = false;
    private volatile int batteryLevel = 50;
    private volatile boolean isScreenOn = true;
    private volatile boolean isWifiConnected = false;
    private volatile String currentWifiSsid = "";
    private volatile boolean isBtConnected = false;
    private volatile String connectedBtDump = "";
    private volatile boolean isHeadsetPlugged = false;
    private volatile boolean isLandscape = false;

    private final BroadcastReceiver sysReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String act = intent.getAction();
            if (act == null) return;
            if (Intent.ACTION_POWER_CONNECTED.equals(act)) {
                isCharging = true;
                AppLogger.log("EVENT", "系统事件: 充电器已连接");
                evaluateAllRules();
            } else if (Intent.ACTION_POWER_DISCONNECTED.equals(act)) {
                isCharging = false;
                AppLogger.log("EVENT", "系统事件: 充电器已断开");
                evaluateAllRules();
            } else if (Intent.ACTION_BATTERY_CHANGED.equals(act)) {
                int lvl = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                if (lvl >= 0 && scale > 0) {
                    int pct = Math.round((lvl * 100f) / scale);
                    if (pct != batteryLevel) {
                        batteryLevel = pct;
                        evaluateAllRules();
                    }
                }
            } else if (Intent.ACTION_SCREEN_ON.equals(act)) {
                isScreenOn = true;
                AppLogger.log("EVENT", "系统事件: 屏幕亮起");
                evaluateAllRules();
            } else if (Intent.ACTION_SCREEN_OFF.equals(act)) {
                isScreenOn = false;
                AppLogger.log("EVENT", "系统事件: 屏幕熄灭");
                evaluateAllRules();
            } else if (AudioManager.ACTION_HEADSET_PLUG.equals(act)) {
                int state = intent.getIntExtra("state", 0);
                boolean plugged = (state == 1);
                if (plugged != isHeadsetPlugged) {
                    isHeadsetPlugged = plugged;
                    AppLogger.log("EVENT", "系统事件: 有线耳机" + (plugged ? "已插入" : "已拔出"));
                    evaluateAllRules();
                }
            } else if (Intent.ACTION_CONFIGURATION_CHANGED.equals(act)) {
                boolean land = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
                if (land != isLandscape) {
                    isLandscape = land;
                    AppLogger.log("EVENT", "系统事件: 屏幕切换为" + (land ? "横屏" : "竖屏"));
                    evaluateAllRules();
                }
            } else if (Intent.ACTION_TIME_TICK.equals(act)) {
                evaluateAllRules();
            } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(act)) {
                refreshNetworkAndBtAsync("网络状态变更");
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(act) ||
                       BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(act) ||
                       BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(act)) {
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String devName = "";
                try {
                    if (dev != null && dev.getName() != null) devName = dev.getName();
                } catch (Throwable ignored) {}
                refreshNetworkAndBtAsync("蓝牙状态变更" + (devName.isEmpty() ? "" : ": " + devName));
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        AppLogger.log("ENGINE", "FluxCore v1.2.0 自动化引擎启动");
        startForegroundCompat();
        initInitialStates();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(AudioManager.ACTION_HEADSET_PLUG);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(sysReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(sysReceiver, filter);
        }

        startEventStreamThread();
    }

    private void initInitialStates() {
        try {
            Intent bat = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (bat != null) {
                int plugged = bat.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                isCharging = plugged != 0;
                int lvl = bat.getIntExtra(BatteryManager.EXTRA_LEVEL, 50);
                int scale = bat.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                if (scale > 0) batteryLevel = Math.round((lvl * 100f) / scale);
            }
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) isScreenOn = pm.isInteractive();
            isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        } catch (Throwable ignored) {}
        refreshNetworkAndBtAsync("初始化硬件状态");
    }

    private void refreshNetworkAndBtAsync(final String reason) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    boolean wifiOk = false;
                    if (cm != null) {
                        NetworkInfo ni = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                        wifiOk = (ni != null && ni.isConnected());
                    }
                    isWifiConnected = wifiOk;
                    currentWifiSsid = wifiOk ? RootShell.queryCurrentWifiSsid() : "";

                    String btDump = RootShell.queryConnectedBtDevices();
                    connectedBtDump = btDump;
                    isBtConnected = btDump.toLowerCase().contains("connected") && !btDump.toLowerCase().contains("disconnected");
                    if (btDump.contains("mActiveDevice=") && !btDump.contains("mActiveDevice=null")) {
                        isBtConnected = true;
                    }

                    AppLogger.log("STATE", reason + " -> Wi-Fi=" + isWifiConnected +
                            (currentWifiSsid.isEmpty() ? "" : "(" + currentWifiSsid + ")") +
                            ", BT=" + isBtConnected + ", Bat=" + batteryLevel + "%");
                    evaluateAllRules();
                } catch (Throwable ignored) {}
            }
        }).start();
    }

    private void startForegroundCompat() {
        String chId = "fluxcore_engine";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(chId, "流枢自动化引擎", NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, chId)
                : new Notification.Builder(this);
        b.setContentTitle("流枢 · FluxCore 运行中")
         .setContentText("零轮询监听系统事件 · 规则引擎已就绪")
         .setSmallIcon(android.R.drawable.ic_popup_sync)
         .setOngoing(true);
        startForeground(101, b.build());
    }

    private void startEventStreamThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Pattern resumePat = Pattern.compile("(?:am_on_resume_called|wm_on_resume_called|am_resumed):\\s*\\[\\d+,([a-zA-Z0-9_\\.]+),");
                while (!stopFlag) {
                    try {
                        logcatProc = Runtime.getRuntime().exec("su");
                        DataOutputStream os = new DataOutputStream(logcatProc.getOutputStream());
                        os.writeBytes("logcat -b events -T 1 -v brief *:I\n");
                        os.flush();

                        BufferedReader br = new BufferedReader(new InputStreamReader(logcatProc.getInputStream(), StandardCharsets.UTF_8));
                        String line;
                        while (!stopFlag && (line = br.readLine()) != null) {
                            Matcher m = resumePat.matcher(line);
                            if (m.find()) {
                                String pkg = m.group(1);
                                if (pkg != null && !pkg.equals(currentFgPkg)) {
                                    currentFgPkg = pkg;
                                    AppLogger.log("APP", "前台应用切换 -> " + pkg);
                                    evaluateAllRules();
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                }
            }
        }).start();
    }

    private boolean isInTimeWindow(String spec) {
        String s = (spec == null || spec.trim().isEmpty()) ? "23:00-07:00" : spec.trim();
        try {
            String[] parts = s.split("-");
            if (parts.length != 2) return false;
            String[] p1 = parts[0].trim().split(":");
            String[] p2 = parts[1].trim().split(":");
            int startMin = Integer.parseInt(p1[0]) * 60 + Integer.parseInt(p1[1]);
            int endMin = Integer.parseInt(p2[0]) * 60 + Integer.parseInt(p2[1]);
            Calendar cal = Calendar.getInstance();
            int nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
            if (startMin <= endMin) {
                return nowMin >= startMin && nowMin < endMin;
            } else {
                return nowMin >= startMin || nowMin < endMin;
            }
        } catch (Throwable e) {
            return false;
        }
    }

    private boolean checkConditionMatch(int type, boolean expectedOn, List<String> pkgs, String targetName, int threshold) {
        switch (type) {
            case Rule.TRIG_APP: {
                if (pkgs == null || pkgs.isEmpty()) return false;
                boolean inList = pkgs.contains(currentFgPkg);
                return expectedOn ? inList : !inList;
            }
            case Rule.TRIG_CHARGING:
                return isCharging == expectedOn;
            case Rule.TRIG_SCREEN:
                return isScreenOn == expectedOn;
            case Rule.TRIG_WIFI: {
                String kw = targetName == null ? "" : targetName.trim();
                if (kw.isEmpty()) {
                    return isWifiConnected == expectedOn;
                } else {
                    boolean matched = isWifiConnected && currentWifiSsid.equalsIgnoreCase(kw);
                    return expectedOn ? matched : !matched;
                }
            }
            case Rule.TRIG_BT: {
                String kw = targetName == null ? "" : targetName.trim().toLowerCase();
                if (kw.isEmpty()) {
                    return isBtConnected == expectedOn;
                } else {
                    boolean matched = isBtConnected && connectedBtDump.toLowerCase().contains(kw);
                    return expectedOn ? matched : !matched;
                }
            }
            case Rule.TRIG_BATTERY:
                return expectedOn ? (batteryLevel >= threshold) : (batteryLevel <= threshold);
            case Rule.TRIG_HEADSET:
                return isHeadsetPlugged == expectedOn;
            case Rule.TRIG_ORIENTATION:
                return isLandscape == expectedOn;
            case Rule.TRIG_TIME:
                return isInTimeWindow(targetName);
            default:
                return false;
        }
    }

    private synchronized void evaluateAllRules() {
        List<Rule> rules = RuleStore.load(this);
        for (Rule r : rules) {
            if (!r.enabled || r.triggerType == Rule.TRIG_MANUAL) continue;

            boolean allMatched = checkConditionMatch(r.triggerType, r.triggerOn, r.triggerPkgs, r.triggerTarget, r.triggerThreshold);
            if (allMatched && r.extraConditions != null) {
                for (Rule.Condition c : r.extraConditions) {
                    if (!checkConditionMatch(c.type, c.stateOn, c.pkgs, c.targetName, c.threshold)) {
                        allMatched = false;
                        break;
                    }
                }
            }

            if (allMatched) {
                ActionExecutor.activate(this, r);
            } else {
                ActionExecutor.deactivate(this, r);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopFlag = true;
        isRunning = false;
        AppLogger.log("ENGINE", "FluxCore 引擎已停止");
        try { unregisterReceiver(sysReceiver); } catch (Throwable ignored) {}
        if (logcatProc != null) logcatProc.destroy();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
