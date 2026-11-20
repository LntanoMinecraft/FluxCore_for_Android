package com.mouajmd.fluxcore;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

public class Md3e {
    public int bg;
    public int surfaceLow;
    public int surface;
    public int surfaceHigh;
    public int primary;
    public int onPrimary;
    public int primaryContainer;
    public int onPrimaryContainer;
    public int secondaryContainer;
    public int onSecondaryContainer;
    public int tertiaryContainer;
    public int onTertiaryContainer;
    public int textPrimary;
    public int textSecondary;
    public int outline;

    public static Md3e of(Context ctx) {
        Md3e t = new Md3e();
        boolean dark = (ctx.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        int pLight = sysColor(ctx, "system_accent1_600", 0xFF6750A4);
        int pDark = sysColor(ctx, "system_accent1_200", 0xFFD0BCFF);
        int pcLight = sysColor(ctx, "system_accent1_100", 0xFFEADDFF);
        int pcDark = sysColor(ctx, "system_accent1_700", 0xFF4F378B);
        int scLight = sysColor(ctx, "system_accent2_100", 0xFFE8DEF8);
        int scDark = sysColor(ctx, "system_accent2_700", 0xFF4A4458);
        int tcLight = sysColor(ctx, "system_accent3_100", 0xFFFFD8E4);
        int tcDark = sysColor(ctx, "system_accent3_700", 0xFF633B48);

        if (dark) {
            t.bg = sysColor(ctx, "system_neutral1_900", 0xFF141218);
            t.surfaceLow = 0xFF1D1B20;
            t.surface = 0xFF211F26;
            t.surfaceHigh = 0xFF2B2930;
            t.primary = pDark;
            t.onPrimary = 0xFF381E72;
            t.primaryContainer = pcDark;
            t.onPrimaryContainer = pcLight;
            t.secondaryContainer = scDark;
            t.onSecondaryContainer = scLight;
            t.tertiaryContainer = tcDark;
            t.onTertiaryContainer = tcLight;
            t.textPrimary = 0xFFE6E0E9;
            t.textSecondary = 0xFFCAC4D0;
            t.outline = 0xFF49454F;
        } else {
            t.bg = sysColor(ctx, "system_neutral1_50", 0xFFFEF7FF);
            t.surfaceLow = 0xFFF7F2FA;
            t.surface = 0xFFF3EDF7;
            t.surfaceHigh = 0xFFECE6F0;
            t.primary = pLight;
            t.onPrimary = 0xFFFFFFFF;
            t.primaryContainer = pcLight;
            t.onPrimaryContainer = 0xFF21005D;
            t.secondaryContainer = scLight;
            t.onSecondaryContainer = 0xFF1D192B;
            t.tertiaryContainer = tcLight;
            t.onTertiaryContainer = 0xFF31111D;
            t.textPrimary = 0xFF1D1B20;
            t.textSecondary = 0xFF49454F;
            t.outline = 0xFFCAC4D0;
        }
        return t;
    }

    private static int sysColor(Context ctx, String name, int fallback) {
        int id = ctx.getResources().getIdentifier(name, "color", "android");
        if (id != 0) {
            try {
                return ctx.getResources().getColor(id, ctx.getTheme());
            } catch (Exception ignored) {}
        }
        return fallback;
    }

    public static int dp(Context ctx, float dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }

    public static Drawable card(Context ctx, int fillColor, float radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fillColor);
        gd.setCornerRadius(dp(ctx, radiusDp));
        return gd;
    }

    public static Drawable outlinedCard(Context ctx, int fillColor, int strokeColor, float radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fillColor);
        gd.setStroke(dp(ctx, 1), strokeColor);
        gd.setCornerRadius(dp(ctx, radiusDp));
        return gd;
    }

    public static Drawable ripple(Context ctx, int fillColor, int rippleColor, float radiusDp) {
        GradientDrawable content = new GradientDrawable();
        content.setColor(fillColor);
        content.setCornerRadius(dp(ctx, radiusDp));
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(Color.WHITE);
        mask.setCornerRadius(dp(ctx, radiusDp));
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask);
    }

    public static TimeInterpolator spring(final float tension, final float damping) {
        return new TimeInterpolator() {
            @Override
            public float getInterpolation(float t) {
                if (t == 0f || t == 1f) return t;
                return (float) (1.0 - Math.exp(-damping * t) * Math.cos(tension * t));
            }
        };
    }

    public static void popIn(View v, long delay) {
        v.setAlpha(0f);
        v.setTranslationY(dp(v.getContext(), 24));
        v.setScaleX(0.95f);
        v.setScaleY(0.95f);
        v.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(delay)
                .setDuration(420)
                .setInterpolator(spring(11f, 7.5f))
                .start();
    }

    public static void attachSpringTouch(final View v) {
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(120).start();
                        break;
                    case MotionEvent.ACTION_UP:
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    case MotionEvent.ACTION_CANCEL:
                        view.animate().scaleX(1f).scaleY(1f).setDuration(280)
                                .setInterpolator(spring(14f, 6.5f)).start();
                        break;
                }
                return false;
            }
        });
    }

    public static void pulse(View v) {
        ObjectAnimator sx = ObjectAnimator.ofFloat(v, "scaleX", 0.90f, 1.04f, 1.0f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(v, "scaleY", 0.90f, 1.04f, 1.0f);
        sx.setDuration(320);
        sy.setDuration(320);
        sx.start();
        sy.start();
    }
}
