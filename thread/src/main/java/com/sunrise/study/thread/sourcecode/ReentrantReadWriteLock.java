package com.sunrise.study.thread.sourcecode;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
//import java.lang.ThreadLocal;

/**
 * 读写锁支持锁降级（写锁降为读锁），但是不支持锁升级（读锁升级为写锁）
 * 因为读锁是共享锁，如果其中一个线程把获取的读锁升级为写锁，
 * 则更新的数据不能立即被其他获取读锁的线程所见
 * @author huangzihua
 * @date 2021-08-11
 */
public class ReentrantReadWriteLock implements ReadWriteLock {
    private static final long serialVersionUID = -6992448646407690164L;
    /** Inner class providing readlock */
    private final ReentrantReadWriteLock.ReadLock readerLock;       // 读锁
    /** Inner class providing writelock */
    private final ReentrantReadWriteLock.WriteLock writerLock;      // 写锁
    /** Performs all synchronization mechanics */
    final Sync sync;

    /**
     * 构造函数，默认是非公平锁
     * Creates a new {@code ReentrantReadWriteLock} with
     * default (nonfair) ordering properties.
     */
    public ReentrantReadWriteLock() {
        this(false);
    }

    /**
     * 构造函数
     * Creates a new {@code ReentrantReadWriteLock} with
     * the given fairness policy.
     *
     * @param fair {@code true} if this lock should use a fair ordering policy
     */
    public ReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
        readerLock = new ReadLock(this);
        writerLock = new WriteLock(this);
    }

    public ReentrantReadWriteLock.WriteLock writeLock() { return writerLock; }
    public ReentrantReadWriteLock.ReadLock  readLock()  { return readerLock; }

    /**
     * Synchronization implementation for ReentrantReadWriteLock.
     * Subclassed into fair and nonfair versions.
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 6317671515068378041L;

        /*
          Read vs write count extraction constants and functions.
          Lock state is logically divided into two unsigned shorts:
          The lower one representing the exclusive (writer) lock hold count,
          and the upper the shared (reader) hold count.
         */

        static final int SHARED_SHIFT   = 16;        // 高16位为读锁，低16位为写锁
        static final int SHARED_UNIT    = (1 << SHARED_SHIFT);      // 读锁单位
        static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1;  // 读锁最大数量
        static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;  // 写锁最大数量

        /** Returns the number of shared holds represented in count  */
        // 表示占有读锁的线程数量
        // 直接将state右移16位，就可以得到读锁的线程数量，因为state的高16位表示读锁，对应的低十六位表示写锁数量
        // 返回共享锁的数量
        static int sharedCount(int c)    { return c >>> SHARED_SHIFT; }

        // 表示占有写锁的线程数量
        // 直接将状态state和(2^16 - 1)做与运算，其等效于将state模上2^16。写锁数量由state的低十六位表示。
        // 返回独占锁的数量
        /** Returns the number of exclusive holds represented in count  */
        static int exclusiveCount(int c) { return c & EXCLUSIVE_MASK; }

        /**
         * 读计数器
         * A counter for per-thread read hold counts.
         * Maintained as a ThreadLocal; cached in cachedHoldCounter
         */
        static final class HoldCounter {
            // 某个读线程重入的次数
            int count = 0;

            // 当前线程的tid
            // Use id, not reference, to avoid garbage retention
            final long tid = getThreadId(Thread.currentThread());
        }

        /**
         * 本地线程计数器
         * ThreadLocal subclass. Easiest to explicitly define for sake
         * of deserialization mechanics.
         */
        static final class ThreadLocalHoldCounter
                extends ThreadLocal<HoldCounter> {

            /*
                重写了ThreadLocal的initialValue()方法
                get()时候，若没有值则会调用此方法初始化值
             */
            public HoldCounter initialValue() {
                return new HoldCounter();
            }
        }

        /**
         * 可重入读锁的数量
         * 仅在构造器和readObject方法中初始化，当数量减为0时删除。
         */
        private transient ThreadLocalHoldCounter readHolds;

        // 缓存的计数器
        private transient HoldCounter cachedHoldCounter;

        // 第一个读线程
        private transient Thread firstReader = null;

        // 第一个读线程的计数
        private transient int firstReaderHoldCount;

        Sync() {
            readHolds = new ThreadLocalHoldCounter();
            // 不是很理解这一步是什么操作
            setState(getState()); // ensures visibility of readHolds
        }

        /*
         * Acquires and releases use the same code for fair and
         * nonfair locks, but differ in whether/how they allow barging
         * when queues are non-empty.
         */

        /**
         * Returns true if the current thread, when trying to acquire the read lock,
         * and otherwise eligible to do so, should block because of policy for overtaking other waiting threads.
         */
        abstract boolean readerShouldBlock();

        /**
         * Returns true if the current thread, when trying to acquire
         * the write lock, and otherwise eligible to do so, should block
         * because of policy for overtaking other waiting threads.
         */
        abstract boolean writerShouldBlock();

        /*
         * Note that tryRelease and tryAcquire can be called by
         * Conditions. So it is possible that their arguments contain
         * both read and write holds that are all released during a
         * condition wait and re-established in tryAcquire.
         */


        /**
         * 释放写锁资源。如果该写锁非当前线程独占，则会抛出IllegalMonitorStateException异常
         */
        protected final boolean tryRelease(int releases) {
            // 判断独占线程是否为当前线程
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();

            // 计算释放资源后的写锁的数量
            int nextc = getState() - releases;

            // 释放写锁
            boolean free = exclusiveCount(nextc) == 0;  // 计算释放后的写锁数量，并判断是否等于0（即完全释放）
            if (free)
                setExclusiveOwnerThread(null);  // 释放成功，设独占线程为null
            setState(nextc);        // 修改锁状态
            return free;
        }


        /**
         * 尝试获取写锁。
         * 返回true表示成功拿到写锁资源，返回false则表明抢不到写锁，或有线程在占用写锁或读锁
         */
        protected final boolean tryAcquire(int acquires) {
            /*
             * Walkthrough:
             * 1. If read count nonzero or write count nonzero
             *    and owner is a different thread, fail.
             * 2. If count would saturate, fail. (This can only
             *    happen if count is already nonzero.)
             * 3. Otherwise, this thread is eligible for lock if
             *    it is either a reentrant acquire or
             *    queue policy allows it. If so, update state
             *    and set owner.
             */
            Thread current = Thread.currentThread();
            int c = getState();
            int w = exclusiveCount(c);      // 写锁资源重入数量
            if (c != 0) {   // 锁状态state不为0
                /*
                    如果写锁数量为0，说明读锁在占用，则返回false，无法获得写锁资源
                    如果写锁数量不为0，且当前线程不是独占线程，说明有别的线程在占用写锁，则无法获取资源
                 */
                if (w == 0 || current != getExclusiveOwnerThread())
                    return false;

                /*
                    如果写锁数量不为0，且当前线程是独占线程，则计算重入后的写锁数量，并判断是否大于最大锁数量，大于则抛错误
                 */
                if (w + exclusiveCount(acquires) > MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");

                // 重入获取资源
                setState(c + acquires);
                return true;
            }

            /*
                如果锁状态为0，则调用writerShouldBlock()方法：
                    公平锁实现：如果前面有等待线程，则返回true，没有则返回false
                    非公平锁实现：总是返回false
                若writerShouldBlock方法返回true，则tryAcquire尝试获取锁资源失败；
                若writerShouldBlock方法返回false，则尝试修改state状态，以获取锁资源
                    若state修改成功，说明获取锁资源成功，则设置独占线程为当前线程并返回true
                    否则修改失败，则返回false。
             */
            if (writerShouldBlock() ||
                    !compareAndSetState(c, c + acquires))
                return false;
            setExclusiveOwnerThread(current);
            return true;
        }

        protected final boolean tryReleaseShared(int unused) {
            Thread current = Thread.currentThread();
            if (firstReader == current) {
                // assert firstReaderHoldCount > 0;
                if (firstReaderHoldCount == 1)
                    firstReader = null;
                else
                    firstReaderHoldCount--;
            } else {
                HoldCounter rh = cachedHoldCounter;
                if (rh == null || rh.tid != getThreadId(current))
                    rh = readHolds.get();
                int count = rh.count;
                if (count <= 1) {
                    readHolds.remove();
                    if (count <= 0)
                        throw unmatchedUnlockException();
                }
                --rh.count;
            }
            for (;;) {
                int c = getState();
                int nextc = c - SHARED_UNIT;
                if (compareAndSetState(c, nextc))
                    // Releasing the read lock has no effect on readers,
                    // but it may allow waiting writers to proceed if
                    // both read and write locks are now free.
                    return nextc == 0;
            }
        }

        private IllegalMonitorStateException unmatchedUnlockException() {
            return new IllegalMonitorStateException(
                    "attempt to unlock read lock, not locked by current thread");
        }

        /**
         * ReentrantReadWriteLock的实现方法
         */
        protected final int tryAcquireShared(int unused) {
            Thread current = Thread.currentThread();
            int c = getState();

            /*
                简略描述：如果存在写锁，返回-1，表示失败。
                若写锁持有数量不等于0，且写锁持有者不是当前线程，则获取读锁失败，返回-1
             */
            if (exclusiveCount(c) != 0 &&
                    getExclusiveOwnerThread() != current)
                return -1;

            // 获取读锁持有数量
            int r = sharedCount(c);

            /*
                readerShouldBlock方法判断是不是要等待：
                        公平锁实现：若队列有等待线程（无论独占还是共享），返回true，否则返回false
                        非公平锁实现：若队列的头一个等待线程是独占模式，则返回true，其他情况返回false
                接着判断读锁持有数量是否超过最大值
                若没有超过，则调用compareAndSetState尝试获取读锁
             */
            if (!readerShouldBlock() &&
                    r < MAX_COUNT &&
                    compareAndSetState(c, c + SHARED_UNIT)) {

                if (r == 0) {
                    // 若读锁持有数量为0
                    firstReader = current;
                    firstReaderHoldCount = 1;
                } else if (firstReader == current) {
                    // 若第一个获取读锁的线程就是当前线程，则第一个读锁持有数+1。
                    firstReaderHoldCount++;
                } else {
                    // 取缓存计数器
                    HoldCounter rh = cachedHoldCounter;

                    // 若缓存为null 或 判断缓存计数器是不是属于当前线程
                    if (rh == null || rh.tid != getThreadId(current))
                        // 缓存计数器不是当前线程，则获取当前线程的计数器并缓存起来
                        cachedHoldCounter = rh = readHolds.get();   // get()会返回当前线程的缓存计数器，若没有则会初始化一个
                    else if (rh.count == 0)
                        readHolds.set(rh);  // 更新缓存

                    // 计数器+1，说明读锁持有数量+1
                    rh.count++;
                }

                // 获取读锁成功
                return 1;
            }

            // readerShouldBlock()返回true 或 读锁持有数超过最大限制 或 compareAndSetState调用失败
            return fullTryAcquireShared(current);
        }

        /**
         * 完整版获取读锁资源。
         * Full version of acquire for reads, that handles CAS misses and reentrant reads not dealt with in tryAcquireShared.
         */
        final int fullTryAcquireShared(Thread current) {
            /*
             * This code is in part redundant with that in tryAcquireShared
             * but is simpler overall by not complicating tryAcquireShared with interactions between retries and lazily reading hold counts.
             */
            HoldCounter rh = null;
            for (;;) {
                int c = getState();

                // 下面这段代码，还是在校验当前线程是不是可以竞争读锁，或是否已经拿到读锁了。
                if (exclusiveCount(c) != 0) {
                    if (getExclusiveOwnerThread() != current)
                        // 若有别的线程持有写锁，则返回-1
                        return -1;
                    // else we hold the exclusive lock; blocking here
                    // would cause deadlock.
                } else if (readerShouldBlock()) {       // 通过readerShouldBlock判断是否阻塞
                    /* 和tryAcquireShared方法的区别在于多了下面这段代码 */
                    // Make sure we're not acquiring read lock reentrantly
                    if (firstReader == current) {
                        // 再次判断下第一个持有读锁的线程是不是当前线程
                        // assert firstReaderHoldCount > 0;
                    } else {

                        if (rh == null) {
                            // 如果HoldCounter为null，则从缓存里取
                            rh = cachedHoldCounter;
                            if (rh == null || rh.tid != getThreadId(current)) {
                                // 如果缓存也没有，或缓存的计数器不是当前线程的，则重新从ThreadLocal中get
                                rh = readHolds.get();
                                if (rh.count == 0)      // 如果读锁持有数为0，则移除ThreadLocal变量
                                    readHolds.remove();
                            }
                        }
                        if (rh.count == 0)      // 如果读锁持有数为0，则返回-1
                            return -1;
                    }
                }

                // 校验读锁持有数量是否已达最大限值，是则抛出错误
                if (sharedCount(c) == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");

                // 通过CAS修改锁状态state，尝试获取读锁资源
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (sharedCount(c) == 0) {
                        // 若读锁持有数量为0
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {
                        // 若第一个获取读锁的线程就是当前线程，则第一个读锁持有数+1
                        firstReaderHoldCount++;
                    } else {
                        if (rh == null)
                            // 如果HoldCounter为null，则从缓存里取
                            rh = cachedHoldCounter;

                        if (rh == null || rh.tid != getThreadId(current))
                            // 如果缓存也没有，或缓存的计数器不是当前线程的，则重新从ThreadLocal中get
                            rh = readHolds.get();
                        else if (rh.count == 0)
                            readHolds.set(rh);      // 初始化ThreadLocalHoldCounter
                        rh.count++;
                        cachedHoldCounter = rh; // cache for release
                    }
                    return 1;
                }
            }
        }

        /**
         * 尝试获取写锁，逻辑和tryAcquire差不多
         */
        final boolean tryWriteLock() {
            Thread current = Thread.currentThread();
            int c = getState();
            if (c != 0) {
                int w = exclusiveCount(c);
                if (w == 0 || current != getExclusiveOwnerThread())
                    return false;
                if (w == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
            }
            if (!compareAndSetState(c, c + 1))
                return false;
            setExclusiveOwnerThread(current);
            return true;
        }

        /**
         * Performs tryLock for read, enabling barging in both modes.
         * This is identical in effect to tryAcquireShared except for
         * lack of calls to readerShouldBlock.
         */
        final boolean tryReadLock() {
            Thread current = Thread.currentThread();
            for (;;) {
                int c = getState();
                if (exclusiveCount(c) != 0 &&
                        getExclusiveOwnerThread() != current)
                    return false;
                int r = sharedCount(c);
                if (r == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (r == 0) {
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {
                        firstReaderHoldCount++;
                    } else {
                        HoldCounter rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current))
                            cachedHoldCounter = rh = readHolds.get();
                        else if (rh.count == 0)
                            readHolds.set(rh);
                        rh.count++;
                    }
                    return true;
                }
            }
        }

        // 判断锁独占线程是不是当前线程
        protected final boolean isHeldExclusively() {
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        // Methods relayed to outer class

        /**
         * 创建Condition实例
         */
        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        final Thread getOwner() {
            // Must read state before owner to ensure memory consistency
            return ((exclusiveCount(getState()) == 0) ?
                    null :
                    getExclusiveOwnerThread());
        }

        final int getReadLockCount() {
            return sharedCount(getState());
        }

        final boolean isWriteLocked() {
            return exclusiveCount(getState()) != 0;
        }

        /**
         * 获取写锁的持有数量，如果当前线程未持有锁，则返回0
         */
        final int getWriteHoldCount() {
            return isHeldExclusively() ? exclusiveCount(getState()) : 0;
        }

        final int getReadHoldCount() {
            if (getReadLockCount() == 0)
                return 0;

            Thread current = Thread.currentThread();
            if (firstReader == current)
                return firstReaderHoldCount;

            HoldCounter rh = cachedHoldCounter;
            if (rh != null && rh.tid == getThreadId(current))
                return rh.count;

            int count = readHolds.get().count;
            if (count == 0) readHolds.remove();
            return count;
        }

        /**
         * Reconstitutes the instance from a stream (that is, deserializes it).
         */
        private void readObject(java.io.ObjectInputStream s)
                throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            readHolds = new ThreadLocalHoldCounter();
            setState(0); // reset to unlocked state
        }

        final int getCount() { return getState(); }
    }

    /**
     * Nonfair version of Sync
     */
    static final class NonfairSync extends ReentrantReadWriteLock.Sync {
        private static final long serialVersionUID = -8159625535654395037L;

        /**
         * 在尝试获取写锁时是否应该等待
         * 非公平锁实现：总是返回false，即不等待，立刻竞争锁资源
         */
        final boolean writerShouldBlock() {
            return false; // writers can always barge
        }

        /**
         * 在尝试获取读锁时是否应该等待
         * 非公平锁实现：若wait队列不为空，且第一个等待线程的锁模式是
         *              独占锁，则需要等待，其他情况不用等待
         */
        final boolean readerShouldBlock() {
            /* As a heuristic to avoid indefinite writer starvation,
             * block if the thread that momentarily appears to be head
             * of queue, if one exists, is a waiting writer.  This is
             * only a probabilistic effect since a new reader will not
             * block if there is a waiting writer behind other enabled
             * readers that have not yet drained from the queue.
             */
            // 检查下wait队列，看下第一个等待的线程是不是独占模式
            return apparentlyFirstQueuedIsExclusive();
        }
    }

    /**
     * Fair version of Sync
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -2274990926593161451L;

        /**
         * 在尝试获取写锁时是否应该等待
         * 公平锁实现：调用hasQueuedPredecessors()方法判断是不是有线程在等待了，如果是则返回true
         */
        final boolean writerShouldBlock() {
            return hasQueuedPredecessors();
        }

        /**
         * 在尝试获取读锁时是否应该等待
         * 公平锁实现：调用hasQueuedPredecessors()方法判断是不是有线程在等待了，如果是则返回true
         */
        final boolean readerShouldBlock() {
            return hasQueuedPredecessors();
        }
    }

    /**
     * 读锁。实现的是共享锁。
     */
    public static class ReadLock implements Lock, java.io.Serializable {
        private static final long serialVersionUID = -5992448646407690164L;
        private final ReentrantReadWriteLock.Sync sync;

        /**
         * 构造函数
         * Constructor for use by subclasses
         *
         * @param lock the outer lock object
         * @throws NullPointerException if the lock is null
         */
        protected ReadLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        /**
         * 获取读锁。
         * 如果写锁没有被其他线程占用，则当前线程可以直接获得读锁
         * 如果写锁被其他线程持有，那么当前线程会进入休眠状态，直到获取到读锁
         */
        public void lock() {
            // 底层调用AQS的acquireShared方法
            sync.acquireShared(1);
        }

        /**
         * Acquires the read lock unless the current thread is
         * {@linkplain Thread#interrupt interrupted}.
         *
         * <p>Acquires the read lock if the write lock is not held
         * by another thread and returns immediately.
         *
         * <p>If the write lock is held by another thread then the
         * current thread becomes disabled for thread scheduling
         * purposes and lies dormant until one of two things happens:
         *
         * <ul>
         *
         * <li>The read lock is acquired by the current thread; or
         *
         * <li>Some other thread {@linkplain Thread#interrupt interrupts}
         * the current thread.
         *
         * </ul>
         *
         * <p>If the current thread:
         *
         * <ul>
         *
         * <li>has its interrupted status set on entry to this method; or
         *
         * <li>is {@linkplain Thread#interrupt interrupted} while
         * acquiring the read lock,
         *
         * </ul>
         *
         * then {@link InterruptedException} is thrown and the current
         * thread's interrupted status is cleared.
         *
         * <p>In this implementation, as this method is an explicit
         * interruption point, preference is given to responding to
         * the interrupt over normal or reentrant acquisition of the
         * lock.
         *
         * @throws InterruptedException if the current thread is interrupted
         */
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }

        /**
         * Acquires the read lock only if the write lock is not held by
         * another thread at the time of invocation.
         *
         * <p>Acquires the read lock if the write lock is not held by
         * another thread and returns immediately with the value
         * {@code true}. Even when this lock has been set to use a
         * fair ordering policy, a call to {@code tryLock()}
         * <em>will</em> immediately acquire the read lock if it is
         * available, whether or not other threads are currently
         * waiting for the read lock.  This &quot;barging&quot; behavior
         * can be useful in certain circumstances, even though it
         * breaks fairness. If you want to honor the fairness setting
         * for this lock, then use {@link #tryLock(long, TimeUnit)
         * tryLock(0, TimeUnit.SECONDS) } which is almost equivalent
         * (it also detects interruption).
         *
         * <p>If the write lock is held by another thread then
         * this method will return immediately with the value
         * {@code false}.
         *
         * @return {@code true} if the read lock was acquired
         */
        public boolean tryLock() {
            return sync.tryReadLock();
        }

        /**
         * Acquires the read lock if the write lock is not held by
         * another thread within the given waiting time and the
         * current thread has not been {@linkplain Thread#interrupt
         * interrupted}.
         *
         * <p>Acquires the read lock if the write lock is not held by
         * another thread and returns immediately with the value
         * {@code true}. If this lock has been set to use a fair
         * ordering policy then an available lock <em>will not</em> be
         * acquired if any other threads are waiting for the
         * lock. This is in contrast to the {@link #tryLock()}
         * method. If you want a timed {@code tryLock} that does
         * permit barging on a fair lock then combine the timed and
         * un-timed forms together:
         *
         *  <pre> {@code
         * if (lock.tryLock() ||
         *     lock.tryLock(timeout, unit)) {
         *   ...
         * }}</pre>
         *
         * <p>If the write lock is held by another thread then the
         * current thread becomes disabled for thread scheduling
         * purposes and lies dormant until one of three things happens:
         *
         * <ul>
         *
         * <li>The read lock is acquired by the current thread; or
         *
         * <li>Some other thread {@linkplain Thread#interrupt interrupts}
         * the current thread; or
         *
         * <li>The specified waiting time elapses.
         *
         * </ul>
         *
         * <p>If the read lock is acquired then the value {@code true} is
         * returned.
         *
         * <p>If the current thread:
         *
         * <ul>
         *
         * <li>has its interrupted status set on entry to this method; or
         *
         * <li>is {@linkplain Thread#interrupt interrupted} while
         * acquiring the read lock,
         *
         * </ul> then {@link InterruptedException} is thrown and the
         * current thread's interrupted status is cleared.
         *
         * <p>If the specified waiting time elapses then the value
         * {@code false} is returned.  If the time is less than or
         * equal to zero, the method will not wait at all.
         *
         * <p>In this implementation, as this method is an explicit
         * interruption point, preference is given to responding to
         * the interrupt over normal or reentrant acquisition of the
         * lock, and over reporting the elapse of the waiting time.
         *
         * @param timeout the time to wait for the read lock
         * @param unit the time unit of the timeout argument
         * @return {@code true} if the read lock was acquired
         * @throws InterruptedException if the current thread is interrupted
         * @throws NullPointerException if the time unit is null
         */
        public boolean tryLock(long timeout, TimeUnit unit)
                throws InterruptedException {
            return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
        }

        /**
         * Attempts to release this lock.
         *
         * <p>If the number of readers is now zero then the lock
         * is made available for write lock attempts.
         */
        public void unlock() {
            sync.releaseShared(1);
        }

        /**
         * Throws {@code UnsupportedOperationException} because
         * {@code ReadLocks} do not support conditions.
         *
         * @throws UnsupportedOperationException always
         */
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        /**
         * Returns a string identifying this lock, as well as its lock state.
         * The state, in brackets, includes the String {@code "Read locks ="}
         * followed by the number of held read locks.
         *
         * @return a string identifying this lock, as well as its lock state
         */
        public String toString() {
            int r = sync.getReadLockCount();
            return super.toString() +
                    "[Read locks = " + r + "]";
        }
    }

    /**
     * 写锁实现类，实现的是独占模式
     */
    public static class WriteLock implements Lock, java.io.Serializable {
        private static final long serialVersionUID = -4992448646407690164L;
        private final ReentrantReadWriteLock.Sync sync;

        /**
         * Constructor for use by subclasses
         *
         * @param lock the outer lock object
         * @throws NullPointerException if the lock is null
         */
        protected WriteLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        /**
         * 锁定，获取写锁资源。
         * 如果没有其他线程占有读锁或写锁，则当前线程立即获得写锁，并持有数设为1。
         * 如果当前线程已经持有该锁，则持有资源数+1。
         * 如果锁资源被其他线程占用，那么当前线程会加入到wait队列，等待写锁资源。
         */
        public void lock() {
            // 正常调用aqs的acquire方法，独占模式获取锁
            sync.acquire(1);
        }

        /**
         * 获取写锁资源，除非当前线程被中断。
         * 如果没有其他线程占有读锁或写锁，则当前线程立即获得写锁，并持有数设为1。
         * 如果当前线程已经持有该锁，则持有资源数+1。
         * 如果锁资源被其他线程占用，那么当前线程会加入到wait队列，进入等待状态。直到下面两个事件中的一个发生：
         *     1.当前线程拿到写锁
         *     2.其他线程中断当前线程
         */
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }

        // 尝试获取锁，底层是调用Sync的tryWriteLock方法，该方法实现逻辑和tryAcquire方法差不多
        /**
         * Acquires the write lock only if it is not held by another thread
         * at the time of invocation.
         *
         * <p>Acquires the write lock if neither the read nor write lock
         * are held by another thread
         * and returns immediately with the value {@code true},
         * setting the write lock hold count to one. Even when this lock has
         * been set to use a fair ordering policy, a call to
         * {@code tryLock()} <em>will</em> immediately acquire the
         * lock if it is available, whether or not other threads are
         * currently waiting for the write lock.  This &quot;barging&quot;
         * behavior can be useful in certain circumstances, even
         * though it breaks fairness. If you want to honor the
         * fairness setting for this lock, then use {@link
         * #tryLock(long, TimeUnit) tryLock(0, TimeUnit.SECONDS) }
         * which is almost equivalent (it also detects interruption).
         *
         * <p>If the current thread already holds this lock then the
         * hold count is incremented by one and the method returns
         * {@code true}.
         *
         * <p>If the lock is held by another thread then this method
         * will return immediately with the value {@code false}.
         *
         * @return {@code true} if the lock was free and was acquired
         * by the current thread, or the write lock was already held
         * by the current thread; and {@code false} otherwise.
         */
        public boolean tryLock( ) {
            return sync.tryWriteLock();
        }

        // 在指定时间内尝试获取锁资源。
        /**
         * Acquires the write lock if it is not held by another thread
         * within the given waiting time and the current thread has
         * not been {@linkplain Thread#interrupt interrupted}.
         *
         * <p>Acquires the write lock if neither the read nor write lock
         * are held by another thread
         * and returns immediately with the value {@code true},
         * setting the write lock hold count to one. If this lock has been
         * set to use a fair ordering policy then an available lock
         * <em>will not</em> be acquired if any other threads are
         * waiting for the write lock. This is in contrast to the {@link
         * #tryLock()} method. If you want a timed {@code tryLock}
         * that does permit barging on a fair lock then combine the
         * timed and un-timed forms together:
         *
         *  <pre> {@code
         * if (lock.tryLock() ||
         *     lock.tryLock(timeout, unit)) {
         *   ...
         * }}</pre>
         *
         * <p>If the current thread already holds this lock then the
         * hold count is incremented by one and the method returns
         * {@code true}.
         *
         * <p>If the lock is held by another thread then the current
         * thread becomes disabled for thread scheduling purposes and
         * lies dormant until one of three things happens:
         *
         * <ul>
         *
         * <li>The write lock is acquired by the current thread; or
         *
         * <li>Some other thread {@linkplain Thread#interrupt interrupts}
         * the current thread; or
         *
         * <li>The specified waiting time elapses
         *
         * </ul>
         *
         * <p>If the write lock is acquired then the value {@code true} is
         * returned and the write lock hold count is set to one.
         *
         * <p>If the current thread:
         *
         * <ul>
         *
         * <li>has its interrupted status set on entry to this method;
         * or
         *
         * <li>is {@linkplain Thread#interrupt interrupted} while
         * acquiring the write lock,
         *
         * </ul>
         *
         * then {@link InterruptedException} is thrown and the current
         * thread's interrupted status is cleared.
         *
         * <p>If the specified waiting time elapses then the value
         * {@code false} is returned.  If the time is less than or
         * equal to zero, the method will not wait at all.
         *
         * <p>In this implementation, as this method is an explicit
         * interruption point, preference is given to responding to
         * the interrupt over normal or reentrant acquisition of the
         * lock, and over reporting the elapse of the waiting time.
         *
         * @param timeout the time to wait for the write lock
         * @param unit the time unit of the timeout argument
         *
         * @return {@code true} if the lock was free and was acquired
         * by the current thread, or the write lock was already held by the
         * current thread; and {@code false} if the waiting time
         * elapsed before the lock could be acquired.
         *
         * @throws InterruptedException if the current thread is interrupted
         * @throws NullPointerException if the time unit is null
         */
        public boolean tryLock(long timeout, TimeUnit unit)
                throws InterruptedException {

            // 在指定时间内尝试获取锁资源，单位是纳秒
            return sync.tryAcquireNanos(1, unit.toNanos(timeout));
        }

        /**
         * 释放写锁。底层是交由Sync来实现
         * 如果当前线程持有写锁，则写锁持有数量-1，若数量减到0，则释放写锁。
         * 如果写锁不是被当前线程持有，而调用了此方法，则抛出异常。
         *
         * @throws IllegalMonitorStateException if the current thread does not
         * hold this lock
         */
        public void unlock() {
            // 释放独占线程的锁资源，并唤醒下一个等待线程
            sync.release(1);
        }


        /**
         * 新建写锁的Condition实例
         *
         * Returns a {@link Condition} instance for use with this
         * {@link Lock} instance.
         * <p>The returned {@link Condition} instance supports the same
         * usages as do the {@link Object} monitor methods ({@link
         * Object#wait() wait}, {@link Object#notify notify}, and {@link
         * Object#notifyAll notifyAll}) when used with the built-in
         * monitor lock.
         *
         * <ul>
         *
         * <li>If this write lock is not held when any {@link
         * Condition} method is called then an {@link
         * IllegalMonitorStateException} is thrown.  (Read locks are
         * held independently of write locks, so are not checked or
         * affected. However it is essentially always an error to
         * invoke a condition waiting method when the current thread
         * has also acquired read locks, since other threads that
         * could unblock it will not be able to acquire the write
         * lock.)
         *
         * <li>When the condition {@linkplain Condition#await() waiting}
         * methods are called the write lock is released and, before
         * they return, the write lock is reacquired and the lock hold
         * count restored to what it was when the method was called.
         *
         * <li>If a thread is {@linkplain Thread#interrupt interrupted} while
         * waiting then the wait will terminate, an {@link
         * InterruptedException} will be thrown, and the thread's
         * interrupted status will be cleared.
         *
         * <li> Waiting threads are signalled in FIFO order.
         *
         * <li>The ordering of lock reacquisition for threads returning
         * from waiting methods is the same as for threads initially
         * acquiring the lock, which is in the default case not specified,
         * but for <em>fair</em> locks favors those threads that have been
         * waiting the longest.
         *
         * </ul>
         *
         * @return the Condition object
         */
        public Condition newCondition() {
            return sync.newCondition();
        }

        /**
         * Returns a string identifying this lock, as well as its lock
         * state.  The state, in brackets includes either the String
         * {@code "Unlocked"} or the String {@code "Locked by"}
         * followed by the {@linkplain Thread#getName name} of the owning thread.
         *
         * @return a string identifying this lock, as well as its lock state
         */
        public String toString() {
            Thread o = sync.getOwner();
            return super.toString() + ((o == null) ?
                    "[Unlocked]" :
                    "[Locked by thread " + o.getName() + "]");
        }

        /**
         * 判断锁的独占线程是不是当前线程
         * Queries if this write lock is held by the current thread.
         * Identical in effect to {@link
         * ReentrantReadWriteLock#isWriteLockedByCurrentThread}.
         *
         * @return {@code true} if the current thread holds this lock and
         *         {@code false} otherwise
         * @since 1.6
         */
        public boolean isHeldByCurrentThread() {
            return sync.isHeldExclusively();
        }


        /**
         * 获取写锁的持有数量，如果当前线程未持有锁，则返回0
         *
         * Queries the number of holds on this write lock by the current
         * thread.  A thread has a hold on a lock for each lock action
         * that is not matched by an unlock action.  Identical in effect
         * to {@link ReentrantReadWriteLock#getWriteHoldCount}.
         *
         * @return the number of holds on this lock by the current thread,
         *         or zero if this lock is not held by the current thread
         * @since 1.6
         */
        public int getHoldCount() {
            return sync.getWriteHoldCount();
        }
    }

    // Instrumentation and status

    /**
     * Returns {@code true} if this lock has fairness set true.
     *
     * @return {@code true} if this lock has fairness set true
     */
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * Returns the thread that currently owns the write lock, or
     * {@code null} if not owned. When this method is called by a
     * thread that is not the owner, the return value reflects a
     * best-effort approximation of current lock status. For example,
     * the owner may be momentarily {@code null} even if there are
     * threads trying to acquire the lock but have not yet done so.
     * This method is designed to facilitate construction of
     * subclasses that provide more extensive lock monitoring
     * facilities.
     *
     * @return the owner, or {@code null} if not owned
     */
    protected Thread getOwner() {
        return sync.getOwner();
    }

    /**
     * Queries the number of read locks held for this lock. This
     * method is designed for use in monitoring system state, not for
     * synchronization control.
     * @return the number of read locks held
     */
    public int getReadLockCount() {
        return sync.getReadLockCount();
    }

    /**
     * Queries if the write lock is held by any thread. This method is
     * designed for use in monitoring system state, not for
     * synchronization control.
     *
     * @return {@code true} if any thread holds the write lock and
     *         {@code false} otherwise
     */
    public boolean isWriteLocked() {
        return sync.isWriteLocked();
    }

    /**
     * Queries if the write lock is held by the current thread.
     *
     * @return {@code true} if the current thread holds the write lock and
     *         {@code false} otherwise
     */
    public boolean isWriteLockedByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * Queries the number of reentrant write holds on this lock by the
     * current thread.  A writer thread has a hold on a lock for
     * each lock action that is not matched by an unlock action.
     *
     * @return the number of holds on the write lock by the current thread,
     *         or zero if the write lock is not held by the current thread
     */
    public int getWriteHoldCount() {
        return sync.getWriteHoldCount();
    }

    /**
     * Queries the number of reentrant read holds on this lock by the
     * current thread.  A reader thread has a hold on a lock for
     * each lock action that is not matched by an unlock action.
     *
     * @return the number of holds on the read lock by the current thread,
     *         or zero if the read lock is not held by the current thread
     * @since 1.6
     */
    public int getReadHoldCount() {
        return sync.getReadHoldCount();
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire the write lock.  Because the actual set of threads may
     * change dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive lock monitoring facilities.
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedWriterThreads() {
        return sync.getExclusiveQueuedThreads();
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire the read lock.  Because the actual set of threads may
     * change dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive lock monitoring facilities.
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedReaderThreads() {
        return sync.getSharedQueuedThreads();
    }

    /**
     * Queries whether any threads are waiting to acquire the read or
     * write lock. Note that because cancellations may occur at any
     * time, a {@code true} return does not guarantee that any other
     * thread will ever acquire a lock.  This method is designed
     * primarily for use in monitoring of the system state.
     *
     * @return {@code true} if there may be other threads waiting to
     *         acquire the lock
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * Queries whether the given thread is waiting to acquire either
     * the read or write lock. Note that because cancellations may
     * occur at any time, a {@code true} return does not guarantee
     * that this thread will ever acquire a lock.  This method is
     * designed primarily for use in monitoring of the system state.
     *
     * @param thread the thread
     * @return {@code true} if the given thread is queued waiting for this lock
     * @throws NullPointerException if the thread is null
     */
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    /**
     * Returns an estimate of the number of threads waiting to acquire
     * either the read or write lock.  The value is only an estimate
     * because the number of threads may change dynamically while this
     * method traverses internal data structures.  This method is
     * designed for use in monitoring of the system state, not for
     * synchronization control.
     *
     * @return the estimated number of threads waiting for this lock
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire either the read or write lock.  Because the actual set
     * of threads may change dynamically while constructing this
     * result, the returned collection is only a best-effort estimate.
     * The elements of the returned collection are in no particular
     * order.  This method is designed to facilitate construction of
     * subclasses that provide more extensive monitoring facilities.
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * Queries whether any threads are waiting on the given condition
     * associated with the write lock. Note that because timeouts and
     * interrupts may occur at any time, a {@code true} return does
     * not guarantee that a future {@code signal} will awaken any
     * threads.  This method is designed primarily for use in
     * monitoring of the system state.
     *
     * @param condition the condition
     * @return {@code true} if there are any waiting threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    public boolean hasWaiters(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns an estimate of the number of threads waiting on the
     * given condition associated with the write lock. Note that because
     * timeouts and interrupts may occur at any time, the estimate
     * serves only as an upper bound on the actual number of waiters.
     * This method is designed for use in monitoring of the system
     * state, not for synchronization control.
     *
     * @param condition the condition
     * @return the estimated number of waiting threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    public int getWaitQueueLength(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns a collection containing those threads that may be
     * waiting on the given condition associated with the write lock.
     * Because the actual set of threads may change dynamically while
     * constructing this result, the returned collection is only a
     * best-effort estimate. The elements of the returned collection
     * are in no particular order.  This method is designed to
     * facilitate construction of subclasses that provide more
     * extensive condition monitoring facilities.
     *
     * @param condition the condition
     * @return the collection of threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns a string identifying this lock, as well as its lock state.
     * The state, in brackets, includes the String {@code "Write locks ="}
     * followed by the number of reentrantly held write locks, and the
     * String {@code "Read locks ="} followed by the number of held
     * read locks.
     *
     * @return a string identifying this lock, as well as its lock state
     */
    public String toString() {
        int c = sync.getCount();
        int w = Sync.exclusiveCount(c);
        int r = Sync.sharedCount(c);

        return super.toString() +
                "[Write locks = " + w + ", Read locks = " + r + "]";
    }

    /**
     * Returns the thread id for the given thread.  We must access
     * this directly rather than via method Thread.getId() because
     * getId() is not final, and has been known to be overridden in
     * ways that do not preserve unique mappings.
     */
    static final long getThreadId(Thread thread) {
        return UNSAFE.getLongVolatile(thread, TID_OFFSET);
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long TID_OFFSET;           // 线程TID的偏移地址
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            // 获取线程的tid字段的内存地址
            TID_OFFSET = UNSAFE.objectFieldOffset
                    (tk.getDeclaredField("tid"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}
