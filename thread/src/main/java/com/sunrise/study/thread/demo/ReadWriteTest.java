package com.sunrise.study.thread.demo;

import com.sunrise.study.thread.utils.AQSPrint;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author huangzihua
 * @date 2021-08-12
 */
public class ReadWriteTest {
    public static void main(String[] args) throws InterruptedException {
        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        ReadThread rt1 = new ReadThread("[RT1]", rwLock);
        ReadThread rt2 = new ReadThread("[RT2]", rwLock);
        ReadThread rt3 = new ReadThread("[RT3]", rwLock);
        WriteThread wt1 = new WriteThread("[WT1]", rwLock);
        WriteThread wt2 = new WriteThread("[WT2]", rwLock);
        rt1.start();
        rt2.start();
        wt1.start();
        Thread.sleep(5000);
        System.out.println("main end sleep");
        wt2.start();
        rt3.start();


    }
}

class ReadThread extends Thread {
    private ReentrantReadWriteLock rwLock;

    public ReadThread(String name, ReentrantReadWriteLock rwLock) {
        super(name);
        this.rwLock = rwLock;
    }

    public void run() {
        AQSPrint.outRRWL(getName() + " 尝试获取读read", rwLock);
        try {
            rwLock.readLock().lock();
            AQSPrint.outRRWL(getName() + " 获取read锁成功，进入睡眠", rwLock);
            Thread.sleep(10000);
            AQSPrint.outRRWL(getName() + " 睡眠结束", rwLock);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            AQSPrint.outRRWL(getName() + " 释放read锁", rwLock);
            rwLock.readLock().unlock();
        }
    }
}

class WriteThread extends Thread {
    private ReentrantReadWriteLock rwLock;

    public WriteThread(String name, ReentrantReadWriteLock rwLock) {
        super(name);
        this.rwLock = rwLock;
    }

    @Override
    public void run() {
        AQSPrint.outRRWL(getName() + " 尝试获取write锁", rwLock);
        try {
            rwLock.writeLock().lock();
            AQSPrint.outRRWL(getName() + " 获取write锁成功，进入睡眠", rwLock);
            Thread.sleep(3000);
            AQSPrint.outRRWL(getName() + " 睡眠结束", rwLock);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            AQSPrint.outRRWL(getName() + " 释放write锁", rwLock);
            rwLock.writeLock().unlock();
        }
    }
}
