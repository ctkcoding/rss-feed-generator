package com.ctkcoding.rssgen.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class Episode {
  String title;
  String description;
  String url;
  LocalDateTime pubDate;
  String image;
  String enclosureUrl; // "{url: enclosure, type: \"audio/mpeg\"}"
  String enclosureType; // type: \"audio/mpeg\"
  Long enclosureLength;
  int duration;
}
