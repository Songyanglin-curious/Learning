import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class ListCompare {
    static int N = 200_000;          // 列表规模
    static int RANDOM_GETS = 5_000;  // 随机按下标取
    static int HEAD_ADDS = 20_000;   // 往头部插

    public static void main(String[] args) {
        List<Integer> arrayList = new ArrayList<>();
        List<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < N; i++) {
            arrayList.add(i);
            linkedList.add(i);
        }
        int[] idx = new int[RANDOM_GETS];
        Random r = new Random(42);
        for (int i = 0; i < RANDOM_GETS; i++) idx[i] = r.nextInt(N);

        long t1 = System.nanoTime();
        long sum = 0;
        for (int i : idx) sum += arrayList.get(i);
        long a1 = (System.nanoTime() - t1) / 1_000_000;

        t1 = System.nanoTime();
        for (int i : idx) sum += linkedList.get(i);
        long l1 = (System.nanoTime() - t1) / 1_000_000;

        t1 = System.nanoTime();
        for (int i = 0; i < HEAD_ADDS; i++) arrayList.add(0, -1);
        long a2 = (System.nanoTime() - t1) / 1_000_000;

        t1 = System.nanoTime();
        for (int i = 0; i < HEAD_ADDS; i++) linkedList.add(0, -1);
        long l2 = (System.nanoTime() - t1) / 1_000_000;

        t1 = System.nanoTime();
        for (int v : arrayList) sum += v;
        long a3 = (System.nanoTime() - t1) / 1_000_000;

        t1 = System.nanoTime();
        for (int v : linkedList) sum += v;
        long l3 = (System.nanoTime() - t1) / 1_000_000;

        System.out.printf("列表规模 %,d%n", N);
        System.out.printf("%-28s %10s %10s%n", "操作", "ArrayList", "LinkedList");
        System.out.printf("%-28s %8d ms %8d ms%n", "随机 get x " + RANDOM_GETS, a1, l1);
        System.out.printf("%-28s %8d ms %8d ms%n", "头部 add(0,x) x " + HEAD_ADDS, a2, l2);
        System.out.printf("%-28s %8d ms %8d ms%n", "增强 for 遍历全部", a3, l3);
        if (sum == 42) System.out.println();
    }
}
