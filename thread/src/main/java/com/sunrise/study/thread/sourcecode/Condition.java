package com.sunrise.study.thread.sourcecode;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author huangzihua
 * @date 2021-08-05
 */
public interface Condition {
    // 等待，当前线程在接到信号或被中断之前一直处于等待状态
    void await() throws InterruptedException;

    // 等待，当前线程在接到信号之前一直处于等待状态，不响应中断
    void awaitUninterruptibly();

    //等待，当前线程在接到信号、被中断或到达指定等待时间之前一直处于等待状态
    long awaitNanos(long nanosTimeout) throws InterruptedException;

    // 等待，当前线程在接到信号、被中断或到达指定等待时间之前一直处于等待状态。此方法在行为上等效于: awaitNanos(unit.toNanos(time)) > 0
    boolean await(long time, TimeUnit unit) throws InterruptedException;

    // 等待，当前线程在接到信号、被中断或到达指定最后期限之前一直处于等待状态
    boolean awaitUntil(Date deadline) throws InterruptedException;

    // 唤醒一个等待线程。如果所有的线程都在等待此条件，则选择其中的一个唤醒。在从 await 返回之前，该线程必须重新获取锁。
    void signal();

    // 唤醒所有等待线程。如果所有的线程都在等待此条件，则唤醒所有线程。在从 await 返回之前，每个线程都必须重新获取锁。
    void signalAll();
}
