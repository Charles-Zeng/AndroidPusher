package com.blueberry.media;

import android.os.Handler;
import android.os.Looper;

/**
 * Created by CharlesZeng on 2018-01-10.
 */

public class DataSource extends Handler {

    static private volatile  DataSource instance = new DataSource();

    private int count = 0;
    //private boolean pushStatus = false;

    private MainActivity activity = null;

    static public DataSource getInstance(){
        return instance;
    }

    private DataSource() {
        super(Looper.getMainLooper());
    }

    void setActivity(MainActivity activity){
        this.activity = activity;
    }

    void switchPush(){
        post(new Runnable() {
            @Override
            public void run() {
                activity.switchPublish();
            }
        });
    }
}
