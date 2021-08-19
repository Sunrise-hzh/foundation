package com.sunrise.study.thread.demo;

import com.sunrise.study.thread.create.Money;

/**
 * @author huangzihua
 * @date 2021-08-04
 */
public class PCDemo {
    public static void main(String[] args) throws InterruptedException {
        Money money = new Money();
        Productor productor = new Productor(money);
        Comsummer comsummer = new Comsummer(money);
        productor.start();
        comsummer.start();
        Thread.sleep(500);
        System.out.println("主线程开始中断");
        productor.interrupt();
        comsummer.interrupt();
        productor.join();
        comsummer.join();
        System.out.println(money.getMoney());
    }
}
