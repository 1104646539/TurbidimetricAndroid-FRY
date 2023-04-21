package com.wl.wllib;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 线程池控制
 */
public class ThreadUtil {
    /**
     * 串口com3接收数据 上下位机通讯
     */
    public static ExecutorService com3ReadDataThread = Executors.newSingleThreadExecutor();
    /**
     * 串口com3发送数据 上下位机通讯
     */
    public static ExecutorService com3SendDataThread = Executors.newSingleThreadExecutor();
    /**
     * 串口com4接收数据 打印机
     */
    public static ExecutorService com4ReadDataThread = Executors.newSingleThreadExecutor();
    /**
     * 串口com4发送数据 打印机
     */
    public static ExecutorService com4SendDataThread = Executors.newSingleThreadExecutor();
    /**
     * 一个任务新建一个线程
     */
    public static ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    /**
     * 所有任务公用一个线程
     */
    public static ExecutorService singleThreadPool = Executors.newSingleThreadExecutor();
    /**
     * 线程池支持定时或者周期性执行任务
     */
    public static ExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(5);
    /**
     * 线程池核心线程数为5，任务太多就等着
     */
    public static ExecutorService fixedThreadPool = Executors.newFixedThreadPool(5);

}
