package com.sunrise.study.temp.futuretask;

import java.util.concurrent.*;

/**
 * @author huangzihua
 * @date 2021-10-25
 */
public class AwaitDoneOutTimeTest {
    public static void main(String[] args) throws ExecutionException, InterruptedException, TimeoutException {
        FutureTask<Integer> ft = new FutureTask<>(new MyFutureTask());
        Integer integer = ft.get(1, TimeUnit.SECONDS);
        System.out.println(integer);
    }

    private static class MyFutureTask implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {

            return 100;
        }
    }
}
