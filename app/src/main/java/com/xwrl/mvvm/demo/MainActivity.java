package com.xwrl.mvvm.demo;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.xwrl.mvvm.demo.adapter.SongSheetAdapter;
import com.xwrl.mvvm.demo.bean.SongSheetBean;
import com.xwrl.mvvm.demo.bean.UserBean;
import com.xwrl.mvvm.demo.custom.GifView;
import com.xwrl.mvvm.demo.databinding.ActivityMainBinding;
import com.xwrl.mvvm.demo.model.AllSongSheetModel;
import com.xwrl.mvvm.demo.model.CrawerActivity;
import com.xwrl.mvvm.demo.model.SongSheet;
import com.xwrl.mvvm.demo.model.helper.SQLiteChangeHelper;
import com.xwrl.mvvm.demo.model.helper.SQLiteUserHelper;
import com.xwrl.mvvm.demo.model.helper.SongSheetHelper;
import com.xwrl.mvvm.demo.service.BaseMusicService;
import com.xwrl.mvvm.demo.service.MusicService;
import com.xwrl.mvvm.demo.service.manager.LastMetaManager;
import com.xwrl.mvvm.demo.util.HttpUtil;
import com.xwrl.mvvm.demo.util.ImmersiveStatusBarUtil;
import com.xwrl.mvvm.demo.util.PermissionUtil;
import com.xwrl.mvvm.demo.util.PictureUtil;
import com.xwrl.mvvm.demo.util.StringUtil;
import com.xwrl.mvvm.demo.view.MusicActivity;
import com.xwrl.mvvm.demo.view.SongLrcActivity;
import com.xwrl.mvvm.demo.view.dialog.AddSheetDialog;
import com.xwrl.mvvm.demo.view.dialog.ConfirmDialog;
import com.xwrl.mvvm.demo.view.dialog.PlayListDialog;
import com.xwrl.mvvm.demo.view.dialog.SheetMoreDialog;
import com.xwrl.mvvm.demo.viewmodel.MusicViewModel;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

public class MainActivity extends BaseActivity<MusicViewModel> {

    private static final String TAG = "MainActivity";

    private ActivityMainBinding mMainBinding;
    private MusicViewModel mMusicViewModel;
    private Timer mTimer;
    private Intent mIntentMusic;
    /*
    * 歌单管理
    * */
    private SongSheetAdapter mSongSheetAdapter;
    private SongSheet mSongSheet;
    private DialogConfirmListener mDialogConfirmListener;
    private SongSheetItemClickListener mItemClickListener;
    /*
    * 用户信息设置
    * */
    private boolean isStopButNoPermission, isStranger;
    private String mReceivePath;
    private UserBean mCurrentBean;
    //选取图片 ActivityResultLauncher 必须得先初始化，不然会抛异常。实际上用的还startActivityForResult();
    private final ActivityResultLauncher<String> mLauncherAlbum = registerForActivityResult(
            new ActivityResultContracts.GetContent(), result -> {
                mReceivePath = result.toString();
                //result为选取的图片的Uri
                loadBitmap(result);
            }
    );

    @Override
    protected MediaControllerCompat.Callback getControllerCallback() { return new MyMediaControllerCallback(); }
    @Override
    protected MediaBrowserCompat.SubscriptionCallback getSubscriptionCallback() { return new MyMediaBrowserSubscriptionCallback(); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //if (PermissionUtil.IsPermissionNotObtained(this)) { PermissionUtil.getStorage(this);}
        super.onCreate(savedInstanceState);
        mMainBinding = DataBindingUtil.setContentView(this,R.layout.activity_main);
        mMusicViewModel = new MusicViewModel(getApplication());
        mMainBinding.setUserInfo(mMusicViewModel);
        super.setBackToDesktop();
        initView();//先初始化控件
        openActivity(6,null); //检查权限的获取

        mIntentMusic = new Intent(this, MusicService.class);
        this.startService(mIntentMusic);

        ImageView imageView = findViewById(R.id.activity_main_iv_edit_user);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CrawerActivity.class);
                startActivity(intent);
            }
        });

        new GifView(this,1);
    }

    @Override
    protected void onStart() {
        super.onStart();
        UpdateProgressBar();
        if (isStopButNoPermission) { openActivity(6,null); }//检查权限的获取
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSongSheet();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        StopProgressBar();
        isStopButNoPermission = PermissionUtil.IsPermissionNotObtained(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIntentMusic != null) { mIntentMusic = null; }
        if (mMusicViewModel != null) { mMusicViewModel = null; }
        if (mMainBinding != null) {
            mMainBinding.unbind();
            mMainBinding = null;
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        System.out.println("获取权限ing");
        if (grantResults[0] == PermissionUtil.REQUEST_PERMISSION_CODE) {
            if (PermissionUtil.IsPermissionNotObtained(this)) {
                openActivity(6, null); //检查权限获取
                System.out.println("未获取");
            } else {
                System.out.println("获取到");
                Log.w(TAG, "onRequestPermissionsResult: 已获取读写权限");
                //添加列表
                super.subscribe();
                HttpUtil.createPictureMyDIR();//创建文件夹
                updateData();//更新歌单与用户信息
            }
        }else {
            openActivity(6, null); //检查权限获取
        }
    }
    /**
     * 重新登录触发此回调
     * */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        updateUser(false, intent);
    }

    private class MyMediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback{
        @Override
        public void onChildrenLoaded(@NonNull String parentId,
                                     @NonNull List<MediaBrowserCompat.MediaItem> children) {
            super.onChildrenLoaded(parentId, children);
            Log.d(TAG, "onChildrenLoaded: parentId "+parentId+", MediaItem数量 "+children.size());
            activityOnChildrenLoad(mMusicViewModel,
                                    mMainBinding.mainActivityIvPlayLoading,
                                    children);
            updateMusicList(children,false);
            mMusicViewModel.setPhoneRefresh(mRefreshRateMax);
            //！！！少更新样式状态
            mMusicViewModel.setCustomStyle(MediaControllerCompat.getMediaController(MainActivity.this)
                    .getMetadata().getLong(BaseMusicService.DYQL_NOTIFICATION_STYLE) == 0
            );
        }

        @Override
        public void onError(@NonNull String parentId) { super.onError(parentId); }
    }

    private class MyMediaControllerCallback extends MediaControllerCompat.Callback{
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            mMusicViewModel.SyncMusicInformation();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat playbackState) {
            super.onPlaybackStateChanged(playbackState);
            //Log.w(TAG, "onPlaybackStateChanged: "+state);
            mMusicViewModel.setPlaybackState(playbackState.getState());
            playbackStateChanged(playbackState,
                    mMainBinding.mainActivityIvPlayLoading);
        }

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            super.onSessionEvent(event, extras);
            switch (event) {
                case BaseMusicService.DYQL_CUSTOM_UPDATE_SHEET_LIST: //更新歌单列表
                    updateSongSheet();
                    break;
            }
        }
    }

    private void initView(){
        mMainBinding.activityMainUiRoot.setOnApplyWindowInsetsListener(this);
        //等信息更新后再设置回调
        mMainBinding.activityMainNotificationStyleSwitch.setOnCheckedChangeListener(
                mMusicViewModel.getCheckedListener()
        );
        //点击进入 歌曲列表 MusicActivity
        mMainBinding.activityMainGridLayout.setOnClickListener(v -> openActivity(1,null));
        //点击进入 歌曲展示 SongLrcActivity
        mMainBinding.mainActivityBottomLayout.setOnClickListener(v -> openActivity(0, null));
        mMainBinding.mainActivityBottomProgressBar.setOnClickListener(v ->
                                                                mMusicViewModel.playbackButton());
        //点击 选取相册图片 为 用户头像
        mMainBinding.dialogEditUserIvCover.setOnClickListener(view -> {
            //隐藏软键盘
            ImmersiveStatusBarUtil.HideSoftInput(this);
            //打开相册 选择照片
            mLauncherAlbum.launch("image/*");
        });
        //点击 设置空白处 隐藏软键盘
        mMainBinding.dialogEditSettingsTips.setOnClickListener(v ->
                                            ImmersiveStatusBarUtil.HideSoftInput(this));
        //点击 保存 更新用户信息至数据库和SharedPreferences缓存
        mMainBinding.dialogEditUserTvSave.setOnClickListener(v ->{
            String userAlias = mMainBinding.dialogEditUserEtName.getText().toString();
            String userLabel = mMainBinding.dialogEditUserEtLabel.getText().toString();
            boolean saveResult = SQLiteUserHelper.saveUserInfo(userAlias, userLabel,
                                                                mReceivePath, isStranger,
                                                                getApplication(), mCurrentBean);
            if (saveResult) {
                ImmersiveStatusBarUtil.HideSoftInput(this);
                if (!TextUtils.isEmpty(userAlias)) { mMainBinding.activityMainTvUser.setText(userAlias); }
                if (!TextUtils.isEmpty(userLabel)) { mMainBinding.activityMainTvLabel.setText(userLabel); }
                if (mReceivePath != null) { loadBitmap(mReceivePath); }


                mMainBinding.dialogEditUserEtName.setText("");
                mMainBinding.dialogEditUserEtLabel.setText("");
                mCurrentBean.updateUserInfo(userAlias, mReceivePath, userLabel);
            }
        });
        //点击退出登录
        mMainBinding.dialogEditUserTvExit.setOnClickListener(v -> openActivity(7, null));

        super.initAnimation(mMainBinding.mainActivityBottomIvAlbum);
        mMainBinding.activityMainTopLayout.setOnClickListener(v ->
                Toast.makeText(this,"打开APP菜单设置",Toast.LENGTH_SHORT).show());

        mMainBinding.dialogEditUserTvExit.setOnLongClickListener(v -> {
                StopProgressBar();
                this.stopService(mIntentMusic);
                finish();
                return true;
            }
        );
        //设置深色模式适配的颜色
        int color = super.getViewColor();;
        mMainBinding.mainActivityBottomIvList.getDrawable().setTint(color);
        mMainBinding.mainActivityBottomProgressBar.setProgressColor(color);
        //歌单列表初始化
        mDialogConfirmListener = new DialogConfirmListener();
        mItemClickListener = new SongSheetItemClickListener();

        mMainBinding.activityMainRvSheetList.setLayoutManager(new LinearLayoutManager(getApplication()));
        mSongSheet = new AllSongSheetModel();
        mSongSheetAdapter = new SongSheetAdapter(getApplication());
        mSongSheetAdapter.setItemClickListener(mItemClickListener);
        mMainBinding.activityMainRvSheetList.setAdapter(mSongSheetAdapter);

        mMainBinding.activityMainIvSheetAdd.setOnClickListener(v -> openActivity(8, null));
        //
        mMainBinding.mainActivityBottomIvList.setOnClickListener(v -> openActivity(5,null));
    }
    /**
     * 更新歌单与用户信息
     * 需要在确认获取权限后
     * */
    private void updateData(){
        //初始化 更新用户信息
        updateUser(false, getIntent());
        //更新歌单
        updateSongSheet();
    }

    private void UpdateProgressBar() {
        if (mTimer != null) { return; }

        mTimer = new Timer();
        mTimer.schedule(mMusicViewModel.getCircleBarTask(),300,300);
    }

    private void StopProgressBar(){
        if (mTimer != null) {
            mTimer.purge();
            mTimer.cancel();
            mTimer = null;
        }
    }

    /**
     * 打开Activity或者Dialog
     * */
    private void openActivity(int mode,Bundle extra){
        if (mode == 0) { //打开歌词Activity
            startActivity(new Intent(MainActivity.this, SongLrcActivity.class));
            overridePendingTransition(R.anim.push_in,0);
        }else if (mode == 1){ //打开列表Activity, 开始实现歌单管理后，需要通过Intent将要显示的歌单名传过去。
            Intent intent = new Intent(this, MusicActivity.class);
            intent.putExtra(BaseActivity.ACTIVITY_SONG_SHEET_NAME,extra == null ?
                    BaseActivity.ACTIVITY_DEFAULT_SHEET_NAME : extra.getString("SheetName"));
            startActivity(intent);
        } else if (mode == 5) { //打开歌曲列表Activity
            PlayListDialog playListDialog = new PlayListDialog(
                    this,getApplication(), getMediaControllerCompat());
            playListDialog.show();
        } else if (mode == 6){
            if (PermissionUtil.IsPermissionNotObtained(this)) {

                String content = getString(R.string.label_Dialog_get_tips2);

                ConfirmDialog confirmDialog = new ConfirmDialog(this, R.style.DialogTheme,
                        StringUtil.getPermissionTips(getApplication(), content), true);

                confirmDialog.setCantCancel();
                confirmDialog.show();

                confirmDialog.setOnConfirmListener(result -> {
                    if (result) {
                        boolean SkipToSettings = ActivityCompat.shouldShowRequestPermissionRationale(
                                this, Manifest.permission.READ_EXTERNAL_STORAGE);
                        //Toast.makeText(this,"shouldShowRequestPermissionRationale："+SkipToSettings,Toast.LENGTH_SHORT).show();
                        if (SkipToSettings) {
                            //跳转至设置中手动同意
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        }else {
                            PermissionUtil.getStorage(this);
                        }
                    }else { //不同意退出APP
                        finish();
                        android.os.Process.killProcess(android.os.Process.myPid());  //获取PID
                        System.exit(0);   //常规java、c#的标准退出法，返回值为0代表正常退出
                    }
                });
            }else {
                HttpUtil.createPictureMyDIR(); //创建缓存文件夹
                updateData();//更新歌单和用户数据
            }
        } else if (mode == 7){
            String msg = SQLiteChangeHelper.isStrangerLogin(getApplication()) ?
                                                "您确定要回到登录页面吗？" : "您确定要注销当前账户吗？";
            ConfirmDialog dialog = new ConfirmDialog(this,
                                                        R.style.DialogTheme,
                                                        msg,
                                                        false);

            dialog.setOnConfirmListener(result -> {
                if (result) {
                    //先执行回到登录页
                    startActivity(new Intent(this, WelcomeActivity.class));
                    //overridePendingTransition(null,0);
                    //将SharedPreferences中保存的数据库名更换为游客数据库名，防止一些意外
                    LastMetaManager lastMetaManager = new LastMetaManager(getApplication());
                    lastMetaManager.saveUserDBName(SQLiteChangeHelper.UserDBName);
                    lastMetaManager.onDestroy();

                }
            });
            dialog.setCantCancel();
            dialog.show();
        }else if (mode == 8){
            AddSheetDialog addSheetDialog = new AddSheetDialog(this);
            addSheetDialog.show();
            addSheetDialog.setOnEnsureListener(mDialogConfirmListener);
        }else if (mode == 9){
            if (mDialogConfirmListener == null) mDialogConfirmListener = new DialogConfirmListener();
            if (extra == null || mSongSheetAdapter == null){
                Toast.makeText(this,"未有额外信息，请检查",Toast.LENGTH_SHORT).show();
            }else {
                int position = extra.getInt("ClickMusicItemPosition");
                SheetMoreDialog sheetMoreDialog = new SheetMoreDialog(this,
                        mSongSheetAdapter.getItems().get(position));
                sheetMoreDialog.show();
                sheetMoreDialog.setOnEnsureListener(mDialogConfirmListener);
                if (position == 0) sheetMoreDialog.setInVisible();
            }
        }
    }

    /**
     * 通过接口回调 异步更新 歌单{@link androidx.recyclerview.widget.RecyclerView}列表
     * */
    private void updateSongSheet(){
        //通知歌单列表设配器 歌单数据更新
        if (mSongSheet != null) {
            mSongSheet.showLocalSheet(MainActivity.this::findSongSheets, getApplicationContext());
        }
    }
    private void findSongSheets(List<SongSheetBean> beans){
        if (beans == null) beans = new ArrayList<>();
        mSongSheetAdapter.updateItems(beans);
        mMusicViewModel.setMySheets(StringUtil.SheetTips(beans.size()));
    }

    private class SongSheetItemClickListener implements SongSheetAdapter.OnItemClickListener{
        @Override
        public void ItemClickListener(SongSheetAdapter adapter, int position) {
            Log.d(TAG, "ItemClickListener: 点击了 "+adapter.getItems().get(position).getTitle()+" 歌单");
            String alias = adapter.getItems().get(position).getTitle();
            Bundle bundle = new Bundle();
            bundle.putString("SheetName",alias);
            openActivity(1,bundle);
        }

        @Override
        public void ItemMoreClickListener(SongSheetAdapter adapter, int position) {
            Log.d(TAG, "ItemMoreClickListener: 点击了歌单更多");
            Bundle bundle = new Bundle();
            bundle.putInt("ClickMusicItemPosition",position);
            openActivity(9,bundle);
        }
    }

    private class DialogConfirmListener implements AddSheetDialog.OnEnsureListener{

        @Override
        public void onEnSureSongSheetName(Bundle bundle) {
            Application application = getApplication();

            if (bundle != null){
                String alias = bundle.getString("AddSheetAlias");
                Log.d(TAG, "onEnSureSongSheetName: "+alias);
                if (alias == null || SQLiteChangeHelper.isSameTable(application, alias)) {
                    SongSheetHelper.createNewSheet(application ,alias);//歌单别名不重复就创建歌单
                }else Toast.makeText(MainActivity.this,"歌单名重复，请重新输入",Toast.LENGTH_SHORT).show();
            }
            //通知歌单列表设配器 歌单数据更新
            mSongSheet.showLocalSheet(MainActivity.this::findSongSheets, application);
        }
    }

    /**
     * 更新用户信息 onStart()、onNenInstance()
     * 需要通过Intent获取{@link WelcomeActivity}传过来的用户数据库名
     * onNenInstance() 是用户退出后再登录(进入MainActivity)才会调用的回调，
     * 因为MainActivity启动模式我设置的 SingleTask，所以{@link Activity#getIntent()} 不会更改。
     * */
    private void updateUser(boolean isSave, Intent intent){
        if (mMainBinding == null){ return; }
        StringBuilder user = new StringBuilder();
        user = intent == null ? user.append("") : user.append(intent.getStringExtra("user"));
        LastMetaManager lastMetaManager = new LastMetaManager(getApplication());
        isStranger = intent == null || intent.getBooleanExtra("isStranger", true);

        Log.d(TAG, "updateUser: 是否是保存后更新："+isSave+", 用户名："+user);

        if (isSave && TextUtils.isEmpty(user.toString())){
            //更新游客的用户信息
            updateUser(lastMetaManager.getStrangerInfo());
        }else {
            String password = intent == null ? "" : intent.getStringExtra("password");
            Log.d(TAG, "updateUser: "+user+", password: "+password);
            UserBean userBean = TextUtils.isEmpty(user.toString()) ? null :
                    SQLiteUserHelper.queryUserInfo(getApplication(), user.toString(), password);
            updateUser(userBean);

            if (userBean != null) {
                lastMetaManager.saveUserInformation(userBean.getAlias(),
                        userBean.getSignature(),
                        userBean.getAlbumPath(),
                        userBean.getUser());
            }
        }
        lastMetaManager.onDestroy();
        Log.d(TAG, "updateUser: mCurrentBean 是否为空 "+(mCurrentBean == null));
    }

    private void updateUser(UserBean userBean){
        String user, albumPath, signature;
        Log.d(TAG, "updateUser: 是否为空 "+(userBean == null));
        if (userBean == null) {
            LastMetaManager lastMetaManager = new LastMetaManager(getApplication());
            mCurrentBean = lastMetaManager.getStrangerInfo();
            lastMetaManager.onDestroy();

            user = mCurrentBean.getAlias();
            signature = mCurrentBean.getSignature();
            albumPath = mCurrentBean.getAlbumPath();
        }else {
            user = userBean.getAlias();
            albumPath = userBean.getAlbumPath();
            signature = userBean.getSignature();
            mCurrentBean = userBean;
        }
        mMainBinding.activityMainTvUser.setText(user);
        mMainBinding.activityMainTvLabel.setText(signature);
        //加载用户头像
        loadBitmap(albumPath);
        Log.d(TAG, "updateUser: 用户名："+user +
                                                "，个性签名："+signature +
                                                "，头像路径："+albumPath);
    }
    private void loadBitmap(Uri imageUri){
        loadBitmap(Glide.with(this).load(imageUri));
    }
    private void loadBitmap(String imageUri){
        if (imageUri.equals("default")) {
            loadBitmap(Glide.with(this).load(R.drawable.ic_test));
        }else {
            loadBitmap(Glide.with(this).load(imageUri));
        }
    }

    private void loadBitmap(@NonNull RequestBuilder<Drawable> requestBuilder){
        requestBuilder.error(R.drawable.ic_test)
                .dontAnimate().
                centerCrop()
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource,
                                                @Nullable Transition<? super Drawable> transition) {

                        WeakReference<Bitmap> bitmap = new WeakReference<>(PictureUtil.drawableToBitmap(resource));
                        LayerDrawable userIconDrawable = PictureUtil.createUserIconDrawable(
                                                                                getApplication(),
                                                                                bitmap.get(),
                                                                                120, dpToPx(64));
                        Log.d(TAG, "onResourceReady: 加载用户头像");
                        mMainBinding.dialogEditUserIvCover.setImageDrawable(userIconDrawable);
                        mMainBinding.activityMainIvUser.setImageDrawable(userIconDrawable);
                        bitmap.clear();
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) { }
                });
    }
}