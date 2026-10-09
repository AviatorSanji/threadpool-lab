package lab;

import java.util.concurrent.Callable;

public class ContextTasks {

    public static Callable<String> wrap(ThreadLocal<String> requestId, Callable<String> task) {
        // 调用 wrap 时执行：本例是在 main 中捕获请求 ID。
        String capturedRequestId = requestId.get();
        System.out.println("包装任务，线程：" + Thread.currentThread().getName()
                + "，捕获的请求ID：" + capturedRequestId);

        // 此时只创建新任务；lambda 内部要等 wrappedTask 被执行时才运行。
        return () -> {
            // 在任务真正执行时保存旧值，再绑定任务自己的上下文。
            String previousRequestId = requestId.get();
            requestId.set(capturedRequestId);
            try {
                System.out.println("绑定上下文，线程：" + Thread.currentThread().getName()
                        + "，请求ID：" + requestId.get());
                // 把实际业务交给原任务；原任务不用处理绑定和清理。
                return task.call();
            } finally {
                if (previousRequestId == null) {
                    // 原来没有值，清理本次任务绑定的值。
                    requestId.remove();
                } else {
                    // 原来有值，恢复执行线程的外层上下文。
                    requestId.set(previousRequestId);
                }
                System.out.println("恢复上下文，线程：" + Thread.currentThread().getName()
                        + "，恢复后：" + requestId.get());
            }
        };
    }
}
