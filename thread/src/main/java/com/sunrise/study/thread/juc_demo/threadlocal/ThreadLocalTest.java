package com.sunrise.study.thread.juc_demo.threadlocal;

/**
 * @author huangzihua
 * @date 2021-09-08
 */
public class ThreadLocalTest {
    ThreadLocal<String> msg = new ThreadLocal<>();
    ThreadLocal<String> inMsg = new InheritableThreadLocal<>();

    public static void main(String[] args) {
        ThreadLocalTest test = new ThreadLocalTest();
        SayHello sayHello = new SayHello(test);

        test.inMsg.set("java");

        new Thread(sayHello, "001").start();
        new Thread(sayHello, "200").start();
    }
}

class SayHello implements Runnable {
    private ThreadLocalTest test;
    public SayHello(ThreadLocalTest test) {
        this.test = test;
    }

    @Override
    public void run() {
        String value = test.inMsg.get();
        String name = Thread.currentThread().getName();
        System.out.println(Thread.currentThread().getName() + value);
        test.inMsg.set(name);
        System.out.println(Thread.currentThread().getName() + test.inMsg.get());
    }
}