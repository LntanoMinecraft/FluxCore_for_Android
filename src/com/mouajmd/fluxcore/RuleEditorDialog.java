package com.mouajmd.fluxcore;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RuleEditorDialog {
    public interface OnSavedListener {
        void onSaved();
    }

    private interface OnSelectIndex {
        void onSelect(int index);
    }

    private static Rule cloneRule(Rule src) {
        if (src == null) return new Rule();
        try {
            return Rule.fromJson(src.toJson());
        } catch (Exception e) {
            return src;
        }
    }

    public static void show(final Context ctx, Rule existing, final OnSavedListener onSaved) {
        final Rule r = cloneRule(existing);
        final Md3e t = Md3e.of(ctx);
        final boolean adv = RuleStore.isAdvancedMode(ctx);

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
        LinearLayout.LayoutParams sclp = new LinearLayout.LayoutParams(-1, Md3e.dp(ctx, 480));
        sclp.topMargin = Md3e.dp(ctx, 12);
        outer.addView(scroll, sclp);

        LinearLayout form = new LinearLayout(ctx);
        form.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(form);

        final EditText etName = input(ctx, t, "未命名情景", existing == null && "未命名情境".equals(r.name) ? "" : r.name);
        form.addView(etName);

        form.addView(section(ctx, t, adv ? "1. 主触发条件 (IF · 进阶模式)" : "1. 触发条件 (IF)"));

        final String[] trigNames = adv
                ? new String[]{"仅手动触发", "打开/退出应用（多选）", "充电器连接", "亮屏/息屏", "连接指定 Wi-Fi", "连接指定蓝牙", "电量阈值", "有线耳机插拔", "屏幕横竖屏", "时间段触发"}
                : new String[]{"仅手动触发", "打开/退出应用", "充电器连接", "亮屏/息屏", "Wi-Fi连接", "蓝牙设备", "电量阈值", "有线耳机插拔", "屏幕横竖屏", "时间段触发"};

        final TextView btnTrigApps = pillSelector(ctx, t, "");

        final LinearLayout targetBox = new LinearLayout(ctx);
        targetBox.setOrientation(LinearLayout.HORIZONTAL);
        targetBox.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams tblp = new LinearLayout.LayoutParams(-1, -2);
        tblp.bottomMargin = Md3e.dp(ctx, 8);

        final EditText etTrigTarget = new EditText(ctx);
        styleEditText(ctx, t, etTrigTarget);
        etTrigTarget.setTextSize(13);
        etTrigTarget.setText(r.triggerTarget);
        etTrigTarget.setBackground(Md3e.card(ctx, t.surfaceHigh, 16));
        int ep = Md3e.dp(ctx, 12);
        etTrigTarget.setPadding(ep, ep, ep, ep);
        targetBox.addView(etTrigTarget, new LinearLayout.LayoutParams(0, -2, 1f));

        final TextView btnFetchCurrent = new TextView(ctx);
        btnFetchCurrent.setText("读取当前");
        btnFetchCurrent.setTextSize(12);
        btnFetchCurrent.setTypeface(null, Typeface.BOLD);
        btnFetchCurrent.setTextColor(t.onPrimaryContainer);
        btnFetchCurrent.setBackground(Md3e.ripple(ctx, t.primaryContainer, 0x22000000, 14));
        btnFetchCurrent.setPadding(Md3e.dp(ctx, 12), Md3e.dp(ctx, 12), Md3e.dp(ctx, 12), Md3e.dp(ctx, 12));
        LinearLayout.LayoutParams fclp = new LinearLayout.LayoutParams(-2, -2);
        fclp.leftMargin = Md3e.dp(ctx, 8);
        targetBox.addView(btnFetchCurrent, fclp);
        Md3e.attachSpringTouch(btnFetchCurrent);

        btnFetchCurrent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (r.triggerType == Rule.TRIG_WIFI) {
                    String ssid = RootShell.queryCurrentWifiSsid();
                    if (!ssid.isEmpty()) {
                        etTrigTarget.setText(ssid);
                        Toast.makeText(ctx, "已填入当前 Wi-Fi: " + ssid, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ctx, "未检测到已连接的 Wi-Fi", Toast.LENGTH_SHORT).show();
                    }
                } else if (r.triggerType == Rule.TRIG_BT) {
                    String dump = RootShell.queryConnectedBtDevices();
                    Toast.makeText(ctx, dump.isEmpty() ? "未检测到已连蓝牙" : "已读取蓝牙状态，请填入设备名称关键词", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 电量阈值滑块区
        final LinearLayout batBox = new LinearLayout(ctx);
        batBox.setOrientation(LinearLayout.VERTICAL);
        final TextView tvBatVal = new TextView(ctx);
        tvBatVal.setText("电量阈值：" + r.triggerThreshold + "%");
        tvBatVal.setTextColor(t.textSecondary);
        tvBatVal.setTextSize(13);
        batBox.addView(tvBatVal);
        SeekBar sbBat = new SeekBar(ctx);
        sbBat.setMax(99);
        sbBat.setProgress(Math.max(0, Math.min(99, r.triggerThreshold - 1)));
        sbBat.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                r.triggerThreshold = progress + 1;
                tvBatVal.setText("电量阈值：" + r.triggerThreshold + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        batBox.addView(sbBat);

        final Switch swTrigOn = new Switch(ctx);
        swTrigOn.setTextColor(t.textPrimary);
        swTrigOn.setChecked(r.triggerOn);

        final LinearLayout extraArea = new LinearLayout(ctx);
        extraArea.setOrientation(LinearLayout.VERTICAL);

        final Runnable updateTrigUi = new Runnable() {
            @Override
            public void run() {
                btnTrigApps.setVisibility(View.GONE);
                targetBox.setVisibility(View.GONE);
                batBox.setVisibility(View.GONE);
                swTrigOn.setVisibility(View.GONE);
                btnFetchCurrent.setVisibility(View.VISIBLE);

                if (r.triggerType == Rule.TRIG_MANUAL) {
                    extraArea.setVisibility(View.GONE);
                } else {
                    extraArea.setVisibility(adv ? View.VISIBLE : View.GONE);
                    if (r.triggerType == Rule.TRIG_APP) {
                        btnTrigApps.setVisibility(View.VISIBLE);
                        if (adv) {
                            btnTrigApps.setText(r.triggerPkgs.isEmpty()
                                    ? "点击选择目标应用（可多选共用此情境）..."
                                    : "已选触发应用：" + r.triggerPkgs.size() + " 个");
                            swTrigOn.setText(swTrigOn.isChecked() ? "条件：任意所选应用进入前台时" : "条件：全部所选应用退出前台时");
                        } else {
                            btnTrigApps.setText(r.triggerPkgs.isEmpty()
                                    ? "点击选择目标应用..."
                                    : "目标应用：" + r.triggerPkgs.get(0));
                            swTrigOn.setText(swTrigOn.isChecked() ? "条件：打开该应用时" : "条件：退出该应用时");
                        }
                        swTrigOn.setVisibility(View.VISIBLE);
                    } else if (r.triggerType == Rule.TRIG_WIFI || r.triggerType == Rule.TRIG_BT) {
                        targetBox.setVisibility(adv ? View.VISIBLE : View.GONE);
                        if (adv) {
                            etTrigTarget.setHint(r.triggerType == Rule.TRIG_WIFI
                                    ? "指定 Wi-Fi 名称（留空=任意 Wi-Fi）"
                                    : "指定蓝牙设备名（留空=任意蓝牙）");
                            swTrigOn.setText(swTrigOn.isChecked() ? "条件：已连接该设备/网络时" : "条件：断开该设备/网络时");
                        } else {
                            swTrigOn.setText(swTrigOn.isChecked() ? "条件：状态开启/连接时" : "条件：状态关闭/断开时");
                        }
                        swTrigOn.setVisibility(View.VISIBLE);
                    } else if (r.triggerType == Rule.TRIG_BATTERY) {
                        batBox.setVisibility(View.VISIBLE);
                        swTrigOn.setVisibility(View.VISIBLE);
                        swTrigOn.setText(swTrigOn.isChecked() ? "条件：电量高于或等于阈值时" : "条件：电量低于或等于阈值时");
                    } else if (r.triggerType == Rule.TRIG_HEADSET) {
                        swTrigOn.setVisibility(View.VISIBLE);
                        swTrigOn.setText(swTrigOn.isChecked() ? "条件：插入有线耳机时" : "条件：拔出有线耳机时");
                    } else if (r.triggerType == Rule.TRIG_ORIENTATION) {
                        swTrigOn.setVisibility(View.VISIBLE);
                        swTrigOn.setText(swTrigOn.isChecked() ? "条件：屏幕旋转为横屏时" : "条件：屏幕恢复为竖屏时");
                    } else if (r.triggerType == Rule.TRIG_TIME) {
                        targetBox.setVisibility(View.VISIBLE);
                        btnFetchCurrent.setVisibility(View.GONE);
                        etTrigTarget.setHint("输入时间段，例如: 23:00-07:00");
                    } else {
                        swTrigOn.setVisibility(View.VISIBLE);
                        swTrigOn.setText(swTrigOn.isChecked() ? "条件：状态开启/连接时" : "条件：状态关闭/断开时");
                    }
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

        form.addView(btnTrigApps);
        form.addView(targetBox, tblp);
        form.addView(batBox);
        form.addView(swTrigOn);

        swTrigOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Md3e.haptic(v);
                r.triggerOn = swTrigOn.isChecked();
                updateTrigUi.run();
            }
        });
        btnTrigApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppPickerDialog.show(ctx, adv ? "选择触发应用（可多选）" : "选择触发应用", adv, r.triggerPkgs, new AppPickerDialog.OnPickedListener() {
                    @Override
                    public void onPicked(List<String> packages) {
                        if (adv) {
                            r.triggerPkgs = new ArrayList<>(packages);
                        } else if (!packages.isEmpty()) {
                            r.triggerPkgs = new ArrayList<>(Collections.singletonList(packages.get(0)));
                        }
                        updateTrigUi.run();
                    }
                });
            }
        });

        // 附加 AND 条件区
        final LinearLayout extraListBox = new LinearLayout(ctx);
        extraListBox.setOrientation(LinearLayout.VERTICAL);

        final Runnable renderExtraConditions = new Runnable() {
            @Override
            public void run() {
                extraListBox.removeAllViews();
                for (int i = 0; i < r.extraConditions.size(); i++) {
                    final int idx = i;
                    final Rule.Condition c = r.extraConditions.get(i);
                    LinearLayout row = new LinearLayout(ctx);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setBackground(Md3e.card(ctx, t.surfaceLow, 16));
                    int rp = Md3e.dp(ctx, 10);
                    row.setPadding(rp, rp, rp, rp);
                    LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
                    rlp.bottomMargin = Md3e.dp(ctx, 8);

                    TextView tvDesc = new TextView(ctx);
                    tvDesc.setText("且 · " + c.describe());
                    tvDesc.setTextColor(t.textPrimary);
                    tvDesc.setTextSize(13);
                    tvDesc.setTypeface(null, Typeface.BOLD);
                    row.addView(tvDesc, new LinearLayout.LayoutParams(0, -2, 1f));

                    TextView btnDel = new TextView(ctx);
                    btnDel.setText("移除");
                    btnDel.setTextSize(12);
                    btnDel.setTextColor(t.primary);
                    btnDel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Md3e.haptic(v);
                            r.extraConditions.remove(idx);
                            run();
                        }
                    });
                    row.addView(btnDel);
                    extraListBox.addView(row, rlp);
                }
            }
        };
        renderExtraConditions.run();

        TextView btnAddCond = pillSelector(ctx, t, "+ 添加附加条件（全部同时满足才触发）");
        btnAddCond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddConditionDialog(ctx, t, new OnConditionAdded() {
                    @Override
                    public void onAdded(Rule.Condition c) {
                        r.extraConditions.add(c);
                        renderExtraConditions.run();
                    }
                });
            }
        });
        extraArea.addView(extraListBox);
        extraArea.addView(btnAddCond);
        form.addView(extraArea);
        updateTrigUi.run();

        form.addView(section(ctx, t, "2. 执行动作 (THEN)"));

        // 状态栏磁贴选择
        final TextView btnPickTile = pillSelector(ctx, t,
                r.tileComponent.isEmpty() ? "切换状态栏磁贴：未选择" : "切换磁贴：" + (r.tileLabel.isEmpty() ? r.tileComponent : r.tileLabel));
        form.addView(btnPickTile);
        btnPickTile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TilePickerDialog.show(ctx, r.tileComponent, new TilePickerDialog.OnTilePickedListener() {
                    @Override
                    public void onPicked(String component, String label) {
                        r.tileComponent = component;
                        r.tileLabel = label;
                        btnPickTile.setText(r.tileComponent.isEmpty()
                                ? "切换状态栏磁贴：未选择"
                                : "切换磁贴：" + (r.tileLabel.isEmpty() ? r.tileComponent : r.tileLabel));
                    }
                });
            }
        });

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
            @Override public void onSelect(int index) { r.dndMode = index - 1; }
        });

        final int[] hzOptions = {0, 60, 90, 120};
        final String[] hzLabels = {"不改变", "60Hz", "90Hz", "120Hz"};
        int hzIdx = 0;
        for (int i = 0; i < hzOptions.length; i++) if (hzOptions[i] == r.refreshRate) hzIdx = i;
        addExpandableSelector(ctx, t, form, "屏幕刷新率", hzLabels, hzIdx, new OnSelectIndex() {
            @Override public void onSelect(int index) { r.refreshRate = hzOptions[index]; }
        });

        final String[] wifiLabels = {"不改变", "开启 Wi-Fi", "关闭 Wi-Fi"};
        int wifiIdx = r.wifiAction == -1 ? 0 : (r.wifiAction == 1 ? 1 : 2);
        addExpandableSelector(ctx, t, form, "Wi-Fi 控制", wifiLabels, wifiIdx, new OnSelectIndex() {
            @Override public void onSelect(int index) { r.wifiAction = index == 0 ? -1 : (index == 1 ? 1 : 0); }
        });

        final String[] dataLabels = {"不改变", "开启移动数据", "关闭移动数据"};
        int dataIdx = r.mobileDataAction == -1 ? 0 : (r.mobileDataAction == 1 ? 1 : 2);
        addExpandableSelector(ctx, t, form, "移动数据", dataLabels, dataIdx, new OnSelectIndex() {
            @Override public void onSelect(int index) { r.mobileDataAction = index == 0 ? -1 : (index == 1 ? 1 : 0); }
        });

        final String[] airLabels = {"不改变", "开启飞行模式", "关闭飞行模式"};
        int airIdx = r.airplaneAction == -1 ? 0 : (r.airplaneAction == 1 ? 1 : 2);
        addExpandableSelector(ctx, t, form, "飞行模式", airLabels, airIdx, new OnSelectIndex() {
            @Override public void onSelect(int index) { r.airplaneAction = index == 0 ? -1 : (index == 1 ? 1 : 0); }
        });

        final String[] batSaveLabels = {"不改变", "开启系统省电", "关闭系统省电"};
        int batSaveIdx = r.batterySaverAction == -1 ? 0 : (r.batterySaverAction == 1 ? 1 : 2);
        addExpandableSelector(ctx, t, form, "系统省电", batSaveLabels, batSaveIdx, new OnSelectIndex() {
            @Override public void onSelect(int index) { r.batterySaverAction = index == 0 ? -1 : (index == 1 ? 1 : 0); }
        });

        final String[] darkLabels = {"不改变", "切换深色模式", "切换浅色模式"};
        int darkIdx = r.darkModeAction == -1 ? 0 : (r.darkModeAction == 1 ? 1 : 2);
        addExpandableSelector(ctx, t, form, "深色主题", darkLabels, darkIdx, new OnSelectIndex() {
            @Override public void onSelect(int index) { r.darkModeAction = index == 0 ? -1 : (index == 1 ? 1 : 0); }
        });

        final String[] rotLabels = {"不改变", "开启自动旋转", "锁定竖屏方向"};
        int rotIdx = r.autoRotateAction == -1 ? 0 : (r.autoRotateAction == 1 ? 1 : 2);
        addExpandableSelector(ctx, t, form, "屏幕旋转", rotLabels, rotIdx, new OnSelectIndex() {
            @Override public void onSelect(int index) { r.autoRotateAction = index == 0 ? -1 : (index == 1 ? 1 : 0); }
        });

        // 媒体音量滑块
        final TextView tvVol = new TextView(ctx);
        tvVol.setText(r.mediaVolume < 0 ? "媒体音量：不改变" : "媒体音量：" + r.mediaVolume + "%");
        tvVol.setTextColor(t.textSecondary);
        tvVol.setTextSize(13);
        form.addView(tvVol);

        SeekBar sbVol = new SeekBar(ctx);
        sbVol.setMax(101);
        sbVol.setProgress(r.mediaVolume < 0 ? 0 : r.mediaVolume + 1);
        sbVol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0) {
                    r.mediaVolume = -1;
                    tvVol.setText("媒体音量：不改变");
                } else {
                    r.mediaVolume = progress - 1;
                    tvVol.setText("媒体音量：" + r.mediaVolume + "%");
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        form.addView(sbVol);

        // 屏幕亮度滑块
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

        final Switch swVib = new Switch(ctx);
        swVib.setText("触发情境时短震反馈提示");
        swVib.setTextColor(t.textPrimary);
        swVib.setChecked(r.vibrateTrigger);
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(-1, -2);
        vlp.topMargin = Md3e.dp(ctx, 8);
        vlp.bottomMargin = Md3e.dp(ctx, 8);
        form.addView(swVib, vlp);

        final EditText etCmd = input(ctx, t, "自定义 Root Shell 指令（可选）", r.customCmd);
        form.addView(etCmd);

        final EditText etRevertCmd = input(ctx, t, "退出还原 Shell 指令（可选）", r.revertCmd);
        form.addView(etRevertCmd);

        final Switch swRevert = new Switch(ctx);
        swRevert.setText("退出条件或再次点击时自动还原系统状态（含磁贴）");
        swRevert.setTextColor(t.textPrimary);
        swRevert.setChecked(r.autoRevert);
        LinearLayout.LayoutParams rvlp = new LinearLayout.LayoutParams(-1, -2);
        rvlp.topMargin = Md3e.dp(ctx, 8);
        form.addView(swRevert, rvlp);

        LinearLayout actions = new LinearLayout(ctx);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(-1, -2);
        alp.topMargin = Md3e.dp(ctx, 16);

        TextView btnCancel = new TextView(ctx);
        btnCancel.setText("取消");
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setTextColor(t.textPrimary);
        btnCancel.setBackground(Md3e.ripple(ctx, t.surfaceHigh, 0x22888888, 50));
        btnCancel.setPadding(0, Md3e.dp(ctx, 14), 0, Md3e.dp(ctx, 14));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, -2, 1f);
        clp.rightMargin = Md3e.dp(ctx, 8);
        actions.addView(btnCancel, clp);

        final TextView btnSave = new TextView(ctx);
        btnSave.setText("保存情境");
        btnSave.setGravity(Gravity.CENTER);
        btnSave.setTextColor(t.onPrimary);
        btnSave.setTypeface(null, Typeface.BOLD);
        btnSave.setBackground(Md3e.ripple(ctx, t.primary, 0x33FFFFFF, 50));
        btnSave.setPadding(0, Md3e.dp(ctx, 14), 0, Md3e.dp(ctx, 14));
        LinearLayout.LayoutParams svlp = new LinearLayout.LayoutParams(0, -2, 1.5f);
        actions.addView(btnSave, svlp);

        outer.addView(actions, alp);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { Md3e.haptic(v); dlg.dismiss(); }
        });

        final boolean[] saving = {false};
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (saving[0]) return;
                saving[0] = true;
                btnSave.setEnabled(false);
                Md3e.haptic(v);

                String nm = etName.getText().toString().trim();
                r.name = nm.isEmpty() ? "未命名情境" : nm;
                r.triggerOn = swTrigOn.isChecked();
                if (adv || r.triggerType == Rule.TRIG_TIME) {
                    r.triggerTarget = etTrigTarget.getText().toString().trim();
                } else {
                    r.triggerTarget = "";
                }
                if (!adv) {
                    r.extraConditions.clear();
                }
                r.vibrateTrigger = swVib.isChecked();
                r.customCmd = etCmd.getText().toString();
                r.revertCmd = etRevertCmd.getText().toString();
                r.autoRevert = swRevert.isChecked();
                RuleStore.upsert(ctx, r);
                AppLogger.log("RULE", "保存情境规则: " + r.name + " [" + r.describeTrigger() + "]");
                dlg.dismiss();
                if (onSaved != null) onSaved.onSaved();
            }
        });

        dlg.setContentView(outer);
        if (dlg.getWindow() != null) {
            dlg.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dlg.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            dlg.getWindow().setLayout((int) (ctx.getResources().getDisplayMetrics().widthPixels * 0.94), -2);
        }
        dlg.show();
    }

    private interface OnConditionAdded {
        void onAdded(Rule.Condition c);
    }

    private static void showAddConditionDialog(final Context ctx, final Md3e t, final OnConditionAdded callback) {
        final Dialog sub = new Dialog(ctx);
        sub.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Md3e.card(ctx, t.bg, 24));
        int p = Md3e.dp(ctx, 18);
        box.setPadding(p, p, p, p);

        TextView title = new TextView(ctx);
        title.setText("添加附加 AND 条件");
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(t.textPrimary);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-1, -2);
        tlp.bottomMargin = Md3e.dp(ctx, 12);
        box.addView(title, tlp);

        final Rule.Condition cond = new Rule.Condition();
        cond.type = Rule.TRIG_CHARGING;
        cond.stateOn = true;

        final String[] cTypes = {"前台应用", "充电器状态", "屏幕亮灭", "指定 Wi-Fi", "指定蓝牙设备", "电量阈值", "有线耳机", "屏幕横竖屏", "时间段限制"};
        final int[] cValues = {Rule.TRIG_APP, Rule.TRIG_CHARGING, Rule.TRIG_SCREEN, Rule.TRIG_WIFI, Rule.TRIG_BT, Rule.TRIG_BATTERY, Rule.TRIG_HEADSET, Rule.TRIG_ORIENTATION, Rule.TRIG_TIME};

        final TextView btnApps = pillSelector(ctx, t, "选择应用（可多选）...");
        final EditText etTarget = input(ctx, t, "指定名称或时间段(如 23:00-07:00)", "");
        final TextView tvSubBat = new TextView(ctx);
        tvSubBat.setText("电量阈值：20%");
        tvSubBat.setTextColor(t.textSecondary);
        tvSubBat.setTextSize(13);
        final SeekBar sbSubBat = new SeekBar(ctx);
        sbSubBat.setMax(99);
        sbSubBat.setProgress(19);
        sbSubBat.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int prog, boolean u) { cond.threshold = prog + 1; tvSubBat.setText("电量阈值：" + cond.threshold + "%"); }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        final Switch swOn = new Switch(ctx);
        swOn.setText("条件要求：已开启 / 已连接 / 高于阈值");
        swOn.setTextColor(t.textPrimary);
        swOn.setChecked(true);

        final Runnable refreshSub = new Runnable() {
            @Override
            public void run() {
                btnApps.setVisibility(cond.type == Rule.TRIG_APP ? View.VISIBLE : View.GONE);
                etTarget.setVisibility((cond.type == Rule.TRIG_WIFI || cond.type == Rule.TRIG_BT || cond.type == Rule.TRIG_TIME) ? View.VISIBLE : View.GONE);
                tvSubBat.setVisibility(cond.type == Rule.TRIG_BATTERY ? View.VISIBLE : View.GONE);
                sbSubBat.setVisibility(cond.type == Rule.TRIG_BATTERY ? View.VISIBLE : View.GONE);
                swOn.setVisibility(cond.type == Rule.TRIG_TIME ? View.GONE : View.VISIBLE);
                if (cond.type == Rule.TRIG_APP) {
                    btnApps.setText(cond.pkgs.isEmpty() ? "点击选择应用（可多选）..." : "已选 " + cond.pkgs.size() + " 个应用");
                }
            }
        };

        addExpandableSelector(ctx, t, box, "条件类型", cTypes, 1, new OnSelectIndex() {
            @Override
            public void onSelect(int index) {
                cond.type = cValues[index];
                refreshSub.run();
            }
        });

        box.addView(btnApps);
        box.addView(etTarget);
        box.addView(tvSubBat);
        box.addView(sbSubBat);
        box.addView(swOn);
        refreshSub.run();

        btnApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppPickerDialog.show(ctx, "选择条件应用", true, cond.pkgs, new AppPickerDialog.OnPickedListener() {
                    @Override
                    public void onPicked(List<String> packages) {
                        cond.pkgs = new ArrayList<>(packages);
                        refreshSub.run();
                    }
                });
            }
        });

        TextView btnConfirm = new TextView(ctx);
        btnConfirm.setText("确认添加条件");
        btnConfirm.setGravity(Gravity.CENTER);
        btnConfirm.setTextColor(t.onPrimary);
        btnConfirm.setTypeface(null, Typeface.BOLD);
        btnConfirm.setBackground(Md3e.ripple(ctx, t.primary, 0x33FFFFFF, 50));
        btnConfirm.setPadding(0, Md3e.dp(ctx, 12), 0, Md3e.dp(ctx, 12));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.topMargin = Md3e.dp(ctx, 14);
        box.addView(btnConfirm, clp);

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Md3e.haptic(v);
                cond.stateOn = swOn.isChecked();
                cond.targetName = etTarget.getText().toString().trim();
                sub.dismiss();
                callback.onAdded(cond);
            }
        });

        sub.setContentView(box);
        if (sub.getWindow() != null) {
            sub.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            sub.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            sub.getWindow().setLayout((int) (ctx.getResources().getDisplayMetrics().widthPixels * 0.88), -2);
        }
        sub.show();
    }

    private static void styleEditText(Context ctx, Md3e t, EditText et) {
        et.setHintTextColor(t.textSecondary);
        et.setTextColor(t.textPrimary);
        et.setFocusable(true);
        et.setFocusableInTouchMode(true);
        et.setCursorVisible(true);
        if (Build.VERSION.SDK_INT >= 29) {
            GradientDrawable cursor = new GradientDrawable();
            cursor.setShape(GradientDrawable.RECTANGLE);
            cursor.setSize(Md3e.dp(ctx, 2), Md3e.dp(ctx, 18));
            cursor.setColor(t.primary);
            et.setTextCursorDrawable(cursor);
        }
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
        styleEditText(ctx, t, et);
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
