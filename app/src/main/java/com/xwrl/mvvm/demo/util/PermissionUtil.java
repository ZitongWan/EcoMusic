package com.xwrl.mvvm.demo.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

public class PermissionUtil {
    public static final int REQUEST_PERMISSION_CODE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    public static void getStorage(Activity activity) {
        /*动态获取存储权限的函数*/
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){ //判断Android版本是否大于6.0 || 在API(26)以后规定必须要动态获取权限
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)!=
                    PackageManager.PERMISSION_GRANTED) {
                System.out.println(666);
                ActivityCompat.requestPermissions(activity,PERMISSIONS_STORAGE,REQUEST_PERMISSION_CODE);
            }
        }
    }

    public static boolean IsPermissionNotObtained(Activity activity){
        return ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED; //判断是否已获取权限;
    }
}
