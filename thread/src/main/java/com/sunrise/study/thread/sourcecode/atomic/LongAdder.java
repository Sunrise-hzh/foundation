package com.sunrise.study.thread.sourcecode.atomic;

import java.io.Serializable;

/**
 * 虽然AtomicLong通过CAS提供了非阻塞的原子性操作，但是在高并发下，大量线程同时
 * 竞争更新同一个AtomicLong原子变量，又由于同时只能有一个线程的CAS操作会成功，
 * 竞争失败的线程会不停地循环尝试进行CAS操作，这就导致CPU资源浪费，因为这些自旋
 * 都是无意义的动作。所以此类LongAdder，就是为了解决这一个问题。
 * LongAdder在内部维护了一个延迟初始化的原子性更新数组cells和一个基值变量base
 * （详情看父类Striped64），当并发数较少时，只竞争使用base进行递增递减操作，当并发数
 * 较大时，会创建cells数组，线程改为竞争cells里面每个Cell变量。
 *
 * One or more variables that together maintain an initially zero
 * {@code long} sum.  When updates (method {@link #add}) are contended
 * across threads, the set of variables may grow dynamically to reduce
 * contention. Method {@link #sum} (or, equivalently, {@link
 * #longValue}) returns the current total combined across the
 * variables maintaining the sum.
 *
 * <p>This class is usually preferable to {@link AtomicLong} when
 * multiple threads update a common sum that is used for purposes such
 * as collecting statistics, not for fine-grained synchronization
 * control.  Under low update contention, the two classes have similar
 * characteristics. But under high contention, expected throughput of
 * this class is significantly higher, at the expense of higher space
 * consumption.
 *
 * <p>LongAdders can be used with a {@link
 * java.util.concurrent.ConcurrentHashMap} to maintain a scalable
 * frequency map (a form of histogram or multiset). For example, to
 * add a count to a {@code ConcurrentHashMap<String,LongAdder> freqs},
 * initializing if not already present, you can use {@code
 * freqs.computeIfAbsent(k -> new LongAdder()).increment();}
 *
 * <p>This class extends {@link Number}, but does <em>not</em> define
 * methods such as {@code equals}, {@code hashCode} and {@code
 * compareTo} because instances are expected to be mutated, and so are
 * not useful as collection keys.
 *
 * @since 1.8
 * @author Doug Lea
 * @author huangzihua
 * @date 2021-09-09
 */
public class LongAdder extends Striped64 {
    private static final long serialVersionUID = 7249069246863182397L;

    /**
     * 默认构造器，初始sum为0
     * Creates a new adder with initial sum of zero.
     */
    public LongAdder() {
    }

    /**
     * 加法运算。
     * Adds the given value.
     *
     * @param x the value to add 增量值
     */
    public void add(long x) {
        Cell[] as; long b, v; int m; Cell a;

        // 这里拿到cells数组，如果数组为空，则对base进行原子操作
        if ((as = cells) != null || !casBase(b = base, b + x)) {
            // cells不为null，或者base更新失败，就执行下面的步骤
            boolean uncontended = true; // 无竞争标识，初始值为true，表示无线程竞争资源

            /*
                (as=cells) == null 或                    // 若cells为null，则执行longAccumulate()
                (m=as.length-1) < 0 或                   // 若cells为空，则执行longAccumulate()
                (a=as[getProbe() & m ]) == null 或       // 若求余得到cells索引，根据索引获取cell元素为null，则执行longAccumulate()
                !(uncontended = a.cas(v=a.value, v+x))   // 若cell元素不为null，则执行cas替换旧值，若不成功，则执行longAccumulate()
                总结：上述操作就是获取当前线程应该访问的cells数组中的某个Cell元素，然后对其进行CAS更新操作，
                若上述的操作途中失败，就调用longAccumulate()方法
            */
            if (as == null || (m = as.length - 1) < 0 ||
                (a = as[getProbe() & m]) == null || // 若cells不为空，则计算应该获取哪个Cell进行计算
                !(uncontended = a.cas(v = a.value, v + x)))

                // 执行longAccumulate()方法
                longAccumulate(x, null, uncontended);
        }
    }

    /**
     * 自增操作，实际调用add(1L)方法
     * Equivalent to {@code add(1)}.
     */
    public void increment() {
        add(1L);
    }

    /**
     * 自减操作，实际调用add(-1L)方法
     * Equivalent to {@code add(-1)}.
     */
    public void decrement() {
        add(-1L);
    }

    /**
     * 计算总和并返回。
     * 实现逻辑就是：for循环累加每个Cell的值和base的值。注意，由于该类加操作没有加锁，
     * 因此返回的总和有可能不是准确的。
     * Returns the current sum.  The returned value is <em>NOT</em> an
     * atomic snapshot; invocation in the absence of concurrent
     * updates returns an accurate result, but concurrent updates that
     * occur while the sum is being calculated might not be
     * incorporated.
     *
     * @return the sum
     */
    public long sum() {
        Cell[] as = cells; Cell a;
        long sum = base;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    sum += a.value;
            }
        }
        return sum;
    }

    /**
     * 重置操作。把base和cells的所有值设为0
     * 实现逻辑：for循环赋值为0。同样没有加锁。
     * Resets variables maintaining the sum to zero.  This method may
     * be a useful alternative to creating a new adder, but is only
     * effective if there are no concurrent updates.  Because this
     * method is intrinsically racy, it should only be used when it is
     * known that no threads are concurrently updating.
     */
    public void reset() {
        Cell[] as = cells; Cell a;
        base = 0L;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    a.value = 0L;
            }
        }
    }

    /**
     * 累加的同时重置为0
     * Equivalent in effect to {@link #sum} followed by {@link
     * #reset}. This method may apply for example during quiescent
     * points between multithreaded computations.  If there are
     * updates concurrent with this method, the returned value is
     * <em>not</em> guaranteed to be the final value occurring before
     * the reset.
     *
     * @return the sum
     */
    public long sumThenReset() {
        Cell[] as = cells; Cell a;
        long sum = base;
        base = 0L;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null) {
                    sum += a.value;
                    a.value = 0L;
                }
            }
        }
        return sum;
    }

    /**
     * Returns the String representation of the {@link #sum}.
     * @return the String representation of the {@link #sum}
     */
    public String toString() {
        return Long.toString(sum());
    }

    // 下面四个方法，都是调用sum()返回总和，并做相应的类型转换。

    /**
     * Equivalent to {@link #sum}.
     *
     * @return the sum
     */
    public long longValue() {
        return sum();
    }

    /**
     * Returns the {@link #sum} as an {@code int} after a narrowing
     * primitive conversion.
     */
    public int intValue() {
        return (int)sum();
    }

    /**
     * Returns the {@link #sum} as a {@code float}
     * after a widening primitive conversion.
     */
    public float floatValue() {
        return (float)sum();
    }

    /**
     * Returns the {@link #sum} as a {@code double} after a widening
     * primitive conversion.
     */
    public double doubleValue() {
        return (double)sum();
    }

    /**
     * Serialization proxy, used to avoid reference to the non-public
     * Striped64 superclass in serialized forms.
     * @serial include
     */
    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 7249069246863182397L;

        /**
         * The current value returned by sum().
         * @serial
         */
        private final long value;

        SerializationProxy(LongAdder a) {
            value = a.sum();
        }

        /**
         * Return a {@code LongAdder} object with initial state
         * held by this proxy.
         *
         * @return a {@code LongAdder} object with initial state
         * held by this proxy.
         */
        private Object readResolve() {
            LongAdder a = new LongAdder();
            a.base = value;
            return a;
        }
    }

    /**
     * Returns a
     * <a href="../../../../serialized-form.html#java.util.concurrent.atomic.LongAdder.SerializationProxy">
     * SerializationProxy</a>
     * representing the state of this instance.
     *
     * @return a {@link SerializationProxy}
     * representing the state of this instance
     */
    private Object writeReplace() {
        return new SerializationProxy(this);
    }

    /**
     * @param s the stream
     * @throws java.io.InvalidObjectException always
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.InvalidObjectException {
        throw new java.io.InvalidObjectException("Proxy required");
    }
}
