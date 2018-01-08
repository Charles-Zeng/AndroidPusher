package com.blueberry.media;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by liyong on 2018/1/5.
 */

public class SocketClientSender{
    private SocketClientSender(){}
    Socket sender = null;
    private static  SocketClientSender  instance;
    public static SocketClientSender getInstance(){
        if(instance == null){
            synchronized(Client.class){
                instance = new SocketClientSender();
            }
        }
        return instance;
    }

    public void sendHeart(){
        try{
            while (true)
            {
                BufferedWriter RequstHeart = new BufferedWriter(new OutputStreamWriter(sender.getOutputStream()));
                //拼接心跳包json结构体
                JSONObject HeartPacket = new JSONObject();
                HeartPacket.put("RequsetTpye","Heart");
                String TotalLoginPacket = HeartPacket.toString() + "\n";
                RequstHeart.write(TotalLoginPacket);
                RequstHeart.flush();
                Thread.sleep(3000);
                //解析心跳响应和服务器发送的cmd命令
                String ReqRepType = ParseHeartAndCmd();
                if(ReqRepType.equals("CMD"))
                {
                    //20180108 添加控制mainact中的停止开关实现
                }else
                {
                    continue;
                }
            }
        }catch(Exception e){
            System.out.print(e);
        }
    }
    public void Login()
    {
        try{
            //开始登陆
            sender = new Socket(GlobalContextValue.ServiceIP,9800);
            BufferedWriter RequstLogin = new BufferedWriter(new OutputStreamWriter(sender.getOutputStream()));
            String tempStr = BuildLoginPacket();
            RequstLogin.write(tempStr);
            RequstLogin.flush();
            Thread.sleep(2000);
            String repsLogin = ParseLogin();
            if (repsLogin.equals("LOGIN_OK"))
            {
                sendHeart();
            }
        }catch(Exception e){
            System.out.print(e);
        }
    }
    //构建心跳包
    public String BuildLoginPacket() throws JSONException
    {
        JSONObject LoginPacket = new JSONObject();
        LoginPacket.put("RequsetTpye","Login");
        LoginPacket.put("name","name");
        LoginPacket.put("pwd","pwd");
        String TotalLoginPacket = LoginPacket.toString() + "\n";
        System.out.print(LoginPacket);
        return TotalLoginPacket;
    }
    //解析登陆返回结构体json
    public String ParseLogin() throws IOException
    {
        //等待，接收来自服务器返回的消息
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                sender.getInputStream()));
        String RecvStr = reader.readLine();
        String loginrsp = "";
        System.out.print(RecvStr);
        try {
            JSONObject PraseLoginRep = new JSONObject(RecvStr);
            loginrsp = PraseLoginRep.getString("LoginRep");
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        return loginrsp;
    }
    //解析服务器发送过来的Heart和Cmd命令请求
    public String ParseHeartAndCmd() throws IOException
    {
        //等待，接收来自服务器返回的消息
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                sender.getInputStream()));
        String RecvStrHeartAndCmd = reader.readLine();
        String HeartAndCmd = "";
        System.out.print(RecvStrHeartAndCmd);
        try {
            JSONObject PraseRecvStrHeartAndCmdRep = new JSONObject(RecvStrHeartAndCmd);
            HeartAndCmd = PraseRecvStrHeartAndCmdRep.getString("TYPE");
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        return HeartAndCmd;
    }
}
