package com.blueberry.media;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        my_button = (Button)findViewById(R.id.btn_login);
        my_button.setText( "登陆" );
        my_button.setOnClickListener(new MyButtonListener());
    }
    class MyButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            // TODO Auto-generated method stub
            initView();
            setSocket();
            Intent intent = new Intent();
            intent.setClass(LoginActivity.this, MainActivity.class);
            LoginActivity.this.startActivity(intent);
        }
    }
    //获取界面输入信息
    private void initView() {
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
    }

    private void setSocket() {
                tcpClient tcp = new tcpClient();
                tcp.start();
            };
    class tcpClient extends Thread {
        public void run() {
            String recv;
            try {
                Socket ConSocket = new Socket();
                //创建套接字地址，其中 IP 地址为通配符地址，端口号为指定值。
                //有效端口值介于 0 和 65535 之间。端口号 zero 允许系统在 bind 操作中挑选暂时的端口。
                isa = new InetSocketAddress(serviceip, 4800);
                //建立一个远程链接
                ConSocket.connect(isa);
                //socket.connect(isa);
                socket=ConSocket;
                if (socket.isConnected()) {

                }
                //向服务器发送命令
                writer = new BufferedWriter(new OutputStreamWriter(
                        socket.getOutputStream()));
                writer.write("嘿嘿，你好啊，服务器.."); // 写一个UTF-8的信息
                System.out.println("发送消息");
                writer.flush();
                //等待，接收来自服务器返回的消息
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                String line;
                String txt = "";
                while ((line = reader.readLine()) != null) {
                    txt += line + "\n";
                }
                reader.close();
                recv = txt;

                if (recv != null) {
                    logMsg += recv;
                    // close BufferedWriter and socket
                    writer.close();
                    socket.close();
                    // 将服务器返回的消息显示出来
                    Message msg = new Message();
                    msg.what = UPDATALOG;
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
