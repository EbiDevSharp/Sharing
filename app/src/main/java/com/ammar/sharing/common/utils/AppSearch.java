package com.ammar.sharing.common.utils;

import java.text.Normalizer;
import java.util.Locale;

/** Search both Persian/Arabic spellings and Latin package names independent of device locale. */
public final class AppSearch {
  private AppSearch() {}

  public static String normalize(String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFKD)
        .replaceAll("\\p{M}+", "")
        .replace('ي', 'ی')
        .replace('ك', 'ک')
        .replace("\u200c", "")
        .trim()
        .toLowerCase(Locale.ROOT);
  }
}
