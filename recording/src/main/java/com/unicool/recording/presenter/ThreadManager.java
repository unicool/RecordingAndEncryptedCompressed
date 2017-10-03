package com.unicool.recording.presenter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class ThreadManager {

    private static final ThreadPoolProxy mNormalPool = new ThreadPoolProxy(3, 3, 5 * 1000);
    private static final ThreadPoolProxy mSinglePool = new ThreadPoolProxy(1, 1, 5 * 1000);

    public static ThreadPoolProxy getNormalPool() {
        return mNormalPool;
    }

    public static ThreadPoolProxy getSinglePool() {
        return mSinglePool;
    }


    public static class ThreadPoolProxy {
        private final int mCorePoolSize;//核心线程池大小
        private final int mMaximumPoolSize;//最大线程池大小
        private final long mKeepAliveTime;//保持存活的时间 TimeUnit.MILLISECONDS
        private ThreadPoolExecutor mPool;

        public ThreadPoolProxy(int corePoolSize, int maximumPoolSize, long keepAliveTime) {
            this.mCorePoolSize = corePoolSize;
            this.mMaximumPoolSize = maximumPoolSize;
            this.mKeepAliveTime = keepAliveTime;

//            Executors.newCachedThreadPool();
//            Executors.newFixedThreadPool(10);
//            Executors.newScheduledThreadPool();
//            Executors.newSingleThreadExecutor();
        }

        private void initPool() {
            if (mPool == null || mPool.isShutdown()) {
                synchronized (this) {
                    if (mPool == null || mPool.isShutdown()) {
                        BlockingQueue<Runnable> workQueue;//阻塞队列
                        //workQueue = new ArrayBlockingQueue<Runnable>(3);//FIFO,大小有限制
                        workQueue = new LinkedBlockingQueue<Runnable>();//Integer.MAX_VALUE
                        //workQueue = new PriorityBlockingQueue<Runnable>();
                        //workQueue = new SynchronousQueue<Runnable>();//0

                        RejectedExecutionHandler handler;//异常捕获器
                        //handler = new ThreadPoolExecutor.AbortPolicy();//触发异常
                        handler = new ThreadPoolExecutor.DiscardPolicy();//丢弃任务，但是不抛出异常
                        //handler = new ThreadPoolExecutor.DiscardOldestPolicy();//丢弃队列最前面的任务，然后重新尝试执行任务（重复此过程）
                        //handler = new ThreadPoolExecutor.CallerRunsPolicy();//直接执行，不归线程池控制，在调用线程中执行，有可能在主线程中执行，所以特别小心

                        mPool = new ThreadPoolExecutor(mCorePoolSize,
                                mMaximumPoolSize,
                                mKeepAliveTime,
                                TimeUnit.MILLISECONDS,//单位
                                workQueue,
                                Executors.defaultThreadFactory(),//线程工厂
                                handler);
                    }
                }
            }
        }

        public void execute(Runnable task) {
            initPool();
            mPool.execute(task);
        }

        public Future<?> submit(Runnable task) {
            initPool();
            return mPool.submit(task);
        }

        public boolean remove(Runnable task) {
            return mPool != null && !mPool.isShutdown() && mPool.getQueue().remove(task);
        }

    }

}
