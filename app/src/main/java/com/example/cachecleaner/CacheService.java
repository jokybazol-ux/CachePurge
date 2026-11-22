package com.example.cachecleaner;

import android.os.RemoteException;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CacheService extends ICacheService.Stub {

    public CacheService() {}

    @Override
    public List<AppCacheInfo> getCacheSizes(List<String> packageNames) throws RemoteException {
        List<AppCacheInfo> results = new ArrayList<>();
        if (packageNames == null || packageNames.isEmpty()) return results;

        for (String pkg : packageNames) {
            try {
                long size = getDirSize(new File("/sdcard/Android/data/" + pkg + "/cache"));
                Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", "du -sb /data/user/0/" + pkg + "/cache /data/user/0/" + pkg + "/code_cache 2>/dev/null"});
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length > 0) {
                        try { size += Long.parseLong(parts[0]); } catch (NumberFormatException ignored) {}
                    }
                }
                p.waitFor();
                AppCacheInfo info = new AppCacheInfo();
                info.packageName = pkg;
                info.cacheSizeBytes = size;
                results.add(info);
            } catch (Exception ignored) {}
        }
        return results;
    }

    @Override
    public void purgeSelectedCaches(List<String> packageNames) throws RemoteException {
        if (packageNames == null || packageNames.isEmpty()) return;
        try {
            Runtime.getRuntime().exec(new String[]{"pm", "trim-caches", "999G"}).waitFor();
            for (String pkg : packageNames) {
                Runtime.getRuntime().exec(new String[]{"sh", "-c", "rm -rf /sdcard/Android/data/" + pkg + "/cache/* /data/user/0/" + pkg + "/cache/* /data/user/0/" + pkg + "/code_cache/* 2>/dev/null"}).waitFor();
            }
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
    }

    private long getDirSize(File dir) {
        if (dir == null || !dir.exists()) return 0;
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                size += f.isDirectory() ? getDirSize(f) : f.length();
            }
        }
        return size;
    }

    @Override
    public void destroy() { System.exit(0); }
}
