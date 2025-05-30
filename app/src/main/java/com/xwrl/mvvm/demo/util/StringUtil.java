package com.xwrl.mvvm.demo.util;

import android.app.Application;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;

import com.xwrl.mvvm.demo.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class StringUtil {

    // 纯数字
    private static String DIGIT_REGEX = "[0-9]+";
    // 含有数字
    private static String CONTAIN_DIGIT_REGEX = ".*[0-9].*";
    // 纯字母
    private static String LETTER_REGEX = "[a-zA-Z]+";
    // 包含字母
    private static String CONTAIN_LETTER_REGEX = ".*[a-zA-z].*";
    // 纯中文
    private static String CHINESE_REGEX = "[\u4e00-\u9fa5]";
    // 仅仅包含字母和数字
    private static String LETTER_DIGIT_REGEX = "^[a-z0-9A-Z]+$";
    private static String CHINESE_LETTER_REGEX = "([\u4e00-\u9fa5]+|[a-zA-Z]+)";
    private static String CHINESE_LETTER_DIGIT_REGEX = "^[a-z0-9A-Z\u4e00-\u9fa5]+$";


    public static Spanned SongSingerName(String title, String artist){
        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(artist))
            return Html.fromHtml("<font color = \"#EEEEEE\">快去听听音乐吧</font>",
                                                Html.FROM_HTML_OPTION_USE_CSS_COLORS);
        if (TextUtils.isEmpty(artist) || artist.equals("<unknown>")) artist = "Unknown";
        String highColor = "#EEEEEE", lowColor = "#CDCDCD";

        return getSpanned(title,highColor,artist,lowColor,true);
    }

    private static Spanned getSpanned(String sHigher, String colorHigher,
                                      String sLower, String colorLower,
                                      boolean isMusicTips){

        if (isMusicTips) sLower = "<bold> - </bold>"+sLower;

        String SongInformation = "<font color = "+colorHigher+"><bold>"+sHigher+"</bold></font>"+
                "<font color = "+colorLower+"><small>"+sLower+"</small></font>";

        return Html.fromHtml(SongInformation,Html.FROM_HTML_OPTION_USE_CSS_COLORS);
    }

    public static Spanned PlayTimerTips(int hourOfDay, int minute, boolean isCheck, String highColor){
        String hour = hourOfDay < 10 ? "0"+hourOfDay : String.valueOf(hourOfDay);
        String min = minute < 10 ? "0"+minute : String.valueOf(minute);
        String lowColor = highColor.replace("#","#99");
        String frequency = isCheck ? "每天" : "仅一次";
        //使用空格符&nbsp;在html语言中进行空格占位
        String sLower = "&nbsp;&nbsp;" + (hourOfDay >= 12 ? "下午" : "上午") + "&nbsp;|&nbsp;" + frequency;

        String playTime = "<font color = "+highColor+"><bold><big>"+hour+":"+min+"</big></bold></font>"+
                "<font color = "+lowColor+"><small><small>"+sLower+"</small></small></font>";

        return Html.fromHtml(playTime,Html.FROM_HTML_OPTION_USE_CSS_COLORS);
    }

    public static String SheetTips(int count){
        return "已有歌单("+count+"个)";
    }

    public static String SheetCountTips(int count){
        return "("+count+")";
    }

    public static String getDefFileName(String title, String artist){

        if (title.contains("-") || artist.contains("(") || artist.contains("[")){
            if(title.contains("-")) title = title.substring(0,title.indexOf("-"));
            if(artist.contains("(")) artist = artist.substring(0,artist.indexOf("("));
            if(artist.contains("[")) artist = artist.substring(0,artist.indexOf("["));
        }
        return title +" - "+artist +".mp3";
    }

    public static String getPermissionTips(Application application, String content){

        return application == null ? content :
                application.getString(R.string.label_Dialog_get_tips)+"\n"+content;
    }

    /**
     * 判断 一句 字符串里 有 多少个 特定字符串
     * @param s 查询字符串
     * @param tag 特定字符串
     * @return int 数量
     * */
    public static int getAtStrNumber(String s, String tag){
        if (s == null || tag == null || TextUtils.isEmpty(s) ||
                            TextUtils.isEmpty(tag) || !s.contains(tag)) return 0;

        return s.length() - s.replaceAll(tag,"").length();
    }

    /**
     * 是否包括中文
     * @param content 字符串参数
     * @return boolean
     */
    public static boolean isHasChinese(String content) {
        return content.matches(CHINESE_REGEX);
    }

    /**
     * 是否仅为字母和数字
     * @param content 字符串参数
     * @return boolean
     */
    public static boolean isOnlyDigitAndLetter(String content) {
        return content != null && content.matches(LETTER_DIGIT_REGEX);
    }
    /**
     * 是否仅为数字
     * @param content 字符串参数
     * @return boolean
     */
    public static boolean isOnlyDigit(String content) {
        return content != null && content.matches(DIGIT_REGEX);
    }

    /**
     * @return 返回一个固定格式的时间 字符串
     * */
    public static String getDate(long date){

        return new SimpleDateFormat("yyyy-MM-dd hh:mm.ss", Locale.CHINA)
                .format(new Date(date));
    }
    public static String getDate(String date){
        if (date == null || TextUtils.isEmpty(date) || !isOnlyDigit(date)) return "";
        return getDate(Long.parseLong(date));
    }

    public static boolean isLegalLength(String text,boolean isName){
        if (text == null) return true;

        int legalLength,trueLength;
        Pattern p = Pattern.compile("[\u4e00-\u9fa5]");//中文与中文字符
        Pattern r = Pattern.compile("[\u3040-\u309F\u30A0-\u30FF]");//日文字符

        legalLength = isName ? 26 : 31;//定义合法长度

        if (!p.matcher(text).find() && !r.matcher(text).find()) {//不包含中文与日文
            trueLength = text.length();
        }else {
            int count = 0;
            //可能不全是字母，故需要对每个字符进行判断
            for (int i = 0;i < text.length(); i++){
                String x = text.substring(i);
                if(p.matcher(x).find() || r.matcher(text).find())  count++;
            }
            trueLength = count *2 +(text.length() - count);
            //Log.d("滚动文字", "汉字个数: "+count+"，理论长度："+trueLength);
        }
        return trueLength > legalLength;
    }
}
