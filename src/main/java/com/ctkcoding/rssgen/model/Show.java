package com.ctkcoding.rssgen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Show {
    String title;
    String description;
    String site;
    String link;
    String image;
    String language;
    String ttl;
    List<Episode> episodes;
}
