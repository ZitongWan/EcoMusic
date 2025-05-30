package com.xwrl.mvvm.demo.view.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaControllerCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.xwrl.mvvm.demo.R;
import com.xwrl.mvvm.demo.model.AllSongSheetModel;
import com.xwrl.mvvm.demo.model.SongSheet;
import com.xwrl.mvvm.demo.model.helper.SQLiteChangeHelper;
import com.xwrl.mvvm.demo.model.helper.SongSheetHelper;
import com.xwrl.mvvm.demo.service.BaseMusicService;
import com.xwrl.mvvm.demo.util.HttpUtil;
import com.xwrl.mvvm.demo.util.PictureUtil;

public class EditSheetFragment extends BaseFragment {
    private static final String TAG = "EditSheetFragment";
    private String mAlias,mAlbumPath;

    private ImageView iv_album;
    private TextView tv_sheet;
    private SongSheet mSongSheet;
    private MediaControllerCompat mMediaController;
    private ClickListener mClickListener;
/*接口回调 显示哪个Fragment*/
    private OnOpenListener mOnOpenListener;
    public interface OnOpenListener{
        void onOpen(String fragment,String alias, boolean isShouldSave);
    }
    public void setOnOpenListener(OnOpenListener onOpenListener) {
        mOnOpenListener = onOpenListener;
    }

    public EditSheetFragment() {
    }

    public void setMediaController(MediaControllerCompat mediaController) {
        this.mMediaController = mediaController;
    }

    public void setDatabase(String alias, String albumPath) {
        this.mAlias = alias;
        this.mAlbumPath = albumPath;
    }

    public void updateAlias(String alias){
        if (alias != null && !TextUtils.isEmpty(alias) &&
                tv_sheet != null){ tv_sheet.setText(alias); }
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_sheet, container, false);
        tv_sheet = view.findViewById(R.id.fragment_edit_tv_sheet_name);
        iv_album = view.findViewById(R.id.fragment_edit_sheet_iv_cover);
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        mSongSheet = new AllSongSheetModel();//向上转型
        mClickListener = new ClickListener();
        iv_album.setOnClickListener(mClickListener);
        //Log.d("EditSheetFragment", "onStart: ");
        showSheetInformation();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: "+this);
    }

    @Override
    public void onStop() {
        super.onStop();
        //Log.d("EditSheetFragment", "onStop: ");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: ");
        if (mSongSheet != null) mSongSheet = null;
        if (mClickListener != null) mClickListener = null;
        if (mOnOpenListener != null) mOnOpenListener = null;
        if (iv_album != null) {
            iv_album.setImageBitmap(null);
            iv_album = null;
        }
        if (tv_sheet != null) tv_sheet = null;
        if (mAlias != null) mAlias = null;
        if (mAlbumPath != null) mAlbumPath = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("1111111111", "requestCode = "+requestCode+"data 不为空 "+(data!=null));
        //接收相册选择返回的结果
        if (requestCode == 200 && data != null){
            String newAlbumPath = PictureUtil.getRealPath(getContext(),data.getData());
            if (newAlbumPath == null) {
                Toast.makeText(getActivity(),"未获得相册图片",Toast.LENGTH_SHORT).show();
            }else {
                mAlbumPath = newAlbumPath;

                String s = SongSheetHelper.editSongSheet(EditSheetFragment.this.getContext(),
                                            mAlias, mAlbumPath, SQLiteChangeHelper.EDIT_SHEET_TYPE_COVER);

                Toast.makeText(getActivity(),s == null ? "更换失败" : "歌单封面已更换",Toast.LENGTH_SHORT).show();

                GlideLoading(newAlbumPath);
                if (s != null && mMediaController != null){
                    Bundle extras = mMediaController.getExtras();
                    extras = extras == null ? new Bundle() : extras;
                    mMediaController.getTransportControls().sendCustomAction(
                            BaseMusicService.DYQL_CUSTOM_UPDATE_SHEET_LIST, extras);
                }
                if (mOnOpenListener != null) { //设置需要保存更新状态
                    mOnOpenListener.onOpen(null, null, !(s == null));
                }

            }
            //Log.d(TAG, "onActivityResult: "+ picturePath);
            //Toast.makeText(getActivity(),""+UserPath,Toast.LENGTH_SHORT).show();
        }else Toast.makeText(getActivity(),"未获得相册图片",Toast.LENGTH_SHORT).show();

    }

    private void showSheetInformation(){
        if(!TextUtils.isEmpty(mAlias) && tv_sheet != null) {
            if (!tv_sheet.getText().toString().equals(mAlias)) {
                tv_sheet.setText(mAlias);
                tv_sheet.setOnClickListener(mClickListener);
            }else return;
        }

        if (mAlbumPath != null && mSongSheet != null) {//只会加载本地图片或者缓存图片
            if (mAlbumPath.matches("^[0-9]+(.[0-9]+)?$") &&
                    HttpUtil.FileExists(HttpUtil.getLocalPathPictures(mAlbumPath+".jpg")))
                mAlbumPath = HttpUtil.getLocalPathPictures(mAlbumPath+".jpg");

            //Log.d(TAG, "showSheetInformation: "+(mAlbumPath == null || TextUtils.isEmpty(mAlbumPath)));

            if (HttpUtil.FileExists(mAlbumPath) || mAlbumPath == null || TextUtils.isEmpty(mAlbumPath)) {
                if (mAlbumPath == null || TextUtils.isEmpty(mAlbumPath) || mAlbumPath.contains(".mp3")){
                    mSongSheet.showAlbumBitmap(bitmap -> updateCover(bitmap.get())
                                                                    , mAlbumPath, getResources());
                }else GlideLoading(mAlbumPath);

            }//else Toast.makeText(getContext(),"无法加载网络图片",Toast.LENGTH_SHORT).show();
        }
    }
    private class ClickListener implements View.OnClickListener{
        @SuppressLint("NonConstantResourceId")
        @Override
        public void onClick(View v) {
            if(HttpUtil.isFastClick()) return;
            switch (v.getId()) {
                case R.id.fragment_edit_tv_sheet_name:
                    mOnOpenListener.onOpen("EditFragment",mAlias, false);
                    break;

                case R.id.fragment_edit_sheet_iv_cover:
                    Intent intent = new Intent(Intent.ACTION_PICK, null);
                    intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*");

                    //*已过时，有更简便的方法。由于不太熟悉，就暂用此种方式了
                    startActivityForResult(intent,200);//单选

                    break;
            }
        }
    }

    private void GlideLoading(String absPath) {
        Glide.with(EditSheetFragment.this).load(absPath)
                .dontAnimate()
                .centerCrop()
                .placeholder(R.drawable.iv_play_loading)
                .error(R.drawable.icon_fate)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource,
                                                @Nullable Transition<? super Drawable> transition) {
                        updateCover(PictureUtil.drawableToBitmap(resource));
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) { }
                });
    }

    private void updateCover(Bitmap bitmap){

        if (bitmap == null){
            bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.icon_fate);
        }

        RoundedBitmapDrawable drawable =
                RoundedBitmapDrawableFactory.create(getResources(), bitmap);

        drawable.setCornerRadius(20);
        iv_album.setImageDrawable(drawable);
    }
}
