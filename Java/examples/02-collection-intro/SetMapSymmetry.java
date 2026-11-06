import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SetMapSymmetry {
    public static void main(String[] args) throws Exception {
        // 1) HashSet 内部就是一个 HashMap
        Field f = HashSet.class.getDeclaredField("map");
        System.out.println("HashSet 的内部字段: " + f.getName() + " : " + f.getType().getName());
        Field p = HashSet.class.getDeclaredField("PRESENT");
        p.setAccessible(true);
        System.out.println("HashSet 塞进 map 的 value 是个占位对象: " + (p.get(null) != null));

        // 2) 反过来，Map 的 entrySet / keySet 是 Set
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Set<String> keys = map.keySet();
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        System.out.println("map.keySet() 的类型: " + keys.getClass().getName());
        System.out.println("map.entrySet() 的类型: " + entries.getClass().getName());

        // 3) 它们是同一个东西的两种视图：改一边，另一边跟着变
        System.out.println("改 map 前 keySet = " + keys);
        map.put("c", 3);
        System.out.println("map.put(\"c\",3) 后 keySet = " + keys);

        keys.remove("a");
        System.out.println("keySet.remove(\"a\") 后 map = " + map);

        try {
            keys.add("d");
        } catch (Exception e) {
            System.out.println("keySet.add(\"d\") -> " + e.getClass().getSimpleName());
        }

        // 4) 写入返回值的差异（这不是数据结构差异，是契约差异）
        Set<String> set = new HashSet<>();
        System.out.println("set.add(\"x\") 第一次返回: " + set.add("x"));
        System.out.println("set.add(\"x\") 第二次返回: " + set.add("x"));

        Map<String, Integer> m2 = new HashMap<>();
        System.out.println("m2.put(\"x\",1) 第一次返回: " + m2.put("x", 1));
        System.out.println("m2.put(\"x\",2) 第二次返回: " + m2.put("x", 2) + "（返回的是被覆盖的旧值）");
    }
}
