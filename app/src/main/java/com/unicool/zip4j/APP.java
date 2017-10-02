package com.unicool.zip4j;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.unicool.recording.service.RecordService;

import java.util.List;


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

    /**
     * @return null may be returned if the specified process not found
     */
    public static String getProcessName(Context context, int pid) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
        if (runningApps == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo procInfo : runningApps) {
            if (procInfo.pid == pid) {
                return procInfo.processName;
            }
        }
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        String processName = getProcessName(this, android.os.Process.myPid());
        //If there are multiple processes, only initialize the main process
        if (processName != null && !processName.startsWith(this.getPackageName() + ":")) {

            // Initializing the full recording function
            startService(new Intent(this, RecordService.class));

        }
    }
}
