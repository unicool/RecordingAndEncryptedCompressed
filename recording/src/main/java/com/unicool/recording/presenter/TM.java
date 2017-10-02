package com.unicool.recording.presenter;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class TM {

    /**
     * AsyncTask的默认线程池Executor. 负责长时间的任务(网络访问) 默认3个线程
     */
    public static final Executor NETWORK_EXECUTOR =
            new ThreadPoolExecutor(3, 3, 5,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(),
                    Executors.defaultThreadFactory(),
                    new ThreadPoolExecutor.DiscardPolicy());
    // UI线程
    private static Handler mUIHandler;
    /**
     * 子线程的Handle, 只有一个线程, 可以执行比较快但不能在ui线程执行的操作
     * 文件读写不建议在此线程执行, 请使用FILE_THREAD_HANDLER
     * 此线程禁止进行网络操作, 如果需要进行网络操作, 请使用NETWORK_EXECUTOR
     */
    private static Handler SUB_THREAD_HANDLER;
    private static HandlerThread SUB_THREAD;

    /**
     * 文件读写线程的Handle, 只有一个线程, 可以执行文件读写操作, 如图片解码等
     * 此线程禁止进行网络操作, 如果需要进行网络操作, 请使用NETWORK_EXECUTOR
     */
    private static Handler FILE_THREAD_HANDLER;
    private static HandlerThread FILE_THREAD;


    /**
     * 取得UI线程Handler
     */
    public static Handler getMainHandler() {
        if (mUIHandler == null) {
            synchronized (TM.class) {
                if (mUIHandler == null) {
                    mUIHandler = new Handler(Looper.getMainLooper());
                }
            }
        }
        return mUIHandler;
    }

    public static void executeOnMainHandler(Runnable run) {
        getMainHandler().post(run);
    }

    public static void executeOnMainHandler(Runnable run, long delayed) {
        getMainHandler().postDelayed(run, delayed);
    }

    /**
     * 在网络线程上执行异步操作.
     * 该线程池负责网络请求等操作, 长时间的执行(如网络请求使用此方法执行)当然也可以执行其他线程, 和AsyncTask共用
     */
    public static void executeOnNetWorkThread(Runnable run) {
        try {
            NETWORK_EXECUTOR.execute(run);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获得副线程的Handler.
     * 副线程可以执行比较快但不能在ui线程执行的操作.
     * 另外, 文件读写建议放到FileThread中执行
     * 此线程禁止进行网络操作.如果需要进行网络操作, 请使用NETWORK_EXECUTOR
     */
    public static Handler getSubThreadHandler() {
        if (SUB_THREAD_HANDLER == null) {
            synchronized (TM.class) {
                SUB_THREAD = new HandlerThread("SUB_Th");
                SUB_THREAD.start();
                SUB_THREAD_HANDLER = new Handler(SUB_THREAD.getLooper());
            }
        }
        return SUB_THREAD_HANDLER;
    }

    public static Looper getSubThreadLooper() {
        return getSubThreadHandler().getLooper();
    }

    /**
     * 在副线程执行.
     * 可以执行本地文件读写等比较快但不能在ui线程执行的操作.
     * 此线程禁止进行网络操作.如果需要进行网络操作. 请使用NETWORK_EXECUTOR
     */
    public static void executeOnSubThread(Runnable run) {
        getSubThreadHandler().post(run);
    }

    public static void executeOnSubThread(Runnable run, long delayed) {
        getSubThreadHandler().postDelayed(run, delayed);
    }

    /**
     * 获得文件线程的Handler.
     * 副线程可以执行本地文件读写等比较快但不能在ui线程执行的操作.
     * 此线程禁止进行网络操作. 如果需要进行网络操作, 请使用NETWORK_EXECUTOR
     */
    public static Handler getFileThreadHandler() {
        if (FILE_THREAD_HANDLER == null) {
            synchronized (TM.class) {
                FILE_THREAD = new HandlerThread("FILE_Th");
                FILE_THREAD.start();
                FILE_THREAD_HANDLER = new Handler(FILE_THREAD.getLooper());
            }
        }
        return FILE_THREAD_HANDLER;
    }

    public static Looper getFileThreadLooper() {
        return getFileThreadHandler().getLooper();
    }

    public static void executeOnFileThread(Runnable run) {
        getFileThreadHandler().post(run);
    }

    public static void executeOnFileThread(Runnable run, long delayed) {
        getFileThreadHandler().postDelayed(run, delayed);
    }
}
