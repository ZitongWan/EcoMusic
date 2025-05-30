package com.xwrl.mvvm.demo.model.helper;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.xwrl.mvvm.demo.bean.MusicBean;
import com.xwrl.mvvm.demo.util.LogUtil;
import com.xwrl.mvvm.demo.util.StringUtil;

import java.io.File;

public class MusicHelper {
    private static final String TAG = "MusicHelper";

    public static String getMusicName(String title, String artist){

        return deleteRedundantString(title) + " - " + deleteRedundantString(artist);
    }
    public static String getMusicFileName(String path){

        return path == null || TextUtils.isEmpty(path) || !path.contains("/") ? path :
                                                        path.substring(path.lastIndexOf("/"));
    }
    public static MediaMetadataCompat getMusicAbsPath(ContentResolver contentResolver, String path){
        Cursor cursor = getSystemLocalMusic(contentResolver);
        MediaMetadataCompat metadata = getMusicAbsPath(cursor, path);

        if (cursor != null) cursor.close();

        return metadata;
    }
    private static String deleteRedundantString(String s){
        if (s != null){

            s = !s.contains("[") ? s :
                    s.replace(s.substring(s.indexOf("[")),"");

        }
        return s;
    }

    public static String getMediaId(String title,String artist,String album){
        String mediaId = getSpanishStr(title+"_"+artist+"_"+album);
        return mediaId.replaceAll("&", "/");
    }

    /**
     * 将西班牙语重音乱码替换为UTF-8
     * */
    private static String getSpanishStr(String str) {
        if (str == null || TextUtils.isEmpty(str)) return str;
        return str.replaceAll("¨¢","á")
                .replaceAll("¨¦","é")
                .replaceAll("¨Ş","í")
                .replaceAll("¨ª","í")
                .replaceAll("¨®","ó")
                .replaceAll("¨²","ú")
                .replaceAll("&","/");
    }

    /**
     * 获得 历史下载歌单 的 时间戳
     * @param date 一串 时间数字 字符串，单位：毫秒*/
    public static String getTimeDifference(String date){
        if (date == null || TextUtils.isEmpty(date)) { return "手冲咖啡"; }
        //判断是否纯数字
        if (!date.matches("^[0-9]+(.[0-9]+)?$")) {
            Log.w(TAG, "MusicHelper: getTimeDifference: "+date);
            return date;
        }

        String time_tips;
        long time_difference = System.currentTimeMillis() - Long.parseLong(date);
        if(time_difference < 0) time_difference = -time_difference;

        if(time_difference <= 1000) time_tips = "1秒前";
        else if(time_difference < 60000 ) {
            time_difference = time_difference / 1000;
            time_tips = time_difference+"秒前";
        }
        else if(time_difference < 3600000) {
            time_difference = time_difference / 60000;
            time_tips = time_difference+"分钟前";
        }
        else if(time_difference < 86400000) {
            time_difference = time_difference / 3600000;
            time_tips = time_difference+"小时前";
        }
        else if(time_difference < 2592000000L) {
            time_difference = time_difference / 86400000;
            time_tips = time_difference+"天前";
        }
        else if(time_difference < 31104000000L) {
            time_difference = time_difference / 2592000000L;
            time_tips = time_difference+"月前";
        }
        else if(time_difference < 3110400000000L) {
            time_difference = time_difference / 31104000000L;
            time_tips = time_difference+"年前";
        } else time_tips = "¡Jódete el culo!";

        return time_tips;
    }

    public static String getMetaInfo(String s, String absPath, boolean isNeedTitle){
        return s == null || TextUtils.isEmpty(s) ?
                MusicHelper.getMusicFileTitleOrArtist(absPath, isNeedTitle) : s;
    }
    /**
     * @return 返回歌曲文件名中的歌手 或 歌名
     * */
    private static String getMusicFileTitleOrArtist(String str, boolean isNeedTitle) {
        if (str == null || TextUtils.isEmpty(str) || !str.contains(".mp3")) return str;

        String defStr = str.replace(".mp3","");
        //当文件命名没有歌名和歌手隔开符“ - ”
        if (!str.contains(" - ")) { return defStr; }

        String title, artist;

        try {

            title = str.substring(str.lastIndexOf("/") + 1, str.indexOf("-") - 1);
            artist = str.substring(str.indexOf("-") + 2, str.indexOf("."));

        }catch (Exception e){
            e.printStackTrace();
            return defStr;
        }

        return isNeedTitle ? title : artist;
    }

    public static Cursor getSystemLocalMusic(ContentResolver contentResolver){

        Uri uri= MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        return contentResolver.query(uri, null, null, null);
    }

    @SuppressLint({"Range", "InlinedApi"})
    public static MediaMetadataCompat getMusicAbsPath(Cursor cursor, String path){
        if (cursor == null || cursor.isClosed() ||
                path == null || TextUtils.isEmpty(path) || !path.contains(".mp3")) {
            //此处应提交异常,至于是否含有.mp3这个得结合 本地媒体数据库 和 工程中媒体播放器 支持的音乐格式来看
            //Log.d("Test", "getMusicAbsPath: cursor为空"+(cursor == null)+", "+path);
            return null;

        }else cursor.moveToFirst(); //每次扫描前都需要将游标移至最开始

        long duration;
        String title, artist, album, pathNew, albumPath;

        while (cursor.moveToNext()) {
            pathNew = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            if (getMusicFileName(pathNew).equals(getMusicFileName(path))){
                albumPath = pathNew;

                title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                title = title.replaceAll("&","/");
                if (!title.contains("(") && title.contains("cover")){
                    String temp = title.substring(title.indexOf("cover"));
                    title = title.replace(temp,"("+temp+")")
                            .replace("cover","cover:");
                }
                artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                album = album.replaceAll("&","/");
                duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                title = MusicHelper.getMetaInfo(title,path,true);

                artist = MusicHelper.getMetaInfo(artist,path,false);
                artist = artist.replaceAll("&","/");

                if (TextUtils.isEmpty(album)) { album = "Music"; }

                String mediaId = MusicHelper.getMediaId(title, artist, album);

                return MusicHelper.getMediaMetadata(mediaId,title,artist,album, albumPath,pathNew,duration);
            }
        }
        return null;
    }
    public static MediaMetadataCompat.Builder getMediaMetadataBuilder(String mediaId,
                                                       String title,
                                                       String artist,
                                                       String album,
                                                       String albumPath,
                                                       String musicAbsPath,
                                                       Long duration){

        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, getSpanishStr(title))
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, getSpanishStr(album))
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, getSpanishStr(artist))
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, "")
                .putString(
                        MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, albumPath)
                .putString(
                        MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, albumPath)
                .putString(
                        MediaMetadataCompat.METADATA_KEY_MEDIA_URI, musicAbsPath);
    }
    public static MediaMetadataCompat getMediaMetadata(MusicBean bean){
        return getMediaMetadata(bean.getMediaId(),
                                bean.getTitle(),
                                bean.getArtist(),
                                bean.getAlbum(),
                                bean.getAlbumPath(),
                                bean.getPath(),
                                bean.getDuration());
    }

    public static MediaMetadataCompat getMediaMetadata(String mediaId,
                                                       String title,
                                                       String artist,
                                                       String album,
                                                       String albumPath,
                                                       String musicAbsPath,
                                                       Long duration){

        return getMediaMetadataBuilder(mediaId, title, artist,
                                        album, albumPath,
                                        musicAbsPath, duration).build();
    }
    public static MediaMetadataCompat getMediaMetadata(Bundle bundle){

        return bundle == null ? null : getMediaMetadata(
                bundle.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,""), //mediaId
                bundle.getString(MediaMetadataCompat.METADATA_KEY_TITLE,""),    //歌名
                bundle.getString(MediaMetadataCompat.METADATA_KEY_ARTIST,""),   //歌手
                bundle.getString(MediaMetadataCompat.METADATA_KEY_ALBUM,""),    //专辑名
                bundle.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,""),    //专辑图片地址
                bundle.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI,""),    //音乐文件地址
                bundle.getLong(MediaMetadataCompat.METADATA_KEY_DURATION,0)     //音乐时长
        );
    }

    public static MediaMetadataCompat.Builder getMediaMetadataBuilder(Bundle bundle){

        return bundle == null ? null : getMediaMetadataBuilder(
                bundle.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,""), //mediaId
                bundle.getString(MediaMetadataCompat.METADATA_KEY_TITLE,""),    //歌名
                bundle.getString(MediaMetadataCompat.METADATA_KEY_ARTIST,""),   //歌手
                bundle.getString(MediaMetadataCompat.METADATA_KEY_ALBUM,""),    //专辑名
                bundle.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,""),    //专辑图片地址
                bundle.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI,""),    //音乐文件地址
                bundle.getLong(MediaMetadataCompat.METADATA_KEY_DURATION,0)     //音乐时长
        );
    }

    public static MusicBean getMusicBean(MediaMetadataCompat metadata){
        return new MusicBean("1",
                metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE),
                metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST),
                metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM),
                metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI),
                metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI),
                metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
    }

    public static Bundle getMusicBundle(MediaMetadataCompat metadata){
        Bundle bundle = new Bundle();

        bundle.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)); //mediaId
        bundle.putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));    //歌名
        bundle.putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));   //歌手
        bundle.putString(MediaMetadataCompat.METADATA_KEY_ALBUM,
                metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM));    //专辑名
        bundle.putString(MediaMetadataCompat.METADATA_KEY_GENRE,
                metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE));    //流派
        bundle.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI));    //专辑图片地址
        bundle.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,
                metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI)); //头像地址
        bundle.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI));    //歌曲文件地址
        bundle.putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));   //时长 long类型

        return bundle;
    }

    /**
     * TODO：音乐播放按钮
     * */
    public static void playbackButton(MediaControllerCompat mediaController, Context application){
        String TAG = "MusicHelper#playbackButton";
        if (mediaController == null){
            LogUtil.e(TAG, "mediaController == null");
            return;
        }
        if (application == null){
            LogUtil.e(TAG, "application == null");
            return;
        }

        // 因为这是一个播放/暂停按钮，所以需要测试当前状态，并相应地选择动作
        int pbState = mediaController.getPlaybackState().getState();
        Log.d(TAG, "initView: 点击了播放/暂停按钮, 播放状态代码: "+pbState);
        if (pbState == PlaybackStateCompat.STATE_PLAYING) {
            mediaController.getTransportControls().pause();
            //this.playbackDrawable = R.drawable.iv_main_pause;

        } else if (pbState == PlaybackStateCompat.STATE_PAUSED) {
            mediaController.getTransportControls().play();
            //this.playbackDrawable = R.drawable.iv_main_play;

        } else {
            //Toast.makeText(this, "进入APP首次播放", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "playbackButton: 首次播放");
            MediaMetadataCompat metadata = mediaController.getMetadata();
            String path = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI);
            if (path == null || TextUtils.isEmpty(path)) {
                //支持在线歌曲播放的话就推送热度高的歌曲，本地就引导
                Toast.makeText(application,"请进入本地列表播放歌曲哦",Toast.LENGTH_SHORT).show();
                return;
            }
            clickMusicItemPlay(mediaController, metadata, application);
        }
    }
    public static void clickMusicItemPlay(@NonNull MediaControllerCompat mediaController,
                                          @NonNull MediaMetadataCompat metadata,
                                          @NonNull Context application){
        String TAG = "MusicHelper#clickMusicItemPlay";
        String path = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI);
        if (StringUtil.isOnlyDigit(path)) { //在线音乐
            //playNetMusic(mediaController, metadata, application);
            Toast.makeText(application,"Saorry~~~在线音乐暂时不能播放！",Toast.LENGTH_SHORT).show();
        }else { //本地音乐
            //Log.d(TAG, "playbackButton: 本地音乐");
            //mediaController.getTransportControls().playFromUri(Uri.parse(path),null);
            String mediaId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
            mediaController.getTransportControls().playFromMediaId(mediaId, null);
        }
    }

    public static boolean isSameSong(@NonNull MediaControllerCompat mediaController,
                                     @NonNull String currentMediaId,
                                     @NonNull Context application){
        String mediaId = mediaController.getMetadata()
                .getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
        if (currentMediaId.equals(mediaId)){//同一首歌 ，暂停
            MusicHelper.playbackButton(mediaController, application);
            return true;
        }
        return false;
    }

    public static boolean FileExists(String targetFileAbsPath){
        try {
            File f = new File(targetFileAbsPath);
            return f.exists() && f.canRead();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
