package com.mouajmd.fluxcore;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

public class Md3e {
    public int bg;
    public int surfaceLow;
    public int surfaceHigh;
    public int primary;
    public int onPrimary;
    public int primaryContainer;
    public int onPrimaryContainer;
    public int secondaryContainer;
    public int onSecondaryContainer;
    public int textPrimary;
    public int textSecondary;
    public boolean isDark;

    public static Md3e of(Context ctx) {
        Md3e t = new Md3e();
        int nightMask = ctx.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        t.isDark = (nightMask == Configuration.UI_MODE_NIGHT_YES);

        if (Build.VERSION.SDK_INT >= 31) {
            if (t.isDark) {
                t.primary = sysColor(ctx, android.R.color.system_accent1_200, 0xFFD0BCFF);
                t.onPrimary = sysColor(ctx, android.R.color.system_accent1_800, 0xFF381E72);
                t.primaryContainer = sysColor(ctx, android.R.color.system_accent1_700, 0xFF4F378B);
                t.onPrimaryContainer = sysColor(ctx, android.R.color.system_accent1_100, 0xFFEADDFF);
                t.secondaryContainer = sysColor(ctx, android.R.color.system_accent2_700, 0xFF4A4458);
                t.onSecondaryContainer = sysColor(ctx, android.R.color.system_accent2_100, 0xFFE8DEF8);

                int baseDark = sysColor(ctx, android.R.color.system_neutral1_900, 0xFF141218);
                t.bg = blend(baseDark, t.primary, 0.03f);
                t.surfaceLow = blend(baseDark, t.primary, 0.09f);
                t.surfaceHigh = blend(baseDark, t.primary, 0.15f);
                t.textPrimary = 0xFFE6E1E5;
                t.textSecondary = 0xFFCAC4D0;
            } else {
                t.primary = sysColor(ctx, android.R.color.system_accent1_600, 0xFF6750A4);
                t.onPrimary = 0xFFFFFFFF;
                t.primaryContainer = sysColor(ctx, android.R.color.system_accent1_100, 0xFFEADDFF);
                t.onPrimaryContainer = sysColor(ctx, android.R.color.system_accent1_900, 0xFF21005D);
                t.secondaryContainer = sysColor(ctx, android.R.color.system_accent2_100, 0xFFE8DEF8);
                t.onSecondaryContainer = sysColor(ctx, android.R.color.system_accent2_900, 0xFF1D192B);

                int baseLight = sysColor(ctx, android.R.color.system_neutral1_10, 0xFFFCF8FD);
                t.bg = blend(baseLight, t.primary, 0.02f);
                t.surfaceLow = blend(baseLight, t.primary, 0.065f);
                t.surfaceHigh = blend(baseLight, t.primary, 0.12f);
                t.textPrimary = 0xFF1C1B1F;
                t.textSecondary = 0xFF49454F;
            }
        } else {
            if (t.isDark) {
                t.primary = 0xFFD0BCFF;
                t.onPrimary = 0xFF381E72;
                t.primaryContainer = 0xFF4F378B;
                t.onPrimaryContainer = 0xFFEADDFF;
                t.secondaryContainer = 0xFF4A4458;
                t.onSecondaryContainer = 0xFFE8DEF8;
                t.bg = 0xFF141218;
                t.surfaceLow = 0xFF211F26;
                t.surfaceHigh = 0xFF2B2930;
                t.textPrimary = 0xFFE6E1E5;
                t.textSecondary = 0xFFCAC4D0;
            } else {
                t.primary = 0xFF6750A4;
                t.onPrimary = 0xFFFFFFFF;
                t.primaryContainer = 0xFFEADDFF;
                t.onPrimaryContainer = 0xFF21005D;
                t.secondaryContainer = 0xFFE8DEF8;
                t.onSecondaryContainer = 0xFF1D192B;
                t.bg = 0xFFFAF7FC;
                t.surfaceLow = 0xFFF1ECF6;
                t.surfaceHigh = 0xFFE6E0EC;
                t.textPrimary = 0xFF1C1B1F;
                t.textSecondary = 0xFF49454F;
            }
        }
        return t;
    }

    private static int blend(int base, int tint, float ratio) {
        float inv = 1f - ratio;
        int r = Math.round(Color.red(base) * inv + Color.red(tint) * ratio);
        int g = Math.round(Color.green(base) * inv + Color.green(tint) * ratio);
        int b = Math.round(Color.blue(base) * inv + Color.blue(tint) * ratio);
        return Color.rgb(
                Math.max(0, Math.min(255, r)),
                Math.max(0, Math.min(255, g)),
                Math.max(0, Math.min(255, b))
        );
    }

    private static int sysColor(Context ctx, int resId, int fallback) {
        try {
            return ctx.getResources().getColor(resId, ctx.getTheme());
        } catch (Throwable e) {
            return fallback;
        }
    }

    public static int dp(Context ctx, float dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, ctx.getResources().getDisplayMetrics()));
    }

    public static GradientDrawable card(Context ctx, int fillColor, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fillColor);
        d.setCornerRadius(dp(ctx, radiusDp));
        return d;
    }

    public static RippleDrawable ripple(Context ctx, int fillColor, int rippleColor, float radiusDp) {
        GradientDrawable content = card(ctx, fillColor, radiusDp);
        GradientDrawable mask = card(ctx, Color.WHITE, radiusDp);
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask);
    }

    public static void attachSpringTouch(final View v) {
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(110).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(
                                view,
                                PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f),
                                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f)
                        );
                        anim.setDuration(320);
                        anim.setInterpolator(new OvershootInterpolator(2.2f));
                        anim.start();
                        break;
                }
                return false;
            }
        });
    }

    public static void popIn(View v, long delayMs) {
        v.setAlpha(0f);
        v.setTranslationY(40f);
        v.setScaleX(0.96f);
        v.setScaleY(0.96f);
        v.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(delayMs)
                .setDuration(380)
                .setInterpolator(new OvershootInterpolator(1.3f))
                .start();
    }
}
