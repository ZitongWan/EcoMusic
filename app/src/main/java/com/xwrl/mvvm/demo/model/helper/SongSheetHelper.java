package com.xwrl.mvvm.demo.model.helper;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;
import android.util.Log;

import com.xwrl.mvvm.demo.R;
import com.xwrl.mvvm.demo.bean.SongSheetBean;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * 作用: 歌单管理的帮助类，主要负责歌单与歌单总表的增、删、改、查的数据返回
 */
@SuppressLint("Range")
public class SongSheetHelper extends SQLiteChangeHelper{

    /**
     * TODO：【增】
     * 增加一张歌单-数据表以及在歌单总表中的记录
     * */
    public static void createNewSheet(Object o, String alias){
        if (o instanceof Context) createNewSheet((Context) o, alias);
    }

    /**用户创建一张新的歌单 的具体步骤
     * 1.传入歌单数据库对象，以及用户输入的歌单别名，去重操作在创建歌单之前
     * 2.查询歌单数据表{@link \SongSheet_table}的行数（默认为一行），
     *                      生成歌单别名对应的歌单名，格式为Tb_user{歌单数据表的行数}
     * 3.根据歌单名，在歌单数据库中创建歌单数据表Tb_user{歌单数据表的行数}
     * 4.将本次创建的歌单数据表信息插入至歌单数据表{@link \SongSheet_table}中
     * 5.通知歌单列表设配器{@link com.xwrl.mvvm.demo.adapter.SongSheetAdapter}数据更新*/
    private static void createNewSheet(Context application, String alias) {
        WeakReference<SQLiteOpenHelper> helperWeak = getHelperWeak(application);

        SQLiteDatabase database = helperWeak.get().getReadableDatabase();
        if (database == null || !database.isOpen()) { releaseHelper(database,helperWeak); return ; }

        //1.生成歌单别名对应的歌单名
        String countReview = "SELECT COUNT(*) FROM SongSheet_table";
        SQLiteStatement statement = database.compileStatement(countReview);
        int count = (int)statement.simpleQueryForLong();
        statement.close();//释放SQLiteStatement资源
        String songSheetName = "Tb_user"+count;
        //2.创建歌单数据表Tb_user{SongSheet_table的行数}
        database.execSQL("create table if not exists "+songSheetName+"(" +
                "_id integer primary Key autoincrement," +
                "title text not null," +
                "artist text not null," +
                "album text not null," +
                "albumPath text not null," +
                "Duration integer not null," +
                "Path text not null)");
        //3.将本次创建的歌单数据表信息插入至歌单数据表{@SongSheet_table}中
        ContentValues values = new ContentValues();
        values.put("title",songSheetName);
        values.put("firstAlbumPath","");
        values.put("alias",alias);
        database.insert("SongSheet_table",null,values);
        values.clear();//释放ContentValues资源
        //4.将通知歌单列表设配器 歌单数据更新
        //关闭数据库
        releaseHelper(database, helperWeak);
    }

    /**
     * TODO：【删】
     * 删除一张歌单-数据表以及在歌单总表中的记录
     * */
    public static void deleteSongSheet(Object o, String alias){
        if (o instanceof Context) deleteSongSheet((Context) o, alias);
    }

    /**用户删除一张歌单 的具体步骤
     * 1.传入歌单数据库对象，以及用户输入的歌单别名【我的收藏歌单不能被删除，在点击删除时判定】
     * 2.根据歌单别名，查询歌单数据表{@link \SongSheet_table}中对应的歌单名
     * 3.根据歌单名，删除该歌单（数据表）
     * 4.将本次删除的歌单数据表信息从歌单数据表{@link \SongSheet_table}中删除
     * 5.通知歌单列表设配器{@link com.xwrl.mvvm.demo.adapter.SongSheetAdapter}数据更新*/
    private static void deleteSongSheet(Context context, String alias) {
        WeakReference<SQLiteOpenHelper> helperWeak = getHelperWeak(context);

        SQLiteDatabase database = helperWeak.get().getReadableDatabase();
        if (database == null || !database.isOpen()) { releaseHelper(database,helperWeak); return ; }

        String sheetName = getTableName(database,alias);

        if (TextUtils.isEmpty(sheetName) || sheetName.equals("Tb_MyLoveList")) { return; }
        //2.根据歌单名，删除该歌单（数据表）
        database.execSQL("DROP TABLE "+sheetName);
        //3.将本次删除的歌单数据表信息从歌单数据表{@SongSheet_table}中删除
        database.delete("SongSheet_table","title=?",new String[]{sheetName});
        //4.将通知歌单列表设配器{@SongSheetAdapter}数据更新

        //5.最后关闭数据库
        releaseHelper(database, helperWeak);
    }

    /**
     * TODO：【改】
     * 修改歌单在歌单总表的信息
     * */
    public static String editSongSheet(Object o, String alias, String newAlias, int type){
        return o instanceof Context ?
                editSongSheet(getHelperWeak((Context) o), alias,
                        newAlias, ((Context) o).getResources(), type) : null;
    }

    private static String editSongSheet(WeakReference<SQLiteOpenHelper> helperWeak,
                                        String alias, String newAlias, Resources resources, int type){
        Log.d("aaa", "editSongSheet: "+(alias == null)+(TextUtils.isEmpty(alias))+
                (helperWeak == null)+(newAlias == null)+(TextUtils.isEmpty(newAlias))+(resources == null));

        if (alias == null || TextUtils.isEmpty(alias) || helperWeak == null || alias.equals("最近很喜欢") ||
                newAlias == null || TextUtils.isEmpty(newAlias) || resources == null) return null;


        SQLiteDatabase database = helperWeak.get().getReadableDatabase();

        if (database == null || !database.isOpen()) {
            releaseHelper(database,helperWeak);
            return null;
        }

        String where = "alias";

        switch (type) {
            case EDIT_SHEET_TYPE_ALIAS:
                where = "alias";
                break;
            case EDIT_SHEET_TYPE_COVER:
                where = "firstAlbumPath";
                break;
            default:
                //where = "other";
                break;
        }
        Log.d("aaa", "editSongSheet: "+where+", change: "+newAlias+", aboriginal = "+alias);
        ContentValues values = new ContentValues();
        values.put(where,newAlias);

        database.update(resources.getString(R.string.label_SheetTableName),
                                values,
                                "alias=?",
                                new String[]{alias});

        //通知MineFragment的歌单列表适配器{@SongSheetAdapter}更新数据
        values.clear();

        releaseHelper(database,helperWeak);

        return "歌单名称修改成功！新歌单名："+newAlias;
    }

    /**
     * TODO: 【查】
     * 获得数据库里面可以显示的数据表-歌单
     * @param o 最好是弱引用，用完就释放掉，不然可能会输出resource failed
     * */
    public static List<SongSheetBean> getDataBaseSongSheet(Object o){
        return o instanceof Context ?
                getDataBaseSongSheet(getHelperWeak((Context) o)) : null;
    }
    public static List<SongSheetBean> getDataBaseSongSheet(WeakReference<SQLiteOpenHelper> helperWeak){
        List<SongSheetBean> mDatas = new ArrayList<>();
        SQLiteDatabase database = helperWeak.get().getReadableDatabase();

        if(database == null || !database.isOpen()) {
            SQLiteChangeHelper.releaseHelper(database,helperWeak);
            mDatas.add(new SongSheetBean("参数为空"));
            return mDatas;
        }
        WeakReference<Cursor> cursor = null;
        String alias,title,firstAlbumPath,count;
        try {
            cursor = getCursorWeak(database.rawQuery("select * from SongSheet_table", null));
            //System.out.println("title = ");
            while (cursor.get() != null && cursor.get().moveToNext()) {
                alias = cursor.get().getString(cursor.get().getColumnIndex("alias"));

                if (alias.equals("上次播放")) continue;
                if (alias.equals("下载历史")) continue;
                if (alias.equals("搜索历史")) continue;

                firstAlbumPath = cursor.get().getString(cursor.get().getColumnIndex("firstAlbumPath"));
                title = cursor.get().getString(cursor.get().getColumnIndex("title"));
                //Log.d("model", "title: "+title+" ,alias: "+alias);

                String countReview = "SELECT COUNT(*) FROM "+title;
                SQLiteStatement statement = database.compileStatement(countReview);
                count = (int)statement.simpleQueryForLong()+"首";
                statement.close();

                SongSheetBean sheetBean = new SongSheetBean(alias,firstAlbumPath,count);
                mDatas.add(sheetBean);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeCursorWeak(cursor);
            releaseHelper(database, helperWeak);
        }
        if(mDatas.isEmpty()) mDatas.add(new SongSheetBean("查询到0张歌单"));
        return mDatas;
    }
}
