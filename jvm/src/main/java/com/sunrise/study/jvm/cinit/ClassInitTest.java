package com.sunrise.study.jvm.cinit;

/**
 * @author huangzihua
 * @date 2021-09-18
 */
public class ClassInitTest {
    public static void main(String[] args) {
        // 例子一：通过子类引用父类的静态变量，不会导致子类执行类初始化
//        int x = B.num;

        // 例子二：直接引用类的静态常量不会导致类初始化
        int y = A.age;
    }
}

class A {
    public static int num = 123;
    static final int age = 321;
    static {
        System.out.println("A init ...");
    }
}

class B extends A {
    static {
        System.out.println("B init ...");
    }
}