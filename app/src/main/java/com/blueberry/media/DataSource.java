package com.blueberry.media;

import android.support.v7.app.AppCompatActivity;

/**
 * Created by CharlesZeng on 2018-01-10.
 */

public class DataSource {

    static private DataSource instance = new DataSource();

    private int count = 0;
    //private boolean pushStatus = false;

    private MainActivity activity = null;

    static public DataSource getInstance(){
        return instance;
    }

    void setActivity(MainActivity activity){
        this.activity = activity;
    }

    void switchPush(){
        activity.switchPublish();
        /*++count;
        if (count % 5 == 0){
            if (activity != null){
                activity.switchPublish();
            }
        }*/
    }
}
