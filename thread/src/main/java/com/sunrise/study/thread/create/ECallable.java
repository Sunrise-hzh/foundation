package com.sunrise.study.thread.create;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * @author huangzihua
 * @date 2021-08-02
 */
public class ECallable implements Callable<Integer> {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ECallable c = new ECallable(0);
        FutureTask<Integer> ft = new FutureTask<>(c);
        FutureTask<Integer> ft2 = new FutureTask<>(c);
        Thread t1 = new Thread(ft);
        Thread t2 = new Thread(ft2);

        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(ft.get());
        System.out.println(ft2.get());
//        System.out.println(ft2.get().getMoney());
    }


    private int money;

    public ECallable(int money) {
        this.money = money;
    }

    @Override
    public Integer call() throws Exception {
        for (int i = 0; i < 1000; i++) {
            money++;
        }
        System.out.println("线程内" + money);
        return money;
    }
}
