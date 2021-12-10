package com.sunrise.study.temp.futuretask;

import java.util.concurrent.*;

/**
 * @author huangzihua
 * @date 2021-10-25
 */
public class CancelTest {
    public static void main(String[] args) {
        ExecutorService es = Executors.newSingleThreadExecutor();
        FutureTask<Integer> ft = new FutureTask<>(new MyFutureTask());
        try {
            es.submit(ft);
            Thread.sleep(1000);
            ft.cancel(true);
            Integer integer = ft.get();
            System.out.println(integer);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } finally {
            es.shutdown();
        }
    }


    private static class MyFutureTask implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            for (int i = 0; i < 20000; i++) {
                if (i % 1000 == 0) {
                    System.out.println(i);
                }
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println(i);
                    return i;
                }
            }
            return null;
        }
    }
}
