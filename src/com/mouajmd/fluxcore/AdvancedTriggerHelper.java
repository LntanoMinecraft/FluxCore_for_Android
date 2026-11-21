package com.mouajmd.fluxcore;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdvancedTriggerHelper {

    public static void attach(final Context ctx, final Md3e t, LinearLayout form,
                              final Rule r, final TextView btnTrigDetail,
                              final Switch swTrigOn, final Runnable baseRefreshOut) {
        final boolean adv = RuleStore.isAdvancedMode(ctx);

        final EditText etTarget = makeInput(ctx, t, "", r.triggerTarget);
        etTarget.setVisibility(View.GONE);
        form.addView(etTarget);

        final TextView btnDetect = makePill(ctx, t, "");
        btnDetect.setVisibility(View.GONE);
        form.addView(btnDetect);

        etTarget.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                r.triggerTarget = s.toString().trim();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (r.triggerType == Rule.TRIG_WIFI) {
                    fillCurrentWifi(ctx, etTarget);
                } else if (r.triggerType == Rule.TRIG_BT) {
                    fillCurrentBluetooth(ctx, etTarget);
                }
            }
        });

        // 进阶模式：多条件同时满足 (AND) 容器
        final LinearLayout advCondBox = new LinearLayout(ctx);
        advCondBox.setOrientation(LinearLayout.VERTICAL);
        advCondBox.setVisibility(adv ? View.VISIBLE : View.GONE);
        LinearLayout.LayoutParams aclp = new LinearLayout.LayoutParams(-1, -2);
        aclp.topMargin = Md3e.dp(ctx, 6);
        aclp.bottomMargin = Md3e.dp(ctx, 6);
        form.addView(advCondBox, aclp);

        TextView tvSub = new TextView(ctx);
        tvSub.setText("附加同时满足条件 (AND · 全部满足才触发)");
        tvSub.setTextSize(12);
        tvSub.setTypeface(null, Typeface.BOLD);
        tvSub.setTextColor(t.primary);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, -2);
        slp.bottomMargin = Md3e.dp(ctx, 6);
        advCondBox.addView(tvSub, slp);

        final LinearLayout condListLayout = new LinearLayout(ctx);
        condListLayout.setOrientation(LinearLayout.VERTICAL);
        advCondBox.addView(condListLayout);

        final Runnable renderExtraList = new Runnable() {
            @Override
            public void run() {
                condListLayout.removeAllViews();
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
                    rlp.bottomMargin = Md3e.dp(ctx, 6);

                    TextView tvDesc = new TextView(ctx);
                    tvDesc.setText("且 " + c.describe());
                    tvDesc.setTextSize(13);
                    tvDesc.setTextColor(t.textPrimary);
                    row.addView(tvDesc, new LinearLayout.LayoutParams(0, -2, 1f));

                    TextView btnDel = new TextView(ctx);
                    btnDel.setText("移除");
                    btnDel.setTextSize(12);
                    btnDel.setTypeface(null, Typeface.BOLD);
                    btnDel.setTextColor(t.primary);
                    btnDel.setPadding(Md3e.dp(ctx, 10), Md3e.dp(ctx, 4), Md3e.dp(ctx, 6), Md3e.dp(ctx, 4));
                    btnDel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            r.extraConditions.remove(idx);
                            run();
                        }
                    });
                    row.addView(btnDel);
                    condListLayout.addView(row, rlp);
                }
            }
        };
        renderExtraList.run();

        TextView btnAddCond = makePill(ctx, t, "+ 添加附加条件 (AND)");
        btnAddCond.setGravity(Gravity.CENTER);
        btnAddCond.setTypeface(null, Typeface.BOLD);
        advCondBox.addView(btnAddCond);
        btnAddCond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (r.triggerType == Rule.TRIG_MANUAL) {
                    Toast.makeText(ctx, "请先将主触发类型设为非「仅手动触发」", Toast.LENGTH_SHORT).show();
                    return;
                }
                ConditionPickerDialog.show(ctx, new ConditionPickerDialog.OnAddedListener() {
                    @Override
                    public void onAdded(Rule.Condition condition) {
                        r.extraConditions.add(condition);
                        renderExtraList.run();
                    }
                });
            }
        });

        // 接管触发器 UI 刷新逻辑
        final Runnable refreshTrigger = new Runnable() {
            @Override
            public void run() {
                etTarget.setVisibility(View.GONE);
                btnDetect.setVisibility(View.GONE);

                if (r.triggerType == Rule.TRIG_MANUAL) {
                    btnTrigDetail.setVisibility(View.GONE);
                    swTrigOn.setVisibility(View.GONE);
                    advCondBox.setVisibility(View.GONE);
                } else if (r.triggerType == Rule.TRIG_APP) {
                    btnTrigDetail.setVisibility(View.VISIBLE);
                    if (adv) {
                        btnTrigDetail.setText(r.triggerPkgs.isEmpty()
                                ? "点击选择共用情境的应用（支持多选）..."
                                : "已选 " + r.triggerPkgs.size() + " 个应用：" + joinPkgs(r.triggerPkgs));
                        swTrigOn.setText(swTrigOn.isChecked() ? "条件：打开任意所选应用时" : "条件：退出全部所选应用时");
                    } else {
                        btnTrigDetail.setText(r.triggerPkgs.isEmpty()
                                ? "点击选择目标应用..."
                                : "目标应用：" + r.triggerPkgs.get(0));
                        swTrigOn.setText(swTrigOn.isChecked() ? "条件：打开该应用时" : "条件：退出该应用时");
                    }
                    swTrigOn.setVisibility(View.VISIBLE);
                    advCondBox.setVisibility(adv ? View.VISIBLE : View.GONE);
                } else {
                    btnTrigDetail.setVisibility(View.GONE);
                    swTrigOn.setVisibility(View.VISIBLE);
                    advCondBox.setVisibility(adv ? View.VISIBLE : View.GONE);
                    swTrigOn.setText(swTrigOn.isChecked() ? "条件：状态开启/连接时" : "条件：状态关闭/断开时");

                    if (adv && r.triggerType == Rule.TRIG_WIFI) {
                        etTarget.setVisibility(View.VISIBLE);
                        etTarget.setHint("指定 Wi-Fi 名称/SSID（留空则任意 Wi-Fi）");
                        btnDetect.setVisibility(View.VISIBLE);
                        btnDetect.setText("填入当前已连 Wi-Fi");
                    } else if (adv && r.triggerType == Rule.TRIG_BT) {
                        etTarget.setVisibility(View.VISIBLE);
                        etTarget.setHint("指定蓝牙设备名称或 MAC（留空则任意蓝牙）");
                        btnDetect.setVisibility(View.VISIBLE);
                        btnDetect.setText("检测当前已连蓝牙设备");
                    }
                }
            }
        };

        // 覆盖目标应用按钮点击事件（普通模式单选，进阶模式多选）
        btnTrigDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppPickerDialog.show(ctx,
                        adv ? "选择共用此情境的应用（可多选）" : "选择触发应用",
                        adv,
                        r.triggerPkgs,
                        new AppPickerDialog.OnPickedListener() {
                            @Override
                            public void onPicked(List<String> packages) {
                                r.triggerPkgs = new ArrayList<>(packages);
                                refreshTrigger.run();
                            }
                        });
            }
        });

        // 将刷新委托绑定到外部回调并立即执行一次
        if (baseRefreshOut instanceof DelegateRunnable) {
            ((DelegateRunnable) baseRefreshOut).target = refreshTrigger;
        }
        refreshTrigger.run();
    }

    public static class DelegateRunnable implements Runnable {
        public Runnable target;
        @Override
        public void run() {
            if (target != null) target.run();
        }
    }

    public static void fillCurrentWifi(final Context ctx, final EditText targetInput) {
        Toast.makeText(ctx, "正在读取当前 Wi-Fi...", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String ssid = RootShell.queryCurrentWifiSsid();
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (ssid != null && !ssid.isEmpty()) {
                            targetInput.setText(ssid);
                            Toast.makeText(ctx, "已填入 Wi-Fi：" + ssid, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ctx, "未检测到已连接的 Wi-Fi，请手动输入", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }

    public static void fillCurrentBluetooth(final Context ctx, final EditText targetInput) {
        Toast.makeText(ctx, "正在检测已连蓝牙设备...", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String dump = RootShell.queryConnectedBtDevices();
                final List<String> candidates = parseBtCandidates(dump);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (candidates.isEmpty()) {
                            Toast.makeText(ctx, "未检测到已连接蓝牙设备，请手动输入名称或 MAC", Toast.LENGTH_SHORT).show();
                        } else if (candidates.size() == 1) {
                            targetInput.setText(candidates.get(0));
                            Toast.makeText(ctx, "已填入蓝牙设备：" + candidates.get(0), Toast.LENGTH_SHORT).show();
                        } else {
                            final String[] arr = candidates.toArray(new String[0]);
                            new AlertDialog.Builder(ctx)
                                    .setTitle("选择检测到的蓝牙设备")
                                    .setItems(arr, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            targetInput.setText(arr[which]);
                                        }
                                    })
                                    .show();
                        }
                    }
                });
            }
        }).start();
    }

    private static List<String> parseBtCandidates(String dump) {
        Set<String> set = new LinkedHashSet<>();
        if (dump == null || dump.isEmpty()) return new ArrayList<>();
        // 优先提取括号中的设备名称，如 Device: AA:BB:CC:DD:EE:FF (AirPods Pro)
        Matcher nameMatcher = Pattern.compile("\\(([^()]{2,40})\\)").matcher(dump);
        while (nameMatcher.find()) {
            String name = nameMatcher.group(1).trim();
            if (!name.equalsIgnoreCase("null") && !name.matches("\\d+")) {
                set.add(name);
            }
        }
        // 其次提取 MAC 地址作为备选
        Matcher macMatcher = Pattern.compile("([0-9A-Fa-f]{2}(?::[0-9A-Fa-f]{2}){5})").matcher(dump);
        while (macMatcher.find()) {
            String mac = macMatcher.group(1);
            if (!mac.equals("00:00:00:00:00:00")) {
                set.add(mac);
            }
        }
        return new ArrayList<>(set);
    }

    public static String joinPkgs(List<String> pkgs) {
        if (pkgs == null || pkgs.isEmpty()) return "";
        if (pkgs.size() <= 2) return String.join(", ", pkgs);
        return pkgs.get(0) + ", " + pkgs.get(1) + " 等";
    }

    public static EditText makeInput(Context ctx, Md3e t, String hint, String val) {
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

    public static TextView makePill(Context ctx, Md3e t, String text) {
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
