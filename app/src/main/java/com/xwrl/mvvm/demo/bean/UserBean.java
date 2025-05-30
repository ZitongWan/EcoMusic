package com.xwrl.mvvm.demo.bean;

import android.text.TextUtils;
import android.util.Log;

public class UserBean {
    private String user; //用户名
    private String password; // 密码
    private String albumPath; //头像地址
    private String signature; //个性签名
    private String alias; //用户别名，可供修改保存
    private String userDBName; //用户数据库名

    public UserBean(String user, String password, String albumPath, String signature) {
        this.user = user;
        this.password = password;
        this.albumPath = albumPath;
        this.signature = signature;
    }

    public String getUser() { return user == null || TextUtils.isEmpty(user) ? "用户名12138" : user; }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAlbumPath() {
        return albumPath == null || TextUtils.isEmpty(albumPath) ? "default" : albumPath;
    }

    public void setAlbumPath(String albumPath) {
        this.albumPath = albumPath;
    }

    public String getSignature() {
        return signature == null || TextUtils.isEmpty(signature) ? "这是一条个性签名" : signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getAlias() {
        return alias;
    }

    public UserBean setAlias(String alias) {
        this.alias = alias;
        return this;
    }

    public String getUserDBName() {
        return userDBName;
    }

    public void setUserDBName(String userDBName) {
        this.userDBName = userDBName;
    }

    public void updateUserInfo(String alias, String albumPath, String signature) {
        this.alias = alias;
        this.albumPath = albumPath;
        this.signature = signature;
    }

    public void logD(String TAG){
        Log.d(TAG, "用户名: "+user+", 密码："+password+
                ", 个性签名: "+signature+", 头像路径："+albumPath+
                ", 用户别名: "+alias+", 用户数据库名: "+userDBName);
    }

    public void clear(){
        if (user != null) user = null;
        if (password != null) password = null;
        if (signature != null) signature = null;
        if (albumPath != null) albumPath = null;
        if (alias != null) alias = null;
        if (userDBName != null) userDBName = null;
    }
}
