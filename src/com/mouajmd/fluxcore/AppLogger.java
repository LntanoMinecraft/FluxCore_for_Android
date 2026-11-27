package com.mouajmd.fluxcore;

import android.content.Context;
import android.os.Build;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

public class AppLogger {
    private static final int MAX_LINES = 600;
    private static final LinkedList<String> BUFFER = new LinkedList<>();
    private static final SimpleDateFormat FMT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US);
    private static final SimpleDateFormat FILE_FMT = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);

    public static synchronized void log(String tag, String msg) {
        String line = "[" + FMT.format(new Date()) + "] [" + tag + "] " + msg;
        BUFFER.addLast(line);
        while (BUFFER.size() > MAX_LINES) {
            BUFFER.removeFirst();
        }
    }

    public static synchronized String exportToDownload(Context ctx) {
        String dirPath = "/storage/emulated/0/Download/FluxCore";
        String fileName = "FluxCore_log_" + FILE_FMT.format(new Date()) + ".txt";
        String fullPath = dirPath + "/" + fileName;

        StringBuilder sb = new StringBuilder();
        sb.append("=== 流枢 · FluxCore v1.2.0 运行日志 ===\n");
        sb.append("导出时间: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date())).append("\n");
        sb.append("设备型号: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL)
          .append(" (Android ").append(Build.VERSION.RELEASE).append(" / SDK ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("Root 状态: ").append(RootShell.hasRoot() ? "已授权" : "未授权").append("\n");
        sb.append("========================================\n\n");
        for (String line : BUFFER) {
            sb.append(line).append("\n");
        }

        try {
            File tmp = new File(ctx.getCacheDir(), fileName);
            FileOutputStream fos = new FileOutputStream(tmp);
            fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            fos.close();

            File outDir = new File(dirPath);
            if (!outDir.exists()) outDir.mkdirs();
            File outFile = new File(outDir, fileName);
            try {
                FileOutputStream outFos = new FileOutputStream(outFile);
                outFos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                outFos.close();
                log("EXPORT", "日志已导出至: " + fullPath);
                return fullPath;
            } catch (Throwable ignored) {
                RootShell.exec("mkdir -p '" + dirPath + "' && cp -f '" + tmp.getAbsolutePath() + "' '" + fullPath + "' && chmod 664 '" + fullPath + "'");
                log("EXPORT", "日志已通过 Root 导出至: " + fullPath);
                return fullPath;
            }
        } catch (Throwable e) {
            log("ERROR", "日志导出失败: " + e.getMessage());
            return null;
        }
    }
}
