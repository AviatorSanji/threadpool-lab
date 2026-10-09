package lab;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ContextBatchDemo {

    public static void main(String[] args) throws Exception {
        ThreadLocal<RequestContext> context = new ThreadLocal<>();
        RequestContext parent = new RequestContext("REQ-001");
        context.set(parent);

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            // main 调用 createTask 时只创建任务；各任务的上下文在执行时创建。
            List<Callable<RequestContext>> tasks = List.of(
                    createTask(context, parent, "A", 600),
                    createTask(context, parent, "B", 200),
                    createTask(context, parent, "C", 300)
            );

            System.out.println("准备执行这一批任务，线程：" + Thread.currentThread().getName());
            // 两个工作线程执行三个任务；main 等全部任务完成后再处理日志。
            List<Future<RequestContext>> results = pool.invokeAll(tasks);
            System.out.println("批次等待结束");
            System.out.println("合并前，主线程日志：" + parent.getLogs());

            // results 按传入的 A、B、C 顺序排列，不按完成顺序排列。
            for (Future<RequestContext> result : results) {
                RequestContext completedContext = result.get();
                System.out.println("准备合并：" + completedContext.getLogs());
                // 只有 main 修改父日志列表；工作任务只写自己的列表。
                parent.getLogs().addAll(completedContext.getLogs());
            }

            System.out.println("合并后，主线程日志：" + parent.getLogs());
            System.out.println("主线程仍然绑定 parent：" + (context.get() == parent));
        } finally {
            context.remove();
            System.out.println("主线程清理后：" + context.get());
        }
    }

    private static Callable<RequestContext> createTask(
            ThreadLocal<RequestContext> context,
            RequestContext parent,
            String taskName,
            long delayMillis) {
        return () -> {
            // 下面由工作线程执行。每次执行都获得新的上下文和空日志列表。
            RequestContext workerContext = parent.forWorker();
            RequestContext previousContext = context.get();
            context.set(workerContext);
            try {
                System.out.println("任务" + taskName + "开始，线程：" + Thread.currentThread().getName()
                        + "，请求ID：" + context.get().getRequestId());
                System.out.println("任务" + taskName + "是否使用父日志列表："
                        + (workerContext.getLogs() == parent.getLogs()));
                // sleep 仅模拟耗时，实际完成顺序仍受线程调度影响。
                Thread.sleep(delayMillis);
                context.get().getLogs().add(taskName + "计算完成");
                System.out.println("任务" + taskName + "结束");
                // 返回任务自己的上下文，供 main 在完成后读取和合并。
                return workerContext;
            } finally {
                if (previousContext == null) {
                    context.remove();
                } else {
                    context.set(previousContext);
                }
                System.out.println("任务" + taskName + "上下文恢复后：" + context.get());
            }
        };
    }
}
