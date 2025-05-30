package com.xwrl.mvvm.demo.model;

public class CrawlerSong {
    private String title;
    private String artist;
    private String link;
    private String id;

    public CrawlerSong(String title, String artist, String link, String id) {
        this.title = title;
        this.artist = artist;
        this.link = link;
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getLink() {
        return link;
    }

    public String getId() {
        return id;
    }
}