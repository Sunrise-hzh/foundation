package com.sunrise.study.thread.juc_demo.queue;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author huangzihua
 * @date 2021-09-28
 */
public class ConcurrentLinkedQueueDemo {
    public static void main(String[] args) throws InterruptedException {
        ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
        queue.add("a");
        queue.add("b");
        AddE addE = new AddE(queue);
        DelE delE = new DelE(queue);
        addE.start();
        delE.start();
        addE.join();
        delE.join();
        System.out.println("end");
    }

    static class AddE extends Thread {
        ConcurrentLinkedQueue<String> queue;
        public AddE(ConcurrentLinkedQueue<String> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            queue.add("c");
        }
    }
    static class DelE extends Thread {
        ConcurrentLinkedQueue<String> queue;
        public DelE(ConcurrentLinkedQueue<String> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            queue.poll();
        }
    }
}
