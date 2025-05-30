package com.xwrl.mvvm.demo.model.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

public class SQLiteOpenHelper extends android.database.sqlite.SQLiteOpenHelper {
    private static final String TAG = "SQLiteOpenHelper";
    public static final String STRING_SONG_TITLE = "title";
    public static final String STRING_SONG_ARTIST = "artist";
    public static final String STRING_SONG_ALBUM = "album";
    public static final String STRING_SONG_ALBUM_PATH = "albumPath";
    public static final String STRING_SONG_PATH = "Path";
    public static final String STRING_SONG_DURATION = "Duration";
    public static final String STRING_SONG_CLICK = "click";
    private boolean isStranger;

    /**
     * @param context 上下文对象
     * @param name  数据库名称 , 前可加绝对路径，在指定文件夹下创建本数据库
     * @param factory  游标工具null
     * @param version  数据库版本
     * */
    public SQLiteOpenHelper(@Nullable Context context,
                            @Nullable String name,
                            @Nullable SQLiteDatabase.CursorFactory factory,
                            int version) {
        super(context, name, factory, version);
        isStranger = SQLiteChangeHelper.UserDBName.equals(name);
        Log.d(TAG, "SQLiteOpenHelper: 当前数据库名 "+name);
    }
    public SQLiteOpenHelper(@Nullable Context context,
                            @Nullable String name) {
        super(context, name, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        //收录用户登录信息的数据表，默认创建不显示.并且只有在默认数据库里面才创建
        if (isStranger) {
            db.execSQL("create table if not exists " +
                    "UserInfo_table(_id integer primary Key autoincrement," +
                    "title text not null," +    //用户名
                    "firstAlbumPath text," +    //用户头像地址
                    "password text," +    //用户头像地址
                    "alias text," +    //用户头像地址
                    "dbName text," +    //用户数据库名称
                    "signature text not null)");    //个性签名
        }

        //★【歌单总表】收录歌单名的数据表：默认创建不显示
        db.execSQL("create table if not exists " +
                "SongSheet_table(_id integer primary Key autoincrement," +
                "title text not null," +    //歌单名
                "firstAlbumPath text," +    //排序第一首歌曲的专辑图片地址
                "alias text not null)");    //歌单别名
        /*
         * 创建历史播放收藏表 HistoryPlay_table  默认创建在小窗口列表显示，收录入SongSheet_table,但不显示
         * @_id 序号 integer {primary Key autoincremen}增加一行数据时，自动添加序列号
         * @title 歌曲名 text
         * @artist 歌手名 text
         * @album 专辑名 text
         * @albumPath 专辑路径名 text
         * @Path 文件路径名 text
         * @Duration 歌曲时长 integer 类型为long
         * @click 点击次数 integer 类型为long 重复歌名写入则将此项数据加1
         * @firstTime 第一次插入此数据表的时间 integer 类型为long 重复插入则只更新@click项
         * */
        db.execSQL("create table if not exists HistoryPlay_table(" +
                "_id integer primary Key autoincrement," +
                "title text not null," +
                "artist text not null," +
                "album text not null," +
                "albumPath text not null," +
                "Duration integer not null," +
                "Path text not null,"+
                "click integer not null,"+
                "firstTime text not null)");
        insertSheetAlias(db,"HistoryPlay_table","上次播放");

        db.execSQL("create table if not exists DownloadMusic_table(" +
                "_id integer primary Key autoincrement," +
                "title text not null," +
                "artist text not null," +
                "album text not null," +
                "albumPath text not null," +
                "Duration integer not null," +
                "Path text not null," +
                "Date integer not null)");
        insertSheetAlias(db,"DownloadMusic_table","下载历史");

        db.execSQL("create table if not exists SearchHistory_table(" +
                "_id integer primary Key autoincrement," +
                "title text not null," +
                "click integer not null)");
        Log.d("dataHelper", "onCreate: ");
        insertSheetAlias(db,"SearchHistory_table","搜索历史");
        /*
         * 创建我的歌单收藏表 Tb_MyLoveList  默认创建收录入SongSheet_table
         * @_id 序号 integer {primary Key autoincremen}增加一行数据时，自动添加序列号
         * @title 歌曲名 text
         * @artist 歌手名 text
         * @album 专辑名 text
         * @albumPath 专辑路径名 text
         * @Path 文件路径名 text
         * @Duration 歌曲时长 integer 类型为long
         * */
        db.execSQL("create table if not exists Tb_MyLoveList(" +
                "_id integer primary Key autoincrement," +
                "title text not null," +
                "artist text not null," +
                "album text not null," +
                "albumPath text not null," +
                "Duration integer not null," +
                "Path text not null)");
       insertSheetAlias(db,"Tb_MyLoveList","最近很喜欢");
    }
    private void insertSheetAlias(SQLiteDatabase database,String title,String alias){
        Log.d("insertSheetAlias: ", "title"+title);
        if (database == null) return;
        if (TextUtils.isEmpty(title)) return;

        WeakReference<Cursor> cursor = null;
        int count;

        try {
            cursor = SQLiteChangeHelper.getCursorWeak(database.query("SongSheet_table",new String[]{"title"},
                    "title = ?",new String[]{title},
                    null,null,null));

            if (cursor.get() != null) {
                count = cursor.get().getCount();
                SQLiteChangeHelper.closeCursorWeak(cursor);
            }else return;

            if (count == 0) {
                ContentValues values = new ContentValues();
                values.put("title",title);
                values.put("firstAlbumPath","");
                values.put("alias",alias);  //此处存入用户自定义的歌单名
                //replace:不存在就插入，存在就更新
                database.insert("SongSheet_table",null,values);
                values.clear();
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            SQLiteChangeHelper.closeCursorWeak(cursor);
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion) {
            Log.d("helper", "数据库有新版本");
            db.execSQL("create table if not exists SearchHistory_table(" +
                    "_id integer primary Key autoincrement," +
                    "title text not null," +
                    "click integer not null)");
            Log.d("dataHelper", "onCreate: ");
            insertSheetAlias(db,"SearchHistory_table","搜索历史");
        }
    }
}
