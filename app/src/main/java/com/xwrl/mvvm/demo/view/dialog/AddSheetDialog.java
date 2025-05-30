package com.xwrl.mvvm.demo.view.dialog;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.xwrl.mvvm.demo.BaseActivity;
import com.xwrl.mvvm.demo.R;
import com.xwrl.mvvm.demo.custom.DyzRoundedBitmapDrawable;
import com.xwrl.mvvm.demo.databinding.DialogAddSheetBinding;
import com.xwrl.mvvm.demo.util.DialogUtil;
import com.xwrl.mvvm.demo.util.ImmersiveStatusBarUtil;

import java.lang.ref.WeakReference;

public class AddSheetDialog extends BottomSheetDialog {

    private DialogAddSheetBinding mBinding;
    private WeakReference<Drawable> mBgDrawable;
    private OnEnsureListener mOnEnsureListener;
    public interface OnEnsureListener{
        void onEnSureSongSheetName(Bundle extra);
    }
    public void setOnEnsureListener(OnEnsureListener onEnsureListener) {
        this.mOnEnsureListener = onEnsureListener;
    }

    public AddSheetDialog(@NonNull Context context) {
        super(context);
    }
    public AddSheetDialog(@NonNull Context context, int theme) {
        super(context, theme);
    }

    protected AddSheetDialog(@NonNull Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        DialogUtil.BottomShow(this, true);
        //设置为展开状态 behavior不会为空
        BottomSheetBehavior<FrameLayout> behavior = getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        //设置展开高度h
        //behavior.setPeekHeight(***);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.inflate(LayoutInflater.from(getContext()),
                R.layout.dialog_add_sheet, null, false);
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
        unBind();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    private void bindView() {

        //生成背景图片
        WeakReference<Bitmap> reference = new WeakReference<>(
                BitmapFactory.decodeResource(
                        getContext().getResources(), R.drawable.dialog_bg_sea));
        DyzRoundedBitmapDrawable drawable = DyzRoundedBitmapDrawable.create(
                getContext().getResources(), reference.get(), 12);
        drawable.setCornerRadius(60);
        reference.clear();

        mBgDrawable = new WeakReference<>(drawable);
        mBinding.dialogAddSheetBg.setImageDrawable(mBgDrawable.get());
        //点击事件
        mBinding.dialogAddSheetCancel.setOnClickListener(v -> cancel());
        mBinding.dialogAddSheetConfirm.setOnClickListener(v -> {
            Editable editable = mBinding.dialogAddSheetEt.getText();
            if (!TextUtils.isEmpty(editable)){
                String s = editable.toString();
                if (s.contains(BaseActivity.ACTIVITY_DEFAULT_SHEET_NAME)) {
                    Toast.makeText(getContext(),"输入的歌单名不被允许",Toast.LENGTH_SHORT).show();
                } else if (editable.toString().length() <= 32){
                    Bundle bundle = new Bundle();
                    bundle.putString("AddSheetAlias",mBinding.dialogAddSheetEt.getText().toString());
                    mOnEnsureListener.onEnSureSongSheetName(bundle);
                    dismiss();
                } else Toast.makeText(getContext(),"歌单名长度超过上限",Toast.LENGTH_SHORT).show();
            }else Toast.makeText(getContext(),"请输入新建歌单名称",Toast.LENGTH_SHORT).show();
        });

        //拉出软键盘
        mBinding.dialogAddSheetEt.post(() -> {
            mBinding.dialogAddSheetEt.requestFocus();
            ImmersiveStatusBarUtil.ShowSoftInput(getContext(),mBinding.dialogAddSheetEt);
        });
    }

    private void unBind(){
        if (mBgDrawable != null) {
            mBgDrawable.clear();
            mBgDrawable = null;
        }
        if (mBinding != null) {
            mBinding.dialogAddSheetBg.setImageDrawable(null);
            mBinding.unbind();
            mBinding = null;
        }
        if (mOnEnsureListener != null) mOnEnsureListener = null;
    }
}