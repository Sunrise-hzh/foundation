package com.sunrise.study.thread.juc_demo.locks;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 分析ReentrantReadWriteLock的读写锁
 * @author huangzihua
 * @date 2021-09-17
 */
public class ReentrantReadWriteLockTest {
    static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    static ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    static ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

    public static void main(String[] args) throws InterruptedException {
        Writer writer = new Writer("Writer-1");
        Reader reader = new Reader("Reader-1");
        Reader reader2 = new Reader("Reader-2");
        Reader reader3 = new Reader("Reader-3");
        writer.start();
        reader.start();
        reader2.start();
        reader3.start();
        writer.join();
        reader.join();
        reader2.join();
        reader3.join();
        System.out.println("main end!");
    }


    static class Reader extends Thread {

        public Reader(String name) {
            super(name);
        }

        @Override
        public void run() {
            readLock.lock();
            try {
                System.out.println(getName() + " ： Get Read-Lock");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                readLock.unlock();
                System.out.println(getName() + " ： Release Read-Lock");
            }
        }
    }

    static class Writer extends Thread {

        public Writer(String name) {
            super(name);
        }

        @Override
        public void run() {
            writeLock.lock();
            try {
                System.out.println(getName() + " ： Get Write-Lock");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                writeLock.unlock();
                System.out.println(getName() + " ： Release Write-Lock");
            }
        }
    }
}


