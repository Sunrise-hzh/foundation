package com.sunrise.study.thread.juc_demo.locks;

import com.sunrise.study.thread.sourcecode.locks.NonReentrantLock;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;

/**
 * @author huangzihua
 * @date 2021-09-16
 */
public class NonReentrantLockTest {
    static final NonReentrantLock lock = new NonReentrantLock();
    static final Condition notFull = lock.newCondition();
    static final Condition notEmpty = lock.newCondition();
    static final Queue<String> queue = new LinkedBlockingQueue<>();
    static final int queueSize = 10;

    public static void main(String[] args) {
        Thread producer = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    lock.lock();
                    try {
                        while (queue.size() == queueSize) {
                            notEmpty.await();
                        }
                        queue.add("ele");
                        printQueue("producer 添加 1");
                        notFull.signalAll();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        lock.unlock();
                    }
                }
            }
        });
        Thread consumer = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    lock.lock();
                    try {
                        while (queue.size() == 0) {
                            notFull.await();
                        }
                        String ele = queue.poll();
                        printQueue("consumer 消费 1");
                        notEmpty.signalAll();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        lock.unlock();
                    }
                }
            }
        });

        producer.start();
        consumer.start();
    }

    public static void printQueue(String msg) {
        System.out.println(msg);
        System.out.println(queue.toString());
    }
}
