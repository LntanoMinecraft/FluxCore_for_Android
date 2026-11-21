package com.mouajmd.fluxcore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

public class Rule {
    public static final int TRIG_MANUAL = 0;
    public static final int TRIG_APP = 1;
    public static final int TRIG_CHARGING = 2;
    public static final int TRIG_SCREEN = 3;
    public static final int TRIG_WIFI = 4;
    public static final int TRIG_BT = 5;

    public static class Condition {
        public int type = TRIG_CHARGING;
        public boolean stateOn = true;
        public List<String> pkgs = new ArrayList<>();
        public String targetName = "";

        public JSONObject toJson() throws Exception {
            JSONObject o = new JSONObject();
            o.put("type", type);
            o.put("stateOn", stateOn);
            o.put("targetName", targetName == null ? "" : targetName);
            JSONArray arr = new JSONArray();
            for (String p : pkgs) arr.put(p);
            o.put("pkgs", arr);
            return o;
        }

        public static Condition fromJson(JSONObject o) {
            Condition c = new Condition();
            c.type = o.optInt("type", TRIG_CHARGING);
            c.stateOn = o.optBoolean("stateOn", true);
            c.targetName = o.optString("targetName", "");
            JSONArray arr = o.optJSONArray("pkgs");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    String s = arr.optString(i, "");
                    if (!s.isEmpty()) c.pkgs.add(s);
                }
            }
            return c;
        }

        public String describe() {
            String dir = stateOn ? "开启/连接" : "关闭/断开";
            switch (type) {
                case TRIG_APP:
                    return (stateOn ? "前台包含 " : "不在前台 ") + pkgs.size() + " 个应用";
                case TRIG_CHARGING:
                    return stateOn ? "正在充电" : "未在充电";
                case TRIG_SCREEN:
                    return stateOn ? "屏幕亮起" : "屏幕熄灭";
                case TRIG_WIFI:
                    if (targetName != null && !targetName.trim().isEmpty()) {
                        return (stateOn ? "已连 Wi-Fi「" : "未连 Wi-Fi「") + targetName.trim() + "」";
                    }
                    return "Wi-Fi " + dir;
                case TRIG_BT:
                    if (targetName != null && !targetName.trim().isEmpty()) {
                        return (stateOn ? "已连蓝牙「" : "未连蓝牙「") + targetName.trim() + "」";
                    }
                    return "蓝牙 " + dir;
                default:
                    return "条件";
            }
        }
    }

    public String id = UUID.randomUUID().toString();
    public String name = "未命名情境";
    public boolean enabled = true;

    public int triggerType = TRIG_MANUAL;
    public List<String> triggerPkgs = new ArrayList<>();
    public String triggerTarget = "";
    public boolean triggerOn = true;
    public List<Condition> extraConditions = new ArrayList<>();

    public List<String> killPkgs = new ArrayList<>();
    public int dndMode = -1;
    public int refreshRate = 0;
    public int brightness = -1;
    public int wifiAction = -1;
    public String customCmd = "";
    public String revertCmd = "";
    public boolean autoRevert = true;

    public JSONObject toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("enabled", enabled);
        o.put("triggerType", triggerType);
        o.put("triggerTarget", triggerTarget == null ? "" : triggerTarget);
        o.put("triggerOn", triggerOn);

        JSONArray tArr = new JSONArray();
        for (String p : triggerPkgs) tArr.put(p);
        o.put("triggerPkgs", tArr);

        JSONArray cArr = new JSONArray();
        for (Condition c : extraConditions) cArr.put(c.toJson());
        o.put("extraConditions", cArr);

        JSONArray kArr = new JSONArray();
        for (String p : killPkgs) kArr.put(p);
        o.put("killPkgs", kArr);

        o.put("dndMode", dndMode);
        o.put("refreshRate", refreshRate);
        o.put("brightness", brightness);
        o.put("wifiAction", wifiAction);
        o.put("customCmd", customCmd);
        o.put("revertCmd", revertCmd);
        o.put("autoRevert", autoRevert);
        return o;
    }

    public static Rule fromJson(JSONObject o) {
        Rule r = new Rule();
        r.id = o.optString("id", UUID.randomUUID().toString());
        r.name = o.optString("name", "未命名情境");
        r.enabled = o.optBoolean("enabled", true);
        r.triggerType = o.optInt("triggerType", TRIG_MANUAL);
        r.triggerTarget = o.optString("triggerTarget", "");
        r.triggerOn = o.optBoolean("triggerOn", true);

        JSONArray tArr = o.optJSONArray("triggerPkgs");
        if (tArr != null) {
            for (int i = 0; i < tArr.length(); i++) {
                String s = tArr.optString(i, "");
                if (!s.isEmpty()) r.triggerPkgs.add(s);
            }
        }
        String legacyPkg = o.optString("triggerPkg", "");
        if (!legacyPkg.isEmpty() && !r.triggerPkgs.contains(legacyPkg)) {
            r.triggerPkgs.add(legacyPkg);
        }

        JSONArray cArr = o.optJSONArray("extraConditions");
        if (cArr != null) {
            for (int i = 0; i < cArr.length(); i++) {
                JSONObject co = cArr.optJSONObject(i);
                if (co != null) r.extraConditions.add(Condition.fromJson(co));
            }
        }

        JSONArray kArr = o.optJSONArray("killPkgs");
        if (kArr != null) {
            for (int i = 0; i < kArr.length(); i++) {
                String ks = kArr.optString(i, ""); if (!ks.isEmpty()) r.killPkgs.add(ks);
            }
        }
        r.dndMode = o.optInt("dndMode", -1);
        r.refreshRate = o.optInt("refreshRate", 0);
        r.brightness = o.optInt("brightness", -1);
        r.wifiAction = o.optInt("wifiAction", -1);
        r.customCmd = o.optString("customCmd", "");
        r.revertCmd = o.optString("revertCmd", "");
        r.autoRevert = o.optBoolean("autoRevert", true);
        return r;
    }

    public String describeTrigger() {
        String base;
        switch (triggerType) {
            case TRIG_APP:
                if (triggerPkgs.size() == 1) {
                    base = (triggerOn ? "打开 " : "退出 ") + triggerPkgs.get(0);
                } else {
                    base = (triggerOn ? "打开任意 " : "退出全部 ") + triggerPkgs.size() + " 个指定应用";
                }
                break;
            case TRIG_CHARGING:
                base = triggerOn ? "连接充电器" : "断开充电器";
                break;
            case TRIG_SCREEN:
                base = triggerOn ? "屏幕亮起" : "屏幕熄灭";
                break;
            case TRIG_WIFI:
                if (triggerTarget != null && !triggerTarget.trim().isEmpty()) {
                    base = (triggerOn ? "连接 Wi-Fi「" : "断开 Wi-Fi「") + triggerTarget.trim() + "」";
                } else {
                    base = triggerOn ? "任意 Wi-Fi 已连接" : "Wi-Fi 已断开";
                }
                break;
            case TRIG_BT:
                if (triggerTarget != null && !triggerTarget.trim().isEmpty()) {
                    base = (triggerOn ? "连接蓝牙「" : "断开蓝牙「") + triggerTarget.trim() + "」";
                } else {
                    base = triggerOn ? "任意蓝牙已连接" : "蓝牙已断开";
                }
                break;
            default:
                base = "仅手动主动触发";
                break;
        }
        if (!extraConditions.isEmpty()) {
            StringBuilder sb = new StringBuilder(base);
            for (Condition c : extraConditions) {
                sb.append(" 且 ").append(c.describe());
            }
            return sb.toString();
        }
        return base;
    }

    public String describeActions() {
        List<String> list = new ArrayList<>();
        if (!killPkgs.isEmpty()) list.add("强停 " + killPkgs.size() + " 个应用");
        if (dndMode >= 0) list.add(dndMode == 0 ? "关闭勿扰" : "开启勿扰");
        if (refreshRate > 0) list.add("刷新率 " + refreshRate + "Hz");
        if (brightness >= 0) list.add("亮度 " + brightness);
        if (wifiAction >= 0) list.add(wifiAction == 1 ? "开启 Wi-Fi" : "关闭 Wi-Fi");
        if (customCmd != null && !customCmd.trim().isEmpty()) list.add("自定义 Root 指令");
        return list.isEmpty() ? "无执行动作" : String.join(" · ", list);
    }
}
