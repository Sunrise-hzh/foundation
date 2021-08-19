package com.sunrise.study.thread.create;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author huangzihua
 * @date 2021-08-02
 */
public class Money {
    private int money = 0;
    private AtomicInteger value = new AtomicInteger(0);

    public AtomicInteger getValue() {
        return value;
    }

    public void setValue(AtomicInteger value) {
        this.value = value;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public void increment() {
        this.money++;
        this.value.addAndGet(1);
    }

    public void decrement() {
        this.money--;
        this.value.decrementAndGet();
    }

}
