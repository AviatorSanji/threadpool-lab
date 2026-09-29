package lab;

import java.util.concurrent.Callable;

/** 一个母房型一个任务。对象是任务描述，不是线程。 */
public record PriceTask(String room, long workMillis, int priceInCents) implements Callable<Integer> {
    @Override
    public Integer call() throws InterruptedException {
        Trace.event("START " + room + "：进入 call()");
        // 仅模拟耗时。没有吞掉中断，避免养成不可取消任务的写法。
        Thread.sleep(workMillis);
        Trace.event("END   " + room + "：call() 即将返回 " + priceInCents);
        return priceInCents;
    }
}
