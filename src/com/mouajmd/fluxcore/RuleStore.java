package com.mouajmd.fluxcore;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;

public class RuleStore {
    private static final String PREF = "fluxcore_rules_v1";
    private static final String KEY = "rules_json";
    private static final String KEY_ADVANCED = "advanced_mode";
    private static final String KEY_PRESETS_INIT = "presets_v1_inited";

    public static boolean isAdvancedMode(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getBoolean(KEY_ADVANCED, false);
    }

    public static void setAdvancedMode(Context ctx, boolean enabled) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ADVANCED, enabled)
                .apply();
    }

    public static synchronized List<Rule> load(Context ctx) {
        List<Rule> list = new ArrayList<>();
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String raw = sp.getString(KEY, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                list.add(Rule.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception ignored) {}

        if (!sp.getBoolean(KEY_PRESETS_INIT, false)) {
            appendDefaultPresets(ctx, sp, list);
        }
        return list;
    }

    private static void appendDefaultPresets(Context ctx, SharedPreferences sp, List<Rule> list) {
        List<Rule> presets = new ArrayList<>();

        // 1. 游戏沉浸预设（默认手动触发，点击卡片可绑定具体游戏）
        Rule game = new Rule();
        game.id = "preset_gaming_mode";
        game.name = "游戏沉浸模式";
        game.enabled = true;
        game.triggerType = Rule.TRIG_MANUAL;
        game.dndMode = 2;
        game.refreshRate = 120;
        game.brightness = 200;
        game.autoRevert = true;
        presets.add(game);

        // 2. 睡眠勿扰预设
        Rule sleep = new Rule();
        sleep.id = "preset_sleep_mode";
        sleep.name = "深度睡眠模式";
        sleep.enabled = true;
        sleep.triggerType = Rule.TRIG_MANUAL;
        sleep.dndMode = 2;
        sleep.refreshRate = 60;
        sleep.brightness = 15;
        sleep.wifiAction = 0;
        sleep.autoRevert = true;
        presets.add(sleep);

        // 3. 极致省电预设（联动系统省电模式 + 60Hz + 低亮度）
        Rule saver = new Rule();
        saver.id = "preset_power_saving";
        saver.name = "极致省电模式";
        saver.enabled = true;
        saver.triggerType = Rule.TRIG_MANUAL;
        saver.refreshRate = 60;
        saver.brightness = 45;
        saver.customCmd = "settings put global low_power 1";
        saver.revertCmd = "settings put global low_power 0";
        saver.autoRevert = true;
        presets.add(saver);

        for (Rule p : presets) {
            boolean exists = false;
            for (Rule cur : list) {
                if (p.id.equals(cur.id)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                list.add(p);
            }
        }

        saveAll(ctx, list);
        sp.edit().putBoolean(KEY_PRESETS_INIT, true).apply();
    }

    public static synchronized void saveAll(Context ctx, List<Rule> rules) {
        JSONArray arr = new JSONArray();
        for (Rule r : rules) {
            try {
                arr.put(r.toJson());
            } catch (Exception ignored) {}
        }
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY, arr.toString())
                .apply();
    }

    public static synchronized void upsert(Context ctx, Rule rule) {
        List<Rule> all = load(ctx);
        boolean found = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id.equals(rule.id)) {
                all.set(i, rule);
                found = true;
                break;
            }
        }
        if (!found) all.add(0, rule);
        saveAll(ctx, all);
    }

    public static synchronized void delete(Context ctx, String id) {
        List<Rule> all = load(ctx);
        for (int i = all.size() - 1; i >= 0; i--) {
            if (all.get(i).id.equals(id)) all.remove(i);
        }
        saveAll(ctx, all);
    }
}
