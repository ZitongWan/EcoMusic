package com.xwrl.mvvm.demo.viewmodel;

import android.app.Application;
import android.view.View;

public class WelcomeViewModel extends BaseViewModel{

    public WelcomeViewModel(Application application) {
        super(application);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }

    public void ToMainActivity(View v){

    }
}
