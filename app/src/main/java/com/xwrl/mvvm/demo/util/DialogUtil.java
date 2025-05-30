package com.xwrl.mvvm.demo.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.xwrl.mvvm.demo.R;

public class DialogUtil {

    public static void BottomShow(Dialog dialog,boolean setSoftInputMode){
        Window window = getDialogWindow(dialog);
        if (isNull(window)) return;
        //设置背景为透明颜色，如果有白色背景
        View view = window.findViewById(R.id.design_bottom_sheet);
        if (!isNull(view)) { view.setBackgroundResource(android.R.color.transparent); }
        //设置显示位置
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        //*如果wrap_content高度有误，则在这儿设置为MATCH_PARENT，防止dialog已完全展开但dialog的高度不够显示完全
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.gravity = Gravity.BOTTOM;

        window.setAttributes(layoutParams);
        if (setSoftInputMode) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            window.setWindowAnimations(R.style.dialog_animation);
        }else window.setWindowAnimations(R.style.dialog_animation_noEnter);//设置为bottomSheetDialog自带的进入动画
    }
    public static void onMiddleAndLowerDisplay(DialogFragment dialogFragment){
        onMiddleAndLowerDisplay(getDialogFragmentWindow(dialogFragment),false);
    }
    public static void onMiddleAndLowerDisplay(Window window, boolean isFull){

        if (isNull(window)) return;

        //设置背景为透明颜色，如果有白色背景
        View view = window.findViewById(R.id.design_bottom_sheet);
        if (!isNull(view)) { view.setBackgroundResource(android.R.color.transparent); }

        window.setBackgroundDrawableResource(R.color.color01Black);//设置背景为透明色
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        if (isFull) {
            int height = window.getContext().getResources().getDisplayMetrics().heightPixels;
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;

            layoutParams.height = (int) (height * 0.69f); //决定此Dialog的高度
        }
        layoutParams.dimAmount = 0.6f;//背景变暗的范围
        layoutParams.gravity = Gravity.BOTTOM;
        window.setAttributes(layoutParams);
        window.setWindowAnimations(R.style.dialog_animation);//设置打开和退出动画
    }
    public static void onMiddleLeftDisplay(DialogFragment dialogFragment){
        Window window = getDialogFragmentWindow(dialogFragment);
        if (isNull(window)) return;

        //设置背景为透明颜色，如果有白色背景
        View view = window.findViewById(R.id.design_bottom_sheet);
        if (!isNull(view)) { view.setBackgroundResource(android.R.color.transparent); }

        DisplayMetrics dm = new DisplayMetrics();
        window.getWindowManager().getDefaultDisplay().getMetrics(dm);

        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.width = (int)(dm.widthPixels * 0.86);
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;//【MIUI】距离顶部 有一内内距离
        layoutParams.dimAmount = 0.6f;//背景变暗的范围
        layoutParams.gravity = Gravity.FILL_VERTICAL | Gravity.START;
        window.setAttributes(layoutParams);

        window.setBackgroundDrawableResource(R.color.color01Black);//设置背景为透明色
        window.setWindowAnimations(R.style.dialog_animation_left);//设置打开和退出动画
    }

    private static Window getDialogWindow(Dialog dialog){
        if (isNull(dialog)) return null;
        return dialog.getWindow();
    }

    private static Window getDialogFragmentWindow(DialogFragment dialogFragment){
        if (isNull(dialogFragment)) return null;
        return getDialogWindow(dialogFragment.getDialog());
    }

    private static boolean isNull(Object o){
        return o == null;
    }


    @Nullable
    public static Activity findActivity(Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        }
        if (context instanceof ContextWrapper) {
            ContextWrapper wrapper = (ContextWrapper) context;
            return findActivity(wrapper.getBaseContext());
        } else {
            return null;
        }
    }

    public static void showCenterDialog(Window window){
        if (isNull(window)) return;

        //设置背景为透明颜色，如果有白色背景
        View view = window.findViewById(R.id.design_bottom_sheet);
        if (!isNull(view)) { view.setBackgroundResource(android.R.color.transparent); }

        window.setBackgroundDrawableResource(R.color.color01Black);//设置背景为透明色
        WindowManager.LayoutParams layoutParams = window.getAttributes();

        int width = window.getContext().getResources().getDisplayMetrics().widthPixels;
        layoutParams.width = (int) (width * 0.7);


        layoutParams.dimAmount = 0.6f;//背景变暗的范围
        layoutParams.gravity = Gravity.CENTER;
        window.setAttributes(layoutParams);
        //window.setWindowAnimations(R.style.dialog_animation);//设置打开和退出动画
    }
}
