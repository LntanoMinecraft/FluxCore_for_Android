package com.mouajmd.fluxcore;

import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class RootShell {
    public interface Callback {
        void onResult(boolean success, String output);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static boolean isRooted() {
        Result r = exec("id");
        return r.success && r.output.contains("uid=0");
    }

    public static class Result {
        public boolean success;
        public String output;
        public Result(boolean s, String o) { success = s; output = o; }
    }

    public static synchronized Result exec(String cmd) {
        Process p = null;
        DataOutputStream os = null;
        BufferedReader br = null;
        try {
            p = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(p.getOutputStream());
            os.writeBytes(cmd + "\nexit\n");
            os.flush();
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            int code = p.waitFor();
            return new Result(code == 0, sb.toString().trim());
        } catch (Exception e) {
            return new Result(false, e.getMessage() == null ? "error" : e.getMessage());
        } finally {
            try { if (os != null) os.close(); } catch (Exception ignored) {}
            try { if (br != null) br.close(); } catch (Exception ignored) {}
            if (p != null) p.destroy();
        }
    }

    public static void execAsync(final String cmd, final Callback cb) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Result r = exec(cmd);
                if (cb != null) {
                    MAIN.post(new Runnable() {
                        @Override
                        public void run() {
                            cb.onResult(r.success, r.output);
                        }
                    });
                }
            }
        }).start();
    }
}
