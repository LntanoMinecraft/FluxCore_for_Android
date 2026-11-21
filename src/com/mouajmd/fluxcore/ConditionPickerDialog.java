package com.mouajmd.fluxcore;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class ConditionPickerDialog {
    public interface OnAddedListener {
        void onAdded(Rule.Condition condition);
    }

    public static void show(final Context ctx, final OnAddedListener listener) {
        final Md3e t = Md3e.of(ctx);
        final Rule.Condition cond = new Rule.Condition();
        cond.type = Rule.TRIG_APP;
        cond.stateOn = true;

        final Dialog dlg = new Dialog(ctx);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(Md3e.card(ctx, t.bg, 28));
        int p = Md3e.dp(ctx, 20);
        root.setPadding(p, p, p, p);

        TextView title = new TextView(ctx);
        title.setText("添加同时满足条件 (AND)");
        title.setTextSize(19);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(t.textPrimary);
        root.addView(title);

        final int[] types = {
            Rule.TRIG_APP, Rule.TRIG_CHARGING, Rule.TRIG_SCREEN, Rule.TRIG_WIFI, Rule.TRIG_BT
        };
        final String[] labels = {
            "前台应用（支持多选）", "充电器状态", "屏幕亮/息状态", "Wi-Fi 连接（可指定名称）", "蓝牙设备（可指定设备）"
        };

        final TextView btnApps = AdvancedTriggerHelper.makePill(ctx, t, "点击选择目标应用（可多选）...");
        final EditText etTarget = AdvancedTriggerHelper.makeInput(ctx, t, "", "");
        final TextView btnDetect = AdvancedTriggerHelper.makePill(ctx, t, "");
        final Switch swState = new Switch(ctx);
        swState.setTextColor(t.textPrimary);
        swState.setChecked(true);

        final Runnable refreshUi = new Runnable() {
            @Override
            public void run() {
                btnApps.setVisibility(View.GONE);
                etTarget.setVisibility(View.GONE);
                btnDetect.setVisibility(View.GONE);

                if (cond.type == Rule.TRIG_APP) {
                    btnApps.setVisibility(View.VISIBLE);
                    btnApps.setText(cond.pkgs.isEmpty()
                            ? "点击选择目标应用（可多选）..."
                            : "已选应用 (" + cond.pkgs.size() + " 个)：" + AdvancedTriggerHelper.joinPkgs(cond.pkgs));
                    swState.setText(cond.stateOn ? "条件：任一所选应用处于前台" : "条件：所选应用均不在前台");
                } else if (cond.type == Rule.TRIG_CHARGING) {
                    swState.setText(cond.stateOn ? "条件：正在充电" : "条件：未在充电");
                } else if (cond.type == Rule.TRIG_SCREEN) {
                    swState.setText(cond.stateOn ? "条件：屏幕处于亮起状态" : "条件：屏幕处于熄灭状态");
                } else if (cond.type == Rule.TRIG_WIFI) {
                    etTarget.setVisibility(View.VISIBLE);
                    etTarget.setHint("指定 Wi-Fi 名称/SSID（留空则匹配任意 Wi-Fi）");
                    btnDetect.setVisibility(View.VISIBLE);
                    btnDetect.setText("填入当前已连 Wi-Fi");
                    swState.setText(cond.stateOn ? "条件：已连接该 Wi-Fi" : "条件：未连接该 Wi-Fi");
                } else if (cond.type == Rule.TRIG_BT) {
                    etTarget.setVisibility(View.VISIBLE);
                    etTarget.setHint("指定蓝牙设备名称或 MAC（留空则匹配任意蓝牙）");
                    btnDetect.setVisibility(View.VISIBLE);
                    btnDetect.setText("检测当前已连蓝牙设备");
                    swState.setText(cond.stateOn ? "条件：已连接该蓝牙设备" : "条件：未连接该蓝牙设备");
                }
            }
        };

        final LinearLayout typeBox = new LinearLayout(ctx);
        typeBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tblp = new LinearLayout.LayoutParams(-1, -2);
        tblp.topMargin = Md3e.dp(ctx, 12);
        tblp.bottomMargin = Md3e.dp(ctx, 8);
        root.addView(typeBox, tblp);

        final TextView[] chipViews = new TextView[types.length];
        for (int i = 0; i < types.length; i++) {
            final int idx = i;
            TextView item = new TextView(ctx);
            item.setTextSize(14);
            item.setPadding(Md3e.dp(ctx, 14), Md3e.dp(ctx, 10), Md3e.dp(ctx, 14), Md3e.dp(ctx, 10));
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(-1, -2);
            ilp.bottomMargin = Md3e.dp(ctx, 6);
            Md3e.attachSpringTouch(item);
            chipViews[i] = item;
            typeBox.addView(item, ilp);

            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cond.type = types[idx];
                    for (int j = 0; j < chipViews.length; j++) {
                        boolean sel = (j == idx);
                        chipViews[j].setText((sel ? "●  " : "○  ") + labels[j]);
                        chipViews[j].setTypeface(null, sel ? Typeface.BOLD : Typeface.NORMAL);
                        chipViews[j].setTextColor(sel ? t.onPrimaryContainer : t.textPrimary);
                        chipViews[j].setBackground(Md3e.ripple(ctx,
                                sel ? t.primaryContainer : t.surfaceLow, 0x22888888, 14));
                    }
                    refreshUi.run();
                }
            });
        }
        chipViews[0].performClick();

        root.addView(btnApps);
        root.addView(etTarget);
        root.addView(btnDetect);

        LinearLayout.LayoutParams swlp = new LinearLayout.LayoutParams(-1, -2);
        swlp.topMargin = Md3e.dp(ctx, 4);
        swlp.bottomMargin = Md3e.dp(ctx, 12);
        root.addView(swState, swlp);

        swState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cond.stateOn = swState.isChecked();
                refreshUi.run();
            }
        });

        btnApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppPickerDialog.show(ctx, "选择条件应用（可多选）", true, cond.pkgs, new AppPickerDialog.OnPickedListener() {
                    @Override
                    public void onPicked(List<String> packages) {
                        cond.pkgs = new ArrayList<>(packages);
                        refreshUi.run();
                    }
                });
            }
        });

        etTarget.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                cond.targetName = s.toString();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cond.type == Rule.TRIG_WIFI) {
                    AdvancedTriggerHelper.fillCurrentWifi(ctx, etTarget);
                } else if (cond.type == Rule.TRIG_BT) {
                    AdvancedTriggerHelper.fillCurrentBluetooth(ctx, etTarget);
                }
            }
        });

        LinearLayout actions = new LinearLayout(ctx);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        TextView btnCancel = new TextView(ctx);
        btnCancel.setText("取消");
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setTextColor(t.textPrimary);
        btnCancel.setBackground(Md3e.ripple(ctx, t.surfaceHigh, 0x22888888, 50));
        btnCancel.setPadding(0, Md3e.dp(ctx, 12), 0, Md3e.dp(ctx, 12));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, -2, 1f);
        clp.rightMargin = Md3e.dp(ctx, 8);
        actions.addView(btnCancel, clp);

        TextView btnOk = new TextView(ctx);
        btnOk.setText("添加条件");
        btnOk.setGravity(Gravity.CENTER);
        btnOk.setTextColor(t.onPrimary);
        btnOk.setTypeface(null, Typeface.BOLD);
        btnOk.setBackground(Md3e.ripple(ctx, t.primary, 0x33FFFFFF, 50));
        btnOk.setPadding(0, Md3e.dp(ctx, 12), 0, Md3e.dp(ctx, 12));
        actions.addView(btnOk, new LinearLayout.LayoutParams(0, -2, 1.4f));
        root.addView(actions);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { dlg.dismiss(); }
        });

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cond.type == Rule.TRIG_APP && cond.pkgs.isEmpty()) {
                    Toast.makeText(ctx, "请至少选择一个目标应用", Toast.LENGTH_SHORT).show();
                    return;
                }
                cond.targetName = etTarget.getText().toString().trim();
                dlg.dismiss();
                if (listener != null) listener.onAdded(cond);
            }
        });

        dlg.setContentView(root);
        if (dlg.getWindow() != null) {
            dlg.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dlg.getWindow().setLayout((int) (ctx.getResources().getDisplayMetrics().widthPixels * 0.90), -2);
        }
        dlg.show();
    }
}
