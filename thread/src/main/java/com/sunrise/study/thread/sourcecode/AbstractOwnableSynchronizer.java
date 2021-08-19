package com.sunrise.study.thread.sourcecode;

import java.io.Serializable;

/**
 * @author huangzihua
 * @date 2021-08-05
 */
public abstract class AbstractOwnableSynchronizer {

    // 构造函数
    protected AbstractOwnableSynchronizer() { }

    // 独占模式下的线程
    private transient Thread exclusiveOwnerThread;

    // 设置独占线程
    protected final void setExclusiveOwnerThread(Thread thread) {
        exclusiveOwnerThread = thread;
    }

    // 获取独占线程
    protected final Thread getExclusiveOwnerThread() {
        return exclusiveOwnerThread;
    }
}
