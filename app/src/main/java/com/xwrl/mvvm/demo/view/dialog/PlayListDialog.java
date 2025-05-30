package com.xwrl.mvvm.demo.view.dialog;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.view.LayoutInflater;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.xwrl.mvvm.demo.R;
import com.xwrl.mvvm.demo.adapter.MusicAdapter;
import com.xwrl.mvvm.demo.databinding.DialogPlayListBinding;
import com.xwrl.mvvm.demo.model.helper.MusicHelper;
import com.xwrl.mvvm.demo.service.manager.MediaPlayerManager;
import com.xwrl.mvvm.demo.util.DialogUtil;
import com.xwrl.mvvm.demo.util.StringUtil;

import java.lang.ref.SoftReference;

public class PlayListDialog extends BaseBottomSheetDialog {

    private static final String TAG = "MusicListDialog";
    private SoftReference<MediaControllerCompat> mMediaController;
    private DialogPlayListBinding mBinding;
    private MusicAdapter mMusicAdapter;
    private Application mApplication;

    @Override
    protected float getPeekHeightFractionOf() { return 1f; }

    @Override
    protected void onDialogLocationSet() {
        DialogUtil.onMiddleAndLowerDisplay(getWindow(),true);
    }

    public PlayListDialog(Context context,
                          Application application,
                          MediaControllerCompat mediaController) {
        super(context);
        this.mMediaController = new SoftReference<>(mediaController);
        this.mApplication = application;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.inflate(LayoutInflater.from(getContext()),
                R.layout.dialog_play_list, null, false);
        setContentView(mBinding.getRoot());
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindView();
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

    private void bindView() {

        if (mMediaController.get() == null) { return; }

        MediaControllerCompat mediaController = mMediaController.get();

        mBinding.dialogPlayListCountTips.setText(
                StringUtil.SheetCountTips(mediaController.getQueue().size()));
        //获取当前播放模式
        long mode = mediaController.getExtras()
                .getInt(MediaPlayerManager.DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE);
        int resMode;
        if (mode == 1) {
            mBinding.dialogPlayListTvPlayMode.setText("顺序播放");
            resMode = R.drawable.iv_playback_mode_order;
            mBinding.dialogPlayListIvPlayMode.setImageResource(resMode);
        }else if (mode == 2) {
            resMode = R.drawable.iv_playback_mode_random;
            mBinding.dialogPlayListTvPlayMode.setText("随机播放");
            mBinding.dialogPlayListIvPlayMode.setImageResource(resMode);
        }else if (mode == 3) {
            resMode = R.drawable.iv_playback_mode_repeat;
            mBinding.dialogPlayListTvPlayMode.setText("单曲循环");
            mBinding.dialogPlayListIvPlayMode.setImageResource(resMode);
        }
        //获取当前播放列表
        //初始化RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mBinding.dialogPlayListRv.setLayoutManager(layoutManager);
        mMusicAdapter = new MusicAdapter(mApplication);
        mMusicAdapter.setInVisibleMoreIv(true);

        mBinding.dialogPlayListRv.setAdapter(mMusicAdapter);
        mMusicAdapter.setQueueItems(mediaController);
        mBinding.dialogPlayListRv.post(() -> {if(mMusicAdapter.getCurrentPosition() > -1)
            mMusicAdapter.notifyItemChanged(mMusicAdapter.getCurrentPosition(),MusicAdapter.TAG_SELECT);
        });
        //在dialog数据初始化时，执行此方法不能获得当前播放位置,注意MediaId要对的上
        moveToPosition(layoutManager,mMusicAdapter);

        mMusicAdapter.setItemClickListener(new MusicAdapter.OnItemClickListener() {
            @Override
            public void ItemClickListener(MusicAdapter adapter, int position) {
                String mediaId = mMediaController.get().getMetadata()
                        .getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID),
                        currentMediaId = adapter.getItems().get(position).getMediaId();
                currentMediaId = currentMediaId == null ? "" : currentMediaId;

                if (!MusicHelper.isSameSong(mMediaController.get(), currentMediaId, mApplication)) {
                    //不是同一首歌
                    adapter.selectOrGone(position, MusicAdapter.TAG_SELECT);
                    mMediaController.get().getTransportControls().playFromMediaId(currentMediaId, null);
                }

                /*if (mediaId.equals(currentMediaId)) { playbackButton();
                } else {
                    adapter.selectOrGone(position, MusicAdapter.TAG_SELECT);
                    mMediaController.get().getTransportControls().playFromMediaId(currentMediaId, null);
                }*/
            }

            @Override
            public void ItemMoreClickListener(View v, int position) { }
        });
    }

    private void unBindView() {
        if (mMediaController != null) {
            mMediaController.clear();
            mMediaController = null;
        }

        if (mMusicAdapter != null){
            mMusicAdapter.release();
            mMusicAdapter = null;
        }

        if (mApplication != null) mApplication = null;
        if (mBinding != null) {
            mBinding.dialogPlayListIvBg.setBackground(null);
            mBinding.unbind();
            mBinding = null;
        }

    }

    private void playbackButton(){
        if (mMediaController.get() == null) return;
        MediaControllerCompat mediaController = mMediaController.get();

        MusicHelper.playbackButton(mediaController, getContext().getApplicationContext());
    }

    /**
     * {@link androidx.recyclerview.widget.RecyclerView} 默认显示的位置（跳转至position，其计数从0开始)）
     *  size 当前播放歌单内的所有歌曲的数目
     * offset item 项的开始边缘与 {@link androidx.recyclerview.widget.RecyclerView} 之间的距离(以像素为单位)，无特殊要求以0即可
     *  position {@link com.xwrl.mvvm.demo.service.MusicService}中当前播放的歌曲在List<MusicBean>中的item位置（其计数从0开始）
     *  歌单为 当前播放 时执行
     * */
    private void moveToPosition(LinearLayoutManager layoutManager, MusicAdapter adapter) {
        if (adapter == null || layoutManager == null) return;

        int size = adapter.getItemCount(), position = adapter.getCurrentPosition();
        //Log.d(TAG, "moveToPosition: "+size + " "+position);

        if (size < 6 || position < 0) return;

        if (position - 2 > 0) layoutManager.scrollToPositionWithOffset(position - 2,0);

        else if (position >= size - 4) mBinding.dialogPlayListRv.smoothScrollToPosition(size - 1);

    }
}
