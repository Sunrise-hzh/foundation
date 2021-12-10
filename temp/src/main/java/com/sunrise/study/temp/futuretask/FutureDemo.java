package com.sunrise.study.temp.futuretask;

import java.util.concurrent.*;

/**
 * @author huangzihua
 * @date 2021-10-25
 */
public class FutureDemo {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        Runnable task;
        Future future = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Long start = System.currentTimeMillis();
                while (true) {
                    Long current = System.currentTimeMillis();
                    if ((current - start) > 1000) {
                        return 1;
                    }
                }
            }
        });

        try {
            Integer result = (Integer) future.get();
            System.out.println(result);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}
