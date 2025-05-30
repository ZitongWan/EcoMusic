package com.xwrl.mvvm.demo.util;

import android.os.Environment;
import android.util.Log;

import java.io.File;

public class HttpUtil {

    /**
     * 1.两次点击按钮之间的点击间隔不能少于1000毫秒
     * 2.数值必须大于{@link com.xwrl.mvvm.demo.service.MusicService}中的TimerTask(暂停音乐)执行时间
     * */
    private static final int MIN_CLICK_DELAY_TIME = 500;
    private static long lastClickTime = 1;
    private static final String TAG = "HttpUtil";


    public static long getLastClickTime() {
        return lastClickTime;
    }

    public static String getLocalPath(String fileName){
        fileName = fileName.replaceAll("/","&");
        String absPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)+"/";
        return absPath+fileName;
    }

    public static boolean FileExists(String targetFileAbsPath){
        try {
            File f = new File(targetFileAbsPath);
            return f.exists() && f.canRead();
        } catch (Exception e) {
            return false;
        }
    }
    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (!FileExists(filePath)) return false;

        if (file.isFile() && file.exists()) {
            return file.delete();
        }
        return false;
    }
    public static String FixFileName(String filePath, String newFileName) {
        File f = new File(filePath);
        if (!f.exists())  return null; // 判断原文件是否存在（防止文件名冲突）

        newFileName = newFileName.trim();
        if ("".equals(newFileName)) return null; // 文件名不能为空

        String newFilePath;
        if (f.isDirectory()) { // 判断是否为文件夹
            newFilePath = filePath.substring(0, filePath.lastIndexOf("/")) + "/" + newFileName;
        } else {
            newFilePath = filePath.substring(0, filePath.lastIndexOf("/")) + "/" + newFileName
                    + filePath.substring(filePath.lastIndexOf("."));
        }
        File nf = new File(newFilePath);
        try {
            Log.d(TAG, "FixFileName: "+f.renameTo(nf));// 修改文件名
        } catch (Exception err) {
            err.printStackTrace();
            return null;
        }
        return newFilePath;
    }

    public static boolean isFastClick() {
        boolean flag = true;
        long curClickTime = System.currentTimeMillis();
        if ((curClickTime - lastClickTime) >= MIN_CLICK_DELAY_TIME) { flag = false; }
        //LogUtil.d(TAG, "isFastClick: "+((curClickTime - lastClickTime)));
        lastClickTime = curClickTime;
        return flag;
    }

    public static String getLocalPathPictures(String fileName){
        fileName = fileName.replaceAll("/","&");
        String absPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/ros/caches/lrc/";
        return absPath+fileName;
    }

    public static void createPictureMyDIR(){
        try {
            String dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/ros/";

            File f = new File(dirPath);
            if(!f.exists())
                Log.d(TAG, "创建本APP文件夹 "+ f.mkdirs());

            File caches = new File(dirPath+"/caches/");
            if(!caches.exists())
                Log.d(TAG, "创建缓存文件夹 "+ caches.mkdirs());

            File lrc = new File(dirPath+"/caches/lrc/");
            if(!lrc.exists())
                Log.d(TAG, "创建歌词文件夹 "+ lrc.mkdirs());

            File icon = new File(dirPath+"/caches/icon/");
            if(!icon.exists())
                Log.d(TAG, "创建头像文件夹 "+ icon.mkdirs());

            File album = new File(dirPath+"/caches/album/");
            if(!album.exists())
                Log.d(TAG, "创建封面缓存文件夹 "+ album.mkdirs());

        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG,"未获取读写权限");
        }
    }

}
