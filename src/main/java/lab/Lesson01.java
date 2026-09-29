package lab;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class Lesson01 {
    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "pool" : args[0];
        if (args.length > 2 || (!mode.equals("serial") && !mode.equals("pool"))) {
            throw new IllegalArgumentException("用法：bash run.sh serial | bash run.sh pool [1..8]");
        }
        int workers = args.length > 1 ? Integer.parseInt(args[1]) : 2;
        if (workers < 1 || workers > 8) {
            throw new IllegalArgumentException("本课线程数请使用 1..8，便于观察输出");
        }

        Trace.event("REQ-001 请求开始，mode=" + mode);
        List<PriceTask> tasks = List.of(
                new PriceTask("A", 600, 10_000),
                new PriceTask("B", 200, 8_000),
                new PriceTask("C", 300, 9_000));
        Trace.event("已创建 3 个任务对象；此时尚未调用 call()");

        int total = mode.equals("serial") ? runSerial(tasks) : runInPool(tasks, workers);
        Trace.event("REQ-001 汇总完成，total=" + total + " 分");
        if (total != 27_000) {
            throw new AssertionError("预期汇总价格为 27000 分，实际=" + total);
        }
    }

    private static int runSerial(List<PriceTask> tasks) throws Exception {
        int total = 0;
        for (PriceTask task : tasks) {
            Trace.event("DIRECT " + task.room() + "：请求线程直接调用 call()");
            total += task.call();
        }
        return total;
    }

    private static int runInPool(List<PriceTask> tasks, int workers) throws Exception {
        // 与旧算价池一样采用固定大小池和无界队列，本课只缩小线程数。
        // JDK 21 的 close() 会等待池终止；这是教学程序的退出管理，业务不能每请求关池。
        try (ExecutorService pool = Executors.newFixedThreadPool(workers,
                Thread.ofPlatform().name("calc-worker-", 1).factory())) {
            List<Future<Integer>> futures = new ArrayList<>();

            // 第一步：先提交全部任务。submit 返回 Future，不要求任务此时已执行完。
            for (PriceTask task : tasks) {
                Trace.event("SUBMIT " + task.room() + "：准备调用 submit(task)");
                Future<Integer> future = pool.submit(task);
                futures.add(future);
                Trace.event("ACCEPT " + task.room() + "：submit 已返回 Future");
            }

            // 第二步：按提交顺序取结果。get 等待结果，不负责启动任务。
            int total = 0;
            for (int i = 0; i < futures.size(); i++) {
                Trace.event("GET   " + tasks.get(i).room() + "：准备调用 Future.get()");
                int price = futures.get(i).get();
                Trace.event("GOT   " + tasks.get(i).room() + "：取得 " + price);
                total += price;
            }
            return total;
        }
    }
}
