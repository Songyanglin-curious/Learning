import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MiddleInsert {
    public static void main(String[] args) {
        int n = 100_000;
        int times = 2_000;
        List<Integer> al = new ArrayList<>();
        List<Integer> ll = new LinkedList<>();
        for (int i = 0; i < n; i++) { al.add(i); ll.add(i); }

        long t = System.nanoTime();
        for (int i = 0; i < times; i++) al.add(n / 2, -1);
        long a = (System.nanoTime() - t) / 1_000_000;

        t = System.nanoTime();
        for (int i = 0; i < times; i++) ll.add(n / 2, -1);
        long l = (System.nanoTime() - t) / 1_000_000;

        t = System.nanoTime();
        for (int i = 0; i < times; i++) al.add(-1);
        long aTail = (System.nanoTime() - t) / 1_000_000;

        t = System.nanoTime();
        for (int i = 0; i < times; i++) ll.add(-1);
        long lTail = (System.nanoTime() - t) / 1_000_000;

        System.out.println("列表 10 万条，插入 " + times + " 次");
        System.out.println("  中间 add(n/2, x): ArrayList " + a + " ms / LinkedList " + l + " ms");
        System.out.println("  尾部 add(x)     : ArrayList " + aTail + " ms / LinkedList " + lTail + " ms");
    }
}
