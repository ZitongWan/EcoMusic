package com.xwrl.mvvm.demo.util;


import android.util.Log;

public class LogUtil {

    public static final int LOG_MODE_I = 1;
    public static final int LOG_MODE_D = 2;
    public static final int LOG_MODE_WARNING = 3;
    public static final int LOG_MODE_ERROR = 4;
    public static final int LOG_MODE_V = 5;

    public static void i(String TAG, String msg) { log(TAG,msg,LOG_MODE_I); }
    public static void d(String TAG, String msg) { log(TAG,msg,LOG_MODE_D); }
    public static void w(String TAG, String msg) { log(TAG,msg,LOG_MODE_WARNING); }
    public static void e(String TAG, String msg) { log(TAG,msg,LOG_MODE_ERROR); }
    public static void v(String TAG, String msg) { log(TAG,msg,LOG_MODE_V); }

    public static void log(String TAG, String msg, int LOG_MOD){
        if (TAG == null || msg == null) return;

        int LOG_MAX_LENGTH = 2000;
        int strLength = msg.length();
        int start = 0;
        int end = LOG_MAX_LENGTH;
        for (int i = 0; i < 1000; i++) {
            //Log.d(TAG, "\ne: "+strLength+", "+end);
            if (strLength > end) {
                Log(TAG + i, msg.substring(start, end), LOG_MOD);
                start = end;
                end = end + LOG_MAX_LENGTH;
            } else {
                Log(TAG, msg.substring(start, strLength), LOG_MOD);
                break;
            }
        }
    }

    private static void Log(String TAG, String msg, int mode){
        if (mode == LOG_MODE_I){ Log.i(TAG, msg);
        }else if (mode == LOG_MODE_D){ Log.d(TAG, msg);
        }else if (mode == LOG_MODE_WARNING){ Log.w(TAG, msg);
        }else if (mode == LOG_MODE_ERROR){ Log.e(TAG, msg);
        }else if (mode == LOG_MODE_V){ Log.v(TAG, msg); }
    }

    /*public static boolean writeText(){

    }*/
}
