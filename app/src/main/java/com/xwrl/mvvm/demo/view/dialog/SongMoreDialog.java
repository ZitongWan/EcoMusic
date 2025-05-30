package com.xwrl.mvvm.demo.view.dialog;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.databinding.DataBindingUtil;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.xwrl.mvvm.demo.BaseActivity;
import com.xwrl.mvvm.demo.R;
import com.xwrl.mvvm.demo.bean.MusicBean;
import com.xwrl.mvvm.demo.databinding.DialogSongMoreBinding;
import com.xwrl.mvvm.demo.model.helper.MusicHelper;
import com.xwrl.mvvm.demo.model.helper.SongListHelper;
import com.xwrl.mvvm.demo.util.DialogUtil;
import com.xwrl.mvvm.demo.util.HttpUtil;
import com.xwrl.mvvm.demo.util.PictureUtil;
import com.xwrl.mvvm.demo.util.StringUtil;

import java.lang.ref.WeakReference;

public class SongMoreDialog extends BottomSheetDialog{

    private static final String TAG = "SongMoreDialog";

    private MediaMetadataCompat mMusicMetadata;
    private DialogSongMoreBinding mBinding;
    private Application mApplication;
    private MusicBean mBean;
    private String mSheetAlias;
    private WeakReference<Drawable> mAlbumBg;
    private ConfirmDialog.OnConfirmListener mOnConfirmListener;
    private MediaControllerCompat mMediaController;

    public SongMoreDialog(@NonNull Context context, Application application,
                          MediaMetadataCompat musicMetadata,
                          MediaControllerCompat mediaController,
                          String alias) {
        super(context);
        this.mMusicMetadata = musicMetadata;
        this.mMediaController = mediaController;
        this.mApplication = application;
        this.mSheetAlias = alias;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        DialogUtil.BottomShow(this,false);
        //设置为展开状态 behavior不会为空
        BottomSheetBehavior<FrameLayout> behavior = getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        //设置展开高度
        //behavior.setPeekHeight(***);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.inflate(LayoutInflater.from(getContext()),
                R.layout.dialog_song_more, null, false);
        setContentView(mBinding.getRoot());
    }

    @Override
    protected void onStart() {
        super.onStart();
        SyncMusicInfo(mMusicMetadata);
        getWindow().setWindowAnimations(R.style.DialogNoFlashScreen);
    }


    @Override
    protected void onStop() {
        super.onStop();
        unBindView();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public void setOnConfirmListener(ConfirmDialog.OnConfirmListener onConfirmListener) {
        this.mOnConfirmListener = onConfirmListener;
    }

    public void setMediaController(MediaControllerCompat mediaController) {
        this.mMediaController = mediaController;
    }

    public void setInVisible(){
        if (mBinding != null) {
            mBinding.dialogSongMoreRvDelete.setVisibility(View.GONE);
            mBinding.dialogSongMoreRvEdit.setVisibility(View.GONE);
        }
    }

    private void unBindView(){
        if (mAlbumBg != null){
            mAlbumBg.clear();
            mAlbumBg = null;
        }
        if (mBinding != null) {
            mBinding.dialogSongMoreIvCover.setImageDrawable(null);
            mBinding.dialogSongMoreRoot.setBackground(null);
            mBinding.getMusicInfo().release();
            mBinding.unbind();
            mBinding = null;
        }
        if (mOnConfirmListener != null) mOnConfirmListener = null;
        if (mMusicMetadata != null) mMusicMetadata = null;
        if (mApplication != null) mApplication = null;
        if (mSheetAlias != null) mSheetAlias = null;
        if (mMediaController != null) mMediaController = null;
        if (mBean != null) {
            mBean.release();
            mBean = null;
        }
    }

    private void SyncMusicInfo(MediaMetadataCompat metadata){
        mBinding.dialogSongMoreIvList.getDrawable().setTint(Color.parseColor("#E6E6E6"));

        if (metadata == null) return;

        mBean = MusicHelper.getMusicBean(mMusicMetadata);
        Log.d(TAG, "SyncMusicInfo: "+mBean.getDuration());
        mBinding.setMusicInfo(mBean);
        //加载专辑图片
        loadAlbumBitmap(metadata);

        mBinding.dialogSongMoreRvLove.setOnClickListener(v -> {

            CollectionToSheetDialog collectionToSheetDialog =
                                        new CollectionToSheetDialog(getContext(),
                                                mApplication,
                                                mMusicMetadata,
                                                mMediaController);
            collectionToSheetDialog.show();
            dismiss();
        });
        mBinding.dialogSongMoreRvDelete.setVisibility(mSheetAlias == null ? View.GONE : View.VISIBLE);
        mBinding.dialogSongMoreRvDelete.setOnClickListener(v -> {
            if (mSheetAlias.equals(BaseActivity.ACTIVITY_DEFAULT_SHEET_NAME)){
                Toast.makeText(getContext(),"该歌单不可删除歌曲",Toast.LENGTH_SHORT).show();
                return;
            }
            String confirmMsg = "您确定要删除"+mBean.getTitle()+"歌曲吗？";
            ConfirmDialog confirmDialog =
                    new ConfirmDialog(getContext(), R.style.DialogTheme, confirmMsg, false);
            confirmDialog.show();
            confirmDialog.setOnConfirmListener(result -> {
                if (!result) return;

                String msg = SongListHelper.deleteSheetSong(getContext(), mSheetAlias,mBean);

                Toast.makeText(getContext(),
                        msg == null || msg.contains("Error") ? "删除歌曲失败！" : msg,
                        Toast.LENGTH_SHORT).show();

                if (msg == null || msg.contains("Error"))
                    Log.w(TAG, msg == null ? "Error : 参数有空！" : msg);
                else {
                    //更新音乐列表,并关闭dialog
                    if (mOnConfirmListener != null) mOnConfirmListener.onConfirm(true);
                    dismiss();
                }

            });
        });

    }

    private void loadAlbumBitmap(MediaMetadataCompat metadata){
        String albumPath = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI);

        String mediaId = mMediaController.getMetadata()
                .getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID),
                currentMediaId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);

        boolean isSameSong = mediaId.equals(currentMediaId);
        if (StringUtil.isOnlyDigit(albumPath) && !isSameSong) { //加载网络或缓存图片
            Log.d(TAG, "loadNetAlbumBitmap: " + albumPath);
            String s = HttpUtil.getLocalPathPictures(albumPath+".jpg");
            if (HttpUtil.FileExists(s)) { loadNetAlbum(s); }
            s = HttpUtil.getLocalPathPictures(albumPath+".png");
            if (HttpUtil.FileExists(s)) { loadNetAlbum(s); }
            else loadNetAlbum(R.drawable.icon_fate);
            //本地缓存图片不存在，进行网络获取
        }else if (!isSameSong && (albumPath.contains(".jpg") || albumPath.contains(".png"))){
            loadNetAlbum(R.drawable.icon_fate);
        }else { //默认加载图片或者音乐文件里面的
            Resources resources = getContext().getResources();
            if (resources == null) return;
            loadNetAlbum(metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART));
        }

    }

    private void loadNetAlbum(@DrawableRes int resId){
        loadNetAlbum(Glide.with(getContext()).load(resId));
    }
    private void loadNetAlbum(Bitmap bitmap){
        loadNetAlbum(Glide.with(getContext()).load(bitmap == null ? R.drawable.icon_fate : bitmap));
    }
    private void loadNetAlbum(String url){
        Log.d(TAG, "loadNetAlbum: "+url);
        loadNetAlbum(Glide.with(getContext()).load(url));
    }
    private void loadNetAlbum(@NonNull RequestBuilder<Drawable> requestBuilder){
        requestBuilder
                .dontAnimate()
                .centerCrop()
                .error(R.drawable.icon_fate)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        RoundedBitmapDrawable drawable =
                                RoundedBitmapDrawableFactory.create(mApplication.getResources(),
                                                    PictureUtil.drawableToBitmap(resource));
                        drawable.setCornerRadius(20);
                        mAlbumBg = new WeakReference<>(drawable);
                        mBinding.dialogSongMoreIvCover.setImageDrawable(mAlbumBg.get());
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) { }
                });
    }
}
