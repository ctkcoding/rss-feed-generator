package com.ctkcoding.rssgen.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder(toBuilder = true)
public class Episode {
    String title;
    String description;
    String url;
    LocalDateTime pubDate; // todo - does XML need this as lower case?
    String image;
    String enclosure; // todo - in .build() always "{url: enclosure, type: \"audio/mpeg\"}";
}
