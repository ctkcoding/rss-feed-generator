package com.ctkcoding.rssgen.service;

public enum ParseErrorReason {
  SHOW_CONFIG_FILE_NOT_FOUND,
  SHOW_CONFIG_INVALID_JSON,
  EPISODE_MP3_PARSE_ERROR,
  EPISODE_PUB_DATE_MISSING,
  EPISODE_FILE_SIZE_UNKNOWN,
  ARTWORK_UNSUPPORTED_MIME,
  ARTWORK_MIME_MISMATCH,
  ARTWORK_WRITE_FAILED,
  RSS_OUTPUT_WRITE_FAILED;

  public String getLabel() {
    return switch (this) {
      case SHOW_CONFIG_FILE_NOT_FOUND -> "Show config - show.json not found";
      case SHOW_CONFIG_INVALID_JSON -> "Show config - Invalid show.json: ";
      case EPISODE_MP3_PARSE_ERROR -> "MP3 parse failed";
      case EPISODE_PUB_DATE_MISSING -> "Could not determine publication date";
      case EPISODE_FILE_SIZE_UNKNOWN -> "WARNING: Could not determine file size";
      case ARTWORK_UNSUPPORTED_MIME -> "WARNING: No matching extension found for MIME type";
      case ARTWORK_MIME_MISMATCH -> "WARNING: MIME type mismatch";
      case ARTWORK_WRITE_FAILED -> "WARNING: Failed to write artwork file";
      case RSS_OUTPUT_WRITE_FAILED -> "RSS output - Failed to write feed";
    };
  }

  public boolean isWarning() {
    return EPISODE_FILE_SIZE_UNKNOWN.equals(this)
        || ARTWORK_UNSUPPORTED_MIME.equals(this)
        || ARTWORK_MIME_MISMATCH.equals(this)
        || ARTWORK_WRITE_FAILED.equals(this);
  }
}
