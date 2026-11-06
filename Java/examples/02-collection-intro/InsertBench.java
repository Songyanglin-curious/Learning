import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class InsertBench {
    static long ms(long start) { return (System.nanoTime() - start) / 1_000_000; }

    static void warmup() {
        for (int w = 0; w < 3; w++) {
            List<Integer> wl = new ArrayList<>(100_000);
            for (int i = 0; i < 100_000; i++) wl.add(i);
            List<Integer> bad = new ArrayList<>();
            for (int i = 0; i < 100_000; i++) bad.add(i);
        }
    }

    public static void main(String[] args) {
        int N = 1_000_000;
        warmup();

        long t = System.nanoTime();
        List<Integer> noCapacity = new ArrayList<>();
        for (int i = 0; i < N; i++) noCapacity.add(i);
        long a1 = ms(t);

        t = System.nanoTime();
        List<Integer> withCapacity = new ArrayList<>(N);
        for (int i = 0; i < N; i++) withCapacity.add(i);
        long a2 = ms(t);

        t = System.nanoTime();
        List<Integer> linked = new LinkedList<>();
        for (int i = 0; i < N; i++) linked.add(i);
        long a3 = ms(t);

        System.out.printf("尾部追加 %,d 个元素%n", N);
        System.out.printf("  ArrayList 默认容量   : %4d ms%n", a1);
        System.out.printf("  ArrayList 预设容量 N : %4d ms%n", a2);
        System.out.printf("  LinkedList（尾部）   : %4d ms%n", a3);
        System.out.println("size 校验: " + noCapacity.size() + " / " + withCapacity.size() + " / " + linked.size());
    }
}
