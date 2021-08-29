package com.sunrise.study.collection.sourcecode;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * 迭代器接口（只提供单向遍历的接口）
 * 子类可以实现自己的迭代器
 * @author huangzihua
 * @date 2021-08-17
 */
public interface Iterator<E> {

    boolean hasNext();

    E next();

    default void remove() {
        throw new UnsupportedOperationException("remove");
    }


    default void forEachRemaining(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        while (hasNext())
            action.accept(next());
    }
}
