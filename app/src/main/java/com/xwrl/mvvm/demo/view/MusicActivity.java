package com.xwrl.mvvm.demo.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.xwrl.mvvm.demo.BaseActivity;
import com.xwrl.mvvm.demo.R;
import com.xwrl.mvvm.demo.adapter.MusicAdapter;
import com.xwrl.mvvm.demo.databinding.ActivityMusicBinding;
import com.xwrl.mvvm.demo.model.helper.MusicHelper;
import com.xwrl.mvvm.demo.service.BaseMusicService;
import com.xwrl.mvvm.demo.util.ImmersiveStatusBarUtil;
import com.xwrl.mvvm.demo.view.dialog.PlayListDialog;
import com.xwrl.mvvm.demo.view.dialog.SongMoreDialog;
import com.xwrl.mvvm.demo.viewmodel.MusicViewModel;

import java.util.List;
import java.util.Timer;

public class MusicActivity extends BaseActivity<MusicViewModel>{

    private static final String TAG = "MusicActivity";

    private ActivityMusicBinding mMusicBinding;
    private MusicViewModel mMusicViewModel;
    private MusicAdapter mMusicAdapter;
    private MyAdapterItemClickListener mItemClickListener;
    private boolean isItemChange;
    private Timer mTimer;

    @Override
    protected MediaControllerCompat.Callback getControllerCallback() { return new MyMediaControllerCallback(); }
    @Override
    protected MediaBrowserCompat.SubscriptionCallback getSubscriptionCallback() { return new MyMediaBrowserSubscriptionCallback(); }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMusicBinding = DataBindingUtil.setContentView(this, R.layout.activity_music);
        mMusicViewModel = new MusicViewModel(getApplication());
        mMusicBinding.setMusicInfo(mMusicViewModel);

        initView();

    }

    @Override
    protected void onStart() {
        super.onStart();
        UpdateProgressBar();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        StopProgressBar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mItemClickListener != null) { mItemClickListener = null; }
        if (mMusicAdapter != null) {
            mMusicAdapter.release();
            mMusicAdapter = null;
        }
        if (mMusicViewModel != null) { mMusicViewModel = null; }
        if (mMusicBinding != null) {
            mMusicBinding.unbind();
            mMusicBinding = null;
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //当打开软键盘时，再按返回按键，则由软键盘本身消耗本次按键事件，此处不会收到该按键回调
        if (keyCode == KeyEvent.KEYCODE_BACK) return returnClick();
        return super.onKeyDown(keyCode, event);
    }

    private class MyMediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback{
        @Override
        public void onChildrenLoaded(@NonNull String parentId,
                                     @NonNull List<MediaItem> children) {
            super.onChildrenLoaded(parentId, children);

            if (isItemChange) {
                isItemChange = false;
                MusicActivity.this.updateMusicList(mMusicAdapter.getItems(), false); return;
            }
            Log.d(TAG, "onChildrenLoaded: ");
            mMusicAdapter.setItems(children);
            activityOnChildrenLoad(mMusicViewModel, mMusicBinding.mainActivityIvPlayLoading, children);
        }

        @Override
        public void onError(@NonNull String parentId) { super.onError(parentId); }
    }

    private class MyMediaControllerCallback extends MediaControllerCompat.Callback{
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            Log.w(TAG, "onMetadataChanged() returned: ");
            mMusicViewModel.SyncMusicInformation();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat playbackState) {
            super.onPlaybackStateChanged(playbackState);
            int state = playbackState.getState();
            //Log.w(TAG, "onPlaybackStateChanged: "+state);
            mMusicViewModel.setPlaybackState(state);
            playbackStateChanged(playbackState, mMusicBinding.mainActivityIvPlayLoading);
        }

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            super.onSessionEvent(event, extras);
            switch (event) {
                case BaseMusicService.DYQL_CUSTOM_ACTION_UPDATE:
                    String playingSheetName = mItemClickListener.getPlayingSheetName(extras),
                            alias = extras.getString("CollectionSheetName",""),
                            curSheetViewName = getSheetName();

                    Log.e(TAG, "歌单更新: 收藏到的歌单："+ alias+
                            ", 正在播放的歌单："+ playingSheetName+",当前处于歌单："+curSheetViewName);

                    isItemChange = (extras.getBoolean("isNeedTopSong", false) &&
                            curSheetViewName.equals(playingSheetName) && curSheetViewName.equals(alias) &&
                            !curSheetViewName.contains(BaseActivity.ACTIVITY_DEFAULT_SHEET_NAME));
                    if (!isItemChange){ return;}

                    MusicActivity.this.subscribe();//向服务端订阅数据，更新列表

                    mMusicAdapter.onItemRangeToTop(
                            mMusicAdapter.getMediaIdPosition(extras.getString("TopSongMediaId",null)));
                    break;
            }
        }
    }

    private void initView() {
        mMusicBinding.musicActivityUiRoot.setOnApplyWindowInsetsListener(this);

        mMusicBinding.musicActivityIvReturn.setOnClickListener(v -> returnClick());

        mMusicBinding.mainActivityBottomLayout.setOnClickListener(v -> openActivity(0, null));
        mMusicBinding.mainActivityBottomIvList.setOnClickListener(v -> openActivity(5, null));

        mMusicBinding.musicActivityIvSearch.setOnClickListener(this::showEditText);

        mMusicBinding.mainActivityBottomProgressBar.setOnClickListener(v -> mMusicViewModel.playbackButton());
        //初始化唱片旋转动画
        super.initAnimation(mMusicBinding.mainActivityBottomIvAlbum);
        //初始化RecyclerView
        mMusicBinding.musicActivityRvMusic.setLayoutManager(new LinearLayoutManager(getApplication()));
        mMusicAdapter = new MusicAdapter(getApplication());

        mMusicBinding.musicActivityRvMusic.setAdapter(mMusicAdapter);
        mItemClickListener = new MyAdapterItemClickListener();
        mMusicAdapter.setItemClickListener(mItemClickListener);
        //设置深色模式适配的颜色
        int color = super.getViewColor();
        mMusicBinding.mainActivityBottomIvList.getDrawable().setTint(color);
        mMusicBinding.mainActivityBottomProgressBar.setProgressColor(color);
        //加载歌曲列表
        //获取当前歌单别名
        super.setSheetName(getIntent().getStringExtra(ACTIVITY_SONG_SHEET_NAME));
    }
    /*
    * 音乐列表点击事件回调
    * */
    private class MyAdapterItemClickListener implements MusicAdapter.OnItemClickListener{
        @Override
        public void ItemClickListener(MusicAdapter adapter, int position) {
            MediaControllerCompat mediaController =
                    MediaControllerCompat.getMediaController(MusicActivity.this);
            MediaMetadataCompat metadata = adapter.getItemMetadata(position);
            MediaItem mediaItem = adapter.getItems().get(position);
            String mediaId = mediaController.getMetadata()
                    .getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID),
                    currentMediaId = mediaItem.getMediaId();
            if (mediaId.equals(currentMediaId)) {
                MusicActivity.this.mMusicViewModel.playbackButton();
            } else {
                //不是同一首歌
                if (isUpdateList()){
                    Log.d(TAG, "ItemClickListener: 更新数据源");
                    MusicActivity.this.updateMusicList(adapter.getItems(), true);
                    mediaController.getTransportControls()
                            .playFromUri(mediaItem.getDescription().getMediaUri(),
                                                        MusicHelper.getMusicBundle(metadata));
                }else {
                    mediaController.getTransportControls().playFromMediaId(currentMediaId, null);
                }

            }

            Log.d(TAG, "ItemClickListener: 点击了 "+mediaId+", "+currentMediaId);
        }

        @Override
        public void ItemMoreClickListener(View v, int position) {
            Log.d(TAG, "ItemMoreClickListener: 点击了更多 "+position);
            Bundle bundle = new Bundle();
            bundle.putInt("ClickMusicItemPosition",position);
            openActivity(1,bundle);
        }

        public String getPlayingSheetName(@NonNull Bundle bundle){
            return bundle.getString(BaseMusicService.DYQL_CUSTOM_PLAYING_SHEETNAME,
                    ACTIVITY_DEFAULT_SHEET_NAME);
        }
        public boolean isUpdateList(){
            MediaControllerCompat mediaController = getMediaControllerCompat();
            List<MediaSessionCompat.QueueItem> queue = mediaController.getQueue();
            int queueSize = queue == null ? 0 : queue.size();
            return queueSize != mMusicAdapter.getItemCount() ||
                    !getSheetName().equals(getPlayingSheetName(mediaController.getExtras()));
        }
    }

    private void UpdateProgressBar() {
        if (mTimer != null) { return; }

        mTimer = new Timer();
        mTimer.schedule(mMusicViewModel.getCircleBarTask(),300,300);
    }

    private void StopProgressBar() {
        if (mTimer != null) {
            mTimer.purge();
            mTimer.cancel();
            mTimer = null;
        }
    }

    private boolean returnClick(){
        if (mMusicViewModel != null && mMusicViewModel.isShowSearchView()){
            mMusicBinding.musicActivityTopEdit
                    .removeTextChangedListener(mMusicViewModel.getTextListener());
            //失去焦点前调用才有效
            mMusicBinding.musicActivityRvSearchMusic.setAdapter(null);
            ImmersiveStatusBarUtil.HideSoftInput(this);
            mMusicBinding.musicActivityTopEdit.clearFocus();
            mMusicViewModel.setSearchGroupVisible(false);
        }else {
            this.finish();
            //overridePendingTransition(0,R.anim.push_out);
        }
        Log.d(TAG, "returnClick: ");
        return true;
    }

    private void showEditText(View v){
        mMusicBinding.musicActivityTopEdit.addTextChangedListener(mMusicViewModel.getTextListener());
        //Log.d(TAG, "showEditText: 音乐列表是否显示 "+mMusicBinding.musicActivityRvMusic.getVisibility());
        mMusicViewModel.setSearchGroupVisible(true);
        mMusicBinding.musicActivityTopEdit.setVisibility(View.VISIBLE);
        mMusicBinding.musicActivityTopEdit.requestFocus();
        //EditText处于显示状态且获取焦点后调用软键盘才有效
        ImmersiveStatusBarUtil.ShowSoftInput(getApplication(),mMusicBinding.musicActivityTopEdit);
        //初始化RecyclerView
        mMusicBinding.musicActivityRvSearchMusic.setLayoutManager(new LinearLayoutManager(getApplication()));
        MusicAdapter adapter = new MusicAdapter(getApplication());
        adapter.setSheetMediaItems(mMusicAdapter.getItems());
        mMusicBinding.musicActivityRvSearchMusic.setAdapter(adapter);
        mMusicViewModel.setAdapter(adapter);
        adapter.setItemClickListener(mItemClickListener);
        //if (mMusicAdapter != null) mMusicAdapter.setItems()
    }
    /**
     * 当前歌单是收藏歌单时，如有收藏或取消收藏的动作，则更新列表
     * */
    private void updateMusicList(MediaMetadataCompat metadata) {
        MusicActivity.this.subscribe();//向服务端订阅数据，更新列表
        MediaControllerCompat mediaController = getMediaControllerCompat();
        if (mediaController != null && mediaController.getExtras() != null &&
                MusicActivity.this.getSheetName().equals(
                        mediaController.getExtras().getString("CurSheetName"))){
            mediaController.removeQueueItem(metadata.getDescription());
        }
    }


    private void openActivity(int mode, Bundle extra){
        MediaControllerCompat controller = getMediaControllerCompat();
        if (mode == 0) {
            startActivity(new Intent(MusicActivity.this, SongLrcActivity.class));
            overridePendingTransition(R.anim.push_in,0);
        }else if (mode == 1){
            MediaMetadataCompat metadata;
            int clickPosition;
            if (extra == null || mMusicAdapter == null){
                clickPosition = 0;
                Toast.makeText(this,"未有额外信息，请检查",Toast.LENGTH_SHORT).show();
                metadata = controller.getMetadata();
            }else {
                clickPosition = extra.getInt("ClickMusicItemPosition");
                metadata = mMusicAdapter.getItemMetadata(clickPosition);
            }
            SongMoreDialog songMoreDialog = new SongMoreDialog(
                    this,getApplication(),
                    metadata,
                    getMediaControllerCompat(),
                    getSheetName());
            songMoreDialog.show();
            songMoreDialog.setMediaController(controller);
            songMoreDialog.setOnConfirmListener(result -> {
                if(result) updateMusicList(metadata);
                mMusicAdapter.onItemRangeRemoved(clickPosition);
                isItemChange = true;
            });
            if (getSheetName().equals(ACTIVITY_DEFAULT_SHEET_NAME)) songMoreDialog.setInVisible();
        } else if (mode == 5) { //打开歌曲列表Dialog
            PlayListDialog playListDialog = new PlayListDialog(
                    this,getApplication(), controller);
            playListDialog.show();
        }

    }
}
