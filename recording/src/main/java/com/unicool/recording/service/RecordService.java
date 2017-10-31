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
import android.support.annotation.Nullable;
import android.util.Log;

import com.unicool.recording.R;
import com.unicool.recording.model.Itf;
import com.unicool.recording.presenter.ThreadManager;
import com.unicool.recording.util.CommonUtil;
import com.unicool.recording.util.CompressUtil;
import com.unicool.recording.util.DateUtil;
import com.unicool.recording.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
    private static final String SD_path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    private static final String TF_path = "/storage/sdcard1/";
    private static final String TF_path_real = "/sdcard/";
    public static boolean isRun = false;
    private static MediaRecorder mediaRecorder;
    private static MediaRecorder mediaRecorder_previous;
    private static boolean isPreRecordNeedStop = false;
    /**
     * Intermediate path = PackageName + "/recording/temp/";
     */
    private String recording_path;
    /**
     * The file path being recorded
     */
    private File currentDir;
    /**
     * The current compression package path = ./Alias + /day.zip
     */
    private String zipFile;
    private String passWd;
    private BroadcastReceiver time_tick_receiver;
    private BroadcastReceiver media_receiver;
    private boolean TFCARD_EJECT = false;
    private boolean TFCARD_MOUNTED = false;

    @Override
    public void onCreate() {
        isRun = true;
        setupForeground();
        recording_path = this.getPackageName() + "/recording/temp/";
        passWd = CommonUtil.getAlias(this);
        // Register the time tick broadcast to turn on a new recording and end the previous one at every integral hour

        ThreadManager.getNormalPool().execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "NormalPool\n\tThread.currentThread():\n\t" + Thread.currentThread());
                updateFile();
                startTimedTask();
                initReceiveTask();
            }
        });
        ThreadManager.getSinglePool().execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "SinglePool\n\tThread.currentThread():\n\t" + Thread.currentThread());
                cleanupAndEncryptCompress();
            }
        });
    }

    /**
     * Delete redundant or expired files, and set the save path for today's files
     */
    private synchronized void updateFile() {
        long l0 = System.currentTimeMillis();
        boolean tfUsable = !TFCARD_EJECT && FileUtil.getExtSDCardPaths().size() > 1;
        Log.v(TAG, "tfUsable:" + tfUsable);
        Log.v(TAG, "Time to get the extrernal SD cards paths:" + (System.currentTimeMillis() - l0));
        String sdPath = tfUsable ? TF_path : SD_path;
        currentDir = FileUtil.makeDirectory(sdPath + recording_path + DateUtil.getDate());
        if (currentDir == null) {
            Log.e(TAG, "\tcurrentDir = null");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            updateFile();
            return;
        }
        TFCARD_EJECT = TFCARD_MOUNTED = false;

        long totalSize = FileUtil.getSDTotalSize(sdPath);
        if (totalSize > 0) FileUtil.THRESHOLD_WARNING_SPACE = totalSize / 10;
        Log.v(TAG, "Time to clear up files:" + (System.currentTimeMillis() - l0));
    }

    /**
     * start record
     */
    private void startTimedTask() {
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
                updateFile();
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
            updateFile();
            startTimedTask();
        }
    }

    /**
     * Save the previous recording object to stop the recording task in the front.
     * Needs to be invoked before {@link #startTimedTask()},
     * {@link #stopPreviousRecording()} needs to be invoked after this method.
     */
    private synchronized void swopRecorders() {
        if (mediaRecorder == null) return;
        isPreRecordNeedStop = true;
        mediaRecorder_previous = mediaRecorder;
    }

    /**
     * If recording, stop and release resources
     */
    private void stopPreviousRecording() {
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

        ThreadManager.getSinglePool().execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "SinglePool\n\tThread.currentThread():\n\t" + Thread.currentThread());
                cleanupAndEncryptCompress();
            }
        });
    }

    /**
     * stop and release current recording
     */
    private void stopCurrentRecording() {
        Log.i(TAG, "Stop Current Recording\t" + currentDir + "\t" + mediaRecorder);
        if (mediaRecorder == null) return;
        if (TFCARD_EJECT) {
            mediaRecorder.reset();
        } else {
            mediaRecorder.stop();
        }
        mediaRecorder.release();
        mediaRecorder = null;

        // encrypt and compress 
        File currentFile = FileUtil.getOldestFiles(currentDir.getParentFile(), true, true);
        while (currentFile != null && currentFile.isDirectory()) { //not null, not directory
            currentFile = FileUtil.getOldestFiles(currentFile, true, true);
        }
        if (currentFile == null) return;
        ArrayList<File> files = new ArrayList<>();
        files.add(currentFile);
        CompressUtil.addFilesToFolderInZip(zipFile,
                CommonUtil.getAlias(this) + File.separator + currentDir.getName(), files, passWd);
        FileUtil.delFiles(currentFile.getAbsolutePath());
    }

    private void cleanupAndEncryptCompress() {
        long l2 = System.currentTimeMillis();
        boolean tfUsable = !TFCARD_EJECT && FileUtil.getExtSDCardPaths().size() > 1;
        Log.d(TAG, "tfUsable:" + tfUsable);
        String sdPath = tfUsable ? TF_path : SD_path;
        File rootDir = FileUtil.makeDirectory(sdPath + recording_path);
        if (rootDir == null) {
            Log.e(TAG, "\trootDir = null");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            cleanupAndEncryptCompress();
            return;
        }

        // Delete recording days that exceeds the specified number of days
        while (rootDir.list().length > Itf.RECORDING_DAYS) {
            boolean b = FileUtil.deleteOldestFiles(rootDir, true);
            Log.w(TAG, "Delete expired files by day.\tresult:\t" + b);
            if (!b) break;
        }

        // Delete the oldest rootDir, if space is not enough
        while (FileUtil.getSDFreeSize(sdPath) < FileUtil.THRESHOLD_WARNING_SPACE) {
            boolean b = FileUtil.deleteOldestFiles(rootDir, false);
            Log.w(TAG, "Residual space exceeds threshold warning space, delete oldest days.\tresult:\t" + b);
            if (!b) break;
        }

        // Move(Cut) the recording days from SD card to the TF card by day
        while (tfUsable) {
            long freeSize = FileUtil.getSDFreeSize(sdPath);
            File newestFile = FileUtil.getOldestFiles(new File(SD_path + recording_path), true, true);
            if (newestFile == null) break;
            long filesSize = FileUtil.getFilesSize(newestFile);
            if (freeSize < filesSize + FileUtil.THRESHOLD_WARNING_SPACE) break;
            boolean b = FileUtil.moveFiles(SD_path + recording_path, TF_path + recording_path, newestFile.getName());
            Log.w(TAG, "Cut days by day:\t" + newestFile.toString() + "\tresult:\t" + b);
            if (!b) break;
        }


        //******  Compress the root folder into a compressed package file  ******/


        while (isPreRecordNeedStop || currentDir == null) { //Prevent two recording files in progress
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        File currentMP3 = FileUtil.getOldestFiles(currentDir.getParentFile(), true, true);
        while (currentMP3 != null && currentMP3.isDirectory()) { //not null
            currentMP3 = FileUtil.getOldestFiles(currentMP3, true, true);
        }
        Log.i(TAG, "currentMP3:" + currentMP3);
        if (currentMP3 == null) return;

        // Compress the root folder into a compressed package file

        String zipParent = sdPath + this.getPackageName() + "/recording/" + CommonUtil.getAlias(this) + "/";
        File[] dirs = rootDir.listFiles();
        if (dirs == null || dirs.length == 0) return;
        for (File d : dirs) { //temp/../...mp3
            zipFile = zipParent + d.getName() + ".zip";
            if (!FileUtil.isFileExists(zipFile) || !CompressUtil.isZip4jLegal(zipFile)) {
                FileUtil.delFiles(zipFile);
                Log.e(TAG, "delFiles zipFile  =  " + zipFile);
            }
            zipFile = CompressUtil.zip(d.getAbsolutePath(), zipParent, passWd);
            Log.v(TAG, "\tzipFile:\n\t" + zipFile);
            if (zipFile == null) {
                Log.e(TAG, "\tzipFile = null");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                cleanupAndEncryptCompress();
                return;
            }
            if (!currentDir.getAbsolutePath().equals(d.getAbsolutePath()) || d.isFile()) {
                FileUtil.delFiles(d.getAbsolutePath());
            } else {
                for (File mp3 : d.listFiles()) {
                    if (mp3.equals(currentMP3)) {
                        continue;
                    }
                    FileUtil.delFiles(mp3.getAbsolutePath());
                    Log.w(TAG, "delete file after compress\t" + mp3.getAbsolutePath());
                }
                if (d.list() != null && d.list().length == 0) {
                    FileUtil.delFiles(d.getAbsolutePath());
                }
            }
        }


        // Delete recording days that exceeds the specified number of days
        File zipDirs = FileUtil.makeDirectory(zipParent);
        if (zipDirs == null) return;
        while (zipDirs.list().length > Itf.RECORDING_DAYS) {
            boolean b = FileUtil.deleteOldestFiles(zipDirs, true);
            Log.w(TAG, "Delete expired compressed files by day.\tresult:\t" + b);
            if (!b) break;
        }

        // Delete the oldest rootDir, if space is not enough
        while (FileUtil.getSDFreeSize(sdPath) < FileUtil.THRESHOLD_WARNING_SPACE) {
            File oldestFiles = FileUtil.getOldestFiles(zipDirs, false, true);
            if (oldestFiles == null) return;
            boolean b = CompressUtil.isZip4jLegal(oldestFiles.getAbsolutePath());
            Log.w(TAG, "Residual space exceeds threshold warning space, delete oldest files." + b);
            if (!b) return;
            CompressUtil.removeOldestFileFromZipArchive(oldestFiles.getAbsolutePath(), passWd, true);
        }

        // TODO: 2017/10/3 cut file 

        Log.v(TAG, "Time to finish compress:" + (System.currentTimeMillis() - l2));
    }

    /**
     * initialze broadcast receiver to perform timed tasks.
     */
    private void initReceiveTask() {
        time_tick_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // A task at each point in time
                if (!intent.getAction().equals(Intent.ACTION_TIME_TICK)) return;
                int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                int min = Calendar.getInstance().get(Calendar.MINUTE);
                if (hour == 0 && min == 0) {
                    updateFile();
                }
                if (min == 0) {
                    if (TFCARD_EJECT || TFCARD_MOUNTED) { //TFCARD_EJECT = false
                        stopCurrentRecording();
                        if (isPreRecordNeedStop) stopPreviousRecording(); //false
                        updateFile();
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
                    updateFile();
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
