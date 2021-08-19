package com.sunrise.study.thread;

/**
 * @author huangzihua
 * @date 2021-07-09
 */
public class Test {

    public static void main(String[] args) {
        try {
            System.out.println("main 开始");
            throw new RuntimeException("抛异常");
//            return;
        } catch (Exception e) {
            System.out.println("进入异常语句块：" + e.getMessage());
        } finally {
            System.out.println("无论如何都会执行该finally语句");
        }
    }
}
