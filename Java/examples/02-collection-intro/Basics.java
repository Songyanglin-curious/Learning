import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Basics {
    public static void main(String[] args) {
        // 1. 数组：能装多个，但长度在创建时就定死
        String[] names = new String[3];
        names[0] = "小明";
        names[1] = "小红";
        names[2] = "小刚";
        System.out.println("数组长度：" + names.length);
        // names[3] = "小强"; // ArrayIndexOutOfBoundsException

        // 2. List：变长、有顺序、允许重复
        List<String> list = new ArrayList<>();
        list.add("小明");
        list.add("小红");
        list.add("小明");
        System.out.println("List：" + list + "，大小 " + list.size());

        // 3. Set：重复的装不进去
        Set<String> set = new HashSet<>();
        set.add("小明");
        set.add("小红");
        boolean added = set.add("小明");
        System.out.println("Set：" + set + "，第二次 add 返回 " + added);

        // 4. Map：按 key 查 value
        Map<String, Integer> scores = new HashMap<>();
        scores.put("小明", 92);
        scores.put("小红", 88);
        System.out.println("Map 查小明：" + scores.get("小明"));
        System.out.println("Map 查小刚：" + scores.get("小刚"));
    }
}
