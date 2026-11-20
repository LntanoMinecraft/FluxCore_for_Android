package com.mouajmd.fluxcore;

import android.content.Context;
import org.json.JSONArray;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RuleStore {
    private static final String FILE_NAME = "flux_rules.json";

    public static synchronized List<Rule> load(Context ctx) {
        List<Rule> list = new ArrayList<>();
        File f = new File(ctx.getFilesDir(), FILE_NAME);
        if (!f.exists()) {
            Rule r1 = new Rule();
            r1.name = "沉浸专注模式";
            r1.triggerType = Rule.TRIG_MANUAL;
            r1.dndMode = 1;
            r1.refreshRate = 60;
            list.add(r1);

            Rule r2 = new Rule();
            r2.name = "息屏深度净化";
            r2.triggerType = Rule.TRIG_SCREEN;
            r2.triggerOn = false;
            r2.autoRevert = false;
            r2.customCmd = "sync && echo 3 > /proc/sys/vm/drop_caches";
            list.add(r2);

            save(ctx, list);
            return list;
        }
        try (FileInputStream fis = new FileInputStream(f)) {
            byte[] buf = new byte[(int) f.length()];
            int read = fis.read(buf);
            if (read > 0) {
                JSONArray arr = new JSONArray(new String(buf, 0, read, StandardCharsets.UTF_8));
                for (int i = 0; i < arr.length(); i++) {
                    list.add(Rule.fromJson(arr.getJSONObject(i)));
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static synchronized void save(Context ctx, List<Rule> list) {
        JSONArray arr = new JSONArray();
        for (Rule r : list) arr.put(r.toJson());
        File f = new File(ctx.getFilesDir(), FILE_NAME);
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(arr.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
    }

    public static synchronized void upsert(Context ctx, Rule rule) {
        List<Rule> list = load(ctx);
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(rule.id)) {
                list.set(i, rule);
                found = true;
                break;
            }
        }
        if (!found) list.add(0, rule);
        save(ctx, list);
    }

    public static synchronized void delete(Context ctx, String id) {
        List<Rule> list = load(ctx);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(id)) {
                list.remove(i);
                break;
            }
        }
        save(ctx, list);
    }
}
