package com.mouajmd.fluxcore;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Rule {
    public static final int TRIG_MANUAL = 0;
    public static final int TRIG_APP = 1;
    public static final int TRIG_CHARGER = 2;
    public static final int TRIG_SCREEN = 3;
    public static final int TRIG_WIFI = 4;
    public static final int TRIG_BT = 5;

    public String id;
    public String name;
    public boolean enabled = true;
    public int triggerType = TRIG_APP;
    public String triggerPkg = "";
    public boolean triggerOn = true;

    public List<String> killPkgs = new ArrayList<>();
    public int dndMode = -1; // -1=不变, 0=关闭, 1=仅重要, 2=完全静音
    public int brightness = -1; // -1=不变, 10..255
    public int refreshRate = 0; // 0=不变, 60, 90, 120
    public int wifiAction = -1; // -1=不变, 1=开启, 0=关闭
    public String customCmd = "";
    public String revertCmd = "";
    public boolean autoRevert = true;
    public boolean active = false;

    public Rule() {
        this.id = UUID.randomUUID().toString();
    }

    public boolean hasStatefulAction() {
        return dndMode > 0 || brightness >= 0 || refreshRate > 0 || wifiAction >= 0 || !revertCmd.trim().isEmpty();
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("name", name);
            o.put("enabled", enabled);
            o.put("triggerType", triggerType);
            o.put("triggerPkg", triggerPkg);
            o.put("triggerOn", triggerOn);
            JSONArray arr = new JSONArray();
            for (String p : killPkgs) arr.put(p);
            o.put("killPkgs", arr);
            o.put("dndMode", dndMode);
            o.put("brightness", brightness);
            o.put("refreshRate", refreshRate);
            o.put("wifiAction", wifiAction);
            o.put("customCmd", customCmd);
            o.put("revertCmd", revertCmd);
            o.put("autoRevert", autoRevert);
            o.put("active", active);
        } catch (Exception ignored) {}
        return o;
    }

    public static Rule fromJson(JSONObject o) {
        Rule r = new Rule();
        r.id = o.optString("id", r.id);
        r.name = o.optString("name", "未命名情境");
        r.enabled = o.optBoolean("enabled", true);
        r.triggerType = o.optInt("triggerType", TRIG_APP);
        r.triggerPkg = o.optString("triggerPkg", "");
        r.triggerOn = o.optBoolean("triggerOn", true);
        JSONArray arr = o.optJSONArray("killPkgs");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                r.killPkgs.add(arr.optString(i));
            }
        }
        r.dndMode = o.optInt("dndMode", -1);
        r.brightness = o.optInt("brightness", -1);
        r.refreshRate = o.optInt("refreshRate", 0);
        r.wifiAction = o.optInt("wifiAction", -1);
        r.customCmd = o.optString("customCmd", "");
        r.revertCmd = o.optString("revertCmd", "");
        r.autoRevert = o.optBoolean("autoRevert", true);
        r.active = o.optBoolean("active", false);
        return r;
    }
}
