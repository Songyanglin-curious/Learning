import java.util.HashMap;
import java.util.Map;

public class MapForeachFail {
    public static void main(String[] args) {
        Map<String, Integer> map = new HashMap<>();
        map.put("小明", 92);
        for (String key : map) {
            System.out.println(key);
        }
    }
}
