package com.sunrise.study.thread.sourcecode;

import sun.misc.Unsafe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * AQS是实现同步器的基础组件，并发包中锁的底层就是使用AQS实现的。重点掌握！
 *
 * @author huangzihua
 * @date 2021-08-05
 */
public class AbstractQueuedSynchronizer extends AbstractOwnableSynchronizer {
    // 构造函数
    protected AbstractQueuedSynchronizer() { }
    
    
    /**
     * 每个线程被阻塞的线程都会被封装成一个Node结点，放入队列。每个节点包含了一个Thread类型的引用，并且每个节点都存在一个状态，具体状态如下。
     * CANCELLED，值为1，表示当前的线程被取消。
     * SIGNAL，值为-1，表示当前节点的后继节点包含的线程需要运行，需要进行unpark操作。
     * CONDITION，值为-2，表示当前节点在等待condition，也就是在condition queue中。
     * PROPAGATE，值为-3，表示当前场景下后续的acquireShared能够得以执行。
     * 值为0，表示当前节点在sync queue中，等待着获取锁。
     */
    static final class Node {

        // 标记node在共享模式中等待
        static final Node SHARED = new Node();

        // 标记node在独占模式中等待
        static final Node EXCLUSIVE = null;

        // 下面是 node 的状态
        // 等待状态，1=线程被取消
        static final int CANCELLED =  1;

        // 等待状态，-1=子线程需要unpark
        static final int SIGNAL    = -1;

        // 等待状态，-2=线程在等待condition，也就是在condition队列中
        static final int CONDITION = -2;

        // 等待状态，-3=下一个acquireShared可以无条件地传播
        static final int PROPAGATE = -3;

        // 节点状态
        volatile int waitStatus;

        // 前驱结点
        volatile Node prev;

        // 后继结点
        volatile Node next;

        // 结点所对应的线程
        volatile Thread thread;

        // 下一个等待者
        Node nextWaiter;

        // 结点是否在共享模式下等待
        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        // 获取前驱结点，若前驱结点为空，抛出异常
        final Node predecessor() throws NullPointerException {
            Node p = prev;
            if (p == null)
                throw new NullPointerException();
            else
                return p;
        }

        // 无参构造方法
        Node() {    // Used to establish initial head or SHARED marker
        }

        // 构造方法
        Node(Thread thread, Node mode) {     // Used by addWaiter
            this.nextWaiter = mode;
            this.thread = thread;
        }

        // 构造方法
        Node(Thread thread, int waitStatus) { // Used by Condition
            this.waitStatus = waitStatus;
            this.thread = thread;
        }
    }
    
    
    
    

    /**
     * Head of the wait queue, lazily initialized.  Except for
     * initialization, it is modified only via method setHead.  Note:
     * If head exists, its waitStatus is guaranteed not to be
     * CANCELLED.
     */
    /*
        wait队列的头结点。
        延迟初始化。除了初始化外，只能通过setHead方法来设置。
        注：如果头结点存在，必须保证头结点的waitStatus不能为CANCELLED
     */
    private transient volatile Node head;

    /**
     * Tail of the wait queue, lazily initialized.  Modified only via
     * method enq to add new wait node.
     */
    // wait队列的尾结点，延迟初始化。只能通过enq方法来添加新的wait节点
    private transient volatile Node tail;

    /**
     * The synchronization state. 同步状态
     */
    private volatile int state;

    /**
     * Returns the current value of synchronization state.
     * This operation has memory semantics of a {@code volatile} read.
     * @return current state value
     */
    protected final int getState() {
        return state;
    }

    /**
     * Sets the value of synchronization state.
     * This operation has memory semantics of a {@code volatile} write.
     * @param newState the new state value
     */
    protected final void setState(int newState) {
        state = newState;
    }

    /**
     * Atomically sets synchronization state to the given updated
     * value if the current state value equals the expected value.
     * This operation has memory semantics of a {@code volatile} read
     * and write.
     *
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful. False return indicates that the actual
     *         value was not equal to the expected value.
     */
    protected final boolean compareAndSetState(int expect, int update) {
        // See below for intrinsics setup to support this
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }

    // Queuing utilities

    /**
     * 自旋时间
     * The number of nanoseconds for which it is faster to spin
     * rather than to use timed park. A rough estimate suffices
     * to improve responsiveness with very short timeouts.
     */
    static final long spinForTimeoutThreshold = 1000L;

    /**
     * 插入节点到wait队列尾，如果队列为空则执行初始化。
     * 其实该方法主要实现两个功能：
     *      1.如果wait队列为空，则初始化wait队列，并往head节点插入一个哨兵节点；
     *      2.通过无限循环，确保传入的node插入到wait队列尾。
     * Inserts node into queue, initializing if necessary. See picture above.
     * @param node the node to insert
     * @return node's predecessor 返回插入节点的前驱节点
     */
    private Node enq(final Node node) {
        /*
         无限循环，确保结点能够成功入队列。
         首次循环会初始化wait队列，head节点为一个哨兵节点。
         */
        for (;;) {
            // 取尾结点
            Node t = tail;

            // 如果尾结点为null，则进行初始化
            if (t == null) { // Must initialize
                // CAS操作，往head节点，设置一个哨兵节点
                if (compareAndSetHead(new Node()))
                    // 执行成功，则 head 和 tail 都将指向刚刚新建的哨兵节点
                    tail = head;
            } else {
                // 若尾结点不为null，则node的前驱节点设为t
                node.prev = t;

                // CAS原子操作，把尾结点替换为当前node
                if (compareAndSetTail(t, node)) {
                    // 替换成功，则关联结点
                    t.next = node;
                    // 返回t，即插入节点的前驱节点
                    return t;
                }
            }
        }
    }

    /**
     * 根据指定模式封装当前线程为Node节点，插入到wait队列尾部
     * 作用：根据模式（独占或共享）和当前线程实例，封装成一个wait队列的节点，然后插入到wait队列尾部
     * 代码逻辑：
     *   1、先封装成node；
     *   2、判断tail是否为null，不为null则通过CAS操作，快速替换tail变量，使其指向刚刚封装的node；
     *   3、若第2步CAS操作成功，则完成后续插入队尾的操作，返回该node。
     *   4、若tail为null，或者第2步CAS操作失败，则调用enq()方法来插入到队列尾。
     *
     * Creates and enqueues node for current thread and given mode.
     * @param mode 独占模式Node.EXCLUSIVE，或共享模式Node.SHARED
     * @return the new node
     */
    private Node addWaiter(Node mode) {
        // 根据模式和当前线程创建一个新的waiter节点
        Node node = new Node(Thread.currentThread(), mode);

        Node pred = tail;
        if (pred != null) {
            // 如果wait队列尾结点不为空，则插入到尾结点
            node.prev = pred;
            // CAS操作，把tail的引用改为指向node
            if (compareAndSetTail(pred, node)) {
                // 设置尾结点的next域为node
                pred.next = node;
                return node;
            }
        }
        // 尾结点为空(即还没有被初始化过)，或者是compareAndSetTail操作失败，则通过这里入队列或初始化
        enq(node);
        return node;
    }

    /**
     * 设置指定节点为头节点，即哨兵节点。
     * Sets head of queue to be node, thus dequeuing. Called only by
     * acquire methods.  Also nulls out unused fields for sake of GC
     * and to suppress unnecessary signals and traversals.
     *
     * @param node the node
     */
    private void setHead(Node node) {
        head = node;
        node.thread = null;
        node.prev = null;
    }

    /**
     * 唤醒传入节点的后继节点，并把传入节点的waitStatus设为0
     * 该方法的作用就是为了释放node节点的后继结点。
     * Wakes up node's successor, if one exists.
     *
     * @param node the node
     */
    private void unparkSuccessor(Node node) {
        /*
         * If status is negative (i.e., possibly needing signal) try
         * to clear in anticipation of signalling.  It is OK if this
         * fails or if status is changed by waiting thread.
         */
        int ws = node.waitStatus;
        if (ws < 0)
            // 如果传入节点的waitStatus为负数，则设置为0
            compareAndSetWaitStatus(node, ws, 0);

        /*
         * Thread to unpark is held in successor, which is normally
         * just the next node.  But if cancelled or apparently null,
         * traverse backwards from tail to find the actual
         * non-cancelled successor.
         */
        // 取下一节点，若下一节点为null或为取消状态，则从尾结点向前遍历，
        // 直到取到距离node最近的一个waitStatus<=0的节点，将其释放
        Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t;
        }

        if (s != null)
            // 释放所取节点
            LockSupport.unpark(s.thread);
    }

    /**
     * 释放共享锁，唤醒后继节点并且确保该行为传播下去。
     * 相当于调用独占模式下的unparkSuccessor()方法
     * Release action for shared mode -- signals successor and ensures propagation.
     * (Note: For exclusive mode, release just amounts to calling unparkSuccessor of head if it needs signal.)
     */
    private void doReleaseShared() {
        /*
         * Ensure that a release propagates, even if there are other
         * in-progress acquires/releases.  This proceeds in the usual
         * way of trying to unparkSuccessor of head if it needs
         * signal. But if it does not, status is set to PROPAGATE to
         * ensure that upon release, propagation continues.
         * Additionally, we must loop in case a new node is added
         * while we are doing this. Also, unlike other uses of
         * unparkSuccessor, we need to know if CAS to reset status
         * fails, if so rechecking.
         */
        for (;;) {
            // 取头结点
            Node h = head;
            // 判断wait队列是否为空
            if (h != null && h != tail) {
                int ws = h.waitStatus;

                // 判断头结点状态是不是SIGNAL
                if (ws == Node.SIGNAL) {
                    /*
                    如果h的waitStatus是SIGNAL，则通过CAS修改为0，
                    若修改失败，则重复循环，
                    若修改成功，则调用unparkSuccessor(h)
                    */
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                        continue;            // loop to recheck cases
                    unparkSuccessor(h);
                }
                // 如果ws==0，则改为PROPAGATE(-3)
                else if (ws == 0 &&
                        !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                    continue;                // loop on failed CAS
            }
            if (h == head)                   // loop if head changed
                break;
        }
    }

    /**
     * 设置队列头，检查后继结点是否以共享模式等待锁，
     * 如果propagate > 0 或 PROPAGATE status 被设置，则传播
     * Sets head of queue, and
     * checks if successor may be waiting in shared mode,
     * if so propagating if either propagate > 0 or PROPAGATE status was set.
     *
     * @param node the node
     * @param propagate  tryAcquireShared()方法的返回值
     */
    private void setHeadAndPropagate(Node node, int propagate) {
        Node h = head;  // Record old head for check below
        // 将当前node设置为头结点
        setHead(node);
        /*
         * Try to signal next queued node if:
         *   Propagation was indicated by caller,
         *     or was recorded (as h.waitStatus either before
         *     or after setHead) by a previous operation
         *     (note: this uses sign-check of waitStatus because
         *      PROPAGATE status may transition to SIGNAL.)
         * and
         *   The next node is waiting in shared mode,
         *     or we don't know, because it appears null
         *
         * The conservatism in both of these checks may cause
         * unnecessary wake-ups, but only when there are multiple
         * racing acquires/releases, so most need signals now or soon
         * anyway.
         */

        // 若是ReentrantReadWriteLock锁，则这里propagate必定为1，后续不用再判断。
        if (propagate > 0 || h == null || h.waitStatus < 0 ||
                (h = head) == null || h.waitStatus < 0) {
            Node s = node.next;

            // 这里会判断s节点是否为共享模式，若是就唤醒它
            if (s == null || s.isShared())
                // 释放共享锁
                doReleaseShared();
        }
    }

    // Utilities for various versions of acquire

    /**
     * 取消正在进行中的尝试获取资源的操作
     * 该方法完成的功能就是取消当前线程对资源的获取，即设置该结点的状态为CANCELLED
     * Cancels an ongoing attempt to acquire.
     *
     * @param node the node
     */
    private void cancelAcquire(Node node) {
        // Ignore if node doesn't exist
        if (node == null)
            return;

        // 设当前节点的线程为null
        node.thread = null;

        // 如果前驱节点为取消状态，则继续向前遍历获取下一个未取消的节点
        Node pred = node.prev;
        while (pred.waitStatus > 0)
            node.prev = pred = pred.prev;

        // predNext is the apparent node to unsplice. CASes below will
        // fail if not, in which case, we lost race vs another cancel
        // or signal, so no further action is necessary.
        // 取前驱未取消状态节点的后继节点
        // 即：pred(未取消)->B(取消)->C(取消)->Node()，取到的就是B节点
        Node predNext = pred.next;

        // 将当前node的状态设为Cancelled
        node.waitStatus = Node.CANCELLED;


        // 如果当前节点是尾结点，则把尾结点的值改为pred
        if (node == tail && compareAndSetTail(node, pred)) {
            // 接着把尾结点的next域修改为null
            compareAndSetNext(pred, predNext, null);
        } else {  // node结点不为尾结点，或者比较设置不成功
            // If successor needs signal, try to set pred's next-link
            // so it will get one. Otherwise wake it up to propagate.

            int ws;
            /*
                下面条件说明：
                    pred不是头结点，
                    且pred的waitStatus是SIGNAL，
                    且pred.thread != null（表明当前线程处于等待过程中，而不是即将拿到锁）
                为什么已经判断了pred != head 还要判断pred.thread != null？
                    猜测是在这两者执行期间，可能锁会被释放，于是当前线程就有几率成为
                    head之后的第一个node节点，从而得到锁资源
             */
            if (pred != head
                && (
                    (ws = pred.waitStatus) == Node.SIGNAL       // 如果是SIGNAL则返回true，否则改为SIGNAL再返回true
                    || (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))
                )
                && pred.thread != null) {
                // 若当前线程前面还有其他等待线程，即当前线程不会成为下一个获取锁资源的线程
                // 那么就把当前线程及前面的CANCELLED状态的node从wait队列中删除
                Node next = node.next;
                if (next != null && next.waitStatus <= 0)       // 后继节点不为null 且状态不是取消
                    // 把pred的next 改为node.next
                    compareAndSetNext(pred, predNext, next);
            } else {
                // 这里表示当前线程排队排到wait队列头部了，于是要执行unparkSuccessor方法唤醒后面的等待节点
                // 注，前面已把node.waitStatus设为CANCELLED
                unparkSuccessor(node);
            }

            node.next = node; // help GC
        }
    }

    /**
     * 当获取资源失败后，检查前驱节点的等待状态，来判断当前节点是否应该继续等待
     *
     * Checks and updates status for a node that failed to acquire.
     * Returns true if thread should block. This is the main signal
     * control in all acquire loops.  Requires that pred == node.prev.
     *
     * @param pred node's predecessor holding status
     * @param node the node
     * @return {@code true} if thread should block
     */
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        // 取前驱节点状态
        int ws = pred.waitStatus;

        if (ws == Node.SIGNAL)
            /*
             * 如果前驱结点的waitStatus是SIGNAL，则返回true，表示当前节点的线程需进行park，挂起等待
             * This node has already set status asking a release to signal it, so it can safely park.
             */
            return true;
        if (ws > 0) {
            /*
             * Predecessor was cancelled. Skip over predecessors and
             * indicate retry.
             */
            // 如果前驱节点被取消了，则继续向前遍历，直到取到一个未取消的节点，并设为当前节点的前驱节点
            // 这里其实就是从当前节点出发，清除前面已经被取消的等待节点
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            /*
             * 到这里，pred.waitStatus的状态要么是0，要么是PROPAGATE。
             * 先把pred的状态改为SIGNAL，然后返回false表明当前线程暂不挂起
             * waitStatus must be 0 or PROPAGATE.
             * Indicate that we need a signal, but don't park yet.
             * Caller will need to retry to make sure it cannot acquire before parking.
             */
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        // 不能进行park操作
        return false;
    }

    /**
     * 中断当前线程
     * Convenience method to interrupt current thread.
     */
    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    /**
     * 执行park()方法，挂起当前线程。
     * 当线程被唤醒时，会执行Thread.interrupted()方法，获取并清除中断标志。
     * @return {@code true} if interrupted
     */
    private final boolean parkAndCheckInterrupt() {
        LockSupport.park(this);
        return Thread.interrupted();
    }

    /*
     * Various flavors of acquire, varying in exclusive/shared and
     * control modes.  Each is mostly the same, but annoyingly
     * different.  Only a little bit of factoring is possible due to
     * interactions of exception mechanics (including ensuring that we
     * cancel if tryAcquire throws exception) and other control, at
     * least not without hurting performance too much.
     */

    /**
     * 以独占、不可中断模式获取已经在wait队列中的线程。
     * 用于条件等待方法或者acquire()方法。
     * Acquires in exclusive uninterruptible mode for thread already in queue.
     * Used by condition wait methods as well as acquire.
     *
     * @param node the node
     * @param arg the acquire argument
     * @return {@code true} if interrupted while waiting
     */
    final boolean acquireQueued(final Node node, int arg) {
        // 失败标志
        boolean failed = true;
        try {
            // 中断标志，记录线程是否在等待过程中被中断
            boolean interrupted = false;
            // 无限循环
            for (;;) {
                // 取传入node的前驱节点p
                final Node p = node.predecessor();

                // 判断节点p是不是head，如果是则调tryAcquire()方法尝试获取锁
                if (p == head && tryAcquire(arg)) {
                    // 成功获得锁资源后，则把当前节点设为哨兵节点，并把head指向当前node
                    setHead(node);
                    p.next = null; // help GC 断开引用，让GC回收
                    failed = false;
                    return interrupted;     // 返回中断标志
                }

                /*
                    调用shouldParkAfterFailedAcquire：只有当该节点的前驱结点的状态为SIGNAL时，
                         才可以对该结点所封装的线程进行park操作。否则，将不能进行park操作
                    调用parkAndCheckInterrupt：进行park操作并且返回该线程是否被中断。
                    补充：看看是不是需要park，如果true，则进行park操作并返回中断信号。
                 */
                /*
                    若p不是头结点，且tryAcquire()方法执行失败，则执行一下逻辑
                    执行shouldParkAfterFailedAcquire(p,node)方法根据p的状态，判断node是否应该挂起？
                        若返回false，则再次循环，尝试获取锁
                        若返回true，则执行parkAndCheckInterrupt()方法挂起线程，而当线程被唤醒时，检查并清除中断标志
                */
                if (shouldParkAfterFailedAcquire(p, node) &&
                        parkAndCheckInterrupt())

                    /*
                    忽略中断，也就是如果当前线程是因为被其他线程中断而唤醒的，
                    则记录一下中断信号，表明当前线程被中断过，而不是抛异常。
                     */
                    interrupted = true;
            }
        } finally {
            // 上述循环过程中，如果因为未知的原因导致方法结束，则取消当前线程的竞争
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * 在独占模式下获取锁资源，若线程在此期间被中断，会抛出异常
     * Acquires in exclusive interruptible mode.
     * @param arg the acquire argument
     */
    private void doAcquireInterruptibly(int arg)
            throws InterruptedException {
        // 把当前线程封装成Node节点，并添加到wait队列
        final Node node = addWaiter(Node.EXCLUSIVE);
        // 失败标志
        boolean failed = true;
        try {
            // 死循环，确保线程能获取到锁资源
            for (;;) {
                // 取前驱结点p
                final Node p = node.predecessor();
                // 判断节点p是不是head，如果是则尝试获取锁资源
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return;
                }

                if (shouldParkAfterFailedAcquire(p, node) &&
                        parkAndCheckInterrupt())
                    // 和acquireQueued()方法的区别在于此处。
                    // acquireQueued()方法会记下中断标志，并继续挂起
                    // 而当前方法会对中断信号做出反应，若线程被中断，则抛出中断异常
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * 指定等待时间内，以独占模式获取锁。会被中断。
     * @param arg the acquire argument
     * @param nanosTimeout max wait time  最长等待时间
     * @return {@code true} if acquired
     */
    private boolean doAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {

        // 如果最长等待时间设为小于等于0，则直接返回false
        if (nanosTimeout <= 0L)
            return false;

        // 计算最终等待时间
        final long deadline = System.nanoTime() + nanosTimeout;

        // 把当前线程封装成Node对象，并加入到wait队列
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            // 死循环
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return true;    // 成功获取锁
                }
                // 计算时间，并判断剩余时间是否小于等于0，小于0表明时间已经到了，则不再挂起，而是直接返回false
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    return false;

                // 是否应该挂起等待，如果是则判断剩余等待时间是否大于自旋时间，大于则执行park
                if (shouldParkAfterFailedAcquire(p, node) &&
                        nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);  // 挂起指定时间段

                // 如果当前线程被其他线程中断，则直接抛出中断异常
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                // 如果在指定时间内没获取到资源或者线程被中断，则取消当前node
                cancelAcquire(node);
        }
    }

    /**
     * 不可中断，获取共享锁。
     * Acquires in shared uninterruptible mode.
     * @param arg the acquire argument
     */
    private void doAcquireShared(int arg) {
        // 以共享模式封装当前线程为Node对象，添加到wait队列尾
        final Node node = addWaiter(Node.SHARED);

        /* 下面就是获取锁的过程 */
        boolean failed = true;  // 失败标志
        try {
            boolean interrupted = false;    // 中断标志
            for (;;) {
                final Node p = node.predecessor();      // 取前驱节点

                // 只有当前驱节点是head节点时，才会再次去竞争共享锁
                if (p == head) {                        // 若前驱节点是head
                    int r = tryAcquireShared(arg);      // 尝试获取读锁资源
                    if (r >= 0) {

                        /*
                        这里是传播行为的关键代码。如前面尝试获取共享锁成功，则这里就调用setHeadAndPropagate()方法
                        该方法会把头结点删除，并把当前节点设为头结点，同时唤醒当前节点的下一个节点
                        */
                        setHeadAndPropagate(node, r);   // 若是ReentrantReadWriteLock，则参数r必定为1
                        p.next = null; // help GC
                        if (interrupted)
                            selfInterrupt();
                        failed = false;
                        return;
                    }
                }

                // 如果前驱节点不是head节点，就会判断是否应该被阻塞
                if (shouldParkAfterFailedAcquire(p, node) &&    // 判断是否该阻塞，是则执行阻塞
                        parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in shared interruptible mode.
     * @param arg the acquire argument
     */
    private void doAcquireSharedInterruptibly(int arg)
            throws InterruptedException {
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                        parkAndCheckInterrupt())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in shared timed mode.
     *
     * @param arg the acquire argument
     * @param nanosTimeout max wait time
     * @return {@code true} if acquired
     */
    private boolean doAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return true;
                    }
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    return false;
                if (shouldParkAfterFailedAcquire(p, node) &&
                        nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    // Main exported methods

    /**
     * 尝试以独占模式获取锁资源。由子类实现该方法。
     * Attempts to acquire in exclusive mode. This method should query
     * if the state of the object permits it to be acquired in the
     * exclusive mode, and if so to acquire it.
     *
     * <p>This method is always invoked by the thread performing
     * acquire.  If this method reports failure, the acquire method
     * may queue the thread, if it is not already queued, until it is
     * signalled by a release from some other thread. This can be used
     * to implement method {@link Lock#tryLock()}.
     *
     * <p>The default
     * implementation throws {@link UnsupportedOperationException}.
     *
     * @param arg the acquire argument. This value is always the one
     *        passed to an acquire method, or is the value saved on entry
     *        to a condition wait.  The value is otherwise uninterpreted
     *        and can represent anything you like.
     * @return {@code true} if successful. Upon success, this object has
     *         been acquired.
     * @throws IllegalMonitorStateException if acquiring would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if exclusive mode is not supported
     */
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * 在独占模式下尝试设置同步状态state。执行释放操作时总是会执行此方法。
     * 自定义实现类可按需重写该方法。
     * 参考ReentrantLock类的实现，当state-arg=0时，返回true，否则返回false。
     * Attempts to set the state to reflect a release in exclusive
     * mode.
     *
     * <p>This method is always invoked by the thread performing release.
     *
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.
     *
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     * @return {@code true} if this object is now in a fully released
     *         state, so that any waiting threads may attempt to acquire;
     *         and {@code false} otherwise.
     * @throws IllegalMonitorStateException if releasing would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if exclusive mode is not supported
     */
    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to acquire in shared mode. This method should query if
     * the state of the object permits it to be acquired in the shared
     * mode, and if so to acquire it.
     *
     * <p>This method is always invoked by the thread performing
     * acquire.  If this method reports failure, the acquire method
     * may queue the thread, if it is not already queued, until it is
     * signalled by a release from some other thread.
     *
     * <p>The default implementation throws {@link
     * UnsupportedOperationException}.
     *
     * @param arg the acquire argument. This value is always the one
     *        passed to an acquire method, or is the value saved on entry
     *        to a condition wait.  The value is otherwise uninterpreted
     *        and can represent anything you like.
     * @return a negative value on failure; zero if acquisition in shared
     *         mode succeeded but no subsequent shared-mode acquire can
     *         succeed; and a positive value if acquisition in shared
     *         mode succeeded and subsequent shared-mode acquires might
     *         also succeed, in which case a subsequent waiting thread
     *         must check availability. (Support for three different
     *         return values enables this method to be used in contexts
     *         where acquires only sometimes act exclusively.)  Upon
     *         success, this object has been acquired.
     * @throws IllegalMonitorStateException if acquiring would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if shared mode is not supported
     */
    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to set the state to reflect a release in shared mode.
     *
     * <p>This method is always invoked by the thread performing release.
     *
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.
     *
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     * @return {@code true} if this release of shared mode may permit a
     *         waiting acquire (shared or exclusive) to succeed; and
     *         {@code false} otherwise
     * @throws IllegalMonitorStateException if releasing would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if shared mode is not supported
     */
    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns {@code true} if synchronization is held exclusively with
     * respect to the current (calling) thread.  This method is invoked
     * upon each call to a non-waiting {@link ConditionObject} method.
     * (Waiting methods instead invoke {@link #release}.)
     *
     * <p>The default implementation throws {@link
     * UnsupportedOperationException}. This method is invoked
     * internally only within {@link ConditionObject} methods, so need
     * not be defined if conditions are not used.
     *
     * @return {@code true} if synchronization is held exclusively;
     *         {@code false} otherwise
     * @throws UnsupportedOperationException if conditions are not supported
     */
    //该线程是否正在独占资源。只有用到condition才需要去实现它。
    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    /**
     * 获取独占锁，忽略中断。
     * Acquires in exclusive mode, ignoring interrupts.
     * Implemented by invoking at least once {@link #tryAcquire}, returning on success.
     * Otherwise the thread is queued, possibly repeatedly blocking and unblocking, invoking {@link #tryAcquire} until success.
     * This method can be used to implement method {@link Lock#lock}.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     */
    public final void acquire(int arg) {
        /*
          tryAcquire()方法交由子类实现。
          addWaiter()方法是封装当前线程为Node节点，添加到wait队列。
          acquireQueued()方法自旋获取锁资源，若获取失败就挂起
        */
        if (!tryAcquire(arg) &&
                acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            // 若acquireQueued返回true，表明线程阻塞过程中曾被中断，所以这里要恢复线程的中断标志
            selfInterrupt();
    }

    /**
     * 以独占模式获取锁，若线程被中断则终止。
     * Acquires in exclusive mode, aborting if interrupted.
     * Implemented by first checking interrupt status, then invoking
     * at least once {@link #tryAcquire}, returning on
     * success.  Otherwise the thread is queued, possibly repeatedly
     * blocking and unblocking, invoking {@link #tryAcquire}
     * until success or the thread is interrupted.  This method can be
     * used to implement method {@link Lock#lockInterruptibly}.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @throws InterruptedException if the current thread is interrupted
     */
    public final void acquireInterruptibly(int arg)
            throws InterruptedException {
        // 判断当前线程是否已中断，若已中断，则抛异常
        if (Thread.interrupted())
            throw new InterruptedException();

        // 尝试获取锁资源
        if (!tryAcquire(arg))
            // 调用tryAcquire()失败时，就调用AQS的可中断方法获取锁资源
            doAcquireInterruptibly(arg);
    }

    /**
     * 在指定时间内尝试获取锁资源
     * Attempts to acquire in exclusive mode, aborting if interrupted,
     * and failing if the given timeout elapses.  Implemented by first
     * checking interrupt status, then invoking at least once {@link
     * #tryAcquire}, returning on success.  Otherwise, the thread is
     * queued, possibly repeatedly blocking and unblocking, invoking
     * {@link #tryAcquire} until success or the thread is interrupted
     * or the timeout elapses.  This method can be used to implement
     * method {@link Lock#tryLock(long, TimeUnit)}.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @param nanosTimeout the maximum number of nanoseconds to wait
     * @return {@code true} if acquired; {@code false} if timed out
     * @throws InterruptedException if the current thread is interrupted
     */
    public final boolean tryAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        // 判断当前线程是否已中断，若已中断，则抛异常
        if (Thread.interrupted())
            throw new InterruptedException();

        // 调tryAcquire方法尝试获取锁资源，若成功则直接返回true
        // 若失败，则调用doAcquireNanos来获取锁资源，在指定时间内没有拿到资源则返回false。
        return tryAcquire(arg) ||
                doAcquireNanos(arg, nanosTimeout);
    }

    /**
     * 释放独占线程的锁资源，并唤醒下一个等待线程
     * 如果tryRelease()返回true，则通过解除一个或多个阻塞线程来实现。
     * 该方法可以用来实现Lock.unlock()。
     * Releases in exclusive mode.  Implemented by unblocking one or
     * more threads if {@link #tryRelease} returns true.
     * This method can be used to implement method {@link Lock#unlock}.
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryRelease} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @return the value returned from {@link #tryRelease}
     */
    public final boolean release(int arg) {
        /*
        执行tryRelease()方法，由子类实现。如果tryRelease()返回true，表明锁被完全释放。
        参考ReentrantLock实现：设state = state - arg，最终state=0，则为true，否则为false
         */
        if (tryRelease(arg)) {
            // 当前线程完全释放锁资源后，就唤醒wait队列的第一个等待节点。
            Node h = head;
            if (h != null && h.waitStatus != 0)
                unparkSuccessor(h);
            return true;
        }

        // 如果线程未完全释放写锁，则返回false
        return false;
    }

    /**
     * 以共享模式获取锁资源，忽略中断。
     */
    public final void acquireShared(int arg) {
        /*
        先调用tryAcquireShared()方法尝试获取共享锁，如果该方法返回值不小于0，说明获取共享锁成功，当前方法结束
        tryAcquireShared()方法为抽象方法，具体由AQS的实现类去实现
        */
        if (tryAcquireShared(arg) < 0)
           /*
           如果tryAcquireShared获取失败，再调doAcquireShared()方法
           该方法为AQS的方法，会把当前线程添加到wait队列
           */
            doAcquireShared(arg);
    }

    /**
     * Acquires in shared mode, aborting if interrupted.  Implemented
     * by first checking interrupt status, then invoking at least once
     * {@link #tryAcquireShared}, returning on success.  Otherwise the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted.
     * @param arg the acquire argument.
     * This value is conveyed to {@link #tryAcquireShared} but is
     * otherwise uninterpreted and can represent anything
     * you like.
     * @throws InterruptedException if the current thread is interrupted
     */
    public final void acquireSharedInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (tryAcquireShared(arg) < 0)
            doAcquireSharedInterruptibly(arg);
    }

    /**
     * Attempts to acquire in shared mode, aborting if interrupted, and
     * failing if the given timeout elapses.  Implemented by first
     * checking interrupt status, then invoking at least once {@link
     * #tryAcquireShared}, returning on success.  Otherwise, the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted or the timeout elapses.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquireShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     * @param nanosTimeout the maximum number of nanoseconds to wait
     * @return {@code true} if acquired; {@code false} if timed out
     * @throws InterruptedException if the current thread is interrupted
     */
    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        return tryAcquireShared(arg) >= 0 ||
                doAcquireSharedNanos(arg, nanosTimeout);
    }

    /**
     * 释放共享锁
     * Releases in shared mode.  Implemented by unblocking one or more
     * threads if {@link #tryReleaseShared} returns true.
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryReleaseShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     * @return the value returned from {@link #tryReleaseShared}
     */
    public final boolean releaseShared(int arg) {
        /*
        调tryReleaseShared()方法以共享模式释放锁资源，由子类实现该方法逻辑
        */
        if (tryReleaseShared(arg)) {
            doReleaseShared();
            return true;
        }
        return false;
    }

    // Queue inspection methods

    /**
     * Queries whether any threads are waiting to acquire. Note that
     * because cancellations due to interrupts and timeouts may occur
     * at any time, a {@code true} return does not guarantee that any
     * other thread will ever acquire.
     *
     * <p>In this implementation, this operation returns in
     * constant time.
     *
     * @return {@code true} if there may be other threads waiting to acquire
     */
    public final boolean hasQueuedThreads() {
        return head != tail;
    }

    /**
     * Queries whether any threads have ever contended to acquire this
     * synchronizer; that is if an acquire method has ever blocked.
     *
     * <p>In this implementation, this operation returns in
     * constant time.
     *
     * @return {@code true} if there has ever been contention
     */
    public final boolean hasContended() {
        return head != null;
    }

    /**
     * Returns the first (longest-waiting) thread in the queue, or
     * {@code null} if no threads are currently queued.
     *
     * <p>In this implementation, this operation normally returns in
     * constant time, but may iterate upon contention if other threads are
     * concurrently modifying the queue.
     *
     * @return the first (longest-waiting) thread in the queue, or
     *         {@code null} if no threads are currently queued
     */
    public final Thread getFirstQueuedThread() {
        // handle only fast path, else relay
        return (head == tail) ? null : fullGetFirstQueuedThread();
    }

    /**
     * Version of getFirstQueuedThread called when fastpath fails
     */
    private Thread fullGetFirstQueuedThread() {
        /*
         * The first node is normally head.next. Try to get its
         * thread field, ensuring consistent reads: If thread
         * field is nulled out or s.prev is no longer head, then
         * some other thread(s) concurrently performed setHead in
         * between some of our reads. We try this twice before
         * resorting to traversal.
         */
        Node h, s;
        Thread st;
        if (((h = head) != null && (s = h.next) != null &&
                s.prev == head && (st = s.thread) != null) ||
                ((h = head) != null && (s = h.next) != null &&
                        s.prev == head && (st = s.thread) != null))
            return st;

        /*
         * Head's next field might not have been set yet, or may have
         * been unset after setHead. So we must check to see if tail
         * is actually first node. If not, we continue on, safely
         * traversing from tail back to head to find first,
         * guaranteeing termination.
         */

        Node t = tail;
        Thread firstThread = null;
        while (t != null && t != head) {
            Thread tt = t.thread;
            if (tt != null)
                firstThread = tt;
            t = t.prev;
        }
        return firstThread;
    }

    /**
     * Returns true if the given thread is currently queued.
     *
     * <p>This implementation traverses the queue to determine
     * presence of the given thread.
     *
     * @param thread the thread
     * @return {@code true} if the given thread is on the queue
     * @throws NullPointerException if the thread is null
     */
    public final boolean isQueued(Thread thread) {
        if (thread == null)
            throw new NullPointerException();
        for (Node p = tail; p != null; p = p.prev)
            if (p.thread == thread)
                return true;
        return false;
    }

    /**
     * 等待队列中的第一个线程是不是独占线程，是则返回true。
     *
     * Returns {@code true} if the apparent first queued thread, if one
     * exists, is waiting in exclusive mode.  If this method returns
     * {@code true}, and the current thread is attempting to acquire in
     * shared mode (that is, this method is invoked from {@link
     * #tryAcquireShared}) then it is guaranteed that the current thread
     * is not the first queued thread.  Used only as a heuristic in
     * ReentrantReadWriteLock.
     */
    final boolean apparentlyFirstQueuedIsExclusive() {
        Node h, s;
        return (h = head) != null &&        // 头节点不为null
                (s = h.next)  != null &&    // 第一个等待节点不为null
                !s.isShared()         &&    // 第一个等待节点是独占线程
                s.thread != null;           // 第一个等待节点的线程不为null
    }

    /**
     * Queries whether any threads have been waiting to acquire longer
     * than the current thread.
     *
     * <p>An invocation of this method is equivalent to (but may be
     * more efficient than):
     *  <pre> {@code
     * getFirstQueuedThread() != Thread.currentThread() &&
     * hasQueuedThreads()}</pre>
     *
     * <p>Note that because cancellations due to interrupts and
     * timeouts may occur at any time, a {@code true} return does not
     * guarantee that some other thread will acquire before the current
     * thread.  Likewise, it is possible for another thread to win a
     * race to enqueue after this method has returned {@code false},
     * due to the queue being empty.
     *
     * <p>This method is designed to be used by a fair synchronizer to
     * avoid <a href="AbstractQueuedSynchronizer#barging">barging</a>.
     * Such a synchronizer's {@link #tryAcquire} method should return
     * {@code false}, and its {@link #tryAcquireShared} method should
     * return a negative value, if this method returns {@code true}
     * (unless this is a reentrant acquire).  For example, the {@code
     * tryAcquire} method for a fair, reentrant, exclusive mode
     * synchronizer might look like this:
     *
     *  <pre> {@code
     * protected boolean tryAcquire(int arg) {
     *   if (isHeldExclusively()) {
     *     // A reentrant acquire; increment hold count
     *     return true;
     *   } else if (hasQueuedPredecessors()) {
     *     return false;
     *   } else {
     *     // try to acquire normally
     *   }
     * }}</pre>
     *
     * @return {@code true} if there is a queued thread preceding the
     *         current thread, and {@code false} if the current thread
     *         is at the head of the queue or the queue is empty
     * @since 1.7
     */
    // 判断当前线程之前是否有比它等待更久准备获取锁的线程
    // 返回true表示，有其他线程在等待了，当前线程必须排队，返回false 表示当前线程不用排队
    public final boolean hasQueuedPredecessors() {
        // The correctness of this depends on head being initialized
        // before tail and on head.next being accurate if the current
        // thread is first in queue.
        Node t = tail; // Read fields in reverse initialization order
        Node h = head;
        Node s;

        /*
            分析：
            （1）如果h == t，则说明当前队列为空，直接返回false；
            （2）如果h != t，且s == null则说明有一个元素将要作为AQS的第一个节点入队列，则返回true
            （参考enq()方法，有可能head有值，而tail为null，所以这里需要判断下head.next == null。
            因为当出现这种情况的时候，说明有别的线程已经在初始化wait队列了，当前线程只能乖乖等待。）
            （3）如果h!=t 且 s!=null 和 s.thread != Thread.currentThread() 则说明队列第一个等待节点不是当前线程，那么返回true
         */
        return h != t &&
                ((s = h.next) == null || s.thread != Thread.currentThread());
    }


    // Instrumentation and monitoring methods

    /**
     * Returns an estimate of the number of threads waiting to
     * acquire.  The value is only an estimate because the number of
     * threads may change dynamically while this method traverses
     * internal data structures.  This method is designed for use in
     * monitoring system state, not for synchronization
     * control.
     *
     * @return the estimated number of threads waiting to acquire
     */
    public final int getQueueLength() {
        int n = 0;
        for (Node p = tail; p != null; p = p.prev) {
            if (p.thread != null)
                ++n;
        }
        return n;
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive monitoring facilities.
     *
     * @return the collection of threads
     */
    public final Collection<Thread> getQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            Thread t = p.thread;
            if (t != null)
                list.add(t);
        }
        return list;
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire in exclusive mode. This has the same properties
     * as {@link #getQueuedThreads} except that it only returns
     * those threads waiting due to an exclusive acquire.
     *
     * @return the collection of threads
     */
    public final Collection<Thread> getExclusiveQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (!p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire in shared mode. This has the same properties
     * as {@link #getQueuedThreads} except that it only returns
     * those threads waiting due to a shared acquire.
     *
     * @return the collection of threads
     */
    public final Collection<Thread> getSharedQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * Returns a string identifying this synchronizer, as well as its state.
     * The state, in brackets, includes the String {@code "State ="}
     * followed by the current value of {@link #getState}, and either
     * {@code "nonempty"} or {@code "empty"} depending on whether the
     * queue is empty.
     *
     * @return a string identifying this synchronizer, as well as its state
     */
    public String toString() {
        int s = getState();
        String q  = hasQueuedThreads() ? "non" : "";
        return super.toString() +
                "[State = " + s + ", " + q + "empty queue]";
    }


    // Internal support methods for Conditions

    /**
     * 判断节点是否等待同步队列
     * Returns true if a node, always one that was initially placed on
     * a condition queue, is now waiting to reacquire on sync queue.
     * @param node the node
     * @return true if is reacquiring
     */
    final boolean isOnSyncQueue(Node node) {
        if (node.waitStatus == Node.CONDITION || node.prev == null)
            return false;
        if (node.next != null) // If has successor, it must be on queue
            return true;
        /*
         * node.prev can be non-null, but not yet on queue because
         * the CAS to place it on queue can fail. So we have to
         * traverse from tail to make sure it actually made it.  It
         * will always be near the tail in calls to this method, and
         * unless the CAS failed (which is unlikely), it will be
         * there, so we hardly ever traverse much.
         */
        return findNodeFromTail(node);
    }

    /**
     * 从尾结点往前找，判断传入节点node是否存在
     * Returns true if node is on sync queue by searching backwards from tail.
     * Called only when needed by isOnSyncQueue.
     * @return true if present
     */
    private boolean findNodeFromTail(Node node) {
        Node t = tail;
        for (;;) {
            if (t == node)
                return true;
            if (t == null)
                return false;
            t = t.prev;
        }
    }

    /**
     * 当前节点状态变为0，前驱节点状态变为Signal-1
     * Transfers a node from a condition queue onto sync queue.
     * Returns true if successful.
     * @param node the node
     * @return true if successfully transferred (else the node was
     * cancelled before signal)
     */
    final boolean transferForSignal(Node node) {
        /*
         * If cannot change waitStatus, the node has been cancelled.
         */
        // CAS原子操作，把node的状态，由Condition改为0
        // 如果不能更改waitStatus，则该节点已被取消
        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
            return false;   // 修改失败，则返回false

        /*
         * Splice onto queue and try to set waitStatus of predecessor to
         * indicate that thread is (probably) waiting. If cancelled or
         * attempt to set waitStatus fails, wake up to resync (in which
         * case the waitStatus can be transiently and harmlessly wrong).
         */
        // 追加节点，并返回前驱节点
        Node p = enq(node);
        int ws = p.waitStatus;  // 取前驱节点的状态

        // 前驱节点的状态为非取消，即非CANCELLED(1)
        // 若是0，则尝试更改为SIGNAL(-1)
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
            // 解除阻塞状态
            LockSupport.unpark(node.thread);
        return true;
    }

    /**
     * Transfers node, if necessary, to sync queue after a cancelled wait.
     * Returns true if thread was cancelled before being signalled.
     *
     * @param node the node
     * @return true if cancelled before the node was signalled
     */
    final boolean transferAfterCancelledWait(Node node) {

        // 节点被取消，则重新改为0
        if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
            // 插入节点到队列尾，必要时初始化。
            enq(node);
            return true;
        }
        /*
         * If we lost out to a signal(), then we can't proceed
         * until it finishes its enq().  Cancelling during an
         * incomplete transfer is both rare and transient, so just
         * spin.
         */
        while (!isOnSyncQueue(node))
            Thread.yield();
        return false;
    }

    /**
     * 调用用当前state值调用release()，返回当前state值。
     * 取消传入节点且抛出失败异常
     * Invokes release with current state value; returns saved state.
     * Cancels node and throws exception on failure.
     * @param node the condition node for this wait 当前node
     * @return previous sync state
     */
    final int fullyRelease(Node node) {
        boolean failed = true;
        try {
            // 获取当前锁的state值
            int savedState = getState();

            // 传入自身state值，释放所有节点
            if (release(savedState)) {
                failed = false;
                return savedState;
            } else {
                throw new IllegalMonitorStateException();
            }
        } finally {
            if (failed)
                node.waitStatus = Node.CANCELLED;
        }
    }

    // Instrumentation methods for conditions

    /**
     * Queries whether the given ConditionObject
     * uses this synchronizer as its lock.
     *
     * @param condition the condition
     * @return {@code true} if owned
     * @throws NullPointerException if the condition is null
     */
    public final boolean owns(ConditionObject condition) {
        return condition.isOwnedBy(this);
    }

    /**
     * Queries whether any threads are waiting on the given condition
     * associated with this synchronizer. Note that because timeouts
     * and interrupts may occur at any time, a {@code true} return
     * does not guarantee that a future {@code signal} will awaken
     * any threads.  This method is designed primarily for use in
     * monitoring of the system state.
     *
     * @param condition the condition
     * @return {@code true} if there are any waiting threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final boolean hasWaiters(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.hasWaiters();
    }

    /**
     * Returns an estimate of the number of threads waiting on the
     * given condition associated with this synchronizer. Note that
     * because timeouts and interrupts may occur at any time, the
     * estimate serves only as an upper bound on the actual number of
     * waiters.  This method is designed for use in monitoring of the
     * system state, not for synchronization control.
     *
     * @param condition the condition
     * @return the estimated number of waiting threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final int getWaitQueueLength(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitQueueLength();
    }

    /**
     * Returns a collection containing those threads that may be
     * waiting on the given condition associated with this
     * synchronizer.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate. The elements of the
     * returned collection are in no particular order.
     *
     * @param condition the condition
     * @return the collection of threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitingThreads();
    }
    
    
    
    public class ConditionObject implements Condition {
        private static final long serialVersionUID = 1173984872572414699L;

        // condition队列头结点
        private transient Node firstWaiter;
        // condition队列尾结点
        private transient Node lastWaiter;


        // 构造方法
        public ConditionObject() { }

        // Internal methods

        /**
         * 把当前线程封装成Node节点，并设为Condition状态，插入到条件队列中
         * 有可能会执行清理非Condition节点的方法
         * @return
         */
        private Node addConditionWaiter() {
            // 取尾节点
            Node t = lastWaiter;

            // 下面这一步，目的是获取最后一个Condition状态的节点。
            if (t != null && t.waitStatus != Node.CONDITION) {
                // 如果尾结点不为空，且非Condition状态，
                // 则执行一遍unlinkCancelledWaiters()方法，对条件队列进行清理
                // 该方法将会清除所有非Condition状态的节点
                unlinkCancelledWaiters();
                t = lastWaiter;     // 清除所有非Condition后，再次获取尾结点
            }

            // 把当前线程封装进Node，并设为Condition状态
            Node node = new Node(Thread.currentThread(), Node.CONDITION);

            /* 下面的操作就是往单向condition队列尾部插入一个元素 */
            // 如果t为null，说明当前condition队列为空
            if (t == null)
                firstWaiter = node;     // 头结点就是当前新建的node
            else
                t.nextWaiter = node;    // 尾结点不为空，则尾结点的下一节点就是新建的node

            // 设置尾结点指向新建的node
            lastWaiter = node;
            return node;
        }

        /**
         * 删除和传输节点，直到达到未取消节点或空节点。从信号中分离出来，部分是为了鼓励编译器内联没有等待器的情况。
         * Removes and transfers nodes until hit non-cancelled one or
         * null. Split out from signal in part to encourage compilers
         * to inline the case of no waiters.
         * @param first (non-null) the first node on condition queue
         */
        private void doSignal(Node first) {
            do {
                // 头结点 = first.next，判断是否为null
                if ( (firstWaiter = first.nextWaiter) == null)
                    // 头结点为null，则尾结点也为null
                    lastWaiter = null;

                // first.next = null
                first.nextWaiter = null;
            } while (!transferForSignal(first) &&   // 将结点从condition队列转移到sync队列失败并且condition队列中的头结点不为空，一直循环
                    (first = firstWaiter) != null);
        }

        /**
         * 删除和传输所有节点
         * Removes and transfers all nodes.
         * @param first (non-null) the first node on condition queue
         */
        private void doSignalAll(Node first) {
            // 头结点和尾结点都设为null
            lastWaiter = firstWaiter = null;
            do {
                Node next = first.nextWaiter;
                first.nextWaiter = null;
                transferForSignal(first);
                first = next;
            } while (first != null);
        }


        // 从condition队列中清除状态为CANCEL的结点
        private void unlinkCancelledWaiters() {
            // 从头结点开始遍历
            Node t = firstWaiter;
            Node trail = null;
            while (t != null) {
                // 获取下一节点
                Node next = t.nextWaiter;

                // 若当前节点t的waitStatus状态不是condition，则移除
                if (t.waitStatus != Node.CONDITION) {
                    // 这里的代码就是把当前节点t移除掉，断开所有关联
                    t.nextWaiter = null;
                    if (trail == null)
                        firstWaiter = next;
                    else
                        trail.nextWaiter = next;
                    if (next == null)
                        lastWaiter = trail;
                }
                else
                    trail = t;
                t = next;
            }
        }

        // public methods

        /**
         * 唤醒一个等待线程。如果所有的线程都在等待此条件，则选择其中的一个唤醒。在从 await 返回之前，该线程必须重新获取锁。
         * Moves the longest-waiting thread, if one exists, from the
         * wait queue for this condition to the wait queue for the
         * owning lock.
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        public final void signal() {
            // 判断当前线程是否独占模式。不是独占，则抛异常
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            // 唤醒第一个，即等待时间最长的一个
            Node first = firstWaiter;
            if (first != null)
                // 将condition队列头元素移动到AQS队列
                doSignal(first);
        }

        /**
         * 唤醒所有
         * Moves all threads from the wait queue for this condition to
         * the wait queue for the owning lock.
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        public final void signalAll() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            if (first != null)
                doSignalAll(first);
        }

        /**
         * 等待，当前线程在接到信号之前一直处于等待状态，不响应中断
         * Implements uninterruptible condition wait.
         * 实现不可中断条件的等待
         * <ol>
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * </ol>
         */
        public final void awaitUninterruptibly() {
            // 先把当前线程插入到condition队列中
            Node node = addConditionWaiter();

            // 获取释放状态
            int savedState = fullyRelease(node);
            boolean interrupted = false;
            // 判断当前线程是否处于同步队列中
            while (!isOnSyncQueue(node)) {
                // 阻塞当前线程
                LockSupport.park(this);
                // 判断当前线程是否处于中断状态，并重置中断status
                if (Thread.interrupted())
                    interrupted = true;
            }
            if (acquireQueued(node, savedState) || interrupted)
                // 中断当前线程
                selfInterrupt();
        }

        /*
         * For interruptible waits, we need to track whether to throw
         * InterruptedException, if interrupted while blocked on
         * condition, versus reinterrupt current thread, if
         * interrupted while blocked waiting to re-acquire.
         */

        /** Mode meaning to reinterrupt on exit from wait */
        private static final int REINTERRUPT =  1;
        /** Mode meaning to throw InterruptedException on exit from wait */
        private static final int THROW_IE    = -1;

        /**
         * 判断当前线程是否被中断，是返回1或-1，否则返回0
         * Checks for interrupt, returning THROW_IE if interrupted
         * before signalled, REINTERRUPT if after signalled, or
         * 0 if not interrupted.
         */
        private int checkInterruptWhileWaiting(Node node) {
            return Thread.interrupted() ?
                    (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) :
                    0;
        }

        /**
         * Throws InterruptedException, reinterrupts current thread, or
         * does nothing, depending on mode.
         */
        private void reportInterruptAfterWait(int interruptMode)
                throws InterruptedException {
            if (interruptMode == THROW_IE)
                throw new InterruptedException();
            else if (interruptMode == REINTERRUPT)
                selfInterrupt();
        }

        /**
         * 可中断条件等待
         * Implements interruptible condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled or interrupted.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         */
        public final void await() throws InterruptedException {
            // 检测当前线程是否处于中断状态，是则抛出中断异常
            if (Thread.interrupted())
                throw new InterruptedException();

            // 把当前线程封装成node，加入到condition队列尾
            Node node = addConditionWaiter();

            // 在进入条件等待之前，先释放当前线程所获取的锁，避免造成死锁
            int savedState = fullyRelease(node);
            int interruptMode = 0;

            while (!isOnSyncQueue(node)) {
                // 阻塞当前线程
                LockSupport.park(this);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }

            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null) // clean up if cancelled
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
        }

        /**
         * Implements timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         */
        // 等待，当前线程在接到信号、被中断或到达指定等待时间之前一直处于等待状态
        public final long awaitNanos(long nanosTimeout)
                throws InterruptedException {
            // 若线程被中断，则抛异常
            if (Thread.interrupted())
                throw new InterruptedException();
            // 添加到条件队列尾
            Node node = addConditionWaiter();
            // 在进入条件等待之前，先释放当前线程所获取的锁，避免造成死锁
            int savedState = fullyRelease(node);
            // 计算等待时间点
            final long deadline = System.nanoTime() + nanosTimeout;
            // 中断标志
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return deadline - System.nanoTime();
        }

        /**
         * Implements absolute timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        // 等待，当前线程在接到信号、被中断或到达指定最后期限之前一直处于等待状态
        public final boolean awaitUntil(Date deadline)
                throws InterruptedException {
            long abstime = deadline.getTime();
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (System.currentTimeMillis() > abstime) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                LockSupport.parkUntil(this, abstime);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        /**
         * Implements timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        // 等待，当前线程在接到信号、被中断或到达指定等待时间之前一直处于等待状态。此方法在行为上等效于: awaitNanos(unit.toNanos(time)) > 0
        public final boolean await(long time, TimeUnit unit)
                throws InterruptedException {
            long nanosTimeout = unit.toNanos(time);
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        //  support for instrumentation

        /**
         * Returns true if this condition was created by the given
         * synchronization object.
         *
         * @return {@code true} if owned
         */
        final boolean isOwnedBy(AbstractQueuedSynchronizer sync) {
            return sync == AbstractQueuedSynchronizer.this;
        }

        /**
         * Queries whether any threads are waiting on this condition.
         * Implements {@link AbstractQueuedSynchronizer#hasWaiters(ConditionObject)}.
         *
         * @return {@code true} if there are any waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        //  查询是否有正在等待此条件的任何线程
        protected final boolean hasWaiters() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    return true;
            }
            return false;
        }

        /**
         * Returns an estimate of the number of threads waiting on
         * this condition.
         * Implements {@link AbstractQueuedSynchronizer#getWaitQueueLength(ConditionObject)}.
         *
         * @return the estimated number of waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        // 返回正在等待此条件的线程数估计值
        protected final int getWaitQueueLength() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            int n = 0;
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    ++n;
            }
            return n;
        }

        /**
         * Returns a collection containing those threads that may be
         * waiting on this Condition.
         * Implements {@link AbstractQueuedSynchronizer#getWaitingThreads(ConditionObject)}.
         *
         * @return the collection of threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        // 返回包含那些可能正在等待此条件的线程集合
        protected final Collection<Thread> getWaitingThreads() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            ArrayList<Thread> list = new ArrayList<Thread>();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION) {
                    Thread t = w.thread;
                    if (t != null)
                        list.add(t);
                }
            }
            return list;
        }
    }
    
    
    


    /**
     * Setup to support compareAndSet. We need to natively implement
     * this here: For the sake of permitting future enhancements, we
     * cannot explicitly subclass AtomicInteger, which would be
     * efficient and useful otherwise. So, as the lesser of evils, we
     * natively implement using hotspot intrinsics API. And while we
     * are at it, we do the same for other CASable fields (which could
     * otherwise be done with atomic field updaters).
     */
    private static final Unsafe unsafe = Unsafe.getUnsafe();    // Unsafe类实例
    private static final long stateOffset;      // state内存偏移地址
    private static final long headOffset;       // head内存偏移地址
    private static final long tailOffset;       // tail内存偏移地址
    private static final long waitStatusOffset; // waitStatus内存偏移地址
    private static final long nextOffset;       // next内存偏移地址

    static {
        try {
            stateOffset = unsafe.objectFieldOffset
                    (AbstractQueuedSynchronizer.class.getDeclaredField("state"));
            headOffset = unsafe.objectFieldOffset
                    (AbstractQueuedSynchronizer.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset
                    (AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
            waitStatusOffset = unsafe.objectFieldOffset
                    (Node.class.getDeclaredField("waitStatus"));
            nextOffset = unsafe.objectFieldOffset
                    (Node.class.getDeclaredField("next"));

        } catch (Exception ex) { throw new Error(ex); }
    }

    /**
     * CAS head field. Used only by enq.
     */
    private final boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }

    /**
     * CAS tail field. Used only by enq.
     */
    private final boolean compareAndSetTail(Node expect, Node update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    /**
     * CAS waitStatus field of a node.
     */
    private static final boolean compareAndSetWaitStatus(Node node,
                                                         int expect,
                                                         int update) {
        return unsafe.compareAndSwapInt(node, waitStatusOffset,
                expect, update);
    }

    /**
     * CAS next field of a node.
     */
    private static final boolean compareAndSetNext(Node node,
                                                   Node expect,
                                                   Node update) {
        return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
    }
}
