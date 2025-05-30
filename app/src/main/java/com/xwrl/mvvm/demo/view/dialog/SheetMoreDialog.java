package com.xwrl.mvvm.demo.view.dialog;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.xwrl.mvvm.demo.R;
import com.xwrl.mvvm.demo.bean.SongSheetBean;
import com.xwrl.mvvm.demo.databinding.DialogSheetMoreBinding;
import com.xwrl.mvvm.demo.model.helper.SongSheetHelper;
import com.xwrl.mvvm.demo.util.DialogUtil;
import com.xwrl.mvvm.demo.view.EditSheetActivity;


/**
 * @since : 2021/12/29
 * 作用: 歌单更多会话
 */
public class SheetMoreDialog extends BottomSheetDialog {

    private static final String TAG = "SheetMoreDialog";
    private DialogSheetMoreBinding mBinding;
    private SongSheetBean mBean;
    private AddSheetDialog.OnEnsureListener mOnEnsureListener;

    public SheetMoreDialog(@NonNull Context context,
                           SongSheetBean bean) {
        super(context);
        this.mBean = bean;
        Log.d(TAG, "SheetMoreDialog: "+bean.getTitle());
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
                R.layout.dialog_sheet_more, null, false);
        setContentView(mBinding.getRoot());
        mBinding.setSheetInfo(mBean);
    }

    @Override
    protected void onStart() {
        super.onStart();
        syncInfo();
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

    public void setOnEnsureListener(AddSheetDialog.OnEnsureListener onEnsureListener) {
        this.mOnEnsureListener = onEnsureListener;
    }

    public void setInVisible(){
        if (mBinding != null) {
            mBinding.dialogSheetMoreRvDelete.setVisibility(View.GONE);
            mBinding.dialogSheetMoreRvEdit.setVisibility(View.GONE);
        }
    }

    private void unBindView(){
        if (mBinding != null) {
            mBinding.unbind();
            mBinding = null;
        }
        if (mBean != null){ mBean = null; }
        if (mOnEnsureListener != null){ mOnEnsureListener = null; }
    }

    private void syncInfo(){
        //删除 点击事件
        mBinding.dialogSheetMoreRvDelete.setOnClickListener(v -> {
            if (mBean.getTitle().equals("最近很喜欢")) {
                Toast.makeText(getContext(),"此歌单不能删除",Toast.LENGTH_SHORT).show();
            }else {
                String confirmTips = "您真的要删除" + mBean.getTitle() + "歌单吗？";
                ConfirmDialog confirmDialog = new ConfirmDialog(getContext(),
                                                                R.style.DialogTheme,
                        confirmTips,
                                                                false);
                confirmDialog.show();
                confirmDialog.setOnConfirmListener(result -> {
                    if (!result) { return;}
                    SongSheetHelper.deleteSongSheet(getContext(),mBean.getTitle());
                    if (mOnEnsureListener != null) {
                        mOnEnsureListener.onEnSureSongSheetName(null);
                        dismiss();
                    }else Toast.makeText(getContext(),"传参有误，请检查",Toast.LENGTH_SHORT).show();
                });
            }
        });
        //编辑歌单 点击事件
        mBinding.dialogSheetMoreRvEdit.setOnClickListener(v -> {
            //打开EditSheetActivity
            Context applicationContext = getContext().getApplicationContext();

            if (applicationContext != null) {
                Intent intent = new Intent(applicationContext, EditSheetActivity.class);
                //由于不是从Activity跳转至新Activity，所以要加个NewTask flag，不然会抛异常
                //别忘记在清单文件中声明EditSheetActivity
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //传入歌单名称（别名）和封面图片绝对路径（分为图片格式或.mp3）
                intent.putExtra("sheetAlias",mBean.getTitle());
                intent.putExtra("albumPath",mBean.getFirstAlbumPath());
                applicationContext.startActivity(intent);

                dismiss();//至此关闭本Dialog，不然不好更新修改后的歌单别名
            }else Toast.makeText(getContext(),"跳转歌单编辑页面失败！",Toast.LENGTH_SHORT).show();

        });
    }
}
