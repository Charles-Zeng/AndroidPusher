package com.blueberry.media;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

/**
 * Created by liyong on 2018/1/9.
 */

public class MinaThread extends Thread {

    private IoSession session = null;
    private IoConnector connector = null;
    @Override
    public void run() {
        super.run();
        // TODO Auto-generated method stub]
        System.out.println("客户端链接开始...");
        connector = new NioSocketConnector();
        // 设置链接超时时间
        connector.setConnectTimeoutMillis(30000);
        // 添加过滤器
        // connector.getFilterChain().addLast("codec", new
        // ProtocolCodecFilter(new CharsetCodecFactory()));
        connector.getFilterChain().addLast("codec",
                new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"),
                        LineDelimiter.WINDOWS.getValue(), LineDelimiter.WINDOWS.getValue())));
        connector.setHandler(new MinaClientHandler());
        connector.setDefaultRemoteAddress(new InetSocketAddress(ConstantUtil.WEB_MATCH_PATH, ConstantUtil.WEB_MATCH_PORT));
        // 监听客户端是否断线
        connector.addListener(new IoListener() {
            @Override
            public void sessionDestroyed(IoSession arg0) throws Exception {
                // TODO Auto-generated method stub
                super.sessionDestroyed(arg0);
                try {
                    int failCount = 0;
                    while (true) {
                        Thread.sleep(5000);
                        System.out.println(((InetSocketAddress) connector.getDefaultRemoteAddress()).getAddress()
                                .getHostAddress());
                        ConnectFuture future = connector.connect();
                        future.awaitUninterruptibly();// 等待连接创建完成
                        session = future.getSession();// 获得session
                        if (session != null && session.isConnected()) {
                            System.out.println("断线重连["
                                    + ((InetSocketAddress) session.getRemoteAddress()).getAddress().getHostAddress()
                                    + ":" + ((InetSocketAddress) session.getRemoteAddress()).getPort() + "]成功");
                            break;
                        } else {
                            System.out.println("断线重连失败---->" + failCount + "次");
                        }
                    }
                } catch (Exception e) {
                    // TODO: handle exception
                }
            }
        });
        //开始连接
        try {
            ConnectFuture future = connector.connect();
            future.awaitUninterruptibly();// 等待连接创建完成
            session = future.getSession();// 获得session
            if (session != null && session.isConnected()) {
                SessionManager.getInstance().setSeesion(session);
            } else {
                System.out.println("SessionManager获取这个conner会话失败！");
            }
        } catch (Exception e) {
            System.out.println("客户端链接异常...");
        }
    }
}
