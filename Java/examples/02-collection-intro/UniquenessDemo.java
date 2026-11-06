import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UniquenessDemo {
    public static void main(String[] args) {
        // 两个"引用不同、内容相同"的对象
        String a = new String("小明");
        String b = new String("小明");
        System.out.println("a == b (引用相同吗) : " + (a == b));
        System.out.println("a.equals(b) 内容相同吗: " + a.equals(b));

        List<String> list = new ArrayList<>();
        list.add(a);
        list.add(b);
        System.out.println("List 加两个等值对象 -> size = " + list.size());

        Set<String> set = new HashSet<>();
        System.out.println("Set 第一次 add: " + set.add(a));
        System.out.println("Set 第二次 add(另一个同内容对象): " + set.add(b));
        System.out.println("Set size = " + set.size());

        // Map：唯一的是 key，value 随便重复
        Map<String, Integer> map = new HashMap<>();
        map.put("小明", 100);
        map.put("小红", 100);      // value 相同，没问题
        System.out.println("两个不同 key、相同 value -> map = " + map + ", size = " + map.size());

        Integer old = map.put("小明", 60);   // key 重复 = 覆盖
        System.out.println("同一个 key 再 put -> 返回旧值 " + old + "，map = " + map + ", size = " + map.size());
    }
}
