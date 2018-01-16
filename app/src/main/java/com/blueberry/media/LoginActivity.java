package com.blueberry.media;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by liyong on 2018/1/4.
 */

public class LoginActivity extends Activity {
    private Button my_button = null;
    private EditText ServiceIP, username,password,ServiceName, VedioServiceIP;
    private String serviceip, user, pwd, servicename,vedioserviceip;
    private TextView log;
    public static final int CONNENTED = 0;
    public static final int UPDATALOG = 1;
    private Socket socket;
    private BufferedWriter writer;
    private InetSocketAddress isa = null;
    private String logMsg;
    private static final String TAG = "LoginActivity";
    private MessageBroadcastReceiver receiver;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        SysApplication.getInstance().addActivity(this);
        my_button = (Button)findViewById(R.id.btn_login);
        my_button.setText( "登陆" );
        my_button.setOnClickListener(new MyButtonListener());
    }
    class MyButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            // TODO Auto-generated method stub
            //初始化从界面上获取的值
            GetInputData();
            //registerBroadcast();
            //判断输入值是否为空
            if(!ValueIsEmpty())
            {
                return;
            }
            //建立socket通信
            setSocket();
            //跳转到页面实现
            Intent intent = new Intent();
            intent.setClass(LoginActivity.this, MainActivity.class);
            LoginActivity.this.startActivity(intent);
        }
    }
    //获取界面输入信息
    private void GetInputData() {
        //服务器ip地址获取
        ServiceIP = (EditText) findViewById(R.id.edittextSerIP);
        // 获得文本框中的用户
        serviceip = ServiceIP.getText().toString().trim();
        //用户名获取
        username = (EditText) findViewById(R.id.edittextName);
        // 获得文本框中的用户
        user = username.getText().toString().trim();
        //密码获取
        password = (EditText) findViewById(R.id.edittextPass);
        // 获得文本框中的用户
        pwd = password.getText().toString().trim();
        //服务名称
        ServiceName = (EditText) findViewById(R.id.edittextSerName);
        // 获得文本框中的用户
        servicename = ServiceName.getText().toString().trim();
        //视频转发服务器的地址获取
        VedioServiceIP = (EditText) findViewById(R.id.edittextVedioIP);
        // 获得文本框中的用户
        vedioserviceip = VedioServiceIP.getText().toString().trim();
        //从界面上的值获取下来存放在全局类中存放
        GlobalContextValue.ServiceIP = serviceip;
        GlobalContextValue.UserName = user;
        GlobalContextValue.UserPwd = pwd;
        GlobalContextValue.ServiceName = servicename;
        GlobalContextValue.VideoServiceIP = vedioserviceip;
        //GlobalContextValue.DeviceMacAddress = getMac();
        //GlobalContextValue.DeviceIMEI = getIMEI();
    }

    private void setSocket() {
        MinaThread mThread = new MinaThread();
        mThread.start();
            }
    private boolean ValueIsEmpty()
    {
        //判断IP输入框是否为空
        if(serviceip.isEmpty()){
            Toast.makeText(LoginActivity.this,"请输入服务器IP地址", Toast.LENGTH_LONG).show();
            return false;
        }
        //判断IP输入框是否为空
        if(user.isEmpty()){
            Toast.makeText(LoginActivity.this,"请输入用户名", Toast.LENGTH_LONG).show();
            return false;
        }
        //判断IP输入框是否为空
        if(pwd.isEmpty()){
            Toast.makeText(LoginActivity.this,"请输入密码", Toast.LENGTH_LONG).show();
            return false;
        }
        //判断IP输入框是否为空
        if(servicename.isEmpty()){
            Toast.makeText(LoginActivity.this,"请输入服务名称", Toast.LENGTH_LONG).show();
            return false;
        }
        //判断IP输入框是否为空
        if(vedioserviceip.isEmpty()){
            Toast.makeText(LoginActivity.this,"请输入视频转发IP地址", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }
    private void registerBroadcast() {
        receiver = new MessageBroadcastReceiver();
        IntentFilter filter = new IntentFilter("com.commonlibrary.mina.broadcast");
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    }
    private class MessageBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            //receive_tv.setText(intent.getStringExtra("message"));
        }
    }
    //获取本机mac地址
    /**
     * 这是使用adb shell命令来获取mac地址的方式
     * @return
     */
    public static String getMac() {
        String macSerial = null;
        String str = "";

        try {
            Process pp = Runtime.getRuntime().exec("cat /sys/class/net/wlan0/address ");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);

            for (; null != str; ) {
                str = input.readLine();
                if (str != null) {
                    macSerial = str.trim();// 去空格
                    break;
                }
            }
        } catch (IOException ex) {
            // 赋予默认值
            ex.printStackTrace();
        }
        return macSerial;
    }
    //获取本机IMEI号
    public String getIMEI() {
        String IMEISerial = null;
        TelephonyManager tm = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
        /*
        * 唯一的设备ID：
        * GSM手机的 IMEI 和 CDMA手机的 MEID.
        * Return null if device ID is not available.
        */
        int permission = ActivityCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.READ_PHONE_STATE);
        if(permission == PackageManager.PERMISSION_GRANTED)
        {
            IMEISerial = tm.getDeviceId();
        }
        return IMEISerial;
    }
    //获取本机GPS位置
    public String getGPS() {
        String GPSStrInfo = null;
        LocationManager mLocationManager =(LocationManager) this.getSystemService (Context.LOCATION_SERVICE);
        Criteria mCriteria = new Criteria();
        mCriteria.setAccuracy(Criteria.ACCURACY_FINE);//高精度
        mCriteria.setAltitudeRequired(true);//包含高度信息
        mCriteria.setBearingRequired(true);//包含方位信息
        mCriteria.setSpeedRequired(true);//包含速度信息
        mCriteria.setCostAllowed(true);//允许付费
        mCriteria.setPowerRequirement(Criteria.POWER_HIGH);//高耗电
        mLocationManager.getBestProvider(mCriteria,true);
        return GPSStrInfo;
    }
}
