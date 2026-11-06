package com.ammar.sharing;

import static org.junit.Assert.*;

import com.ammar.sharing.common.utils.QrCodeEncoder;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.Test;

public class QrCodeEncoderTest {
  private void roundTrip(String text) throws Exception {
    byte[] modules = QrCodeEncoder.encode(text);
    int size = (int) Math.sqrt(modules.length);
    for (int y = 0; y < size; y++)
      for (int x = 0; x < size; x++) {
        if (x < 4 || x >= size - 4 || y < 4 || y >= size - 4)
          assertEquals(0, modules[y * size + x]);
      }
    int[] pixels = QrCodeEncoder.pixels(modules);
    int side = (int) Math.sqrt(pixels.length);
    for (int pixel : pixels) assertTrue(pixel == 0xFF000000 || pixel == 0xFFFFFFFF);
    assertTrue(side <= 720);
    assertEquals(
        text,
        new MultiFormatReader()
            .decode(
                new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(side, side, pixels))))
            .getText());
  }

  @Test
  public void decodesHttpWifiLink() throws Exception {
    roundTrip("http://192.168.1.1:2999");
  }

  @Test
  public void decodesLongHttpsLinkBeyondOldVersionTwoCapacity() throws Exception {
    roundTrip("https://192.168.168.254:65535/path?token=" + "a".repeat(160));
  }

  @Test
  public void decodesIpv6Link() throws Exception {
    roundTrip("http://[fe80::abcd:1234:5678:90ab]:2999");
  }

  @Test
  public void decodesUnicodeAndDevicePairingPayload() throws Exception {
    roundTrip(
        "{\"name\":\"گوشی من\",\"ip\":\"192.168.168.254\",\"fingerprint\":\""
            + "f".repeat(64)
            + "\"}");
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsInvalidMatrix() {
    QrCodeEncoder.pixels(new byte[8]);
  }
}
