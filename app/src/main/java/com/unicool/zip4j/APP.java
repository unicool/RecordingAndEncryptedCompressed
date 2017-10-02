package com.unicool.zip4j;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;

import com.unicool.recording.service.RecordService;


/*
 *  @项目名：  NationalCar-Android 
 *  @包名：    com.unicool.zip4j
 *  @文件名:   APP
 *  @创建者:   cjf
 *  @创建时间:  2017/9/30 12:32
 *  @描述：    TODO
 */
public class APP extends Application {
    public static APP mInstance;
    public static Handler mHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        // Initializing the full recording function
        startService(new Intent(this, RecordService.class));

    }
}
