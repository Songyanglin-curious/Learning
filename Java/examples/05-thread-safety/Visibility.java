public class Visibility {
    private static class SharedState {
        int data;
        boolean ready;
        volatile boolean volatileReady;
        int observed = -1;
    }

    // 对比普通标志与 volatile 标志的读数。
    public static void main(String[] args) throws InterruptedException {
        compareVisibility(false);
        compareVisibility(true);
    }

    // 重复发布和读取数据，统计普通标志或 volatile 标志下的观测结果。
    private static void compareVisibility(boolean useVolatile) throws InterruptedException {
        int trials = 1_000;
        int notReady = 0;
        int sawZero = 0;
        int sawHundred = 0;

        for (int i = 0; i < trials; i++) {
            SharedState state = new SharedState();
            Thread writer = new Thread(() -> {
                state.data = 100;
                if (useVolatile) {
                    state.volatileReady = true;
                } else {
                    state.ready = true;
                }
            });
            Thread reader = new Thread(() -> {
                boolean ready = useVolatile ? state.volatileReady : state.ready;
                if (ready) {
                    state.observed = state.data;
                }
            });

            // 两个线程先启动。主线程只在实验结束后收集结果。
            writer.start();
            reader.start();
            writer.join();
            reader.join();

            switch (state.observed) {
                case -1 -> notReady++;
                case 0 -> sawZero++;
                case 100 -> sawHundred++;
                default -> throw new IllegalStateException("意外读数：" + state.observed);
            }
        }
        // 本机未复现异常读数。`volatile` 提供的保证是，一旦读到写线程设置的 true，接下来就一定读到 100。
        // 但是这一问题在语言规范中确实存在
        // https://docs.oracle.com/javase/specs/jls/se17/html/jls-17.html#jls-17.4.5
        String label = useVolatile ? "volatile ready" : "普通 ready";
        System.out.printf("%s（%d 轮）：ready 为 false %d 次，读到 0 共 %d 次，读到 100 共 %d 次%n",
                label, trials, notReady, sawZero, sawHundred);
    }
}
