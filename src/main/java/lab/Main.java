package lab;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws Exception {
        MyPriceTask myPriceTask = new MyPriceTask();

        Callable<Integer> myPriceTask2 = () -> {
            System.out.println("任务2开始，线程名称：" + Thread.currentThread().getName());
            Thread.sleep(500);
            //throw new IllegalStateException("任务2失败");
            System.out.println("任务2结束");
            return 10000-2000;
        };

        Callable<Integer> myPriceTask3 = () -> {
            System.out.println("任务3开始，线程名称：" + Thread.currentThread().getName());
            Thread.sleep(300);

            System.out.println("任务3结束");
            return 10000-3000;
        };

        Callable<Integer> myPriceTask4 = () -> {
            System.out.println("任务4开始，线程名称：" + Thread.currentThread().getName());
            Thread.sleep(1000);

            System.out.println("任务4结束");
            return 10000-4000;
        };

        Callable<Integer> myPriceTask5 = () -> {
            System.out.println("任务5开始，线程名称：" + Thread.currentThread().getName());
            Thread.sleep(1000);

            System.out.println("任务5结束");
            return 10000-5000;
        };

        List<Callable<Integer>> tasks  = List.of(myPriceTask,myPriceTask2,myPriceTask3,myPriceTask4,myPriceTask5);
        System.out.println(Thread.currentThread().getName());
        try (ExecutorService pool = new ThreadPoolExecutor(
                2,
                3,
                60L,TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1),
                new ThreadPoolExecutor.CallerRunsPolicy())) {
            try{
                System.out.println("准备执行");
                List<Future<Integer>> results = pool.invokeAll(tasks);
                System.out.println("批次等待结束");
                for (Future<Integer> result : results) {
                    try{
                        System.out.println(result.get());
                    } catch (ExecutionException e) {
                        System.out.println(e.getCause());
                    } catch (CancellationException e) {
                        System.out.println("当前任务被取消了");
                    }
                }
            } catch (RejectedExecutionException e){
                System.out.println("提交任务被拒绝：" + e);
            }
            // Future<Integer> result = pool.submit(myPriceTask);
            // Future<Integer> result2 = pool.submit(myPriceTask2);
            // Future<Integer> result3 = pool.submit(myPriceTask3);
            // System.out.println("已提交三个任务");
            // System.out.println(result.get());
            // try{
            //     Integer price2 = result2.get();
            //     System.out.println(price2);
            // } catch (ExecutionException e) {
            //     System.out.println("任务2失败，原因：" + e.getCause());
            // }
            // System.out.println(result3.get());
            // System.out.println("主线程继续执行");
        }
    }
}
