package com.sunrise.study.thread.juc_demo.cas;

import com.sunrise.study.thread.create.Money;

/**
 * @author huangzihua
 * @date 2021-08-03
 */
public class CASTest implements Runnable {
    public static void main(String[] args) throws InterruptedException {
        Money money = new Money();
        CASTest casTest = new CASTest(money);
        Thread thread1 = new Thread(casTest);
        Thread thread2 = new Thread(casTest);
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        System.out.println(money.getMoney());
        System.out.println(money.getValue());
    }

    private Money money;

    public CASTest(Money money) {
        this.money = money;
    }

    @Override
    public void run() {
        for (int i = 0; i < 10000; i++) {
            money.increment();
        }
    }
}
