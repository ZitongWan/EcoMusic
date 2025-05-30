package com.xwrl.mvvm.demo.view.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.xwrl.mvvm.demo.R;

public class ConfirmDialog extends AlertDialog implements View.OnClickListener{
    private static final String TAG = "ConfirmDialog";
    private TextView tv_tips, tv_cancel, tv_confirm;
    private String mConfirmationContent;
    private final boolean isUseAgreeText, isPermissionTips;
    private boolean isJokeTips;

    private OnConfirmListener mOnConfirmListener;
    public interface OnConfirmListener{
        void onConfirm(boolean result);
    }
    public void setOnConfirmListener(OnConfirmListener onConfirmListener) {
        this.mOnConfirmListener = onConfirmListener;
    }

    /**
     * @param context 不能传{@link android.app.Application}过来
     * 否则会报无Appcompat主题的错误，md一个Dialog哪儿去设置Activity主题[doge]
     * */
    public ConfirmDialog(@NonNull Context context, int themeResId,
                                        String msg, boolean isPermissionTips) {
        super(context, themeResId);

        this.mConfirmationContent = msg;
        this.isPermissionTips = isPermissionTips;
        this.isJokeTips = isPermissionTips;

        isUseAgreeText = msg != null && msg.contains(context.getText(R.string.label_Dialog_get_tips_confirm));
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(isPermissionTips ? R.layout.dialog_get_tips : R.layout.dialog_confirm);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindView();
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

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.dialog_confirm_cancel:
                if (isJokeTips && tv_cancel != null) {
                    tv_cancel.setText(R.string.label_Dialog_get_tips_exit);
                    isJokeTips = false;
                    break;
                }
                if (mOnConfirmListener != null) { mOnConfirmListener.onConfirm(false); }
                cancel();
                break;
            case R.id.dialog_confirm_confirm:
                if (mOnConfirmListener != null) mOnConfirmListener.onConfirm(true);
                dismiss();
                break;
        }
    }

    public void setConfirmationContent(String confirmationContent) {
        this.mConfirmationContent = confirmationContent;
        if (tv_tips != null) { tv_tips.setText(mConfirmationContent); }
    }

    public void setCantCancel(){
        this.setCancelable(false);
        this.setCanceledOnTouchOutside(false);
    }

    private void bindView() {
        tv_tips = findViewById(R.id.dialog_confirm_message);
        if (tv_tips != null) { tv_tips.setText(mConfirmationContent); }

        tv_cancel = findViewById(R.id.dialog_confirm_cancel);
        if (tv_cancel != null) {  tv_cancel.setOnClickListener(this); }

        tv_confirm = findViewById(R.id.dialog_confirm_confirm);
        if (tv_confirm != null) {
            tv_confirm.setOnClickListener(this);
            if (isUseAgreeText) tv_confirm.setText(getContext().getText(R.string.label_Dialog_get_tips_confirm));
        }
    }

    private void unBindView() {
        if (tv_tips != null) { tv_tips = null; }
        if (tv_cancel != null) { tv_cancel = null; }
        if (tv_confirm != null) { tv_confirm = null; }
        if (mOnConfirmListener != null) { mOnConfirmListener = null; }
        if (mConfirmationContent != null) { mConfirmationContent = null; }
    }
}
