package com.xwrl.mvvm.demo.view.fragment;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xwrl.mvvm.demo.R;
import com.xwrl.mvvm.demo.util.HttpUtil;

public class EditFragment extends BaseFragment implements View.OnClickListener{

    private static final String TAG = "EditFragment";

    public EditText et_input;
    private String mAlias;
    private ImageView iv_empty;

    public EditFragment() {
    }

    public void setAlias(String alias) {
        this.mAlias = alias;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit, container, false);
        et_input = view.findViewById(R.id.fragment_edit_et);
        iv_empty = view.findViewById(R.id.fragment_edit_iv_empty);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        //Log.d("EditFragment", "onStart: ");
        iv_empty.setOnClickListener(this);
        if (!TextUtils.isEmpty(mAlias)) et_input.setText(mAlias);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);//视图重新可见时
        if (this.isVisible()) {
            et_input.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireActivity()
                                            .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(et_input,InputMethodManager.SHOW_IMPLICIT);
        } else et_input.clearFocus();
    }

    @Override
    public void onStop() {
        super.onStop();
        //Log.d("EditFragment", "onStop: ");
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: ");
        if (et_input != null) et_input = null;
        if (mAlias != null) mAlias = null;
    }

    @Override
    public void onClick(View v) {
        if(HttpUtil.isFastClick()) return;
        if (!TextUtils.isEmpty(et_input.getText().toString()))
            et_input.setText(null);
    }
}
