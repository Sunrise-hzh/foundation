package com.sunrise.study.thread.sourcecode.locks;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * 这是一个先进先出的锁，也就是只有队列的首元素可以获取锁。
 * @author huangzihua
 * @date 2021-09-15
 */
public class FIFOMutex {
    private final AtomicBoolean locked = new AtomicBoolean(false);
    private final Queue<Thread> waiters = new ConcurrentLinkedQueue<>();

    public void lock() {
        // 记录线程阻塞过程中的中断状态
        boolean wasInterrupted = false;
        Thread current = Thread.currentThread();
        waiters.add(current);
        // 只有队首的线程可以获取锁
        while (waiters.peek() != current || !locked.compareAndSet(false, true)) {
            // 不是队首线程，或cas调用失败，则挂起
            LockSupport.park(this);

            /*
            如果park方法是因为被中断而返回，则忽略中断，并且重置中断标志，
            做个标记，然后再次判断当前线程是不是队首元素或者当前锁是否已
            经被其他线程获取，如果是则继续调用park方法挂起自己
            */
            if (Thread.interrupted())
                wasInterrupted = true;
        }
        // 拿到锁之后，需要把当前线程从等待队列中移除
        waiters.remove();

        /*
            判断标记，如果标记为true则中断该线程，这个怎么理解呢？其实就
            是其他线程中断了该线程，虽然我对中断信号不感兴趣，忽略它，但
            是不代表其他线程对该标志不感兴趣，所以要恢复下。
        */
        if (wasInterrupted)
            // 恢复中断标志
            current.interrupt();
    }

    public void unlock() {
        locked.set(false);
        LockSupport.unpark(waiters.peek());
    }
}
