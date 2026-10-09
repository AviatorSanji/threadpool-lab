package lab;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ContextObjectDemo {

    public static void main(String[] args) throws Exception {
        ThreadLocal<RequestContext> context = new ThreadLocal<>();
        RequestContext parent = new RequestContext("REQ-001");
        context.set(parent);
        System.out.println("任务执行前，主线程的日志：" + parent.getLogs());

        try (ExecutorService pool = Executors.newSingleThreadExecutor()) {
            Callable<String> task = () -> {
                // 两个线程的 ThreadLocal 记录不同，但记录指向同一个 parent 对象。
                context.set(parent);
                try {
                    RequestContext workerView = context.get();
                    System.out.println("工作线程：" + Thread.currentThread().getName());
                    System.out.println("工作线程与主线程使用同一个上下文对象：" + (workerView == parent));
                    System.out.println("工作线程与主线程使用同一个日志列表："
                            + (workerView.getLogs() == parent.getLogs()));

                    // 本实验只有一个工作任务写入；主线程等它完成后才读取。
                    workerView.getLogs().add("任务1计算完成");
                    return workerView.getRequestId();
                } finally {
                    // 移除工作线程的记录，不会清空 parent 对象内部的日志。
                    context.remove();
                    System.out.println("工作线程清理后：" + context.get());
                }
            };

            Future<String> result = pool.submit(task);
            System.out.println("主线程收到任务结果：" + result.get());
            System.out.println("任务执行后，主线程的日志：" + parent.getLogs());
            System.out.println("主线程仍然绑定 parent 对象：" + (context.get() == parent));
        } finally {
            context.remove();
            System.out.println("主线程清理后：" + context.get());
        }
    }
}
