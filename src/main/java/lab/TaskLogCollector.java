package lab;

import java.util.ArrayList;
import java.util.List;

public class TaskLogCollector {

    // 一批任务共用这个收集器；每个下标保存对应任务的日志。
    private final List<List<String>> logsByTask = new ArrayList<>();
    // 只在持有本收集器的锁时访问，控制是否还接收日志。
    private boolean closed;

    public TaskLogCollector(int taskCount) {
        // 先放入占位元素，之后才能用 set(index, logs) 替换相应位置。
        for (int i = 0; i < taskCount; i++) {
            logsByTask.add(null);
        }
    }

    // synchronized 实例方法锁住 this，也就是这个收集器对象。
    // 三个工作任务共用同一个收集器，因此使用同一把锁。
    public synchronized void collect(int taskIndex, List<String> logs) {
        // 必须在 set 前检查：关闭时列表已经清空，迟到任务不能再写入。
        if (closed) {
            System.out.println("收集器已关闭，忽略迟到日志，任务下标：" + taskIndex + "，日志：" + logs);
            return;
        }
        // 保存日志列表的副本，避免之后对任务列表的修改影响收集结果。
        logsByTask.set(taskIndex, new ArrayList<>(logs));
        System.out.println("收集日志，线程：" + Thread.currentThread().getName()
                + "，任务下标：" + taskIndex + "，收集器：" + logsByTask);
    }

    // 主线程使用同一把锁读取。返回一个新列表，不暴露内部可变列表。
    public synchronized List<String> getLogsInTaskOrder() {
        return copyLogsInTaskOrder();
    }

    public synchronized List<String> closeAndGetLogs() {
        // 三个动作在同一把锁下完成，collect 不能插入到它们之间。
        closed = true;
        List<String> snapshot = copyLogsInTaskOrder();
        logsByTask.clear();
        System.out.println("收集器已关闭并清空，日志快照：" + snapshot);
        return snapshot;
    }

    // 仅由上面的 synchronized 方法调用；返回独立的日志列表。
    private List<String> copyLogsInTaskOrder() {
        List<String> result = new ArrayList<>();
        for (List<String> logs : logsByTask) {
            if (logs != null) {
                result.addAll(logs);
            }
        }
        return result;
    }
}
