package com.xwrl.mvvm.demo.view;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.xwrl.mvvm.demo.BaseActivity;
import com.xwrl.mvvm.demo.R;
import com.xwrl.mvvm.demo.databinding.ActivityEditSheetBinding;
import com.xwrl.mvvm.demo.model.helper.SQLiteChangeHelper;
import com.xwrl.mvvm.demo.model.helper.SongSheetHelper;
import com.xwrl.mvvm.demo.service.BaseMusicService;
import com.xwrl.mvvm.demo.util.ImmersiveStatusBarUtil;
import com.xwrl.mvvm.demo.view.fragment.EditFragment;
import com.xwrl.mvvm.demo.view.fragment.EditSheetFragment;

import java.util.List;
import java.util.Objects;

public class EditSheetActivity extends BaseActivity {

    private static final String TAG = "EditSheetActivity";
    public static final String FRAGMENT_EDIT = "EditFragment";
    public static final String FRAGMENT_EDIT_SHEET = "EditSheetFragment";

    private ActivityEditSheetBinding mEditSheetBinding;

    private EditFragment mEditFragment;
    private EditSheetFragment mMainFragment;
    private String mAlias;
    private boolean isShouldSave;
    private OpenListener mOpenListener;

    @Override
    protected MediaControllerCompat.Callback getControllerCallback() {
        return new MyMediaControllerCallback();
    }

    @Override
    protected MediaBrowserCompat.SubscriptionCallback getSubscriptionCallback() {
        return new MyMediaBrowserSubscriptionCallback();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImmersiveStatusBarUtil.transparentBar(this,true);
        mEditSheetBinding = DataBindingUtil.setContentView(this, R.layout.activity_edit_sheet);

        initView();
        isShouldSave = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseFragment();
        if (mAlias != null) mAlias = null;
        if (mOpenListener != null) mOpenListener = null;
        if (mEditSheetBinding != null) {
            mEditSheetBinding.unbind();
            mEditSheetBinding = null;
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
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (getVisibleFragment() == null || getVisibleFragment() instanceof EditSheetFragment){
                finish();
                overridePendingTransition(0, R.anim.push_out);
                return true;
            } else {
                initFragment(FRAGMENT_EDIT_SHEET,"");
                HideSoftInput();//关闭已经打开的软键盘
                return false;
            }
        }else return super.onKeyDown(keyCode, event);
    }

    private class MyMediaControllerCallback extends MediaControllerCompat.Callback{
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat playbackState) {
            super.onPlaybackStateChanged(playbackState);
            int state = playbackState.getState();
        }

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            super.onSessionEvent(event, extras);
            if (BaseMusicService.DYQL_CUSTOM_ACTION_TOAST.equals(event)){
                if (extras == null) { return; }

                String toastMsg = extras.getString("toastMsg", "");
                if (!TextUtils.isEmpty(toastMsg)){
                    //Toast.makeText(EditSheetActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                    EditSheetActivity.super.snackMsg(mEditSheetBinding.activityEditSheetUiRoot, toastMsg);
                }
            }
        }
    }

    private class MyMediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback{
        @Override
        public void onChildrenLoaded(@NonNull String parentId,
                                     @NonNull List<MediaBrowserCompat.MediaItem> children) {
            super.onChildrenLoaded(parentId, children);
        }

        @Override
        public void onError(@NonNull String parentId) { super.onError(parentId); }
    }

    private class OpenListener implements EditSheetFragment.OnOpenListener{
        @Override
        public void onOpen(String fragment,String alias, boolean isSave) {
            Log.d(TAG, "onOpen: "+fragment+", "+(fragment != null)+", "+FRAGMENT_EDIT.equals(fragment));
            if (fragment != null){
                initFragment(fragment,alias);
            }else {
                isShouldSave = isSave;
            }
        }
    }
    private void HideSoftInput(){
        InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (Objects.requireNonNull(manager).isActive() && getCurrentFocus() != null) {
            if (getCurrentFocus().getWindowToken() != null) {
                manager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }
    private Fragment getVisibleFragment(){
        FragmentManager manager = getSupportFragmentManager();
        List<Fragment> fragments = manager.getFragments();
        for (Fragment fragment : fragments){
            if (fragment != null && fragment.isVisible()) {
                return fragment;
            }
        }
        return null;
    }

    private void initView() {
        mEditSheetBinding.activityEditSheetUiRoot.setOnApplyWindowInsetsListener(this);

        mEditSheetBinding.activityEditSheetTvSave.setVisibility(View.VISIBLE);

        //保存点击事件
        mEditSheetBinding.activityEditSheetTvSave.setOnClickListener(v -> {
            if (getVisibleFragment() instanceof EditFragment) {
                String alias = mEditFragment.et_input.getText().toString();
                if(alias.equals(mAlias)){
                    Toast.makeText(EditSheetActivity.this,"新旧歌单名一致，歌单名称更改失败",Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!TextUtils.isEmpty(alias)) {
                    if (alias.length() > 16) Toast.makeText(EditSheetActivity.this,"歌单名称过长",Toast.LENGTH_SHORT).show();
                    else {
                        //在开始更新歌单总表数据前，需要先判断下新歌单别名是否跟其他歌单别名重名
                        //数据库操作
                        if (!TextUtils.isEmpty(mAlias) && SQLiteChangeHelper.isSameTable(getApplicationContext(),alias)) {
                            String s = SongSheetHelper
                                    .editSongSheet(EditSheetActivity.this, mAlias, alias,
                                            SQLiteChangeHelper.EDIT_SHEET_TYPE_ALIAS);
                            isShouldSave = !(s == null);
                            initFragment(FRAGMENT_EDIT_SHEET, alias);
                            Toast.makeText(this, s == null ? "修改失败" : s, Toast.LENGTH_SHORT).show();
                        }else Toast.makeText(EditSheetActivity.this,"输入的歌单名称与其他歌单名重名",Toast.LENGTH_SHORT).show();
                    }
                }else Toast.makeText(EditSheetActivity.this,"请输入歌单名称",Toast.LENGTH_SHORT).show();
            }else {
                if (isShouldSave){ //通过向服务端发送自定义事件，让歌单列表更新, 并结束本Activity回到上一级
                    MediaControllerCompat mediaController = getMediaControllerCompat();
                    Bundle extras = mediaController.getExtras();
                    extras = extras == null ? new Bundle() : extras;
                    mediaController.getTransportControls().sendCustomAction(
                            BaseMusicService.DYQL_CUSTOM_UPDATE_SHEET_LIST, extras);
                }
                finish();
            }
        });

        //返回点击事件
        mEditSheetBinding.activityEditSheetIvReturn.setOnClickListener(v -> {
            if (getVisibleFragment() == null || getVisibleFragment() instanceof EditSheetFragment){
                finish();
            } else {
                initFragment(FRAGMENT_EDIT_SHEET,"");
                HideSoftInput();//关闭已经打开的软键盘
            }
        });

        //初始化Fragment内的接口回调
        mOpenListener = new OpenListener();

        //初始化Fragment
        initFragment(FRAGMENT_EDIT_SHEET,"");
    }

    private void initFragment(String fragment,String sheetAlias) {
        if(fragment == null || sheetAlias == null) return;
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        switch (fragment) {
            case FRAGMENT_EDIT_SHEET:
                if (mMainFragment == null) {
                    if (!TextUtils.isEmpty(getIntent().getStringExtra("sheetAlias"))) {
                        //Log.d(TAG, "initFragment: "+mAlias);
                        mAlias = getIntent().getStringExtra("sheetAlias");
                        String albumPath = getIntent().getStringExtra("albumPath");
                        Log.d(TAG, "alias: "+mAlias+",Path: "+albumPath);
                        mMainFragment = new EditSheetFragment();
                        mMainFragment.setDatabase(mAlias,albumPath);
                    }
                    transaction.add(R.id.interface_fragment_edit,mMainFragment,"Edit_main_fragment");
                }else { mMainFragment.updateAlias(sheetAlias); }
                mMainFragment.setOnOpenListener(mOpenListener);
                hideFragment(transaction);
                if(mMainFragment != null) transaction.show(mMainFragment);

                mEditSheetBinding.activityEditSheetTvTips.setText(
                            getResources().getString(R.string.label_Dialog_edit_sheet));
                mEditSheetBinding.activityEditSheetTvSave.setText(R.string.label_Dialog_edit_sheet_save);
                ImmersiveStatusBarUtil.HideSoftInput(this);
                break;
            case FRAGMENT_EDIT:
                if (mEditFragment == null) {
                    mEditFragment = new EditFragment();
                    mEditFragment.setAlias(sheetAlias);
                    transaction.add(R.id.interface_fragment_edit,mEditFragment,"Edit_edit_fragment");
                }
                hideFragment(transaction);
                if(mEditFragment != null) transaction.show(mEditFragment);
                mEditSheetBinding.activityEditSheetTvTips.setText(getResources().getString(R.string.label_Dialog_edit_sheet_name1));
                mEditSheetBinding.activityEditSheetTvSave.setVisibility(View.VISIBLE);
                mEditSheetBinding.activityEditSheetTvSave.setText(R.string.label_Dialog_confirm);
                break;
        }
        //提交事务
        transaction.commit();
    }

    //隐藏所有Fragment
    private void hideFragment(FragmentTransaction transaction){
        if (mMainFragment != null) {
            transaction.hide(mMainFragment);
        }
        if (mEditFragment != null) {
            transaction.hide(mEditFragment);
        }
    }

    private void releaseFragment(){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if (mMainFragment != null) {
            transaction.remove(mMainFragment);
            mMainFragment = null;
        }
        if (mEditFragment != null) {
            transaction.remove(mEditFragment);
            mEditFragment = null;
        }
    }
}
