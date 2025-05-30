package com.xwrl.mvvm.demo.bean;

import android.net.Uri;
import android.support.v4.media.MediaDescriptionCompat;

import java.lang.ref.WeakReference;


public class MusicBean {
    private String id;
    private String title;
    private String artist;
    private String album;
    private String albumPath;
    private String path;
    private long duration;
    private boolean isLoved;

    public MusicBean(String id, String title, String artist,
                     String album, String albumPath,
                     String path, long duration) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.albumPath = albumPath;
        this.path = path;
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getAlbumPath() {
        return albumPath;
    }

    public void setAlbumPath(String albumPath) {
        this.albumPath = albumPath;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getDurationString(){
        return getDurationString(this.duration, false);
    }

    public String getDurationString(long duration, boolean isShowMil){
        if (duration <= 0) return "00:00";
        int min = (int) (duration / 60000);
        int sec = (int) (duration % 60000 / 1000);
        int mil = (int) (duration % 6000000);

        return (min < 10 ? "0" + min : min) + ":" + //分钟
                (sec < 10 ? "0" + sec : sec) +      //秒钟
                (isShowMil ? ":" + mil : "");     //毫秒
    }

    public boolean isLoved() {
        return isLoved;
    }

    public void setLoved(boolean loved) {
        isLoved = loved;
    }

    public String getDescription(){
        return this.artist + " - " + this.album;
    }

    public String getMediaId(){
        return this.title+"_"+this.artist+"_"+this.album;
    }

    public WeakReference<MediaDescriptionCompat> getDescriptionCompat(){
        return new WeakReference<>(new MediaDescriptionCompat.Builder()
                .setMediaId(getMediaId())
                .setTitle(getTitle())
                .setSubtitle(getArtist())
                .setDescription(getAlbum())
                .setIconBitmap(null)
                .setIconUri(Uri.parse(getAlbumPath()))
                .setMediaUri(Uri.parse(getPath()))
                .build());
    }

    public void release(){
        if (this.id != null) this.id = null;
        if (this.title != null) this.title = null;
        if (this.artist != null) this.artist = null;
        if (this.album != null) this.album = null;
        if (this.albumPath != null) this.albumPath = null;
        if (this.path != null) this.path = null;
    }

    public String getPlayTips(){
        return "〢";
    }
}
