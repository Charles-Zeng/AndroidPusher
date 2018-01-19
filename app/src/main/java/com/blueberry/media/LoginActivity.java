package com.blueberry.media;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * Created by liyong on 2018/1/4.
 */

public class LoginActivity extends Activity {
    private Button my_button = null;
    private EditText ServiceIP, username,password,ServiceName, VedioServiceIP;
    private String serviceip, user, pwd, servicename,vedioserviceip;
    private static final String TAG = "LoginActivity";
    //新增dialog界面显示摄像头分辨率
    private FVDialog fvDialog;
    private TextView ej_tv_title;
    private Camera.Size mSize;
    //获取gps定位
    //定位都要通过LocationManager这个类实现
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        SysApplication.getInstance().addActivity(this);
        ej_tv_title = (TextView) findViewById(R.id.ej_tv_title);
        my_button = (Button)findViewById(R.id.btn_login);
        my_button.setText( "登陆" );
        my_button.setOnClickListener(new MyButtonListener());
        //创建对话框显示摄像头支持分辨率
        fvDialog = new FVDialog(this, new FVDialog.FVDialogListener() {
            @Override
            public void fVListener(Camera.Size size) {
                ej_tv_title.setText(size.width+"*"+size.height);
                mSize = size;
            }
        });
        findViewById(R.id.lv_ll_fv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fvDialog.show();
            }
        });
    }
    class MyButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            // TODO Auto-generated method stub
            //初始化从界面上获取的值
            GetInputData();
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
        serviceip = ServiceIP.getText().toString().trim();
        //用户名获取
        username = (EditText) findViewById(R.id.edittextName);
        user = username.getText().toString().trim();
        //密码获取
        password = (EditText) findViewById(R.id.edittextPass);
        pwd = password.getText().toString().trim();
        //服务名称
        ServiceName = (EditText) findViewById(R.id.edittextSerName);
        servicename = ServiceName.getText().toString().trim();
        //视频转发服务器的地址获取
        VedioServiceIP = (EditText) findViewById(R.id.edittextVedioIP);
        vedioserviceip = VedioServiceIP.getText().toString().trim();
        //从界面上的值获取下来存放在全局类中存放
        GlobalContextValue.ServiceIP = serviceip;
        GlobalContextValue.UserName = user;
        GlobalContextValue.UserPwd = pwd;
        GlobalContextValue.ServiceName = servicename;
        GlobalContextValue.VideoServiceIP = vedioserviceip;
        GlobalContextValue.DeviceMacAddress = getMac();
        GlobalContextValue.DeviceIMEI = getIMEI();
        GlobalContextValue.DeviceBrand = getDeviceBrand();
        GlobalContextValue.DeviceGPS = getLngAndLat();
        GlobalContextValue.width = mSize.width;
        GlobalContextValue.height = mSize.height;
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
        //判断用户名输入框是否为空
        if(user.isEmpty()){
            Toast.makeText(LoginActivity.this,"请输入用户名", Toast.LENGTH_LONG).show();
            return false;
        }
        //判断密码输入框是否为空
        if(pwd.isEmpty()){
            Toast.makeText(LoginActivity.this,"请输入密码", Toast.LENGTH_LONG).show();
            return false;
        }
        //判断服务名输入框是否为空
        if(servicename.isEmpty()){
            Toast.makeText(LoginActivity.this,"请输入服务名称", Toast.LENGTH_LONG).show();
            return false;
        }
        //判断视频转发地址输入框是否为空
        if(vedioserviceip.isEmpty()){
            Toast.makeText(LoginActivity.this,"请输入视频转发IP地址", Toast.LENGTH_LONG).show();
            return false;
        }
        //判断摄像头分辨率是否为空
        if(GlobalContextValue.height == 0 | GlobalContextValue.width == 0){
            Toast.makeText(LoginActivity.this,"请选择摄像头分辨率", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }
    //获取本机mac地址
    /**
     * 这是使用adb shell命令来获取mac地址的方式
     * @return
     */
    public static String getMac() {
        String macAddress = null;
        StringBuffer buf = new StringBuffer();
        NetworkInterface networkInterface = null;
        try {
            networkInterface = NetworkInterface.getByName("eth1");
            if (networkInterface == null) {
                networkInterface = NetworkInterface.getByName("wlan0");
            }
            if (networkInterface == null) {
                return "02:00:00:00:00:02";
            }
            byte[] addr = networkInterface.getHardwareAddress();
            for (byte b : addr) {
                buf.append(String.format("%02X:", b));
            }
            if (buf.length() > 0) {
                buf.deleteCharAt(buf.length() - 1);
            }
            macAddress = buf.toString();
        } catch (SocketException e) {
            e.printStackTrace();
            return "02:00:00:00:00:02";
        }
        return macAddress;
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
    /*** 获取经纬度** @param* @return*/
    private String getLngAndLat() {
        double latitude = 0.0;
        double longitude = 0.0;
        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            //从gps获取经纬度
            int permission = ActivityCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
            Location location = null;
            if(permission == PackageManager.PERMISSION_GRANTED)
            {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            } else {
                //当GPS信号弱没获取到位置的时候又从网络获取
                return getLngAndLatWithNetwork();
            }
        } else {
            //从网络获取经纬度
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListener);
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }
        }
        return longitude + "," + latitude;
    }
    //从网络获取经纬度
    public String getLngAndLatWithNetwork() {
        double latitude = 0.0;
        double longitude = 0.0;
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        int permission = ActivityCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListener);
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
        return longitude + "," + latitude;
    }

    LocationListener locationListener = new LocationListener() {
        // Provider的状态在可用、暂时不可用和无服务三个状态直接切换时触发此函数
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
        // Provider被enable时触发此函数，比如GPS被打开
        @Override
        public void onProviderEnabled(String provider) {

        }
        // Provider被disable时触发此函数，比如GPS被关闭
        @Override
        public void onProviderDisabled(String provider) {

        }
        //当坐标改变时触发此函数，如果Provider传进相同的坐标，它就不会被触发
        @Override
        public void onLocationChanged(Location location) {
        }
    };
    //获取本机制造厂商
    public String getDeviceBrand() {
        return android.os.Build.BRAND;
    }
    public void ShowMess()
    {
        new AlertDialog.Builder(this).setTitle("删除提示框").setMessage("确认删除该数据？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }}).setNegativeButton("取消",null).show();
    }
}
