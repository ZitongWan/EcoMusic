package com.xwrl.mvvm.demo.model;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.text.TextUtils;
import android.util.Log;

import com.xwrl.mvvm.demo.R;
import com.xwrl.mvvm.demo.model.helper.MusicHelper;
import com.xwrl.mvvm.demo.model.helper.SongListHelper;
import com.xwrl.mvvm.demo.model.helper.SongSheetHelper;
import com.xwrl.mvvm.demo.util.PictureUtil;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Paths;

@SuppressLint("Range")
public class AllSongSheetModel implements SongSheet {
    private static final String TAG = "AllSongSheetModel";
    @Override
    public void showLocalSheet(OnLocalSheetListener onLocalSheetListener, Context context) {
        onLocalSheetListener.onComplete(SongSheetHelper.getDataBaseSongSheet(context));
    }

    @Override
    public void showSheetMusic(OnSheetMusicListener onSheetMusicListener,
                                                    Application application, String alias) {

        onSheetMusicListener.onComplete(SongListHelper.getSongSheetMusic(application,alias));
    }

    @Override
    public void showAlbumBitmap(OnAlbumListener onAlbumListener, String path, Resources resource) {
        Bitmap bitmap = getAlbumBitmap(path);
        onAlbumListener.onComplete(new WeakReference<>(bitmap == null ?
                PictureUtil.getResIdBitmap(R.drawable.icon_fate,500,resource,0) : bitmap));
    }

    @Override
    public void showAlbumBitmaps(OnAlbumsListener onAlbumsListener, String... Path) {
        onAlbumsListener.onComplete(getAlbumBitmaps(Path));
    }

    @Override
    public void InsertSearchListener(OnInsertSearchListener onInsertListener, SQLiteDatabase database, String title) {
        onInsertListener.onInsert(InsertSearchHistory(database,title));
    }

    /** 获得多张图片的Bitmap数组*/
    private Bitmap[] getAlbumBitmaps(String... paths){
        if (paths == null || paths.length == 0) return null;
        Bitmap[] bitmaps = new Bitmap[paths.length];
        for (int i = 0; i < paths.length; i++){
            //LogUtil.d(TAG, "getAlbumBitmaps: "+paths[i]);
            if (i > 0 && paths[i].equals(paths[i - 1])) {
                bitmaps[i] = bitmaps[i - 1];
                continue;
            }
            bitmaps[i] = paths[i].matches("^[0-9]+(.[0-9]+)?$") && !paths[i].contains("/") ?
                                                null : getAlbumBitmap(paths[i]);
        }
        return bitmaps;
    }

    private boolean InsertSearchHistory(SQLiteDatabase database,String title){
        if (database == null || !database.isOpen() || TextUtils.isEmpty(title)) return false;
        //Log.d("AllSongSheetModel", "InsertSearchHistory: "+title);
        String alias = "搜索历史";
        String sql = "select title from SongSheet_table where alias=?";
        Cursor cursor = null;
        ContentValues values = new ContentValues();
        try {
            cursor = database.rawQuery(sql,new String[]{alias});
            if (cursor.moveToNext() && cursor.getCount() > 0) {
                //2.根据查询到的歌单名将此首歌曲写进选择的歌单之中
                String table = cursor.getString(cursor.getColumnIndex("title"));
                //Log.d(TAG, "table: "+table);
                if (!cursor.isClosed()) cursor.close();
                sql = "select * from "+table+" where title=?";
                cursor = database.rawQuery(sql,new String[]{title});//查询是否有此搜索记录
                if(cursor.getCount() == 0){
                    values.put("title",title);
                    values.put("click",1);
                    database.insert(table,null,values);
                }else {
                    if (cursor.moveToNext()){
                        String click = cursor.getString(cursor.getColumnIndex("click"));
                        values.put("click",Integer.parseInt(click)+1);
                        database.update(table,values,"title=?",new String[]{title});
                    }
                }
                return true;
            }
        }catch (Exception e){
            Log.d(TAG, "InsertSearchHistory: "+e);
        }finally {
            if(cursor != null ) {
                if (values.size() > 0) values.clear();
                cursor.close();
                Log.d(TAG, "InsertSearchHistory: 关闭游标工具 "+cursor.isClosed()); }
        }
        return false;
    }

    private Bitmap getAlbumBitmap(String Path){
        if (Path.isEmpty()) return null;//返回默认的专辑封面
        if (!MusicHelper.FileExists(Path)) return null; //找不到文件返回空

        Log.d(TAG, "getAlbumBitmap: "+Path);

        Bitmap bitmap = null;
        if (!Path.contains(".mp3")) {
            BufferedInputStream bis = null;
            try {
                bis = new BufferedInputStream(Files.newInputStream(Paths.get(Path)));
                bitmap = BitmapFactory.decodeStream(bis);
                //关闭输输入通道
                bis.close();
            }  catch (IOException e) {
                e.printStackTrace();
                Log.w("AllSongSheetModel", "getAlbumBitmap: 本地图片转Bitmap失败");
            }finally {
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.w("AllSongSheetModel", "getAlbumBitmap: 输出流关闭异常");
                    }
                }
            }
            //Log.d("加载本地图片", "getAlbumBitmap: ");
        }else {

            try {
                WeakReference<MediaMetadataRetriever> metadataRetriever =
                                    new WeakReference<>(new MediaMetadataRetriever());

                metadataRetriever.get().setDataSource(Path);

                WeakReference<byte[]> picture =
                        new WeakReference<>(metadataRetriever.get().getEmbeddedPicture());

                metadataRetriever.get().release();//SDK > 26 才有close，且close与release是一样的

                if(picture.get() != null){
                    bitmap = BitmapFactory.decodeByteArray(picture.get(), 0, picture.get().length);
                }
                metadataRetriever.clear();
                picture.clear();
            }catch (Exception e){
                e.printStackTrace();
            }

        }
        //返回默认的专辑封面
        return bitmap;
    }
}
