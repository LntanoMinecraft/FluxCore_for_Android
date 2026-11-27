package com.mouajmd.fluxcore;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

public class RuleStore {
    private static final String PREF = "fluxcore_rules_v1";
    private static final String KEY = "rules_json";
    private static final String KEY_ADVANCED = "advanced_mode";

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
        return list;
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

    public static synchronized String exportConfig(Context ctx) {
        String dirPath = "/storage/emulated/0/Download/FluxCore";
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "FluxCore_config_" + stamp + ".json";
        String fullPath = dirPath + "/" + fileName;

        try {
            JSONObject root = new JSONObject();
            root.put("app", "FluxCore");
            root.put("version", "1.2.0");
            root.put("advancedMode", isAdvancedMode(ctx));
            JSONArray arr = new JSONArray();
            for (Rule r : load(ctx)) arr.put(r.toJson());
            root.put("rules", arr);

            byte[] data = root.toString(2).getBytes(StandardCharsets.UTF_8);
            File tmp = new File(ctx.getCacheDir(), fileName);
            FileOutputStream fos = new FileOutputStream(tmp);
            fos.write(data);
            fos.close();

            File outDir = new File(dirPath);
            if (!outDir.exists()) outDir.mkdirs();
            try {
                FileOutputStream outFos = new FileOutputStream(new File(outDir, fileName));
                outFos.write(data);
                outFos.close();
            } catch (Throwable ignored) {
                RootShell.exec("mkdir -p '" + dirPath + "' && cp -f '" + tmp.getAbsolutePath() + "' '" + fullPath + "' && chmod 664 '" + fullPath + "'");
            }
            AppLogger.log("CONFIG", "配置已导出至: " + fullPath);
            return fullPath;
        } catch (Throwable e) {
            AppLogger.log("ERROR", "配置导出失败: " + e.getMessage());
            return null;
        }
    }

    public static synchronized int importConfigFromFile(Context ctx, File file, boolean overwrite) {
        try {
            String raw;
            try {
                FileInputStream fis = new FileInputStream(file);
                byte[] buf = new byte[(int) file.length()];
                int read = fis.read(buf);
                fis.close();
                raw = new String(buf, 0, Math.max(0, read), StandardCharsets.UTF_8);
            } catch (Throwable ignored) {
                raw = RootShell.exec("cat '" + file.getAbsolutePath() + "'");
            }
            JSONObject root = new JSONObject(raw);
            if (root.has("advancedMode")) {
                setAdvancedMode(ctx, root.optBoolean("advancedMode", false));
            }
            JSONArray arr = root.getJSONArray("rules");
            List<Rule> imported = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                Rule r = Rule.fromJson(arr.getJSONObject(i));
                if (!overwrite) r.id = UUID.randomUUID().toString();
                imported.add(r);
            }
            if (overwrite) {
                saveAll(ctx, imported);
            } else {
                List<Rule> current = load(ctx);
                current.addAll(0, imported);
                saveAll(ctx, current);
            }
            AppLogger.log("CONFIG", "已导入 " + imported.size() + " 条规则 (" + (overwrite ? "覆盖" : "合并") + ")");
            return imported.size();
        } catch (Throwable e) {
            AppLogger.log("ERROR", "配置导入失败: " + e.getMessage());
            return -1;
        }
    }
}
