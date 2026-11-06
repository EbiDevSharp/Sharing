package com.ammar.sharing.activities.AddAppsAndFilesActivity.picker;

import android.app.Application;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Owns picker selection across navigation, filtering, view changes, previews and rotation. */
public class PickerViewModel extends AndroidViewModel {
  public final MutableLiveData<List<PickerItem>> items = new MutableLiveData<>(new ArrayList<>());
  public final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
  public final MutableLiveData<Boolean> failed = new MutableLiveData<>(false);
  public final LinkedHashMap<String, PickerItem> selected = new LinkedHashMap<>();
  public final MutableLiveData<Integer> selectionCount = new MutableLiveData<>(0);
  public int tab = -1;
  public File folder = Environment.getExternalStorageDirectory();
  public String query = "";
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private final Handler main = new Handler(Looper.getMainLooper());
  private int generation;
  private volatile boolean cleared;

  public PickerViewModel(@NonNull Application app) {
    super(app);
  }

  public void toggle(PickerItem item) {
    if (selected.remove(item.id) == null) selected.put(item.id, item);
    selectionCount.setValue(selected.size());
  }

  public void select(PickerItem item) {
    selected.put(item.id, item);
    selectionCount.setValue(selected.size());
  }

  public void load(int nextTab, File nextFolder) {
    tab = nextTab;
    folder = nextFolder;
    int request = ++generation;
    items.setValue(new ArrayList<>());
    loading.setValue(true);
    failed.setValue(false);
    worker.execute(
        () -> {
          List<PickerItem> result = new ArrayList<>();
          boolean error = false;
          try {
            if (nextTab == 0) {
              File[] files = nextFolder.listFiles();
              if (files == null) error = true;
              else
                for (File f : files) {
                  if (f.getName().startsWith(".")) continue;
                  String ext = extension(f.getName());
                  String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                  try {
                    android.system.StructStat stat = android.system.Os.stat(f.getAbsolutePath());
                    result.add(
                        new PickerItem(
                            f.getAbsolutePath(),
                            f.getName(),
                            mime,
                            f.getAbsolutePath(),
                            null,
                            Uri.fromFile(f),
                            stat.st_size,
                            stat.st_mtime * 1000,
                            android.system.OsConstants.S_ISDIR(stat.st_mode),
                            false));
                  } catch (android.system.ErrnoException ignored) {
                    /* A file may be removed during listing. */
                  }
                }
            } else {
              Uri collection =
                  nextTab == 2
                      ? MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                      : nextTab == 3
                          ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                          : MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
              String[] projection = {
                "_id", "_display_name", "_size", "date_modified", "mime_type", "_data"
              };
              try (Cursor c =
                  getApplication()
                      .getContentResolver()
                      .query(collection, projection, null, null, null)) {
                if (c != null)
                  while (c.moveToNext()) {
                    Uri uri = ContentUris.withAppendedId(collection, c.getLong(0));
                    String mediaPath = c.getString(5);
                    result.add(
                        new PickerItem(
                            mediaPath == null ? uri.toString() : mediaPath,
                            c.getString(1),
                            c.getString(4),
                            mediaPath,
                            null,
                            uri,
                            c.getLong(2),
                            c.getLong(3) * 1000,
                            false,
                            false));
                  }
              }
            }
          } catch (RuntimeException e) {
            error = true;
          }
          final boolean finalError = error;
          main.post(
              () -> {
                if (!cleared && request == generation) {
                  items.setValue(result);
                  failed.setValue(finalError);
                  loading.setValue(false);
                }
              });
        });
  }

  public void waitForApps() {
    ++generation;
    tab = 1;
    items.setValue(new ArrayList<>());
    loading.setValue(true);
    failed.setValue(false);
  }

  public void showApps(List<PickerItem> apps) {
    ++generation;
    tab = 1;
    items.setValue(apps);
    loading.setValue(false);
    failed.setValue(false);
  }

  public static String extension(String name) {
    int dot = name.lastIndexOf('.');
    return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
  }

  @Override
  protected void onCleared() {
    cleared = true;
    worker.shutdownNow();
  }
}
