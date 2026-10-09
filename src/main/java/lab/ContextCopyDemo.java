package lab;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ContextCopyDemo {

    public static void main(String[] args) throws Exception {
        ThreadLocal<RequestContext> context = new ThreadLocal<>();
        RequestContext parent = new RequestContext("REQ-001");
        context.set(parent);
        System.out.println("任务执行前，主线程日志：" + parent.getLogs());

        try (ExecutorService pool = Executors.newSingleThreadExecutor()) {
            Callable<RequestContext> task = () -> {
                // 在工作线程中创建新上下文：请求 ID 相同，日志列表是新建的。
                RequestContext workerContext = parent.forWorker();
                RequestContext previousContext = context.get();
                context.set(workerContext);
                try {
                    System.out.println("工作线程：" + Thread.currentThread().getName());
                    System.out.println("工作任务请求ID：" + context.get().getRequestId());
                    System.out.println("同一个上下文对象：" + (workerContext == parent));
                    System.out.println("同一个日志列表："
                            + (workerContext.getLogs() == parent.getLogs()));

                    // 业务通过 ThreadLocal 找到自己的上下文，只写自己的日志列表。
                    context.get().getLogs().add("任务1计算完成");
                    // 返回这个对象，让主线程在任务结束后取得日志。
                    return workerContext;
                } finally {
                    // 保留上一课的恢复规则；新工作线程原本没有上下文，通常走 remove。
                    if (previousContext == null) {
                        context.remove();
                    } else {
                        context.set(previousContext);
                    }
                    System.out.println("工作线程恢复后：" + context.get());
                }
            };

            Future<RequestContext> result = pool.submit(task);
            // get 返回后，任务及其 finally 已完成，主线程可以读取任务返回的上下文。
            RequestContext completedContext = result.get();
            System.out.println("合并前，主线程日志：" + parent.getLogs());
            System.out.println("工作任务日志：" + completedContext.getLogs());

            // 主线程负责合并：将日志条目添加到主线程自己的列表中。
            parent.getLogs().addAll(completedContext.getLogs());
            System.out.println("合并后，主线程日志：" + parent.getLogs());
            System.out.println("合并后，仍是不同日志列表："
                    + (parent.getLogs() != completedContext.getLogs()));
            System.out.println("主线程仍然绑定 parent：" + (context.get() == parent));
        } finally {
            context.remove();
            System.out.println("主线程清理后：" + context.get());
        }
    }
}
