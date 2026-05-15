package com.ctkcoding.rssgen.model;

import lombok.Builder;

@Builder(toBuilder = true)
public class Episode {
    String title;
    String description;
    String url;
    String pubDate; // does XML need this as lower case?
    String image;
    String enclosure; // todo - in .build() always "{url: enclosure, type: \"audio/mpeg\"}";
}
