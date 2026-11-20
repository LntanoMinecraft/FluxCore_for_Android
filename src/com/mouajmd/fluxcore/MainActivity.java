package com.mouajmd.fluxcore;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public class MainActivity extends Activity {
    private Md3e t;
    private int currentTab = 0;
    private FrameLayout pageContainer;
    private TextView[] navItems = new TextView[3];
    private TextView badgeRoot;
    private TextView badgeService;
    private TextView fabAdd;
    private boolean hasRoot = false;

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (currentTab == 0) renderTab(0, false);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        t = Md3e.of(this);
        getWindow().setStatusBarColor(t.bg);
        getWindow().setNavigationBarColor(t.bg);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(t.bg);

        LinearLayout mainCol = new LinearLayout(this);
        mainCol.setOrientation(LinearLayout.VERTICAL);
        root.addView(mainCol, new FrameLayout.LayoutParams(-1, -1));

        mainCol.addView(buildHeader());

        pageContainer = new FrameLayout(this);
        LinearLayout.LayoutParams pclp = new LinearLayout.LayoutParams(-1, 0, 1f);
        mainCol.addView(pageContainer, pclp);

        fabAdd = new TextView(this);
        fabAdd.setText("＋ 新建情境");
        fabAdd.setTextColor(t.onPrimaryContainer);
        fabAdd.setTextSize(15);
        fabAdd.setTypeface(null, Typeface.BOLD);
        fabAdd.setBackground(Md3e.ripple(this, t.primaryContainer, 0x22000000, 22));
        int fp = Md3e.dp(this, 18);
        fabAdd.setPadding(fp, Md3e.dp(this, 14), fp, Md3e.dp(this, 14));
        fabAdd.setElevation(Md3e.dp(this, 6));
        Md3e.attachSpringTouch(fabAdd);
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(-2, -2);
        flp.gravity = Gravity.BOTTOM | Gravity.END;
        flp.rightMargin = Md3e.dp(this, 20);
        flp.bottomMargin = Md3e.dp(this, 92);
        root.addView(fabAdd, flp);

        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RuleEditorDialog.show(MainActivity.this, null, new RuleEditorDialog.OnSavedListener() {
                    @Override
                    public void onSaved() {
                        renderTab(0, true);
                    }
                });
            }
        });

        root.addView(buildFloatingNav());
        setContentView(root);

        checkRootAndService();
        renderTab(0, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter(Engine.ACTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(stateReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stateReceiver, f);
        }
        updateServiceBadge();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try { unregisterReceiver(stateReceiver); } catch (Exception ignored) {}
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        int hp = Md3e.dp(this, 20);
        header.setPadding(hp, Md3e.dp(this, 18), hp, Md3e.dp(this, 10));

        TextView title = new TextView(this);
        title.setText("流枢 · FluxCore");
        title.setTextSize(28);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(t.textPrimary);
        header.addView(title);

        LinearLayout pills = new LinearLayout(this);
        pills.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-1, -2);
        plp.topMargin = Md3e.dp(this, 10);

        badgeRoot = makePillBadge("检测 Root 权限...", t.surfaceHigh, t.textSecondary);
        badgeService = makePillBadge("监听引擎：未启动", t.surfaceHigh, t.textSecondary);

        LinearLayout.LayoutParams b1lp = new LinearLayout.LayoutParams(-2, -2);
        b1lp.rightMargin = Md3e.dp(this, 8);
        pills.addView(badgeRoot, b1lp);
        pills.addView(badgeService);

        badgeService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasRoot) {
                    Toast.makeText(MainActivity.this, "请先授予 Root 权限", Toast.LENGTH_SHORT).show();
                    checkRootAndService();
                    return;
                }
                Intent svc = new Intent(MainActivity.this, CoreService.class);
                if (CoreService.running) {
                    stopService(svc);
                    CoreService.running = false;
                } else {
                    if (Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
                    else startService(svc);
                    CoreService.running = true;
                }
                Md3e.pulse(badgeService);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() { updateServiceBadge(); }
                }, 250);
            }
        });

        header.addView(pills, plp);
        return header;
    }

    private TextView makePillBadge(String text, int bg, int fg) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(12);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(fg);
        tv.setBackground(Md3e.ripple(this, bg, 0x22000000, 50));
        tv.setPadding(Md3e.dp(this, 12), Md3e.dp(this, 6), Md3e.dp(this, 12), Md3e.dp(this, 6));
        Md3e.attachSpringTouch(tv);
        return tv;
    }

    private void checkRootAndService() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean ok = RootShell.isRooted();
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        hasRoot = ok;
                        if (ok) {
                            badgeRoot.setText("● Root 已就绪");
                            badgeRoot.setTextColor(t.onPrimaryContainer);
                            badgeRoot.setBackground(Md3e.ripple(MainActivity.this, t.primaryContainer, 0x22000000, 50));
                            if (!CoreService.running) {
                                Intent svc = new Intent(MainActivity.this, CoreService.class);
                                if (Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
                                else startService(svc);
                                CoreService.running = true;
                            }
                        } else {
                            badgeRoot.setText("○ 未获取 Root");
                            badgeRoot.setTextColor(t.onTertiaryContainer);
                            badgeRoot.setBackground(Md3e.ripple(MainActivity.this, t.tertiaryContainer, 0x22000000, 50));
                        }
                        updateServiceBadge();
                    }
                });
            }
        }).start();
    }

    private void updateServiceBadge() {
        if (CoreService.running) {
            badgeService.setText("● 后台监听中");
            badgeService.setTextColor(t.onSecondaryContainer);
            badgeService.setBackground(Md3e.ripple(this, t.secondaryContainer, 0x22000000, 50));
        } else {
            badgeService.setText("○ 点击启动监听");
            badgeService.setTextColor(t.textSecondary);
            badgeService.setBackground(Md3e.ripple(this, t.surfaceHigh, 0x22000000, 50));
        }
    }

    private View buildFloatingNav() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackground(Md3e.card(this, t.surfaceHigh, 50));
        bar.setElevation(Md3e.dp(this, 8));
        int p = Md3e.dp(this, 6);
        bar.setPadding(p, p, p, p);

        String[] labels = {"情境", "控制台", "关于"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            TextView item = new TextView(this);
            item.setText(labels[i]);
            item.setGravity(Gravity.CENTER);
            item.setTextSize(14);
            item.setTypeface(null, Typeface.BOLD);
            item.setPadding(0, Md3e.dp(this, 12), 0, Md3e.dp(this, 12));
            Md3e.attachSpringTouch(item);
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentTab != idx) renderTab(idx, true);
                }
            });
            navItems[i] = item;
            bar.addView(item, new LinearLayout.LayoutParams(0, -2, 1f));
        }

        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(-1, -2);
        blp.gravity = Gravity.BOTTOM;
        int m = Md3e.dp(this, 20);
        blp.setMargins(m, 0, m, Md3e.dp(this, 18));
        bar.setLayoutParams(blp);
        return bar;
    }

    private void renderTab(int idx, boolean animate) {
        currentTab = idx;
        for (int i = 0; i < 3; i++) {
            if (i == idx) {
                navItems[i].setTextColor(t.onPrimary);
                navItems[i].setBackground(Md3e.card(this, t.primary, 50));
            } else {
                navItems[i].setTextColor(t.textSecondary);
                navItems[i].setBackgroundColor(Color.TRANSPARENT);
            }
        }
        fabAdd.setVisibility(idx == 0 ? View.VISIBLE : View.GONE);

        pageContainer.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.setPadding(Md3e.dp(this, 20), Md3e.dp(this, 6), Md3e.dp(this, 20), Md3e.dp(this, 150));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        pageContainer.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        if (idx == 0) buildScenariosPage(content, animate);
        else if (idx == 1) buildConsolePage(content, animate);
        else buildAboutPage(content, animate);
    }

    private void buildScenariosPage(LinearLayout container, boolean animate) {
        List<Rule> rules = RuleStore.load(this);
        if (rules.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("暂无情境规则，点击右下角「＋ 新建情境」开始");
            empty.setTextColor(t.textSecondary);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, Md3e.dp(this, 80), 0, 0);
            container.addView(empty);
            return;
        }

        for (int i = 0; i < rules.size(); i++) {
            final Rule r = rules.get(i);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(Md3e.ripple(this, r.active ? t.primaryContainer : t.surface, 0x18888888, 28));
            int cp = Md3e.dp(this, 18);
            card.setPadding(cp, cp, cp, cp);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
            clp.bottomMargin = Md3e.dp(this, 14);
            Md3e.attachSpringTouch(card);

            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvName = new TextView(this);
            tvName.setText(r.name);
            tvName.setTextSize(18);
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setTextColor(r.active ? t.onPrimaryContainer : t.textPrimary);
            topRow.addView(tvName, new LinearLayout.LayoutParams(0, -2, 1f));

            Switch sw = new Switch(this);
            sw.setChecked(r.enabled);
            sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    r.enabled = isChecked;
                    RuleStore.upsert(MainActivity.this, r);
                }
            });
            topRow.addView(sw);
            card.addView(topRow);

            TextView tvDesc = new TextView(this);
            tvDesc.setText(buildRuleSummary(r));
            tvDesc.setTextSize(13);
            tvDesc.setTextColor(r.active ? t.onPrimaryContainer : t.textSecondary);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1, -2);
            dlp.topMargin = Md3e.dp(this, 6);
            dlp.bottomMargin = Md3e.dp(this, 14);
            card.addView(tvDesc, dlp);

            LinearLayout bottomRow = new LinearLayout(this);
            bottomRow.setOrientation(LinearLayout.HORIZONTAL);
            bottomRow.setGravity(Gravity.CENTER_VERTICAL);

            final TextView btnExec = new TextView(this);
            if (r.active) {
                btnExec.setText("■ 运行中 · 点击还原");
                btnExec.setTextColor(t.onPrimary);
                btnExec.setBackground(Md3e.ripple(this, t.primary, 0x33FFFFFF, 50));
            } else {
                btnExec.setText("▶ 主动触发");
                btnExec.setTextColor(t.onSecondaryContainer);
                btnExec.setBackground(Md3e.ripple(this, t.secondaryContainer, 0x22000000, 50));
            }
            btnExec.setTextSize(13);
            btnExec.setTypeface(null, Typeface.BOLD);
            btnExec.setPadding(Md3e.dp(this, 16), Md3e.dp(this, 9), Md3e.dp(this, 16), Md3e.dp(this, 9));
            Md3e.attachSpringTouch(btnExec);

            btnExec.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Md3e.pulse(btnExec);
                    if (r.active) {
                        Engine.revert(MainActivity.this, r, new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "已还原：" + r.name, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        btnExec.setText("✓ 已执行");
                        Engine.apply(MainActivity.this, r, new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "已触发：" + r.name, Toast.LENGTH_SHORT).show();
                                if (!r.hasStatefulAction()) {
                                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            btnExec.setText("▶ 主动触发");
                                        }
                                    }, 1200);
                                }
                            }
                        });
                    }
                }
            });
            bottomRow.addView(btnExec);

            View spacer = new View(this);
            bottomRow.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

            TextView btnDel = new TextView(this);
            btnDel.setText("删除");
            btnDel.setTextSize(12);
            btnDel.setTextColor(t.textSecondary);
            btnDel.setPadding(Md3e.dp(this, 10), Md3e.dp(this, 8), Md3e.dp(this, 6), Md3e.dp(this, 8));
            btnDel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("删除情境")
                            .setMessage("确定删除「" + r.name + "」吗？")
                            .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    RuleStore.delete(MainActivity.this, r.id);
                                    renderTab(0, false);
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();
                }
            });
            bottomRow.addView(btnDel);

            card.addView(bottomRow);

            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RuleEditorDialog.show(MainActivity.this, r, new RuleEditorDialog.OnSavedListener() {
                        @Override
                        public void onSaved() {
                            renderTab(0, false);
                        }
                    });
                }
            });

            container.addView(card, clp);
            if (animate) Md3e.popIn(card, i * 45L);
        }
    }

    private String buildRuleSummary(Rule r) {
        StringBuilder sb = new StringBuilder();
        switch (r.triggerType) {
            case Rule.TRIG_MANUAL: sb.append("触发：仅手动主动触发"); break;
            case Rule.TRIG_APP: sb.append("触发：").append(r.triggerOn ? "打开 " : "退出 ").append(r.triggerPkg); break;
            case Rule.TRIG_CHARGER: sb.append("触发：").append(r.triggerOn ? "连接充电器" : "断开充电器"); break;
            case Rule.TRIG_SCREEN: sb.append("触发：").append(r.triggerOn ? "屏幕点亮" : "屏幕关闭"); break;
            case Rule.TRIG_WIFI: sb.append("触发：").append(r.triggerOn ? "连接 Wi-Fi" : "断开 Wi-Fi"); break;
            case Rule.TRIG_BT: sb.append("触发：").append(r.triggerOn ? "连接蓝牙" : "断开蓝牙"); break;
        }
        sb.append("\n动作：");
        int count = 0;
        if (!r.killPkgs.isEmpty()) { sb.append("强停 ").append(r.killPkgs.size()).append(" 个应用  "); count++; }
        if (r.dndMode >= 0) { sb.append("勿扰调节  "); count++; }
        if (r.refreshRate > 0) { sb.append(r.refreshRate).append("Hz  "); count++; }
        if (r.brightness >= 0) { sb.append("亮度 ").append(r.brightness).append("  "); count++; }
        if (r.wifiAction >= 0) { sb.append(r.wifiAction == 1 ? "开Wi-Fi  " : "关Wi-Fi  "); count++; }
        if (!r.customCmd.trim().isEmpty()) { sb.append("自定义Shell"); count++; }
        if (count == 0) sb.append("未配置具体动作");
        return sb.toString();
    }

    private void buildConsolePage(LinearLayout container, boolean animate) {
        LinearLayout cleanCard = makeBentoCard(t.primaryContainer, 28);
        TextView cTitle = new TextView(this);
        cTitle.setText("一键深度后台净化");
        cTitle.setTextSize(19);
        cTitle.setTypeface(null, Typeface.BOLD);
        cTitle.setTextColor(t.onPrimaryContainer);
        cleanCard.addView(cTitle);

        TextView cSub = new TextView(this);
        cSub.setText("强行停止所有非白名单第三方后台应用并释放内核缓存");
        cSub.setTextSize(13);
        cSub.setTextColor(t.onPrimaryContainer);
        LinearLayout.LayoutParams cslp = new LinearLayout.LayoutParams(-1, -2);
        cslp.topMargin = Md3e.dp(this, 4);
        cslp.bottomMargin = Md3e.dp(this, 14);
        cleanCard.addView(cSub, cslp);

        final TextView btnClean = new TextView(this);
        btnClean.setText("立即清理释放");
        btnClean.setTextColor(t.onPrimary);
        btnClean.setTypeface(null, Typeface.BOLD);
        btnClean.setGravity(Gravity.CENTER);
        btnClean.setBackground(Md3e.ripple(this, t.primary, 0x33FFFFFF, 50));
        btnClean.setPadding(0, Md3e.dp(this, 12), 0, Md3e.dp(this, 12));
        Md3e.attachSpringTouch(btnClean);
        cleanCard.addView(btnClean);

        btnClean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Md3e.pulse(btnClean);
                btnClean.setText("正在清理...");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        PackageManager pm = getPackageManager();
                        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
                        StringBuilder sb = new StringBuilder();
                        int n = 0;
                        for (ApplicationInfo ai : apps) {
                            if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                                    && !ai.packageName.equals(getPackageName())) {
                                sb.append("am force-stop ").append(ai.packageName).append(";\n");
                                n++;
                            }
                        }
                        sb.append("sync && echo 3 > /proc/sys/vm/drop_caches;\n");
                        RootShell.exec(sb.toString());
                        final int killed = n;
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                btnClean.setText("✓ 已净化 " + killed + " 个应用");
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() { btnClean.setText("立即清理释放"); }
                                }, 1800);
                            }
                        });
                    }
                }).start();
            }
        });
        container.addView(cleanCard);
        if (animate) Md3e.popIn(cleanCard, 0);

        LinearLayout dndCard = makeBentoCard(t.surface, 28);
        dndCard.addView(bentoTitle("勿扰模式快捷控制"));
        LinearLayout dndRow = new LinearLayout(this);
        dndRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] dndNames = {"关闭勿扰", "仅重要", "完全静音"};
        final String[] dndCmds = {
                "cmd notification set_dnd off",
                "cmd notification set_dnd priority",
                "cmd notification set_dnd total"
        };
        for (int i = 0; i < 3; i++) {
            final int k = i;
            TextView pill = makeActionPill(dndNames[i]);
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(0, -2, 1f);
            if (i < 2) plp.rightMargin = Md3e.dp(this, 8);
            pill.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Md3e.pulse(v);
                    RootShell.execAsync(dndCmds[k], null);
                    Toast.makeText(MainActivity.this, "勿扰已切换：" + dndNames[k], Toast.LENGTH_SHORT).show();
                }
            });
            dndRow.addView(pill, plp);
        }
        dndCard.addView(dndRow);
        container.addView(dndCard);
        if (animate) Md3e.popIn(dndCard, 50);

        LinearLayout hzCard = makeBentoCard(t.surface, 28);
        hzCard.addView(bentoTitle("屏幕刷新率锁定"));
        LinearLayout hzRow = new LinearLayout(this);
        hzRow.setOrientation(LinearLayout.HORIZONTAL);
        final int[] hzs = {60, 90, 120, 0};
        String[] hzLabels = {"60Hz", "90Hz", "120Hz", "恢复默认"};
        for (int i = 0; i < 4; i++) {
            final int k = i;
            TextView pill = makeActionPill(hzLabels[i]);
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(0, -2, 1f);
            if (i < 3) plp.rightMargin = Md3e.dp(this, 6);
            pill.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Md3e.pulse(v);
                    String c = hzs[k] == 0
                            ? "settings delete system min_refresh_rate; settings delete system peak_refresh_rate"
                            : "settings put system min_refresh_rate " + hzs[k] + "; settings put system peak_refresh_rate " + hzs[k];
                    RootShell.execAsync(c, null);
                    Toast.makeText(MainActivity.this, "刷新率已设为：" + hzLabels[k], Toast.LENGTH_SHORT).show();
                }
            });
            hzRow.addView(pill, plp);
        }
        hzCard.addView(hzRow);
        container.addView(hzCard);
        if (animate) Md3e.popIn(hzCard, 100);

        LinearLayout brCard = makeBentoCard(t.surface, 28);
        final TextView brTitle = bentoTitle("手动屏幕亮度调节");
        brCard.addView(brTitle);
        SeekBar sb = new SeekBar(this);
        sb.setMax(255);
        sb.setProgress(128);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int val = Math.max(5, progress);
                    brTitle.setText("手动屏幕亮度调节 (" + val + ")");
                    RootShell.execAsync("settings put system screen_brightness_mode 0; settings put system screen_brightness " + val, null);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        brCard.addView(sb);
        container.addView(brCard);
        if (animate) Md3e.popIn(brCard, 150);

        LinearLayout shCard = makeBentoCard(t.surface, 28);
        shCard.addView(bentoTitle("快捷 Root Shell 终端"));
        final EditText etShell = new EditText(this);
        etShell.setHint("输入 su 命令，例如：dumpsys battery");
        etShell.setHintTextColor(t.textSecondary);
        etShell.setTextColor(t.textPrimary);
        etShell.setTextSize(13);
        etShell.setBackground(Md3e.card(this, t.surfaceHigh, 16));
        int sp = Md3e.dp(this, 12);
        etShell.setPadding(sp, sp, sp, sp);
        shCard.addView(etShell);

        final TextView tvOut = new TextView(this);
        tvOut.setTextColor(t.textSecondary);
        tvOut.setTextSize(12);
        tvOut.setVisibility(View.GONE);
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(-1, -2);
        olp.topMargin = Md3e.dp(this, 8);
        shCard.addView(tvOut, olp);

        TextView btnRun = makeActionPill("执行命令");
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
        rlp.topMargin = Md3e.dp(this, 10);
        btnRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String c = etShell.getText().toString().trim();
                if (c.isEmpty()) return;
                Md3e.pulse(v);
                RootShell.execAsync(c, new RootShell.Callback() {
                    @Override
                    public void onResult(boolean success, String output) {
                        tvOut.setVisibility(View.VISIBLE);
                        tvOut.setText(output.isEmpty() ? "(执行完成，无输出)" : output);
                    }
                });
            }
        });
        shCard.addView(btnRun, rlp);
        container.addView(shCard);
        if (animate) Md3e.popIn(shCard, 200);
    }

    private void buildAboutPage(LinearLayout container, boolean animate) {
        LinearLayout hero = makeBentoCard(t.primaryContainer, 28);
        TextView appName = new TextView(this);
        appName.setText("流枢 · FluxCore");
        appName.setTextSize(24);
        appName.setTypeface(null, Typeface.BOLD);
        appName.setTextColor(t.onPrimaryContainer);
        hero.addView(appName);

        TextView ver = new TextView(this);
        ver.setText("v1.0.0 · 原生 Root 事件驱动自动化与系统控制中枢");
        ver.setTextSize(13);
        ver.setTextColor(t.onPrimaryContainer);
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(-1, -2);
        vlp.topMargin = Md3e.dp(this, 6);
        hero.addView(ver, vlp);
        container.addView(hero);
        if (animate) Md3e.popIn(hero, 0);

        LinearLayout ghCard = makeBentoCard(t.surface, 28);
        TextView ghTitle = bentoTitle("GitHub 开源主页");
        ghCard.addView(ghTitle);
        TextView ghUrl = new TextView(this);
        ghUrl.setText("https://github.com/MOUAJMD");
        ghUrl.setTextColor(t.primary);
        ghUrl.setTextSize(14);
        ghCard.addView(ghUrl);

        TextView btnGh = makeActionPill("打开 GitHub 主页 ↗");
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(-1, -2);
        glp.topMargin = Md3e.dp(this, 12);
        btnGh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Md3e.pulse(v);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/MOUAJMD")));
            }
        });
        ghCard.addView(btnGh, glp);
        container.addView(ghCard);
        if (animate) Md3e.popIn(ghCard, 60);

        LinearLayout caCard = makeBentoCard(t.surface, 28);
        caCard.addView(bentoTitle("酷安开发者账号"));
        TextView caId = new TextView(this);
        caId.setText("@C_AJMD_W");
        caId.setTextColor(t.primary);
        caId.setTextSize(15);
        caId.setTypeface(null, Typeface.BOLD);
        caCard.addView(caId);

        TextView btnCa = makeActionPill("复制酷安 ID 并跳转");
        LinearLayout.LayoutParams calp = new LinearLayout.LayoutParams(-1, -2);
        calp.topMargin = Md3e.dp(this, 12);
        btnCa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Md3e.pulse(v);
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("coolapk", "C_AJMD_W"));
                }
                Toast.makeText(MainActivity.this, "已复制酷安账号：@C_AJMD_W", Toast.LENGTH_SHORT).show();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("coolmarket://u/C_AJMD_W")));
                } catch (Exception ignored) {}
            }
        });
        caCard.addView(btnCa, calp);
        container.addView(caCard);
        if (animate) Md3e.popIn(caCard, 120);
    }

    private LinearLayout makeBentoCard(int bgColor, float radius) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Md3e.card(this, bgColor, radius));
        int p = Md3e.dp(this, 18);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = Md3e.dp(this, 14);
        card.setLayoutParams(lp);
        return card;
    }

    private TextView bentoTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(t.textPrimary);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = Md3e.dp(this, 10);
        tv.setLayoutParams(lp);
        return tv;
    }

    private TextView makeActionPill(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(13);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(t.onSecondaryContainer);
        tv.setBackground(Md3e.ripple(this, t.secondaryContainer, 0x22000000, 50));
        tv.setPadding(Md3e.dp(this, 10), Md3e.dp(this, 11), Md3e.dp(this, 10), Md3e.dp(this, 11));
        Md3e.attachSpringTouch(tv);
        return tv;
    }
}
