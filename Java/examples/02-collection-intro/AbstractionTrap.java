import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AbstractionTrap {
    // 签名只承诺"能按下标取"，没承诺代价
    static long sumByIndex(List<Integer> list) {
        long sum = 0;
        for (int i = 0; i < list.size(); i++) sum += list.get(i);
        return sum;
    }

    public static void main(String[] args) {
        int n = 20_000;
        List<Integer> al = new ArrayList<>();
        List<Integer> ll = new LinkedList<>();
        for (int i = 0; i < n; i++) { al.add(i); ll.add(i); }

        long t = System.nanoTime();
        sumByIndex(al);
        long d1 = (System.nanoTime() - t) / 1_000_000;

        t = System.nanoTime();
        sumByIndex(ll);
        long d2 = (System.nanoTime() - t) / 1_000_000;

        System.out.println("同一个方法 sumByIndex(List)，n = " + n);
        System.out.println("传 ArrayList  : " + d1 + " ms");
        System.out.println("传 LinkedList : " + d2 + " ms");
    }
}
