package com.sunrise.study.temp.futuretask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author huangzihua
 * @date 2021-10-25
 */
public class FutureTaskTest {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        List<FutureTask<Integer>> taskList = new ArrayList<>();
        ExecutorService exec = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 10; i++) {
            FutureTask<Integer> ft = new FutureTask<>(new ComputeTask(i, "" + i));
            taskList.add(ft);
            exec.submit(ft);
        }
        System.out.println("全部计算任务提交完毕，主线程接着执行");
        Integer totalResult = 0;
        for (FutureTask<Integer> futureTask : taskList) {
            totalResult = totalResult + futureTask.get();
        }

        exec.shutdown();
        System.out.println("结果是:" + totalResult);
    }

    private static class ComputeTask implements Callable<Integer> {
        private Integer result = 0;
        private String taskName = "";

        public ComputeTask(Integer result, String taskName) {
            this.result = result;
            this.taskName = taskName;
            System.out.println("生产子线程计算任务：" + taskName);
        }

        public String getTaskName() {
            return this.taskName;
        }

        @Override
        public Integer call() throws Exception {
            for (int i = 0; i < 100; i++) {
                result += i;
            }
            Thread.sleep(5000);
            System.out.println("子线程计算任务：" + taskName + " 执行完成！");
            return result;
        }
    }

    private static class FutureTaskForMultiCompute {

    }
}
