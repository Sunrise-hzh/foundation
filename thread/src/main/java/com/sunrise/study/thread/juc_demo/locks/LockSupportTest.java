package com.sunrise.study.thread.juc_demo.locks;

import java.util.concurrent.locks.LockSupport;

/**
 * @author huangzihua
 * @date 2021-09-15
 */
public class LockSupportTest {
    public static void main(String[] args) {

        Thread cur = Thread.currentThread();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("child thread unpark main-thread!!");
                LockSupport.unpark(cur);
            }
        }).start();

        System.out.println("first unpark");
        LockSupport.unpark(Thread.currentThread());

        System.out.println("second unpark");
        LockSupport.unpark(Thread.currentThread());

        System.out.println("first park!!");
        LockSupport.park();


        System.out.println("second park!!");
        LockSupport.park();



        System.out.println("main end!!");
    }
}
