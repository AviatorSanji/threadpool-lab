package lab;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ContextTimeoutDemo {

    public static void main(String[] args) throws Exception {
        ThreadLocal<RequestContext> context = new ThreadLocal<>();
        RequestContext parent = new RequestContext("REQ-001");
        context.set(parent);
        TaskLogCollector collector = new TaskLogCollector(2);

        // 初始计数为 1，await 会等待。主线程关闭收集器后 countDown，才允许 B 继续。
        CountDownLatch mainFinished = new CountDownLatch(1);

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Callable<Integer> taskA = () -> {
                System.out.println("业务任务A开始，线程：" + Thread.currentThread().getName());
                context.get().getLogs().add("A计算完成");
                return 9000;
            };
            Callable<Integer> taskB = () -> {
                System.out.println("业务任务B开始，线程：" + Thread.currentThread().getName());
                context.get().getLogs().add("B超时前的日志");
                try {
                    // 教学用门闩：主线程在批次收尾前不会放行，因此 B 无法正常完成。
                    mainFinished.await();
                    return 8000;
                } catch (InterruptedException e) {
                    System.out.println("业务任务B收到取消引发的中断");
                    throw e;
                }
            };

            List<Callable<Integer>> tasks = List.of(
                    wrapTask(context, parent, collector, 0, taskA, mainFinished),
                    wrapTask(context, parent, collector, 1, taskB, mainFinished)
            );

            try {
                System.out.println("准备执行，批次等待预算为 1000 毫秒");
                List<Future<Integer>> results = pool.invokeAll(tasks, 1000, TimeUnit.MILLISECONDS);
                System.out.println("批次等待结束");
                for (int i = 0; i < results.size(); i++) {
                    try {
                        System.out.println("任务下标" + i + "的价格：" + results.get(i).get());
                    } catch (CancellationException e) {
                        System.out.println("任务下标" + i + "的 Future 已取消，任务 finally 可能还没结束");
                    }
                }
            } finally {
                try {
                    // 不论批次正常返回还是抛异常，都执行关闭、快照和清空。
                    List<String> acceptedLogs = collector.closeAndGetLogs();
                    // 合并在 main 中完成，不需要一直持有收集器的锁。
                    parent.getLogs().addAll(acceptedLogs);
                    System.out.println("收尾后，主线程日志：" + parent.getLogs());
                } finally {
                    // 必须先放行 B，再离开资源块；pool.close() 会等待工作线程真正结束。
                    System.out.println("main 放行迟到日志提交");
                    mainFinished.countDown();
                }
            }
        } finally {
            context.remove();
            System.out.println("主线程清理后：" + context.get());
        }

        // 已离开线程池资源块，B 的 finally 也已经执行完，结果仍不应改变。
        System.out.println("所有工作线程退出后，主线程日志：" + parent.getLogs());
        System.out.println("所有工作线程退出后，收集器内容：" + collector.getLogsInTaskOrder());
    }

    private static Callable<Integer> wrapTask(
            ThreadLocal<RequestContext> context,
            RequestContext parent,
            TaskLogCollector collector,
            int taskIndex,
            Callable<Integer> task,
            CountDownLatch mainFinished) {
        return () -> {
            RequestContext workerContext = parent.forWorker();
            RequestContext previousContext = context.get();
            context.set(workerContext);
            try {
                return task.call();
            } finally {
                if (previousContext == null) {
                    context.remove();
                } else {
                    context.set(previousContext);
                }
                System.out.println("任务下标" + taskIndex + "上下文恢复后：" + context.get());

                if (taskIndex == 1) {
                    // 故意延迟 B 的日志移交，确保它在主线程关闭收集器后才提交。
                    // B 的 await 抛 InterruptedException 时已清除中断标志，此处可继续等待。
                    System.out.println("B进入 finally，等待 main 完成收尾");
                    mainFinished.await();
                }
                collector.collect(taskIndex, workerContext.getLogs());
            }
        };
    }
}
