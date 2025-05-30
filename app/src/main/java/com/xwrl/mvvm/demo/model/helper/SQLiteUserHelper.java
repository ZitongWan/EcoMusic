package com.xwrl.mvvm.demo.model.helper;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.xwrl.mvvm.demo.bean.UserBean;
import com.xwrl.mvvm.demo.service.manager.LastMetaManager;
import com.xwrl.mvvm.demo.util.StringUtil;

import java.lang.ref.WeakReference;


@SuppressLint("Range")
public class SQLiteUserHelper extends SQLiteChangeHelper{
    private static final String TAG = "SQLiteUserHelper";

    /**
     * TODO：【增】
     * 在注册用户总表中新增一个用户
     * */
    public static int insertUserInfo(Object o, UserBean userBean, boolean isCheck){
        return o instanceof Context ?
                insertUserInfo(getHelperWeak((Context) o, UserDBName), userBean, isCheck) : -1;
    }

    private static int insertUserInfo(WeakReference<SQLiteOpenHelper> helperWeak,
                                      UserBean userBean, boolean isCheck){
        SQLiteDatabase database = helperWeak.get().getWritableDatabase();

        if (database == null || userBean == null) return 10;
        String user = userBean.getUser(), password = userBean.getPassword(),
                albumPath = userBean.getAlbumPath(), signature = userBean.getSignature();
        if (TextUtils.isEmpty(password) || TextUtils.isEmpty(password)) return 10;

        Log.d("insertUserInfo: ", "title"+user);
        WeakReference<Cursor> cursor = null;
        int count;

        try {
            cursor = SQLiteChangeHelper.getCursorWeak(database.query("UserInfo_table",new String[]{"title,password"},
                    "title = ? and password = ?",new String[]{user, password},
                    null,null,null));

            if (cursor.get() != null) {
                count = cursor.get().getCount();
                SQLiteChangeHelper.closeCursorWeak(cursor);
            }else return 10;

            Log.d(TAG, "insertUserInfo: 已查询，个数为 "+count);

            if (count == 0) {
                if (isCheck){ return count; }

                ContentValues values = new ContentValues();
                values.put("title",user);//用户别名
                values.put("firstAlbumPath",albumPath);  //封面图片地址
                values.put("password",password);  //此处存入用户账户的密码
                values.put("signature",signature);  //此处存入用户签名

                cursor = getCursorWeak(database.rawQuery("select * from UserInfo_table", null));
                values.put("alias","UUUUUUser"+cursor.get().getCount());  //此处存入用户账户的随机用户名
                String userDBName = "musicListUser"+cursor.get().getCount()+".db";
                values.put("dbName",userDBName);  //此处存入用户数据库命名
                Log.d(TAG, "insertUserInfo: 用户数据库命名为："+userDBName);

                //replace:不存在就插入，存在就更新
                database.insert("UserInfo_table",null,values);
                values.clear();
            }else { return -1; }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            SQLiteChangeHelper.closeCursorWeak(cursor);
            releaseHelper(database, helperWeak);
        }
        return 20;
    }

    /**
     * TODO：【改】
     * 用途：更新一条用户信息，以创建。
     * */
    public static int updateUserInfo(Object o, UserBean userBean){
        return o instanceof Context ?
                updateUserInfo(getHelperWeak((Context) o, UserDBName), userBean) : -1;
    }

    private static int updateUserInfo(WeakReference<SQLiteOpenHelper> helperWeak,
                                      UserBean userBean){
        SQLiteDatabase database = helperWeak.get().getWritableDatabase();

        if (database == null || userBean == null) return 10;

        userBean.logD("updateUserInfo");
        WeakReference<Cursor> cursor = null;
        int count;

        try {
            cursor = SQLiteChangeHelper.getCursorWeak(database.rawQuery(
                    "SELECT * FROM UserInfo_table WHERE title=? and password=?",
                    new String[]{userBean.getUser(), userBean.getPassword()}
            ));
            count = cursor.get().getCount();

            Log.d(TAG, "updateUserInfo: 已查询，个数为 "+count);

            if (count == 1) {
                ContentValues values = new ContentValues();
                //用户别名
                values.put("title", userBean.getUser());
                //专辑图片
                values.put("firstAlbumPath", userBean.getAlbumPath());
                //此处存入用户签名
                values.put("signature", userBean.getSignature());
                values.put("password", userBean.getPassword());
                values.put("alias", userBean.getAlias());

                //replace:不存在就插入，存在就更新
                int result = database.update("UserInfo_table", values,
                        "title=? and password=?",
                        new String[]{userBean.getUser(), userBean.getPassword()});
                values.clear();
                Log.d(TAG, "updateUserInfo: 数据库更改返回值 "+result);
            }else { return -1; }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            SQLiteChangeHelper.closeCursorWeak(cursor);
            releaseHelper(database, helperWeak);
        }
        return 20;
    }

    /**
     * TODO：【查】
     * 查询其余的用户信息
     * */
    public static UserBean queryUserInfo(Object o, String user, String password){
        return o instanceof Context ? queryUserInfo(getHelperWeak((Context) o, UserDBName), user, password) : null;
    }

    private static UserBean queryUserInfo(WeakReference<SQLiteOpenHelper> helperWeak,
                                          String user, String password){
        Log.d("queryUserInfo: ", "title: "+user);
        SQLiteDatabase database = helperWeak.get().getWritableDatabase();

        if (database == null) return null;
        if (TextUtils.isEmpty(user)) return null;

        WeakReference<Cursor> cursor = null;
        UserBean userBean = null;
        int count;

        try {
            cursor = SQLiteChangeHelper.getCursorWeak(database.rawQuery(
                    "SELECT * FROM UserInfo_table WHERE title=? and password=?",
                    new String[]{user, password}
            ));

            if (cursor.get() != null) {
                count = cursor.get().getCount();
            }else return null;

            Log.d(TAG, "insertUserInfo: 已查询，个数为 "+count);
            cursor.get().moveToFirst();
            if (count == 1) {
                String albumPath = cursor.get().getString(cursor.get().getColumnIndex("firstAlbumPath"));
                String signature = cursor.get().getString(cursor.get().getColumnIndex("signature"));
                String alias = cursor.get().getString(cursor.get().getColumnIndex("alias"));
                String dbName = cursor.get().getString(cursor.get().getColumnIndex("dbName"));
                Log.d(TAG, "queryUserInfo: albumPath = "+albumPath+
                        ", signature: "+signature+", alias: "+alias+", dbName: "+dbName);

                userBean = new UserBean(user,password,albumPath,signature);
                userBean.setAlias(alias);
                userBean.setUserDBName(dbName);
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            SQLiteChangeHelper.closeCursorWeak(cursor);
            releaseHelper(database, helperWeak);
        }
        return userBean;
    }

    public static boolean saveUserInfo(String name, String label, String iconPath,
                                        boolean isStranger,
                                        @NonNull Application application,
                                        @NonNull UserBean userBean){
        Log.d(TAG, "saveUserInfo: 用户名："+name+", 个性便笺："+label);
        if(userBean.getUser().equals(name) && userBean.getSignature().equals(label)
                &&(iconPath != null && iconPath.equals(userBean.getAlbumPath()))){
            Toast.makeText(application,"重复保存",Toast.LENGTH_SHORT).show();
            return false;
        }
        if(TextUtils.isEmpty(name) && TextUtils.isEmpty(label) && TextUtils.isEmpty(iconPath)) {
            Toast.makeText(application,"用户名、个性便笺和头像至少更改一个才能保存哦",Toast.LENGTH_SHORT).show();
            return false;
        }
        //限制用户名和个性便笺的长度
        if (!TextUtils.isEmpty(name) && StringUtil.isLegalLength(name, true)) {
            Toast.makeText(application,"用户名忒长了！",Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!TextUtils.isEmpty(label) && StringUtil.isLegalLength(label, false)) {
            Toast.makeText(application,"个性便笺忒长了！",Toast.LENGTH_SHORT).show();
            return false;
        }
        //符合设定，开始保存
        //Log.d(TAG, "name = "+name+",label = "+label+",path = "+UserPath);
        boolean isChangeResult;
        Log.d(TAG, "saveUserInfo: 是否为游客模式："+isStranger);
        LastMetaManager lastMetaManager = new LastMetaManager(application);
        if (isStranger) {
            lastMetaManager.saveStrangerInformation(name,label,iconPath);
            isChangeResult = true;
        }else {
            UserBean userBeanChange = new UserBean(userBean.getUser(),
                    userBean.getPassword(),
                    TextUtils.isEmpty(iconPath) ? userBean.getAlbumPath() : iconPath,
                    TextUtils.isEmpty(label) ? userBean.getSignature() : label);
            userBeanChange.setAlias(TextUtils.isEmpty(name) ? userBean.getAlias() : name);

            userBean.logD(TAG);
            int i = updateUserInfo(application, userBeanChange);
            isChangeResult = i == 20;
            if (isChangeResult){
                //数据库更新后才更新SharedPreferences缓存
                lastMetaManager.saveUserInformation(name,label,iconPath, userBeanChange.getUser());
            }
            Log.d(TAG, "saveUserInfo: 更新结果"+i);
        }
        Toast.makeText(application,"保存"+(isChangeResult ? "成功" : "失败")+"!",
                                                                    Toast.LENGTH_SHORT).show();

        lastMetaManager.onDestroy();
        //修改成功后记得更新MainActivity中的记录变量

        return isChangeResult;
    }
}
