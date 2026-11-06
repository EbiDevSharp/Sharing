package com.ammar.sharing;

import static org.junit.Assert.*;

import com.ammar.sharing.activities.AddAppsAndFilesActivity.picker.PickerItem;
import com.ammar.sharing.activities.AddAppsAndFilesActivity.picker.PickerOrder;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class PickerOrderTest {
  private PickerItem item(String id, long size, long date, boolean folder) {
    return new PickerItem(
        id,
        id,
        id.endsWith(".jpg") ? "image/jpeg" : "text/plain",
        null,
        null,
        null,
        size,
        date,
        folder,
        false);
  }

  @Test
  public void newestFirstRetainsDirectoriesFirst() {
    List<PickerItem> list =
        new ArrayList<>(
            List.of(
                item("old", 1, 1, false), item("folder", 0, 0, true), item("new", 4, 20, false)));
    list.sort(PickerOrder.comparator(0, true));
    assertEquals(List.of("folder", "new", "old"), list.stream().map(i -> i.id).toList());
  }

  @Test
  public void sizeDirectionReversesFilesOnly() {
    List<PickerItem> list =
        new ArrayList<>(
            List.of(
                item("large", 400, 0, false),
                item("small", 1, 0, false),
                item("folder", 0, 0, true)));
    list.sort(PickerOrder.comparator(1, false));
    assertEquals("folder", list.get(0).id);
    assertEquals("small", list.get(1).id);
    list.sort(PickerOrder.comparator(1, true));
    assertEquals("folder", list.get(0).id);
    assertEquals("large", list.get(1).id);
  }

  @Test
  public void normalizesNamesAndSearchWithoutRepeatedMetadataRead() {
    assertEquals(item("كي بورد", 0, 0, false).searchKey, item("کی بورد", 0, 0, false).searchKey);
  }

  @Test
  public void typeSortGroupsFormatsAndBreaksTiesByName() {
    List<PickerItem> list =
        new ArrayList<>(
            List.of(
                item("z.txt", 0, 0, false),
                item("b.jpg", 0, 0, false),
                item("a.jpg", 0, 0, false)));
    list.sort(PickerOrder.comparator(3, false));
    assertEquals(List.of("a.jpg", "b.jpg", "z.txt"), list.stream().map(i -> i.id).toList());
  }
}
