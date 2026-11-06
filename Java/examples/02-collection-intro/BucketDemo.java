import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class BucketDemo {
    static Object[] table(Map<?, ?> m) throws Exception {
        Field f = HashMap.class.getDeclaredField("table");
        f.setAccessible(true);
        return (Object[]) f.get(m);
    }

    static int spread(Object key) {
        int h = key.hashCode();
        return h ^ (h >>> 16);
    }

    static int index(Object key, int len) {
        return spread(key) & (len - 1);
    }

    static Object field(Object node, String name) throws Exception {
        Field f = Class.forName("java.util.HashMap$Node").getDeclaredField(name);
        f.setAccessible(true);
        return f.get(node);
    }

    public static void main(String[] args) throws Exception {
        Map<String, Integer> m = new HashMap<>();
        m.put("小明", 92);
        Object[] tab = table(m);
        System.out.println("第一次 put 之后，桶数组长度 = " + tab.length + "（下标范围 0 ~ " + (tab.length - 1) + "）");
        System.out.println("「小明」的 hashCode 原始值 = " + "小明".hashCode());
        System.out.println("  右移 16 位异或后的散列值 = " + spread("小明"));
        System.out.println("  落在下标 = " + index("小明", tab.length) + "  （即 散列值 & (长度-1)）");
        System.out.println();

        // 找两个撞进同一桶的 key
        Map<Integer, String> firstSeen = new HashMap<>();
        String k1 = null, k2 = null;
        for (int i = 0; i < 100000 && k2 == null; i++) {
            String s = "key" + i;
            int idx = index(s, 16);
            if (firstSeen.containsKey(idx)) { k1 = firstSeen.get(idx); k2 = s; }
            else firstSeen.put(idx, s);
        }
        Map<String, Integer> collide = new HashMap<>();
        collide.put(k1, 1);
        collide.put(k2, 2);
        Object[] tab2 = table(collide);
        int idx = index(k1, tab2.length);
        System.out.println("找到一对撞桶的 key：\"" + k1 + "\" 与 \"" + k2 + "\"");
        System.out.println("  它们都落在下标 " + idx + "，桶数组长度 " + tab2.length);
        System.out.print("  该桶里的链表：");
        Object node = tab2[idx];
        while (node != null) {
            System.out.print("[" + field(node, "key") + "=" + field(node, "value") + "] -> ");
            node = field(node, "next");
        }
        System.out.println("null");
        System.out.println();

        // 扩容：默认阈值 0.75 * 16 = 12
        Map<String, Integer> grow = new HashMap<>();
        for (int i = 0; i < 12; i++) grow.put("k" + i, i);
        System.out.println("放入 12 个元素后，桶数组长度 = " + table(grow).length);
        grow.put("k12", 12);
        System.out.println("放入第 13 个元素后，桶数组长度 = " + table(grow).length + "（翻倍）");
    }
}
