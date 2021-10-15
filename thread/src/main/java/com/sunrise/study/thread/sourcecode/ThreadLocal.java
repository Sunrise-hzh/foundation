package com.sunrise.study.thread.sourcecode;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * @author huangzihua
 * @date 2021-08-03
 */
public class ThreadLocal<T> {
    /**
     * 常量，哈希值。构造ThreadLocal时初始化，final修饰。
     */
    private final int threadLocalHashCode = nextHashCode();     // 初始化时调用nextHashCode()方法来取值

    /**
     * 原子类，保证hash code的计算是原子性。
     */
    private static AtomicInteger nextHashCode =
            new AtomicInteger();

    /**
     * hash值增量
     * 用0x61c88647作为魔数累加为每个ThreadLocal分配各自的ID
     * 也就是threadLocalHashCode再与2的幂取模，得到的结果分布比较均匀
     */
    private static final int HASH_INCREMENT = 0x61c88647;

    /**
     * 返回下一个hash code
     */
    private static int nextHashCode() {
        // 先 return nextHashCode
        // 再 nextHashCode += HASH_INCREMENT
        return nextHashCode.getAndAdd(HASH_INCREMENT);
    }

    /**
     * 初始化ThreadLocal的值，其子类可以重写该方法
     * 例如：ReentrantReadWriteLock下的内部类ThreadLocalHoldCounter
     */
    protected T initialValue() {
        return null;
    }

    /**
     * 类方法，返回SuppliedThreadLocal类实例
     * Creates a thread local variable. The initial value of the variable is
     * determined by invoking the {@code get} method on the {@code Supplier}.
     *
     * @param <S> the type of the thread local's value
     * @param supplier the supplier to be used to determine the initial value
     * @return a new thread local variable
     * @throws NullPointerException if the specified supplier is null
     * @since 1.8
     */
    public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
        return new SuppliedThreadLocal<>(supplier);
    }

    /**
     * 无参构造器
     */
    public ThreadLocal() {
    }

    /**
     * get元素
     *
     * @return the current thread's value of this thread-local
     */
    public T get() {
        // 取当前线程实例
        Thread t = Thread.currentThread();

        // 每个线程有一个变量threadLocals，存放的是当前线程的ThreadLocalMap对象
        // 这里获取当前Thread中的threadLocals变量
        ThreadLocalMap map = getMap(t);         // 执行代码：return t.threadLocals;
        if (map != null) {
            // 如果map不为null，则调取getEntry方法获取Table元素
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;  // 强制类型转换
                return result;
            }
        }

        // 如果map为null，或者在map中没找到要取的元素，就执行下面的方法
        return setInitialValue();
    }

    /**
     * set初始化value
     *
     * @return the initial value
     */
    private T setInitialValue() {
        T value = initialValue();       // 调用initialValue()，获取初始化值
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        // 把初始化值放入map中
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);    // map为null，则创建map
        return value;
    }

    /**
     * 插入值/设置值
     *
     * @param value the value to be stored in the current thread's copy of
     *        this thread-local.
     */
    public void set(T value) {
        // 获取当前线程
        Thread t = Thread.currentThread();

        // 获取当前线程的threadLocals变量
        ThreadLocalMap map = getMap(t);
        if (map != null)
            // this只是当前ThreadLocal实例，value就是要保存的值
            map.set(this, value);
        else
            // 若当前线程的threadLocals变量为null，则执行createMap()方法进行初始化
            createMap(t, value);    // t.threadLocals = new ThreadLocalMap(this, firstValue);
    }

    /**
     * 移除元素
     *
     * @since 1.5
     */
    public void remove() {
        ThreadLocalMap m = getMap(Thread.currentThread());
        if (m != null)
            m.remove(this);
    }

    /**
     * 取当前线程的threadLocals值
     *
     * Get the map associated with a ThreadLocal.
     * Overridden in InheritableThreadLocal.
     *
     * @param  t the current thread
     * @return the map
     */
    ThreadLocalMap getMap(Thread t) {
//        return t.threadLocals;

        // 这里真实执行的是上面那条代码，为保证当前项目可以运行、没红线才注释掉
        return null;
    }

    /**
     * 创建一个ThreadLocalMap
     * Create the map associated with a ThreadLocal. Overridden in
     * InheritableThreadLocal.
     *
     * @param t the current thread
     * @param firstValue value for the initial entry of the map
     */
    void createMap(Thread t, T firstValue) {
//        t.threadLocals = new ThreadLocalMap(this, firstValue);
    }

    /**
     * 工厂方法，创建可继承的 ThreadLocalMap
     *
     * Factory method to create map of inherited thread locals.
     * Designed to be called only from Thread constructor.
     *
     * @param  parentMap the map associated with parent thread
     * @return a map containing the parent's inheritable bindings
     */
    static ThreadLocalMap createInheritedMap(ThreadLocalMap parentMap) {
        return new ThreadLocalMap(parentMap);
    }

    /**
     * Method childValue is visibly defined in subclass InheritableThreadLocal,
     * but is internally defined here for the sake of providing createInheritedMap factory method without needing to subclass the map class in InheritableThreadLocal.
     * This technique is preferable to the alternative of embedding instanceof tests in methods.
     */
    T childValue(T parentValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * 静态内部类。继承ThreadLocal类。
     * An extension of ThreadLocal that obtains its initial value from the specified {@code Supplier}.
     */
    static final class SuppliedThreadLocal<T> extends ThreadLocal<T> {

        /*
            Supplier是个实现类，单纯只有一个get()方法
         */
        private final Supplier<? extends T> supplier;

        SuppliedThreadLocal(Supplier<? extends T> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        @Override
        protected T initialValue() {
            return supplier.get();
        }
    }

    /**
     * 内部实现了Entry静态内部类，用于存放值，通过hash code来计算索引。
     */
    static class ThreadLocalMap {

        /**
         * Entry继承了弱引用 WeakReference类，并增加一个变量value，用于保存映射值
         * 被弱引用关联的对象只能生存到下一次垃圾收集发生之前。
         * 当垃圾收集器工作时，无论当前内存是否足够，都会回收掉只被弱引用关联的对象
         */
        static class Entry extends WeakReference<ThreadLocal<?>> {
            Object value;

            Entry(ThreadLocal<?> k, Object v) {
                super(k);
                value = v;
            }
        }

        /**
         * 初始化容量，默认是16
         */
        private static final int INITIAL_CAPACITY = 16;

        /**
         * Entry数组，存放ThreadLocal元素
         * 底层实现了自动扩容。其长度必须是2的幂次
         */
        private Entry[] table;

        /**
         * table中的元素的个数。即Table数组的长度
         */
        private int size = 0;

        /**
         * 容量扩充的阔值
         */
        private int threshold; // Default to 0

        /**
         * 修改容量扩充的阔值
         */
        private void setThreshold(int len) {
            threshold = len * 2 / 3;    // 容量阔值为长度的2/3
        }

        /**
         * 类方法，获取下一索引。如果到数组末尾则回到0
         */
        private static int nextIndex(int i, int len) {
            return ((i + 1 < len) ? i + 1 : 0);
        }

        /**
         * 类方法，获取上一索引。如果到数组头，则下一索引重新回到尾部
         */
        private static int prevIndex(int i, int len) {
            return ((i - 1 >= 0) ? i - 1 : len - 1);
        }

        /**
         * 构造函数
         * Construct a new map initially containing (firstKey, firstValue).
         * ThreadLocalMaps are constructed lazily, so we only create
         * one when we have at least one entry to put in it.
         */
        ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
            table = new Entry[INITIAL_CAPACITY];    // 初始化table
            int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);  // 计算索引
            table[i] = new Entry(firstKey, firstValue);     // 初始化第一个元素值
            size = 1;       // table大小为1，因为前面设置了一个元素
            setThreshold(INITIAL_CAPACITY);     // 设置table的默认阔值
        }

        /**
         * 私有构造函数
         *
         * Construct a new map including all Inheritable ThreadLocals from given parent map.
         * Called only by createInheritedMap.
         *
         * @param parentMap the map associated with parent thread.
         */
        private ThreadLocalMap(ThreadLocalMap parentMap) {
            Entry[] parentTable = parentMap.table;
            int len = parentTable.length;
            setThreshold(len);
            table = new Entry[len];

            for (int j = 0; j < len; j++) {
                Entry e = parentTable[j];
                if (e != null) {
                    @SuppressWarnings("unchecked")
                    ThreadLocal<Object> key = (ThreadLocal<Object>) e.get();
                    if (key != null) {
                        Object value = key.childValue(e.value);
                        Entry c = new Entry(key, value);
                        int h = key.threadLocalHashCode & (len - 1);
                        while (table[h] != null)
                            h = nextIndex(h, len);
                        table[h] = c;
                        size++;
                    }
                }
            }
        }

        /**
         * 取元素值
         *
         * Get the entry associated with key.  This method
         * itself handles only the fast path: a direct hit of existing
         * key. It otherwise relays to getEntryAfterMiss.  This is
         * designed to maximize performance for direct hits, in part
         * by making this method readily inlinable.
         *
         * @param  key the thread local object
         * @return the entry associated with key, or null if no such
         */
        private Entry getEntry(ThreadLocal<?> key) {
            int i = key.threadLocalHashCode & (table.length - 1);   // 通过hashcode计算索引
            Entry e = table[i];     // 取值
            // 这里e.get()调用的是Entry的底层抽象类Reference的方法，返回该实例绑定的引用对象。即返回ThreadLocal类对象
            if (e != null && e.get() == key)
                // e不为空且e的引用对象和key是同一对象，则直接返回
                return e;
            else
                // 如果e为空，或者e的引用对象和key不是同一对象
                return getEntryAfterMiss(key, i, e);
        }

        /**
         * 如果在getEntry方法中不能直接通过hash code取到对应值时，调用此方法
         *
         * @param  key the thread local object
         * @param  i the table index for key's hash code
         * @param  e the entry at table[i]
         * @return the entry associated with key, or null if no such
         */
        private Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e) {
            Entry[] tab = table;
            int len = tab.length;

            // 判断e是否为null
            while (e != null) {
                ThreadLocal<?> k = e.get();     // 先取e对应ThreadLocal类

                if (k == key)
                    return e;  // 如果e.get() == key，说明取到对应的值，返回e

                if (k == null)
                    expungeStaleEntry(i);   // 如果k为null，则删除该元素
                else
                    i = nextIndex(i, len);  // 如果k不为null，则通过nextIndex方法取下一索引

                e = tab[i]; // i修改后，取最新的元素e
            }
            return null;
        }

        /**
         * 往hash table插入值
         * 底层实现是：通过hash算法计算存储索引，若hash冲突，则通过开放寻址法寻找一个存储索引
         * 如果插入新元素后，容量达到阔值，则先清理一遍旧元素，如果清理后容量仍然超过阔值，再进行扩容处理
         * Set the value associated with key.
         *
         * @param key the thread local object
         * @param value the value to be set
         */
        private void set(ThreadLocal<?> key, Object value) {

            // We don't use a fast path as with get() because it is at
            // least as common to use set() to create new entries as
            // it is to replace existing ones, in which case, a fast
            // path would fail more often than not.

            Entry[] tab = table;
            int len = tab.length;
            // 通过hash code计算索引（即在Entry[] table中的位置）
            int i = key.threadLocalHashCode & (len-1);

            /*
                遍历table数组
                大致逻辑是：
                    1、先看看table[i]是否为null，是则结束循环
                    2、table[i]不是null，则判断table[i].key和传入的key是不是同一个，是则覆盖旧值
                    3、不是则判断table[i].key是否为null，是null则调用replaceStaleEntry方法设置值
                    4、table[i].key不是null且也不是传入的key，说明这个索引有值了，则i++回到步骤1
             */
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                // 取元素e的关联引用，也就是key对象
                ThreadLocal<?> k = e.get();

                // 如果key相同，说明原来就有值，则覆盖原来的value值，然后结束方法
                if (k == key) {
                    e.value = value;
                    return;
                }

                // 如果key为空，但元素e存在，可能是被回收了，则调replaceStaleEntry方法设置值，方法结束
                if (k == null) {
                    // 替换旧元素
                    replaceStaleEntry(key, value, i);
                    return;
                }
            }

            // 设置新值
            tab[i] = new Entry(key, value);
            // 数组元素个数+1
            int sz = ++size;

            // 执行cleanSomeSlots方法清理元素，如果有清理则返回true，当前方法结束
            // 如果没有元素被删除，则判断sz是否大于等于threshold阔值，是则调rehash方法扩容
            if (!cleanSomeSlots(i, sz) && sz >= threshold)
                rehash();
        }

        /**
         * 移除元素
         */
        private void remove(ThreadLocal<?> key) {
            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len-1);
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                if (e.get() == key) {
                    e.clear();  // clear方法是Reference抽象类的方法
                    expungeStaleEntry(i);
                    return;
                }
            }
        }

        /**
         * 替换旧值
         *
         * Replace a stale entry encountered during a set operationwith an entry for the specified key.
         * The value passed in the value parameter is stored in the entry, whether or not an entry already exists for the specified key.
         *
         * As a side effect, this method expunges all stale entries in the "run" containing the stale entry.
         * (A run is a sequence of entries between two null slots.)
         *
         * @param  key the key
         * @param  value the value to be associated with key
         * @param  staleSlot index of the first stale entry encountered while
         *         searching for key.
         */
        private void replaceStaleEntry(ThreadLocal<?> key, Object value,
                                       int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;
            Entry e;

            // 向前遍历，如果前面的元素也是key==null，则slotToExpunge改为该元素的索引。
            // 找到旧元素的开始索引
            int slotToExpunge = staleSlot;
            for (int i = prevIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = prevIndex(i, len))
                if (e.get() == null)
                    slotToExpunge = i;

            // Find either the key or trailing null slot of run, whichever
            // occurs first
            for (int i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();

                // If we find key, then we need to swap it
                // with the stale entry to maintain hash table order.
                // The newly stale slot, or any other stale slot
                // encountered above it, can then be sent to expungeStaleEntry
                // to remove or rehash all of the other entries in run.
                if (k == key) {
                    e.value = value;

                    tab[i] = tab[staleSlot];
                    tab[staleSlot] = e;

                    // Start expunge at preceding stale entry if it exists
                    if (slotToExpunge == staleSlot)
                        slotToExpunge = i;
                    cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
                    return;
                }

                // If we didn't find stale entry on backward scan, the
                // first stale entry seen while scanning for key is the
                // first still present in the run.
                if (k == null && slotToExpunge == staleSlot)
                    slotToExpunge = i;
            }

            // If key not found, put new entry in stale slot
            tab[staleSlot].value = null;
            tab[staleSlot] = new Entry(key, value);

            // If there are any other stale entries in run, expunge them
            if (slotToExpunge != staleSlot)
                cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
        }

        /**
         * 删除旧元素
         * 旧元素（stale entry）是指：value != null && key == null，就是有值但是没有绑定任何ThreadLocal实例
         */
        private int expungeStaleEntry(int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;

            // 先删除当前元素
            tab[staleSlot].value = null;
            tab[staleSlot] = null;
            size--;

            // 传染处理，往下遍历，把所有断开引用的元素都删除掉，并重新调整元素位置
            Entry e;
            int i;
            for (i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();
                if (k == null) {
                    e.value = null;
                    tab[i] = null;
                    size--;
                } else {
                    /*
                        取i对应的元素，判断其散列后的索引h是否等于i，
                        如果 h!=i 为true，说明其散列后的索引发生偏移，所以
                        这里需要对该元素的索引重新计算，并移动
                     */
                    int h = k.threadLocalHashCode & (len - 1);
                    if (h != i) {
                        tab[i] = null;

                        // Unlike Knuth 6.4 Algorithm R, we must scan until
                        // null because multiple entries could have been stale.
                        while (tab[h] != null)
                            h = nextIndex(h, len);
                        tab[h] = e;
                    }
                }
            }
            return i;
        }

        /**
         * 清理e.key==null的元素
         */
        private boolean cleanSomeSlots(int i, int n) {
            boolean removed = false;
            Entry[] tab = table;
            int len = tab.length;
            do {
                i = nextIndex(i, len);
                Entry e = tab[i];
                if (e != null && e.get() == null) {
                    n = len;
                    removed = true;
                    i = expungeStaleEntry(i);
                }
            } while ( (n >>>= 1) != 0);
            return removed;
        }

        /**
         * 对table重新封装和调整大小。
         * 先是遍历table，移除旧的元素，如果移除元素后还是溢出，则扩充2倍容量
         * Re-pack and/or re-size the table.
         * First scan the entire table removing stale entries.
         * If this doesn't sufficiently shrink the size of the table, double the table size.
         */
        private void rehash() {
            // 删掉所有废弃的元素，即ThreadLocal引用为null
            expungeStaleEntries();

            // 判断size 达到扩容阔值threshold的四分之三，是则扩容
            if (size >= threshold - threshold / 4)
                resize();
        }

        /**
         * 扩容2倍
         * Double the capacity of the table.
         */
        private void resize() {
            Entry[] oldTab = table;
            int oldLen = oldTab.length;
            int newLen = oldLen * 2;      // 新容量是原来的2倍
            Entry[] newTab = new Entry[newLen];
            int count = 0;

            // 重新计算hash值，迁移旧table的元素到新table中
            for (int j = 0; j < oldLen; ++j) {
                Entry e = oldTab[j];
                if (e != null) {
                    ThreadLocal<?> k = e.get();
                    if (k == null) {
                        e.value = null; // Help the GC
                    } else {
                        int h = k.threadLocalHashCode & (newLen - 1);
                        while (newTab[h] != null)
                            h = nextIndex(h, newLen);
                        newTab[h] = e;
                        count++;
                    }
                }
            }

            setThreshold(newLen);
            size = count;
            table = newTab;
        }

        /**
         * 删掉所有旧的元素
         * Expunge all stale entries in the table.
         */
        private void expungeStaleEntries() {
            Entry[] tab = table;
            int len = tab.length;
            for (int j = 0; j < len; j++) {
                Entry e = tab[j];
                if (e != null && e.get() == null)
                    expungeStaleEntry(j);
            }
        }
    }
}
