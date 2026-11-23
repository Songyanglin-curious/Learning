public class Main {
    // 演示主线程启动子线程后继续工作，并在最后等待子线程结束。
    public static void main(String[] args) throws InterruptedException {
        System.out.println("主线程开始");

        Thread worker = new Thread(() -> {
            System.out.println("子线程开始");
            System.out.println("子线程工作结束");
        });

        worker.start();
        Thread.sleep(1);
        System.out.println("主线程继续工作");

        worker.join();

        System.out.println("全部结束");
    }
}
