package com.sunrise.study.collection.sourcecode;

import java.util.*;
import java.util.function.Consumer;

/**
 * 优先队列：
 * 1、底层是使用数组来存储元素，实现的数据结构是有序的完全二叉树，也称最小堆
 * 2、插入元素、删除元素都会涉及到下沉和上浮操作
 * 3、最小值在堆顶
 * @author huangzihua
 * @date 2021-08-25
 */
public class PriorityQueue<E> extends AbstractQueue<E> implements java.io.Serializable {

    private static final long serialVersionUID = 6303316439030705576L;
    private static final int DEFAULT_INITIAL_CAPACITY = 11;

    /**
     * 优先队列以平衡二叉堆来表示，存储在数组格式中。
     * 因为平衡二叉堆，可以通过下标计算来获得父子节点，如：
     *      左子节点：2*n+1
     *      右子节点：2*n+2
     *      父节点：(n-1)/2
     * 优先队列是基于接口Comparator实现排序，是有序队列。最小值在堆顶
     */
    transient Object[] queue; // non-private to simplify nested class access

    /**
     * 队列中元素个数
     */
    private int size = 0;

    /**
     * 比较器。若使用自然排序，则为null
     */
    private final Comparator<? super E> comparator;

    /**
     * The number of times this priority queue has been
     * <i>structurally modified</i>.  See AbstractList for gory details.
     */
    transient int modCount = 0; // non-private to simplify nested class access


    /*********************************************构造器*******************************************************/

    /**
     * 无参构造器
     * 默认容量为11，comparator为null
     */
    public PriorityQueue() {
        this(DEFAULT_INITIAL_CAPACITY, null);
    }

    /**
     * 构造器。指定初始化容量，comparator为null
     */
    public PriorityQueue(int initialCapacity) {
        this(initialCapacity, null);
    }

    /**
     * 构造器。指定comparator，容量为默认值11
     */
    public PriorityQueue(Comparator<? super E> comparator) {
        this(DEFAULT_INITIAL_CAPACITY, comparator);
    }

    /**
     * 构造器。指定初始化容量和comparator
     */
    public PriorityQueue(int initialCapacity,
                         Comparator<? super E> comparator) {
        // Note: This restriction of at least one is not actually needed,
        // but continues for 1.5 compatibility
        if (initialCapacity < 1)
            throw new IllegalArgumentException();
        this.queue = new Object[initialCapacity];
        this.comparator = comparator;
    }

    /**
     * 构造器。参数是继承了Collection接口的集合对象
     */
    @SuppressWarnings("unchecked")
    public PriorityQueue(Collection<? extends E> c) {
        // 判断参数类型是否为有序集合
        if (c instanceof SortedSet<?>) {
            // 强制转型
            SortedSet<? extends E> ss = (SortedSet<? extends E>) c;
            // 获取参数的comparator，并赋值
            this.comparator = (Comparator<? super E>) ss.comparator();
            // initElementsFromCollection()方法是针对其他集合是有序集合但不是优先队列
            initElementsFromCollection(ss);
        }
        // 接着判断参数类型是否为优先队列
        else if (c instanceof PriorityQueue<?>) {
            PriorityQueue<? extends E> pq = (PriorityQueue<? extends E>) c;
            this.comparator = (Comparator<? super E>) pq.comparator();
            initFromPriorityQueue(pq);
        }
        else {
            this.comparator = null;
            // 初始化堆
            initFromCollection(c);
        }
    }

    /**
     * 构造器。参数是优先队列类型
     */
    @SuppressWarnings("unchecked")
    public PriorityQueue(PriorityQueue<? extends E> c) {
        this.comparator = (Comparator<? super E>) c.comparator();
        initFromPriorityQueue(c);
    }

    /**
     * 构造器。参数是有序集合类型
     */
    @SuppressWarnings("unchecked")
    public PriorityQueue(SortedSet<? extends E> c) {
        this.comparator = (Comparator<? super E>) c.comparator();
        initElementsFromCollection(c);
    }


    /**
     * 以优先队列为参数，进行初始化操作。
     */
    private void initFromPriorityQueue(PriorityQueue<? extends E> c) {
        // 判断下队列的类型是否一致
        if (c.getClass() == PriorityQueue.class) {
            // 这里是调用toArray()方法
            this.queue = c.toArray();
            this.size = c.size();
        } else {
            initFromCollection(c);
        }
    }

    /**
     * 初始化元素，把来自其他集合的元素添加到当前队列
     * 底层实现是直接拷贝一份其他集合的元素数组，然后判断该数组是否存在null值，不存在就直接赋值给queue了。
     * @param c 其他集合
     */
    private void initElementsFromCollection(Collection<? extends E> c) {
        // 获取参数的数组副本
        Object[] a = c.toArray();
        // If c.toArray incorrectly doesn't return Object[], copy it.
        // 如果 c.toArray 没有返回Object[]，就通过 Arrays.copyOf() 方法拷贝一份
        if (a.getClass() != Object[].class)
            a = Arrays.copyOf(a, a.length, Object[].class);
        int len = a.length;

        // 如果a数组长度为1，或者当前队列的比较器不为null，
        // 则遍历a数组看看是否存在null值，是则抛异常。优先队列不允许插入null值。
        if (len == 1 || this.comparator != null)
            for (int i = 0; i < len; i++)
                if (a[i] == null)
                    throw new NullPointerException();
        this.queue = a;
        this.size = a.length;
    }

    /**
     * 初始化队列，并进行堆化操作
     */
    private void initFromCollection(Collection<? extends E> c) {
        // 初始化队列
        initElementsFromCollection(c);
        // 进行堆化操作
        heapify();
    }


    /*********************************************集合操作*******************************************************/


    /**
     * 队列可分配的最大容量。
     * -8的原因是，一些虚拟机实现可能会有head信息，如果分配更大的空间可能会导致内存溢出
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * 扩容操作
     * 底层实现很简单，就是计算扩容的值，然后调用Arrays.copyOf()方法，创建一个容量更大数组
     */
    private void grow(int minCapacity) {
        int oldCapacity = queue.length;
        /*
            计算扩容大小：
                若原容量小于64，则 new = old*2 + 2;
                若原容量不小于64，则 new = old + old/2
         */
        int newCapacity = oldCapacity + ((oldCapacity < 64) ?
                                         (oldCapacity + 2) :
                                         (oldCapacity >> 1));
        // overflow-conscious code
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        queue = Arrays.copyOf(queue, newCapacity);
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?
            Integer.MAX_VALUE :
            MAX_ARRAY_SIZE;
    }

    /**
     * 插入元素。和offer()方法一样。
     * 底层调用offer()方法实现
     */
    public boolean add(E e) {
        return offer(e);
    }

    /**
     * 插入元素
     * 底层实现是：
     *  1、插入到最后一个叶子结点，然后进行上浮操作
     *  2、若队列为空则直接插入到0索引处
     *  3、若队列已满，则进行扩容
     */
    public boolean offer(E e) {
        // 不允许插入null值
        if (e == null)
            throw new NullPointerException();
        modCount++;
        int i = size;
        if (i >= queue.length)
            grow(i + 1);    // 如果数组已满，则扩容
        size = i + 1;
        if (i == 0)
            queue[0] = e;   // 如果当前队列为空，则直接赋值到索引0处
        else
            // 如果队列不为空，则调用siftUp方法插入元素。
            // 插入位置是size+1的位置，即插入到最后一个叶子结点，然后进行上浮操作。
            siftUp(i, e);
        return true;
    }

    /**
     * 获取堆最小值。即queue[0]
     */
    @SuppressWarnings("unchecked")
    public E peek() {
        return (size == 0) ? null : (E) queue[0];
    }


    /**
     * 获取指定元素的索引位置，若不存在则返回-1
     * 底层实现是通过for循环遍历
     * @param o
     * @return
     */
    private int indexOf(Object o) {
        if (o != null) {
            for (int i = 0; i < size; i++)
                if (o.equals(queue[i]))
                    return i;
        }
        return -1;
    }

    /**
     * 删除元素，若有多个相等值，则只删除第一个遇到的元素
     */
    public boolean remove(Object o) {
        int i = indexOf(o);     // 先执行indexOf()方法得到索引
        if (i == -1)
            return false;
        else {
            removeAt(i);    // 实际交由removeAt()方法删除
            return true;
        }
    }

    /**
     * 删除元素，与remove的区别在于：
     *      查找要删除的元素时，remove是使用equals()比较，而removeEq使用的是==
     */
    boolean removeEq(Object o) {
        for (int i = 0; i < size; i++) {
            if (o == queue[i]) {
                removeAt(i);
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否包含某个元素
     * 底层是调用indexOf()方法，返回值>0表示存在该元素
     */
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    /**
     * 返回一个包含当前队列所有元素的数组。
     */
    public Object[] toArray() {
        return Arrays.copyOf(queue, size);
    }

    /**
     * Returns an array containing all of the elements in this queue; the
     * runtime type of the returned array is that of the specified array.
     * The returned array elements are in no particular order.
     * If the queue fits in the specified array, it is returned therein.
     * Otherwise, a new array is allocated with the runtime type of the
     * specified array and the size of this queue.
     *
     * <p>If the queue fits in the specified array with room to spare
     * (i.e., the array has more elements than the queue), the element in
     * the array immediately following the end of the collection is set to
     * {@code null}.
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *
     * <p>Suppose {@code x} is a queue known to contain only strings.
     * The following code can be used to dump the queue into a newly
     * allocated array of {@code String}:
     *
     *  <pre> {@code String[] y = x.toArray(new String[0]);}</pre>
     *
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of the queue are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose.
     * @return an array containing all of the elements in this queue
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this queue
     * @throws NullPointerException if the specified array is null
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        final int size = this.size;
        if (a.length < size)
            // Make a new array of a's runtime type, but my contents:
            return (T[]) Arrays.copyOf(queue, size, a.getClass());
        System.arraycopy(queue, 0, a, 0, size);
        if (a.length > size)
            a[size] = null;
        return a;
    }

    /**
     * Returns an iterator over the elements in this queue. The iterator
     * does not return the elements in any particular order.
     *
     * @return an iterator over the elements in this queue
     */
    public Iterator<E> iterator() {
        return new Itr();
    }

    /**
     * 迭代器，实现了Iterator接口
     */
    private final class Itr implements Iterator<E> {
        /**
         * Index (into queue array) of element to be returned by
         * subsequent call to next.
         */
        private int cursor = 0;

        /**
         * Index of element returned by most recent call to next,
         * unless that element came from the forgetMeNot list.
         * Set to -1 if element is deleted by a call to remove.
         */
        private int lastRet = -1;

        /**
         * A queue of elements that were moved from the unvisited portion of
         * the heap into the visited portion as a result of "unlucky" element
         * removals during the iteration.  (Unlucky element removals are those
         * that require a siftup instead of a siftdown.)  We must visit all of
         * the elements in this list to complete the iteration.  We do this
         * after we've completed the "normal" iteration.
         *
         * We expect that most iterations, even those involving removals,
         * will not need to store elements in this field.
         */
        private ArrayDeque<E> forgetMeNot = null;

        /**
         * Element returned by the most recent call to next iff that
         * element was drawn from the forgetMeNot list.
         */
        private E lastRetElt = null;

        /**
         * The modCount value that the iterator believes that the backing
         * Queue should have.  If this expectation is violated, the iterator
         * has detected concurrent modification.
         */
        private int expectedModCount = modCount;

        public boolean hasNext() {
            return cursor < size ||
                (forgetMeNot != null && !forgetMeNot.isEmpty());
        }

        @SuppressWarnings("unchecked")
        public E next() {
            if (expectedModCount != modCount)
                throw new ConcurrentModificationException();
            if (cursor < size)
                return (E) queue[lastRet = cursor++];
            if (forgetMeNot != null) {
                lastRet = -1;
                lastRetElt = forgetMeNot.poll();
                if (lastRetElt != null)
                    return lastRetElt;
            }
            throw new NoSuchElementException();
        }

        public void remove() {
            if (expectedModCount != modCount)
                throw new ConcurrentModificationException();
            if (lastRet != -1) {
                E moved = PriorityQueue.this.removeAt(lastRet);
                lastRet = -1;
                if (moved == null)
                    cursor--;
                else {
                    if (forgetMeNot == null)
                        forgetMeNot = new ArrayDeque<>();
                    forgetMeNot.add(moved);
                }
            } else if (lastRetElt != null) {
                PriorityQueue.this.removeEq(lastRetElt);
                lastRetElt = null;
            } else {
                throw new IllegalStateException();
            }
            expectedModCount = modCount;
        }
    }

    public int size() {
        return size;
    }

    /**
     * 删除所有元素
     */
    public void clear() {
        modCount++;
        for (int i = 0; i < size; i++)
            queue[i] = null;
        size = 0;
    }

    /**
     * 弹出堆顶元素，即获取并删除堆顶元素。
     * 底层实现是：先把堆顶元素缓存起来，然后把最后一个节点元素，覆盖到根节点，接着执行下沉操作。
     */
    @SuppressWarnings("unchecked")
    public E poll() {
        if (size == 0)
            return null;
        int s = --size;
        modCount++;
        E result = (E) queue[0];
        E x = (E) queue[s];
        queue[s] = null;
        if (s != 0)
            siftDown(0, x);
        return result;
    }

    /**
     * 删除某个位置的元素
     * 底层实现，若删除的是最后一个元素，则直接size-1，并把queue[size-1]=null即可
     * 若删除的不是最后一个元素，则把最后一个元素，插入到删除位置，覆盖删除值，然后进行下沉、上浮调整
     */
    @SuppressWarnings("unchecked")
    private E removeAt(int i) {
        // assert i >= 0 && i < size;
        modCount++;
        // s表示最后一个节点
        int s = --size;
        if (s == i) // removed last element
            queue[i] = null;    // 如果删除的是最后一个元素，则直接设为null，让GC回收
        else {
            // 这里缓存最后一个节点，并把它插入到删除位置以覆盖原来的元素
            E moved = (E) queue[s];
            queue[s] = null;            // 把最后一个节点设为null，让GC回收
            siftDown(i, moved);         // 插入到i位置，并执行下沉操作
            if (queue[i] == moved) {
                /*
                 若相等，说明插入元素，比子节点都小，并未进行下沉操作
                 这时有可能插入元素，比父节点还小，所以下面需要执行一次上浮操作
                 */
                siftUp(i, moved);
                if (queue[i] != moved)  // 若元素进行过上浮或下沉，则返回该元素
                    return moved;
            }
        }
        return null;
    }

    /**
     * 在k位置插入元素，并进行上浮调整
     * @param k the position to fill
     * @param x the item to insert
     */
    private void siftUp(int k, E x) {
        if (comparator != null)
            siftUpUsingComparator(k, x);
        else
            siftUpComparable(k, x);
    }

    /**
     * 基于默认比较逻辑的上浮操作
     */
    @SuppressWarnings("unchecked")
    private void siftUpComparable(int k, E x) {
        Comparable<? super E> key = (Comparable<? super E>) x;
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            Object e = queue[parent];
            if (key.compareTo((E) e) >= 0)
                break;
            queue[k] = e;
            k = parent;
        }
        queue[k] = key;
    }

    /**
     * 基于comparator的上浮操作
     */
    @SuppressWarnings("unchecked")
    private void siftUpUsingComparator(int k, E x) {
        // 不断把当前节点和父节点比较，若父节点更大，则父节点下沉，直到大于等于父节点或是根节点，才停止循环
        while (k > 0) {
            int parent = (k - 1) >>> 1; // (n-1)/2，取得父节点索引
            Object e = queue[parent];   // 父节点
            if (comparator.compare(x, (E) e) >= 0)
                break;  // 比父节点大，则跳出循环，此时的k就是元素插入的位置
            queue[k] = e;
            k = parent;
        }
        // 最后把元素插入到k的位置
        queue[k] = x;
    }

    /**
     * 元素下沉方法。
     * @param k the position to fill   插入位置
     * @param x the item to insert     插入元素
     */
    private void siftDown(int k, E x) {
        // 下面的函数代码逻辑都是一样的，唯一区别是比较元素大小时，调用的是哪个比较器
        // 若当前队列的comparator不为null，则使用comparator，否则使用默认的比较逻辑
        if (comparator != null)
            siftDownUsingComparator(k, x);
        else
            siftDownComparable(k, x);
    }


    /**
     * 基于默认比较逻辑的下沉操作
     * 底层实现是：
     *      1、把待下沉的节点key作为父节点parent，执行下面的操作
     *      2、先比较 leftChild 和 rightChild 的大小，选出较小的一个子节点 child；
     *      3、接着比较 父节点parent 和 选中的子节点child 的大小：
     *      4、若 parent <= child，则不用下沉，整个方法结束。
     *      5、若 parent > child，则 parent 下沉到 child 的位置作为子节点，
     *         而 child 上浮到 parent 的位置作为父节点，回到步骤2重复操作
     * @param k 待下沉节点的索引
     * @param x 带下沉节点
     */
    @SuppressWarnings("unchecked")
    private void siftDownComparable(int k, E x) {
        Comparable<? super E> key = (Comparable<? super E>)x;
        int half = size >>> 1;        // loop while a non-leaf
        while (k < half) {
            // 左子节点索引
            int child = (k << 1) + 1; // assume left child is least
            Object c = queue[child];
            // 右子节点索引
            int right = child + 1;
            // 判断下right是否越界，没有则比较左右子节点大小
            if (right < size &&
                ((Comparable<? super E>) c).compareTo((E) queue[right]) > 0)
                // 若右子节点较小，则 c 和 child 切换为右子节点来进行后面的交换操作
                c = queue[child = right];

            // 若key比孩子节点小，则跳出循环，不用交换
            if (key.compareTo((E) c) <= 0)
                break;

            // 把较小的子节点 c，替换到父节点的位置。
            queue[k] = c;
            // 然后切换到较小的子节点，以它为父节点继续往下比较
            k = child;
        }
        // 循环到这里结束，最后的k就是节点x的替换位置。
        // 这时，这个key要么是比两个孩子节点都小，要么是叶子节点，即不可再下沉了。
        queue[k] = key;
    }

    /**
     * 基于comparator比较逻辑的下沉操作
     * 代码逻辑与siftDownComparable相似，区别在于元素大小的比较逻辑
     */
    @SuppressWarnings("unchecked")
    private void siftDownUsingComparator(int k, E x) {
        int half = size >>> 1;
        while (k < half) {
            int child = (k << 1) + 1;
            Object c = queue[child];
            int right = child + 1;
            if (right < size &&
                comparator.compare((E) c, (E) queue[right]) > 0)
                c = queue[child = right];
            if (comparator.compare(x, (E) c) <= 0)
                break;
            queue[k] = c;
            k = child;
        }
        queue[k] = x;
    }

    /**
     * 堆化操作
     * 底层实现是：从最后一个非叶子节点开始，往前遍历，逐个下沉。
     */
    @SuppressWarnings("unchecked")
    private void heapify() {
        /*
            关于size >>> 1
                1、>>>是无符号右移，无论正负，高位都是补0
                2、size必定是大于等于0，为正数
                3、size >>> 1，表示size/2。
            综上，i的初始值为 size/2 -1。
            再根据堆的规则，size/2-1可以得到最后一个非叶子节点
            所以这里遍历的是，从最后一个非叶子节点开始，往前遍历
         */
        for (int i = (size >>> 1) - 1; i >= 0; i--)
            siftDown(i, (E) queue[i]);
    }

    /**
     * Returns the comparator used to order the elements in this
     * queue, or {@code null} if this queue is sorted according to
     * the {@linkplain Comparable natural ordering} of its elements.
     *
     * @return the comparator used to order this queue, or
     *         {@code null} if this queue is sorted according to the
     *         natural ordering of its elements
     */
    public Comparator<? super E> comparator() {
        return comparator;
    }

    /**
     * Saves this queue to a stream (that is, serializes it).
     *
     * @serialData The length of the array backing the instance is
     *             emitted (int), followed by all of its elements
     *             (each an {@code Object}) in the proper order.
     * @param s the stream
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        // Write out element count, and any hidden stuff
        s.defaultWriteObject();

        // Write out array length, for compatibility with 1.5 version
        s.writeInt(Math.max(2, size + 1));

        // Write out all elements in the "proper order".
        for (int i = 0; i < size; i++)
            s.writeObject(queue[i]);
    }

    /**
     * Reconstitutes the {@code PriorityQueue} instance from a stream
     * (that is, deserializes it).
     *
     * @param s the stream
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        // Read in size, and any hidden stuff
        s.defaultReadObject();

        // Read in (and discard) array length
        s.readInt();

        SharedSecrets.getJavaOISAccess().checkArray(s, Object[].class, size);
        queue = new Object[size];

        // Read in all elements.
        for (int i = 0; i < size; i++)
            queue[i] = s.readObject();

        // Elements are guaranteed to be in "proper order", but the
        // spec has never explained what that might be.
        heapify();
    }

    /**
     * Creates a <em><a href="Spliterator.html#binding">late-binding</a></em>
     * and <em>fail-fast</em> {@link Spliterator} over the elements in this
     * queue.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#SIZED},
     * {@link Spliterator#SUBSIZED}, and {@link Spliterator#NONNULL}.
     * Overriding implementations should document the reporting of additional
     * characteristic values.
     *
     * @return a {@code Spliterator} over the elements in this queue
     * @since 1.8
     */
    public final Spliterator<E> spliterator() {
        return new PriorityQueueSpliterator<E>(this, 0, -1, 0);
    }

    static final class PriorityQueueSpliterator<E> implements Spliterator<E> {
        /*
         * This is very similar to ArrayList Spliterator, except for
         * extra null checks.
         */
        private final PriorityQueue<E> pq;
        private int index;            // current index, modified on advance/split
        private int fence;            // -1 until first use
        private int expectedModCount; // initialized when fence set

        /** Creates new spliterator covering the given range */
        PriorityQueueSpliterator(PriorityQueue<E> pq, int origin, int fence,
                             int expectedModCount) {
            this.pq = pq;
            this.index = origin;
            this.fence = fence;
            this.expectedModCount = expectedModCount;
        }

        private int getFence() { // initialize fence to size on first use
            int hi;
            if ((hi = fence) < 0) {
                expectedModCount = pq.modCount;
                hi = fence = pq.size;
            }
            return hi;
        }

        public PriorityQueueSpliterator<E> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid) ? null :
                new PriorityQueueSpliterator<E>(pq, lo, index = mid,
                                                expectedModCount);
        }

        @SuppressWarnings("unchecked")
        public void forEachRemaining(Consumer<? super E> action) {
            int i, hi, mc; // hoist accesses and checks from loop
            PriorityQueue<E> q; Object[] a;
            if (action == null)
                throw new NullPointerException();
            if ((q = pq) != null && (a = q.queue) != null) {
                if ((hi = fence) < 0) {
                    mc = q.modCount;
                    hi = q.size;
                }
                else
                    mc = expectedModCount;
                if ((i = index) >= 0 && (index = hi) <= a.length) {
                    for (E e;; ++i) {
                        if (i < hi) {
                            if ((e = (E) a[i]) == null) // must be CME
                                break;
                            action.accept(e);
                        }
                        else if (q.modCount != mc)
                            break;
                        else
                            return;
                    }
                }
            }
            throw new ConcurrentModificationException();
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            if (action == null)
                throw new NullPointerException();
            int hi = getFence(), lo = index;
            if (lo >= 0 && lo < hi) {
                index = lo + 1;
                @SuppressWarnings("unchecked") E e = (E)pq.queue[lo];
                if (e == null)
                    throw new ConcurrentModificationException();
                action.accept(e);
                if (pq.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                return true;
            }
            return false;
        }

        public long estimateSize() {
            return (long) (getFence() - index);
        }

        public int characteristics() {
            return Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.NONNULL;
        }
    }
}
