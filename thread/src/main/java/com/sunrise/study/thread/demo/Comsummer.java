package com.sunrise.study.thread.demo;

import com.sunrise.study.thread.create.Money;

/**
 * @author huangzihua
 * @date 2021-08-04
 */
public class Comsummer extends Thread {
    private Money money;
    private int count = 0;
    private int waitCount = 0;

    public Comsummer(Money money) {
        this.money = money;
    }

    @Override
    public void run() {
        try {
            while (true) {
                if (this.isInterrupted()) {
                    System.out.println("消费者被中断");
                    return;
                }
                synchronized (money) {
                    if (money.getMoney() <= 0) {
                        System.out.println("消费者被挂起次数：" + waitCount);
                        waitCount++;
                        money.wait();
                    }
                    money.decrement();
                    count++;
                    System.out.println("消费者，第"+count+"次：money = " + money.getMoney());
                    money.notify();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
