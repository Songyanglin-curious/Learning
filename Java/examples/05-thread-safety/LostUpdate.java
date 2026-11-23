import java.util.concurrent.CountDownLatch;

public class LostUpdate {
    private static class SharedState {
        int count;
    }

    // 让两个线程都读完 count，再分别写回自己的计算结果。
    public static void main(String[] args) throws InterruptedException {
        SharedState state = new SharedState();
        CountDownLatch bothRead = new CountDownLatch(2);

        Runnable increment = () -> {
            int previous = state.count;
            bothRead.countDown();
            await(bothRead);
            state.count = previous + 1;
        };

        Thread first = new Thread(increment);
        Thread second = new Thread(increment);
        first.start();
        second.start();
        first.join();
        second.join();

        System.out.printf("丢失更新：两个线程各加 1，最终 count = %d%n", state.count);
    }

    // 在工作线程中等待锁存器，收到中断时结束当前任务并报告原因。
    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("实验线程等待时被中断", e);
        }
    }
}
