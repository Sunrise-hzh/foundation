package com.sunrise.study.thread.demo;

/**
 * @author huangzihua
 * @date 2021-09-18
 */
public class ClassTestTest {


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
