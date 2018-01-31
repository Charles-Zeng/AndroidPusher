package com.blueberry.media;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.Poi;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.List;

/**
 * Created by liyong on 2018/1/4.
 */

public class LoginActivity extends Activity {
    private Button my_button = null;
    private CheckBox checkBox; //记住用户信息
    //声明一个SharedPreferences对象和一个Editor对象
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private EditText ServiceIP, username,password,ServiceName, VedioServiceIP;
    private String serviceip, user, pwd, servicename,vedioserviceip;
    private static final String TAG = "LoginActivity";
    //新增dialog界面显示摄像头分辨率
    private FVDialog fvDialog;
    private TextView ej_tv_title;
    private Camera.Size mSize;
    //摄像头预览高度,宽度
    private int height = 0;
    private int width = 0;
    //获取到的停止时间秒数
    public int StopPushVideoSec;
    //获取gps定位
    //定位都要通过LocationManager这个类实现
    public LocationClient mLocationClient = null;
    public BDLocationListener myListener = new MyLocationListener();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        SysApplication.getInstance().addActivity(this);
        ej_tv_title = (TextView) findViewById(R.id.ej_tv_title);
        ServiceIP = (EditText)findViewById(R.id.edittextSerIP);
        username = (EditText)findViewById(R.id.edittextName);
        password = (EditText)findViewById(R.id.edittextPass);
        ServiceName = (EditText)findViewById(R.id.edittextSerName);
        VedioServiceIP = (EditText)findViewById(R.id.edittextVedioIP);
        my_button = (Button)findViewById(R.id.btn_login);
        checkBox = (CheckBox) findViewById(R.id.checkBox);
        //获取preferences和editor对象
        preferences = getSharedPreferences("UserInfo", MODE_PRIVATE);
        editor = preferences.edit();
        /*
        启动程序时首先检查sharedPreferences中是否储存有用户名和密码
        若无，则将checkbox状态显示为未选中
        若有，则直接中sharedPreferences中读取用户名和密码，并将checkbox状态显示为已选中
        这里getString()方法需要两个参数，第一个是键，第二个是值。
        启动程序时我们传入需要读取的键，值填null即可。若有值则会自动显示，没有则为空。
        */
        serviceip = preferences.getString("serviceip",null);
        user = preferences.getString("username", null);
        pwd = preferences.getString("userpass", null);
        servicename = preferences.getString("servicename", null);
        vedioserviceip = preferences.getString("vedioserviceip", null);
        if (serviceip == null || user == null || pwd == null || servicename == null ||vedioserviceip == null)
        {
            checkBox.setChecked(false);
        } else {
            ServiceIP.setText(serviceip);
            username.setText(user);
            password.setText(pwd);
            ServiceName.setText(servicename);
            VedioServiceIP.setText(vedioserviceip);
            checkBox.setChecked(true);
        }
        my_button.setText( "登陆" );
        my_button.setOnClickListener(new MyButtonListener());
        //创建对话框显示摄像头支持分辨率
        fvDialog = new FVDialog(this, new FVDialog.FVDialogListener() {
            @Override
            public void fVListener(Camera.Size size) {
                ej_tv_title.setText(size.width+"*"+size.height);
                mSize = size;
                height = size.height;
                width = size.width;
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
            //测试获取gps地址
            startLocate();
            //初始化从界面上获取的值
            GetInputData();
            //判断输入值是否为空
            if(!ValueIsEmpty())
            {
                return;
            }
            //记住用户信息
            String serviceip = ServiceIP.getText().toString().trim();
            String user = username.getText().toString().trim();
            String pwd = password.getText().toString().trim();
            String servicename = ServiceName.getText().toString().trim();
            String vedioserviceip = VedioServiceIP.getText().toString().trim();
            if (checkBox.isChecked()) {
                //如果用户选择了记住用户信息
                //将用户输入的用户信息存入储存中，键为serviceip,username,userpass,servicename,vedioserviceip.
                editor.putString("serviceip", serviceip);
                editor.putString("username", user);
                editor.putString("userpass", pwd);
                editor.putString("servicename", servicename);
                editor.putString("vedioserviceip", vedioserviceip);
                editor.commit();
            } else {
                //否则将用户名清除
                editor.remove("serviceip");
                editor.remove("username");
                editor.remove("userpass");
                editor.remove("servicename");
                editor.remove("vedioserviceip");
                editor.commit();
            }
            //建立socket通信
            setSocket();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        LoginValidation.getInstance().setActivity(this);
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
        if(0 == height || 0 == width){
            Toast.makeText(LoginActivity.this,"请选择摄像头分辨率", Toast.LENGTH_LONG).show();
            return false;
        }else
        {
            GlobalContextValue.width = mSize.width;
            GlobalContextValue.height = mSize.height;
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
    //获取本机制造厂商
    public String getDeviceBrand() {
        return android.os.Build.BRAND;
    }
    //判断登陆用户密码是否正确以及接受到该用户的自动停止推流时间
    public void ShowMess(boolean sucOrFaild,int stopPushSec, String RespMess)
    {
        if (sucOrFaild)
        {
            //跳转到页面实现
            Intent intent = new Intent();
            //用Bundle携带数据
            Bundle bundle = new Bundle();
            //传递stopSec参数为StopPushVideoSec
            StopPushVideoSec = stopPushSec;
            Log.i(TAG, "ShowMess: " + stopPushSec);
            bundle.putInt("stopSec", StopPushVideoSec);
            intent.putExtras(bundle);
            intent.setClass(LoginActivity.this, MainActivity.class);
            LoginActivity.this.startActivity(intent);
        }
        else
        {
            Toast.makeText(LoginActivity.this, RespMess, Toast.LENGTH_LONG).show();
            return;
        }
    }
    //以下代码实现获取手机gps定位
    /*** 定位*/
    private void startLocate() {
        mLocationClient = new LocationClient(getApplicationContext());     //声明LocationClient类
        mLocationClient.registerLocationListener(myListener);    //注册监听函数
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Battery_Saving
        );//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系
        //int span = 1000;
        option.setScanSpan(0);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);//可选，默认false,设置是否使用gps
        option.setLocationNotify(true);//可选，默认false，设置是否当GPS有效时按照1S/1次频率输出GPS结果
        option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤GPS仿真结果，默认需要
        mLocationClient.setLocOption(option);
        //开启定位
        mLocationClient.start();
    }

    private class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            StringBuffer sb = new StringBuffer(256);
            sb.append("time : ");
            sb.append(location.getTime());
            sb.append("\nerror code : ");
            sb.append(location.getLocType());
            sb.append("\nlatitude : ");
            sb.append(location.getLatitude());
            sb.append("\nlontitude : ");
            sb.append(location.getLongitude());
            sb.append("\nradius : ");
            sb.append(location.getRadius());
            if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
                sb.append("\nspeed : ");
                sb.append(location.getSpeed());// 单位：公里每小时
                sb.append("\nsatellite : ");
                sb.append(location.getSatelliteNumber());
                sb.append("\nheight : ");
                sb.append(location.getAltitude());// 单位：米
                sb.append("\ndirection : ");
                sb.append(location.getDirection());// 单位度
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
                sb.append("\ndescribe : ");
                sb.append("gps定位成功");

            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
                //运营商信息
                sb.append("\noperationers : ");
                sb.append(location.getOperators());
                sb.append("\ndescribe : ");
                sb.append("网络定位成功");
            } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
                sb.append("\ndescribe : ");
                sb.append("离线定位成功，离线定位结果也是有效的");
            } else if (location.getLocType() == BDLocation.TypeServerError) {
                sb.append("\ndescribe : ");
                sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
            } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                sb.append("\ndescribe : ");
                sb.append("网络不同导致定位失败，请检查网络是否通畅");
            } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
                sb.append("\ndescribe : ");
                sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
            }
            sb.append("\nlocationdescribe : ");
            sb.append(location.getLocationDescribe());// 位置语义化信息
            List<Poi> list = location.getPoiList();// POI数据
            if (list != null) {
                sb.append("\npoilist size = : ");
                sb.append(list.size());
                for (Poi p : list) {
                    sb.append("\npoi= : ");
                    sb.append(p.getId() + " " + p.getName() + " " + p.getRank());
                }
            }
            //将获取到的gps定位地址和描述
            GlobalContextValue.DeviceGPS = location.getAddrStr() + location.getLocationDescribe();
            if (GlobalContextValue.DeviceGPS != null)
            {
                String clientGps = "";
                try {
                    clientGps = BuildGpsPacket();
                }catch (JSONException e)
                {
                    e.printStackTrace();
                }
                //客户端停止推流告诉服务器开始推送状态
                SessionManager.getInstance().writeToServer(clientGps);
                Log.i(TAG, "GPS位置信息：" + clientGps);
            }
            //Log.e("描述：", sb.toString());
        }
        //构建登陆的gps位置信息
        public String BuildGpsPacket() throws JSONException
        {
            JSONObject GpsPacket = new JSONObject();
            GpsPacket.put("Type","LoginGps");
            GpsPacket.put("ServiceName",GlobalContextValue.ServiceName);
            GpsPacket.put("Gps",GlobalContextValue.DeviceGPS);
            System.out.print(GpsPacket);
            return GpsPacket.toString();
        }
    }
}
