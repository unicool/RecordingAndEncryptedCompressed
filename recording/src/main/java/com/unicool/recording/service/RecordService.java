package com.unicool.recording.service;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;

import com.unicool.recording.R;
import com.unicool.recording.model.Itf;
import com.unicool.recording.presenter.ThreadManager;
import com.unicool.recording.util.CommonUtil;
import com.unicool.recording.util.DateUtil;
import com.unicool.recording.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;


/*
 *  @项目名：  NationalCar-Android 
 *  @包名：    com.uniool.recording.service
 *  @文件名:   RecordService
 *  @创建者:   cjf
 *  @创建时间:  2017/9/25 14:46
 *  @描述：    This is a separate process :recording
 */
public class RecordService extends Service {
    private static final String TAG = Itf.TAG;
    private static final int NOTIID = 237;
    private static final String SD_path = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final String TF_path = "/storage/sdcard1";
    private static final String TF_path_real = "/sdcard";
    public static boolean isRun = false;
    private static MediaRecorder mediaRecorder;
    private static MediaRecorder mediaRecorder_previous;
    private static boolean isPreRecordNeedStop = false;
    private String recording_path;
    private String file_path;
    private File currentDir;
    private String passWd;
    private BroadcastReceiver time_tick_receiver;
    private BroadcastReceiver media_receiver;
    private boolean TFCARD_EJECT = false;
    private boolean TFCARD_MOUNTED = false;

    @Override
    public void onCreate() {
        isRun = true;
        setupForeground();
        recording_path = "/" + this.getPackageName() + "/recording/" + CommonUtil.getAlias(this);
//        passWd = CommonUtil.getAlias(this);
        passWd = "test0123";
        // Register the time tick broadcast to turn on a new recording and end the previous one at every integral hour

        ThreadManager.getNormalPool().execute(new Runnable() {
            @Override
            public void run() {
                clearUpFiles();
                startTimedTask();
                initReceiveTask();
            }
        });
    }

    /**
     * Delete redundant or expired files, and set the save path for today's files
     */
    private synchronized void clearUpFiles() {
        long l0 = System.currentTimeMillis();
        boolean tfUsable = !TFCARD_EJECT && FileUtil.getExtSDCardPaths().size() > 1;
        Log.d(TAG, "tfUsable:" + tfUsable);
        Log.d(TAG, "Time to get the extrernal SD cards paths:" + (System.currentTimeMillis() - l0));
        String rootPath = tfUsable ? TF_path : SD_path;
        file_path = rootPath + recording_path;


        /////////////////////
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                String file_path = SD_path + recording_path;
//                String folderInzip = CompressUtil.addFolder2Zip(file_path + ".zip", CommonUtil.getAlias(RecordService.this), passWd);
//                Log.d(TAG, "folderInzip:" + folderInzip);
//                FileUtil.isFileExists(file_path + ".zip");
//                File[] zipFiles = FileUtil.haveZipFiles(new File(file_path).getParent());
//                Log.d(TAG, "zipFiles:\t" + Arrays.toString(zipFiles));
////                String zip = CompressUtil.zip(file_path + ".zip", new File(file_path).getParent(), passWd);
////                Log.d(TAG, "zip:\t" + zip);
//            }
//        }).start();


        /////////////////////

        File rDir = FileUtil.makeDirectory(file_path);
        if (rDir == null) {
            SystemClock.sleep(1000);
            Log.e(TAG, "\trDir = null");
            clearUpFiles();
            //throw new RuntimeException("FileUtil makeDirectory error:\n\t" + file_path);
            return;
        }

        // Delete recording files that exceeds the specified number of days
        while (rDir.list().length > Itf.RECORDING_DAYS) {
            boolean b = FileUtil.deleteOldestFiles(rDir, true);
            Log.w(TAG, "Delete expired files by day.\tresult:\t" + b);
            if (!b) break;
        }

        // Delete the oldest rDir, if space is not enough
        while (FileUtil.getSDFreeSize(rootPath) < FileUtil.THRESHOLD_WARNING_SPACE) {
            // TODO: 2017/9/28 Get the earliest files, unzip them, and then delete them one by one, fianlly compress again. 
            boolean b = FileUtil.deleteOldestFiles(rDir, false);
            Log.w(TAG, "Residual space exceeds threshold warning space, delete oldest files.\tresult:\t" + b);
            if (!b) break;
        }

        // Move(Cut) the recording files from SD card to the TF card by day
        while (tfUsable) {
            long freeSize = FileUtil.getSDFreeSize(rootPath);
            File newestFile = FileUtil.getOldestFiles(new File(SD_path + recording_path), true, true);
            if (newestFile == null) break;
            long filesSize = FileUtil.getFilesSize(newestFile);
            if (freeSize < filesSize + FileUtil.THRESHOLD_WARNING_SPACE) break;
            boolean b = FileUtil.moveFiles(SD_path + recording_path, TF_path + recording_path, newestFile.getName());
            Log.i(TAG, "Cut files by day:\t" + newestFile.toString() + "\tresult:\t" + b);
            if (!b) break;
        }

        file_path = file_path + "/" + DateUtil.getDate();
        currentDir = FileUtil.makeDirectory(file_path);
        Log.d(TAG, "Time to clear up files:" + (System.currentTimeMillis() - l0));
        long l2 = System.currentTimeMillis();
        // TODO: 2017/9/28 If there is a compression file, unzip it, then compress the folder 

        TFCARD_EJECT = TFCARD_MOUNTED = false;
    }

    /**
     * start record
     */
    private synchronized void startTimedTask() {
        mediaRecorder = new MediaRecorder();
        Log.i(TAG, "start record\t" + currentDir + "\t" + mediaRecorder);
        // 设置音频录入源
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        // 设置录制音频的输出格式
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
        // 设置音频的编码格式
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        // 设置录制音频文件输出文件路径
        mediaRecorder.setOutputFile(currentDir + "/" + DateUtil.getSimpleDateTimeNoSeparator() + ".mp3");
        // 最长录音时间 720分钟
        mediaRecorder.setMaxDuration(720 * 60 * 1000);
        //
        mediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {

            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                stopCurrentRecording();
                if (isPreRecordNeedStop) stopPreviousRecording();
                clearUpFiles();
                // Recording error, stop recording
                startTimedTask();
                Log.e(TAG, "An error occurred in the recorder:\t" + mr + "\t.what:" + what);
            }
        });
        // Prepare, and then start
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "An exception occurs when " + mediaRecorder + " starts to prepare.\t" + e.getMessage());
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            clearUpFiles();
            startTimedTask();
        }
    }

    /**
     * Save the previous recording object to stop the recording task in the front
     */
    private synchronized void swopRecorders() {
        if (mediaRecorder == null) return;
        isPreRecordNeedStop = true;
        mediaRecorder_previous = mediaRecorder;
    }

    /**
     * If recording, stop and release resources
     */
    private synchronized void stopPreviousRecording() {
        Log.i(TAG, "Stop recording\t" + currentDir + "\t" + mediaRecorder_previous);
        if (mediaRecorder_previous == null) return;
        if (TFCARD_EJECT) {
            mediaRecorder_previous.reset();
        } else {
            mediaRecorder_previous.stop();
        }
        mediaRecorder_previous.release();
        isPreRecordNeedStop = false;
        mediaRecorder_previous = null;
    }

    /**
     * stop and release current recording
     */
    private synchronized void stopCurrentRecording() {
        Log.i(TAG, "Stop Current Recording\t" + currentDir + "\t" + mediaRecorder);
        if (mediaRecorder == null) return;
        if (TFCARD_EJECT) {
            mediaRecorder.reset();
        } else {
            mediaRecorder.stop();
        }
        mediaRecorder.release();
        mediaRecorder = null;
    }

    private void encryptCompress() {

    }

    /**
     * initialze broadcast receiver to perform timed tasks.
     */
    private void initReceiveTask() {
        time_tick_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!intent.getAction().equals(Intent.ACTION_TIME_TICK)) return;
                // A task at each point in time
                if (!intent.getAction().equals(Intent.ACTION_TIME_TICK)) return;
                int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                int min = Calendar.getInstance().get(Calendar.MINUTE);
                if (hour == 0 && min == 0) {
                    clearUpFiles();
                }
                if (min == 0) {
                    if (TFCARD_EJECT || TFCARD_MOUNTED) { //TFCARD_EJECT = false
                        stopCurrentRecording();
                        if (isPreRecordNeedStop) stopPreviousRecording(); //false
                        clearUpFiles();
                    }
                    swopRecorders();
                    startTimedTask();
                    stopPreviousRecording();
                }
            }
        };
        IntentFilter time_tick_filter = new IntentFilter();
        time_tick_filter.addAction(Intent.ACTION_TIME_TICK);
        time_tick_filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        this.registerReceiver(time_tick_receiver, time_tick_filter);

        media_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.e(TAG, "media_receiver\t" + intent.getAction());
                if (intent.getAction().equals(Intent.ACTION_MEDIA_EJECT)) {
                    TFCARD_EJECT = true;
                    TFCARD_MOUNTED = false;
                    stopCurrentRecording();
                    if (isPreRecordNeedStop) stopPreviousRecording();
                    clearUpFiles();
                    startTimedTask();
                } else if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)) {
                    TFCARD_MOUNTED = true;
                    TFCARD_EJECT = false;
                }
            }
        };
        IntentFilter media_filter = new IntentFilter();
        media_filter.addAction(Intent.ACTION_MEDIA_EJECT);
        media_filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        media_filter.addDataScheme("file");
        media_filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        this.registerReceiver(media_receiver, media_filter);
    }

    /**
     * Setting up foreground service.
     */
    private void setupForeground() {
        Notification n = new Notification.Builder(this.getApplication())
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher))
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("RecordService")
                .setContentText("The recording service is ongoing")
                .setTicker("The whole recording of the equipment has been opened")
                .setPriority(Notification.PRIORITY_MAX)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(false)
                .setOngoing(true)
                .build();
        startForeground(NOTIID, n);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        isRun = false;
        this.unregisterReceiver(time_tick_receiver);
        this.unregisterReceiver(media_receiver);
        if (mediaRecorder != null) {
            Log.e(TAG, "End the recording of " + mediaRecorder);
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if (mediaRecorder_previous != null) {
            Log.e(TAG, "Stop the recording of " + mediaRecorder_previous);
            mediaRecorder_previous.stop();
            mediaRecorder_previous.release();
            mediaRecorder_previous = null;
        }
    }
}
