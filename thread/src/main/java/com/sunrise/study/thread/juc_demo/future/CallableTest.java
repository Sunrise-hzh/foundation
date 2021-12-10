package com.sunrise.study.thread.juc_demo.future;

import java.util.concurrent.*;

/**
 * @author huangzihua
 * @date 2021-10-22
 */
public class CallableTest {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Callable<String> callable = new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "TEST";
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<String> result = executor.submit(callable);
        System.out.println(result.get());
    }
}
