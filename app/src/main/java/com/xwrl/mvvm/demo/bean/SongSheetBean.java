package com.xwrl.mvvm.demo.bean;

public class SongSheetBean {
    private String title;
    private String firstAlbumPath;
    private String count;
    private String errorMsg;

    public SongSheetBean(String title, String firstAlbumPath, String count) {
        this.title = title;
        this.firstAlbumPath = firstAlbumPath;
        this.count = count;
    }
    public SongSheetBean(String msg) {
        this.errorMsg = msg;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public String getFirstAlbumPath() {
        return firstAlbumPath;
    }

    public void setFirstAlbumPath(String firstAlbumPath) {
        this.firstAlbumPath = firstAlbumPath;
    }

    public String getSheetName(){
        return "歌单："+this.title;
    }

    public void release(){
        if (this.title != null) this.title = null;
        if (this.count != null) this.count = null;
        if (this.firstAlbumPath != null) this.firstAlbumPath = null;
        if (this.errorMsg != null) this.errorMsg = null;
    }
}
