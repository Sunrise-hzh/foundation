package com.sunrise.study.generic.inter;

/**
 * 自定义泛型接口
 * @author huangzihua
 * @date 2021-05-08
 */
public interface MyList<E> {
    //打印泛型实际类型参数的类型名称
    void showTypeName(E e);
}
