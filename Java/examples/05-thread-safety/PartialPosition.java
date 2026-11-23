import java.util.concurrent.CountDownLatch;

public class PartialPosition {
    private static class SharedState {
        int x;
        int y;
    }

    // 让读线程在 x 已更新、y 尚未更新时读取坐标。
    public static void main(String[] args) throws InterruptedException {
        SharedState state = new SharedState();
        CountDownLatch xWritten = new CountDownLatch(1);
        CountDownLatch positionRead = new CountDownLatch(1);

        Thread writer = new Thread(() -> {
            state.x = 100;
            xWritten.countDown();
            await(positionRead);
            state.y = 100;
        });
        Thread reader = new Thread(() -> {
            await(xWritten);
            int x = state.x;
            int y = state.y;
            positionRead.countDown();
            System.out.printf("中间状态：读到坐标 (%d, %d)%n", x, y);
        });

        writer.start();
        reader.start();
        writer.join();
        reader.join();
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
