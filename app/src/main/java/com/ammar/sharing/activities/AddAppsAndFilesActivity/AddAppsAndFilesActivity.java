package com.ammar.sharing.activities.AddAppsAndFilesActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ammar.sharing.R;
import com.ammar.sharing.activities.AddAppsAndFilesActivity.data.AppData;
import com.ammar.sharing.activities.AddAppsAndFilesActivity.picker.*;
import com.ammar.sharing.activities.AddAppsAndFilesActivity.viewModels.AppsViewModel;
import com.ammar.sharing.common.utils.AppAppearance;
import com.ammar.sharing.common.utils.AppSearch;
import com.ammar.sharing.custom.ui.DefaultActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddAppsAndFilesActivity extends DefaultActivity {
  public static final String EXTRA_INITIAL_TAB = "initial_tab";
  public static final String EXTRA_APPS_SHARED = "EXTRA_APPS_SHARED",
      EXTRA_URIS_SHARED = "EXTRA_URIS_SHARED",
      EXTRA_FILE_PATHS_SHARED = "EXTRA_FILE_PATHS_SHARED";
  public static final String EXTRA_PACKAGES_NAMES = "EXTRA_PACKAGES_NAMES",
      EXTRA_FILES_PATHS = "EXTRA_FILES_PATHS",
      EXTRA_URIS = "EXTRA_URIS";
  private PickerViewModel model;
  private AppsViewModel apps;
  private PickerAdapter adapter;
  private RecyclerView recycler;
  private EditText search;
  private TextView path, empty, count;
  private Button add, sort, gridButton, listButton;
  private View loading;
  private Toolbar toolbar;
  private SharedPreferences prefs;
  private boolean grid;
  private int sortField;
  private boolean descending;
  private int filterGeneration;
  private boolean destroyed;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private Runnable pendingFilter;
  private final ExecutorService filterWorker = Executors.newSingleThreadExecutor();
  private final int[] tabLabels = {
    R.string.files, R.string.apps, R.string.images, R.string.videos, R.string.audio
  };
  private final int[] sortLabels = {
    R.string.date_modified, R.string.size, R.string.name, R.string.type
  };
  private final ActivityResultLauncher<String[]> documents =
      registerForActivityResult(
          new ActivityResultContracts.OpenMultipleDocuments(),
          uris -> {
            if (model == null) return;
            for (Uri uri : uris) {
              try {
                getContentResolver()
                    .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
              } catch (SecurityException ignored) {
              }
              // Resolve names/sizes off the UI thread; keep the current picker session open.
              filterWorker.execute(
                  () -> {
                    String name = uri.getLastPathSegment();
                    long size = 0;
                    try (android.database.Cursor c =
                        getContentResolver()
                            .query(
                                uri,
                                new String[] {
                                  android.provider.OpenableColumns.DISPLAY_NAME,
                                  android.provider.OpenableColumns.SIZE
                                },
                                null,
                                null,
                                null)) {
                      if (c != null && c.moveToFirst()) {
                        name = c.getString(0);
                        size = c.getLong(1);
                      }
                    } catch (RuntimeException ignored) {
                    }
                    PickerItem item =
                        new PickerItem(
                            uri.toString(),
                            name == null ? "file" : name,
                            getContentResolver().getType(uri),
                            null,
                            null,
                            uri,
                            size,
                            0,
                            false,
                            false);
                    handler.post(
                        () -> {
                          if (!destroyed) model.select(item);
                        });
                  });
            }
          });

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_picker);
    prefs = getSharedPreferences("picker_appearance", MODE_PRIVATE);
    grid = prefs.getBoolean("grid", true);
    sortField = prefs.getInt("sort", 0);
    descending = prefs.getBoolean("descending", true);
    model = new ViewModelProvider(this).get(PickerViewModel.class);
    toolbar = findViewById(R.id.TB_Toolbar);
    toolbar.setNavigationOnClickListener(v -> goBack());
    toolbar.getMenu().add(R.string.select_all).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    toolbar
        .getMenu()
        .add(R.string.browse_internal_storage)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    toolbar.setOnMenuItemClickListener(
        item -> {
          if (item.getTitle().equals(getString(R.string.select_all))) {
            List<PickerItem> visible = adapter.getCurrentList();
            boolean all = !visible.isEmpty();
            for (PickerItem i : visible)
              if (!i.directory && !model.selected.containsKey(i.id)) {
                all = false;
                break;
              }
            for (PickerItem i : visible)
              if (!i.directory) {
                if (all) model.selected.remove(i.id);
                else model.selected.put(i.id, i);
              }
            model.selectionCount.setValue(model.selected.size());
            return true;
          }
          documents.launch(new String[] {"*/*"});
          return true;
        });
    search = findViewById(R.id.PickerSearch);
    path = findViewById(R.id.PickerPath);
    empty = findViewById(R.id.PickerEmpty);
    count = findViewById(R.id.PickerCount);
    add = findViewById(R.id.PickerAdd);
    loading = findViewById(R.id.PickerLoading);
    sort = findViewById(R.id.PickerSort);
    gridButton = findViewById(R.id.PickerGrid);
    listButton = findViewById(R.id.PickerList);
    recycler = findViewById(R.id.PickerRecycler);
    adapter = new PickerAdapter(model.selected.keySet(), this::open, model::toggle);
    recycler.setAdapter(adapter);
    recycler.setItemAnimator(null);
    configureView();
    search.setText(model.query);
    search.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

          public void afterTextChanged(Editable e) {}

          public void onTextChanged(CharSequence s, int st, int before, int c) {
            model.query = s.toString();
            scheduleFilter();
          }
        });
    gridButton.setOnClickListener(v -> setGrid(true));
    listButton.setOnClickListener(v -> setGrid(false));
    sort.setOnClickListener(v -> showSort());
    add.setOnClickListener(v -> finishSelection());
    model.items.observe(this, items -> scheduleFilter());
    model.loading.observe(
        this,
        b -> {
          loading.setVisibility(b ? View.VISIBLE : View.GONE);
          if (b) empty.setVisibility(View.GONE);
        });
    model.failed.observe(
        this,
        b -> {
          if (b) {
            empty.setText(R.string.permission_denied);
            empty.setVisibility(View.VISIBLE);
          }
        });
    model.selectionCount.observe(
        this,
        n -> {
          count.setText(getString(R.string.selected_num, n));
          add.setEnabled(n > 0);
          add.setAlpha(n > 0 ? 1f : 0.5f);
          adapter.notifyItemRangeChanged(0, adapter.getItemCount(), "selection");
        });
    LinearLayout tabs = findViewById(R.id.PickerTabs);
    for (int i = 0; i < tabLabels.length; i++) {
      final int index = i;
      Button b = new Button(this);
      b.setText(tabLabels[i]);
      b.setTextSize(12);
      b.setTextColor(getResources().getColor(R.color.ui_muted));
      b.setBackgroundResource(android.R.color.transparent);
      b.setMinWidth(0);
      b.setMinimumWidth(0);
      tabs.addView(b, new LinearLayout.LayoutParams(0, dp(44), 1));
      b.setOnClickListener(v -> selectTab(index, true));
    }
    getOnBackPressedDispatcher()
        .addCallback(
            this,
            new OnBackPressedCallback(true) {
              @Override
              public void handleOnBackPressed() {
                goBack();
              }
            });
    selectTab(model.tab < 0 ? getIntent().getIntExtra(EXTRA_INITIAL_TAB, 0) : model.tab, false);
    AppAppearance.apply(findViewById(android.R.id.content));
  }

  private int dp(int n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  private void setGrid(boolean next) {
    if (grid == next) return;
    grid = next;
    prefs.edit().putBoolean("grid", grid).apply();
    configureView();
  }

  private void configureView() {
    int spans = Math.max(2, getResources().getDisplayMetrics().widthPixels / dp(116));
    adapter.grid = grid;
    adapter.spanCount = spans;
    recycler.setLayoutManager(
        grid ? new GridLayoutManager(this, spans) : new LinearLayoutManager(this));
    recycler.setAdapter(adapter);
    gridButton.setTextColor(getResources().getColor(grid ? R.color.ui_cyan : R.color.ui_muted));
    listButton.setTextColor(getResources().getColor(grid ? R.color.ui_muted : R.color.ui_cyan));
    gridButton.setSelected(grid);
    listButton.setSelected(!grid);
  }

  private void selectTab(int tab, boolean clearSearch) {
    if (tab < 0 || tab >= tabLabels.length) tab = 0;
    if (clearSearch) {
      model.query = "";
      search.setText("");
    }
    ++filterGeneration;
    adapter.submitList(new ArrayList<>());
    model.tab = tab;
    toolbar.setTitle(tabLabels[tab]);
    path.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
    path.setText(model.folder.getAbsolutePath());
    LinearLayout tabs = findViewById(R.id.PickerTabs);
    for (int i = 0; i < tabs.getChildCount(); i++)
      ((Button) tabs.getChildAt(i))
          .setTextColor(getResources().getColor(i == tab ? R.color.ui_cyan : R.color.ui_muted));
    if (tab == 1) {
      ensureApps();
      AppData[] data = apps.getAppsLiveData().getValue();
      if (data != null) publishApps(data);
      else {
        model.waitForApps();
        apps.loadData();
      }
    } else model.load(tab, model.folder);
  }

  private void ensureApps() {
    if (apps != null) return;
    apps = new ViewModelProvider(this).get(AppsViewModel.class);
    apps.getAppsLiveData()
        .observe(
            this,
            data -> {
              if (model.tab == 1) publishApps(data);
            });
  }

  private void publishApps(AppData[] data) {
    List<PickerItem> result = new ArrayList<>();
    for (AppData app : data)
      result.add(
          new PickerItem(
              "app:" + app.packageName,
              app.label,
              "application/vnd.android.package-archive",
              null,
              app.packageName,
              null,
              app.size,
              app.modified,
              false,
              app.hasSplits));
    model.showApps(result);
  }

  private void scheduleFilter() {
    if (adapter == null) return;
    if (pendingFilter != null) handler.removeCallbacks(pendingFilter);
    int request = ++filterGeneration;
    String query = AppSearch.normalize(model.query);
    int field = sortField;
    boolean desc = descending;
    List<PickerItem> snapshot =
        new ArrayList<>(
            model.items.getValue() == null ? new ArrayList<>() : model.items.getValue());
    pendingFilter =
        () ->
            filterWorker.execute(
                () -> {
                  List<PickerItem> result = new ArrayList<>();
                  for (PickerItem item : snapshot) {
                    if (query.isEmpty() || item.searchKey.contains(query)) result.add(item);
                  }
                  java.util.Collections.sort(result, PickerOrder.comparator(field, desc));
                  handler.post(
                      () -> {
                        if (destroyed || request != filterGeneration) return;
                        adapter.submitList(result);
                        empty.setText(
                            Boolean.TRUE.equals(model.failed.getValue())
                                ? R.string.permission_denied
                                : model.tab == 1
                                    ? R.string.no_apps_found
                                    : R.string.no_files_found);
                        empty.setVisibility(
                            result.isEmpty() && !Boolean.TRUE.equals(model.loading.getValue())
                                ? View.VISIBLE
                                : View.GONE);
                      });
                });
    handler.postDelayed(pendingFilter, 150);
    sort.setText(getString(sortLabels[sortField]) + (descending ? " ↓" : " ↑"));
  }

  private void showSort() {
    BottomSheetDialog dialog = new BottomSheetDialog(this);
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(24), dp(16), dp(24), dp(24));
    root.setBackgroundColor(getResources().getColor(R.color.ui_nav));
    TextView title = new TextView(this);
    title.setText(R.string.sort_by);
    title.setTextSize(22);
    title.setTextColor(getResources().getColor(R.color.ui_text));
    root.addView(title);
    RadioGroup group = new RadioGroup(this);
    for (int i = 0; i < sortLabels.length; i++) {
      RadioButton b = new RadioButton(this);
      b.setId(View.generateViewId());
      b.setText(sortLabels[i]);
      b.setTextColor(getResources().getColor(R.color.ui_text));
      group.addView(b);
      b.setChecked(i == sortField);
      final int value = i;
      b.setOnClickListener(
          v -> {
            sortField = value;
            prefs.edit().putInt("sort", value).apply();
            scheduleFilter();
          });
    }
    root.addView(group);
    RadioGroup direction = new RadioGroup(this);
    RadioButton up = new RadioButton(this), down = new RadioButton(this);
    up.setId(View.generateViewId());
    down.setId(View.generateViewId());
    up.setText(R.string.ascending);
    down.setText(R.string.descending);
    up.setTextColor(getResources().getColor(R.color.ui_text));
    down.setTextColor(getResources().getColor(R.color.ui_text));
    direction.addView(up);
    direction.addView(down);
    up.setChecked(!descending);
    down.setChecked(descending);
    direction.setOnCheckedChangeListener(
        (g, id) -> {
          descending = id == down.getId();
          prefs.edit().putBoolean("descending", descending).apply();
          scheduleFilter();
        });
    root.addView(direction);
    Button done = new Button(this);
    done.setText(R.string.done);
    done.setOnClickListener(v -> dialog.dismiss());
    root.addView(done);
    AppAppearance.apply(root);
    dialog.setContentView(root);
    dialog.show();
  }

  private void open(PickerItem item) {
    if (item.directory) {
      model.folder = new File(item.path);
      search.setText("");
      path.setText(item.path);
      model.load(0, model.folder);
      return;
    }
    if (item.packageName != null) {
      model.toggle(item);
      return;
    }
    if (getSupportFragmentManager().findFragmentByTag("preview") == null)
      MediaPreviewDialog.create(item).show(getSupportFragmentManager(), "preview");
  }

  public void selectFromPreview(PickerItem item) {
    model.select(item);
  }

  private void goBack() {
    if (model.tab == 0
        && !model.folder.equals(Environment.getExternalStorageDirectory())
        && model.folder.getParentFile() != null) {
      model.folder = model.folder.getParentFile();
      path.setText(model.folder.getAbsolutePath());
      search.setText("");
      model.load(0, model.folder);
    } else finish();
  }

  private void finishSelection() {
    if (model.selected.isEmpty()) return;
    Intent result = new Intent();
    ArrayList<String> packages = new ArrayList<>(), paths = new ArrayList<>();
    ArrayList<Uri> uris = new ArrayList<>();
    for (PickerItem item : model.selected.values()) {
      if (item.packageName != null) packages.add(item.packageName);
      else if (item.path != null) paths.add(item.path);
      else if (item.uri != null) uris.add(item.uri);
    }
    result.putExtra(EXTRA_APPS_SHARED, !packages.isEmpty());
    result.putExtra(EXTRA_PACKAGES_NAMES, packages);
    result.putExtra(EXTRA_FILE_PATHS_SHARED, !paths.isEmpty());
    result.putExtra(EXTRA_FILES_PATHS, paths);
    result.putExtra(EXTRA_URIS_SHARED, !uris.isEmpty());
    result.putParcelableArrayListExtra(EXTRA_URIS, uris);
    setResult(RESULT_OK, result);
    finish();
  }

  @Override
  protected void onDestroy() {
    destroyed = true;
    handler.removeCallbacksAndMessages(null);
    filterWorker.shutdownNow();
    super.onDestroy();
  }
}
