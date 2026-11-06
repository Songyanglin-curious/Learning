import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapSetDemo {
    public static void main(String[] args) {
        Set<String> set = new HashSet<>(List.of("小明", "小红"));
        for (String s : set) System.out.println("Set 能 for-each  : " + s);

        Map<String, Integer> map = new HashMap<>();
        map.put("小明", 92);
        map.put("小红", 88);
        for (Map.Entry<String, Integer> e : map.entrySet())
            System.out.println("Map 用 entrySet  : " + e.getKey() + " = " + e.getValue());
        for (String k : map.keySet()) System.out.println("Map 用 keySet    : " + k);
        for (Integer v : map.values()) System.out.println("Map 用 values    : " + v);

        System.out.println("set instanceof Iterable : " + (set instanceof Iterable));
        System.out.println("map instanceof Iterable : " + (map instanceof Iterable));
        System.out.println("set instanceof Collection : " + (set instanceof java.util.Collection));

        System.out.println("--- java.util.Set 自己声明的实例方法 ---");
        int count = 0;
        for (Method m : Set.class.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            System.out.println("  " + m.getName());
            count++;
        }
        System.out.println("  （实例方法共 " + count + " 个）");
        System.out.println("--- Set 的静态工厂 ---");
        for (Method m : Set.class.getDeclaredMethods())
            if (Modifier.isStatic(m.getModifiers())) System.out.println("  " + m.getName());

        List<String> of = List.of("a");
        try { of.set(0, "b"); } catch (Exception e) {
            System.out.println("List.of(\"a\").set(0,\"b\") -> " + e.getClass().getSimpleName());
        }
    }
}
