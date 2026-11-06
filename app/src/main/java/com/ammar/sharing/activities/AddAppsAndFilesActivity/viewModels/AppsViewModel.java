package com.ammar.sharing.activities.AddAppsAndFilesActivity.viewModels;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ammar.sharing.activities.AddAppsAndFilesActivity.data.AppData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppsViewModel extends AndroidViewModel {
    private static final ExecutorService SCANNER = Executors.newSingleThreadExecutor();
    // Only labels are shared, never mutable picker selection or full-resolution drawables.
    private static final Map<String, String[]> LABELS = new HashMap<>();
    private final MutableLiveData<AppData[]> apps = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private boolean started;
    private volatile boolean cleared;

    public AppsViewModel(@NonNull Application application) { super(application); loadData(); }
    public LiveData<AppData[]> getAppsLiveData() { return apps; }
    public LiveData<Boolean> getIsLoadingLiveData() { return loading; }
    public void loadData() {
        if (started) return;
        started = true;
        SCANNER.execute(() -> {
            ArrayList<AppData> result = new ArrayList<>();
            PackageManager pm = getApplication().getPackageManager();
            try {
                Map<String, String[]> currentLabels = new HashMap<>();
                // No metadata, shared libraries, or uninstalled packages are needed for APK sharing.
                for (ApplicationInfo info : pm.getInstalledApplications(0)) {
                    if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                    try {
                        String[] cached = LABELS.get(info.packageName);
                        String sourceKey = info.sourceDir + java.util.Locale.getDefault().toLanguageTag();
                        String label = cached != null && sourceKey.equals(cached[0]) ? cached[1] : pm.getApplicationLabel(info).toString();
                        currentLabels.put(info.packageName, new String[]{sourceKey, label});
                        AppData data = new AppData(info.packageName, info.splitPublicSourceDirs != null, label, info.sourceDir);
                        if (info.splitPublicSourceDirs != null) for (String split : info.splitPublicSourceDirs) {
                            try { android.system.StructStat stat = android.system.Os.stat(split); data.size += stat.st_size; data.modified = Math.max(data.modified, stat.st_mtime * 1000); }
                            catch (android.system.ErrnoException ignored) { /* A split may disappear during an app update. */ }
                        }
                        result.add(data);
                    } catch (RuntimeException ignored) { /* Package can disappear during enumeration. */ }
                }
                LABELS.clear(); LABELS.putAll(currentLabels);
            } catch (RuntimeException ignored) { /* Show an empty list instead of a permanent spinner. */ }
            java.util.Collections.sort(result, (a, b) -> a.searchKey.compareTo(b.searchKey));
            new Handler(Looper.getMainLooper()).post(() -> {
                if (cleared) return;
                apps.setValue(result.toArray(new AppData[0]));
                loading.setValue(false);
            });
        });
    }
    public void reset() {
        AppData[] current = apps.getValue();
        if (current != null) for (AppData app : current) app.isChecked = false;
    }
    @Override protected void onCleared() { cleared = true; }
}
