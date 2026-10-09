package lab;

import java.util.concurrent.Callable;

public class MyPriceTask implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        int originalPrice = 10000;
        int discount = 1000;
        System.out.println("任务1开始，线程名称：" + Thread.currentThread().getName());
        try{
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            System.out.println("任务1中断，线程名称"+Thread.currentThread().getName());
            throw e;
        }
        System.out.println("任务1结束");
        return originalPrice - discount;
    }
}
