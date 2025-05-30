package com.xwrl.mvvm.demo.view.dialog;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.xwrl.mvvm.demo.R;
import com.xwrl.mvvm.demo.adapter.SongSheetAdapter;
import com.xwrl.mvvm.demo.bean.MusicBean;
import com.xwrl.mvvm.demo.databinding.DialogCollection2SheetListBinding;
import com.xwrl.mvvm.demo.model.AllSongSheetModel;
import com.xwrl.mvvm.demo.model.helper.MusicHelper;
import com.xwrl.mvvm.demo.model.helper.SongListHelper;
import com.xwrl.mvvm.demo.service.BaseMusicService;
import com.xwrl.mvvm.demo.util.DialogUtil;

import java.lang.ref.WeakReference;

public class CollectionToSheetDialog extends BottomSheetDialog {

    private DialogCollection2SheetListBinding mBinding;
    private SongSheetAdapter mSongSheetAdapter;
    private Application mApplication;
    private MusicBean mBean;
    private MediaMetadataCompat mMediaMetadata;
    private WeakReference<MediaControllerCompat> mMediaController;
    private int itemHeight;

    public CollectionToSheetDialog(@NonNull Context context,
                                   Application application,
                                   MediaMetadataCompat mediaMetadata,
                                   MediaControllerCompat mediaController) {
        super(context);
        mApplication = application;
        mBean = MusicHelper.getMusicBean(mediaMetadata);
        mMediaMetadata = mediaMetadata;
        mMediaController = new WeakReference<>(mediaController);
        //Log.e("dialog", "SyncMusicInfo: "+mBean.getTitle());
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
                R.layout.dialog_collection2_sheet_list, null, false);
        setContentView(mBinding.getRoot());
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindView();
        getWindow().setWindowAnimations(R.style.DialogNoFlashScreen);
        ConfirmHeight();
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

    private void bindView() {
        //初始化歌单列表

        mBinding.dialogCollectionListRv.setLayoutManager(new LinearLayoutManager(mApplication));
        mSongSheetAdapter = new SongSheetAdapter(mApplication);
        mSongSheetAdapter.setInVisibleMoreIv(true);
        mBinding.dialogCollectionListRv.setAdapter(mSongSheetAdapter);

        new AllSongSheetModel().showLocalSheet(beans ->
                mSongSheetAdapter.setItems(beans),getContext().getApplicationContext());

        mSongSheetAdapter.setItemClickListener(new SongSheetAdapter.OnItemClickListener() {
            @Override
            public void ItemClickListener(SongSheetAdapter adapter, int position) {
                String alias = adapter.getItems().get(position).getTitle();
                Bundle bundle = SongListHelper.collectionSong(getContext(), alias, mBean);
                boolean isSuccess = bundle != null && bundle.getBoolean("isSuccessful", false);
                String toastMsg = isSuccess ? "已收藏歌曲至"+alias+"歌单！" : "收藏歌曲失败!";
                if (isSuccess){
                    //传入MediaController更新MusicService中的音乐列表集合
                    MediaControllerCompat mediaController = mMediaController.get();

                    if (mediaController != null ){
                        Bundle extras = mediaController.getExtras();
                        String playingSheetName = extras.getString(BaseMusicService.DYQL_CUSTOM_PLAYING_SHEETNAME),
                                curSheetViewName = extras.getString(BaseMusicService.DYQL_CUSTOM_CURRENT_SHEETNAME);

                        boolean isNeedTopSong = bundle.getBoolean("isNeedTopSong", false);

                        Log.e("dialog", "ItemClickListener: 收藏到的歌单："+ alias+
                                ", 正在播放的歌单："+ playingSheetName+",当前处于歌单："+curSheetViewName+
                                ", 添加歌曲 "+mBean.getTitle());

                        extras.putString("SongSheetName",playingSheetName);
                        extras.putString("CollectionSheetName",alias);


                        if (isNeedTopSong) { //单置顶
                            //将置顶歌曲的mediaId放进Bundle并传给服务端Server
                            extras.putString("TopSongMediaId", mBean.getMediaId());
                            extras.putBoolean("isNeedTopSong", true);

                            if (alias.equals(curSheetViewName)) { //当 收藏到的歌单 是 当前处于的歌单 时
                                toastMsg = "歌曲"+mBean.getTitle()+" 已置顶";
                            }else if (alias.equals(playingSheetName)) {  //当 收藏到的歌单 是 当前播放的歌单 时
                                toastMsg = "歌曲"+mBean.getTitle()+" 在播放列表中置顶";
                                //需要先移除再添加，最后再更新列表
                                mediaController.getTransportControls().sendCustomAction(
                                        BaseMusicService.DYQL_CUSTOM_ACTION_UPDATE_PLAYLIST,extras);

                            }else { toastMsg = "歌曲"+mBean.getTitle()+" 已置顶至"+alias+"歌单"; }
                        }else if (alias.equals(playingSheetName)){ //当 收藏到的歌单 是 当前播放的歌单 时, 添加并置顶

                            WeakReference<MediaDescriptionCompat> description = mBean.getDescriptionCompat();

                            mediaController.addQueueItem(description.get(),0);

                            //Log.d("dialog", "ItemClickListener: "+description.get());
                            description.clear();
                        }

                        mediaController.getTransportControls().sendCustomAction(
                                                BaseMusicService.DYQL_CUSTOM_ACTION_UPDATE,extras);
                    }
                }
                if (bundle != null && bundle.getBoolean("isSheetTopSong",false)){
                    toastMsg = "歌曲 "+mBean.getTitle()+" 已经置顶"+alias+"歌单列表了";
                }
                Toast.makeText(getContext(),toastMsg,Toast.LENGTH_SHORT).show();
                dismiss();
            }

            @Override
            public void ItemMoreClickListener(SongSheetAdapter adapter, int position) { }
        });
    }

    private void unBindView() {
        if (mBinding != null) {
            mBinding.unbind();
            mBinding = null;
        }
        if (mSongSheetAdapter != null) {
            mSongSheetAdapter.release();
            mSongSheetAdapter = null;
        }
        if (mApplication != null) mApplication = null;
        if (mMediaController != null) { mMediaController.clear(); mMediaController = null; }
        if (mBean != null) {
            mBean.release();
            mBean = null;
        }
        if (mMediaMetadata != null) { mMediaMetadata = null; }
    }

    private void ConfirmHeight(){
        //使用了ViewPager加载fragment之后则只能在onResume获取
        RecyclerView mSheetRv = mBinding.dialogCollectionListRv;
        ViewTreeObserver vto = mSheetRv.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (itemHeight > 0) {//（防止child个数为零）获取到了高度就停止监听
                            mSheetRv.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }//item高度初始化
                        if (mSheetRv.getHeight() > 0 && mSheetRv.getChildCount() > 0) {
                            int height = mSheetRv.getHeight()/mSheetRv.getChildCount();
                            if (itemHeight != height && itemHeight == 0) {
                                itemHeight = height;
                                if (mSheetRv.getChildCount() > 0 && itemHeight > 0) {
                                    ViewGroup.LayoutParams params = mSheetRv.getLayoutParams();
                                    if (mSheetRv.getChildCount() < 6)
                                        params.height = itemHeight * mSheetRv.getChildCount();
                                    else params.height = itemHeight * 6;
                                    mSheetRv.setLayoutParams(params);
                                }
                            }
                        }
                    }
                });
    }
}
