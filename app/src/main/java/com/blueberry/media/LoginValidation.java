package com.blueberry.media;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Created by liyong on 2018/1/22.
 */

public class LoginValidation extends Handler {

    static private volatile  LoginValidation instance = new LoginValidation();
    private LoginActivity activity = null;

    static public LoginValidation getInstance(){
        return instance;
    }

    private LoginValidation() {
        super(Looper.getMainLooper());
    }

    void setActivity(LoginActivity activity){
        this.activity = activity;
    }

    //登陆判断用户密码是否正确，参数1.final boolean sucOrFaild true表示成功，false失败；2.表示多少时间开始停止。
    void LoginSucOrFaild(final boolean sucOrFaild, final int StopPushVideoSec,final String ParesMes){
        post(new Runnable() {
            @Override
            public void run() {
                Log.i("LoginSucOrFaild","LoginSucOrFaild开始调用 " + sucOrFaild + StopPushVideoSec + ParesMes);
                activity.ShowMess(sucOrFaild, StopPushVideoSec, ParesMes);  //用户密码是否成功
            }
        });
    }
}
