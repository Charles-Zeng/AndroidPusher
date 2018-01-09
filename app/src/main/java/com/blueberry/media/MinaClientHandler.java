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
        String RespType = PraseServiceRep.getString("TYPE");
        if(RespType.equals("LOGIN"))
        {
            //解析登陆返回类型，如果返回的是登陆成功，那么久开始发送心跳
            String PraseLoginResp = PraseServiceRep.getString("LoginRep");
            if(PraseLoginResp.equals("LOGIN_OK"))
            {
                session.write(BuildHeartPacket());
            }
        }else if(RespType.equals("HEART"))
        {
            //如果返回的是心跳，那么继续发送心跳
            session.write(BuildHeartPacket());
            Thread.sleep(5000);
        }else
        {
            //接受到服务器发送过来的控制命令，分为START和STOP两种操作命令
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
        LoginPacket.put("RequsetTpye","Login");
        LoginPacket.put("name","name");
        System.out.print(LoginPacket);
        return LoginPacket.toString();
    }
    //构建心跳包
    public String BuildHeartPacket() throws JSONException
    {
        JSONObject HeartPacket = new JSONObject();
        HeartPacket.put("RequsetTpye","Heart");
        System.out.print(HeartPacket);
        return HeartPacket.toString();
    }
}