# Java 集合

## 核心理解

集合是 JDK 自带的一批容器类，替我们做三件手工活：**变长、按值查找、去重与按键查询**。但真正的价值不在"实现了数据结构"——那自己也能写——而在**把所有数据结构的操作统一到一套接口**上：调用方只依赖能力，实现可以替换。所以学集合等于学两件事：接口约定了什么，每种实现要付什么代价。

全文代码是说明片段或已运行的示例，示例文件见文末"配套实验"。测量数据来自本机 JDK 17.0.18 与 .NET 10，数值只作量级参考。

## 从数组到集合

数组已经能装多个同类东西，但它有三个硬伤：

- **长度固定**：加第 4 个元素要新建数组、逐个搬、换引用。
- **只能按下标取**：想找"小红在第几个"就得自己写循环。
- **无法按别的方式组织**：去重、排序、按名字查，全都要手写。

集合就是把这三种重复劳动提前写好、测好、优化好。它只有四种基本形状：

| 形状 | 回答的问题 | 生活对应 |
| --- | --- | --- |
| `List` | 第几个是什么（有顺序、可重复） | 排队名单 |
| `Set` | 这个东西在不在（不重复） | 签到名单 |
| `Map` | 这个 key 对应什么 | 通讯录 |
| `Queue` / `Deque` | 按队形进出，两端操作 | 取号机 |

其余类都是"同一种形状的不同实现方式"。

## 接口族与各层职责

```
Iterable          → 只有 iterator()：能被 for-each 遍历
└ Collection      → add / remove / contains / size / isEmpty / toArray
  ├ List          → 有顺序 + 按下标访问（get/set/add(i)/indexOf/subList）+ ListIterator
  ├ Set           → 无新方法，只收紧契约：元素不许重复
  └ Queue         → offer / poll / peek（失败返回 null/false，另有抛异常版本）
    └ Deque       → 两端进出（offerFirst / pollLast / push / pop）

Map               → 独立成树，既不是 Collection 也不是 Iterable
```

三层要点：

- **每层只加一点能力**，所以 `Set` 一个方法都没加——它表达的是**约束**而非能力。反射看 `Set.class.getDeclaredMethods()` 会有 16 个方法，那全是把 `Collection` 的签名重声明一遍以书写契约文档，Java 9 之后才多出静态工厂 `of` / `copyOf`。
- **`AbstractCollection` / `AbstractList` 是骨架实现**：子类实现 `iterator()` 和 `size()`，就免费得到 `contains` / `toString` / `toArray` 等。它们存在的目的是让实现者只写差异部分。
- **`Map` 独立**：它没有"一堆元素"的语义，遍历时要先取视图——`entrySet()` / `keySet()` / `values()`，这三个是 `Collection`，所以能 for-each。直接 `for (K k : map)` 编译失败（"需要: 数组或 java.lang.Iterable，找到: Map"）。

**声明类型决定你能调什么方法，实际类型决定代价。** `List<String> list = new ArrayList<>()` 里，左边给能力，右边给性能。

## List 的两种实现

能力完全相同（都实现 `List`），差别只来自**东西存在哪**：`ArrayList` 内部是一个 `Object[]`，`LinkedList` 是一串散落在堆上的 `Node`。

实测（20 万条）：

| 操作 | ArrayList | LinkedList |
| --- | --- | --- |
| 随机 `get(i)` × 5000 | 0 ms | 1055 ms |
| 头部 `add(0, x)` × 20000 | 421 ms | 2 ms |
| 增强 `for` 遍历全部 | 5 ms | 15 ms |
| 中间 `add(n/2, x)` × 2000（10 万条） | 11 ms | 197 ms |
| 尾部 `add(x)` × 2000 | 0 ms | 0 ms |

结论：

- 下标访问：数组算地址 O(1)，链表只能走，O(n)。
- 头部插入：数组整体后移，链表改指针。
- 同样 O(n) 的遍历，数组仍快数倍——连续内存的缓存局部性，链表每个元素还多背对象头和两个引用。
- **“链表插入快”只在已持有节点位置（如迭代器当前位置）时成立**；按位置插入反而更慢，因为要先走过去。

**选型：默认 `ArrayList`；需要两端操作用 `ArrayDeque`，`LinkedList` 在实际代码里基本不用。** 真需要定长或装基本类型时用数组，`int[]` 远比 `List<Integer>` 省。

### 扩容

- `ArrayList`：默认构造后容量 0，首次 `add` 分配 10，之后按 1.5 倍增长。实测序列：`10 → 15 → 22 → 33 → 49`。
- 扩容要新建数组并复制，代价均摊 O(1)。
- 实测"预设容量"的收益远比传说中小：100 万次尾插，默认容量 29 ms、预设容量 26 ms——因为 1.5 倍增长下累计拷贝量约 2N，真正的大头是百万次自动装箱。它的实际价值是降低内存峰值与 GC 压力。

## 三种"只读"与视图

| 写法 | `set` | 能否看到外部变化 | 本质 |
| --- | --- | --- | --- |
| `Arrays.asList(arr)` | ✅ | ✅ 跟随原数组 | 数组适配器，固定**长度**，非只读 |
| `List.of(...)` | ❌ | ❌ 独立存储 | 真不可变，且拒 `null`（`NullPointerException`） |
| `Collections.unmodifiableList(x)` | ❌ | ✅ 跟随底层 | 装饰器视图 |
| `new ArrayList<>(x)` | ✅ | ❌ | 唯一真拷贝 |

- `Arrays.asList` 的 `add` / `remove` 抛 `UnsupportedOperationException`，因为数组不能变长；`set` 允许，改的是原数组。
- `unmodifiableList` 挡的是"通过这个引用写"，不挡底层被改——底层 `add` 之后视图立刻看到。
- `subList`、`keySet`、`entrySet`、`values` 都是**活视图**，不是拷贝。对原容器做结构性修改后再访问视图，抛 `ConcurrentModificationException`（`modCount` 版本对不上）。

实用规则：要"别改我的数据"用 `List.of` 或 `List.copyOf(x)`；只临时禁写用 `unmodifiableList`；要可改的独立副本用 `new ArrayList<>(x)`。

## Set 与 Map：同一台机器的两种投影

- `HashSet` 内部字段 `map` 的类型就是 `java.util.HashMap`，value 是一个静态占位对象；`set.add(x)` 等价于 `map.put(x, 占位)`。
- 反过来，`map.keySet()` 是 `HashMap$KeySet`，`map.entrySet()` 是 `HashMap$EntrySet`，都是 `Set`。
- 两者是**活视图双向耦合**：`map.put("c",3)` 后 `keySet` 立刻变成 `[a, b, c]`；`keySet.remove("a")` 后 `map` 变成 `{b=2, c=3}`；`keySet.add` 抛 `UnsupportedOperationException`。

数学关系两个方向都成立：`Set<E>` ≈ `Map<E, 占位>`；`Map<K,V>` ≈ `Set<Map.Entry<K,V>>`。所以分成两棵树是**抽象方向**的差异，不是数据结构的差异：

| | `Set` | `Map` |
| --- | --- | --- |
| 回答的问题 | 在不在 | 对应什么 |
| 唯一性 | 元素唯一 | **key** 唯一，value 可重复 |
| 读取 | `contains(x)` | `get(k)` |
| 写入返回 | `add` → `boolean` | `put` → **被覆盖的旧值** |
| 遍历 | `iterator()` | 只能借 `entrySet` / `keySet` / `values` |
| `equals` 语义 | 元素集合相同 | 每个 key 的 value 都相同 |

`Map` 不给迭代器的原因：一个条目是"两个东西"，接口无法回答 `for (X x : map)` 里的 X 是 key 还是 Entry，于是改给三个视图。

**唯一性的三层规则**：`List` 不检查；`Set` 按 `equals`（+ `hashCode`）判唯一；`Map` 只对 key 强制唯一。判等依据是**内容相等而非引用相同**——两个 `new String("小明")` 在 `HashSet` 里只留一个，第二次 `add` 返回 `false`。

"字典"有两种含义，别混：语义层（键→值）上 `Set` 不是字典，只是"只有键的字典"；结构层（哈希表）上 `HashSet` 确实与 `HashMap` 同源，但不能推广——**Set 的底层由实现决定**：

| 实现 | 内部字段类型 |
| --- | --- |
| `HashSet` | `HashMap`（哈希表） |
| `TreeSet` | `NavigableMap`（红黑树，有序） |
| `LinkedHashSet` | 继承 `HashSet`（哈希表 + 链表记插入序） |
| `CopyOnWriteArraySet` | `CopyOnWriteArrayList`（数组 + 写时复制） |
| `EnumSet` | `RegularEnumSet`（位图，一个 `long`） |

（`CopyOnWriteArraySet` 在 `java.util.concurrent` 包。另有个历史遗留类 `java.util.Dictionary`，是 `Hashtable` 的父类，已被 `Map` 取代。）

## equals 与 hashCode：隐式契约

`Object.equals` 的默认实现是 `this == obj`，即**引用相等**。集合的所有"按值查找"都调它：`contains` / `indexOf` / `remove(Object)`。

实测三态（`HashSet` 连续加两个"内容相同、引用不同"的对象）：

| 类的重写情况 | `list.contains(新对象)` | `HashSet.size()` |
| --- | --- | --- |
| 不重写 | `false` | 2 |
| 只重写 `equals` | `true` | **2** |
| 重写 `equals` + `hashCode` | `true` | 1 |

中间那行是关键：`HashSet` **先算 `hashCode` 定位桶，只在同一个桶里才比 `equals`**。`hashCode` 没重写，两个对象进不同桶，压根走不到 `equals`。

契约条文（`equals`）：自反、对称、传递、一致、与 `null` 不等。
契约条文（`hashCode`）：`equals` 相等的两个对象，`hashCode` 必须相等；反之不要求（允许撞桶）。

**两者是绑定的，编译器和接口都不会替你检查**——这是集合里最"沉默"的一处抽象。

## 哈希表如何工作：桶、散列、扩容

哈希表本体是一个数组，每格叫一个**桶**（bucket）。key 先算 `hashCode`，再压缩成下标：

```java
index = (hash ^ (hash >>> 16)) & (length - 1)
```

- 必须压缩：`hashCode` 是 32 位，有 42 亿种取值，不可能开这么大的数组。
- `& (length - 1)` 相当于取模，要求长度是 2 的幂，位运算更快。
- `hash ^ (hash >>> 16)` 是扰动，把高 16 位混进低位，缓解低位分布不均。

实测（本机 JDK 17）：

- 第一次 `put` 后桶数组长度 16；`"小明"` 的 `hashCode` 为 756703，扰动后 756692，落在下标 4。
- 撞桶是常态：`"key0"` 与 `"key12"` 都落在下标 3，该桶链表为 `[key0=1] -> [key12=2] -> null`。
- 负载因子 0.75：放入 12 个元素后表长仍 16，放第 13 个后变 32，所有元素重新算下标（rehash）。

查找三步：散列定位桶 → 桶内逐个比 `hash`（整数比较，快）→ 相同再比 `equals`。所以分工是：**`hashCode` 决定去哪找，`equals` 决定是不是它**。

- `equals` 相等但 `hashCode` 不同 → 分到不同桶 → 永远找不到对方 → 去重失效。
- `hashCode` 相同但 `equals` 不等 → 只是撞桶，性能略降，不算重复。

桶内 JDK 8 起用链表；单桶链表长度 ≥ 8 且表容量 ≥ 64 时转红黑树，防止被刻意构造的冲突拖成 O(n)。能预估大小时预设容量有意义：`new HashMap<>(n)` 会把容量向上取到 2 的幂。

## 与 C# 的对照

| 能力 | Java | C# |
| --- | --- | --- |
| 可变长数组 | `List` 接口 → `ArrayList` | `List<T>` 具体类 |
| 链表 | `LinkedList`（实现 `List` 和 `Deque`） | `LinkedList<T>`（**不是** `IList`，无索引器） |
| 去重集合 | `Set` → `HashSet` | `HashSet<T>` |
| 键值表 | `Map` → `HashMap` | `Dictionary<K,V>` |
| 有序集合 | `TreeMap` / `TreeSet` | `SortedDictionary` / `SortedSet` |
| 队列与栈 | `Deque` → `ArrayDeque` | `Queue<T>` / `Stack<T>`（无 Deque 概念） |
| 优先队列 | `PriorityQueue` | `PriorityQueue<TElement,TPriority>`（.NET 6+） |
| 并发映射 | `ConcurrentHashMap` | `ConcurrentDictionary` |

同机实测（20 万条）：

| 操作 | Java `ArrayList` | Java `LinkedList` | C# `List<T>` | C# `LinkedList<T>` |
| --- | --- | --- | --- | --- |
| 随机 `get(i)` × 5000 | 0 ms | 1055 ms | 0 ms | 2803 ms |
| 头部插入 × 20000 | 421 ms | 2 ms | 456 ms | 1 ms |
| 增强 `for` 遍历 | 5 ms | 15 ms | 1 ms | 3 ms |

公平性说明：C# 的 `LinkedList<T>` 没有索引器，对比时由手写循环每次从 `First` 走；Java 的 `get()` 会从较近一端走（平均 `size/4`），所以 1055 与 2803 的约 2.6 倍比值是 API 设计差异，不是性能差异。

设计哲学差异：

- **Java 强制面向接口**（`new ArrayList<>()` 却声明为 `List`）；C# 习惯直接用 `List<T>`，`IList<T>` 反而少用。C# 还有编译期的 `IReadOnlyList<T>`，Java 只有运行期包装或独立不可变实现。
- **泛型差异直接体现在集合上**：`List<T>` 对值类型无装箱，`List<Integer>` 每个元素都是对象，这就是遍历那行 1 ms 与 5 ms 的差距。
- **重载陷阱的消失**：Java 的 `remove(int)` / `remove(Object)` 歧义在 C# 不存在，因为用两个名字 `RemoveAt(int)` / `Remove(T)`。
- **抽象层次**：Java 在容器层做统一抽象（可替换、算法复用），C# 把统一抽象上移到查询层（`IEnumerable` + LINQ）；代价是 C# 缺少承载"通用增删算法"的抽象，而 Java 的接口不承载代价信息——正是这条造成 `LinkedList` 能当作 `List` 使用这一隐患。

## 常用陷阱清单

- `Arrays.asList` 是固定大小的数组视图；`List.of` 真不可变且拒 `null`；`unmodifiableList` 只是装饰器视图；只有 `new ArrayList<>(x)` 是拷贝。
- `subList` / `keySet` / `entrySet` / `values` 都是活视图，结构性修改后失效。
- `toArray()` 返回 `Object[]`，不能强转 `String[]`；用 `toArray(new String[0])` 或 `toArray(String[]::new)`。
- `list.remove(1)` 按下标；删元素要 `list.remove(Integer.valueOf(1))`。
- `ArrayList.contains` 是 O(n)。按值查找要靠 `Map` / `Set`。
- `LinkedList.get(i)` 是 O(n)，`for (int i...)` 遍历它等于 O(n²)。
- 不重写 `equals` / `hashCode`，`Set` 与 `Map` 的去重/查找全部失效。
- **对象一进哈希容器，参与 `hashCode` 的字段就不能再改**，否则在老桶里却按新散列找，永远找不到。
- `Map` 的 `get` 找不到返回 `null`，与"value 本身是 `null`"无法区分，需要判断存在时用 `containsKey`。
- 遍历哈希容器时不要改结构，否则 `ConcurrentModificationException`（`modCount` 尽力而为的检测，不是并发安全机制）。

## 学习进度

**已完成**（廖雪峰 8.1–8.4 主体，加一次 C# 横向对照）：

- 集合的存在理由与四种基本形状
- 接口族层次、各层职责、骨架实现
- `List` 两种实现与实测代价、扩容、三种只读与视图
- `Set` 与 `Map` 的同构关系、唯一性三层规则、各 Set 实现的底层
- `equals` / `hashCode` 契约与三态实测
- 哈希表的桶、散列、扰动、撞桶、扩容

**未完成**：

- 8.5 编写 `hashCode`：从契约推导一个正确实现（含 `Objects.hash`）
- 8.6 `EnumMap`：用数组实现，为什么对枚举特别快
- 8.7 `TreeMap` 与 `TreeSet`：靠 `Comparable` / `Comparator` 而非哈希；比较结果与 `equals` 不一致的后果；`null` key 的限制
- 8.9 `Set` 细节、`LinkedHashSet` 的插入序
- 8.10–8.15：`Queue`、`PriorityQueue`（堆）、`Deque`、`Stack`（历史包袱）、`Iterator`、`Collections` 工具类
- 8.8 `Properties`：留到 IO 章节
- 待补：`LinkedHashMap` 的 `accessOrder` 与 `removeEldestEntry`（LRU 雏形）、并发集合、Java 21 `SequencedCollection`（本机 JDK 17 无法实测）
- 待做实验：可变对象做 key 后修改字段的事故复现；`compareTo` 返回 0 而 `equals` 为 `false` 时同时进 `TreeSet` / `HashSet` 的差异

## 配套实验

全部位于 `Java/examples/02-collection-intro/`，均可直接编译运行（读私有字段的需 `--add-opens=java.base/java.util=ALL-UNNAMED`）：

| 文件 | 用途 |
| --- | --- |
| `Basics.java` | 数组、List、Set、Map 的最小对照 |
| `ListCompare.java` | `ArrayList` 与 `LinkedList` 三类操作对比 |
| `AbstractionTrap.java` | 同一个 `List` 签名，传不同实现耗时差两个数量级 |
| `InsertBench.java` | 100 万次插入；预设容量收益的实测 |
| `MiddleInsert.java` | 按位置插入：中间插入时 `LinkedList` 反而慢约 18 倍 |
| `GrowDemo.java` | `ArrayList` 容量增长序列（1.5 倍） |
| `ReadOnlyForms.java` | 三种"只读"与 `subList` 视图、`remove` 重载 |
| `MapSetDemo.java` | `Map` 非 `Iterable`、三个视图、`Set` 无新增方法 |
| `MapForeachFail.java` | `for (K k : map)` 的编译错误现场 |
| `EqualsDemo.java` | `equals` / `hashCode` 重写的三态对比 |
| `SetMapSymmetry.java` | `HashSet` 内部即 `HashMap`、活视图双向耦合 |
| `UniquenessDemo.java` | "元素唯一"按内容判等而非引用 |
| `SetBackends.java` | 四种 `Set` 实现的底层字段类型 |
| `BucketDemo.java` | 桶数组、散列下标、撞桶链表、扩容阈值 |

## 参考资料

- [廖雪峰：集合](https://liaoxuefeng.com/books/java/collection/index.html)：本次学习主线（8.1–8.15）。
- [Oracle：The Collections Framework](https://docs.oracle.com/javase/8/docs/technotes/guides/collections/index.html)：接口族与实现选择的官方说明。
- [Oracle：Object.equals](https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html#equals-java.lang.Object-) 与 [Object.hashCode](https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html#hashCode--)：契约原文。
- [Oracle：HashMap](https://docs.oracle.com/javase/8/docs/api/java/util/HashMap.html)：负载因子、容量、树化阈值的官方描述。
- [Microsoft：Collections and Data Structures](https://learn.microsoft.com/dotnet/standard/collections/)：C# 集合选型对照。
