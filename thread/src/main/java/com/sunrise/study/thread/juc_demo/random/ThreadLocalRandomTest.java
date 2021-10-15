package com.sunrise.study.thread.juc_demo.random;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author huangzihua
 * @date 2021-09-07
 */
public class ThreadLocalRandomTest {
    public static void main(String[] args) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 10; i++) {
            System.out.println(r.nextInt());
        }
    }
}
