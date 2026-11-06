package com.ammar.sharing.common.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.util.EnumMap;
import java.util.Map;

/** Theme-independent QR modules, including the standard four-module quiet zone. */
public final class QrCodeEncoder {
  private QrCodeEncoder() {}

  public static byte[] encode(String text) {
    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
    hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
    hints.put(EncodeHintType.MARGIN, 4);
    try {
      BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints);
      int size = matrix.getWidth();
      byte[] modules = new byte[size * size];
      for (int y = 0; y < size; y++)
        for (int x = 0; x < size; x++) {
          modules[y * size + x] = (byte) (matrix.get(x, y) ? 1 : 0);
        }
      return modules;
    } catch (WriterException e) {
      throw new IllegalArgumentException("QR payload is too large", e);
    }
  }

  public static int[] pixels(byte[] modules) {
    int size = (int) Math.sqrt(modules.length);
    if (size == 0 || size * size != modules.length)
      throw new IllegalArgumentException("Invalid QR matrix");
    // Integer scale keeps every module crisp. ~720 px uses <3 MB, instead of 100 MB.
    int scale = Math.max(1, 720 / size);
    int side = size * scale;
    int[] pixels = new int[side * side];
    for (int y = 0; y < side; y++)
      for (int x = 0; x < side; x++) {
        pixels[y * side + x] =
            modules[(y / scale) * size + x / scale] == 1 ? 0xFF000000 : 0xFFFFFFFF;
      }
    return pixels;
  }
}
