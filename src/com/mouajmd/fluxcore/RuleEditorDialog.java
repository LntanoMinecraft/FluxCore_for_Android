package com.mouajmd.fluxcore;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class RuleEditorDialog {
    public interface OnSavedListener {
        void onSaved();
    }

    private interface OnSelectIndex {
        void onSelect(int index);
    }

    public static void show(final Context ctx, Rule existing, final OnSavedListener onSaved) {
        final Rule r = existing != null ? existing : new Rule();
        final Md3e t = Md3e.of(ctx);

        final Dialog dlg = new Dialog(ctx);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout outer = new LinearLayout(ctx);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackground(Md3e.card(ctx, t.bg, 28));
        int p = Md3e.dp(ctx, 20);
        outer.setPadding(p, p, p, p);

        TextView header = new TextView(ctx);
        header.setText(existing == null ? "新建自动化情境" : "编辑情境规则");
        header.setTextSize(22);
        header.setTypeface(null, Typeface.BOLD);
        header.setTextColor(t.textPrimary);
        outer.addView(header);

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout.LayoutParams sclp = new LinearLayout.LayoutParams(-1, Md3e.dp(ctx, 460));
        sclp.topMargin = Md3e.dp(ctx, 12);
        outer.addView(scroll, sclp);

        LinearLayout form = new LinearLayout(ctx);
        form.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(form);

        final EditText etName = input(ctx, t, "情境名称（如：游戏沉浸模式）", r.name);
        form.addView(etName);

        form.addView(section(ctx, t, "1. 触发条件 (IF)"));

        final String[] trigNames = {"仅手动触发", "打开/退出应用", "充电器连接", "亮屏/息屏", "Wi-Fi连接", "蓝牙设备"};
        final TextView btnTrigDetail = pillSelector(ctx, t, "");
        final Switch swTrigOn = new Switch(ctx);
        swTrigOn.setText("正向触发（开启/进入时执行）");
        swTrigOn.setTextColor(t.textPrimary);
        swTrigOn.setChecked(r.triggerOn);

        final Runnable updateTrigUi = new Runnable() {
            @Override
            public void run() {
                if (r.triggerType == Rule.TRIG_MANUAL) {
                    btnTrigDetail.setVisibility(View.GONE);
                    swTrigOn.setVisibility(View.GONE);
                } else if (r.triggerType == Rule.TRIG_APP) {
                    btnTrigDetail.setVisibility(View.VISIBLE);
                    btnTrigDetail.setText(r.triggerPkg.isEmpty() ? "点击选择目标应用..." : "目标应用：" + r.triggerPkg);
                    swTrigOn.setVisibility(View.VISIBLE);
                    swTrigOn.setText(swTrigOn.isChecked() ? "条件：打开该应用时" : "条件：退出该应用时");
                } else {
                    btnTrigDetail.setVisibility(View.GONE);
                    swTrigOn.setVisibility(View.VISIBLE);
                    swTrigOn.setText(swTrigOn.isChecked() ? "条件：状态开启/连接时" : "条件：状态关闭/断开时");
                }
            }
        };

        addExpandableSelector(ctx, t, form, "触发类型", trigNames, r.triggerType, new OnSelectIndex() {
            @Override
            public void onSelect(int index) {
                r.triggerType = index;
                updateTrigUi.run();
            }
        });

        form.addView(btnTrigDetail);
        form.addView(swTrigOn);
        updateTrigUi.run();

        swTrigOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                r.triggerOn = swTrigOn.isChecked();
                updateTrigUi.run();
            }
        });
        btnTrigDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppPickerDialog.show(ctx, "选择触发应用", false, null, new AppPickerDialog.OnPickedListener() {
                    @Override
                    public void onPicked(List<String> packages) {
                        if (!packages.isEmpty()) {
                            r.triggerPkg = packages.get(0);
                            updateTrigUi.run();
                        }
                    }
                });
            }
        });

        form.addView(section(ctx, t, "2. 执行动作 (THEN)"));
        final TextView btnKillApps = pillSelector(ctx, t,
                r.killPkgs.isEmpty() ? "强行停止后台应用：未选择" : "强行停止应用：已选 " + r.killPkgs.size() + " 个");
        form.addView(btnKillApps);
        btnKillApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppPickerDialog.show(ctx, "选择要强行停止的应用", true, r.killPkgs, new AppPickerDialog.OnPickedListener() {
                    @Override
                    public void onPicked(List<String> packages) {
                        r.killPkgs = new ArrayList<>(packages);
                        btnKillApps.setText(r.killPkgs.isEmpty()
                                ? "强行停止后台应用：未选择"
                                : "强行停止应用：已选 " + r.killPkgs.size() + " 个");
                    }
                });
            }
        });

        final String[] dndLabels = {"不改变", "关闭勿扰", "仅允许重要打扰", "完全静音勿扰"};
        addExpandableSelector(ctx, t, form, "勿扰模式", dndLabels, r.dndMode + 1, new OnSelectIndex() {
            @Override
            public void onSelect(int index) {
                r.dndMode = index - 1;
            }
        });

        final int[] hzOptions = {0, 60, 90, 120};
        final String[] hzLabels = {"不改变", "60Hz", "90Hz", "120Hz"};
        int hzIdx = 0;
        for (int i = 0; i < hzOptions.length; i++) if (hzOptions[i] == r.refreshRate) hzIdx = i;
        addExpandableSelector(ctx, t, form, "屏幕刷新率", hzLabels, hzIdx, new OnSelectIndex() {
            @Override
            public void onSelect(int index) {
                r.refreshRate = hzOptions[index];
            }
        });

        final String[] wifiLabels = {"不改变", "开启 Wi-Fi", "关闭 Wi-Fi"};
        int wifiIdx = r.wifiAction == -1 ? 0 : (r.wifiAction == 1 ? 1 : 2);
        addExpandableSelector(ctx, t, form, "Wi-Fi 控制", wifiLabels, wifiIdx, new OnSelectIndex() {
            @Override
            public void onSelect(int index) {
                r.wifiAction = index == 0 ? -1 : (index == 1 ? 1 : 0);
            }
        });

        final TextView tvBright = new TextView(ctx);
        tvBright.setText(r.brightness < 0 ? "固定屏幕亮度：不改变" : "固定屏幕亮度：" + r.brightness);
        tvBright.setTextColor(t.textSecondary);
        tvBright.setTextSize(13);
        form.addView(tvBright);

        SeekBar sbBright = new SeekBar(ctx);
        sbBright.setMax(256);
        sbBright.setProgress(r.brightness < 0 ? 0 : r.brightness + 1);
        sbBright.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0) {
                    r.brightness = -1;
                    tvBright.setText("固定屏幕亮度：不改变");
                } else {
                    r.brightness = progress - 1;
                    tvBright.setText("固定屏幕亮度：" + r.brightness);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        form.addView(sbBright);

        final EditText etCmd = input(ctx, t, "自定义 Root Shell 指令（可选）", r.customCmd);
        form.addView(etCmd);

        final EditText etRevertCmd = input(ctx, t, "退出还原 Shell 指令（可选）", r.revertCmd);
        form.addView(etRevertCmd);

        final Switch swRevert = new Switch(ctx);
        swRevert.setText("退出条件或再次点击时自动还原系统状态");
        swRevert.setTextColor(t.textPrimary);
        swRevert.setChecked(r.autoRevert);
        LinearLayout.LayoutParams rvlp = new LinearLayout.LayoutParams(-1, -2);
        rvlp.topMargin = Md3e.dp(ctx, 10);
        form.addView(swRevert, rvlp);

        LinearLayout actions = new LinearLayout(ctx);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(-1, -2);
        alp.topMargin = Md3e.dp(ctx, 16);

        TextView btnCancel = new TextView(ctx);
        btnCancel.setText("取消");
        btnCancel.setGravity(android.view.Gravity.CENTER);
        btnCancel.setTextColor(t.textPrimary);
        btnCancel.setBackground(Md3e.ripple(ctx, t.surfaceHigh, 0x22888888, 50));
        btnCancel.setPadding(0, Md3e.dp(ctx, 14), 0, Md3e.dp(ctx, 14));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, -2, 1f);
        clp.rightMargin = Md3e.dp(ctx, 8);
        actions.addView(btnCancel, clp);

        TextView btnSave = new TextView(ctx);
        btnSave.setText("保存情境");
        btnSave.setGravity(android.view.Gravity.CENTER);
        btnSave.setTextColor(t.onPrimary);
        btnSave.setTypeface(null, Typeface.BOLD);
        btnSave.setBackground(Md3e.ripple(ctx, t.primary, 0x33FFFFFF, 50));
        btnSave.setPadding(0, Md3e.dp(ctx, 14), 0, Md3e.dp(ctx, 14));
        LinearLayout.LayoutParams svlp = new LinearLayout.LayoutParams(0, -2, 1.5f);
        actions.addView(btnSave, svlp);

        outer.addView(actions, alp);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { dlg.dismiss(); }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nm = etName.getText().toString().trim();
                r.name = nm.isEmpty() ? "未命名情境" : nm;
                r.triggerOn = swTrigOn.isChecked();
                r.customCmd = etCmd.getText().toString();
                r.revertCmd = etRevertCmd.getText().toString();
                r.autoRevert = swRevert.isChecked();
                RuleStore.upsert(ctx, r);
                dlg.dismiss();
                if (onSaved != null) onSaved.onSaved();
            }
        });

        dlg.setContentView(outer);
        if (dlg.getWindow() != null) {
            dlg.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dlg.getWindow().setLayout((int) (ctx.getResources().getDisplayMetrics().widthPixels * 0.94), -2);
        }
        dlg.show();
    }

    private static void addExpandableSelector(final Context ctx, final Md3e t, LinearLayout parent,
                                              final String prefix, final String[] options,
                                              int initialIndex, final OnSelectIndex listener) {
        final int[] curIdx = {Math.max(0, Math.min(options.length - 1, initialIndex))};
        final boolean[] expanded = {false};

        final TextView headerBtn = pillSelector(ctx, t, prefix + "：" + options[curIdx[0]] + "  ▾");
        parent.addView(headerBtn);

        final LinearLayout menuBox = new LinearLayout(ctx);
        menuBox.setOrientation(LinearLayout.VERTICAL);
        menuBox.setBackground(Md3e.card(ctx, t.surfaceLow, 20));
        int mp = Md3e.dp(ctx, 6);
        menuBox.setPadding(mp, mp, mp, mp);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(-1, -2);
        mlp.bottomMargin = Md3e.dp(ctx, 8);
        menuBox.setVisibility(View.GONE);
        parent.addView(menuBox, mlp);

        final TextView[] itemViews = new TextView[options.length];
        for (int i = 0; i < options.length; i++) {
            final int idx = i;
            TextView tv = new TextView(ctx);
            tv.setText(options[i]);
            tv.setTextSize(14);
            tv.setPadding(Md3e.dp(ctx, 14), Md3e.dp(ctx, 11), Md3e.dp(ctx, 14), Md3e.dp(ctx, 11));
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(-1, -2);
            if (i < options.length - 1) ilp.bottomMargin = Md3e.dp(ctx, 4);
            Md3e.attachSpringTouch(tv);
            itemViews[i] = tv;
            menuBox.addView(tv, ilp);

            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    curIdx[0] = idx;
                    expanded[0] = false;
                    menuBox.setVisibility(View.GONE);
                    headerBtn.setText(prefix + "：" + options[idx] + "  ▾");
                    refreshDropdownItems(ctx, t, itemViews, options, idx);
                    listener.onSelect(idx);
                }
            });
        }
        refreshDropdownItems(ctx, t, itemViews, options, curIdx[0]);

        headerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expanded[0] = !expanded[0];
                if (expanded[0]) {
                    headerBtn.setText(prefix + "：" + options[curIdx[0]] + "  ▴");
                    menuBox.setVisibility(View.VISIBLE);
                    Md3e.popIn(menuBox, 0);
                } else {
                    headerBtn.setText(prefix + "：" + options[curIdx[0]] + "  ▾");
                    menuBox.setVisibility(View.GONE);
                }
            }
        });
    }

    private static void refreshDropdownItems(Context ctx, Md3e t, TextView[] views, String[] options, int activeIdx) {
        for (int i = 0; i < views.length; i++) {
            if (i == activeIdx) {
                views[i].setText("✓  " + options[i]);
                views[i].setTypeface(null, Typeface.BOLD);
                views[i].setTextColor(t.onPrimaryContainer);
                views[i].setBackground(Md3e.ripple(ctx, t.primaryContainer, 0x22000000, 14));
            } else {
                views[i].setText("    " + options[i]);
                views[i].setTypeface(null, Typeface.NORMAL);
                views[i].setTextColor(t.textPrimary);
                views[i].setBackground(Md3e.ripple(ctx, Color.TRANSPARENT, 0x22888888, 14));
            }
        }
    }

    private static TextView section(Context ctx, Md3e t, String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(t.primary);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Md3e.dp(ctx, 14);
        lp.bottomMargin = Md3e.dp(ctx, 6);
        tv.setLayoutParams(lp);
        return tv;
    }

    private static EditText input(Context ctx, Md3e t, String hint, String val) {
        EditText et = new EditText(ctx);
        et.setHint(hint);
        et.setText(val);
        et.setHintTextColor(t.textSecondary);
        et.setTextColor(t.textPrimary);
        et.setTextSize(14);
        et.setBackground(Md3e.card(ctx, t.surfaceHigh, 16));
        int p = Md3e.dp(ctx, 12);
        et.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = Md3e.dp(ctx, 8);
        et.setLayoutParams(lp);
        return et;
    }

    private static TextView pillSelector(Context ctx, Md3e t, String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextColor(t.onSecondaryContainer);
        tv.setTextSize(14);
        tv.setBackground(Md3e.ripple(ctx, t.secondaryContainer, 0x22000000, 18));
        int p = Md3e.dp(ctx, 14);
        tv.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = Md3e.dp(ctx, 8);
        tv.setLayoutParams(lp);
        Md3e.attachSpringTouch(tv);
        return tv;
    }
}
