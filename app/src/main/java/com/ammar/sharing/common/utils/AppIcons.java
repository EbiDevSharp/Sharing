package com.ammar.sharing.common.utils;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.widget.ImageView;
import com.ammar.sharing.R;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Bounded worker/cache; recycled rows cannot display a previous app's icon. */
public final class AppIcons {
  private static final ExecutorService WORKERS = Executors.newFixedThreadPool(2);
  private static final LruCache<String, Bitmap> CACHE =
      new LruCache<>(2 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
          return value.getAllocationByteCount();
        }
      };
  private static final Map<ImageView, Future<?>> REQUESTS = new WeakHashMap<>();

  private AppIcons() {}

  public static void clear(ImageView view) {
    Future<?> previous = REQUESTS.remove(view);
    if (previous != null) previous.cancel(false);
    view.setTag(null);
  }

  public static void bind(ImageView view, String packageName) {
    clear(view);
    view.setTag(packageName);
    Bitmap cached = CACHE.get(packageName);
    if (cached != null) {
      view.setImageBitmap(cached);
      return;
    }
    view.setImageResource(R.drawable.line_apps);
    PackageManager pm = view.getContext().getApplicationContext().getPackageManager();
    int size = (int) (50 * view.getResources().getDisplayMetrics().density);
    WeakReference<ImageView> ref = new WeakReference<>(view);
    REQUESTS.put(
        view,
        WORKERS.submit(
            () -> {
              try {
                Drawable drawable = pm.getApplicationIcon(packageName);
                Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                drawable.setBounds(0, 0, size, size);
                drawable.draw(new Canvas(bitmap));
                CACHE.put(packageName, bitmap);
                ImageView target = ref.get();
                if (target != null)
                  target.post(
                      () -> {
                        if (packageName.equals(target.getTag())) target.setImageBitmap(bitmap);
                      });
              } catch (PackageManager.NameNotFoundException | RuntimeException ignored) {
                /* Keep placeholder. */
              }
            }));
  }
}
