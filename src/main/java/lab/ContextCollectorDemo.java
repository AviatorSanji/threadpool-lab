package lab;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ContextCollectorDemo {

    public static void main(String[] args) throws Exception {
        ThreadLocal<RequestContext> context = new ThreadLocal<>();
        RequestContext parent = new RequestContext("REQ-001");
        context.set(parent);

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            // 原业务任务返回价格，不返回上下文，也不负责绑定、恢复或收集日志。
            List<Callable<Integer>> originalTasks = List.of(
                    createBusinessTask(context, "A", 600, 9000),
                    createBusinessTask(context, "B", 200, 8000),
                    createBusinessTask(context, "C", 300, 7000)
            );

            // main 创建本批次的收集器和包装任务；A/B/C 下标固定为 0/1/2。
            TaskLogCollector collector = new TaskLogCollector(originalTasks.size());
            List<Callable<Integer>> wrappedTasks = new ArrayList<>();
            for (int i = 0; i < originalTasks.size(); i++) {
                wrappedTasks.add(wrapTask(context, parent, collector, i, originalTasks.get(i)));
            }

            System.out.println("准备执行这一批任务");
            List<Future<Integer>> results = pool.invokeAll(wrappedTasks);
            System.out.println("批次等待结束");

            int totalPrice = 0;
            for (int i = 0; i < results.size(); i++) {
                // Future 只返回业务价格，日志已由包装层交给共享收集器。
                int price = results.get(i).get();
                System.out.println("任务下标" + i + "的价格：" + price);
                totalPrice += price;
            }
            System.out.println("价格合计：" + totalPrice);

            System.out.println("合并前，主线程日志：" + parent.getLogs());
            // 日志顺序由收集器的下标保证，这里不从 Future 读取上下文。
            parent.getLogs().addAll(collector.getLogsInTaskOrder());
            System.out.println("合并后，主线程日志：" + parent.getLogs());
            System.out.println("主线程仍然绑定 parent：" + (context.get() == parent));
        } finally {
            context.remove();
            System.out.println("主线程清理后：" + context.get());
        }
    }

    private static Callable<Integer> createBusinessTask(
            ThreadLocal<RequestContext> context, String taskName, long delayMillis, int price) {
        return () -> {
            System.out.println("业务任务" + taskName + "开始，线程：" + Thread.currentThread().getName()
                    + "，请求ID：" + context.get().getRequestId());
            // sleep 仅用于观察调度，不保证每次运行的完成顺序完全相同。
            Thread.sleep(delayMillis);
            context.get().getLogs().add(taskName + "计算完成");
            System.out.println("业务任务" + taskName + "结束");
            return price;
        };
    }

    private static Callable<Integer> wrapTask(
            ThreadLocal<RequestContext> context,
            RequestContext parent,
            TaskLogCollector collector,
            int taskIndex,
            Callable<Integer> task) {
        // wrapTask 在 main 中创建 lambda，下面的代码在实际执行任务时运行。
        return () -> {
            RequestContext workerContext = parent.forWorker();
            RequestContext previousContext = context.get();
            context.set(workerContext);
            try {
                // 执行原业务任务，将它的价格继续向外返回。
                return task.call();
            } finally {
                if (previousContext == null) {
                    context.remove();
                } else {
                    context.set(previousContext);
                }
                System.out.println("任务下标" + taskIndex + "上下文恢复后：" + context.get());

                // 工作上下文仍然存在；清理绑定不会删除对象或日志。
                // collect 内部才加锁，整个业务执行过程没有被锁住。
                collector.collect(taskIndex, workerContext.getLogs());
            }
        };
    }
}
