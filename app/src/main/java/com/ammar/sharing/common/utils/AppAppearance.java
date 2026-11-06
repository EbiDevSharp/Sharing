package com.ammar.sharing.common.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.res.ResourcesCompat;
import com.ammar.sharing.R;
import com.ammar.sharing.activities.MainActivity.MainActivity;
import com.ammar.sharing.common.Consts;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Persistent app appearance, applied before inflation and to dynamically added text. */
public final class AppAppearance {
  public static final String PREF_FONT = "appearance_font";
  public static final String[] FONT_KEYS = {
    "auto", "vazirmatn", "plus_jakarta_sans", "inter", "space_grotesk", "noto_sans_arabic"
  };
  public static final String[] FONT_NAMES = {
    "Auto", "Vazirmatn", "Plus Jakarta Sans", "Inter", "Space Grotesk", "Noto Sans Arabic"
  };
  private static final Map<String, Typeface> FONTS = new HashMap<>();

  private AppAppearance() {}

  public static void prepare(Activity activity) {
    SharedPreferences prefs =
        activity.getSharedPreferences(Consts.PREF_SETTINGS, Context.MODE_PRIVATE);
    MainActivity.sDarkMode = prefs.getBoolean(Consts.PREF_FIELD_IS_DARK, true);
    AppCompatDelegate.setDefaultNightMode(
        MainActivity.sDarkMode
            ? AppCompatDelegate.MODE_NIGHT_YES
            : AppCompatDelegate.MODE_NIGHT_NO);
    activity.setTheme(MainActivity.sDarkMode ? R.style.AppThemeDark : R.style.AppTheme);
    Utils.setLocale(activity, prefs.getString(Consts.PREF_FIELD_LANG, ""));
    activity
        .getWindow()
        .setBackgroundDrawableResource(
            MainActivity.sDarkMode
                ? R.drawable.gradient_background_dark
                : R.drawable.gradient_background_light);
  }

  public static void apply(View root) {
    if (root == null) return;
    Context context = root.getContext();
    String key =
        context
            .getSharedPreferences(Consts.PREF_SETTINGS, Context.MODE_PRIVATE)
            .getString(PREF_FONT, "auto");
    if ("auto".equals(key))
      key = "fa".equals(Locale.getDefault().getLanguage()) ? "vazirmatn" : "plus_jakarta_sans";
    Typeface face = FONTS.get(key);
    if (face == null) {
      int resource =
          switch (key) {
            case "vazirmatn" -> R.font.vazirmatn;
            case "inter" -> R.font.inter;
            case "space_grotesk" -> R.font.space_grotesk;
            case "noto_sans_arabic" -> R.font.noto_sans_arabic;
            default -> R.font.plus_jakarta_sans;
          };
      face = ResourcesCompat.getFont(context, resource);
      FONTS.put(key, face);
    }
    applyTree(root, face, key);
  }

  private static void applyTree(View view, Typeface face, String key) {
    if (view instanceof TextView toggle && view.getId() == R.id.MI_LanguageToggle) {
      toggle.setTypeface(ResourcesCompat.getFont(view.getContext(), R.font.vazirmatn), Typeface.BOLD);
      toggle.setTextSize(16);
      view.setTag(R.id.tag_font_applied, key);
    }
    if (view instanceof TextView text && !key.equals(view.getTag(R.id.tag_font_applied))) {
      int style = text.getTypeface() == null ? Typeface.NORMAL : text.getTypeface().getStyle();
      text.setTypeface(face, style);
      view.setTag(R.id.tag_font_applied, key);
    }
    if (view instanceof ViewGroup group) {
      for (int i = 0; i < group.getChildCount(); i++) applyTree(group.getChildAt(i), face, key);
    }
  }
}
