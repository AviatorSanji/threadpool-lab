package lab;

import java.util.concurrent.Callable;

public class ContextRestoreDemo {

    public static void main(String[] args) throws Exception {
        ThreadLocal<String> requestId = new ThreadLocal<>();

        try {
            requestId.set("REQ-001");
            Callable<String> originalTask = () -> {
                System.out.println("业务任务执行，线程：" + Thread.currentThread().getName()
                        + "，读到请求ID：" + requestId.get());
                return requestId.get();
            };

            // 包装时捕获 REQ-001，尚未执行任务。
            Callable<String> wrappedTask = ContextTasks.wrap(requestId, originalTask);

            // 模拟执行任务的线程已经有外层上下文。
            requestId.set("OUTER");
            System.out.println("执行前：" + requestId.get());

            // 直接在 main 执行，便于观察保存、绑定和恢复的顺序。
            String result = wrappedTask.call();
            System.out.println("任务返回：" + result);
            System.out.println("执行后：" + requestId.get());
        } finally {
            requestId.remove();
            System.out.println("主线程清理后：" + requestId.get());
        }
    }
}
