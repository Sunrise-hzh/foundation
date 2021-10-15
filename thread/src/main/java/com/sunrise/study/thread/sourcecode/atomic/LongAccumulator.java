package com.sunrise.study.thread.sourcecode.atomic;

import java.io.Serializable;
import java.util.function.LongBinaryOperator;

/**
 * LongAdder类是LongAccumulator的一个特例，而LongAccumulator比LongAdder的功能更强大。
 *
 * LongAccumulator相比于LongAdder，可以为累加器提供非0的初始值，后者只能
 * 提供默认的0值。另外，前者还可以指定累加规则，比如不进行累加而进行相乘，只
 * 需要在构造LongAccumulator时传入自定义的双目运算器即可，后者则内置累加的
 * 规则。
 *
 * 总结：
 *   1、LongAccumulator和LongAdder的底层原理是一样的，都继承了Striped64抽象类；
 *   2、LongAccumulator比LongAdder强大的原因是，其内部定义了一个LongBinaryOperator
 *      属性，它由用户自定义实现计算规则，例如不是累加，而是累乘、累除等等；
 *   3、LongBinaryOperator其实就是一个函数式接口，在构建LongAccumulator实例时，
 *      必须提供该函数式接口的实现；
 *
 * @author huangzihua
 * @date 2021-09-15
 */
public class LongAccumulator  extends Striped64 implements Serializable {
    private static final long serialVersionUID = 7249069246863182397L;

    // 二元运算符的函数式接口
    private final LongBinaryOperator function;
    private final long identity;

    /**
     * 构造器，必须给定初始值和二元运算符接口实现
     * Creates a new instance using the given accumulator function
     * and identity element.
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @param identity identity (initial value) for the accumulator function
     */
    public LongAccumulator(LongBinaryOperator accumulatorFunction,
                           long identity) {
        this.function = accumulatorFunction;
        base = this.identity = identity;
    }

    /**
     * Updates with the given value.
     *
     * @param x the value
     */
    public void accumulate(long x) {
        Cell[] as; long b, v, r; int m; Cell a;
        if ((as = cells) != null ||
            (r = function.applyAsLong(b = base, x)) != b && !casBase(b, r)) {
            boolean uncontended = true;
            if (as == null || (m = as.length - 1) < 0 ||
                (a = as[getProbe() & m]) == null ||
                !(uncontended =
                  (r = function.applyAsLong(v = a.value, x)) == v ||
                  a.cas(v, r)))

                // LongAddr在调用此方法时，第二个参数传递的是null，而LongAccumulate
                // 则传递function函数式接口实现。
                longAccumulate(x, function, uncontended);
        }
    }

    /**
     * Returns the current value.  The returned value is <em>NOT</em>
     * an atomic snapshot; invocation in the absence of concurrent
     * updates returns an accurate result, but concurrent updates that
     * occur while the value is being calculated might not be
     * incorporated.
     *
     * @return the current value
     */
    public long get() {
        Cell[] as = cells; Cell a;
        long result = base;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    result = function.applyAsLong(result, a.value);
            }
        }
        return result;
    }

    /**
     * Resets variables maintaining updates to the identity value.
     * This method may be a useful alternative to creating a new
     * updater, but is only effective if there are no concurrent
     * updates.  Because this method is intrinsically racy, it should
     * only be used when it is known that no threads are concurrently
     * updating.
     */
    public void reset() {
        Cell[] as = cells; Cell a;
        base = identity;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    a.value = identity;
            }
        }
    }

    /**
     * Equivalent in effect to {@link #get} followed by {@link
     * #reset}. This method may apply for example during quiescent
     * points between multithreaded computations.  If there are
     * updates concurrent with this method, the returned value is
     * <em>not</em> guaranteed to be the final value occurring before
     * the reset.
     *
     * @return the value before reset
     */
    public long getThenReset() {
        Cell[] as = cells; Cell a;
        long result = base;
        base = identity;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null) {
                    long v = a.value;
                    a.value = identity;
                    result = function.applyAsLong(result, v);
                }
            }
        }
        return result;
    }

    /**
     * Returns the String representation of the current value.
     * @return the String representation of the current value
     */
    public String toString() {
        return Long.toString(get());
    }

    /**
     * Equivalent to {@link #get}.
     *
     * @return the current value
     */
    public long longValue() {
        return get();
    }

    /**
     * Returns the {@linkplain #get current value} as an {@code int}
     * after a narrowing primitive conversion.
     */
    public int intValue() {
        return (int)get();
    }

    /**
     * Returns the {@linkplain #get current value} as a {@code float}
     * after a widening primitive conversion.
     */
    public float floatValue() {
        return (float)get();
    }

    /**
     * Returns the {@linkplain #get current value} as a {@code double}
     * after a widening primitive conversion.
     */
    public double doubleValue() {
        return (double)get();
    }

    /**
     * Serialization proxy, used to avoid reference to the non-public
     * Striped64 superclass in serialized forms.
     * @serial include
     */
    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 7249069246863182397L;

        /**
         * The current value returned by get().
         * @serial
         */
        private final long value;
        /**
         * The function used for updates.
         * @serial
         */
        private final LongBinaryOperator function;
        /**
         * The identity value
         * @serial
         */
        private final long identity;

        SerializationProxy(LongAccumulator a) {
            function = a.function;
            identity = a.identity;
            value = a.get();
        }

        /**
         * Returns a {@code LongAccumulator} object with initial state
         * held by this proxy.
         *
         * @return a {@code LongAccumulator} object with initial state
         * held by this proxy.
         */
        private Object readResolve() {
            LongAccumulator a = new LongAccumulator(function, identity);
            a.base = value;
            return a;
        }
    }

    /**
     * Returns a
     * <a href="../../../../serialized-form.html#java.util.concurrent.atomic.LongAccumulator.SerializationProxy">
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
