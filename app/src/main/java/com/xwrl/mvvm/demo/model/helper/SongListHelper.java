package com.xwrl.mvvm.demo.model.helper;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.media.MediaMetadataCompat;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.xwrl.mvvm.demo.BaseActivity;
import com.xwrl.mvvm.demo.bean.MusicBean;
import com.xwrl.mvvm.demo.bean.SongSheetBean;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;

@SuppressLint("Range")
public class SongListHelper extends SQLiteChangeHelper{
    private static final String TAG = "SongListHelper";

    /**
     * TODO：【增】
     * 收藏一首歌曲至某张歌单里
     * */
    public static Bundle collectionSong(Object o, String alias, MusicBean bean){
        return o instanceof Context ? collectionSong((Context) o,alias,bean) : null;
    }
    @SuppressLint("Range")
    private static Bundle collectionSong(Context application, String alias,
                                         MusicBean bean){

        WeakReference<SQLiteOpenHelper> helperWeak = getHelperWeak(application);

        SQLiteDatabase database = helperWeak.get().getReadableDatabase();
        if (database == null || !database.isOpen()) {
            releaseHelper(database,helperWeak);
            return null;
        }
        //添加歌曲至指定歌单,先获取到该歌单的数据表名
        String table = getTableName(database,alias);
        Bundle bundle = insertMusicToSheet(database, table, bean);

        releaseHelper(database,helperWeak);
        return bundle;
    }

    private static Bundle insertMusicToSheet(SQLiteDatabase database, String table, MusicBean bean){
        String TAG = "insertMusicToSheet";
        Bundle bundle = new Bundle();

        //Log.d(TAG, "insertMusicToSheet: "+table);
        if (bean == null || TextUtils.isEmpty(table)) return bundle;//收藏该歌曲失败
        if (database == null || !database.isOpen()) return bundle;//收藏该歌曲失败

        String title = bean.getTitle();
        String artist = bean.getArtist();
        String album = bean.getAlbum();
        String albumPath = bean.getAlbumPath();
        String path = bean.getPath();
        long duration = bean.getDuration();
        Log.d(TAG, "title "+title+", artist "+artist+", album "+album+", duration "+duration);

        if (TextUtils.isEmpty(path)) return null;//收藏该歌曲失败
        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(artist)) return null;//收藏该歌曲失败

        //Log.d(TAG, "insertMusicToSheet: 开始插入");
        WeakReference<Cursor> cursor = null;
        bundle.putString("InsertTableName",table);
        try {
            cursor = getCursorWeak(database.query(table,new String[]{"_id"},
                    "title=? and artist=? and album=?",new String[]{title,artist,album},
                    null,null,null));
            int count = -1;
            boolean isSheetTopSong = false;
            if(cursor.get() != null) {
                count = cursor.get().getCount();
                closeCursorWeak(cursor);
                cursor = getCursorWeak(database.rawQuery("SELECT * FROM "+table+" order by _id desc limit 1",null));
                if (cursor.get() != null && cursor.get().moveToNext()){
                    String descTitle = cursor.get().getString(cursor.get().getColumnIndex("title")),
                            descArtist = cursor.get().getString(cursor.get().getColumnIndex("artist")),
                            descAlbum = cursor.get().getString(cursor.get().getColumnIndex("album"));

                    Log.d(TAG, "insertMusicToSheet: "+descTitle+", "+descArtist+", "+descAlbum);

                    isSheetTopSong = title.equals(descTitle) && artist.equals(descArtist) &&
                            album.equals(descAlbum);
                    closeCursorWeak(cursor);
                }
            }
            if (isSheetTopSong){ //该歌曲已经在第一行了
                bundle.putBoolean("isSheetTopSong",true);
                return bundle;
            }
            Log.d(TAG, "insertMusicToSheet: "+table+" 有"+count+" 首");

            ContentValues values = new ContentValues();
            if (count > 0) {
                bundle.putString("UpdateMsg","[355]：歌曲名重复,删除该项，重新插入");
                //2.根据歌单名，歌曲名，从该歌单数据表删除该项数据（删除歌曲）
                database.delete(table, "title=?", new String[]{title});
                //当前歌曲在该歌单置顶处理
                bundle.putBoolean("isNeedTopSong",true);
            }
            //Log.d("writeSheet", "LovedMusic: "+duration);
            values.put("title", title);
            values.put("artist", artist);
            values.put("album", album);
            values.put("albumPath", albumPath);//在线和本地再判断
            values.put("Duration", duration);
            values.put("Path", path);
            if (table.equals("DownloadMusic_table")) values.put("Date", System.currentTimeMillis());

            database.insert(table, null, values);
            values.clear();
            //更新第一首歌的专辑图片地址
            values.put("firstAlbumPath", albumPath);
            database.update("SongSheet_table", values, "title=?", new String[]{table});
            values.clear();
        }catch (Exception e){
            Log.d(TAG, "insertMusicToSheet：获取模糊查询数据异常 "+e);
        }finally {
            closeCursorWeak(cursor);
        }
        bundle.putBoolean("isSuccessful",true);
        return bundle;
    }

    /**
     * TODO：【删】
     * 删除某张歌单里的一首歌曲
     * */
    public static String deleteSheetSong(Object o, String alias, MusicBean bean) {

        return o instanceof Context ?
                deleteSheetSong(getHelperWeak((Context) o),alias,bean,((Context) o).getResources()) : null;
    }
    /**用户删除一张歌单里的一首歌曲 的具体步骤
     * 1.传入歌单数据库对象，歌单别名,歌曲名【歌曲名得在歌单数据表能查到】
     * 2.根据歌单别名，查询歌单数据表{@link \SongSheet_table}中对应的歌单名
     * 3.根据歌单名，歌曲名，从该歌单数据表删除该项数据
     * 4.删除操作后，查询该数据表，其结果作为一个
     * List<{@link android.support.v4.media.session.MediaSessionCompat.QueueItem}>的歌曲信息集合
     * 返回给{@link com.xwrl.mvvm.demo.adapter.MusicAdapter}
     * 5.通知歌曲列表设配器{@link com.xwrl.mvvm.demo.adapter.MusicAdapter}数据更新*/
    private static String deleteSheetSong(WeakReference<SQLiteOpenHelper> helperWeak,
                                          String alias, MusicBean bean, Resources resources) {

        if (alias == null || TextUtils.isEmpty(alias) || helperWeak == null ||
                bean == null || resources == null) return null;

        SQLiteDatabase database = helperWeak.get().getReadableDatabase();

        if (database == null || !database.isOpen()) {
            releaseHelper(database,helperWeak);
            return null;
        }
        StringBuilder returnMsg = new StringBuilder();
        String title = bean.getTitle(),
                artist = bean.getArtist(),
                album = bean.getAlbum();

        //1.查询到歌单名
        String sheetName = getTableName(database,alias);
        if (!TextUtils.isEmpty(sheetName)) {
            //Log.d("", "查询到的歌单名为 "+sheetName);
            //2.根据歌单名，歌曲名，判断该歌曲是否是该歌单的最后一首歌，如果是则更新歌单封面，
            // 然后从该歌单数据表删除该项数据（删除歌曲）
            WeakReference<Cursor> cursor = getCursorWeak(
                    database.rawQuery("SELECT * FROM " + sheetName + " ORDER BY _id DESC", null));//倒序查询
            if (cursor.get() != null && cursor.get().moveToNext()) {
                String lastTitle = cursor.get().getString(cursor.get().getColumnIndex("title"));
                String lastArtist = cursor.get().getString(cursor.get().getColumnIndex("artist"));
                String lastAlbum = cursor.get().getString(cursor.get().getColumnIndex("album"));
                String albumPath = "";

                if (cursor.get().moveToNext())
                    albumPath = cursor.get().getString(cursor.get().getColumnIndex("albumPath"));
                closeCursorWeak(cursor);

                if (lastTitle.equals(title) && lastArtist.equals(artist) &&
                        lastAlbum.equals(album) && !TextUtils.isEmpty(albumPath)) {
                    //更新歌单封面
                    ContentValues values = new ContentValues();
                    //更新第一首歌的专辑图片地址
                    values.put("firstAlbumPath", albumPath);
                    database.update("SongSheet_table", values, "title=?", new String[]{sheetName});
                    values.clear();
                }
            }
            int i = database.delete(sheetName, "title=? and artist=? and album=?",
                    new String[]{title, artist, album});
            returnMsg.append("您").append((i == 1 ? "已" : "未")).append("删除歌曲 ").append(title);

        }else returnMsg.append("ErrorMsg : 未查询到 ").append(alias).append("歌单的 ").append(title).append(" 歌曲");

        releaseHelper(database,helperWeak);

        return returnMsg.toString();
    }

    /**
     * TODO：【改】
     * 更改歌单中记录的某首歌曲的信息，有一定限制
     * */
    public static boolean editSheetMusic(Object o, String oldPath, String alias,
                                         MediaMetadataCompat metadata){

        return o instanceof Context && editSheetMusic((Context) o, alias, metadata);
    }

    /**
     * @param application 上下文对象，在{@link SQLiteChangeHelper}中获取数据库对象
     * @param metadata 一首歌曲的信息集合
     * @param alias 歌单别名
     * */
    @SuppressLint("Range")
    private static boolean editSheetMusic(Context application,
                                          String alias,
                                          MediaMetadataCompat metadata){

        if (application == null || metadata == null ) return false;

        WeakReference<SQLiteOpenHelper> helperWeak = getHelperWeak(application);
        SQLiteDatabase database = helperWeak.get().getReadableDatabase();

        if(database == null || !database.isOpen()) {
            releaseHelper(database,helperWeak);
            return false;
        }

        String newTitle = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE),
                newArtist = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST),
                newAlbum = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM),
                newPath = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI);

        ContentValues values = new ContentValues();
        try {
            //获取歌单的数据表名
            String table = getTableName(database, alias);
            //Log.d("SheetModel", "EditSheetSong 查询到的数据表名: "+table);
            if(newTitle != null && !TextUtils.isEmpty(newTitle)) values.put("title",newTitle);
            if(newArtist != null && !TextUtils.isEmpty(newArtist)) { values.put("artist",newArtist); }
            if(newAlbum != null && !TextUtils.isEmpty(newAlbum)) values.put("album",newAlbum);

            if(newPath != null && !TextUtils.isEmpty(newPath) &&
                    newPath.contains(".mp3") && MusicHelper.FileExists(newPath)) {
                values.put("Path",newPath);
                values.put("albumPath",newPath);
            }

            if (values.size() > 0) {
                //Log.d(TAG, "EditSheetSong: "+values.size());
                int i = database.update(table, values,
                        "title=? and artist=? and album=?",
                        new String[]{newTitle,newArtist,newAlbum});
                Log.d(TAG, "editSheetMusic: 歌单更新 "+newPath+" 结果 "+i);
                return true;
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            releaseHelper(database,helperWeak);
            if (values.size() > 0) values.clear();
        }
        return false;
    }

    /**
     * TODO：【查】
     * 查询默认收藏歌单里是否收藏了这首歌曲
     * */
    public static boolean isSongLoved(Object object, String title){
        //Log.d("数据库帮助类", "isSongLoved: W/Sys");
        return object instanceof Context && isSongLoved(getHelperWeak((Context) object), title);
    }
    public static boolean isSongLoved(WeakReference<SQLiteOpenHelper> helperWeak, String title){
        if (title == null || TextUtils.isEmpty(title) || helperWeak == null) return false;

        boolean isLoved = false;
        SQLiteDatabase database = helperWeak.get().getReadableDatabase();
        if (database == null || !database.isOpen()){
            releaseHelper(database,helperWeak);
            return false;
        }
        //查询我的收藏（最近喜欢听）歌单里是否有此歌曲
        WeakReference<Cursor> cursor = null;
        try {
            cursor = getCursorWeak(database.query("Tb_MyLoveList",new String[]{"title"},
                    "title = ?",new String[]{title},
                    null,null,null));

            if (cursor.get() != null) {
                int count = cursor.get().getCount();
                if (count > 0) isLoved = true;
            }
            closeCursorWeak(cursor);

        }catch (Exception e){
            Log.e("SheetModel", "isSongLoved：获取模糊查询数据异常");

        }finally {
            releaseHelper(database,helperWeak);
            closeCursorWeak(cursor);
        }

        return isLoved;
    }
    /**
     * TODO：【查】
     * 查询 该歌单里这首歌曲的 总时长
     * 一般用于在线音乐存进歌单，但是{@link android.support.v4.media.session.MediaSessionCompat.QueueItem}
     * 和{@link android.support.v4.media.MediaBrowserCompat.MediaItem} 不会存有总时长属性。
     * 真是JR梆梆JR硬！！！还不能 通过 继承为子类 存放总时长。
     * @param alias 歌单别名 由此向歌单总表查询 总时长。
     * @return 歌曲总时长
     * */
    public static String getMusicDuration(@NonNull Context o, String title,
                                          String artist, String album, String alias){
        Log.d(TAG, "在线音乐 getMusicDuration: 输入字符串");
        return getMusicDuration(getHelperWeak(o), title, artist, album, alias);
    }
    public static String getMusicDuration(@NonNull Context o, MediaMetadataCompat metadata, String alias){
        Log.d(TAG, "在线音乐 getMusicDuration: 输入metadata");
        if (metadata == null) { return "0"; }
        String title = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
        String artist = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
        String album = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
        return getMusicDuration(getHelperWeak(o), title, artist, album, alias);
    }

    private static String getMusicDuration(WeakReference<SQLiteOpenHelper> helperWeak,
                                         String title, String artist,
                                         String album, String alias){
        String duration = "0";
        if (helperWeak == null|| title == null || TextUtils.isEmpty(title)) return duration;
        if (artist == null || TextUtils.isEmpty(artist)) return duration;
        if (album == null || TextUtils.isEmpty(album)) return duration;
        if (alias == null || TextUtils.isEmpty(alias)) return duration;

        SQLiteDatabase database = helperWeak.get().getReadableDatabase();
        if (database == null || !database.isOpen()){
            releaseHelper(database,helperWeak);
            return duration;
        }
        /*Log.d(TAG, "在线音乐 getMusicDuration: title = "+title+", artist = "+artist+", album = "+album+", alias = "+alias);*/
        String tableName = getTableName(database, alias);
        WeakReference<Cursor> cursor = null;
        try {
            cursor = getCursorWeak(database.rawQuery(
                    "SELECT * FROM " + tableName + " WHERE title=? AND artist=? AND album=?",
                    new String[]{title, artist, album}));// 条件查询
            //Log.d(TAG, "getMusicDuration: 查询到的歌曲数量 "+cursor.get().getCount());
            cursor.get().moveToFirst();
            duration = cursor.get().getString(
                                cursor.get().getColumnIndex(SQLiteOpenHelper.STRING_SONG_DURATION));
            //Log.d(TAG, "getMusicDuration: "+duration);
        }catch (Exception exception){
            exception.printStackTrace();
            return duration;
        }finally {
            closeCursorWeak(cursor);
            releaseHelper(database, helperWeak);
        }

        return duration;
    }

    /**
     * TODO：【查】
     * 查询 本地音乐 与 某一歌单 里的 全部歌曲
     * */
    public static LinkedHashMap<String, MediaMetadataCompat> getSongSheetMusic(
                                        Application application, String alias){

        LinkedHashMap<String, MediaMetadataCompat> result = new LinkedHashMap<>();
        Log.d(TAG, "getSongSheetMusic - alias: "+alias);
        WeakReference<SQLiteOpenHelper> helperWeak = null;
        SQLiteDatabase database = null;
        WeakReference<Cursor> cursor = null;

        try {
            if (alias.equals(BaseActivity.ACTIVITY_DEFAULT_SHEET_NAME)) {
                ContentResolver contentResolver = application.getContentResolver();
                if (contentResolver == null) return result;
                cursor = getCursorWeak(MusicHelper.getSystemLocalMusic(contentResolver));
                //Log.d(TAG, "initData: 歌曲数量："+cursor.getCount());
            }else {
                helperWeak = getHelperWeak(application);
                database = helperWeak.get().getReadableDatabase();
                if (TextUtils.isEmpty(alias) ||
                        database == null || !database.isOpen()) return result;

                String sql = "select title from SongSheet_table where alias=?";
                cursor = getCursorWeak(database.rawQuery(sql,new String[]{alias}));
                if (cursor.get() != null && cursor.get().moveToNext() && cursor.get().getCount() > 0) {
                    //2.根据查询到的歌单名将此首歌曲写进选择的歌单之中
                    String table = cursor.get().getString(
                                    cursor.get().getColumnIndex(SQLiteOpenHelper.STRING_SONG_TITLE));
                    closeCursorWeak(cursor);
                    //Log.d("model", "table: "+table);
                    //歌单歌曲使用倒序查询，使用户最新添加的一首歌曲处于首行位置
                    cursor = getCursorWeak(database.rawQuery("select * from "+table+" order by _id desc", null));
                }else { closeCursorWeak(cursor); }
            }
            long duration;
            String /*errorTitle,*/ title, artist, album, path, albumPath;

            while (cursor.get() != null && cursor.get().moveToNext() ) {

                if (alias.equals(BaseActivity.ACTIVITY_DEFAULT_SHEET_NAME)) {
                    duration = cursor.get().getLong(cursor.get().getColumnIndex(MediaStore.Audio.Media.DURATION));
                    if (duration < 90000) continue;//判断该mp3文件时长是否小于90秒，是则进行下一次循环 }
                    path = cursor.get().getString(cursor.get().getColumnIndex(MediaStore.Audio.Media.DATA));
                    albumPath = path;

                    title = cursor.get().getString(cursor.get().getColumnIndex(MediaStore.Audio.Media.TITLE));
                    title = title.replaceAll("&","/");
                    if (!title.contains("(") && title.contains("cover")){
                        String temp = title.substring(title.indexOf("cover"));
                        title = title.replace(temp,"("+temp+")")
                                .replace("cover","cover:");
                    }
                    artist = cursor.get().getString(cursor.get().getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    album = cursor.get().getString(cursor.get().getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    album = album.replaceAll("&","/");

                    //sid = String.valueOf(id);
                    if (title.contains("dyql") || artist.contains("dyql") || title.contains(" - ")) {
                        if (path.contains("Music/") && path.contains(" - ")) {
                            title  = path.substring(path.indexOf("Music/")+6,path.indexOf(" -"));
                            artist = path.substring(path.indexOf(" - ")+3,path.indexOf(".mp3"));
                        }
                    }
                    artist = artist.replaceAll("&","/");
                    //Log.d(alias+"歌单详情: ", "id = "+id+" title = "+title+" artist = "+artist+" album = "+album+" albumPath = "+path+" duration = "+duration+" path = "+path);
                } else {
                    path = cursor.get().getString(cursor.get().getColumnIndex(SQLiteOpenHelper.STRING_SONG_PATH));
                    title = cursor.get().getString(cursor.get().getColumnIndex(SQLiteOpenHelper.STRING_SONG_TITLE));
                    //Log.d(TAG, "getSongSheetMusic: "+MusicHelper.FileExists(path));

                    artist = cursor.get().getString(cursor.get().getColumnIndex(SQLiteOpenHelper.STRING_SONG_ARTIST));
                    artist = artist.replaceAll("&","/");
                    album = cursor.get().getString(cursor.get().getColumnIndex(SQLiteOpenHelper.STRING_SONG_ALBUM));
                    if (!album.contains(" & ")) album = album.replaceAll("&","/");
                    albumPath = cursor.get().getString(cursor.get().getColumnIndex(SQLiteOpenHelper.STRING_SONG_ALBUM_PATH));
                    String Duration = cursor.get().getString(cursor.get().getColumnIndex(SQLiteOpenHelper.STRING_SONG_DURATION));
                    duration = Long.parseLong(Duration);
//                    if (alias.equals("下载历史")) {
//                        String date = cursor.getString(cursor.getColumnIndex("Date"));
//                        sid = getTimeDifference(date);
//                        //Log.d("下载历史", "时间戳 "+sid);
//                    }else sid = String.valueOf(id);
                    //Log.d(alias+"歌单详情: ", "id = "+id+" song = "+title+" singer = "+artist+" album = "+album+" albumPath = "+albumPath+" duration = "+duration+" path = "+Path);
                }
                //Log.d(alias+"歌单详情: ", "id = "+0+" song = "+title+" singer = "+artist+" album = "+album+" albumPath = "+albumPath+" duration = "+duration+" path = "+path);
                //西班牙语重音字母替换
                title = MusicHelper.getMetaInfo(title,path,true);

                artist = MusicHelper.getMetaInfo(artist,path,false);
                artist = artist.replaceAll("&","/");

                if (TextUtils.isEmpty(album)) { album = "Music"; }

                String mediaId = MusicHelper.getMediaId(title, artist, album);
                //Log.d(alias+"歌单详情: ", "id = "+mediaId+" title = "+title+" artist = "+artist+" album = "+album+" albumPath = "+path+" duration = "+duration+" path = "+path);

                result.put(mediaId,
                        MusicHelper.getMediaMetadata(mediaId,title,artist,album, albumPath,path,duration));
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            //Log.w(TAG, "getSongSheetMusic: finally");
            if (cursor != null) { closeCursorWeak(cursor); }
            if (helperWeak != null) { SQLiteChangeHelper.releaseHelper(database,helperWeak); }
        }
        if (result.isEmpty()) new SongSheetBean(alias+"歌单歌曲 0首");
        return result;
    }
}
