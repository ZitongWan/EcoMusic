package com.xwrl.mvvm.demo.model;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.v4.media.MediaMetadataCompat;

import com.xwrl.mvvm.demo.bean.MusicBean;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.List;

@Deprecated
public interface BaseModel {
    // Obtain local music metadata and return List<customBean>collection data
    void getLocalMusic(OnMusicListener onMusicListener, ContentResolver resolver);
    interface OnMusicListener{
        void OnComplete(List<MusicBean> beans);
    }
    // Obtain local music metadata and return List<MediaBrowserCompact MediaItem>Collection data, suitable for MediaSession media framework
    void getLocalMusicMetadata(OnMusicMetadataListener onMusicListener, ContentResolver resolver);
    interface OnMusicMetadataListener{
        void OnComplete(LinkedHashMap<String, MediaMetadataCompat> musicMaps);
    }
    // Obtain an album image of a local music, Bitmap
    void getLocalMusicAlbum(OnLoadPictureListener onLoadPictureListener, String path, Resources resources);
    interface OnLoadPictureListener{
        void OnComplete(WeakReference<Bitmap> bitmap);
    }
}
