package com.xwrl.mvvm.demo.service.manager;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.VolumeShaper;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.xwrl.mvvm.demo.R;
import com.xwrl.mvvm.demo.model.helper.SongListHelper;

import java.io.IOException;
import java.util.Locale;


public class MediaPlayerManager {
    private static final String TAG = "MediaPlayManager";
    public static final String DYQL_CUSTOM_ACTION_IS_LOVED = "is_loved_dyql";
    //
    private Application mApplication;
    private MediaSessionCompat mMediaSession;
    private MyAudioManager mMyAudioManager;
    private MediaPlayer mMediaPlayer;
    private boolean isFirstPlay = true;
    private int mCurrentPosition = -1, mCurrentAudioLevel = 0;
    //播放模式Flag
    private int DYQL_PLAYBACK_MODE = 0;
    protected static final int DYQL_PLAYBACK_MODE_ORDER = 1;
    protected static final int DYQL_PLAYBACK_MODE_REPEAT = 3;
    protected static final int DYQL_PLAYBACK_MODE_RANDOM = 2;
    protected static final int DYQL_DELAY_PAUSE_TIME = 1600;
    protected static final int DYQL_DELAY_PAUSE_TIME_INTERVAL = 160;
    public static final String DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE = "playback_mode_change_dyql";
    private VolumeShaper mVolumeShaper;
    private MyCountDownTimer mMyCountDownTimer;

    //MusicService 回调更新通知栏
    private NotificationListener mNotificationListener;
    public interface NotificationListener{
        void onUpdateNotification();
        void onStopNotification();
    }

    public MediaPlayerManager(Application application,
                              MediaSessionCompat mediaSession,
                              NotificationListener notificationListener,
                              AudioManager.OnAudioFocusChangeListener focusChangeListener){
        mApplication = application;
        mMediaSession = mediaSession;
        mNotificationListener = notificationListener;
        //初始化MediaPlayer
        mMediaPlayer = new MediaPlayer();
        //唤醒锁定模式，关闭屏幕时，CPU不休眠
        mMediaPlayer.setWakeMode(application, PowerManager.PARTIAL_WAKE_LOCK);
        //初始化MyAudioManager
        mMyAudioManager = new MyAudioManager(application,focusChangeListener,
                                                        mMediaPlayer.getAudioSessionId());

        mMediaPlayer.setAudioAttributes(mMyAudioManager.getPlaybackAttributes());
        mMediaPlayer.setOnErrorListener(new onErrorListener());
        mMediaPlayer.setOnPreparedListener(new onPreparedListener());
        mMediaPlayer.setOnCompletionListener(new onCompleteListener());
    }

    public void onDestroy(){
        if (mApplication != null) { mApplication = null; }
        if (mMediaSession != null) { mMediaSession = null; }
        if (mNotificationListener != null) { mNotificationListener = null; }
        if (mMyAudioManager != null) {
            mMyAudioManager.onDestroy();
            mMyAudioManager = null;
        }
        releaseMediaPlayer();
    }

    public void releaseMediaPlayer(){
        if (mMediaPlayer != null) {
            StopMediaPlayer();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
    //MediaPlayer 相关方法
    public void StopMediaPlayer(){
        //Log.d(TAG, "StopMediaPlayer: ");
        if (isFirstPlay() && mCurrentPosition <= 0) return;

        if (mMediaPlayer != null) {
            if (isPlaying())  {
                mMediaPlayer.pause();
                mMediaPlayer.seekTo(0);
                mMediaPlayer.stop();
            }
            //释放wifi锁 , 释放音频焦点
            if (mMyAudioManager != null) { mMyAudioManager.releaseAudioFocus(); }
            //先停止音乐再释放VolumeShaper，此步骤应该在MediaPlayer的 reset 或者 release 之前
            if (mVolumeShaper != null) {
                mVolumeShaper.close(); //施放VolumeShaper
                mVolumeShaper = null;
            }
            //重置MediaPlayer
            mMediaPlayer.reset();
            //适用于上次播放有进度，但是第一次播放了这首歌曲，所以播放进度保留
            if (!isFirstPlay()) resetCurrentPosition();

        }else System.out.println("MediaPlayer is null!");
    }
    public void setDataRes(String path){
        StopMediaPlayer();
        try {
            mMediaPlayer.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void PlayFromUri(){
        if (mMediaPlayer == null || isPlaying() ) return;
        if (!mMediaSession.isActive()) { mMediaSession.setActive(true); }
        if (mMyAudioManager != null) { setVolume(mCurrentAudioLevel); }

        if (mCurrentPosition <= 0 || isFirstPlay) {
            mMediaSession.setPlaybackState(
                    newPlaybackState(PlaybackStateCompat.STATE_BUFFERING,null));
            mMediaPlayer.prepareAsync();
        }else { //暂停后继续播放
            mMediaPlayer.seekTo(mCurrentPosition);

            Bundle bundle = mMediaSession.getController().getExtras();
            bundle.putBoolean("Continue_Playing_Tips",true);
            checkFocusPlay(bundle);

            Log.d(TAG, "PlayMediaPlayer: 暂停后播放");
        }

    }
    public void OnPause(boolean notReleaseAudio){

        //停止播放音乐释放焦点
        if (!notReleaseAudio) { mMyAudioManager.releaseAudioFocus(); }

        mMediaSession.setPlaybackState(newPlaybackState(PlaybackStateCompat.STATE_PAUSED,null));
        mNotificationListener.onUpdateNotification();

        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            //记录播放队列位置
            mCurrentPosition = mMediaPlayer.getCurrentPosition();
            mMediaPlayer.pause();
        }
        mMyCountDownTimer.releaseCountTimer();

        mNotificationListener.onStopNotification();
    }

    public void seekTo(long pos) {
        if (mMediaPlayer == null && mMediaSession == null) return;

        MediaControllerCompat mMediaController = mMediaSession.getController();
        if (mMediaController.getPlaybackState().getState()
                == PlaybackStateCompat.STATE_PLAYING) mMediaPlayer.pause();
        mCurrentPosition = pos == 0 ? 1 : (int) pos;
        if (isFirstPlay()) {
            String path = mMediaController.getMetadata()
                    .getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI);
            setDataRes(path);
        }
        PlayFromUri();
    }
    //***************************音量更改与获取****************************/
    public void audioPlayerAmplitude(boolean notReleaseAudio, boolean isSlowPlay){
        mMyCountDownTimer = new MyCountDownTimer(
                DYQL_DELAY_PAUSE_TIME, DYQL_DELAY_PAUSE_TIME_INTERVAL,
                notReleaseAudio, isSlowPlay);
        mMyCountDownTimer.start();
    }
    private class MyCountDownTimer extends CountDownTimer {

        private final float VolumeIndex;
        private int count;
        private final int minuend;
        private final boolean notReleaseAudio;

        // millisInFuture: 倒计时总时间，  countDownInterval: 倒计时时间间隔， 就可以算出倒计时的次数。
        // notReleaseAudio 不释放音频焦点， isSlowPlay 是否是缓慢播放(true 声音渐强， false 声音减弱)
        public MyCountDownTimer(long millisInFuture, long countDownInterval,
                                boolean notReleaseAudio, boolean isSlowPlay) {
            super(millisInFuture, countDownInterval);//获得当前音量，在重新播放前设置

            this.VolumeIndex = getVolumePercent((float) mCurrentAudioLevel / getMaxVolume());
            this.count = 0;

            this.notReleaseAudio = notReleaseAudio;
            this.minuend = isSlowPlay ? 0 : -10;
        }

        @Override
        public void onTick(long l) { //以 倒计时的执行次数 来 降低媒体音量 的 方法逻辑

            int x = Math.min(Math.abs(Math.min(++count,10) + minuend), 10);
            float ratio = Math.min((float) (x * x + x * 2) / 100, 1.f),
                    allVolume = getVolumePercent(VolumeIndex * ratio);

            mMediaPlayer.setVolume(allVolume,allVolume);

            Log.d(TAG,"->onTick: "+l+", "+VolumeIndex+", count = "+count+
                    ", x = "+x+", ratio = "+ratio+", 声道音量 = "+allVolume);
        }

        @Override
        public void onFinish() { //倒计时结束时 的 方法逻辑
            if (minuend == -10) { //声音减弱，为音乐缓慢暂停
                OnPause(this.notReleaseAudio); //调用暂停音乐的方法
            } else releaseCountTimer(); //声音渐强，结束后直接释放掉本倒计时定时器即可
        }

        public void releaseCountTimer() { this.cancel(); }

        //将运算后的浮点数float的小数点位控制在两位数！
        private Float getVolumePercent(float x){
            return Float.parseFloat(String.format(Locale.CHINA,"%.2f",x));
        }
    }
    public boolean checkAudioChange(){ return mCurrentAudioLevel != mMyAudioManager.getVolume();}
    public void lowerTheVolume(){ mMyAudioManager.lowerTheVolume(); }
    public void setVolume(int volume) {
        if (mMyAudioManager == null) { return; }

        if (mMyAudioManager.canVolumeFix()) { mMyAudioManager.setVolume(volume);}

        //设置播放流音量，防止静音时播放后，再调整音量无效的问题
        float percent = Float.parseFloat(String.format(Locale.CHINA, "%.2f",
                (float)volume / (float)mMyAudioManager.getMaxVolume()));
        Log.d(TAG, "setVolume: "+percent);
        if (mMediaPlayer != null) mMediaPlayer.setVolume(percent,percent);

        mCurrentAudioLevel = volume;

    }
    public int getVolume(){
        //Log.d(TAG, "getVolume: "+mMyAudioManager.getVolume());
        return mMyAudioManager.getVolume();}
    public int getMaxVolume(){
        //Log.d(TAG, "getMaxVolume: "+mMyAudioManager.getMaxVolume());
        return mMyAudioManager.getMaxVolume();
    }
    private VolumeShaper.Configuration getVolumeConfiguration(){

        return new VolumeShaper.Configuration.Builder()
                .setDuration(1600)  //执行时长
                .setCurve(new float[]{0.f,1.f}, new float[]{0.f,1.f})//时间和音量区间
                //插值器类型, 指定声音变化的插值方式
                //VolumeShaper.Configuration.INTERPOLATOR_TYPE_STEP（分段曲线）
                //VolumeShaper.Configuration.INTERPOLATOR_TYPE_LINEAR（线性）
                //VolumeShaper.Configuration.INTERPOLATOR_TYPE_CUBIC（三次曲线）
                //VolumeShaper.Configuration.INTERPOLATOR_TYPE_CUBIC_MONOTONIC（单调三次曲线）
                .setInterpolatorType(VolumeShaper.Configuration.INTERPOLATOR_TYPE_LINEAR)
                .build();
    }

    public void resetCurrentPosition() { mCurrentPosition = 0; }

    public boolean resetCurrentPosition(@NonNull String currentMediaId){
        boolean isReset = !isFirstPlay() || !mMediaSession.getController().getMetadata()
                .getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID).equals(currentMediaId);
        if (isReset) { resetCurrentPosition(); }
        return isReset;
    }
    public int getCurrentPosition() {
        return isPlaying() ? mMediaPlayer.getCurrentPosition() : mCurrentPosition;
    }
    public void initCurrentPosition(int position){ mCurrentPosition = position;}
    //***************************一些播放状态****************************/
    public boolean isPlaying() { return mMediaPlayer != null && mMediaPlayer.isPlaying(); }
    public boolean isFirstPlay() { return isFirstPlay; }

    public void setLooping(){
        mMediaPlayer.setLooping(getPlaybackMode() == getDyqlPlaybackModeRepeat());
    }
    //***************************MediaPlayer回调区****************************/
    private class onCompleteListener implements MediaPlayer.OnCompletionListener {
        @Override
        public void onCompletion(MediaPlayer mp) {
            Log.d(TAG, "onCompletion: ");
            //播放下一曲
            mMediaSession.getController().getTransportControls().skipToNext();
        }
    }

    private class onErrorListener implements  MediaPlayer.OnErrorListener{
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            //当播放错误时，MediaPlayer执行此回调并停止播放，故isPlayerPrepared 状态应为true，已准备好播放
            /*if (!isPlayerPrepared) isPlayerPrepared = true;
            isError_Flag = true;//必须要设置不同Path才能继续播放*/
            if (what != -38 && extra != 0) mApplication.sendBroadcast(new Intent("error"));
            Log.d(TAG, "onErrorListener: what:"+what+" , extra = "+extra);
            switch (what) {
                case 1:
                    if (extra == 1)
                        Log.e(TAG,"播放错误，歌曲地址为空,请播放其他歌曲");
                    else if (extra == 2)
                        Log.e(TAG,"该音乐文件已损坏,请播放其他歌曲");
                    else if (extra == 28)
                        Log.e(TAG,"该音乐媒体对象为空，请重新打开App");
                    else if (extra == -2147483648)
                        Log.e(TAG,"音乐文件解码失败,请删除该文件,尝试播放网络版本");
                    else Log.e(TAG,"播放错误,请播放其他歌曲");
                    break;
                case 2:
                    if (extra == 2) Log.d(TAG, "onError: 音乐文件解码失败,请播放其他歌曲");
                    else Log.d(TAG, "onError: 播放错误,请播放其他歌曲");
                    Log.e(TAG, "Error Prepare 只能使用log或者通知");
                    break;
            }
            //onError返回值返回false会触发onCompletionListener，y
            //所以返回false，一般意味着会退出当前歌曲播放。
            //如果不想退出当前歌曲播放则应该返回true
            return true;
        }
    }

    private class onPreparedListener implements MediaPlayer.OnPreparedListener{

        @Override
        public void onPrepared(MediaPlayer mp) {
            Log.e(TAG, "onPrepared: "+mCurrentPosition);
            if (mCurrentPosition > 0) mMediaPlayer.seekTo(mCurrentPosition);
            else if (mCurrentPosition == -1) resetCurrentPosition();
            //获得音频焦点
            checkFocusPlay(mMediaSession.getController().getExtras());
            if (isFirstPlay) { isFirstPlay = false; }
        }
    }
    private void checkFocusPlay(Bundle bundle){
        if (bundle == null) { bundle = new Bundle(); }
        int audioFocusState = mMyAudioManager.registerAudioFocus();

        if(audioFocusState == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            mMediaPlayer.start();
            //mVolumeShaper = mMediaPlayer.createVolumeShaper(getVolumeConfiguration());
            //mVolumeShaper.apply(VolumeShaper.Operation.PLAY);
            audioPlayerAmplitude(false,true);
            CharSequence title = mMediaSession.getController().getMetadata().getDescription().getTitle();
            bundle.putString("MusicTitle",title == null ? null : title.toString());
            //★更新状态, 使该通知处于栈顶, 赋予播放进度初始值, 从而在通知中展示播放进度
            PlaybackStateCompat newPlaybackState = newPlaybackState(PlaybackStateCompat.STATE_PLAYING, bundle);
            //现在mediaSession也更新播放模式和是否收藏的额外信息 | 2022.01.10
            mMediaSession.setPlaybackState(newPlaybackState);
            Bundle extras = newPlaybackState.getExtras();
            //同步播放模式 和 是否收藏，都是为了保存上下一曲的MediaId
            if (extras != null) {
                bundle.putInt(DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE,
                        extras.getInt(DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE,
                                DYQL_PLAYBACK_MODE_ORDER));

                bundle.putBoolean(DYQL_CUSTOM_ACTION_IS_LOVED,
                        extras.getBoolean(DYQL_CUSTOM_ACTION_IS_LOVED, false));
            }
            mMediaSession.setExtras(bundle);
            mNotificationListener.onUpdateNotification();

        }else if (audioFocusState == AudioManager.AUDIOFOCUS_REQUEST_FAILED){//请求焦点失败
            Toast.makeText(mApplication,"请求播放失败",Toast.LENGTH_SHORT).show();
        }else Log.e(TAG, "checkFocusPlay: 音频焦点延迟获得！");
    }
    //***************************获取新播放状态PlaybackStateCompat****************************/
    public PlaybackStateCompat newPlaybackState(@PlaybackStateCompat.State int newState, Bundle bundle){
        boolean isLoved = false;
        if (bundle == null) { bundle = new Bundle(); }
        else {
            PlaybackStateCompat playbackState = mMediaSession.getController().getPlaybackState();
            //如果是当前状态正在播放时 进行状态更改为 音乐暂停||停止 则不查询 是否是收藏歌曲

            boolean isNotCheckLove = playbackState != null && playbackState.getExtras() != null &&
                    (playbackState.getState() == PlaybackStateCompat.STATE_PAUSED ||
                            playbackState.getState() == PlaybackStateCompat.STATE_STOPPED) &&
                    getCurrentPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING;

            isLoved = isNotCheckLove ? playbackState.getExtras()
                    .getBoolean(DYQL_CUSTOM_ACTION_IS_LOVED,false) :
                    SongListHelper.isSongLoved(
                            mApplication ,bundle.getString("MusicTitle"));
        }

        bundle.putInt(DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE,getPlaybackMode());

        bundle.putBoolean(DYQL_CUSTOM_ACTION_IS_LOVED,isLoved);
        return new PlaybackStateCompat.Builder()
                .setExtras(bundle)
                //设置需使用的Action
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SEEK_TO |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                //关闭Notification
                                PlaybackStateCompat.ACTION_STOP |
                                //歌词action，翻译为字幕
                                PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED |
                                //歌曲收藏action，翻译为星级评级
                                PlaybackStateCompat.ACTION_SET_RATING |
                                //播放模式切换action，翻译为设置重复播放
                                PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE)
                .setState(newState, getCurrentPosition(), 1.0f).build();
        //.setState()中 播放进度的传递应该为最准确的播放进度，播放中 ？从MediaPlayer获取 ：成员变量 ！
        //否则依靠播放状态来获取播放进度的地方可能会与播放进度不一样！！！包括系统媒体展开式通知、自定义歌词控件等！！！
    }

    public PlaybackStateCompat getCurrentPlaybackState(){
        return mMediaSession != null ?
                mMediaSession.getController().getPlaybackState() :
                newPlaybackState(PlaybackStateCompat.STATE_NONE,null);
    }

    //***************************播放模式更改与获取****************************/
    public void setPlayBackMode(int mode){
        DYQL_PLAYBACK_MODE = (mode < 1 || mode > 3) ? 1 : mode;
        Log.e(TAG, "setPlayBackMode: "+mode);
    }

    public int getPlaybackMode(){
        Log.e(TAG, "getPlaybackMode: "+DYQL_PLAYBACK_MODE);return DYQL_PLAYBACK_MODE; }

    public static int getDyqlPlaybackModeOrder() { return DYQL_PLAYBACK_MODE_ORDER; }

    public static int getDyqlPlaybackModeRandom() { return DYQL_PLAYBACK_MODE_RANDOM; }

    public static int getDyqlPlaybackModeRepeat() { return DYQL_PLAYBACK_MODE_REPEAT; }

    public int playbackModeChange(){
        if (DYQL_PLAYBACK_MODE != DYQL_PLAYBACK_MODE_REPEAT) { DYQL_PLAYBACK_MODE++; }
        else DYQL_PLAYBACK_MODE = DYQL_PLAYBACK_MODE_ORDER;

        SharedPreferences settings = mApplication.getSharedPreferences("UserLastMusicPlay", 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt("MusicPlaybackMode",DYQL_PLAYBACK_MODE);
        editor.apply();

        return DYQL_PLAYBACK_MODE;
    }

}
