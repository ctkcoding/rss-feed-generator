package com.ctkcoding.rssgen.service;

import com.ctkcoding.rssgen.model.Episode;
import com.ctkcoding.rssgen.model.Show;

public class ParseService {
    public Show generateShow() {
        // todo - use parse methods to build and return
        return Show.builder()
                .build();
    }
    public Show parseShow() {
        // todo - take root and show dirs
        // todo - parse into a show
        return null;
    }

    public Episode parseEpisode(String episodeFile) {
        // todo - add file name to root paths
        // todo - extract artwork if enabled

        return Episode.builder()
                .build();
    }

    public Boolean artworkExists(String episodeFile) {
        // todo - if config enabled
        // todo - check for artwork;
        return false;
    }

    public void generateArtwork(String artworkPath) {
        // todo - if config enabled
        // todo - extract embedded artwork
    }

    public String generateCoverPath() {
        // todo - check ts for what this did
        return "";
    }
}
