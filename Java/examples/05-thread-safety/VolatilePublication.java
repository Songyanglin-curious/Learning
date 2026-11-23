public class VolatilePublication {
    private static int data = 0;
    private static volatile boolean ready = false;

    // 启动读线程，再写入数据并通过 ready 发布。
    public static void main(String[] args) throws InterruptedException {
        Thread reader = new Thread(() -> {
            while (!ready) {
                Thread.onSpinWait();
            }
            System.out.println("读到 ready = true，data = " + data);
        });

        reader.start();

        // 主线程先准备数据，再通知读线程。
        data = 100;
        ready = true;

        reader.join();
    }
}
