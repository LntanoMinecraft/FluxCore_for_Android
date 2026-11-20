package com.mouajmd.fluxcore;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoreService extends Service {
    public static volatile boolean running = false;
    private static final String CHANNEL_ID = "flux_core_engine";
    private volatile boolean stopFlag = false;
    private Process logProcess;
    private String currentPkg = "";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingExitTask;

    private final BroadcastReceiver sysReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                dispatchStateTrigger(Rule.TRIG_CHARGER, true);
            } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                dispatchStateTrigger(Rule.TRIG_CHARGER, false);
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                dispatchStateTrigger(Rule.TRIG_SCREEN, true);
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                dispatchStateTrigger(Rule.TRIG_SCREEN, false);
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info != null) {
                    dispatchStateTrigger(Rule.TRIG_WIFI, info.isConnected());
                }
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                dispatchStateTrigger(Rule.TRIG_BT, true);
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                dispatchStateTrigger(Rule.TRIG_BT, false);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        startForegroundNotification();
        registerSysReceivers();
        startEventLogMonitor();
    }

    private void startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "流枢自动化引擎", NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder nb = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        Notification n = nb.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("流枢 · FluxCore 引擎运行中")
                .setContentText("事件流与系统状态监听已就绪")
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
        startForeground(101, n);
    }

    private void registerSysReceivers() {
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_POWER_CONNECTED);
        f.addAction(Intent.ACTION_POWER_DISCONNECTED);
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        f.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        f.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        f.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(sysReceiver, f);
    }

    private void startEventLogMonitor() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Pattern pkgPat = Pattern.compile("\\[\\d+,([a-zA-Z0-9_\\.]+),");
                while (!stopFlag) {
                    try {
                        logProcess = Runtime.getRuntime().exec("su");
                        DataOutputStream os = new DataOutputStream(logProcess.getOutputStream());
                        os.writeBytes("logcat -b events -c && logcat -b events -v brief *:S am_focused_activity:I\n");
                        os.flush();
                        BufferedReader br = new BufferedReader(new InputStreamReader(logProcess.getInputStream()));
                        String line;
                        while (!stopFlag && (line = br.readLine()) != null) {
                            Matcher m = pkgPat.matcher(line);
                            if (m.find()) {
                                final String newPkg = m.group(1);
                                if (newPkg != null && !newPkg.equals(currentPkg)) {
                                    final String oldPkg = currentPkg;
                                    currentPkg = newPkg;
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            onAppChanged(oldPkg, newPkg);
                                        }
                                    });
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                    if (!stopFlag) {
                        try { Thread.sleep(2000); } catch (Exception ignored) {}
                    }
                }
            }
        }).start();
    }

    private void onAppChanged(final String oldPkg, final String newPkg) {
        if (pendingExitTask != null) {
            handler.removeCallbacks(pendingExitTask);
            pendingExitTask = null;
        }
        List<Rule> rules = RuleStore.load(this);
        for (Rule r : rules) {
            if (!r.enabled || r.triggerType != Rule.TRIG_APP || r.triggerPkg.isEmpty()) continue;
            if (r.triggerOn && r.triggerPkg.equals(newPkg)) {
                Engine.apply(this, r, null);
            } else if (!r.triggerOn && r.triggerPkg.equals(oldPkg)) {
                Engine.apply(this, r, null);
            }
        }
        if (!oldPkg.isEmpty() && !oldPkg.equals(newPkg)) {
            pendingExitTask = new Runnable() {
                @Override
                public void run() {
                    if (oldPkg.equals(currentPkg)) return;
                    List<Rule> latest = RuleStore.load(CoreService.this);
                    for (Rule r : latest) {
                        if (r.enabled && r.triggerType == Rule.TRIG_APP && r.triggerOn
                                && r.autoRevert && r.active && r.triggerPkg.equals(oldPkg)) {
                            Engine.revert(CoreService.this, r, null);
                        }
                    }
                }
            };
            handler.postDelayed(pendingExitTask, 2500);
        }
    }

    private void dispatchStateTrigger(int type, boolean isOn) {
        List<Rule> rules = RuleStore.load(this);
        for (Rule r : rules) {
            if (!r.enabled || r.triggerType != type) continue;
            if (r.triggerOn == isOn) {
                Engine.apply(this, r, null);
            } else if (r.autoRevert && r.active) {
                Engine.revert(this, r, null);
            }
        }
    }

    @Override
    public void onDestroy() {
        stopFlag = true;
        running = false;
        try { unregisterReceiver(sysReceiver); } catch (Exception ignored) {}
        if (logProcess != null) logProcess.destroy();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
