package com.redbeard.poketrack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class ContentItem {
    final String id;
    final String type;
    final String title;
    final int season;
    final int episode;
    final String note;

    ContentItem(String id, String type, String title, int season, int episode, String note) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.season = season;
        this.episode = episode;
        this.note = note == null ? "" : note;
    }

    String number() {
        if ("EPISODIO".equals(type)) return String.format(Locale.ITALY, "%03d", episode);
        if ("FILM".equals(type)) return "FILM";
        if ("SPECIALE".equals(type)) return "SPEC";
        return String.format(Locale.ITALY, "%02d", episode);
    }

    String subtitle() {
        if ("EPISODIO".equals(type)) return "Stagione " + season + " · Episodio " + episode;
        if (!note.isEmpty()) return note;
        return type;
    }
}

class CatalogGroup {
    final String id;
    final String title;
    final String type;
    final double order;
    final List<ContentItem> items = new ArrayList<>();

    CatalogGroup(String id, String title, String type, double order) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.order = order;
    }
}
