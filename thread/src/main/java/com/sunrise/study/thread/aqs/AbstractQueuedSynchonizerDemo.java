package com.sunrise.study.thread.aqs;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author huangzihua
 * @date 2021-08-06
 */
public class AbstractQueuedSynchonizerDemo  {
    public static void main(String[] args) {
        Lock lock = new ReentrantLock();

        MyThread t1 = new MyThread("t1", lock);
        MyThread t2 = new MyThread("t2", lock);
        t1.start();
        t2.start();
    }
}

class MyThread extends Thread {
    private Lock lock;

    public MyThread(String name, Lock lock) {
        super(name);
        this.lock = lock;
    }

    @Override
    public void run() {
        lock.lock();
        try {
            System.out.println(Thread.currentThread() + " running");
            Thread.sleep(2000);
            System.out.println(Thread.currentThread() + " sleep end.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}
