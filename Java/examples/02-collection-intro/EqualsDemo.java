import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class EqualsDemo {
    // 1）什么都不重写：用 Object 的 equals（即 == 引用相等）
    static class Plain {
        final String name;
        Plain(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    // 2）只重写 equals
    static class EqualsOnly {
        final String name;
        EqualsOnly(String name) { this.name = name; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EqualsOnly)) return false;
            return name.equals(((EqualsOnly) o).name);
        }
        @Override public String toString() { return name; }
    }

    // 3）equals + hashCode 都重写
    static class Both {
        final String name;
        Both(String name) { this.name = name; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Both)) return false;
            return name.equals(((Both) o).name);
        }
        @Override public int hashCode() { return Objects.hash(name); }
        @Override public String toString() { return name; }
    }

    static void report(String label, List<?> list, Set<?> set) {
        System.out.println(label);
        System.out.println("  list.contains(内容相同的新对象) = " + list.contains(list.get(0).getClass().cast(list.get(0)) == null ? null : cloneFirst(list)));
        System.out.println("  set.size() = " + set.size() + "  " + set);
    }

    static Object cloneFirst(List<?> list) { return null; }

    public static void main(String[] args) {
        System.out.println("== 1. 不重写 equals（默认 = 引用相等） ==");
        List<Plain> l1 = new ArrayList<>(List.of(new Plain("小明")));
        System.out.println("  list.contains(new Plain(\"小明\")) = " + l1.contains(new Plain("小明")));
        Set<Plain> s1 = new HashSet<>();
        s1.add(new Plain("小明"));
        s1.add(new Plain("小明"));
        System.out.println("  HashSet 加两次相同内容，size = " + s1.size() + "  " + s1);

        System.out.println("== 2. 只重写 equals，不重写 hashCode ==");
        List<EqualsOnly> l2 = new ArrayList<>(List.of(new EqualsOnly("小明")));
        System.out.println("  list.contains(new EqualsOnly(\"小明\")) = " + l2.contains(new EqualsOnly("小明")));
        Set<EqualsOnly> s2 = new HashSet<>();
        s2.add(new EqualsOnly("小明"));
        s2.add(new EqualsOnly("小明"));
        System.out.println("  HashSet 加两次相同内容，size = " + s2.size() + "  " + s2);

        System.out.println("== 3. equals 与 hashCode 都重写 ==");
        List<Both> l3 = new ArrayList<>(List.of(new Both("小明")));
        System.out.println("  list.contains(new Both(\"小明\")) = " + l3.contains(new Both("小明")));
        Set<Both> s3 = new HashSet<>();
        s3.add(new Both("小明"));
        s3.add(new Both("小明"));
        System.out.println("  HashSet 加两次相同内容，size = " + s3.size() + "  " + s3);
    }
}
