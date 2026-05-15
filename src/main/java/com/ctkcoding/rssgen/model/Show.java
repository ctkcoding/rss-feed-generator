package com.ctkcoding.rssgen.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder(toBuilder = true)
public class Show {
    String title;
    String description;
    String site;
    String link;
    String image;
    String language; // todo - default us-en
    String ttl; // todo - what was prev default

    List<Episode> episodes;
}
