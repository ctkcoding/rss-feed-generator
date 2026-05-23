package com.ctkcoding.rssgen.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Show {

    @JsonProperty
    private String title;

    @JsonProperty
    private String description;

    @JsonProperty
    private String site;

    @JsonProperty
    private String link;

    @JsonProperty
    private String image;

    @JsonProperty
    private String language;

    private List<Episode> episodes;
}
