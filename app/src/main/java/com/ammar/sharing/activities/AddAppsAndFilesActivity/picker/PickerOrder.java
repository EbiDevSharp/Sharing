package com.ammar.sharing.activities.AddAppsAndFilesActivity.picker;

import java.util.Comparator;

public final class PickerOrder {
  private PickerOrder() {}

  public static Comparator<PickerItem> comparator(int field, boolean descending) {
    return (a, b) -> {
      // Keep folders first in both directions. All keys were computed when metadata was read.
      if (a.directory != b.directory) return a.directory ? -1 : 1;
      int comparison =
          switch (field) {
            case 0 -> Long.compare(a.modified, b.modified);
            case 1 -> Long.compare(a.size, b.size);
            case 3 -> a.mime.compareTo(b.mime);
            default -> a.searchKey.compareTo(b.searchKey);
          };
      if (descending) comparison = -comparison;
      if (comparison == 0) comparison = a.searchKey.compareTo(b.searchKey);
      return comparison == 0 ? a.id.compareTo(b.id) : comparison;
    };
  }
}
