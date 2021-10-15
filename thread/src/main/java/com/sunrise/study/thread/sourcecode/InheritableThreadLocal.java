package com.sunrise.study.thread.sourcecode;

/**
 * 继承了ThreadLocal类，重写了getMap()等方法
 * 实现了线程本地变量可传递给子线程。
 *
 * @author huangzihua
 * @date 2021-09-08
 */
public class InheritableThreadLocal<T> extends ThreadLocal<T> {
    /**
     * Computes the child's initial value for this inheritable thread-local
     * variable as a function of the parent's value at the time the child
     * thread is created.  This method is called from within the parent
     * thread before the child is started.
     * <p>
     * This method merely returns its input argument, and should be overridden
     * if a different behavior is desired.
     *
     * @param parentValue the parent thread's value
     * @return the child thread's initial value
     */
    protected T childValue(T parentValue) {
        return parentValue;
    }

    /**
     * Get the map associated with a ThreadLocal.
     *
     * @param t the current thread
     */
    ThreadLocalMap getMap(Thread t) {
//       return t.inheritableThreadLocals;      // 为保证项目能跑，先注释掉
        return null;
    }

    /**
     * Create the map associated with a ThreadLocal.
     *
     * @param t the current thread
     * @param firstValue value for the initial entry of the table.
     */
    void createMap(Thread t, T firstValue) {
//        t.inheritableThreadLocals = new ThreadLocalMap(this, firstValue);     // 为保证项目能跑，先注释掉
    }
}
