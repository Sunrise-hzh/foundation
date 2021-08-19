package com.sunrise.study.thread.sourcecode;


/**
 * @author huangzihua
 * @date 2021-08-11
 */
public interface ReadWriteLock {
    /**
     * Returns the lock used for reading.
     *
     * @return the lock used for reading
     */
    Lock readLock();

    /**
     * Returns the lock used for writing.
     *
     * @return the lock used for writing
     */
    Lock writeLock();
}
