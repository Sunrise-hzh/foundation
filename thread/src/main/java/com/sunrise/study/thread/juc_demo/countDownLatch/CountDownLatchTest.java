package com.sunrise.study.thread.juc_demo.countDownLatch;

import java.util.concurrent.CountDownLatch;

/**
 * @author huangzihua
 * @date 2021-10-20
 */
public class CountDownLatchTest {
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);

        System.out.println("in " + Thread.currentThread().getName() + "...");
        System.out.println("before latch.await()...");

        for (int i = 0; i < 3; i++) {
            new Thread("T" + i) {
                @Override
                public void run() {
                    System.out.println("enter Thread " + getName() + "...");
                    System.out.println("execute countdown ...");
                    latch.countDown();
                    System.out.println("exit Thread " + getName() + ".");
                }
            }.start();
        }
        latch.await();

        System.out.println("in " + Thread.currentThread().getName() + "...");
        System.out.println("after latch.await()...");
    }
}
