package com.sunrise.study.thread.create;

/**
 * @author huangzihua
 * @date 2021-08-02
 */
public class SynchronizedTest {

    public static void main(String[] args) {
        System.out.println("hello");
    }

    public void test() {
        int v = 1;
        synchronized (this) {
            v++;
        }
    }
}
