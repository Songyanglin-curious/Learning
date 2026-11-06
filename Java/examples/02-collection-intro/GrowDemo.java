import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GrowDemo {
    static int capacity(ArrayList<?> list) throws Exception {
        Field f = ArrayList.class.getDeclaredField("elementData");
        f.setAccessible(true);
        return ((Object[]) f.get(list)).length;
    }

    public static void main(String[] args) throws Exception {
        ArrayList<Integer> al = new ArrayList<>();
        System.out.print("ArrayList capacity growth: ");
        int last = -1;
        for (int i = 0; i < 40; i++) {
            al.add(i);
            int c = capacity(al);
            if (c != last) { System.out.print(c + " "); last = c; }
        }
        System.out.println();

        // 验证 LinkedList.get 从较近一端走：对比只从头走的情形不易做，这里打印一次规模无关的观察
        List<Integer> ll = new LinkedList<>();
        for (int i = 0; i < 10; i++) ll.add(i);
        System.out.println("LinkedList get(8) = " + ll.get(8));
        System.out.println("ArrayList 默认构造后再取容量（0 表示未分配）: " + capacity(new ArrayList<>()));
    }
}
