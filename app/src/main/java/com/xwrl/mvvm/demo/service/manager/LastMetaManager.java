package com.xwrl.mvvm.demo.service.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.xwrl.mvvm.demo.BaseActivity;
import com.xwrl.mvvm.demo.bean.MusicBean;
import com.xwrl.mvvm.demo.bean.UserBean;
import com.xwrl.mvvm.demo.model.helper.MusicHelper;
import com.xwrl.mvvm.demo.model.helper.SQLiteChangeHelper;
import com.xwrl.mvvm.demo.util.StringUtil;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class LastMetaManager {
    private static final String TAG = "LastMetaManager";
    private final SoftReference<SharedPreferences> settings;

    public LastMetaManager(Context application){
        settings = new SoftReference<>(
                application.getSharedPreferences("UserLastMusicPlay",0));
        if (settings.get() == null) Log.e(TAG, "LastMetaManager: SharedPreferences为空");
    }

    public SharedPreferences get(){ return settings == null ? null : settings.get();}

    private boolean checkSetting(){
        boolean isNull = settings == null || settings.get() == null;
        if (isNull) Log.e(TAG, "checkSetting: 为空！");
        return isNull;
    }

    public String getLastSheetName(){
        return get().getString("PlayingSheetName", BaseActivity.ACTIVITY_DEFAULT_SHEET_NAME);
    }
    public void saveSheetName(String sheetName){
        if (sheetName == null || TextUtils.isEmpty(sheetName)) return;
        Log.e(TAG, "保存SheetName: "+sheetName);
        SharedPreferences.Editor editor = get().edit();
        editor.putString("PlayingSheetName",sheetName);
        editor.apply();
    }
    /**
     * 保存 游客和用户 的 大部分信息
     * */
    public UserBean getStrangerInfo(){
        UserBean userBean = new UserBean("", "",
                getStrangerIconPath(), getStrangerLabel());
        return userBean.setAlias(getStrangerName());
    }
    public String getStrangerIconPath() { return get().getString("StrangerIconPath","default"); }
    public String getStrangerName() { return get().getString("StrangerName","游客账户"); }
    public String getStrangerLabel() { return get().getString("StrangerLabel","己自于叶，凝渊之子。"); }
    public void saveStrangerInformation(String strangerName, String strangerLabel, String strangerIconPath) {

        SharedPreferences.Editor editor = get().edit();
        Log.d(TAG, "保存游客信息: name = "+strangerName+",label = "+strangerLabel+",path = "+strangerIconPath);

        if (strangerName != null && !TextUtils.isEmpty(strangerName))
            editor.putString("StrangerName",strangerName);
        if (strangerLabel != null && !TextUtils.isEmpty(strangerLabel))
            editor.putString("StrangerLabel",strangerLabel);
        if (strangerIconPath != null && !TextUtils.isEmpty(strangerIconPath))
            editor.putString("StrangerIconPath",strangerIconPath);

        editor.apply();//不关心返回值用这个
    }

    public UserBean getUserInfo(){
        return new UserBean(getUserName(), "",
                getUserIconPath(), getUserLabel());
    }
    public String getUserDBName() { return get().getString("userDBName", SQLiteChangeHelper.UserDBName);  }
    public void saveUserDBName(String userDBName){
        SharedPreferences.Editor editor = get().edit();
        Log.d(TAG, "保存用户数据库信息: userDBName = "+userDBName);
        if (userDBName != null && !TextUtils.isEmpty(userDBName)){
            editor.putString("userDBName",userDBName);
        }
        editor.apply();//不关心返回值用这个
    }
    public String getUserIconPath() { return get().getString("IconPath","default");  }
    public String getUserName() { return get().getString("UserName","用户名"); }
    public String getUserLabel() { return get().getString("UserLabel","这是一条个性签名。"); }
    public String getUserAlias() { return get().getString("UserAlias",""); }

    public void saveUserInformation(String userName, String userLabel,
                                    String userIconPath, String userAlias) {

        SharedPreferences.Editor editor = get().edit();
        Log.d(TAG, "保存用户信息: name = "+userName+", label = "+userLabel+
                ", path = "+userIconPath+", alias = "+userAlias);

        if (userName != null && !TextUtils.isEmpty(userName))
            editor.putString("UserName",userName);
        if (userLabel != null && !TextUtils.isEmpty(userLabel))
            editor.putString("UserLabel",userLabel);
        if (userIconPath != null && !TextUtils.isEmpty(userIconPath))
            editor.putString("IconPath",userIconPath);
        if (userAlias != null && !TextUtils.isEmpty(userAlias))
            editor.putString("UserAlias",userAlias);

        editor.apply();//不关心返回值用这个
    }

    public void saveMusicPosition(int position){
        if(!checkSetting()){
            SharedPreferences.Editor editor = get().edit();
            editor.putInt("MusicPosition",position);
            editor.apply();
        }
    }
    public int getLastMusicPosition(){
        return get().getInt("MusicPosition",0);
    }

    public int getLastPlaybackMode(int defaultMode){
        return get().getInt("MusicPlaybackMode", defaultMode);
    }

    public String getLastAlbumPath(){
        return get().getString("MusicAlbumPath","");
    }
    public boolean getNotificationStyle(){
        return get().getBoolean("NotificationStyle",false);
    }
    public void saveNotificationStyle(boolean isCustomNotificationStyle){
        SharedPreferences.Editor editor = get().edit();
        editor.putBoolean("NotificationStyle",isCustomNotificationStyle);
        editor.apply();
    }

    public MediaMetadataCompat.Builder getLastMusicPlay(){
        String title,artist,album,path,albumPath;
        SharedPreferences setting = get();

        title = setting.getString("MusicTitle","");
        artist = setting.getString("MusicArtist","")
                .replaceAll("&","/");
        album = setting.getString("MusicAlbum","");
        path = setting.getString("MusicPath","");
        albumPath = setting.getString("MusicAlbumPath","");
        long duration = setting.getLong("MusicDuration",0);
        //Log.d(TAG, "GetLastMusicPlay: "+title);

        //mCurrentDuration = settings.getLong("MusicDuration",0);
        /*mLastAlbumPath = settings.getString("LastAlbumPath","");
        mNextAlbumPath = settings.getString("NextAlbumPath","");
        mMusicQueueIndex = settings.getInt("MusicQueue",0);
        mLastQueueIndex = settings.getInt("MusicLastQueue",-1);
        mNextQueueIndex = settings.getInt("MusicNextQueue",-1);
        mCurrentSource = settings.getString("MusicSourceAlias","本地播放");
        mUserName = settings.getString("UserName","用户名");     //用户个性名称
        mUserLabel = settings.getString("UserLabel","Supreme"); //用户个性便笺
        mUserIconPath = settings.getString("IconPath","none");  //用户头像Url
        mCurrentBlurs = settings.getFloat("UserSetBlurs",13f);  //用户设置的模糊值
        mCurrentCycleTime = settings.getLong("UserCycleTime",36000);  //用户设置的唱片周期
        mCurrentVoiceMb = settings.getLong("UserVoiceMb",1000);  //用户设置的人声增强（mb）1000mb = 10db
        mCurrentFileMusicTitle = settings.getString("FileMusicTitle","");//默认为顺序播放
        mCurrentFileMusicArtist = settings.getString("FileMusicArtist","");//默认为顺序播放
        isNotificationMediaStyle = settings.getBoolean("NotificationStyle",true);
        if(!UsersGuidePermissionsUtil.checkCanDrawOverlays(this.getApplicationContext()))
            isLrcViewShow = settings.getBoolean("LrcShow",false);*/


        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, MusicHelper.getMediaId(title,artist,album))
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, "")
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, albumPath)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, albumPath)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, path);

    }

    public void SaveLastMusicPlay(@NonNull MediaMetadataCompat metadata, int position){

        SharedPreferences.Editor editor = get().edit();

        editor.putString("MusicTitle",metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
        editor.putString("MusicArtist",metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
        editor.putString("MusicAlbum", metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM));
        editor.putString("MusicPath",metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI));
        editor.putString("MusicAlbumPath", metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI));
        editor.putInt("MusicPosition",position);
        editor.putLong("MusicDuration",metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
        /*editor.putString("LastAlbumPath", mLastAlbumPath);
        editor.putString("NextAlbumPath", mNextAlbumPath);
        editor.putInt("MusicQueue",mMusicQueueIndex);
        editor.putInt("MusicLastQueue",mLastQueueIndex);
        editor.putInt("MusicNextQueue",mNextQueueIndex);
        editor.putString("MusicSourceAlias",mCurrentSource);
        editor.putString("PlayMode",PLAYER_PLAY_MODE);
        editor.putString("UserName",mUserName);
        editor.putString("UserLabel",mUserLabel);
        editor.putString("IconPath",mUserIconPath);
        editor.putString("FileMusicTitle",mCurrentFileMusicTitle);
        editor.putString("FileMusicArtist",mCurrentFileMusicArtist);
        editor.putBoolean("LrcShow",isLrcViewShow);*/
        //Log.d(TAG, "SaveLastMusicPlay: mCurrentPosition= "+mCurrentPosition+"mMusicQueueIndex= "+mMusicQueueIndex);
        Log.d(TAG, "SaveLastMusicPlay: 是否保存成功 "+editor.commit());
    }

    public List<MediaBrowserCompat.MediaItem> getMediaItems(LinkedHashMap<String, MediaMetadataCompat> musicMaps) {
        List<MediaBrowserCompat.MediaItem> result = new ArrayList<>();
        for (MediaMetadataCompat metadata : musicMaps.values()) {
            MediaBrowserCompat.MediaItem item = new MediaBrowserCompat.MediaItem(
                    metadata.getDescription(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
            result.add(item);
            /*Log.d(TAG, "getMediaItems: "+metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)+
                    " 键值 "+metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID));*/
        }
        Log.d(TAG, "getMediaItems: "+result.size());
        return result;
    }

    public List<MediaSessionCompat.QueueItem>
    MetadataToQueue(LinkedHashMap<String, MediaMetadataCompat> musicList){
        List<MediaSessionCompat.QueueItem> playList = new ArrayList<>();
        for (MediaMetadataCompat m : musicList.values()){
            MediaDescriptionCompat description = m.getDescription();
            playList.add(new MediaSessionCompat.QueueItem(description,description.hashCode()));
        }

        return playList;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q) //为了编译通过，但还是要加版本判断
    public LinkedHashMap<String, MediaMetadataCompat> QueueToMetadata(
            List<MediaSessionCompat.QueueItem> playlist, Bundle bundle){
        LinkedHashMap<String, MediaMetadataCompat> musicLists = new LinkedHashMap<>();
        for (MediaSessionCompat.QueueItem item : playlist){
            String title = item.getDescription().getTitle() != null ?
                    item.getDescription().getTitle().toString() : null;
            String artist = item.getDescription().getSubtitle() != null ?
                    item.getDescription().getSubtitle().toString() : null;
            String album = item.getDescription().getDescription() != null ?
                    item.getDescription().getDescription().toString() : null;
            String albumPath = item.getDescription().getIconUri() != null ?
                    item.getDescription().getIconUri().toString() : null;
            String path = item.getDescription().getMediaUri() != null ?
                    item.getDescription().getMediaUri().toString() : null;
            String duration = "0";
            //因为文件命名的问题，所以“&”或“/”需要统一
            String mediaId = MusicHelper.getMediaId(title,artist,album);
            //Log.d(TAG, "QueueToMetadata: MediaId："+mediaId);
            if (!StringUtil.isOnlyDigit(path) && !MusicHelper.FileExists(path)) {
                Log.w(TAG, "QueueToMetadata: 没有歌曲文件！"+path);
                continue;//没有文件就下一曲
            }
            if (!StringUtil.isOnlyDigit(path)) { // 本地音乐
                MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
                try {
                    metadataRetriever.setDataSource(path);
                    duration = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    if (duration == null) duration = "0"; // 防止解析失败
                } catch (Exception e) {
                    Log.e(TAG, "Error retrieving metadata from: " + path, e);
                    continue; // 发生异常，跳过当前项
                } finally {
                    metadataRetriever.release(); // 确保资源被释放
                }
            }

            MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, Long.parseLong(duration))
                    .putString(MediaMetadataCompat.METADATA_KEY_GENRE, "")
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, albumPath)
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, albumPath)
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, path)
                    .build();

            musicLists.put(mediaId, metadata);
        }
        return musicLists;
    }

    public MusicBean getMusicBean(MediaMetadataCompat metadata){
        if (metadata == null) return null;
        return new WeakReference<>(new MusicBean("0",
                metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE),
                metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST),
                metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM),
                metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI),
                metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI),
                metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)))
                .get();
    }

    public void onDestroy(){
        if (settings != null) { settings.clear(); }
    }
}
