package com.sunrise.study.thread.juc_demo.queue;

import java.util.concurrent.PriorityBlockingQueue;

/**
 * @author huangzihua
 * @date 2021-10-11
 */
public class PriorityBlockingQueueDemo {
    public static void main(String[] args) {
        PriorityBlockingQueue<String> queue = new PriorityBlockingQueue<>(1);
        queue.add("t");
        AddE addE1 = new AddE("Thread-A", queue);
        AddE addE2 = new AddE("Thread-B", queue);
        addE1.start();
        addE2.start();
        System.out.println("end");
    }

    private static class AddE extends Thread{
        private PriorityBlockingQueue queue;

        public AddE(String name, PriorityBlockingQueue queue) {
            super(name);
            this.queue = queue;
        }

        @Override
        public void run() {
            queue.add(getName());
        }
    }
}
