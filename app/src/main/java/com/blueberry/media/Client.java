package com.blueberry.media;

/**
 * Created by liyong on 2018/1/5.
 */
public class Client extends Thread {
    private boolean flag = true;
    @Override
    public void run() {
        try {
            while (flag){
                SocketClientSender.getInstance().Login();
                synchronized (Client.class) {
                    //Thread.sleep(5000);
                    flag = false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
