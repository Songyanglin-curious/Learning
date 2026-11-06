import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ReadOnlyForms {
    static void probe(String label, List<String> list) {
        System.out.println("== " + label + " ==");
        try {
            list.set(0, "改");
            System.out.println("  set(0) 成功 -> " + list);
        } catch (Exception e) {
            System.out.println("  set(0) 失败 -> " + e.getClass().getSimpleName());
        }
        try {
            list.add("新");
            System.out.println("  add 成功 -> " + list);
        } catch (Exception e) {
            System.out.println("  add 失败 -> " + e.getClass().getSimpleName());
        }
    }

    public static void main(String[] args) {
        String[] arr = {"a", "b", "c"};
        List<String> asList = Arrays.asList(arr);
        probe("Arrays.asList", asList);
        arr[0] = "原数组改的";
        System.out.println("  改原数组后，asList 看到: " + asList);
        System.out.println();

        List<String> of = List.of("a", "b");
        probe("List.of", of);
        try {
            List.of("a", null);
        } catch (Exception e) {
            System.out.println("  List.of(\"a\", null) -> " + e.getClass().getSimpleName());
        }
        System.out.println();

        List<String> base = new ArrayList<>(List.of("a", "b"));
        List<String> view = Collections.unmodifiableList(base);
        probe("Collections.unmodifiableList", view);
        base.add("c");
        System.out.println("  底层加 c 后，view 看到: " + view);
        System.out.println();

        List<String> copy = new ArrayList<>(base);
        base.add("d");
        System.out.println("  拷贝(new ArrayList<>(base))不受影响: " + copy);
        System.out.println();

        List<Integer> nums = new ArrayList<>(List.of(1, 2, 3, 4, 5));
        List<Integer> sub = nums.subList(1, 4);
        sub.set(0, 99);
        System.out.println("  改 subList(1,4) 后原列表: " + nums);
        nums.add(6);
        try {
            System.out.println("  原列表结构改动后访问 subList -> " + sub);
        } catch (Exception e) {
            System.out.println("  原列表结构改动后访问 subList -> " + e.getClass().getSimpleName());
        }
        System.out.println();

        List<Integer> a1 = new ArrayList<>(List.of(10, 20, 30));
        a1.remove(1);
        System.out.println("  remove(1)            -> " + a1);

        List<Integer> a2 = new ArrayList<>(List.of(10, 20, 30));
        a2.remove(Integer.valueOf(1));
        System.out.println("  remove(Integer(1))   -> " + a2 + "  (列表里没有元素 1，所以没删)");

        List<Integer> a3 = new ArrayList<>(List.of(10, 1, 30));
        a3.remove(Integer.valueOf(1));
        System.out.println("  删元素 1 的正确写法   -> " + a3);
    }
}
