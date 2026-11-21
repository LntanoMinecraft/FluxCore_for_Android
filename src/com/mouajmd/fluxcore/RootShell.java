package com.mouajmd.fluxcore;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RootShell {
    public static String exec(String cmd) {
        StringBuilder out = new StringBuilder();
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.write((cmd + "\nexit\n").getBytes(StandardCharsets.UTF_8));
            os.flush();

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                out.append(line).append("\n");
            }
            p.waitFor();
        } catch (Throwable e) {
            return "ERR: " + e.getMessage();
        } finally {
            if (p != null) p.destroy();
        }
        return out.toString().trim();
    }

    public static boolean hasRoot() {
        String res = exec("id");
        return res != null && res.contains("uid=0");
    }

    public static String queryCurrentWifiSsid() {
        String raw = exec("cmd wifi status 2>/dev/null; dumpsys wifi 2>/dev/null | grep -E 'WifiInfo|mWifiInfo|SSID:' | head -n 15");
        if (raw == null || raw.isEmpty()) return "";

        Matcher mQuoted = Pattern.compile("SSID:\\s*\"([^\"]+)\"").matcher(raw);
        while (mQuoted.find()) {
            String s = mQuoted.group(1).trim();
            if (!s.isEmpty() && !s.equalsIgnoreCase("<unknown ssid>")) return s;
        }

        Matcher mUnquoted = Pattern.compile("SSID:\\s*([^,\\s\\n\"<>]+)").matcher(raw);
        while (mUnquoted.find()) {
            String s = mUnquoted.group(1).trim();
            if (!s.isEmpty() && !s.equalsIgnoreCase("null") && !s.equalsIgnoreCase("<unknown")) return s;
        }
        return "";
    }

    public static String queryConnectedBtDevices() {
        String raw = exec("dumpsys bluetooth_manager 2>/dev/null | grep -iE 'Connected:|mActiveDevice|Device:' | head -n 15");
        return raw == null ? "" : raw;
    }
}
