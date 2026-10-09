package lab;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class WrappingDemo {

    public static void main(String[] args) throws Exception {
        ThreadLocal<String> requestId = new ThreadLocal<>();
        requestId.set("REQ-001");

        try (ExecutorService pool = Executors.newSingleThreadExecutor()) {
            // 业务任务只读取上下文，没有 set 和 remove。
            Callable<String> originalTask = () -> {
                System.out.println("业务任务执行，线程：" + Thread.currentThread().getName()
                        + "，读到请求ID：" + requestId.get());
                return requestId.get();
            };

            Callable<String> wrappedTask = ContextTasks.wrap(requestId, originalTask);
            Future<String> result = pool.submit(wrappedTask);
            System.out.println("主线程收到业务结果：" + result.get());

            // 等前一个任务完成，再复用同一工作线程，检查是否留下上下文。
            Callable<String> nextTask = () -> {
                System.out.println("后续任务执行，线程：" + Thread.currentThread().getName());
                return requestId.get();
            };
            Future<String> nextResult = pool.submit(nextTask);
            System.out.println("后续任务读到：" + nextResult.get());
            System.out.println("主线程仍然读到：" + requestId.get());
        } finally {
            requestId.remove();
            System.out.println("主线程清理后：" + requestId.get());
        }
    }
}
