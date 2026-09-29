package lab;

/** 只负责把执行过程打印出来；elapsed 是观察值，不是性能基准。 */
public final class Trace {
    private static final long START = System.nanoTime();
    private static int sequence;

    private Trace() {}

    public static synchronized void event(String message) {
        long elapsed = (System.nanoTime() - START) / 1_000_000;
        System.out.printf("%02d | %4d ms | %-14s | %s%n",
                ++sequence, elapsed, Thread.currentThread().getName(), message);
    }
}
