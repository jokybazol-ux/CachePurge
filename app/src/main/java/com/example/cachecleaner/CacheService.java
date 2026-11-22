package com.example.cachecleaner;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Process;
import android.os.storage.StorageManager;
import android.app.usage.IStorageStatsManager;
import android.app.usage.StorageStats;
import android.content.Context;
import android.content.pm.IPackageManager;
import java.util.ArrayList;
import java.util.List;

public class CacheService extends ICacheService.Stub {

    public CacheService() {}

    @Override
    public List<AppCacheInfo> getCacheSizes(List<String> packageNames) throws RemoteException {
        List<AppCacheInfo> results = new ArrayList<>();
        if (packageNames == null || packageNames.isEmpty()) return results;

        try {
            IBinder binder = ServiceManager.getService(Context.STORAGE_STATS_SERVICE);
            IStorageStatsManager statsManager = IStorageStatsManager.Stub.asInterface(binder);
            String volumeUuid = StorageManager.UUID_PRIVATE_INTERNAL;
            int userId = Process.myUserHandle().hashCode();

            for (String pkg : packageNames) {
                try {
                    StorageStats stats = statsManager.queryStatsForPackage(volumeUuid, pkg, userId, "com.android.shell");
                    AppCacheInfo info = new AppCacheInfo();
                    info.packageName = pkg;
                    info.cacheSizeBytes = stats.getCacheBytes();
                    results.add(info);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
        return results;
    }

    @Override
    public void purgeSelectedCaches(List<String> packageNames) throws RemoteException {
        if (packageNames == null || packageNames.isEmpty()) return;
        try {
            IBinder binder = ServiceManager.getService("package");
            IPackageManager pm = IPackageManager.Stub.asInterface(binder);
            int userId = Process.myUserHandle().hashCode();
            if (pm != null) {
                for (String pkg : packageNames) {
                    try {
                        pm.deleteApplicationCacheFilesAsUser(pkg, userId, null);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void destroy() { System.exit(0); }
}
