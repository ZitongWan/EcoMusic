package com.xwrl.mvvm.demo.viewmodel;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.LayerDrawable;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.TypedValue;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.databinding.Observable;
import androidx.databinding.PropertyChangeRegistry;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModel;

import com.xwrl.mvvm.demo.R;
import com.xwrl.mvvm.demo.service.manager.MediaPlayerManager;
import com.xwrl.mvvm.demo.util.PictureUtil;

import java.lang.ref.SoftReference;

public abstract class BaseViewModel extends AndroidViewModel implements Observable {

    private PropertyChangeRegistry callbacks = new PropertyChangeRegistry();
    protected MediaControllerCompat mMediaController;
    protected long lastClickTime = 0;
    protected final int SAFE_CLICK_INTERVAL = 360;


    public BaseViewModel(Application application) {
        super(application);
    }

    public void setMediaControllerCompat(MediaControllerCompat mediaControllerCompat) {
        this.mMediaController = mediaControllerCompat;
    }

    @Override
    public void addOnPropertyChangedCallback(
            OnPropertyChangedCallback callback) {
        callbacks.add(callback);
    }

    @Override
    public void removeOnPropertyChangedCallback(
            OnPropertyChangedCallback callback) {
        callbacks.remove(callback);
    }

    /**
     * Notifies observers that all properties of this instance have changed.
     */
    void notifyChange() {
        callbacks.notifyCallbacks(this, 0, null);
    }

    /**
     * Notifies observers that a specific property has changed. The getter for the
     * property that changes should be marked with the @Bindable annotation to
     * generate a field in the BR class to be used as the fieldId parameter.
     *
     * @param fieldId The generated BR id for the Bindable field.
     */
    void notifyPropertyChanged(int fieldId) {
        callbacks.notifyCallbacks(this, fieldId, null);
    }
    /** ViewModel类的生命周期方法。
     * 当不再使用此ViewModel并将其销毁时，将调用此方法。
     * 当ViewModel观察到一些数据并且需要清除此订阅以防止此ViewModel泄漏时，
     * 此选项非常有用。*/
    @Override
    protected void onCleared() {
        super.onCleared();
        if (callbacks != null) {
            callbacks.clear();
            callbacks = null;
        }
        if (mMediaController != null) mMediaController = null;
    }

    protected boolean isAgreeClick(){
        if (System.currentTimeMillis() - lastClickTime > SAFE_CLICK_INTERVAL){
            lastClickTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    protected int dpToPx(int dp, Application application){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,application.getResources().getDisplayMetrics());
    }
    /*
    * 已用Shape资源文件代替
    * */
    @Deprecated
    protected RoundedBitmapDrawable getRecordBg(Application application){
        return PictureUtil.createColorDrawable(application,
                Color.argb(18,255,255,255),400,400);
    }
    //绘制小唱片
    protected LayerDrawable getRecord(Bitmap bitmap, Application application){

        return PictureUtil.createCircleDrawable(application, bitmap,
                dpToPx(42,application),100, 1,
                Color.argb(32,255,255,255));
    }
    //绘制SongLrc页面的大唱片
    protected RoundedBitmapDrawable getRecordBig(Bitmap bitmap, Application application, int size){

        //return PictureUtil.createCircleDrawableBig(application, bitmap,400, size);
        return PictureUtil.createCircleDrawable(application, bitmap,400, 400);
    }
    //绘制高斯模糊背景
    protected LayerDrawable getBlurDrawable(Bitmap bitmap, Application application){
        return PictureUtil.createBlurDrawable(application,
                1080,1920,20f, bitmap);
    }

    protected <T>T getSoftReference(T obj){
        return new SoftReference<>(obj).get();
    }

    private PlaybackStateCompat.CustomAction getCustomAction(){
        return new PlaybackStateCompat.CustomAction.Builder(
                MediaPlayerManager.DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE,
                "playback_mode_change",
                R.drawable.iv_playback_mode_order).build();
    }
}
