package com.ammar.sharing.activities.AddAppsAndFilesActivity.picker;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.ammar.sharing.R;
import com.ammar.sharing.common.utils.AppAppearance;
import com.ammar.sharing.common.utils.AppIcons;
import com.ammar.sharing.common.utils.Utils;
import com.bumptech.glide.Glide;
import java.text.DateFormat;
import java.util.Date;
import java.util.Set;

public class PickerAdapter extends ListAdapter<PickerItem, PickerAdapter.Holder> {
  public interface Action {
    void accept(PickerItem item);
  }

  private final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
  public boolean grid = true;
  public int spanCount = 3;
  private final Set<String> selected;
  private final Action open, toggle;

  public PickerAdapter(Set<String> selected, Action open, Action toggle) {
    super(
        new DiffUtil.ItemCallback<>() {
          @Override
          public boolean areItemsTheSame(@NonNull PickerItem a, @NonNull PickerItem b) {
            return a.id.equals(b.id);
          }

          @Override
          public boolean areContentsTheSame(@NonNull PickerItem a, @NonNull PickerItem b) {
            return a.name.equals(b.name)
                && a.size == b.size
                && a.modified == b.modified
                && a.splits == b.splits;
          }
        });
    this.selected = selected;
    this.open = open;
    this.toggle = toggle;
  }

  private static int dp(View v, int value) {
    return Math.round(value * v.getResources().getDisplayMetrics().density);
  }

  @Override
  public int getItemViewType(int position) {
    return grid ? 1 : 0;
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
    return new Holder(parent, type == 1);
  }

  @Override
  public void onBindViewHolder(@NonNull Holder h, int position) {
    PickerItem item = getItem(position);
    h.root.setOnClickListener(v -> open.accept(item));
    h.root.setOnLongClickListener(
        v -> {
          if (!item.directory) toggle.accept(item);
          return true;
        });
    h.root.setContentDescription(item.name);
    h.name.setText(item.name);
    h.detail.setText(
        (item.directory
                ? h.root.getContext().getString(R.string.folder)
                : Utils.getFormattedSize(item.size))
            + " · "
            + dateFormat.format(new Date(item.modified))
            + (item.splits ? " · " + h.root.getContext().getString(R.string.splits) : ""));
    bindSelection(h, item);
    int icon =
        item.directory
            ? R.drawable.ic_folder
            : item.mime.startsWith("image/")
                ? R.drawable.line_image
                : item.mime.startsWith("video/")
                    ? R.drawable.line_video
                    : item.mime.startsWith("audio/") ? R.drawable.line_audio : R.drawable.line_file;
    h.image.setImageResource(icon);
    h.image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    if (item.packageName != null) {
      Glide.with(h.image).clear(h.image);
      h.image.setPadding(
          dp(h.root, grid ? 28 : 4),
          dp(h.root, grid ? 20 : 4),
          dp(h.root, grid ? 28 : 4),
          dp(h.root, grid ? 28 : 4));
      AppIcons.bind(h.image, item.packageName);
    } else {
      AppIcons.clear(h.image);
      h.image.setPadding(dp(h.root, 8), dp(h.root, 8), dp(h.root, 8), dp(h.root, 8));
      if (item.mime.startsWith("image/") || item.mime.startsWith("video/")) {
        h.image.setPadding(0, 0, 0, 0);
        h.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(h.image).load(item.uri).centerCrop().placeholder(icon).error(icon).into(h.image);
      } else {
        Glide.with(h.image).clear(h.image);
        h.image.setImageResource(icon);
      }
    }
  }

  @Override
  public void onBindViewHolder(
      @NonNull Holder h, int position, @NonNull java.util.List<Object> payloads) {
    if (payloads.isEmpty()) onBindViewHolder(h, position);
    else bindSelection(h, getItem(position));
  }

  private void bindSelection(Holder h, PickerItem item) {
    boolean checked = selected.contains(item.id);
    h.root.setSelected(checked);
    h.check.setVisibility(item.directory ? View.GONE : View.VISIBLE);
    h.check.setText(checked ? "✓" : "");
    h.check.setContentDescription(
        h.root
            .getContext()
            .getString(checked ? R.string.deselect_named : R.string.select_named, item.name));
    h.check.setOnClickListener(v -> toggle.accept(item));
    GradientDrawable circle = new GradientDrawable();
    circle.setShape(GradientDrawable.OVAL);
    circle.setColor(checked ? h.root.getResources().getColor(R.color.ui_cyan) : 0x50303850);
    circle.setStroke(
        dp(h.root, 1), checked ? h.root.getResources().getColor(R.color.ui_cyan) : 0xFFC6C4D2);
    h.check.setBackground(circle);
    GradientDrawable border = new GradientDrawable();
    border.setCornerRadius(dp(h.root, 18));
    border.setColor(h.root.getResources().getColor(R.color.ui_glass));
    border.setStroke(
        dp(h.root, checked ? 2 : 1),
        h.root.getResources().getColor(checked ? R.color.ui_cyan : R.color.ui_stroke));
    h.root.setBackground(border);
    h.root.setClipToOutline(true);
  }

  @Override
  public void onViewRecycled(@NonNull Holder h) {
    Glide.with(h.image).clear(h.image);
    AppIcons.clear(h.image);
    super.onViewRecycled(h);
  }

  public final class Holder extends RecyclerView.ViewHolder {
    final FrameLayout root;
    final ImageView image;
    final TextView name, detail, check;

    Holder(ViewGroup parent, boolean gridMode) {
      super(new FrameLayout(parent.getContext()));
      root = (FrameLayout) itemView;
      int side =
          (parent.getResources().getDisplayMetrics().widthPixels - dp(parent, 32)) / spanCount;
      RecyclerView.LayoutParams lp =
          new RecyclerView.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, gridMode ? side : dp(parent, 52));
      lp.setMargins(dp(parent, 3), dp(parent, 3), dp(parent, 3), dp(parent, 3));
      root.setLayoutParams(lp);
      root.setForeground(
          parent.getContext().getDrawable(android.R.drawable.list_selector_background));
      image = new ImageView(parent.getContext());
      FrameLayout.LayoutParams imageLp =
          new FrameLayout.LayoutParams(
              gridMode ? -1 : dp(parent, 44),
              gridMode ? -1 : dp(parent, 44),
              Gravity.START | Gravity.CENTER_VERTICAL);
      root.addView(image, imageLp);
      LinearLayout texts = new LinearLayout(parent.getContext());
      texts.setOrientation(LinearLayout.VERTICAL);
      texts.setGravity(Gravity.CENTER_VERTICAL);
      name = new TextView(parent.getContext());
      name.setTextSize(gridMode ? 11 : 13);
      name.setMaxLines(1);
      name.setEllipsize(android.text.TextUtils.TruncateAt.END);
      name.setTextColor(parent.getResources().getColor(R.color.ui_text));
      detail = new TextView(parent.getContext());
      detail.setTextSize(10);
      detail.setMaxLines(1);
      detail.setEllipsize(android.text.TextUtils.TruncateAt.END);
      detail.setTextColor(parent.getResources().getColor(R.color.ui_muted));
      texts.addView(name);
      texts.addView(detail);
      detail.setVisibility(gridMode ? View.GONE : View.VISIBLE);
      FrameLayout.LayoutParams textLp =
          new FrameLayout.LayoutParams(
              gridMode ? -1 : -1,
              gridMode ? dp(parent, 30) : -1,
              gridMode ? Gravity.BOTTOM : Gravity.CENTER_VERTICAL);
      if (gridMode) {
        texts.setPadding(dp(parent, 6), dp(parent, 4), dp(parent, 6), dp(parent, 4));
        texts.setBackground(
            new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, new int[] {0x00000000, 0xCC050617}));
        name.setTextColor(Color.WHITE);
        name.setTypeface(null, Typeface.BOLD);
      } else {
        textLp.setMarginStart(dp(parent, 52));
        textLp.setMarginEnd(dp(parent, 44));
      }
      root.addView(texts, textLp);
      check = new TextView(parent.getContext());
      check.setGravity(Gravity.CENTER);
      check.setTextSize(16);
      check.setTextColor(Color.WHITE);
      check.setClickable(true);
      check.setFocusable(true);
      FrameLayout.LayoutParams checkLp =
          new FrameLayout.LayoutParams(dp(parent, 30), dp(parent, 30), Gravity.TOP | Gravity.END);
      checkLp.setMargins(dp(parent, 6), dp(parent, 6), dp(parent, 6), dp(parent, 6));
      root.addView(check, checkLp);
      AppAppearance.apply(root);
    }
  }
}
