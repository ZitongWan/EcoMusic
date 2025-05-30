package com.xwrl.mvvm.demo.adapter;

import android.app.Application;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.support.v4.media.MediaMetadataCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.databinding.ObservableArrayList;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.xwrl.mvvm.demo.R;
import com.xwrl.mvvm.demo.bean.SongSheetBean;
import com.xwrl.mvvm.demo.databinding.ItemSongSheetBinding;
import com.xwrl.mvvm.demo.model.helper.MusicHelper;
import com.xwrl.mvvm.demo.model.helper.SQLiteChangeHelper;
import com.xwrl.mvvm.demo.model.helper.SongSheetHelper;
import com.xwrl.mvvm.demo.util.PictureUtil;
import com.xwrl.mvvm.demo.util.StringUtil;

import java.lang.ref.WeakReference;
import java.util.List;

public class SongSheetAdapter extends BaseBindingAdapter<SongSheetBean, ItemSongSheetBinding> {

    private static final String TAG = "SongSheetAdapter";
    private Application mApplication;
    private boolean isInVisibleMoreIv;

    private OnItemClickListener mItemClickListener;

    public interface OnItemClickListener{
        void ItemClickListener(SongSheetAdapter adapter, int position);
        void ItemMoreClickListener(SongSheetAdapter adapter, int position);
    }

    public void setItemClickListener(OnItemClickListener itemClickListener) {
        this.mItemClickListener = itemClickListener;
    }

    public SongSheetAdapter(Application context) {
        super(context);
        this.mApplication = context;
    }

    @Override
    protected int getLayoutResId(int ViewType) {
        return R.layout.item_song_sheet;
    }

    @Override
    protected void onBindItem(ItemSongSheetBinding binding, SongSheetBean bean, int position) {
        String firstAlbumPath = bean.getFirstAlbumPath();
        String alias = bean.getTitle();

        binding.setSongSheetInfo(bean);
        binding.itemSongSheetAlbum.setImageDrawable(null);

        Log.d(TAG, "onBindItem: "+firstAlbumPath);
        if (firstAlbumPath == null || firstAlbumPath.contains(".mp3")){
            //加载元媒体专辑图片
            binding.itemSongSheetAlbum.setImageDrawable(getSheetDrawable(firstAlbumPath, alias));
        }else { GlideLoading(binding, firstAlbumPath);}


        binding.itemSongSheetLayout.setOnClickListener(v ->
                mItemClickListener.ItemClickListener(this,position));
        binding.itemSongSheetMore.setOnClickListener(v ->
                mItemClickListener.ItemMoreClickListener(this,position));

        if (isInVisibleMoreIv) binding.itemSongSheetMore.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onUnBindItem(ItemSongSheetBinding binding) {
        binding.getSongSheetInfo().release();
        binding.itemSongSheetAlbum.setImageDrawable(null);
        binding.unbind();
    }

    @Override
    protected void onExtraBindItem(ItemSongSheetBinding binding, int position,
                                   @NonNull List<Object> payloads) { }

    public void release(){
        super.release();
        if (mItemClickListener != null) { mItemClickListener = null; }
        if (mApplication != null) { mApplication = null; }
        //releaseThisMediaItems();
    }

    public void setInVisibleMoreIv(boolean inVisibleMoreIv) {
        this.isInVisibleMoreIv = inVisibleMoreIv;
    }

    private RoundedBitmapDrawable getSheetDrawable(String musicFilePath, String alias){
        WeakReference<Bitmap> bitmap;
        Resources resources = mApplication.getResources();
        if (musicFilePath == null || TextUtils.isEmpty(musicFilePath)) {
            bitmap = new WeakReference<>(BitmapFactory.decodeResource(resources, R.drawable.icon_fate));
        }else {
            //Log.d(TAG, "getSheetDrawable: "+MusicHelper.FileExists(musicFilePath));
            if (!MusicHelper.FileExists(musicFilePath)) {
                //Log.d(TAG, "getSheetDrawable: 尝试从本地媒体数据库中获得当前歌曲文件的绝对路径");
                MediaMetadataCompat metadata = MusicHelper.getMusicAbsPath(
                                                mApplication.getContentResolver(), musicFilePath);
                String musicPathNew = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI);

                if (MusicHelper.FileExists(musicPathNew)) {
                    musicFilePath = musicPathNew;
                    //更新歌单总表的专辑图片地址
                    SongSheetHelper.editSongSheet(mApplication,alias,musicPathNew,
                                                    SQLiteChangeHelper.EDIT_SHEET_TYPE_COVER);
                }else getSheetDrawable(null,alias);
            }
            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
            metadataRetriever.setDataSource(musicFilePath);
            byte[] picture = metadataRetriever.getEmbeddedPicture();
            metadataRetriever.release();

            bitmap = picture != null ?
                    new WeakReference<>(BitmapFactory.decodeByteArray(picture, 0, picture.length)) :
                    new WeakReference<>(BitmapFactory.decodeResource(resources, R.drawable.icon_fate));
        }
        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(resources,
                PictureUtil.zoomImg(bitmap.get(),80,80));
        drawable.setCornerRadius(20);

        bitmap.clear();

        return drawable;
    }

    private void GlideLoading(ItemSongSheetBinding binding, String absPath) {
        if (StringUtil.isOnlyDigit(absPath)) { absPath += ".jpg"; }

        if (!MusicHelper.FileExists(absPath)){//当图片不存在时
            WeakReference<Bitmap> bitmapW = new WeakReference<>(
                    BitmapFactory.decodeResource(mApplication.getResources(), R.drawable.icon_fate));
            RoundedBitmapDrawable drawable =
                    RoundedBitmapDrawableFactory.create(mApplication.getResources(), bitmapW.get());
            drawable.setCornerRadius(20);
            binding.itemSongSheetAlbum.setImageDrawable(drawable);
        }else {
            Glide.with(binding.getRoot()).load(absPath)
                    .dontAnimate()
                    .centerCrop()
                    .placeholder(R.drawable.iv_play_loading)
                    .error(R.drawable.icon_fate)
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            Log.d(TAG, "onResourceReady: ");
                            RoundedBitmapDrawable drawable =
                                    RoundedBitmapDrawableFactory.create(mApplication.getResources(), PictureUtil.drawableToBitmap(resource));
                            drawable.setCornerRadius(20);
                            binding.itemSongSheetAlbum.setImageDrawable(drawable);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            Log.d(TAG, "onLoadCleared: ");
                        }
                    });
        }
    }

    public void updateItems(List<SongSheetBean> newItems) {
        //Log.d(TAG, "updateItems: "+newItems.size());
        if (isUpdateSheet(newItems)) super.setItems(newItems);
    }

    /**
     * 什么情况更新歌单以及歌单封面
     * */
    private boolean isUpdateSheet(List<SongSheetBean> bean) {
        ObservableArrayList<SongSheetBean> beans = getItems();

        if (bean == null || bean.size() <= 0) return false;
        if (beans == null || beans.size() != bean.size()) return true;

        for (int i = 0; i < bean.size(); i++) {
            if (!bean.get(i).getTitle().equals(beans.get(i).getTitle()) ||
                    !bean.get(i).getFirstAlbumPath().equals(beans.get(i).getFirstAlbumPath()) ||
                    !bean.get(i).getCount().equals(beans.get(i).getCount())) {
                return true;
            }
        }
        return false;
    }
}
