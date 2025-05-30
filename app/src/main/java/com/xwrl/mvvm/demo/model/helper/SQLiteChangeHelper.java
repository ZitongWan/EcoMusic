package com.xwrl.mvvm.demo.model.helper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.xwrl.mvvm.demo.service.manager.LastMetaManager;

import java.lang.ref.WeakReference;

@SuppressLint("Range")
public class SQLiteChangeHelper {
    private static final String TAG = "SQLiteChangeHelper";
    public static final String UserDBName = "musicList.db";

    public static final int EDIT_SHEET_TYPE_ALIAS = 0x01;
    public static final int EDIT_SHEET_TYPE_COVER = 0x02;
    public static final int EDIT_SHEET_TYPE_OTHER = 0x03;

    protected static String getTableName(SQLiteDatabase database, String sheetAlias){
        String tableName = "";

        if (database == null || !database.isOpen() ||
                sheetAlias == null || TextUtils.isEmpty(sheetAlias)) { return tableName; }

        //1.根据歌单别名，查询歌单数据表{@SongSheet_table}中对应的歌单名
        String sql = "select title from SongSheet_table where alias=?";
        //Log.d("", "getTableName: "+sheetAlias);
        WeakReference<Cursor> cursor = getCursorWeak(database.rawQuery(sql,new String[]{sheetAlias}));
        //Log.d("", "查询到："+cursor.getCount());
        if (cursor.get().moveToNext() && cursor.get().getCount() > 0){
            tableName = cursor.get().getString(0);
        }
        closeCursorWeak(cursor);

        return tableName;
    }

    public static boolean isSameTable(Context context, String alias){
        WeakReference<SQLiteOpenHelper> helperWeak = getHelperWeak(context);
        SQLiteDatabase database = helperWeak.get().getReadableDatabase();

        if (database == null || !database.isOpen()) {
            releaseHelper(database,helperWeak);
            return false;
        }

        boolean isSame = false;
        WeakReference<Cursor> cursor = null;
        try {
            //正式创建新歌单之前，进行 歌单别名 去重 操作
            cursor = getCursorWeak(database.query("SongSheet_table",new String[]{"alias"},
                    "alias = ?",new String[]{alias},
                    null,null,null));
            isSame = cursor.get().getCount() == 0;
            closeCursorWeak(cursor);//释放Cursor资源
            releaseHelper(database,helperWeak);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeCursorWeak(cursor);//释放Cursor资源
            releaseHelper(database,helperWeak);
        }
        return isSame;
    }

    //TODO: 以下为本类资源引用和释放
    public static WeakReference<Cursor> getCursorWeak(Cursor cursor){
        return new WeakReference<>(cursor);
    }

    public static void closeCursorWeak(WeakReference<Cursor> cursor){
        if (cursor == null) return;
        if(cursor.get() != null) {
            cursor.get().close();
            Log.d(TAG, "关闭游标工具 "+cursor.get().isClosed());
        }
        cursor.clear();
    }

    public static WeakReference<SQLiteOpenHelper> getHelperWeak(Context application){
        return getHelperWeak(application, getCurrentDBName(application));
    }

    public static WeakReference<SQLiteOpenHelper> getHelperWeak(@NonNull Context application, String DBName){
        if (DBName == null || TextUtils.isEmpty(DBName)) {
            Log.e(TAG, "getHelperWeak: 获取临时数据库对象失败！ 已设置为默认数据库名。 DBName = "+DBName);
            DBName = UserDBName;
        }
        WeakReference<Context> context = new WeakReference<>(application);

        WeakReference<SQLiteOpenHelper> R = new WeakReference<>(
                                                    new SQLiteOpenHelper(context.get(),
                                                            DBName, null, 1));
        context.clear();

        return R;
    }
    protected static String getCurrentDBName(Context application){
        if (application == null) {
            Log.d(TAG, "getCurrentDBName: application == null");
            return UserDBName;
        }
        LastMetaManager lastMetaManager = new LastMetaManager(application);
        String dbName = lastMetaManager.getUserDBName();
        lastMetaManager.onDestroy();
        //Log.d(TAG, "getCurrentDBName: "+dbName);
        return dbName;
    }

    public static boolean isStrangerLogin(Context application){
        return UserDBName.equals(getCurrentDBName(application));
    }

    public static void releaseHelper(SQLiteDatabase database,
                                      WeakReference<SQLiteOpenHelper> helperWeak){

        if (database != null && database.isOpen()) { database.close(); }

        if (helperWeak != null && helperWeak.get() != null) {
            helperWeak.get().close();
            helperWeak.clear();
        }
    }
}
