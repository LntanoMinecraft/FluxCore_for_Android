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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FluxService extends Service {
    public static volatile boolean isRunning = false;
    private volatile boolean stopFlag = false;
    private Process monitorProc;
    private final Object pollLock = new Object();

    private volatile String lastFgPkg = "";
    private volatile String currentFgPkg = "";
    private volatile boolean isCharging = false;
    private volatile boolean isScreenOn = true;
    private volatile boolean isWifiConnected = false;
    private volatile String currentWifiSsid = "";
    private volatile boolean isBtConnected = false;
    private volatile String connectedBtDump = "";

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
            } else if (Intent.ACTION_SCREEN_ON.equals(act)) {
                isScreenOn = true;
                AppLogger.log("EVENT", "系统事件: 屏幕亮起");
                synchronized (pollLock) {
                    pollLock.notifyAll();
                }
                evaluateAllRules();
            } else if (Intent.ACTION_SCREEN_OFF.equals(act)) {
                isScreenOn = false;
                AppLogger.log("EVENT", "系统事件: 屏幕熄灭");
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
        AppLogger.log("ENGINE", "FluxCore 自动化引擎启动 (兼容活动栈监听)");
        startForegroundCompat();
        initInitialStates();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(sysReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(sysReceiver, filter);
        }

        startAppMonitorThread();
    }

    private void initInitialStates() {
        try {
            Intent bat = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (bat != null) {
                int plugged = bat.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                isCharging = plugged != 0;
            }
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) isScreenOn = pm.isInteractive();
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
                    String ssid = RootShell.queryCurrentWifiSsid();
                    if (!wifiOk && !ssid.isEmpty()) wifiOk = true;
                    isWifiConnected = wifiOk;
                    currentWifiSsid = wifiOk ? ssid : "";

                    String btDump = RootShell.queryConnectedBtDevices();
                    connectedBtDump = btDump;
                    isBtConnected = btDump.toLowerCase().contains("connected") && !btDump.toLowerCase().contains("disconnected");
                    if (btDump.contains("mActiveDevice=") && !btDump.contains("mActiveDevice=null")) {
                        isBtConnected = true;
                    }

                    AppLogger.log("STATE", reason + " -> Wi-Fi=" + isWifiConnected +
                            (currentWifiSsid.isEmpty() ? "" : "(" + currentWifiSsid + ")") +
                            ", BT=" + isBtConnected);
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
         .setContentText("应用与系统事件监听引擎已就绪")
         .setSmallIcon(android.R.drawable.ic_popup_sync)
         .setOngoing(true);
        startForeground(101, b.build());
    }

    private void startAppMonitorThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Pattern actPat = Pattern.compile("(?:u\\d+\\s+|\\s)([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+)/");
                String probeCmd =
                        "dumpsys activity activities 2>/dev/null | grep -E 'topResumedActivity=|mResumedActivity:|ResumedActivity:' | head -n 2\n" +
                        "dumpsys window displays 2>/dev/null | grep -E 'mCurrentFocus=|mFocusedApp=' | head -n 2\n" +
                        "echo '===FLUX_TICK==='\n";

                while (!stopFlag) {
                    DataOutputStream os = null;
                    BufferedReader br = null;
                    try {
                        monitorProc = Runtime.getRuntime().exec("su");
                        os = new DataOutputStream(monitorProc.getOutputStream());
                        br = new BufferedReader(new InputStreamReader(monitorProc.getInputStream(), StandardCharsets.UTF_8));

                        while (!stopFlag) {
                            if (!isScreenOn) {
                                synchronized (pollLock) {
                                    if (!isScreenOn && !stopFlag) {
                                        pollLock.wait(5000);
                                    }
                                }
                                continue;
                            }

                            os.writeBytes(probeCmd);
                            os.flush();

                            String detectedPkg = "";
                            String fallbackPkg = "";
                            String line;
                            while (!stopFlag && (line = br.readLine()) != null) {
                                if ("===FLUX_TICK===".equals(line.trim())) {
                                    break;
                                }
                                Matcher m = actPat.matcher(line);
                                if (m.find()) {
                                    String candidate = m.group(1);
                                    if (candidate != null && !candidate.isEmpty()) {
                                        if (line.contains("ResumedActivity")) {
                                            if (detectedPkg.isEmpty()) detectedPkg = candidate;
                                        } else if (fallbackPkg.isEmpty()
                                                && !"com.android.systemui".equals(candidate)
                                                && !candidate.contains("inputmethod")) {
                                            fallbackPkg = candidate;
                                        }
                                    }
                                }
                            }

                            String finalPkg = !detectedPkg.isEmpty() ? detectedPkg : fallbackPkg;
                            if (!finalPkg.isEmpty() && !finalPkg.equals(currentFgPkg)) {
                                lastFgPkg = currentFgPkg;
                                currentFgPkg = finalPkg;
                                AppLogger.log("APP", "前台应用切换: " + (lastFgPkg.isEmpty() ? "无" : lastFgPkg) + " -> " + currentFgPkg);
                                evaluateAllRules();
                            }

                            synchronized (pollLock) {
                                pollLock.wait(450);
                            }
                        }
                    } catch (Throwable ignored) {
                    } finally {
                        try { if (os != null) os.close(); } catch (Throwable ignored) {}
                        try { if (br != null) br.close(); } catch (Throwable ignored) {}
                        if (monitorProc != null) {
                            try { monitorProc.destroy(); } catch (Throwable ignored) {}
                            monitorProc = null;
                        }
                    }
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                }
            }
        }).start();
    }

    private boolean checkConditionMatch(Rule r, int type, boolean expectedOn, List<String> pkgs, String targetName) {
        switch (type) {
            case Rule.TRIG_APP: {
                if (pkgs == null || pkgs.isEmpty() || currentFgPkg.isEmpty()) return false;
                boolean inList = pkgs.contains(currentFgPkg);
                if (expectedOn) {
                    return inList;
                } else {
                    if (inList) return false;
                    boolean wasInList = !lastFgPkg.isEmpty() && pkgs.contains(lastFgPkg);
                    return wasInList || ActionExecutor.isActive(r.id);
                }
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
                    if (isWifiConnected && (currentWifiSsid == null || currentWifiSsid.isEmpty())) {
                        currentWifiSsid = RootShell.queryCurrentWifiSsid();
                    }
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
            default:
                return false;
        }
    }

    private synchronized void evaluateAllRules() {
        List<Rule> rules = RuleStore.load(this);
        for (Rule r : rules) {
            if (!r.enabled || r.triggerType == Rule.TRIG_MANUAL) continue;

            boolean allMatched = checkConditionMatch(r, r.triggerType, r.triggerOn, r.triggerPkgs, r.triggerTarget);
            if (allMatched && r.extraConditions != null) {
                for (Rule.Condition c : r.extraConditions) {
                    if (!checkConditionMatch(r, c.type, c.stateOn, c.pkgs, c.targetName)) {
                        allMatched = false;
                        break;
                    }
                }
            }

            if (allMatched) {
                if (!ActionExecutor.isActive(r.id)) {
                    AppLogger.log("MATCH", "满足条件，激活情境: " + r.name + " (fg=" + currentFgPkg + ")");
                    ActionExecutor.activate(this, r);
                }
            } else {
                if (ActionExecutor.isActive(r.id)) {
                    ActionExecutor.deactivate(this, r);
                }
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
        synchronized (pollLock) {
            pollLock.notifyAll();
        }
        AppLogger.log("ENGINE", "FluxCore 引擎已停止");
        try { unregisterReceiver(sysReceiver); } catch (Throwable ignored) {}
        if (monitorProc != null) {
            try { monitorProc.destroy(); } catch (Throwable ignored) {}
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
