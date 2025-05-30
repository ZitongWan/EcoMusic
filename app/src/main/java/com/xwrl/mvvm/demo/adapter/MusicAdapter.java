package com.xwrl.mvvm.demo.adapter;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.ObservableArrayList;

import com.xwrl.mvvm.demo.R;
import com.xwrl.mvvm.demo.bean.MusicBean;
import com.xwrl.mvvm.demo.databinding.ItemMusicListBinding;
import com.xwrl.mvvm.demo.model.helper.MusicHelper;
import com.xwrl.mvvm.demo.util.PictureUtil;
import com.xwrl.mvvm.demo.util.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MusicAdapter extends BaseBindingAdapter<MediaItem, ItemMusicListBinding>{

    private static final String TAG = "MusicAdapter";
    public static final String TAG_SELECT = "⚡", TAG_UNKNOWN = "unknown";
    private ObservableArrayList<MediaItem> mSearchMediaItems, mSheetMediaItems;
    private OnItemClickListener mItemClickListener;
    private boolean isInVisibleMoreIv;
    private int currentPosition;
    private final int paddingLeft, dp7;

    public interface OnItemClickListener{
        void ItemClickListener(MusicAdapter adapter, int position);
        void ItemMoreClickListener(View v, int position);
    }

    public void setItemClickListener(OnItemClickListener itemClickListener) {
        this.mItemClickListener = itemClickListener;
    }

    public MusicAdapter(Application context) {
        super(context);
        currentPosition = -1; //初始化
        paddingLeft = PictureUtil.dpToPx(13,context.getResources());
        dp7 = PictureUtil.dpToPx(7,context.getResources());
    }

    @Override
    protected int getLayoutResId(int ViewType) {
        return R.layout.item_music_list;
    }

    @Override
    protected void onBindItem(ItemMusicListBinding binding, MediaItem item, int position) {
        int number = position;
        String artist = Objects.requireNonNull(item.getDescription().getSubtitle()).toString(),
                album = Objects.requireNonNull(item.getDescription().getDescription()).toString();
        MusicBean bean = new MusicBean(isInVisibleMoreIv ? TAG_SELECT : String.valueOf(++number),
                Objects.requireNonNull(item.getDescription().getTitle()).toString(),
                artist,album,
                Objects.requireNonNull(item.getDescription().getMediaUri()).toString(),
                Objects.requireNonNull(item.getDescription().getMediaUri()).toString(),
                100000);
        binding.setMusicInfo(bean);

        if (isInVisibleMoreIv) {
            binding.itemLocalMusicNumber.setVisibility(View.GONE);
            binding.itemLocalMusicMore.setVisibility(View.INVISIBLE);
            binding.itemLocalMusicSong.setPadding(paddingLeft,0,0,0);
            binding.itemLocalMusicSingerAndAlbum.setPadding(paddingLeft,0,0,0);
        }
        //防止滑动列表时，播放中的列被回收从而重新加载 无播放中提示的问题
        if (currentPosition >= 0 && currentPosition == position){ showTips(true,binding); }

        if (mItemClickListener == null) return;
        binding.itemMusicListLayout.setOnClickListener(v -> mItemClickListener.ItemClickListener(this,position));
        binding.itemLocalMusicMore.setOnClickListener(v -> mItemClickListener.ItemMoreClickListener(v,position));
    }

    @Override
    protected void onUnBindItem(ItemMusicListBinding binding) {
        binding.getMusicInfo().release();
        binding.unbind();
    }

    @Override
    protected void onExtraBindItem(ItemMusicListBinding binding, int queue, @NonNull List<Object> payloads) {
        for (Object payload : payloads){
            showTips(String.valueOf(payload).equals(TAG_SELECT),binding);
        }
    }

    public void release(){
        super.release();
        if (mItemClickListener != null) { mItemClickListener = null; }
        releaseThisMediaItems();
    }
    public void releaseThisMediaItems(){
        if (mSearchMediaItems != null) {
            if (mSearchMediaItems.size() > 0) { mSearchMediaItems.clear(); }
            mSearchMediaItems = null;
        }
        if (mSheetMediaItems != null) {
            if (mSheetMediaItems.size() > 0) { mSheetMediaItems.clear(); }
            mSheetMediaItems = null;
        }
    }

    public void setSheetMediaItems(ObservableArrayList<MediaItem> sheetMediaItems) {
        if (sheetMediaItems == null || sheetMediaItems.size() == 0) return;

        this.mSheetMediaItems = new ObservableArrayList<>();
        this.mSheetMediaItems.addAll(sheetMediaItems);

    }

    public void setQueueItems(MediaControllerCompat mediaController){
        if (mediaController == null) return;
        //Log.w(TAG, "setQueueItems1: " + currentPosition );
        MediaDescriptionCompat description = mediaController.getMetadata().getDescription();
        List<MediaSessionCompat.QueueItem> items = mediaController.getQueue();
        if (items == null) return;
        //Log.w(TAG, "setQueueItems2: " + currentPosition +", "+curMediaId);
        List<MediaItem> mediaItems = new ArrayList<>();
        if (items.size() == 0) super.setItems(mediaItems);

        int i = 0;
        //确定播放位置 和 转化为MediaItem
        for (MediaSessionCompat.QueueItem item : items){
            MediaItem mediaItem = new MediaItem(
                    item.getDescription(), MediaItem.FLAG_PLAYABLE);
            mediaItems.add(mediaItem);

            Log.d(TAG, "setQueueItems4: "+item.getDescription().getMediaId()+", "+i);
            if (description != null && item.getDescription() != null
                    && description.toString().equals(item.getDescription().toString())) {
                currentPosition = i;
            }
            i++;
        }
        Log.w(TAG, "setQueueItems3: 确定正播放位置" + currentPosition );

        items.clear();

        super.setItems(mediaItems);
    }


    private void showTips(boolean isCur, ItemMusicListBinding binding){
        binding.itemLocalMusicNumber.setVisibility(isCur ? View.VISIBLE : View.GONE);
        int dex = isCur ? this.paddingLeft - dp7 : this.paddingLeft;

        binding.itemLocalMusicSong.setPadding(dex,0,0,0);
        binding.itemLocalMusicSingerAndAlbum.setPadding(dex,0,0,0);
    }

    public void setInVisibleMoreIv(boolean inVisibleMoreIv) {
        this.isInVisibleMoreIv = inVisibleMoreIv;
    }

    public int getCurrentPosition() {
        //return currentPosition == -1 ? : currentPosition;
        return currentPosition;
    }

    /** 局部更新item
     * @param position 点击位置
     * @param payloads 额外信息 {@link Object}
     * {@link MusicAdapter#onExtraBindItem(ItemMusicListBinding, int, List)} 中判断局部更新item
     * <p>
     * 注：此方法必须在{@link androidx.fragment.app.Fragment#onResume}时调用才执行*/
    public void selectOrGone(int position,String payloads){

        //Log.w(TAG, "selectOrGone: ");
        if (currentPosition != -1) notifyItemChanged(currentPosition,"gone");

        this.currentPosition = position;

        notifyItemChanged(currentPosition,payloads);
    }


    //搜索集合返回
    public void searchMediaItems(String s){

        if (mSheetMediaItems == null || mSheetMediaItems.size() == 0 ||
                s == null || TextUtils.isEmpty(s)) return;

        if (mSearchMediaItems == null) mSearchMediaItems = new ObservableArrayList<>();
        if (mSearchMediaItems.size() > 0) mSearchMediaItems.clear();

        for (MediaItem m: mSheetMediaItems) {
            //判断每一项歌名、歌手和专辑名是否包含搜索内容或者其字母大小写
            String description = m.getDescription().toString().toLowerCase();
            if (description.contains(s)) {
                mSearchMediaItems.add(m);
            }
        }
        Log.d(TAG, "searchMediaItems: "+mSearchMediaItems.size());
        super.setItems(mSearchMediaItems);
    }

    //根据列表位置获得该列表的音乐元数据
    public MediaMetadataCompat getItemMetadata(int position){
        MediaItem mediaItem = getItems().get(position);

        String path = getMediaItemString(mediaItem.getDescription().getMediaUri());

        long duration = StringUtil.isOnlyDigit(path) ? 0L : getDuration(path);

        return getItemMetadataBuilder(position)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                .build();
    }

    /**
     * 通过 {@link MediaMetadataRetriever} 读取音频文件元数据获取播放时长。
     * 注：释放资源 {@link MediaMetadataRetriever#close()}。
     * @param musicFilePath 音频文件的绝对地址。
     * @return {@link Long} 长整型类型的 音频文件 播放时长。
     * */
    private long getDuration(String musicFilePath){
        if (musicFilePath == null || TextUtils.isEmpty(musicFilePath) ||
                !new File(musicFilePath).exists() || !musicFilePath.contains(".mp3")) return 0;

        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        metadataRetriever.setDataSource(musicFilePath);
        String duration = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        //Log.d(TAG, "getDuration: "+duration);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            metadataRetriever.release();//释放资源
        }else metadataRetriever.close();


        return Long.parseLong(duration);
    }

    /**
     * 缺少 duration 的 Metadata.Builder
     * 适用于 歌单列表 的 在线音乐
     * */
    public MediaMetadataCompat.Builder getItemMetadataBuilder(int position){
        MediaItem mediaItem = getItems().get(position);

        String title = getMediaItemString(mediaItem.getDescription().getTitle()),
                artist = getMediaItemString(mediaItem.getDescription().getSubtitle()),
                album = getMediaItemString(mediaItem.getDescription().getDescription()),
                albumPath = getMediaItemString(mediaItem.getDescription().getIconUri()),
                path = getMediaItemString(mediaItem.getDescription().getMediaUri());

        Bitmap cover = null;

        if (MusicHelper.FileExists(path)) {


            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
            metadataRetriever.setDataSource(path);
            byte[] picture = metadataRetriever.getEmbeddedPicture();
            if (Build.VERSION.SDK_INT >= 29) {
                metadataRetriever.close();
            }else {
                metadataRetriever.release();//SDK > 26 才有close，且close与release是一样的
            }

            if (picture != null) {
                cover = BitmapFactory.decodeByteArray(picture,0, picture.length);}

        } else if(!StringUtil.isOnlyDigit(path)){
            Toast.makeText(context,"该歌曲列表的文件路径不存在，请点击重新播放后再进入此界面或者删除此记录！ "+path,Toast.LENGTH_LONG).show();
        }

        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaItem.getMediaId())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, "")
                .putString(
                        MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, albumPath)
                .putString(
                        MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, albumPath)
                .putString(
                        MediaMetadataCompat.METADATA_KEY_MEDIA_URI, path)
                .putBitmap(
                        MediaMetadataCompat.METADATA_KEY_ALBUM_ART,cover);
    }

    private String getMediaItemString(Object x){
        return x == null ? TAG_UNKNOWN : x.toString();
    }

    public int getMediaIdPosition(String mediaId){
        int position = -1;
        if (mediaId == null || TextUtils.isEmpty(mediaId)) { return position; }

        //position = -1 符合 return 0;
        //position = 0 符合 return 1;
        //position = 1 符合 return 2;
        for (MediaItem mediaItem : getItems()){
            //Log.d(TAG, "getMediaIdPosition: "+mediaId+", "+mediaItem.getMediaId());
            if (mediaId.equals(mediaItem.getMediaId())) return ++position;
            else position++;
        }

        return -1;
    }

}
