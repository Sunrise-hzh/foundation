package com.sunrise.study.thread.juc_demo.random;

import java.util.Random;

/**
 * 观察源码得知
 * @author huangzihua
 * @date 2021-09-07
 */
public class RandomTest {
    public static void main(String[] args) {
        // 测试使用Random类
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            System.out.println(random.nextInt());
        }
    }
}
