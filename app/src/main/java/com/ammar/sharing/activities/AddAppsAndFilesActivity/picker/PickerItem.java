package com.ammar.sharing.activities.AddAppsAndFilesActivity.picker;

import android.net.Uri;
import com.ammar.sharing.common.utils.AppSearch;

public final class PickerItem {
  public final String id, name, mime, path, packageName, searchKey;
  public final Uri uri;
  public final long size, modified;
  public final boolean directory, splits;

  public PickerItem(
      String id,
      String name,
      String mime,
      String path,
      String packageName,
      Uri uri,
      long size,
      long modified,
      boolean directory,
      boolean splits) {
    this.id = id;
    this.name = name == null || name.isEmpty() ? id : name;
    this.mime = mime == null ? "application/octet-stream" : mime;
    this.path = path;
    this.packageName = packageName;
    this.uri = uri;
    this.size = size;
    this.modified = modified;
    this.directory = directory;
    this.splits = splits;
    this.searchKey = AppSearch.normalize(this.name + " " + (packageName == null ? "" : packageName));
  }

  public boolean isMedia() {
    return mime.startsWith("image/") || mime.startsWith("video/") || mime.startsWith("audio/");
  }
}
