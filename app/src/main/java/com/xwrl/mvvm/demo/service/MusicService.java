package com.xwrl.mvvm.demo.service;

import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;

import com.xwrl.mvvm.demo.bean.MusicBean;
import com.xwrl.mvvm.demo.model.AllSongSheetModel;
import com.xwrl.mvvm.demo.model.SongSheet;
import com.xwrl.mvvm.demo.model.helper.MusicHelper;
import com.xwrl.mvvm.demo.model.helper.SongListHelper;
import com.xwrl.mvvm.demo.service.manager.LastMetaManager;
import com.xwrl.mvvm.demo.service.manager.MediaPlayerManager;
import com.xwrl.mvvm.demo.service.manager.MyAudioManager;
import com.xwrl.mvvm.demo.util.LogUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MusicService extends BaseMusicService{

    private static final String TAG = "MusicService";
    private static final String MY_MEDIA_ROOT_ID = "media_root_id";
    private static final String MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id";

    private MediaSessionCompat mMediaSession;
    private int mQueueIndex = -1;
    private MediaPlayerManager mMediaPlayerManager;
    private boolean IS_AUDIO_FOCUS_LOSS_TRANSIENT;
    private LastMetaManager mLastMetaManager;
    private Timer mTimer;
    private final List<MediaSessionCompat.QueueItem> mPlaylist = new ArrayList<>();

    private LinkedHashMap<String, MediaMetadataCompat> mMusicList;
    private SongSheet mSongSheet;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
        init();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "onTaskRemoved: ");
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseMedia();
        Log.d(TAG, "onDestroy: ");
    }

    //************************************本Service处理与客户端的链接**********************************/
    //参考：https://developer.android.google.cn/guide/topics/media-apps/audio-app/building-a-mediabrowserservice

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {

        if (allowBrowsing(clientPackageName, clientUid)) { //允许浏览
            // Returns a root ID that clients can use with onLoadChildren() to retrieve
            // the content hierarchy.
            return new BrowserRoot(MY_MEDIA_ROOT_ID, null);
        } else {
            // Clients can connect, but this BrowserRoot is an empty hierachy
            // so onLoadChildren returns nothing. This disables the ability to browse for content.
            return new BrowserRoot(MY_EMPTY_MEDIA_ROOT_ID, null);
        }
    }
    private boolean allowBrowsing(String clientPackageName, int clientUid) {
        return true;
    }

    @Override
    public void onLoadChildren(@NonNull String parentMediaId,
                               @NonNull MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> result) {
        //将信息从当前线程中移除，允许后续调用sendResult方法
        result.detach();
        //获取存在MediaController中的额外信息bundle
        Bundle bundle = mMediaSession.getController().getExtras();
        //sheetName：根据MediaBrowser传过来的parentMediaId获得需要订阅音乐列表的的歌单名
        //curSheetName：根据额外信息bundle获取保存的parentMediaId
        String sheetName = parentMediaId.substring(parentMediaId.indexOf("id_")+3),
                curSheetName = bundle == null ? null : bundle.getString(DYQL_CUSTOM_CURRENT_SHEETNAME);
        //浏览不被允许
        if (parentMediaId.contains("SongLrc") ||
                TextUtils.equals(MY_EMPTY_MEDIA_ROOT_ID, parentMediaId)) {
            List<MediaBrowserCompat.MediaItem> xwrl =
                    mMusicList != null && sheetName.equals(curSheetName) ?
                            mLastMetaManager.getMediaItems(mMusicList) : new ArrayList<>();
            result.sendResult(xwrl);
            return;
        }
        //接口回调传回音乐列表数据集合
        mSongSheet.showSheetMusic(resultMusics -> {

            if (curSheetName == null || sheetName.equals(curSheetName)){
                if (mMusicList == null) { mMusicList = new LinkedHashMap<>(); }
                else if(mMusicList.size() > 0) mMusicList.clear();
                mMusicList.putAll(resultMusics);
            }

            if (mLastMetaManager == null) {
                mLastMetaManager = new LastMetaManager(getApplication());
            }
            //.sendResult() -> Activity[Browser客户端]收到OnChildrenLoaded()回调
            result.sendResult(mLastMetaManager.getMediaItems(resultMusics));

        },getApplication(), sheetName);
        //保存本次的parentMediaId至mMediaSession，为了下次订阅前在MediaBrowser取消本次订阅

        bundle = bundle == null ? new Bundle() : bundle;
        bundle.putString("ParentMediaId",parentMediaId);
        bundle.putString(DYQL_CUSTOM_CURRENT_SHEETNAME,sheetName);
        mMediaSession.setExtras(bundle);

        LogUtil.d(TAG,"onLoadChildren: 传入歌单名 "+sheetName+"， 当前播放歌单名 "+curSheetName +
                ", parentMediaId = "+parentMediaId+", "+parentMediaId.contains(MY_MEDIA_ROOT_ID)+
                ", musicList<> 获得歌曲"+mMusicList.size());
        super.setMediaController(mMediaSession.getController());
    }

    private void init(){
        mSongSheet = new AllSongSheetModel();
        mLastMetaManager = new LastMetaManager(getApplication());
        initMediaSession();
        initLifeBackground();
        GetLastMusicPlay();
    }

    private void initMediaSession() {
        //初始化MediaSession | 媒体会话
        mMediaSession = new MediaSessionCompat(this,getPackageName());
        mMediaSession.setActive(true);
        mMediaSession.setQueue(mPlaylist);

        mMediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        //给MediaSession设置来自MediaController的回调内部类
        mMediaSession.setCallback(new MyMediaSessionCallback());

        // 设置会话的令牌，以便客户端活动可以与其通信。
        setSessionToken(mMediaSession.getSessionToken());

        initMediaPlayerManager();

        //给MediaSession设置初始状态
        mMediaSession.setPlaybackState(
                mMediaPlayerManager.newPlaybackState(PlaybackStateCompat.STATE_NONE,null));

    }

    private void initMediaPlayerManager() {
        if (mMediaPlayerManager != null) { return; }

        mMediaPlayerManager = new MediaPlayerManager(getApplication(),
                                                        mMediaSession,
                                                        new MyNotificationListener(),
                                                        new MyAudioFocusChangeListener());
        mLastMetaManager = new LastMetaManager(getApplication());
        //初始化播放模式
        mMediaPlayerManager.setPlayBackMode(mLastMetaManager
                .getLastPlaybackMode(MediaPlayerManager.getDyqlPlaybackModeOrder()));
        //获得上次播放进度int，默认为0
        mMediaPlayerManager.initCurrentPosition(mLastMetaManager.getLastMusicPosition());
    }

    private void releaseMediaPlayerManager(){
        if (mMediaSession != null) {
            mMediaSession.setPlaybackState(mMediaPlayerManager.newPlaybackState(
                    PlaybackStateCompat.STATE_STOPPED,null));
            mMediaSession.setActive(false);
        }

        if (mMediaPlayerManager != null) {
            mMediaPlayerManager.onDestroy();
            mMediaPlayerManager = null;
        }
        if (mLastMetaManager != null) { mLastMetaManager.onDestroy(); mLastMetaManager = null;}

    }

    private void releaseMedia() {
        mPlaylist.clear();
        if (mMusicList != null) {
            if (mMusicList.size() > 0) { mMusicList.clear(); }
            mMusicList = null;
        }
        releaseMediaPlayerManager();
        if (mMediaSession != null) {
            mMediaSession.release();
            mMediaSession = null;
        }
    }
    //**********************************************Metadata元数据相关方法***************************/
    private List<MediaBrowserCompat.MediaItem> getMediaItems(LinkedHashMap<String, MediaMetadataCompat> musicMaps) {
        List<MediaBrowserCompat.MediaItem> result = new ArrayList<>();
        for (MediaMetadataCompat metadata : musicMaps.values()) {
            result.add(
                    new MediaBrowserCompat.MediaItem(
                            metadata.getDescription(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
            /*Log.d(TAG, "getMediaItems: "+metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)+
                    " 键值 "+metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID));*/
        }
        Log.d(TAG, "getMediaItems: "+result.size());
        return result;
    }

    private class MyMediaSessionCallback extends MediaSessionCompat.Callback{
        private boolean isAddQueueItem(MediaDescriptionCompat description){
            for (MediaSessionCompat.QueueItem queueItem : mPlaylist){
                MediaDescriptionCompat mediaId = queueItem.getDescription();
                if (mediaId == null) continue;
                //Log.d(TAG, "onAddQueueItem: "+queueItem.getDescription()+", 当前："+description);
                if (mediaId.toString().equals(description.toString())) {
                    return false;
                }
            }
            return true;
        }
        private boolean updatePlayList(MediaMetadataCompat metadata){
            return metadata != null && updatePlayList(metadata.getDescription());
        }
        private boolean updatePlayList(MediaDescriptionCompat description){
            if (description == null) return false;

            mPlaylist.clear();
            mPlaylist.add(new MediaSessionCompat.QueueItem(description,description.hashCode()));
            mPlaylist.addAll(mLastMetaManager.MetadataToQueue(mMusicList));
            onPrepare();

            mMediaSession.setQueue(mPlaylist);
            mMusicList.clear();
            mMusicList = mLastMetaManager.QueueToMetadata(mPlaylist, mMediaSession.getController().getExtras());

            return true;
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description) {
            super.onAddQueueItem(description);
            Log.d(TAG, "onAddQueueItem: "+description);

            boolean isUpdate = isAddQueueItem(description);

            if (isUpdate) {
                mPlaylist.add(new MediaSessionCompat.QueueItem(description, description.hashCode()));

                mQueueIndex = (mQueueIndex == -1) ? 0 : mQueueIndex;
                mMediaSession.setQueue(mPlaylist);
            }

        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description, int index) {
            super.onAddQueueItem(description, index);
            if (!isAddQueueItem(description)) {
                LogUtil.e(TAG,"onAddQueueItem：添加歌曲失败！原因：该播放队列中已有此歌曲。");
                return;
            }
            if (index >= mPlaylist.size()) {
                onAddQueueItem(description);
                return;
            }
            boolean result = updatePlayList(description);
            Log.d(TAG, "onAddQueueItem: "+description+", "+index+", "+(result ? "成功":"失败"));
        }

        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            super.onRemoveQueueItem(description);
            MediaSessionCompat.QueueItem itemTarget = null;
            for (MediaSessionCompat.QueueItem item : mPlaylist){
                if (description.toString().equals(item.getDescription().toString())){
                    itemTarget = item;
                    break;
                }
            }
            if (itemTarget != null) {
                LogUtil.d(TAG,"onRemoveQueueItem: 移除是否成功 " + mPlaylist.remove(itemTarget));
            }
            mQueueIndex = (mPlaylist.isEmpty()) ? -1 : mQueueIndex;
            mMediaSession.setQueue(mPlaylist);
        }

        @Override
        public void onSetShuffleMode(int shuffleMode) {//设置随机播放模式
            //super.onSetShuffleMode(shuffleMode);
            Bundle bundle = new Bundle();
            bundle.putInt(MediaPlayerManager.DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE,
                    mMediaPlayerManager.playbackModeChange());
            mMediaSession.sendSessionEvent(
                    MediaPlayerManager.DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE,bundle);
            StartForeground(mMediaSession.getSessionToken(),
                    mMediaPlayerManager.newPlaybackState(
                            mMediaSession.getController().getPlaybackState().getState(),null));
        }

        @Override
        public void onSetRating(RatingCompat rating) {//设置收藏
            //super.onSetRating(rating);
            MusicBean musicBean =
                    mLastMetaManager.getMusicBean(mMediaSession.getController().getMetadata());

            if (!SongListHelper.isSongLoved(getApplication(), musicBean.getTitle())){
                Bundle bundle = SongListHelper.collectionSong(
                        getApplication(),"最近很喜欢", musicBean);
                String toastMsg =  bundle == null || !bundle.getBoolean("isSuccessful") ?
                        "收藏歌曲失败!" : "已收藏歌曲至最近很喜欢歌单！";
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() -> Toast.makeText(getApplicationContext(),toastMsg,Toast.LENGTH_SHORT).show());
            }else{
                SongListHelper.deleteSheetSong(getApplication(), "最近很喜欢", musicBean);
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() -> Toast.makeText(getApplicationContext(),"已取消收藏",Toast.LENGTH_SHORT).show());
            }
            //更新通知消息
            Bundle bundle = mMediaSession.getController().getExtras();
            bundle = bundle == null ? new Bundle() : bundle;

            bundle.putString("MusicTitle",musicBean.getTitle());

            PlaybackStateCompat newPlaybackState = mMediaPlayerManager.newPlaybackState(
                    mMediaSession.getController().getPlaybackState().getState(),bundle);

            updateNotification(newPlaybackState);
            //更新视图
            mMediaSession.sendSessionEvent(MediaPlayerManager.DYQL_CUSTOM_ACTION_IS_LOVED, bundle);
            //因为局部更新播放信息后，会导致页面初始化时还沿用的PlaybackStateCompat的滞后信息
            mMediaSession.setExtras(bundle);
        }

        @Override
        public void onPlay() {
            if (mMediaPlayerManager == null && !mMediaSession.isActive()) {
                initMediaPlayerManager();
                //播放上次记录的播放进度.第一次播放且拖动音乐进度条
                mMediaPlayerManager.seekTo(mLastMetaManager.getLastMusicPosition());
            }else mMediaPlayerManager.PlayFromUri();
        }

        @Override
        public void onPause() {
            //mMediaPlayerManager.OnPause(IS_AUDIO_FOCUS_LOSS_TRANSIENT);
            mMediaPlayerManager.audioPlayerAmplitude(IS_AUDIO_FOCUS_LOSS_TRANSIENT,false);
            IS_AUDIO_FOCUS_LOSS_TRANSIENT = false;
            //stopNotification();
        }

        @Override
        public void onStop() { releaseMediaPlayerManager(); stopNotification();}

        @Override
        public void onPrepare() {
            if (mPlaylist.size() == 0) { Log.e(TAG, "onPrepare: 确定初始队列位置失败！");return; }

            MediaMetadataCompat metadata = mMediaSession.getController().getMetadata();
            String mediaId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
            int i = 0;
            for (MediaSessionCompat.QueueItem item : mPlaylist){
                Log.d(TAG, "onPrepare: "+mediaId+", "+item.getDescription().getMediaId());
                if (mediaId.equals(item.getDescription().getMediaId())) {
                    mQueueIndex = i;
                }
                i++;
            }
            Log.d(TAG, "onPrepare: 确定初始队列位置 "+mQueueIndex);
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            MediaPlayerNextPlay(true);
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            MediaPlayerNextPlay(false);
        }

        @Override
        public void onSeekTo(long pos) {
            if (mMediaPlayerManager == null && !mMediaSession.isActive()) {
                initMediaPlayerManager();
            }
            mMediaPlayerManager.seekTo(pos);
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            Log.w(TAG, "onPlayFromMediaId: "+mediaId);
            if (mMediaPlayerManager == null && !mMediaSession.isActive()) { initMediaPlayerManager(); }

            mMediaPlayerManager.resetCurrentPosition();
            mMediaPlayerManager.setDataRes(
                    getMetadata(mediaId).getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI));
            mMediaPlayerManager.PlayFromUri();
            mMediaSession.setMetadata(getMetadata(mediaId));
            SaveLastMusicPlay();
            //！！！确认列表位置，因为此回调会包含搜索列表点击
            onPrepare();
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            //super.onPlayFromUri(uri, extras);
            if (uri == null) { return;}
            if (mMediaPlayerManager == null && !mMediaSession.isActive()) { initMediaPlayerManager(); }

            if (extras != null){ //
                String mediaId = extras.getString(
                        MediaMetadataCompat.METADATA_KEY_MEDIA_ID,"");
                //是否重置播放进度
                LogUtil.d(TAG,"是否重置播放进度 "+
                        mMediaPlayerManager.resetCurrentPosition(mediaId));
                //调试
                //将其他音量，播放模式等信息装载进去
                MediaMetadataCompat metadata = getMetadata(MusicHelper.getMediaMetadata(extras));
                //更新歌曲数据
                mMediaSession.setMetadata(metadata);
                //保存歌曲数据
                SaveLastMusicPlay();
            }

            mMediaPlayerManager.setDataRes(uri.getPath());
            mMediaPlayerManager.PlayFromUri();
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            super.onCustomAction(action, extras);
            Bundle bundle = new Bundle();

            switch (action) {
                case MyAudioManager.DYQL_CUSTOM_ACTION_CURRENT_VOLUME:
                    if (extras == null) { Log.e(TAG, "onCustomAction: 音量新消息为空！");return; }
                    int volume = extras.getInt(MyAudioManager.DYQL_CUSTOM_ACTION_CURRENT_VOLUME);
                    setVolume(volume);
                    bundle.putInt(action,volume);
                    break;
                case BaseMusicService.DYQL_NOTIFICATION_STYLE:
                    //样式更改
                    boolean style = extras.getBoolean(BaseMusicService.DYQL_NOTIFICATION_STYLE);
                    MusicService.super.setNotificationStyle(mMediaSession.getSessionToken(),
                            mMediaSession.getController().getPlaybackState(), style);
                    //保存当前样式状态
                    if (mLastMetaManager != null) {mLastMetaManager.saveNotificationStyle(style);}
                    break;
                case  DYQL_CUSTOM_ACTION_UPDATE_PLAYLIST: //更新播放列表, 不改变数量并置顶
                    if (mMediaSession != null) {
                        LogUtil.d(TAG,"更新播放列表");
                        if (extras != null && extras.getBoolean("isNeedTopSong", false)){
                            String mediaId = extras.getString("TopSongMediaId", null);
                            MediaMetadataCompat metadata = getMetadata(mediaId);

                            boolean result = updatePlayList(metadata);
                            LogUtil.d(TAG,"置顶播放列表"+(result ? "成功":"失败")+"，id "+mediaId);
                        }
                    }
                    break;
            }
            //最后通知客户端：服务端已完成自定义动作，并将额外信息Bundle发送给客户端，以便其上控件 更新 数据和状态
            if (mMediaSession != null) { mMediaSession.sendSessionEvent(action,bundle); }
            else { LogUtil.e(TAG,"媒体会话为空！无法向客户端发送自定义动作！"); }
        }

        private void MediaPlayerNextPlay(boolean isSkipNext){
            if (mMediaPlayerManager == null) { initMediaPlayerManager(); }
            int mode = mMediaPlayerManager.getPlaybackMode();
            Log.d(TAG, "MediaPlayerNextPlay: 当前播放队列"+mQueueIndex+", 播放模式 "+mode);
            mMediaPlayerManager.setLooping();
            int musicCount = mPlaylist.size();
            //确定上、下一曲的位置
            if (musicCount < 2) {
                //如果音乐列表里只有一首歌曲，则getMusicListSize()为 1，
                // 那么mMusicQueueIndex为getMusicListSize()-1
                if (musicCount == 0) {
                    Toast.makeText(MusicService.this, "亲，没有发现歌曲哦", Toast.LENGTH_SHORT).show();
                    return;
                }
                mQueueIndex = 0;
            } else {
                if (mode == MediaPlayerManager.getDyqlPlaybackModeOrder()){
                    nextQueueOrder(isSkipNext,musicCount);
                }else if (mode == MediaPlayerManager.getDyqlPlaybackModeRandom()){
                    if (musicCount <= 3) {
                        nextQueueOrder(isSkipNext,musicCount);
                    }else {
                        int index = new Random().nextInt(musicCount -1);
                        while (index == mQueueIndex) {
                            //当音乐列表里有至少5首时才能随机到不同的歌曲
                            index = new Random().nextInt(musicCount -1);
                        }
                        mQueueIndex = index;
                    }
                }else {
                    Log.d(TAG, "NextMediaPlayer: 重复播放");
                }
            }
            //获得歌曲信息，并播放歌曲
            final String mediaId = mPlaylist.get(mQueueIndex).getDescription().getMediaId();
            onPlayFromMediaId(mediaId,null);
            Log.d(TAG, "MediaPlayerNextPlay: "+mediaId+", "+mQueueIndex);
        }

        private void nextQueueOrder(boolean isSkipNext, int musicCount){
            if (isSkipNext) {//前缀运算符先于取余运算符执行【顺序播放】下一曲
                mQueueIndex = (mQueueIndex + 1) % musicCount;
            }else {//【顺序播放】上一曲
                mQueueIndex = mQueueIndex > 0 ? mQueueIndex - 1 : musicCount - 1;
            }
        }
    }

    private class MyNotificationListener implements MediaPlayerManager.NotificationListener{

        @Override
        public void onUpdateNotification() {
            Log.w(TAG, "onUpdateNotification: ");
            updateNotification();
        }

        @Override
        public void onStopNotification() {
            stopNotification();
        }
    }
    private void updateNotification(){
        super.StartForeground(mMediaSession.getSessionToken(),
                mMediaSession.getController().getPlaybackState());
    }
    private void updateNotification(PlaybackStateCompat playbackState){
        super.StartForeground(mMediaSession.getSessionToken(), playbackState);
    }
    private void stopNotification(){ super.StopForeground(); }

    private class MyAudioFocusChangeListener implements AudioManager.OnAudioFocusChangeListener{

        @Override
        public void onAudioFocusChange(int focusChange) {
            //Log.d(TAG, "onAudioFocusChange: "+focusChange);
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN: //获得长时间播放焦点,短暂失去焦点后触发此回调
                    Log.e(TAG, "onAudioFocusChange: 获得长时间播放焦点");
                    mMediaSession.getController().getTransportControls().play();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT: //短暂失去焦点
                    Log.e(TAG, "onAudioFocusChange: 短暂失去焦点");
                    IS_AUDIO_FOCUS_LOSS_TRANSIENT = true;
                    mMediaSession.getController().getTransportControls().pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    Log.e(TAG, "onAudioFocusChange: 失去焦点，但可以共同使用");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS://长时间失去焦点
                    Log.e(TAG, "onAudioFocusChange: 长时间失去焦点");
                    IS_AUDIO_FOCUS_LOSS_TRANSIENT = true;
                    mMediaSession.getController().getTransportControls().pause();
                    break;
            }
        }
    }

    private void initLifeBackground(){
        if (mTimer != null) { return; }

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mMediaSession != null && !mMediaSession.isActive()){ return;} //当音乐播放时监听进度和音量更改

                //Log.d(TAG, "报告组织！MusicService还活着！position = "+getPosition());

                if (mMediaPlayerManager != null){
                    mLastMetaManager.saveMusicPosition(mMediaPlayerManager.getCurrentPosition());
                }

                //监听音量更改
                if (mMediaPlayerManager != null && mMediaPlayerManager.checkAudioChange()) {
                    setVolume(mMediaPlayerManager.getVolume());
                    Bundle bundle = new Bundle();
                    bundle.putInt(MyAudioManager.DYQL_CUSTOM_ACTION_CURRENT_VOLUME,mMediaPlayerManager.getVolume());
                    mMediaSession.sendSessionEvent(MyAudioManager.DYQL_CUSTOM_ACTION_CURRENT_VOLUME,bundle);
                }
                // Log.d(TAG, "报告组织！MusicService还活着！"+sdf.format(System.currentTimeMillis()));
            }
        },300,1000);
    }

    private MediaMetadataCompat getMetadata(String mediaId) {
        if (mediaId == null || TextUtils.isEmpty(mediaId) || getMusicNumber() < 1) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> Toast.makeText(getApplicationContext(),"播放错误，列表还没有歌曲！",Toast.LENGTH_SHORT).show());

            LogUtil.d(TAG,"播放错误，列表没有歌曲！MediaId: "+mediaId);
            return null;
        }

        MediaMetadataCompat metadata = mMusicList.get(mediaId);
        return metadata == null ? null : getMetadata(metadata);
    }

    private MediaMetadataCompat getMetadata(@NonNull MediaMetadataCompat metadata) {

        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();

        mSongSheet.showAlbumBitmap(
                bitmap -> builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART,bitmap.get()),
                Objects.requireNonNull(metadata).getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI),
                getResources());

        for (String key :
                new String[]{
                        MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                        MediaMetadataCompat.METADATA_KEY_ALBUM,
                        MediaMetadataCompat.METADATA_KEY_ARTIST,
                        MediaMetadataCompat.METADATA_KEY_GENRE,
                        MediaMetadataCompat.METADATA_KEY_TITLE,
                        MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                        MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,
                        MediaMetadataCompat.METADATA_KEY_MEDIA_URI
                }) {
            builder.putString(key, metadata.getString(key));
            //Log.d(TAG, "getMetadata: "+metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI));
        }
        //放入播放时长 long
        builder.putLong(
                MediaMetadataCompat.METADATA_KEY_DURATION,
                metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
        //放入当前音量 long
        builder.putLong(
                MyAudioManager.DYQL_CUSTOM_ACTION_CURRENT_VOLUME, mMediaPlayerManager.getVolume());
        //放入最大音量 long
        builder.putLong(
                MyAudioManager.DYQL_CUSTOM_ACTION_MAX_VOLUME, mMediaPlayerManager.getMaxVolume());
        //放入播放模式 long
        builder.putLong(
                MediaPlayerManager.DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE, mMediaPlayerManager.getPlaybackMode());
        return builder.build();
    }
    private int getMusicNumber(){
        return mMusicList == null ? 0 : mMusicList.size();
    }
    //**************************************记录上次播放音乐信息*************************************//
    private void GetLastMusicPlay(){
        int mode = mMediaPlayerManager.getPlaybackMode();
        //将上次播放的音乐信息放入MediaSession
        MediaMetadataCompat.Builder metadataBuilder = mLastMetaManager.getLastMusicPlay();
        String albumPath = mLastMetaManager.getLastAlbumPath();
        //装载播放模式、当前音量，最大音量
        metadataBuilder
                .putLong(MediaPlayerManager.DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE, mode)
                .putLong(MyAudioManager.DYQL_CUSTOM_ACTION_MAX_VOLUME,mMediaPlayerManager.getMaxVolume())
                .putLong(MyAudioManager.DYQL_CUSTOM_ACTION_CURRENT_VOLUME, mMediaPlayerManager.getVolume())
                //个性样式为0，系统样式为1
                .putLong(BaseMusicService.DYQL_NOTIFICATION_STYLE,mLastMetaManager.getNotificationStyle() ? 0:1);
        //装载专辑图片 Bitmap
        mSongSheet.showAlbumBitmap(bitmap -> mMediaSession.setMetadata(
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART,bitmap.get()).build()),
                albumPath,getResources());

    }
    private void SaveLastMusicPlay(){
        mLastMetaManager.SaveLastMusicPlay(
                mMediaSession.getController().getMetadata(),mMediaPlayerManager.getCurrentPosition());
    }
    private void setVolume(int level){ mMediaPlayerManager.setVolume(level); }
}
