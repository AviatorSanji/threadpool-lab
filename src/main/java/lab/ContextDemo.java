package lab;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ContextDemo {
    public static void main(String[] args) throws Exception {
        System.out.println("=== 实验1：任务1不清理工作线程上下文 ===");
        runExperiment(false);

        System.out.println();
        System.out.println("=== 实验2：任务1清理工作线程上下文 ===");
        runExperiment(true);
    }

    // 两轮只改变是否清理；每轮的两个任务共用一个单线程池。
    private static void runExperiment(boolean clearWorkerContext) throws Exception {
        ThreadLocal<String> requestId = new ThreadLocal<>();
        System.out.println("设置前：" + requestId.get());

        requestId.set("REQ-001");
        System.out.println("设置后：" + requestId.get());

        String capturedRequestId = requestId.get();

        Callable<String> task = () -> {
            requestId.set(capturedRequestId);
            try {
                System.out.println("任务1执行线程：" + Thread.currentThread().getName());
                return requestId.get();
            } finally {
                // false 仅用于演示残留；实际任务应在 finally 中清理。
                if (clearWorkerContext) {
                    requestId.remove();
                }
                System.out.println("任务1收尾时，工作线程中的值：" + requestId.get());
            }
        };

        try (ExecutorService pool = Executors.newSingleThreadExecutor()) {
            Future<String> result = pool.submit(task);

            System.out.println("主线程收到任务1返回值：" + result.get());

            // 任务1已经结束。后续任务只读取，不调用 set，也不捕获请求 ID。
            Callable<String> nextTask = () -> {
                System.out.println("任务2执行线程：" + Thread.currentThread().getName());
                return requestId.get();
            };
            Future<String> nextResult = pool.submit(nextTask);
            System.out.println("后续任务读到：" + nextResult.get());
            System.out.println("主线程仍然读到：" + requestId.get());
        } finally {
            requestId.remove();
        }

        System.out.println("主线程清理后：" + requestId.get());
    }
}
