package com.ammar.sharing;

import static org.junit.Assert.*;

import com.ammar.sharing.common.utils.AppSearch;
import java.util.Locale;
import org.junit.Test;

public class AppSearchTest {
  @Test
  public void matchesPersianArabicVariants() {
    assertEquals(AppSearch.normalize("كي بورد"), AppSearch.normalize("کی بورد"));
  }

  @Test
  public void removesDiacriticsAndJoiners() {
    assertEquals(AppSearch.normalize("برنامهها"), AppSearch.normalize("بَرنامه‌ها"));
  }

  @Test
  public void foldsLatinIndependentOfTurkishDeviceLocale() {
    Locale previous = Locale.getDefault();
    try {
      Locale.setDefault(Locale.forLanguageTag("tr"));
      assertEquals("inter", AppSearch.normalize("INTER"));
    } finally {
      Locale.setDefault(previous);
    }
  }

  @Test
  public void searchesPackageNames() {
    assertTrue(
        AppSearch.normalize("Telegram org.telegram.messenger")
            .contains(AppSearch.normalize("ORG.TELEGRAM")));
  }
}
