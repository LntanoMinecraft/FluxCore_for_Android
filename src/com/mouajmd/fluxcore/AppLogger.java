package com.mouajmd.fluxcore;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
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

    public static File resolvePublicDownloadDir() {
        try {
            File dl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (dl != null) return new File(dl, "FluxCore");
        } catch (Throwable ignored) {}
        return new File("/storage/emulated/0/Download/FluxCore");
    }

    private static File resolveFallbackDir(Context ctx) {
        File ext = ctx.getExternalFilesDir("logs");
        return ext != null ? ext : new File(ctx.getFilesDir(), "logs");
    }

    public static synchronized String exportToDownload(Context ctx) {
        String fileName = "FluxCore_log_" + FILE_FMT.format(new Date()) + ".txt";
        File outDir = resolvePublicDownloadDir();
        String dirPath = outDir.getAbsolutePath();
        String fullPath = dirPath + "/" + fileName;

        StringBuilder sb = new StringBuilder();
        sb.append("=== 流枢 · FluxCore v1.1.0 运行日志 ===\n");
        sb.append("导出时间: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date())).append("\n");
        sb.append("设备型号: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL)
          .append(" (Android ").append(Build.VERSION.RELEASE).append(" / SDK ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("Root 状态: ").append(RootShell.hasRoot() ? "已授权" : "未授权").append("\n");
        sb.append("========================================\n\n");
        for (String line : BUFFER) {
            sb.append(line).append("\n");
        }
        byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);

        try {
            File tmp = new File(ctx.getCacheDir(), fileName);
            try (FileOutputStream fos = new FileOutputStream(tmp)) {
                fos.write(data);
            }

            // 1. 优先尝试常规文件写入公共 Download/FluxCore
            try {
                if (!outDir.exists()) outDir.mkdirs();
                File outFile = new File(outDir, fileName);
                try (FileOutputStream outFos = new FileOutputStream(outFile)) {
                    outFos.write(data);
                }
                if (outFile.exists() && outFile.length() > 0) {
                    log("EXPORT", "日志已导出至: " + fullPath);
                    return fullPath;
                }
            } catch (Throwable ignored) {}

            // 2. 分区存储限制时通过 Root 建目录并复制
            if (RootShell.hasRoot()) {
                RootShell.exec("mkdir -p '" + dirPath + "' && cp -f '" + tmp.getAbsolutePath() + "' '" + fullPath + "' && chmod 664 '" + fullPath + "'");
                String chk = RootShell.exec("[ -s '" + fullPath + "' ] && echo OK");
                if (chk != null && chk.contains("OK")) {
                    log("EXPORT", "日志已通过 Root 导出至: " + fullPath);
                    return fullPath;
                }
            }

            // 3. 无 Root 且公共目录不可写时，自动回退至应用专属外部目录
            File fbDir = resolveFallbackDir(ctx);
            if (!fbDir.exists()) fbDir.mkdirs();
            File fbFile = new File(fbDir, fileName);
            try (FileOutputStream fbFos = new FileOutputStream(fbFile)) {
                fbFos.write(data);
            }
            String fbPath = fbFile.getAbsolutePath();
            log("EXPORT", "日志已回退导出至: " + fbPath);
            return fbPath;
        } catch (Throwable e) {
            log("ERROR", "日志导出失败: " + e.getMessage());
            return null;
        }
    }
}
