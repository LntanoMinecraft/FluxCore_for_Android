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
    public static final int TRIG_BATTERY = 6;     // 电量阈值
    public static final int TRIG_HEADSET = 7;     // 有线耳机插入
    public static final int TRIG_ORIENTATION = 8; // 横竖屏状态
    public static final int TRIG_TIME = 9;        // 时间段约束

    public static class Condition {
        public int type = TRIG_CHARGING;
        public boolean stateOn = true;
        public List<String> pkgs = new ArrayList<>();
        public String targetName = ""; // Wi-Fi SSID / 蓝牙名 / 时间段(如 23:00-07:00)
        public int threshold = 20;     // 电量百分比阈值

        public JSONObject toJson() throws Exception {
            JSONObject o = new JSONObject();
            o.put("type", type);
            o.put("stateOn", stateOn);
            o.put("targetName", targetName == null ? "" : targetName);
            o.put("threshold", threshold);
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
            c.threshold = o.optInt("threshold", 20);
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
                case TRIG_BATTERY:
                    return (stateOn ? "电量高于 " : "电量低于 ") + threshold + "%";
                case TRIG_HEADSET:
                    return stateOn ? "已插入有线耳机" : "未插入有线耳机";
                case TRIG_ORIENTATION:
                    return stateOn ? "屏幕处于横屏" : "屏幕处于竖屏";
                case TRIG_TIME:
                    return "时间段 " + (targetName.isEmpty() ? "23:00-07:00" : targetName);
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
    public int triggerThreshold = 20;
    public boolean triggerOn = true;
    public List<Condition> extraConditions = new ArrayList<>();

    // 执行动作
    public String tileComponent = ""; // 状态栏磁贴 Component (pkg/cls)
    public String tileLabel = "";     // 磁贴显示名称
    public List<String> killPkgs = new ArrayList<>();
    public int dndMode = -1;          // -1不改, 0关闭, 1重要打扰, 2完全静音
    public int refreshRate = 0;       // 0不改, 60, 90, 120
    public int brightness = -1;       // -1不改, 0~255
    public int wifiAction = -1;       // -1不改, 1开, 0关
    public int mobileDataAction = -1; // -1不改, 1开, 0关
    public int airplaneAction = -1;   // -1不改, 1开, 0关
    public int batterySaverAction = -1; // -1不改, 1开, 0关
    public int darkModeAction = -1;   // -1不改, 1深色, 0浅色
    public int autoRotateAction = -1; // -1不改, 1开启旋转, 0锁定竖屏
    public int mediaVolume = -1;      // -1不改, 0~100 (%)
    public boolean vibrateTrigger = false;
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
        o.put("triggerThreshold", triggerThreshold);
        o.put("triggerOn", triggerOn);

        JSONArray tArr = new JSONArray();
        for (String p : triggerPkgs) tArr.put(p);
        o.put("triggerPkgs", tArr);

        JSONArray cArr = new JSONArray();
        for (Condition c : extraConditions) cArr.put(c.toJson());
        o.put("extraConditions", cArr);

        o.put("tileComponent", tileComponent == null ? "" : tileComponent);
        o.put("tileLabel", tileLabel == null ? "" : tileLabel);

        JSONArray kArr = new JSONArray();
        for (String p : killPkgs) kArr.put(p);
        o.put("killPkgs", kArr);

        o.put("dndMode", dndMode);
        o.put("refreshRate", refreshRate);
        o.put("brightness", brightness);
        o.put("wifiAction", wifiAction);
        o.put("mobileDataAction", mobileDataAction);
        o.put("airplaneAction", airplaneAction);
        o.put("batterySaverAction", batterySaverAction);
        o.put("darkModeAction", darkModeAction);
        o.put("autoRotateAction", autoRotateAction);
        o.put("mediaVolume", mediaVolume);
        o.put("vibrateTrigger", vibrateTrigger);
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
        r.triggerThreshold = o.optInt("triggerThreshold", 20);
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

        r.tileComponent = o.optString("tileComponent", "");
        r.tileLabel = o.optString("tileLabel", "");

        JSONArray kArr = o.optJSONArray("killPkgs");
        if (kArr != null) {
            for (int i = 0; i < kArr.length(); i++) {
                String ks = kArr.optString(i, "");
                if (!ks.isEmpty()) r.killPkgs.add(ks);
            }
        }
        r.dndMode = o.optInt("dndMode", -1);
        r.refreshRate = o.optInt("refreshRate", 0);
        r.brightness = o.optInt("brightness", -1);
        r.wifiAction = o.optInt("wifiAction", -1);
        r.mobileDataAction = o.optInt("mobileDataAction", -1);
        r.airplaneAction = o.optInt("airplaneAction", -1);
        r.batterySaverAction = o.optInt("batterySaverAction", -1);
        r.darkModeAction = o.optInt("darkModeAction", -1);
        r.autoRotateAction = o.optInt("autoRotateAction", -1);
        r.mediaVolume = o.optInt("mediaVolume", -1);
        r.vibrateTrigger = o.optBoolean("vibrateTrigger", false);
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
            case TRIG_BATTERY:
                base = (triggerOn ? "电量 ≥ " : "电量 ≤ ") + triggerThreshold + "%";
                break;
            case TRIG_HEADSET:
                base = triggerOn ? "插入有线耳机" : "拔出有线耳机";
                break;
            case TRIG_ORIENTATION:
                base = triggerOn ? "屏幕横屏" : "屏幕竖屏";
                break;
            case TRIG_TIME:
                base = "时间段 " + (triggerTarget == null || triggerTarget.trim().isEmpty() ? "23:00-07:00" : triggerTarget.trim());
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
        if (tileComponent != null && !tileComponent.trim().isEmpty()) {
            String lbl = (tileLabel != null && !tileLabel.isEmpty()) ? tileLabel : tileComponent;
            list.add("切换磁贴[" + lbl + "]");
        }
        if (!killPkgs.isEmpty()) list.add("强停 " + killPkgs.size() + " 个应用");
        if (dndMode >= 0) list.add(dndMode == 0 ? "关闭勿扰" : "开启勿扰");
        if (refreshRate > 0) list.add("刷新率 " + refreshRate + "Hz");
        if (brightness >= 0) list.add("亮度 " + brightness);
        if (wifiAction >= 0) list.add(wifiAction == 1 ? "开启 Wi-Fi" : "关闭 Wi-Fi");
        if (mobileDataAction >= 0) list.add(mobileDataAction == 1 ? "开启数据" : "关闭数据");
        if (airplaneAction >= 0) list.add(airplaneAction == 1 ? "开启飞行模式" : "关闭飞行模式");
        if (batterySaverAction >= 0) list.add(batterySaverAction == 1 ? "开启省电" : "关闭省电");
        if (darkModeAction >= 0) list.add(darkModeAction == 1 ? "深色模式" : "浅色模式");
        if (autoRotateAction >= 0) list.add(autoRotateAction == 1 ? "允许旋转" : "锁定竖屏");
        if (mediaVolume >= 0) list.add("媒体音量 " + mediaVolume + "%");
        if (vibrateTrigger) list.add("震动提示");
        if (customCmd != null && !customCmd.trim().isEmpty()) list.add("自定义 Root 指令");
        return list.isEmpty() ? "无执行动作" : String.join(" · ", list);
    }
}
