package com.xwrl.mvvm.demo;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.xwrl.mvvm.demo.bean.UserBean;
import com.xwrl.mvvm.demo.databinding.ActivityWelcomeBinding;
import com.xwrl.mvvm.demo.model.helper.SQLiteChangeHelper;
import com.xwrl.mvvm.demo.model.helper.SQLiteUserHelper;
import com.xwrl.mvvm.demo.service.manager.LastMetaManager;
import com.xwrl.mvvm.demo.util.ImmersiveStatusBarUtil;
import com.xwrl.mvvm.demo.util.PictureUtil;
import com.xwrl.mvvm.demo.util.StringUtil;
import com.xwrl.mvvm.demo.viewmodel.WelcomeViewModel;

public class WelcomeActivity extends AppCompatActivity implements View.OnApplyWindowInsetsListener{
    private static final String TAG = "WelcomeActivity";

    private ActivityWelcomeBinding mWelcomeBinding;
    private WelcomeViewModel mWelcomeViewModel;

    private boolean isRegister;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ImmersiveStatusBarUtil.transparentBar(this,true);
        super.onCreate(savedInstanceState);

        mWelcomeBinding = DataBindingUtil.setContentView(this, R.layout.activity_welcome);
        mWelcomeViewModel = new WelcomeViewModel(this.getApplication());
        mWelcomeBinding.setWelcome(mWelcomeViewModel);

        mWelcomeBinding.activityWelcomeRootLayout.setOnApplyWindowInsetsListener(this);

        mWelcomeBinding.activityWelcomeBtToMain.setOnClickListener( v -> toMainActivity(null));
        mWelcomeBinding.activityWelcomeBtLogin.setOnClickListener( v -> toLogin());
        mWelcomeBinding.activityWelcomeBtRegister.setOnClickListener( v -> toRegister());
        mWelcomeBinding.activityWelcomeRootLayout.setOnClickListener( v ->
                ImmersiveStatusBarUtil.HideSoftInput(WelcomeActivity.this));

        mWelcomeBinding.activityWelcomeIcon.setBackground(
                PictureUtil.createCircleDrawable(getApplication(), R.drawable.icon_fate, 50)
        );

        isRegister = true;
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
        if (mWelcomeViewModel != null) { mWelcomeViewModel = null; }

        if (mWelcomeBinding != null) {
            mWelcomeBinding.unbind();
            mWelcomeBinding = null;
        }
    }

    @Override
    public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
        Log.d(TAG, "onApplyWindowInsets: "+insets.getSystemWindowInsetTop()+
                " paddingBottom "+insets.getSystemWindowInsetBottom());
        if (insets.getSystemWindowInsetBottom() < 288) {
            int paddingBottom = insets.getSystemWindowInsetBottom();
            int paddingTop = insets.getSystemWindowInsetTop();
            v.setPadding(insets.getSystemWindowInsetLeft(),paddingTop,
                    insets.getSystemWindowInsetRight(), paddingBottom);
            Log.d(TAG, "onApplyWindowInsets: ");

            //更改导航栏颜色
            getWindow().setNavigationBarColor(Color.BLACK);
        }

        return insets;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            moveTaskToBack(true);
            return false; //需要返回一个值，但不能返回默认的
        }
        return super.onKeyDown(keyCode, event);
    }

    private void toMainActivity(UserBean userBean){
        Boolean isStranger = userBean == null;
        //1. 设置全局 用户数据库名 "musicListUser+用户个数.db", 游客登录则为"musicList.db"
        LastMetaManager lastMetaManager = new LastMetaManager(getApplication());
        String dbName = isStranger ? SQLiteChangeHelper.UserDBName :
                            SQLiteUserHelper.queryUserInfo(getApplicationContext(),
                                    userBean.getUser(), userBean.getPassword()).getUserDBName();
        Log.d(TAG, "toMainActivity: 保存全局数据库名 ："+dbName);
        lastMetaManager.saveUserDBName(dbName);
        lastMetaManager.onDestroy();

        //2. 跳转至主界面MainActivity
        Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
        //每次跳转 mainActivity 就刷新intent信息。
        intent.putExtra("user",
                isStranger ? "" : userBean.getUser());
        intent.putExtra("password",
                isStranger ? "" : userBean.getPassword());
        intent.putExtra("userAlias",
                isStranger ? "" : userBean.getAlias());

        intent.putExtra("isStranger",isStranger);
        startActivity(intent);
    }

    private void toLogin(){
        if (isRegister) {
            checkLogin();
        }else {
            registerUser();
        }
    }

    private boolean checkString(String user, String password){
        if (user == null || password == null){ return false;}
        else if (TextUtils.isEmpty(user) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "用户名或密码为空！请重新输入", Toast.LENGTH_SHORT).show();
            return false;
        } else if (!StringUtil.isOnlyDigitAndLetter(user)) {
            Toast.makeText(this, "用户名只能为数字和字母组合！请重新输入", Toast.LENGTH_SHORT).show();
            return false;
        } else{ return true; }
    }

    private void checkLogin(){
        String user = "", password = "";
        //1.获得 用户输入的 用户名和密码，通过 editText 格式转换
        user += mWelcomeBinding.activityWelcomeEtUser.getText().toString();
        password += mWelcomeBinding.activityWelcomeEtPassword.getText().toString();

        if (!checkString(user, password)) {return;}

        //核验通过
        UserBean userBean = new UserBean(user, password, "", "");
        boolean checkResult = SQLiteUserHelper.insertUserInfo(getApplicationContext(),
                userBean, true) == -1;
        Log.d(TAG, "check: 用户是否已注册 "+checkResult+", 用户名："+userBean.getUser());
        if (checkResult) {
            toMainActivity(userBean);
        }else {
            ImmersiveStatusBarUtil.HideSoftInput(this);
            mWelcomeBinding.activityWelcomeEtPassword.setText("");
            Toast.makeText(this, "登录失败，请检查用户名或密码。", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerUser(){
        String user = "", password = "";
        //1.获得 用户输入的 用户名和密码，通过 editText 格式转换
        user += mWelcomeBinding.activityWelcomeEtUser.getText().toString();
        password += mWelcomeBinding.activityWelcomeEtPassword.getText().toString();
        //2. 判断输入的用户名和密码是否仅为字母、数字和标点符号
        if (!checkString(user, password)) {return;}
        //3. 将 用户名和密码 保存至用户信息数据表里面  检查结果
        UserBean userBean = new UserBean(user, password, "", "");
        int i = SQLiteUserHelper.insertUserInfo(getApplicationContext(), userBean,false);
        //4.获得结果 以 toast 的形式告知用户，然后 回到登录状态，并将用户名和密码填入editText里
        if (i == 20) {
            Toast.makeText(this, "创建用户成功，请登录！", Toast.LENGTH_SHORT).show();
            toRegister();
        }else {
            Toast.makeText(this,
                    i == -1 ? "创建用户失败，用户已创建！" : "创建用户遇到问题，请重试！",
                    Toast.LENGTH_SHORT).show();
        }
        //5.最后隐藏软键盘
        ImmersiveStatusBarUtil.HideSoftInput(this);
    }

    private void toRegister(){

        if(mWelcomeBinding == null) {
            Log.e(TAG, "toRegister: databinding == null");
            return;
        }

        mWelcomeBinding.activityWelcomeBtLogin
                .setHint(isRegister ?
                        R.string.label_WelcomeActivity_hint_save : R.string.label_WelcomeActivity_hint_login);
        mWelcomeBinding.activityWelcomeBtRegister
                .setHint(isRegister ?
                        R.string.label_WelcomeActivity_hint_cancel : R.string.label_WelcomeActivity_hint_register);
        isRegister = !isRegister;

    }
}
