package com.sunrise.study.thread.aqs;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * @author huangzihua
 * @date 2021-08-05
 */
public class MyAQS extends AbstractQueuedSynchronizer {
    @Override
    protected boolean tryAcquire(int arg) {
        return super.tryAcquire(arg);
    }

    // 尝试释放
    @Override
    protected boolean tryRelease(int arg) {
        return super.tryRelease(arg);
    }

    @Override
    protected int tryAcquireShared(int arg) {
        return super.tryAcquireShared(arg);
    }

    @Override
    protected boolean tryReleaseShared(int arg) {
        return super.tryReleaseShared(arg);
    }

    @Override
    protected boolean isHeldExclusively() {
        return super.isHeldExclusively();
    }
}
