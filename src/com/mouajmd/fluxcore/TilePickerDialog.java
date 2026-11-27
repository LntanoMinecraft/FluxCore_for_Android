package com.mouajmd.fluxcore;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TilePickerDialog {
    public interface OnTilePickedListener {
        void onPicked(String component, String label);
    }

    private static class TileItem {
        String label;
        String appName;
        String component;
        Drawable icon;
    }

    public static void show(final Context ctx, final String currentComp, final OnTilePickedListener listener) {
        final Md3e t = Md3e.of(ctx);
        final Dialog dlg = new Dialog(ctx);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(Md3e.card(ctx, t.bg, 28));
        int pad = Md3e.dp(ctx, 20);
        root.setPadding(pad, pad, pad, pad);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText("选择状态栏磁贴 (QS Tile)");
        tvTitle.setTextSize(20);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(t.textPrimary);
        root.addView(tvTitle);

        final EditText search = new EditText(ctx);
        search.setHint("搜索磁贴名称或所属应用...");
        search.setHintTextColor(t.textSecondary);
        search.setTextColor(t.textPrimary);
        search.setSingleLine(true);
        search.setBackground(Md3e.card(ctx, t.surfaceHigh, 16));
        int sp = Md3e.dp(ctx, 12);
        search.setPadding(sp, sp, sp, sp);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, -2);
        slp.topMargin = Md3e.dp(ctx, 12);
        slp.bottomMargin = Md3e.dp(ctx, 10);
        root.addView(search, slp);

        TextView btnClear = new TextView(ctx);
        btnClear.setText("清除已选磁贴（不控制磁贴）");
        btnClear.setGravity(Gravity.CENTER);
        btnClear.setTextSize(13);
        btnClear.setTextColor(t.primary);
        btnClear.setBackground(Md3e.ripple(ctx, t.surfaceLow, 0x22888888, 16));
        btnClear.setPadding(0, Md3e.dp(ctx, 10), 0, Md3e.dp(ctx, 10));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.bottomMargin = Md3e.dp(ctx, 10);
        root.addView(btnClear, clp);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Md3e.haptic(v);
                dlg.dismiss();
                listener.onPicked("", "");
            }
        });

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout.LayoutParams sclp = new LinearLayout.LayoutParams(-1, Md3e.dp(ctx, 350));
        root.addView(scroll, sclp);

        final LinearLayout listContainer = new LinearLayout(ctx);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listContainer);

        final List<TileItem> allTiles = new ArrayList<>();

        dlg.setContentView(root);
        if (dlg.getWindow() != null) {
            dlg.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dlg.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            dlg.getWindow().setLayout((int) (ctx.getResources().getDisplayMetrics().widthPixels * 0.92), -2);
        }
        dlg.show();

        final Runnable render = new Runnable() {
            @Override
            public void run() {
                listContainer.removeAllViews();
                String kw = search.getText().toString().trim().toLowerCase();
                for (final TileItem item : allTiles) {
                    if (!kw.isEmpty() && !item.label.toLowerCase().contains(kw)
                            && !item.appName.toLowerCase().contains(kw)
                            && !item.component.toLowerCase().contains(kw)) {
                        continue;
                    }
                    boolean isCur = item.component.equals(currentComp);
                    LinearLayout row = new LinearLayout(ctx);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setBackground(Md3e.ripple(ctx, isCur ? t.secondaryContainer : t.surfaceLow, 0x22888888, 18));
                    int rp = Md3e.dp(ctx, 10);
                    row.setPadding(rp, rp, rp, rp);
                    LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
                    rlp.bottomMargin = Md3e.dp(ctx, 8);

                    ImageView iv = new ImageView(ctx);
                    iv.setImageDrawable(item.icon);
                    row.addView(iv, new LinearLayout.LayoutParams(Md3e.dp(ctx, 34), Md3e.dp(ctx, 34)));

                    LinearLayout texts = new LinearLayout(ctx);
                    texts.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, -2, 1f);
                    tlp.leftMargin = Md3e.dp(ctx, 12);

                    TextView tvName = new TextView(ctx);
                    tvName.setText(item.label + " · " + item.appName);
                    tvName.setTextColor(isCur ? t.onSecondaryContainer : t.textPrimary);
                    tvName.setTextSize(14);
                    tvName.setTypeface(null, Typeface.BOLD);

                    TextView tvComp = new TextView(ctx);
                    tvComp.setText(item.component);
                    tvComp.setTextColor(isCur ? t.onSecondaryContainer : t.textSecondary);
                    tvComp.setTextSize(10);

                    texts.addView(tvName);
                    texts.addView(tvComp);
                    row.addView(texts, tlp);

                    row.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Md3e.haptic(v);
                            dlg.dismiss();
                            listener.onPicked(item.component, item.label + "(" + item.appName + ")");
                        }
                    });
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
                Intent qIntent = new Intent("android.service.quicksettings.action.QS_TILE");
                List<ResolveInfo> ris = pm.queryIntentServices(qIntent, 0);
                final List<TileItem> loaded = new ArrayList<>();
                for (ResolveInfo ri : ris) {
                    if (ri.serviceInfo == null) continue;
                    TileItem item = new TileItem();
                    item.component = ri.serviceInfo.packageName + "/" + ri.serviceInfo.name;
                    item.label = ri.loadLabel(pm).toString();
                    try {
                        item.appName = ri.serviceInfo.applicationInfo.loadLabel(pm).toString();
                    } catch (Throwable e) {
                        item.appName = ri.serviceInfo.packageName;
                    }
                    item.icon = ri.loadIcon(pm);
                    loaded.add(item);
                }
                Collections.sort(loaded, new Comparator<TileItem>() {
                    @Override
                    public int compare(TileItem a, TileItem b) {
                        boolean ca = a.component.equals(currentComp);
                        boolean cb = b.component.equals(currentComp);
                        if (ca != cb) return ca ? -1 : 1;
                        return a.appName.compareToIgnoreCase(b.appName);
                    }
                });
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        allTiles.clear();
                        allTiles.addAll(loaded);
                        render.run();
                    }
                });
            }
        }).start();
    }
}
