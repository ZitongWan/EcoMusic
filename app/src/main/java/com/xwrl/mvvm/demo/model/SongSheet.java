package com.xwrl.mvvm.demo.model;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.support.v4.media.MediaMetadataCompat;

import com.xwrl.mvvm.demo.bean.SongSheetBean;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.List;

public interface SongSheet {
    void showLocalSheet(OnLocalSheetListener onLocalSheetListener, Context context);
    void showSheetMusic(OnSheetMusicListener onSheetMusicListener, Application application, String alias);
    void showAlbumBitmap(OnAlbumListener onAlbumListener, String path, Resources resource);
    void showAlbumBitmaps(OnAlbumsListener onAlbumListener, String... Path);
    void InsertSearchListener(OnInsertSearchListener onInsertListener,SQLiteDatabase database,String title);
    interface OnLocalSheetListener{
        void onComplete(List<SongSheetBean> bean);
    }
    interface OnSheetMusicListener{
        void onComplete(LinkedHashMap<String, MediaMetadataCompat> result);
    }
    interface OnAlbumListener {
        void onComplete(WeakReference<Bitmap> bitmap);
    }
    interface OnAlbumsListener {
        void onComplete(Bitmap... bitmaps);
    }
    interface OnInsertSearchListener{
        void onInsert(boolean isSuccessful);
    }

}
