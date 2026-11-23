import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class VolatileReadComparison {
    private static final int ROUNDS = 30_000;

    private int x;
    private int y;
    private volatile int volatileX;
    private volatile int volatileY;
    private int a;
    private int b;
    private long startAt;

    // 分别统计普通字段和 volatile 字段下，两边同时读到 0 的次数。
    public static void main(String[] args) throws InterruptedException {
        new VolatileReadComparison().run(false);
        new VolatileReadComparison().run(true);
    }

    // 用屏障划分每轮实验，在线程完成读写后统计结果。
    private void run(boolean useVolatile) throws InterruptedException {
        CyclicBarrier start = new CyclicBarrier(2, () -> startAt = System.nanoTime() + 50_000);
        CyclicBarrier finish = new CyclicBarrier(2);
        Thread worker = new Thread(() -> {
            for (int i = 0; i < ROUNDS; i++) {
                await(start);
                waitForStart();
                if (useVolatile) {
                    volatileX = 1;
                    a = volatileY;
                } else {
                    x = 1;
                    a = y;
                }
                await(finish);
            }
        });
        worker.start();

        int bothZero = 0;
        for (int i = 0; i < ROUNDS; i++) {
            x = y = 0;
            volatileX = volatileY = 0;
            await(start);
            waitForStart();
            if (useVolatile) {
                volatileY = 1;
                b = volatileX;
            } else {
                y = 1;
                b = x;
            }
            await(finish);
            if (a == 0 && b == 0) {
                bothZero++;
            }
        }
        worker.join();

        String label = useVolatile ? "volatile 字段" : "普通字段";
        System.out.printf("%s：%d 轮中，两边都读到 0 共 %d 次%n", label, ROUNDS, bothZero);
    }

    // 等到同一起跑时刻，减少屏障唤醒先后造成的偏差。
    private void waitForStart() {
        while (System.nanoTime() - startAt < 0) {
            Thread.onSpinWait();
        }
    }

    // 等两个线程都到达本轮的起点或终点。
    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("实验被中断", e);
        } catch (BrokenBarrierException e) {
            throw new IllegalStateException("另一个线程未能完成本轮实验", e);
        }
    }
}
