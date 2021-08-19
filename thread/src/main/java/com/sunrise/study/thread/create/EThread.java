package com.sunrise.study.thread.create;

/**
 * 继承Thread类
 * @author huangzihua
 * @date 2021-08-02
 */
public class EThread extends Thread{
    private Money money;

    public EThread(Money money) {
        this.money = money;
    }

    @Override
    public void run() {
        for (int i = 0; i < 1000; i++) {
            int v = this.money.getMoney();
            v++;
            money.setMoney(v);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Money money = new Money();
        EThread thread = new EThread(money);
        EThread thread2 = new EThread(money);
        thread.start();
        thread2.start();
        thread.join();
        thread2.join();
        System.out.println(money.getMoney());
    }
}