package com.ctkcoding.rssgen.handler;

public class XmlSanitizer {

  private XmlSanitizer() {}

  public static String sanitize(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }

    StringBuilder sb = new StringBuilder(input.length());
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (isValidXmlChar(c)) {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private static boolean isValidXmlChar(char c) {
    if (c == '\t' || c == '\n' || c == '\r') {
      return true;
    }
    // XML 1.0: #x9 #xA #xD allowed
    // Then: #x20-#x7E, #xA0-#xD7FF, #xE000-#xFFFD
    // (excludes 0x00-0x08, 0x0B-0x0C, 0x0E-0x1F, 0x7F-0x9F)
    return (c >= 0x20 && c <= 0x7E) || (c >= 0xA0 && c <= 0xD7FF) || (c >= 0xE000 && c <= 0xFFFD);
  }
}
