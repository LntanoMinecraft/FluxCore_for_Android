package com.mouajmd.fluxcore;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppPickerDialog {
    public interface OnPickedListener {
        void onPicked(List<String> packages);
    }

    private static class AppItem {
        String label;
        String pkg;
        Drawable icon;
    }

    public static void show(final Context ctx, String title, final boolean multi,
                            List<String> preselected, final OnPickedListener listener) {
        final Md3e t = Md3e.of(ctx);
        final Dialog dlg = new Dialog(ctx);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(Md3e.card(ctx, t.bg, 28));
        int pad = Md3e.dp(ctx, 20);
        root.setPadding(pad, pad, pad, pad);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(title);
        tvTitle.setTextSize(20);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(t.textPrimary);
        root.addView(tvTitle);

        final EditText search = new EditText(ctx);
        search.setHint("搜索应用名称或包名...");
        search.setHintTextColor(t.textSecondary);
        search.setTextColor(t.textPrimary);
        search.setBackground(Md3e.card(ctx, t.surfaceHigh, 16));
        int sp = Md3e.dp(ctx, 12);
        search.setPadding(sp, sp, sp, sp);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, -2);
        slp.topMargin = Md3e.dp(ctx, 12);
        slp.bottomMargin = Md3e.dp(ctx, 12);
        root.addView(search, slp);

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout.LayoutParams sclp = new LinearLayout.LayoutParams(-1, Md3e.dp(ctx, 360));
        root.addView(scroll, sclp);

        final LinearLayout listContainer = new LinearLayout(ctx);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listContainer);

        final Set<String> selected = new HashSet<>();
        if (preselected != null) selected.addAll(preselected);
        final List<AppItem> allApps = new ArrayList<>();

        final TextView btnOk = multi ? new TextView(ctx) : null;
        final Runnable updateOkText = new Runnable() {
            @Override
            public void run() {
                if (btnOk != null) {
                    btnOk.setText("确认选择 (" + selected.size() + ")");
                }
            }
        };

        if (multi) {
            updateOkText.run();
            btnOk.setGravity(Gravity.CENTER);
            btnOk.setTextColor(t.onPrimary);
            btnOk.setTypeface(null, Typeface.BOLD);
            btnOk.setBackground(Md3e.ripple(ctx, t.primary, 0x33FFFFFF, 50));
            btnOk.setPadding(0, Md3e.dp(ctx, 14), 0, Md3e.dp(ctx, 14));
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, -2);
            blp.topMargin = Md3e.dp(ctx, 12);
            root.addView(btnOk, blp);
            btnOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dlg.dismiss();
                    listener.onPicked(new ArrayList<>(selected));
                }
            });
        }

        dlg.setContentView(root);
        if (dlg.getWindow() != null) {
            dlg.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dlg.getWindow().setLayout((int) (ctx.getResources().getDisplayMetrics().widthPixels * 0.92), -2);
        }
        dlg.show();

        final Runnable render = new Runnable() {
            @Override
            public void run() {
                listContainer.removeAllViews();
                String kw = search.getText().toString().trim().toLowerCase();
                for (final AppItem item : allApps) {
                    if (!kw.isEmpty() && !item.label.toLowerCase().contains(kw) && !item.pkg.toLowerCase().contains(kw)) {
                        continue;
                    }
                    LinearLayout row = new LinearLayout(ctx);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setBackground(Md3e.ripple(ctx, t.surfaceLow, 0x22888888, 18));
                    int rp = Md3e.dp(ctx, 10);
                    row.setPadding(rp, rp, rp, rp);
                    LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
                    rlp.bottomMargin = Md3e.dp(ctx, 8);

                    ImageView iv = new ImageView(ctx);
                    iv.setImageDrawable(item.icon);
                    row.addView(iv, new LinearLayout.LayoutParams(Md3e.dp(ctx, 36), Md3e.dp(ctx, 36)));

                    LinearLayout texts = new LinearLayout(ctx);
                    texts.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, -2, 1f);
                    tlp.leftMargin = Md3e.dp(ctx, 12);

                    TextView tvName = new TextView(ctx);
                    tvName.setText(item.label);
                    tvName.setTextColor(t.textPrimary);
                    tvName.setTextSize(15);
                    tvName.setTypeface(null, Typeface.BOLD);

                    TextView tvPkg = new TextView(ctx);
                    tvPkg.setText(item.pkg);
                    tvPkg.setTextColor(t.textSecondary);
                    tvPkg.setTextSize(11);

                    texts.addView(tvName);
                    texts.addView(tvPkg);
                    row.addView(texts, tlp);

                    if (multi) {
                        final CheckBox cb = new CheckBox(ctx);
                        cb.setChecked(selected.contains(item.pkg));
                        cb.setClickable(false);
                        row.addView(cb);
                        row.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (selected.contains(item.pkg)) {
                                    selected.remove(item.pkg);
                                    cb.setChecked(false);
                                } else {
                                    selected.add(item.pkg);
                                    cb.setChecked(true);
                                }
                                updateOkText.run();
                            }
                        });
                    } else {
                        row.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dlg.dismiss();
                                listener.onPicked(Collections.singletonList(item.pkg));
                            }
                        });
                    }
                    listContainer.addView(row, rlp);
                }
            }
        };

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { render.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                PackageManager pm = ctx.getPackageManager();
                List<ApplicationInfo> apps = pm.getInstalledApplications(0);
                final List<AppItem> loaded = new ArrayList<>();
                for (ApplicationInfo ai : apps) {
                    if (ai.packageName.equals(ctx.getPackageName())) continue;
                    boolean isSys = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                    if (isSys && pm.getLaunchIntentForPackage(ai.packageName) == null) continue;
                    AppItem item = new AppItem();
                    item.pkg = ai.packageName;
                    item.label = ai.loadLabel(pm).toString();
                    item.icon = ai.loadIcon(pm);
                    loaded.add(item);
                }
                Collections.sort(loaded, new Comparator<AppItem>() {
                    @Override
                    public int compare(AppItem a, AppItem b) {
                        return a.label.compareToIgnoreCase(b.label);
                    }
                });
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        allApps.clear();
                        allApps.addAll(loaded);
                        render.run();
                    }
                });
            }
        }).start();
    }
}
