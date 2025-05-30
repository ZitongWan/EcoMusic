package com.xwrl.mvvm.demo.view.dialog;

import android.content.Context;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.xwrl.mvvm.demo.R;

public abstract class BaseBottomSheetDialog extends BottomSheetDialog {

    public BaseBottomSheetDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        onDialogLocationSet();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initBottomSheetDialog();
    }

    protected abstract float getPeekHeightFractionOf();
    protected abstract void onDialogLocationSet();

    private void initBottomSheetDialog(){
        FrameLayout bottomSheet = getDelegate().findViewById(R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            //此高度受
            int peekHeight = getPeekHeight(getPeekHeightFractionOf());
            /*//获取根部局的LayoutParams对象
            CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) bottomSheet.getLayoutParams();
            layoutParams.height = peekHeight;
            //修改弹窗的最大高度，与展开高度保持一致即不允许上滑（默认可以上滑）
            bottomSheet.setLayoutParams(layoutParams);*/

            final BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            //peekHeight即弹窗的最大高度
            behavior.setPeekHeight(peekHeight);
            //初始为展开状态
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }
    /**
     * 弹窗高度，默认为屏幕高度的四分之三
     * 子类可重写该方法返回peekHeight
     *
     * @return height
     */
    private int getPeekHeight(float x) {
        if (x <= 0 || x > 1) x = 0.5f;

        int peekHeight = getContext().getResources().getDisplayMetrics().heightPixels;

        return (int) (peekHeight * x);
    }
}
