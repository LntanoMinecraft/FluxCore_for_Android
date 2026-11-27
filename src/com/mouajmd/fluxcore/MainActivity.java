package com.mouajmd.fluxcore;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private Md3e theme;
    private int currentTab = 0;
    private LinearLayout pageContainer;
    private LinearLayout statusChipsBox;
    private TextView[] navItems = new TextView[3];
    private TextView fabAdd;
    private boolean rootOk = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        theme = Md3e.of(this);
        getWindow().setStatusBarColor(theme.bg);
        getWindow().setNavigationBarColor(theme.bg);
        if (!theme.isDark) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }

        rootOk = RootShell.hasRoot();
        ensureServiceStarted();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(theme.bg);

        LinearLayout mainCol = new LinearLayout(this);
        mainCol.setOrientation(LinearLayout.VERTICAL);
        root.addView(mainCol, new FrameLayout.LayoutParams(-1, -1));

        mainCol.addView(buildHeader());

        ScrollView scroll = new ScrollView(this);
        LinearLayout.LayoutParams sclp = new LinearLayout.LayoutParams(-1, 0, 1f);
        mainCol.addView(scroll, sclp);

        pageContainer = new LinearLayout(this);
        pageContainer.setOrientation(LinearLayout.VERTICAL);
        int pad = Md3e.dp(this, 16);
        pageContainer.setPadding(pad, Md3e.dp(this, 6), pad, Md3e.dp(this, 96));
        scroll.addView(pageContainer);

        mainCol.addView(buildBottomNav());

        int navBarH = getSystemDimen("navigation_bar_height", Md3e.dp(this, 16));
        fabAdd = new TextView(this);
        fabAdd.setText("+ 新建情境");
        fabAdd.setTextSize(15);
        fabAdd.setTypeface(null, Typeface.BOLD);
        fabAdd.setTextColor(theme.onPrimaryContainer);
        fabAdd.setBackground(Md3e.ripple(this, theme.primaryContainer, 0x22000000, 22));
        fabAdd.setPadding(Md3e.dp(this, 20), Md3e.dp(this, 16), Md3e.dp(this, 20), Md3e.dp(this, 16));
        fabAdd.setElevation(Md3e.dp(this, 6));
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(-2, -2);
        flp.gravity = Gravity.BOTTOM | Gravity.END;
        flp.rightMargin = Md3e.dp(this, 20);
        flp.bottomMargin = Md3e.dp(this, 78) + navBarH;
        root.addView(fabAdd, flp);
        Md3e.attachSpringTouch(fabAdd);

        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RuleEditorDialog.show(MainActivity.this, null, new RuleEditorDialog.OnSavedListener() {
                    @Override
                    public void onSaved() { renderTab(); }
                });
            }
        });

        setContentView(root);
        renderTab();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ensureServiceStarted();
        refreshStatusChips();
        if (currentTab == 0 && pageContainer != null) {
            renderRulesPage();
        }
    }

    private void ensureServiceStarted() {
        if (rootOk && !FluxService.isRunning) {
            try {
                Intent svc = new Intent(this, FluxService.class);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
                else startService(svc);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() { refreshStatusChips(); }
                }, 400);
            } catch (Throwable ignored) {}
        }
    }

    private int getSystemDimen(String name, int fallback) {
        int id = getResources().getIdentifier(name, "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : fallback;
    }

    private View buildHeader() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = Md3e.dp(this, 20);
        int statusBarH = getSystemDimen("status_bar_height", Md3e.dp(this, 28));
        box.setPadding(p, statusBarH + Md3e.dp(this, 14), p, Md3e.dp(this, 10));

        TextView title = new TextView(this);
        title.setText("流枢 · FluxCore");
        title.setTextSize(26);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(theme.textPrimary);
        box.addView(title);

        statusChipsBox = new LinearLayout(this);
        statusChipsBox.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.topMargin = Md3e.dp(this, 8);
        refreshStatusChips();

        box.addView(statusChipsBox, clp);
        return box;
    }

    private void refreshStatusChips() {
        if (statusChipsBox == null) return;
        statusChipsBox.removeAllViews();
        statusChipsBox.addView(statusChip(rootOk ? "● Root 已就绪" : "● 未获取 Root", rootOk));
        View spacer = new View(this);
        statusChipsBox.addView(spacer, new LinearLayout.LayoutParams(Md3e.dp(this, 8), 1));
        statusChipsBox.addView(statusChip(FluxService.isRunning ? "● 后台监听中" : "● 服务未启动", FluxService.isRunning));
    }

    private TextView statusChip(String label, boolean active) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(12);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(active ? theme.onSecondaryContainer : theme.textSecondary);
        tv.setBackground(Md3e.card(this, active ? theme.secondaryContainer : theme.surfaceHigh, 50));
        tv.setPadding(Md3e.dp(this, 12), Md3e.dp(this, 6), Md3e.dp(this, 12), Md3e.dp(this, 6));
        return tv;
    }

    private View buildBottomNav() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackground(Md3e.card(this, theme.surfaceHigh, 28));
        int p = Md3e.dp(this, 8);
        bar.setPadding(p, p, p, p);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, -2);
        int m = Md3e.dp(this, 16);
        int navBarH = getSystemDimen("navigation_bar_height", Md3e.dp(this, 12));
        blp.setMargins(m, 0, m, Math.max(Md3e.dp(this, 12), navBarH));

        String[] labels = {"情境", "控制台", "设置"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            TextView tv = new TextView(this);
            tv.setText(labels[i]);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(14);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setPadding(0, Md3e.dp(this, 12), 0, Md3e.dp(this, 12));
            Md3e.attachSpringTouch(tv);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentTab = idx;
                    renderTab();
                }
            });
            navItems[i] = tv;
            bar.addView(tv, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        bar.setLayoutParams(blp);
        return bar;
    }

    private void renderTab() {
        for (int i = 0; i < 3; i++) {
            if (i == currentTab) {
                navItems[i].setTextColor(theme.onPrimary);
                navItems[i].setBackground(Md3e.card(this, theme.primary, 22));
            } else {
                navItems[i].setTextColor(theme.textSecondary);
                navItems[i].setBackgroundColor(Color.TRANSPARENT);
            }
        }
        fabAdd.setVisibility(currentTab == 0 ? View.VISIBLE : View.GONE);
        pageContainer.removeAllViews();

        if (currentTab == 0) renderRulesPage();
        else if (currentTab == 1) renderConsolePage();
        else renderSettingsPage();
    }

    private void renderRulesPage() {
        pageContainer.removeAllViews();
        final List<Rule> rules = RuleStore.load(this);
        if (rules.isEmpty()) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            empty.setBackground(Md3e.card(this, theme.surfaceLow, 26));
            int p = Md3e.dp(this, 32);
            empty.setPadding(p, p, p, p);

            TextView t1 = new TextView(this);
            t1.setText("尚未创建任何自动化情境");
            t1.setTextSize(17);
            t1.setTypeface(null, Typeface.BOLD);
            t1.setTextColor(theme.textPrimary);
            t1.setGravity(Gravity.CENTER);

            TextView t2 = new TextView(this);
            t2.setText("点击右下角「+ 新建情境」开始配置\n支持磁贴控制、音量/旋转/网络切换及多条件组合");
            t2.setTextSize(13);
            t2.setTextColor(theme.textSecondary);
            t2.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = Md3e.dp(this, 8);

            empty.addView(t1);
            empty.addView(t2, lp);
            pageContainer.addView(empty);
            Md3e.popIn(empty, 0);
            return;
        }

        for (int i = 0; i < rules.size(); i++) {
            final Rule r = rules.get(i);
            final boolean active = ActionExecutor.isActive(r.id);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(Md3e.ripple(this, active ? theme.primaryContainer : theme.surfaceLow, 0x22888888, 26));
            int p = Md3e.dp(this, 18);
            card.setPadding(p, p, p, p);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
            clp.bottomMargin = Md3e.dp(this, 12);

            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvTitle = new TextView(this);
            tvTitle.setText(r.name);
            tvTitle.setTextSize(18);
            tvTitle.setTypeface(null, Typeface.BOLD);
            tvTitle.setTextColor(active ? theme.onPrimaryContainer : theme.textPrimary);
            top.addView(tvTitle, new LinearLayout.LayoutParams(0, -2, 1f));

            final Switch sw = new Switch(this);
            sw.setChecked(r.enabled);
            sw.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Md3e.haptic(v);
                    final boolean targetEnabled = sw.isChecked();
                    r.enabled = targetEnabled;
                    RuleStore.upsert(MainActivity.this, r);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (!targetEnabled && ActionExecutor.isActive(r.id)) {
                                ActionExecutor.deactivate(MainActivity.this, r);
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() { renderRulesPage(); }
                            });
                        }
                    }).start();
                }
            });
            top.addView(sw);
            card.addView(top);

            TextView tvTrig = new TextView(this);
            tvTrig.setText("触发：" + r.describeTrigger());
            tvTrig.setTextSize(13);
            tvTrig.setTextColor(active ? theme.onPrimaryContainer : theme.textSecondary);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-1, -2);
            tlp.topMargin = Md3e.dp(this, 6);
            card.addView(tvTrig, tlp);

            TextView tvAct = new TextView(this);
            tvAct.setText("动作：" + r.describeActions());
            tvAct.setTextSize(13);
            tvAct.setTextColor(active ? theme.onPrimaryContainer : theme.textSecondary);
            LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(-1, -2);
            alp.topMargin = Md3e.dp(this, 4);
            card.addView(tvAct, alp);

            LinearLayout bar = new LinearLayout(this);
            bar.setOrientation(LinearLayout.HORIZONTAL);
            bar.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, -2);
            blp.topMargin = Md3e.dp(this, 14);

            final TextView btnTrigger = new TextView(this);
            btnTrigger.setText(active ? "■ 还原状态" : "▶ 主动触发");
            btnTrigger.setTextSize(13);
            btnTrigger.setTypeface(null, Typeface.BOLD);
            btnTrigger.setTextColor(active ? theme.onPrimary : theme.onSecondaryContainer);
            btnTrigger.setBackground(Md3e.ripple(this, active ? theme.primary : theme.secondaryContainer, 0x22000000, 50));
            btnTrigger.setPadding(Md3e.dp(this, 16), Md3e.dp(this, 9), Md3e.dp(this, 16), Md3e.dp(this, 9));
            Md3e.attachSpringTouch(btnTrigger);
            btnTrigger.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    btnTrigger.setEnabled(false);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (ActionExecutor.isActive(r.id)) {
                                ActionExecutor.deactivate(MainActivity.this, r);
                            } else {
                                ActionExecutor.activate(MainActivity.this, r);
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() { renderRulesPage(); }
                            });
                        }
                    }).start();
                }
            });
            bar.addView(btnTrigger);

            bar.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));

            TextView btnDel = new TextView(this);
            btnDel.setText("删除");
            btnDel.setTextSize(13);
            btnDel.setTextColor(theme.textSecondary);
            btnDel.setPadding(Md3e.dp(this, 12), Md3e.dp(this, 8), Md3e.dp(this, 12), Md3e.dp(this, 8));
            btnDel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Md3e.haptic(v);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            ActionExecutor.deactivate(MainActivity.this, r);
                            RuleStore.delete(MainActivity.this, r.id);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() { renderRulesPage(); }
                            });
                        }
                    }).start();
                }
            });
            bar.addView(btnDel);

            card.addView(bar, blp);
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Md3e.haptic(v);
                    RuleEditorDialog.show(MainActivity.this, r, new RuleEditorDialog.OnSavedListener() {
                        @Override
                        public void onSaved() { renderRulesPage(); }
                    });
                }
            });

            pageContainer.addView(card, clp);
            Md3e.popIn(card, i * 45L);
        }
    }

    private void renderConsolePage() {
        LinearLayout cleanCard = new LinearLayout(this);
        cleanCard.setOrientation(LinearLayout.VERTICAL);
        cleanCard.setBackground(Md3e.card(this, theme.primaryContainer, 26));
        int p = Md3e.dp(this, 20);
        cleanCard.setPadding(p, p, p, p);

        TextView t1 = new TextView(this);
        t1.setText("一键深度后台净化");
        t1.setTextSize(19);
        t1.setTypeface(null, Typeface.BOLD);
        t1.setTextColor(theme.onPrimaryContainer);
        cleanCard.addView(t1);

        TextView t2 = new TextView(this);
        t2.setText("强行停止所有第三方后台进程并释放系统页面缓存");
        t2.setTextSize(13);
        t2.setTextColor(theme.onPrimaryContainer);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(-1, -2);
        lp2.topMargin = Md3e.dp(this, 6);
        cleanCard.addView(t2, lp2);

        TextView btnClean = new TextView(this);
        btnClean.setText("立即执行净化");
        btnClean.setGravity(Gravity.CENTER);
        btnClean.setTextColor(theme.onPrimary);
        btnClean.setTypeface(null, Typeface.BOLD);
        btnClean.setBackground(Md3e.ripple(this, theme.primary, 0x33FFFFFF, 50));
        btnClean.setPadding(0, Md3e.dp(this, 13), 0, Md3e.dp(this, 13));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, -2);
        blp.topMargin = Md3e.dp(this, 14);
        cleanCard.addView(btnClean, blp);
        Md3e.attachSpringTouch(btnClean);

        btnClean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "正在净化后台...", Toast.LENGTH_SHORT).show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ActionExecutor.cleanAllBackground(MainActivity.this);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Md3e.vibratePulse(MainActivity.this, 45);
                                Toast.makeText(MainActivity.this, "后台净化完成", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }).start();
            }
        });
        pageContainer.addView(cleanCard);
        Md3e.popIn(cleanCard, 0);

        LinearLayout shellCard = new LinearLayout(this);
        shellCard.setOrientation(LinearLayout.VERTICAL);
        shellCard.setBackground(Md3e.card(this, theme.surfaceLow, 26));
        shellCard.setPadding(p, p, p, p);
        LinearLayout.LayoutParams sclp = new LinearLayout.LayoutParams(-1, -2);
        sclp.topMargin = Md3e.dp(this, 14);

        TextView st1 = new TextView(this);
        st1.setText("快捷 Root Shell 终端");
        st1.setTextSize(17);
        st1.setTypeface(null, Typeface.BOLD);
        st1.setTextColor(theme.textPrimary);
        shellCard.addView(st1);

        final EditText etCmd = new EditText(this);
        etCmd.setHint("输入 su 命令，例如: dumpsys battery");
        etCmd.setHintTextColor(theme.textSecondary);
        etCmd.setTextColor(theme.textPrimary);
        etCmd.setTextSize(13);
        etCmd.setBackground(Md3e.card(this, theme.surfaceHigh, 16));
        int ep = Md3e.dp(this, 12);
        etCmd.setPadding(ep, ep, ep, ep);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(-1, -2);
        elp.topMargin = Md3e.dp(this, 10);
        shellCard.addView(etCmd, elp);

        final TextView tvOut = new TextView(this);
        tvOut.setText("等待执行指令...");
        tvOut.setTextSize(12);
        tvOut.setTypeface(Typeface.MONOSPACE);
        tvOut.setTextColor(theme.textSecondary);
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(-1, -2);
        olp.topMargin = Md3e.dp(this, 10);

        TextView btnRun = new TextView(this);
        btnRun.setText("运行命令");
        btnRun.setGravity(Gravity.CENTER);
        btnRun.setTextColor(theme.onSecondaryContainer);
        btnRun.setTypeface(null, Typeface.BOLD);
        btnRun.setBackground(Md3e.ripple(this, theme.secondaryContainer, 0x22000000, 50));
        btnRun.setPadding(0, Md3e.dp(this, 12), 0, Md3e.dp(this, 12));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
        rlp.topMargin = Md3e.dp(this, 10);
        shellCard.addView(btnRun, rlp);
        shellCard.addView(tvOut, olp);
        Md3e.attachSpringTouch(btnRun);

        btnRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String c = etCmd.getText().toString().trim();
                if (c.isEmpty()) return;
                tvOut.setText("执行中...");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String res = RootShell.exec(c);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvOut.setText(res.isEmpty() ? "(无输出 / 执行成功)" : res);
                            }
                        });
                    }
                }).start();
            }
        });

        pageContainer.addView(shellCard, sclp);
        Md3e.popIn(shellCard, 60);
    }

    private void renderSettingsPage() {
        int p = Md3e.dp(this, 20);

        // 1. 进阶模式开关
        LinearLayout advCard = new LinearLayout(this);
        advCard.setOrientation(LinearLayout.VERTICAL);
        advCard.setBackground(Md3e.card(this, theme.surfaceLow, 26));
        advCard.setPadding(p, p, p, p);

        LinearLayout advRow = new LinearLayout(this);
        advRow.setOrientation(LinearLayout.HORIZONTAL);
        advRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout advTexts = new LinearLayout(this);
        advTexts.setOrientation(LinearLayout.VERTICAL);

        TextView advTitle = new TextView(this);
        advTitle.setText("进阶模式");
        advTitle.setTextSize(18);
        advTitle.setTypeface(null, Typeface.BOLD);
        advTitle.setTextColor(theme.textPrimary);
        advTexts.addView(advTitle);

        TextView advDesc = new TextView(this);
        advDesc.setText("开启后在规则编辑中解锁多应用共用情境、指定 Wi-Fi / 蓝牙触发及多条件 AND 组合");
        advDesc.setTextSize(13);
        advDesc.setTextColor(theme.textSecondary);
        LinearLayout.LayoutParams adlp = new LinearLayout.LayoutParams(-1, -2);
        adlp.topMargin = Md3e.dp(this, 4);
        advTexts.addView(advDesc, adlp);

        advRow.addView(advTexts, new LinearLayout.LayoutParams(0, -2, 1f));

        final Switch swAdv = new Switch(this);
        swAdv.setChecked(RuleStore.isAdvancedMode(this));
        LinearLayout.LayoutParams swlp = new LinearLayout.LayoutParams(-2, -2);
        swlp.leftMargin = Md3e.dp(this, 12);
        advRow.addView(swAdv, swlp);

        swAdv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Md3e.haptic(v);
                boolean on = swAdv.isChecked();
                RuleStore.setAdvancedMode(MainActivity.this, on);
                AppLogger.log("SETTINGS", "进阶模式已" + (on ? "开启" : "关闭"));
                Toast.makeText(MainActivity.this, on ? "已开启进阶模式" : "已关闭进阶模式", Toast.LENGTH_SHORT).show();
            }
        });

        advCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swAdv.performClick();
            }
        });

        advCard.addView(advRow);
        pageContainer.addView(advCard);
        Md3e.popIn(advCard, 0);

        // 2. 配置导入导出与日志导出卡片
        LinearLayout logCard = new LinearLayout(this);
        logCard.setOrientation(LinearLayout.VERTICAL);
        logCard.setBackground(Md3e.card(this, theme.surfaceLow, 26));
        logCard.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lclp = new LinearLayout.LayoutParams(-1, -2);
        lclp.topMargin = Md3e.dp(this, 14);

        TextView logTitle = new TextView(this);
        logTitle.setText("配置备份与运行日志");
        logTitle.setTextSize(18);
        logTitle.setTypeface(null, Typeface.BOLD);
        logTitle.setTextColor(theme.textPrimary);
        logCard.addView(logTitle);

        TextView logDesc = new TextView(this);
        logDesc.setText("配置文件与运行日志统一保存在 /storage/emulated/0/Download/FluxCore/");
        logDesc.setTextSize(13);
        logDesc.setTextColor(theme.textSecondary);
        LinearLayout.LayoutParams ldlp = new LinearLayout.LayoutParams(-1, -2);
        ldlp.topMargin = Md3e.dp(this, 4);
        logCard.addView(logDesc, ldlp);

        LinearLayout cfgRow = new LinearLayout(this);
        cfgRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams crlp = new LinearLayout.LayoutParams(-1, -2);
        crlp.topMargin = Md3e.dp(this, 14);

        TextView btnExportCfg = new TextView(this);
        btnExportCfg.setText("导出配置 (.json)");
        btnExportCfg.setGravity(Gravity.CENTER);
        btnExportCfg.setTextColor(theme.onSecondaryContainer);
        btnExportCfg.setTypeface(null, Typeface.BOLD);
        btnExportCfg.setBackground(Md3e.ripple(this, theme.secondaryContainer, 0x22000000, 50));
        btnExportCfg.setPadding(0, Md3e.dp(this, 12), 0, Md3e.dp(this, 12));
        LinearLayout.LayoutParams eclp = new LinearLayout.LayoutParams(0, -2, 1f);
        eclp.rightMargin = Md3e.dp(this, 8);
        cfgRow.addView(btnExportCfg, eclp);
        Md3e.attachSpringTouch(btnExportCfg);

        btnExportCfg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String path = RuleStore.exportConfig(MainActivity.this);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this,
                                        path != null ? "配置已导出: " + path : "配置导出失败",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }).start();
            }
        });

        TextView btnImportCfg = new TextView(this);
        btnImportCfg.setText("导入配置 (.json)");
        btnImportCfg.setGravity(Gravity.CENTER);
        btnImportCfg.setTextColor(theme.onSecondaryContainer);
        btnImportCfg.setTypeface(null, Typeface.BOLD);
        btnImportCfg.setBackground(Md3e.ripple(this, theme.secondaryContainer, 0x22000000, 50));
        btnImportCfg.setPadding(0, Md3e.dp(this, 12), 0, Md3e.dp(this, 12));
        cfgRow.addView(btnImportCfg, new LinearLayout.LayoutParams(0, -2, 1f));
        Md3e.attachSpringTouch(btnImportCfg);

        btnImportCfg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImportConfigDialog();
            }
        });

        logCard.addView(cfgRow, crlp);

        TextView btnExport = new TextView(this);
        btnExport.setText("导出运行日志 (.txt)");
        btnExport.setGravity(Gravity.CENTER);
        btnExport.setTextColor(theme.onSecondaryContainer);
        btnExport.setTypeface(null, Typeface.BOLD);
        btnExport.setBackground(Md3e.ripple(this, theme.secondaryContainer, 0x22000000, 50));
        btnExport.setPadding(0, Md3e.dp(this, 12), 0, Md3e.dp(this, 12));
        LinearLayout.LayoutParams belp = new LinearLayout.LayoutParams(-1, -2);
        belp.topMargin = Md3e.dp(this, 10);
        logCard.addView(btnExport, belp);
        Md3e.attachSpringTouch(btnExport);

        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String path = AppLogger.exportToDownload(MainActivity.this);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this,
                                        path != null ? "日志已导出: " + path : "日志导出失败",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }).start();
            }
        });

        pageContainer.addView(logCard, lclp);
        Md3e.popIn(logCard, 45);

        // 3. 关于
        LinearLayout aboutCard = new LinearLayout(this);
        aboutCard.setOrientation(LinearLayout.VERTICAL);
        aboutCard.setBackground(Md3e.card(this, theme.surfaceLow, 26));
        aboutCard.setPadding(p, p, p, p);
        LinearLayout.LayoutParams aclp = new LinearLayout.LayoutParams(-1, -2);
        aclp.topMargin = Md3e.dp(this, 14);

        TextView aboutTitle = new TextView(this);
        aboutTitle.setText("关于 · 流枢 FluxCore");
        aboutTitle.setTextSize(18);
        aboutTitle.setTypeface(null, Typeface.BOLD);
        aboutTitle.setTextColor(theme.textPrimary);
        aboutCard.addView(aboutTitle);

        TextView aboutBody = new TextView(this);
        aboutBody.setText("v1.2.0 · 原生 Root 事件驱动自动化与系统控制中枢\n" +
                "GitHub: https://github.com/MOUAJMD\n" +
                "酷安开发者账号: @C_AJMD_W");
        aboutBody.setTextSize(13);
        aboutBody.setLineSpacing(0, 1.35f);
        aboutBody.setTextColor(theme.textSecondary);
        LinearLayout.LayoutParams ablp = new LinearLayout.LayoutParams(-1, -2);
        ablp.topMargin = Md3e.dp(this, 8);
        aboutCard.addView(aboutBody, ablp);

        TextView btnGh = new TextView(this);
        btnGh.setText("打开 GitHub 主页 ↗");
        btnGh.setGravity(Gravity.CENTER);
        btnGh.setTextColor(theme.onSecondaryContainer);
        btnGh.setTypeface(null, Typeface.BOLD);
        btnGh.setBackground(Md3e.ripple(this, theme.secondaryContainer, 0x22000000, 50));
        btnGh.setPadding(0, Md3e.dp(this, 12), 0, Md3e.dp(this, 12));
        LinearLayout.LayoutParams ghlp = new LinearLayout.LayoutParams(-1, -2);
        ghlp.topMargin = Md3e.dp(this, 12);
        aboutCard.addView(btnGh, ghlp);
        Md3e.attachSpringTouch(btnGh);
        btnGh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/MOUAJMD/FluxCore_for_Android")));
                } catch (Throwable ignored) {}
            }
        });

        TextView btnCool = new TextView(this);
        btnCool.setText("复制酷安 ID 并跳转");
        btnCool.setGravity(Gravity.CENTER);
        btnCool.setTextColor(theme.onSecondaryContainer);
        btnCool.setTypeface(null, Typeface.BOLD);
        btnCool.setBackground(Md3e.ripple(this, theme.secondaryContainer, 0x22000000, 50));
        btnCool.setPadding(0, Md3e.dp(this, 12), 0, Md3e.dp(this, 12));
        LinearLayout.LayoutParams clp2 = new LinearLayout.LayoutParams(-1, -2);
        clp2.topMargin = Md3e.dp(this, 8);
        aboutCard.addView(btnCool, clp2);
        Md3e.attachSpringTouch(btnCool);
        btnCool.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("coolapk_id", "@C_AJMD_W"));
                }
                Toast.makeText(MainActivity.this, "已复制 @C_AJMD_W", Toast.LENGTH_SHORT).show();
                try {
                    Intent i = getPackageManager().getLaunchIntentForPackage("com.coolapk.market");
                    if (i != null) startActivity(i);
                } catch (Throwable ignored) {}
            }
        });

        pageContainer.addView(aboutCard, aclp);
        Md3e.popIn(aboutCard, 90);
    }

    private void showImportConfigDialog() {
        File dir = new File("/storage/emulated/0/Download/FluxCore");
        List<File> jsonFiles = new ArrayList<>();
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                Arrays.sort(files, new Comparator<File>() {
                    @Override public int compare(File a, File b) { return Long.compare(b.lastModified(), a.lastModified()); }
                });
                for (File f : files) {
                    if (f.isFile() && f.getName().endsWith(".json")) jsonFiles.add(f);
                }
            }
        }
        if (jsonFiles.isEmpty()) {
            Toast.makeText(this, "未在 Download/FluxCore/ 找到 .json 配置文件", Toast.LENGTH_LONG).show();
            return;
        }

        final Dialog dlg = new Dialog(this);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Md3e.card(this, theme.bg, 26));
        int p = Md3e.dp(this, 20);
        box.setPadding(p, p, p, p);

        TextView title = new TextView(this);
        title.setText("选择要导入的配置文件");
        title.setTextSize(19);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(theme.textPrimary);
        box.addView(title);

        final Switch swOverwrite = new Switch(this);
        swOverwrite.setText("覆盖现有全部情境（关闭则为合并追加）");
        swOverwrite.setTextColor(theme.textSecondary);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, -2);
        slp.topMargin = Md3e.dp(this, 10);
        slp.bottomMargin = Md3e.dp(this, 10);
        box.addView(swOverwrite, slp);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        box.addView(scroll, new LinearLayout.LayoutParams(-1, Md3e.dp(this, 260)));

        for (final File f : jsonFiles) {
            TextView item = new TextView(this);
            item.setText(f.getName());
            item.setTextSize(13);
            item.setTypeface(null, Typeface.BOLD);
            item.setTextColor(theme.onSecondaryContainer);
            item.setBackground(Md3e.ripple(this, theme.secondaryContainer, 0x22000000, 16));
            int ip = Md3e.dp(this, 14);
            item.setPadding(ip, ip, ip, ip);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(-1, -2);
            ilp.bottomMargin = Md3e.dp(this, 8);
            Md3e.attachSpringTouch(item);
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int count = RuleStore.importConfigFromFile(MainActivity.this, f, swOverwrite.isChecked());
                    dlg.dismiss();
                    if (count >= 0) {
                        Toast.makeText(MainActivity.this, "成功导入 " + count + " 条情境规则", Toast.LENGTH_SHORT).show();
                        renderTab();
                    } else {
                        Toast.makeText(MainActivity.this, "配置文件解析失败", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            list.addView(item, ilp);
        }

        dlg.setContentView(box);
        if (dlg.getWindow() != null) {
            dlg.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dlg.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.90), -2);
        }
        dlg.show();
    }
}
