package com.blueberry.media;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by liyong on 2018/1/9.
 */

public class BuildAndParsePakgeJson {
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
