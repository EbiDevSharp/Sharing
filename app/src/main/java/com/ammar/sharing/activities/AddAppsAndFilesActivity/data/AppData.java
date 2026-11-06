package com.ammar.sharing.activities.AddAppsAndFilesActivity.data;

import com.ammar.sharing.common.utils.AppSearch;

/** Lightweight metadata. Icons are loaded only for visible rows. Selection belongs to one picker. */
public class AppData {
    public AppData(String packageName, boolean hasSplits, String label) {
        this.packageName = packageName;
        this.hasSplits = hasSplits;
        this.label = label;
        this.searchKey = AppSearch.normalize(label + " " + packageName);
    }
    public AppData(String packageName, boolean hasSplits, String label, String source) {
        this(packageName,hasSplits,label);
        try { android.system.StructStat stat=android.system.Os.stat(source); size=stat.st_size; modified=stat.st_mtime*1000; } catch(android.system.ErrnoException ignored) {}
    }
    public long size,modified;
    public final String packageName;
    public final String label;
    public final String searchKey;
    public final boolean hasSplits;
    public boolean isChecked;
}
