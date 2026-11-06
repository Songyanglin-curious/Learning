import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;

public class SetBackends {
    enum Day { MON, TUE }

    static String fieldType(Class<?> c, String name) throws Exception {
        Field f = c.getDeclaredField(name);
        return f.getType().getName();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("HashSet 内部字段 map       : " + fieldType(HashSet.class, "map"));
        System.out.println("TreeSet 内部字段 m         : " + fieldType(TreeSet.class, "m"));
        System.out.println("LinkedHashSet 的父类       : " + LinkedHashSet.class.getSuperclass().getName());
        System.out.println("CopyOnWriteArraySet 字段 al: " + fieldType(CopyOnWriteArraySet.class, "al"));

        Set<Day> es = EnumSet.of(Day.MON);
        System.out.println("EnumSet.of(MON) 的实际类型  : " + es.getClass().getName());

        System.out.println("java.util.Dictionary 存在吗 : " + (Class.forName("java.util.Dictionary") != null));
    }
}
