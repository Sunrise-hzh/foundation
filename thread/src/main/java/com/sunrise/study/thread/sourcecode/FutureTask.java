package com.sunrise.study.thread.sourcecode;

import com.sunrise.study.thread.sourcecode.locks.LockSupport;

import java.util.concurrent.*;

/**
 * Future 表示了一个任务的生命周期，是一个可取消的异步运算，可以把它看作是一个异步操作的结果的占位符，
 * 它将在未来的某个时刻完成，并提供对其结果的访问。在并发包中许多异步任务类都继承自Future，其中最典型的就是 FutureTask。
 *
 * FutureTask简介
 *   FutureTask 为 Future 提供了基础实现，如获取任务执行结果(get)和取消任务(cancel)等。如果任务尚未完成，
 *   获取任务执行结果时将会阻塞。一旦执行结束，任务就不能被重启或取消(除非使用runAndReset执行计算)。
 *   FutureTask 常用来封装 Callable 和 Runnable，也可以作为一个任务提交到线程池中执行。除了作为一个独立的
 *   类之外，此类也提供了一些功能性函数供我们创建自定义 task 类使用。FutureTask 的线程安全由CAS来保证。
 *
 * A cancellable asynchronous computation.  This class provides a base
 * implementation of {@link Future}, with methods to start and cancel
 * a computation, query to see if the computation is complete, and
 * retrieve the result of the computation.  The result can only be
 * retrieved when the computation has completed; the {@code get}
 * methods will block if the computation has not yet completed.  Once
 * the computation has completed, the computation cannot be restarted
 * or cancelled (unless the computation is invoked using
 * {@link #runAndReset}).
 *
 * <p>A {@code FutureTask} can be used to wrap a {@link Callable} or
 * {@link Runnable} object.  Because {@code FutureTask} implements
 * {@code Runnable}, a {@code FutureTask} can be submitted to an
 * {@link Executor} for execution.
 *
 * <p>In addition to serving as a standalone class, this class provides
 * {@code protected} functionality that may be useful when creating
 * customized task classes.
 *
 * @since 1.5
 * @author Doug Lea
 * @param <V> The result type returned by this FutureTask's {@code get} methods
 */
public class FutureTask<V> implements RunnableFuture<V> {
    /*
     * Revision notes: This differs from previous versions of this
     * class that relied on AbstractQueuedSynchronizer, mainly to
     * avoid surprising users about retaining interrupt status during
     * cancellation races. Sync control in the current design relies
     * on a "state" field updated via CAS to track completion, along
     * with a simple Treiber stack to hold waiting threads.
     *
     * Style note: As usual, we bypass overhead of using
     * AtomicXFieldUpdaters and instead directly use Unsafe intrinsics.
     */

    /**
     * 任务状态
     * The run state of this task, initially NEW.
     * The run state transitions to a terminal state only in methods set,
     * setException, and cancel.
     * During completion, state may take on transient values of COMPLETING (while outcome is being set)
     * or INTERRUPTING (only while interrupting the runner to satisfy a cancel(true)).
     * Transitions from these intermediate to final states use cheaper ordered/lazy writes
     * because values are unique and cannot be further modified.
     *
     * Possible state transitions:
     * NEW -> COMPLETING -> NORMAL
     * NEW -> COMPLETING -> EXCEPTIONAL
     * NEW -> CANCELLED
     * NEW -> INTERRUPTING -> INTERRUPTED
     */
    private volatile int state;
    /*
    * NEW          ：表示是一个新的任务或者还没被执行完的任务。这是初始状态。
    * COMPLETING   ：任务已经执行完成或者执行任务的时候发生异常，但是任务执行结果或者异常原因
    *                还没有保存到outcome字段(outcome字段用来保存任务执行结果，如果发生异常，
    *                则用来保存异常原因)的时候，状态会从NEW变更到COMPLETING。但是这个状态存
    *                在时间会比较短，属于中间状态。
    * NORMAL       ：任务已经执行完成并且任务执行结果已经保存到outcome字段，状态会从COMPLETING转换到NORMAL。这是一个最终态。
    * EXCEPTIONAL  ：任务执行发生异常并且异常原因已经保存到outcome字段中后，状态会从COMPLETING转换到EXCEPTIONAL。这是一个最终态。
    * CANCELLED    ：任务还没开始执行或者已经开始执行但是还没有执行完成的时候，用户调用了
    *                cancel(false)方法取消任务且不中断任务执行线程，这个时候状态会从NEW转化为CANCELLED状态。这是一个最终态。
    * INTERRUPTING ：任务还没开始执行或者已经开始执行但是还没有执行完成的时候，用户调用了
    *                cancel(true)方法取消任务并且要中断任务执行线程但是还没有中断任务执
    *                行线程之前，状态会从NEW转化为INTERRUPTING。这是一个中间状态。
    * INTERRUPTED  ：调用interrupt()中断任务执行线程之后状态会从INTERRUPTING转换到INTERRUPTED。
    *                这是一个最终态。有一点需要注意的是，所有值大于COMPLETING的状态都表示任务已经
    *                执行完成(任务正常执行完成，任务执行异常或者任务被取消)。
    */
    private static final int NEW          = 0;
    private static final int COMPLETING   = 1;
    private static final int NORMAL       = 2;
    private static final int EXCEPTIONAL  = 3;
    private static final int CANCELLED    = 4;
    private static final int INTERRUPTING = 5;
    private static final int INTERRUPTED  = 6;

    /**
     * 内部持有的callable任务，运行结束后会被置为null。
     * FutureTask新构建时，必须赋值
     * The underlying callable; nulled out after running
     */
    private Callable<V> callable;

    /**
     * 保存从get()方法返回的结果或者抛出的异常
     * The result to return or exception to throw from get()
     */
    private Object outcome; // non-volatile, protected by state reads/writes

    /**
     * 运行 callable 的线程
     * 构建当前任务时不会初始化，默认值为null。当执行run()方法时，会通过CAS操作替换为当前线程。
     * The thread running the callable; CASed during run()
     */
    private volatile Thread runner;

    /**
     * 使用 Treiber 栈保存等待线程
     * Treiber stack of waiting threads
     */
    private volatile WaitNode waiters;

    /**
     * 返回执行结果或异常
     * Returns result or throws exception for completed task.
     *
     * @param s completed state value
     */
    @SuppressWarnings("unchecked")
    private V report(int s) throws ExecutionException {
        Object x = outcome;
        // 如果 state 为 NORMAL，说明正常结束，则转换类型后返回
        if (s == NORMAL)
            return (V)x;
        // 如果 state>=CANCELLED，说明任务被取消或中断，则抛出CancellationException异常
        if (s >= CANCELLED)
            throw new CancellationException();
        // 除去以上情况，剩下只有是EXCEPTIONAL状态，则抛出保存的异常
        throw new ExecutionException((Throwable)x);
    }

    /**
     * Creates a {@code FutureTask} that will, upon running, execute the
     * given {@code Callable}.
     *
     * @param  callable the callable task
     * @throws NullPointerException if the callable is null
     */
    public FutureTask(Callable<V> callable) {
        if (callable == null)
            throw new NullPointerException();
        this.callable = callable;
        this.state = NEW;       // ensure visibility of callable
    }

    /**
     * Creates a {@code FutureTask} that will, upon running, execute the
     * given {@code Runnable}, and arrange that {@code get} will return the
     * given result on successful completion.
     *
     * @param runnable the runnable task
     * @param result the result to return on successful completion. If
     * you don't need a particular result, consider using
     * constructions of the form:
     * {@code Future<?> f = new FutureTask<Void>(runnable, null)}
     * @throws NullPointerException if the runnable is null
     */
    public FutureTask(Runnable runnable, V result) {
        this.callable = Executors.callable(runnable, result);
        this.state = NEW;       // ensure visibility of callable
    }

    /**
     * 判断任务是否被取消或被中断
     * @return true表示任务已被取消或被中断
     */
    public boolean isCancelled() {
        return state >= CANCELLED;
    }

    /**
     * 判断任务是否开始运行
     * @return true任务正在运行，false任务还没开始执行
     */
    public boolean isDone() {
        return state != NEW;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        // 如果state==NEW，则通过CAS将state值由NEW修改为
        //   - INTERRUPTING (当mayInterruptIfRunning为true时)；
        //   - 或 CANCELLED (当mayInterruptIfRunning为false时)。
        // 如果state!=NEW，或CAS更新失败，则任务取消失败，返回false。
        if (!(state == NEW &&
              UNSAFE.compareAndSwapInt(this, stateOffset, NEW,
                  mayInterruptIfRunning ? INTERRUPTING : CANCELLED)))
            return false;

        // 如果前面CAS执行成功，则执行下面的代码
        try {    // in case call to interrupt throws exception
            // 根据mayInterruptIfRunning，决定是否对运行中的任务进行中断操作
            if (mayInterruptIfRunning) {
                try {
                    Thread t = runner;
                    if (t != null)
                        t.interrupt();  // 中断任务
                } finally { // final state
                    // 最后设置任务的状态 state 为 INTERRUPTED
                    UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTED);
                }
            }
        } finally {
            // 调用finishCompletion()，移除并唤醒所有等待线程
            finishCompletion();
        }
        return true;
    }

    /**
     * 获取执行结果
     * @throws CancellationException {@inheritDoc}
     */
    public V get() throws InterruptedException, ExecutionException {
        int s = state;
        // 任务还没执行完成，则调用awaitDone()方法等待任务完成
        if (s <= COMPLETING)
            // 如果任务处于 NEW 或 COMPLETING 状态，则调用awaitDone()方法，等待任务完成
            s = awaitDone(false, 0L);
        // 任务完成后，通过report()方法获取执行结果或异常
        return report(s);
    }

    /**
     * 获取执行结果，可设定等待时间
     * @throws CancellationException {@inheritDoc}
     */
    public V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        if (unit == null)
            throw new NullPointerException();
        int s = state;
        // s<=COMPLETING，说明任务还没运行或正在运行，则调用awaitDone()方法进入限时挂起
        // 如果awaitDone()返回值仍然小于COMPLETING，说明等待超时了，则抛出超时异常
        if (s <= COMPLETING &&
            (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING)
            throw new TimeoutException();
        return report(s);
    }

    /**
     * 空方法，只会在finishCompletion()方法中被调用，可由子类实现
     * Protected method invoked when this task transitions to state
     * {@code isDone} (whether normally or via cancellation). The
     * default implementation does nothing.  Subclasses may override
     * this method to invoke completion callbacks or perform
     * bookkeeping. Note that you can query status inside the
     * implementation of this method to determine whether this task
     * has been cancelled.
     */
    protected void done() { }

    /**
     * 设置任务完成后的outcome和state
     * Sets the result of this future to the given value unless
     * this future has already been set or has been cancelled.
     *
     * <p>This method is invoked internally by the {@link #run} method
     * upon successful completion of the computation.
     *
     * @param v the value
     */
    protected void set(V v) {
        // 通过CAS设置state的值，由 NEW 改为 COMPLETING
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            // 若前面CAS执行成功，则这里就保存任务的计算结果
            outcome = v;
            // 同时将state设为 NORMAL
            UNSAFE.putOrderedInt(this, stateOffset, NORMAL); // final state
            // 执行完，唤醒等待线程
            finishCompletion();
        }
    }

    /**
     * 设置抛出异常后的outcome和state
     * Causes this future to report an {@link ExecutionException}
     * with the given throwable as its cause, unless this future has
     * already been set or has been cancelled.
     *
     * <p>This method is invoked internally by the {@link #run} method
     * upon failure of the computation.
     *
     * @param t the cause of failure
     */
    protected void setException(Throwable t) {
        // 通过CAS设置state的值，由 NEW 改为 COMPLETING
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            // 若前面CAS执行成功，则这里就保存发生的异常
            outcome = t;
            // 同时将state设为 EXCEPTIONAL
            UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL); // final state
            // 执行完，唤醒等待线程
            finishCompletion();
        }
    }

    /**
     * 运行任务
     */
    public void run() {

        // 如果当前任务的运行状态不是NEW，则直接返回
        // 如果当前任务的状态是NEW，但是CAS替换runner属性时失败，也直接返回
        // 如果当前任务的状态是NEW，且CAS成功替换runner为当前调用的线程，则往下执行
        if (state != NEW ||
            !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                         null, Thread.currentThread()))
            return;

        try {
            // 取要执行的任务
            Callable<V> c = callable;
            // 当任务不为null，且状态为NEW时，执行下面代码
            if (c != null && state == NEW) {
                V result;
                boolean ran;
                try {
                    // 执行c的call()方法
                    result = c.call();
                    ran = true;
                } catch (Throwable ex) {
                    // 异常处理
                    result = null;
                    ran = false;
                    setException(ex);  // 设置EXCEPTIONAL状态，并保存异常对象
                }
                if (ran)
                    set(result);  // 设置NORMAL状态，并保存计算结果
            }
        } finally {
            // 最后重置runner属性
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            runner = null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            int s = state;
            // 如果线程被中断，则调用handlePossibleCancellationInterrupt()方法处理中断
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }
    }

    /**
     * Executes the computation without setting its result, and then
     * resets this future to initial state, failing to do so if the
     * computation encounters an exception or is cancelled.  This is
     * designed for use with tasks that intrinsically execute more
     * than once.
     *
     * @return {@code true} if successfully run and reset
     */
    protected boolean runAndReset() {
        if (state != NEW ||
            !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                         null, Thread.currentThread()))
            return false;
        boolean ran = false;
        int s = state;
        try {
            Callable<V> c = callable;
            if (c != null && s == NEW) {
                try {
                    c.call(); // don't set result
                    ran = true;
                } catch (Throwable ex) {
                    setException(ex);
                }
            }
        } finally {
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            runner = null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            s = state;
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }
        return ran && s == NEW;
    }

    /**
     * Ensures that any interrupt from a possible cancel(true) is only
     * delivered to a task while in run or runAndReset.
     */
    private void handlePossibleCancellationInterrupt(int s) {
        // 在中断者中断线程之前可能会延迟，所以我们只需要让出CPU时间片自旋等待
        // It is possible for our interrupter to stall before getting a
        // chance to interrupt us.  Let's spin-wait patiently.
        if (s == INTERRUPTING)
            while (state == INTERRUPTING)
                Thread.yield(); // wait out pending interrupt

        // assert state == INTERRUPTED;

        // We want to clear any interrupt we may have received from
        // cancel(true).  However, it is permissible to use interrupts
        // as an independent mechanism for a task to communicate with
        // its caller, and there is no way to clear only the
        // cancellation interrupt.
        //
        // Thread.interrupted();
    }

    /**
     * 用于在 Treiber 栈中的节点。
     * 关于 Treiber栈 详情可了解 Phaser类和 SynchronousQueue类。
     * Simple linked list nodes to record waiting threads in a Treiber
     * stack.  See other classes such as Phaser and SynchronousQueue
     * for more detailed explanation.
     */
    static final class WaitNode {
        volatile Thread thread;
        volatile WaitNode next;
        WaitNode() { thread = Thread.currentThread(); }
    }

    /**
     * 移除并唤醒所有等待线程，然后调用done()，最后重置callable属性为null
     * Removes and signals all waiting threads, invokes done(), and
     * nulls out callable.
     */
    private void finishCompletion() {
        // assert state > COMPLETING;
        // 遍历等待线程，将后续所有等待线程都唤醒
        for (WaitNode q; (q = waiters) != null;) {
            // 移除等待线程
            if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) {
                for (;;) {
                    Thread t = q.thread;
                    if (t != null) {
                        q.thread = null;
                        // 唤醒线程
                        LockSupport.unpark(t);
                    }
                    // 移除当前线程并获取下一个等待线程
                    WaitNode next = q.next;
                    if (next == null)
                        break;
                    q.next = null; // unlink to help gc
                    q = next;
                }
                break;
            }
        }

        // 任务完成后调用done()方法，可自定义实现
        done();
        // 重置为null
        callable = null;        // to reduce footprint
    }

    /**
     * 等待任务完成
     * Awaits completion or aborts on interrupt or timeout.
     *
     * @param timed true if use timed waits
     * @param nanos time to wait, if timed
     * @return state upon completion
     */
    private int awaitDone(boolean timed, long nanos)
        throws InterruptedException {

        final long deadline = timed ? System.nanoTime() + nanos : 0L;
        WaitNode q = null;
        boolean queued = false;
        // 死循环，在这自旋
        for (;;) {
            // 获取并重置线程中断状态
            if (Thread.interrupted()) {
                // 如果线程已被中断，则调用removeWaiter()方法，移除等待WaitNode
                removeWaiter(q);
                // 抛出中断异常
                throw new InterruptedException();
            }

            int s = state;
            // 如果当前状态为结束状态(s>COMPLETING)，则根据需要置空等待节点的thread，并返回状态s
            if (s > COMPLETING) {
                // 若 q!=null，则重置q.thread为null
                if (q != null)
                    q.thread = null;
                // 返回状态值
                return s;
            }

            // 如果任务还没完成，则让出CPU
            else if (s == COMPLETING) // cannot time out yet
                Thread.yield();

            // 如果state处于NEW状态，且q==null，说明是第一遍循环，则先创建一个WaitNode节点
            else if (q == null)
                q = new WaitNode();

            // 如果s==NEW，且q!=null，且!queued，说明是第二遍循环，且任务还没开始执行，
            // 则通过CAS将当前waiters替换为当前节点q，并且设q.next指向原先的waiters（也就是头插法）。
            else if (!queued)
                queued = UNSAFE.compareAndSwapObject(this, waitersOffset,
                                                     q.next = waiters, q);

            // 如果s==NEW，且q!=null，且queued为true，说明当前线程已在等待队列中，
            // 且timed为true，则说明是限时挂起
            else if (timed) {
                nanos = deadline - System.nanoTime();
                if (nanos <= 0L) {
                    removeWaiter(q);
                    return state;
                }
                LockSupport.parkNanos(this, nanos);
            }
            // 如果s==NEW，且q!=null，且queued为true，且timed为false，说明是不限时挂起
            else
                LockSupport.park(this);
        }
    }

    /**
     * 移除指定等待节点node
     * Tries to unlink a timed-out or interrupted wait node to avoid
     * accumulating garbage.  Internal nodes are simply unspliced
     * without CAS since it is harmless if they are traversed anyway
     * by releasers.  To avoid effects of unsplicing from already
     * removed nodes, the list is retraversed in case of an apparent
     * race.  This is slow when there are a lot of nodes, but we don't
     * expect lists to be long enough to outweigh higher-overhead
     * schemes.
     */
    private void removeWaiter(WaitNode node) {
        if (node != null) {
            // 先断开thread绑定的线程，下面的循环就是移除thread为null的节点
            node.thread = null;
            // 双循环
            retry:
            for (;;) {          // restart on removeWaiter race
                // 不断遍历下一个等待节点
                for (WaitNode pred = null, q = waiters, s; q != null; q = s) {
                    s = q.next;

                    // 如果thread不为null，则保存为pred，并进入下一轮循环
                    if (q.thread != null)
                        pred = q;

                    // 如果thread为null，且pred不为null，则移除当前等待节点q
                    else if (pred != null) {
                        pred.next = s;
                        // 如果pred被移除了，则跳到最外层循环重新开始遍历
                        if (pred.thread == null) // check for race
                            continue retry;
                    }

                    // 如果thread为null，且pred为null，说明是头节点，则通过CAS将当前节点替换为下一节点
                    else if (!UNSAFE.compareAndSwapObject(this, waitersOffset,
                                                          q, s))
                        // CAS执行失败，则从外层循环开始
                        continue retry;
                }
                break;
            }
        }
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long stateOffset;
    private static final long runnerOffset;
    private static final long waitersOffset;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = FutureTask.class;
            stateOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("state"));
            runnerOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("runner"));
            waitersOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("waiters"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
