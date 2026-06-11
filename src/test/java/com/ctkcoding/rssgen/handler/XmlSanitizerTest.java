package com.ctkcoding.rssgen.handler;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class XmlSanitizerTest {

  @Test
  void sanitize_returnsNullForNull() {
    assertNull(XmlSanitizer.sanitize(null));
  }

  @Test
  void sanitize_returnsEmptyForEmpty() {
    assertEquals("", XmlSanitizer.sanitize(""));
  }

  @Test
  void sanitize_passesValidChars() {
    assertEquals("Hello World", XmlSanitizer.sanitize("Hello World"));
    assertEquals(
        "Ben & Jerry <test> \"quoted\"", XmlSanitizer.sanitize("Ben & Jerry <test> \"quoted\""));
  }

  @Test
  void sanitize_removes_0x01() {
    assertEquals("abcxyz", XmlSanitizer.sanitize("abc\u0001xyz"));
  }

  @Test
  void sanitize_removes_0x00_to_0x08() {
    String input = "\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007";
    assertEquals("", XmlSanitizer.sanitize(input));
  }

  @Test
  void sanitize_removes_0x0B_to_0x0C() {
    assertEquals("abcxyz", XmlSanitizer.sanitize("abc\u000b\u000cxyz"));
  }

  @Test
  void sanitize_removes_0x0E_to_0x1F() {
    String input =
        "abc\u000e\u000f\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001a\u001b\u001c\u001d\u001e\u001f";
    assertEquals("abc", XmlSanitizer.sanitize(input));
  }

  @Test
  void sanitize_removes_0x7F() {
    assertEquals("abcxyz", XmlSanitizer.sanitize("abc\u007fxyz"));
  }

  @Test
  void sanitize_removes_0x9F() {
    assertEquals("abcxyz", XmlSanitizer.sanitize("abc\u009fxyz"));
  }

  @Test
  void sanitize_preserves_valid_ranges() {
    StringBuilder sb = new StringBuilder();
    for (char c = 0x20; c <= 0x7E; c++) {
      sb.append(c);
    }
    for (char c = 0xE0; c <= 0xFF; c++) {
      sb.append(c);
    }
    assertEquals(sb.toString(), XmlSanitizer.sanitize(sb.toString()));
  }

  @Test
  void sanitize_preserves_tab() {
    assertEquals("abc\ndef", XmlSanitizer.sanitize("abc\ndef"));
  }

  @Test
  void sanitize_preserves_cr() {
    assertEquals("abc\rdef", XmlSanitizer.sanitize("abc\rdef"));
  }

  @Test
  void sanitize_handles_real_world_control_chars() {
    String input = "Ben & Jerry\u2019s & Nike\u001d\u2019s";
    assertEquals("Ben & Jerry\u2019s & Nike\u2019s", XmlSanitizer.sanitize(input));
  }
}
