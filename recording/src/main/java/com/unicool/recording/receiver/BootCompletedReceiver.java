package com.unicool.recording.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.unicool.recording.service.RecordService;


/*
 *  @项目名：  RecordingAndEncryptedCompressed 
 *  @包名：    com.unicool.recording.receiver
 *  @文件名:   BootCompletedReceiver
 *  @创建者:   cjf
 *  @创建时间:  2017/10/23 10:38
 *  @描述：    
 */
public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            context.startService(new Intent(context, RecordService.class)
                    //Avoid being unable to receive broadcast after being forced to stop
                    .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES));
        }
    }
}
