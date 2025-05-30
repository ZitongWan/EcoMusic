package com.xwrl.mvvm.demo.view.fragment;

import android.util.TypedValue;

import androidx.fragment.app.Fragment;

public abstract class BaseFragment extends Fragment {

    protected int dpToPx(int dp){
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }
}
