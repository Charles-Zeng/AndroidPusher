package com.blueberry.media;

import android.util.Log;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by liyong on 2018/1/9.
 */

public class MinaClientHandler extends IoHandlerAdapter {

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        Log.i("TEST", "客户端发生异常");
        super.exceptionCaught(session, cause);
        Log.i("TEST", cause.getMessage());
    }
    @Override
    public void sessionOpened(IoSession session) throws Exception {
        super.sessionOpened(session);
        System.out.println("sessionOpened");
        session.write(BuildLoginPacket());
    }
    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        String msg = message.toString();
        Log.i("TEST", "i客户端接收到的信息为:" + msg);
        super.messageReceived(session, message);
        JSONObject PraseServiceRep = new JSONObject(message.toString());
        String RespType = PraseServiceRep.getString("Type");
        if(RespType.equals("LoginResp"))
        {
            //解析登陆返回类型，如果返回的是登陆成功，那么久开始发送心跳
            String PraseLoginResp = PraseServiceRep.getString("Status");
            if(PraseLoginResp.equals("Ok"))
            {
                //test20180122 测试验证用户密码是否成功,并且发送开始停止推流时间
                String PraseStopPushVideoSec = PraseServiceRep.getString("AutoStopPushMinutes");
                LoginValidation.getInstance().LoginSucOrFaild(true, 60000*Integer.parseInt(PraseStopPushVideoSec),"成功");
                session.write(BuildHeartPacket());
            }else
            {
                String RespMess = PraseServiceRep.getString("ErrorCode");
                if (RespMess.equals("-1"))
                {
                    LoginValidation.getInstance().LoginSucOrFaild(false, 0, "用户密码错误！");
                }else
                {
                    LoginValidation.getInstance().LoginSucOrFaild(false, 0, "服务名称已存在，请重输！");
                }

            }
        }else if(RespType.equals("Heart"))
        {
            //如果返回的是心跳，那么继续发送心跳
            session.write(BuildHeartPacket());
            Thread.sleep(5000);
        }else
        {
            //接受到服务器发送过来的控制命令，分为START和STOP两种操作命令
            Log.i("TEST","收到控制命令 " + msg);
            DataSource.getInstance().switchPush();
        }
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        // TODO Auto-generated method stub
        super.messageSent(session, message);
    }
    //构建登陆请求
    public String BuildLoginPacket() throws JSONException
    {
        JSONObject LoginPacket = new JSONObject();
        LoginPacket.put("Type","Login");
        LoginPacket.put("UserName",GlobalContextValue.UserName);
        LoginPacket.put("UserPwd",GlobalContextValue.UserPwd);
        LoginPacket.put("ServiceName",GlobalContextValue.ServiceName);
        LoginPacket.put("Mac",GlobalContextValue.DeviceMacAddress);
        LoginPacket.put("Imei",GlobalContextValue.DeviceIMEI);
        //LoginPacket.put("Gps",GlobalContextValue.DeviceGPS);
        System.out.print(LoginPacket);
        return LoginPacket.toString();
    }
    //构建心跳包
    public String BuildHeartPacket() throws JSONException
    {
        JSONObject HeartPacket = new JSONObject();
        HeartPacket.put("Type","Heart");
        System.out.print(HeartPacket);
        return HeartPacket.toString();
    }
    //构建登陆的gps位置信息
    public String BuildGpsPacket() throws JSONException
    {
        JSONObject GpsPacket = new JSONObject();
        GpsPacket.put("Type","LoginGps");
        GpsPacket.put("Gps",GlobalContextValue.DeviceGPS);
        System.out.print(GpsPacket);
        return GpsPacket.toString();
    }
}
