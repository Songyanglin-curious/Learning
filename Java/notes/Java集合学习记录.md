# Java 集合学习记录

更新日期：2026-11-06。

本文件系统总结四轮 Java 集合学习，保留学习范围、核心结论、例子、理解反馈与后续边界。四轮核心内容已讨论完，当前以能够理解、判断和日常使用为阶段目标，集合基础暂告一段落。现有的《Java集合》笔记作为参考材料，其篇幅及原有“已完成”条目不代表本轮学习进度；恢复学习时以本文件为准。

## 学习方式

前三轮围绕同一组集合推进：**先建立全貌，再理解行为，最后用原理解释行为**；第四轮补齐专用容器与工具的用途和边界。不要求第一次就把某个集合彻底研究完，也不把集合学习扩展成完整的数据结构课程。

- 已有 C# 基础，共通的数据结构、判等和哈希概念快速带过，重点学习 Java 的具体规则、行为边界和容易误判之处。
- 每次只推进一个小问题，先说明它接在哪个已有认识上；不固定安排简单预测题。
- 用“操作前有哪些对象 → 执行什么 → 操作后发生什么”解释行为，不只给“浅拷贝”“活视图”等标签。
- 只有结果不容易凭直觉判断或存在疑问时，才做最小实验；说明代码本身不作为运行证据。
- 理解稳定后再整理笔记。教程用于检查知识覆盖，旧笔记用于参考，都不是必须逐条消化的清单。
- 暂时用不到的分支记为待办；同一个疑问最多深入两轮，仍未解决时记录问题和后续步骤。
- 用户已经确认，从引用关系理解对象、容器和视图最清楚；后续先明确“共享什么、操作依赖什么数据”，再给术语。讲解可以直接指出机制，不需要营造“魔法”感。

## 四轮总览与选型入口

| 轮次 | 回答的问题 | 本轮形成的认识 |
| --- | --- | --- |
| 第一轮：能力与选择 | 面对需求，选择哪一类容器？ | List 表达位置与重复记录，Set 表达成员，Map 表达映射，Queue / Deque 表达处理次序 |
| 第二轮：行为与边界 | 它实际保证什么，哪些操作会影响谁？ | 判等、顺序、修改能力、共享引用和遍历期间修改都需要明确规则 |
| 第三轮：机制与代价 | 为什么有这些行为，选择它需要付什么成本？ | 数组、链表、哈希、搜索树与堆解释定位、修改、排序和优先处理的取舍 |
| 第四轮：专用能力与工具 | 特定需求有没有更合适的现成工具？ | EnumMap / EnumSet 处理枚举映射与成员，Properties 处理简单配置，Collections 提供通用操作 |

### 接口、实现与底层结构分开看

List、Set、Map 描述容器的行为约定；数组、链表、哈希表、树描述底层组织方式，不能把这两个层次混为一谈。

| 保存内容 | 常见实现 | 内部组织与行为重点 |
| --- | --- | --- |
| 有位置、允许重复的元素序列 | ArrayList / LinkedList | 分别借助数组 / 双向链表，定位和修改的成本不同 |
| 不重复的成员 | HashSet / TreeSet | 分别借助哈希 / 有序树，判重和遍历顺序不同 |
| key → value 的映射 | HashMap / TreeMap | 分别借助哈希 / 有序树，按 key 定位和排序的方式不同 |
| 双端放入和取出 | ArrayDeque | 数组实现的双端队列 |
| 每次取出当前最优先项 | PriorityQueue | 数组存储的二叉堆，不维护全体元素的完整排序 |

`List<Student> students = new ArrayList<>();` 中，声明类型表达调用代码依赖的能力，实际对象决定具体行为和操作成本。面向接口声明是常见设计方式，C# 也有同样做法，并非 Java 强制要求；接口中存在某个方法，也不表示每个实例都支持该修改操作。

Map 与 Collection 是集合框架中不同的接口体系，Map 不是 Collection 的子接口；List、Set、Queue 属于 Collection 体系。这里的 Collections 则是另一个静态工具类。

### 日常选型的判断顺序

1. 先明确保存的是记录、成员、映射，还是待处理项。
2. 确认是否允许重复，以及使用哪种判等或比较规则。
3. 确认需要插入顺序、排序顺序，还是只需要取出最优先项。
4. 确认主要操作是按位置读取、按 key 查找、顺序遍历，还是在特定位置修改。
5. 确认交出去的是原容器、副本还是视图，接收方能否修改容器和元素对象。
6. 最后结合实现的时间、内存和使用限制做选择；线程安全有要求时另行选择合适方案。

## 四轮学习范围

### 第一轮：知道有哪些能力，面对需求能做选择

用“学生报名”场景建立集合地图：

| 需求 | 集合能力 | 第一轮重点 |
| --- | --- | --- |
| 保存每次报名记录 | `List` | 有位置，允许重复 |
| 保存已经报名的学号 | `Set` | 成员是否存在，不重复 |
| 根据学号查学生资料 | `Map` | key 与 value 的对应关系 |
| 保存待审核的报名 | `Queue` / `Deque` | 按处理规则放入、取出，必要时操作两端 |

顺带区分接口与实现，例如 `List` 与 `ArrayList`，不展开完整继承树。达到能解释“为什么选它”即可。

### 第二轮：理解集合实际承诺了什么

围绕第一轮的需求讨论：

- 两个学生对象编号相同，算不算重复：`equals` 与哈希集合的 `hashCode` 契约。
- “有顺序”指插入顺序还是自动排序：不同顺序要求与比较规则。
- 把列表交给别人，怎样限制修改：副本、视图、不可修改集合，以及元素本身能否修改。
- 遍历期间增删元素：迭代器及修改边界。

达到能预测主要行为、解释常见陷阱即可。共通概念简短确认，把讨论重点放在 Java 的具体表现上。

### 第三轮：理解能力的代价和来源

有了前两轮的问题，再解释实现与取舍：

- `ArrayList`：数组如何支持按下标访问，扩容和插入的代价。
- `LinkedList`：定位和修改如何发生，“插入快”成立的前提。
- `HashMap` / `HashSet`：哈希如何缩小查找范围，判等契约和 key 的稳定性为什么重要。
- `TreeMap` / `TreeSet`、`PriorityQueue`：排序与优先处理需要怎样的组织方式。

达到能说明“为什么这样选，以及要付什么代价”即可。精确扩容倍数、扰动公式、树化阈值和性能微测留到有具体疑问时研究。

### 第四轮：补齐用途与边界

已按用途讨论 `EnumMap`、`EnumSet`、`Properties`、`Collections`，不为每个类安排同等篇幅。目标是能判断“它解决什么问题、何时需要、有什么限制”；用户询问 Collections 方法时，再按用途给出查阅入口。

教程用于检查知识覆盖，旧笔记中的高级主题和实验不自动成为必做任务。具体 API、并发集合、LRU 实现等是否深入，按实际疑问决定。

## 第一、二轮总结：能力选择与行为边界

### 学生报名场景中的选择

系统依次收到 S01、S02、S01 三次报名：

- `List` 保存三条提交记录，重复报名也有记录价值；下标从 0 开始。逐条追加时保留提交顺序，不会自动按学号排序。
- `Set<String>` 保存学号成员，只保留 S01、S02；可用 `contains("S01")` 判断是否报名。不凭 `Set` 接口推断遍历顺序。
- `Map<String, Student>` 保存“学号 → 学生资料”。key 唯一，value 可以重复；对同一个 key 再次 `put` 会替换对应值，不会保留该 key 的提交历史。
- 已有“学号 → 资料”的 `Map` 时，可以用 `containsKey` 判断是否报名，不必只为查重再维护一个 `Set`。
- 即使允许 value 为 `null`，也能用 `containsKey` 判断 key 是否存在；只有用 `get(key) == null` 判断时，才会混淆“key 不存在”和“value 为 null”。

接口与实现的关系：

```java
List<Student> records = new ArrayList<>();
Set<String> registeredIds = new HashSet<>();
Map<String, Student> studentsById = new HashMap<>();
```

左侧声明可使用哪类接口能力，右侧创建具体实现。第一行只创建一个 `ArrayList` 对象；面向接口声明是可选的设计方式，不是 Java 的强制语法。

### Queue 与 Deque 的用途

```java
Queue<Student> pending = new ArrayDeque<>();
pending.offer(zhangSan); // 加入待审核队列
pending.offer(liSi);
Student next = pending.poll(); // 取出并移除张三；为空时返回 null
```

这里使用 `ArrayDeque` 按先进先出处理。但 `Queue` 不保证所有实现都先进先出，`PriorityQueue` 根据优先级决定下一个取出的元素。

```java
Deque<Student> urgent = new ArrayDeque<>();
urgent.offerLast(zhangSan); // 普通报名放末尾
urgent.offerFirst(liSi); // 加急报名放最前面
Student first = urgent.pollFirst(); // 从最前面取出李四
```

`Deque` 支持操作两端；在同一端放入和取出，也可以表达后进先出的栈行为。

### 去重依赖明确的相等规则

假设 `Student` 是普通类，没有重写 `equals` / `hashCode`。分别创建两个对象，即使学号和姓名相同，默认 `equals` 仍认为它们不相等，加入 `HashSet` 会保留两个成员。

如果业务规定“学号相同就是同一个学生”，可以定义按学号判等的 `equals`，并配套正确的 `hashCode`。此后第二个相等对象加入 `HashSet` 时不会新增成员，也不会替换先加入的对象。

对象判等不会把两个对象合并成一个；它只是定义它们何时算相等。只需要按学号查重时，直接用 `Set<String>` 更直接。

哈希查找的行为边界见本轮后文，桶、散列定位和扩容机制见第三轮总结。

### List 类型不保证实例支持所有修改操作

```java
List<String> a = new ArrayList<>(List.of("张三", "李四"));
List<String> b = Arrays.asList("张三", "李四");
List<String> c = List.of("张三", "李四");
```

| 操作 | `a`：ArrayList | `b`：Arrays.asList | `c`：List.of |
| --- | --- | --- | --- |
| `get(0)` | 可以 | 可以 | 可以 |
| `set(0, "王五")` | 可以 | 可以 | 抛异常 |
| `add("赵六")` | 可以 | 抛异常 | 抛异常 |
| `remove(0)` | 可以 | 抛异常 | 抛异常 |

`List` 中部分修改方法属于可选操作。代码通过编译，只说明接口包含该方法；不支持该操作的实例会在运行时抛出 `UnsupportedOperationException`。

- `ArrayList` 可调整元素数量，也可替换元素。
- `Arrays.asList` 返回固定大小、由数组支撑的列表，可替换元素，不能增删。
- `List.of` 返回不可修改列表，不能增删或替换元素。“不可修改列表”不意味着元素对象本身不可变。

### 数组视图与容器副本

```java
String[] source = {"张三", "李四"};
List<String> view = Arrays.asList(source);
List<String> copy = new ArrayList<>(view);

source[0] = "王五";
```

此时：

| 表达式 | 结果 |
| --- | --- |
| `source[0]` | 王五 |
| `view.get(0)` | 王五 |
| `copy.get(0)` | 张三 |

`source` 和 `view` 使用同一个数组作为元素存储；`copy` 有自己的元素存储，创建时复制了原有元素引用。

反过来执行 `view.set(1, "赵六")`，`source[1]` 也会变为“赵六”。因此：

- `Arrays.asList(source)`：通过 `List` 操作原数组。
- `new ArrayList<>(view)`：建立能独立增删、替换元素的列表。

这里的独立只针对容器中的位置和元素引用。保存可修改的 `Student` 对象时，建立新列表不会自动创建一批新学生；这一层已继续讨论并通过引用题，见下节。

### 容器独立与元素对象共享

以下讨论中的 `Student` 是可修改的普通类：

```java
Student student = new Student("张三");
List<Student> original = new ArrayList<>();
original.add(student);
List<Student> copy = new ArrayList<>(original);
```

此时有两个列表对象，但只有一个学生对象；两个列表的第 0 个位置保存了指向同一学生的引用。

| 操作 | 修改了什么 | 另一列表是否受影响 |
| --- | --- | --- |
| `copy.get(0).setName("李四")` | 共享学生对象的姓名 | 两边读取这个学生，都会看到李四 |
| `copy.set(0, new Student("王五"))` | copy 第 0 个位置保存的引用 | original 仍指向原学生 |
| 对 copy 增删或清空 | copy 的元素引用序列 | 不改变 original 的元素引用序列 |

用户已经理解：区分“容器 → 元素引用 → 元素对象”，可以直接推导上述行为。复制容器和其中的元素引用通常叫浅拷贝；若元素对象也需要独立，必须另外创建对象，并继续考虑其内部引用的对象是否需要复制。

C# 中 `Student` 为 `class` 时行为相同；若为 `struct`，元素按值复制，不能直接套用 class 的结论，但结构体内的引用字段仍可能指向共享对象。

### 实际对象类型与变量声明类型

这是学习中的旁支，已确认理解，不需要在新窗口重新从头讲：

```java
B b = new A();
```

前提是 A 继承 B 或实现接口 B。对象实际是 A，不会因赋值或转型变成另一种对象；变量声明为 B，编译器按 B 检查允许调用的方法。对被重写的实例方法，运行时执行实际对象的实现。

向上转型可隐式进行；向下转型需检查实际对象是否兼容，强制转换不会改造对象。普通引用兼容与泛型兼容要分开，`A` 可作为 `B` 使用不代表 Java 的 `List<A>` 能赋给 `List<B>`；后者涉及不变性、协变与逆变。

### List.of、不可修改视图与 List.copyOf

| 写法 | 作用 | 原集合后续增删、替换是否影响结果 | null 元素 |
| --- | --- | --- | --- |
| `List.of(...)` | 用给出的元素组成不可修改列表 | 不依赖一个可修改的原列表 | 不接受 |
| `Collections.unmodifiableList(source)` | 提供原列表的不可修改视图 | 会，读取的是原列表当前内容 | 可以保留原列表中的 null |
| `List.copyOf(source)` | 保留原集合当时的元素引用序列，得到不可修改列表 | 不会 | 不接受 |

三者都不允许通过结果列表增删、替换元素；都不承诺冻结或复制元素对象。`of` 是静态工厂方法名称，不是 Java 关键字。

在 source 为普通 `ArrayList` 的例子中，source 与其不可修改视图是不同对象；视图包装 source，把读取操作交给它，不是两个变量直接指向同一个列表实例。

`List.copyOf` 不保证每次新建实例，输入为合适的不可修改列表时可以复用。应依赖行为保证，而不是假设必然发生对象复制。

```java
Student s = new Student("张三");
List<Student> source = new ArrayList<>(List.of(s));
List<Student> view = Collections.unmodifiableList(source);
List<Student> snapshot = List.copyOf(source);

source.set(0, new Student("李四"));
s.setName("王五");
```

最终 source、view 都读到李四，snapshot 仍引用最初的 s，所以读到王五。此题用户独立答对。

snapshot 固定的是元素引用序列，元素对象仍可能变化。不要用“数据库与查询结果自动同步”作类比；这里变化可见的原因是内存中的共享对象。

### 三种顺序与比较规则

| 要求 | 含义与代表 |
| --- | --- |
| 插入顺序 | 谁先放入谁在前；按到达顺序追加的 List、LinkedHashSet、默认配置的 LinkedHashMap |
| 排序顺序 | 按比较规则排列；TreeSet 按元素、TreeMap 按 key 维持顺序；List.sort 只对调用时的列表排序，不保证后续插入仍有序 |
| 优先处理 | 每次取出当前最优先的项；PriorityQueue，按其比较规则最小的元素在队头 |

HashSet、HashMap 不承诺遍历顺序。PriorityQueue 不要求输入已排序，也不保证遍历或直接打印的顺序是排序结果；连续 poll 才按优先规则取出，同优先级不保证插入先后。

Comparator 与 JavaScript sort 的比较函数可对应理解：负数表示前者靠前，0 表示按该规则相同，正数表示前者靠后。

```java
Comparator<Student> rule =
        Comparator.comparingInt(Student::getPriority)
                  .thenComparing(Student::getId);
```

含义是先按优先级比较，相同时再按学号比较。

| 比较结果为 0 | 集合如何处理 |
| --- | --- |
| List.sort | 都保留；稳定排序保持原有相对顺序 |
| PriorityQueue | 都保留；取出先后不保证 |
| TreeSet | 视为重复，不加入后来的元素，即使 equals 为 false |

不能把 TreeSet 理解成先排序、再用 equals 去重；比较规则直接参与其唯一性判断。“以优先级为 key”可以类比去重效果，但实际存储的仍是学生对象。

### 遍历期间删除元素

统一判断入口：谁管理下一个位置？删除后元素怎样移动，遍历进度是否相应调整？

按下标正向遍历 `[A, B, B, C]`，遇 B 执行 `remove(i)`，随后仍执行 `i++`：第一个 B 删除后，第二个 B 前移到当前下标，随后被跳过，最终剩 `[A, B, C]`。此题用户独立答对。

对按下标访问的列表，倒序检查并删除当前元素时，前方未检查元素的位置不变，因此可以避免这种漏项；不能推广成“任意修改只要倒序就安全”。

ArrayList 的 for-each 使用迭代器。直接通过列表增删结构，可能使迭代器抛 `ConcurrentModificationException`；单线程也会发生，检测是尽力而为，没抛异常不代表操作可靠。C# List 的 foreach 也有枚举期间修改检测。

```java
Iterator<String> it = ids.iterator();
while (it.hasNext()) {
    String id = it.next();
    if (id.equals("S01")) {
        it.remove(); // 通过迭代器删除刚返回的元素，协调遍历进度
    }
}
```

以上针对支持迭代器删除的 ArrayList；每次 next 后最多 remove 一次。单纯删除符合条件的元素，可用 `ids.removeIf(id -> id.equals("S01"))`。

### 哈希查找、对象身份与字段稳定性

HashSet / HashMap 不会先遍历所有元素确认是不是同一个引用，而是先根据哈希值寻找候选，再通过对象身份或 equals 确认目标。

- equals 相等的对象必须有相同的 hashCode。
- hashCode 相同不代表 equals 相等。
- 作为哈希 key，或作为 HashSet 成员时，不应修改影响 equals、hashCode 的字段；修改对象不会让集合自动重新安排条目。

例如 Student 按学号判等并计算哈希值：加入后仅改姓名，不影响查找；改学号则可能查不到，即便查找参数仍引用同一个实例。对象仍在集合中，与按当前身份规则能否查到它，是两回事。

用不可变学号字符串作为 Map 的 key、Student 作为 value，可把资料修改与 key 的稳定性分开。但 Student 中的学号字段变化，不会自动替换 Map 中原来的字符串 key，需要程序维护对应关系。

理解入口：要看操作真正使用什么数据；同一实例的身份，与参与判等、哈希、比较的字段值，需要分开判断。第三轮已用桶定位与条目存放机制解释这一行为。

## 第三轮总结：机制、成本与选择

### ArrayList：引用数组与按位置访问

ArrayList 内部用数组保存元素引用，列表下标对应数组中的位置。`get(i)` 检查范围后可直接读取相应槽位，不需要先经过前面的元素，因此按下标访问是 O(1)。

- 中间插入、删除需要移动后续元素引用，成本随需要移动的数量增加。
- 尾部追加通常不移动已有元素；容量不足时要分配新数组并复制引用，因此追加是均摊 O(1)，单次扩容仍有成本。
- 数组中连续组织的是引用槽位，Student 等元素对象本身不保证相邻。顺序读取引用有利于缓存局部性，但访问对象字段仍可能跳到其他位置。
- 连续组织通常能降低实际访问成本；具体加速幅度取决于数据和访问方式，不能笼统称为“指数级提升”。

读多写少是常见选择依据，但不是固定的业务事实；追加、批量处理、预留容量等也会影响写入成本。不能只按“增删多还是查询多”机械选型。

### LinkedList：已知位置的修改，与寻找位置分开

LinkedList 用双向连接的节点保存元素引用。已经定位到相关节点时，插入、删除可以通过调整局部连接完成，不需要移动整个后续序列。但按下标操作，要先从较近的一端逐步定位。

Java LinkedList 不把内部节点作为公开操作对象。持有某个 Student，不等于持有它对应的节点；`remove(student)` 仍要搜索。已经定位的 ListIterator 适合在当前位置进行局部增删。

“链表插入快”成立的前提是定位问题已经解决。作为普通容器，它还要承担节点对象、前后引用、分配与回收等开销，沿节点读取也有数据依赖和局部性限制。数组预留容量同样有内存成本，需要结合场景比较，不能断言链表总比数组慢。

链表在实际软件中有用途，尤其适合已经能找到对象、又需要频繁摘除和重新连接的场景。例如哈希表定位条目、双向链表维护访问顺序，可以用于 LRU 缓存；LinkedHashMap 的访问顺序模式提供了相关能力。Linux 内核也提供把连接字段嵌入对象结构体的链表实现。这里只理解用途，未设计或实现缓存、内核链表。

### HashMap：先定位桶，再确认 key

HashMap 通过 key 的 hashCode、内部哈希处理及桶数组长度确定查找位置，再在相关候选条目中检查哈希信息以及对象身份或 equals。

- 哈希缩小范围，判等确认目标。不同 key 可以落在同一个桶，落桶相同甚至不要求 hashCode 相同。
- equals 相等的 key 必须具有相同的 hashCode；hashCode 相同不表示 key 相等。
- 哈希值在相关状态不变时是确定的。“均匀”指一批 key 的分布效果，不是每次随机选桶。
- 分布受 hashCode 算法、HashMap 的映射方式和实际 key 数据共同影响；分散良好时，基本查找、插入通常可以按期望 O(1) 理解，不能把它当作任意输入下的保证。
- 所有对象都返回同一个 hashCode，可以满足相等对象哈希相同的必要约束，却会失去分桶缩小查找范围的优势。

**可变 key 为什么可能查不到**：条目放入时按当时的哈希信息存放；修改 key 对象不会自动重定位条目，也不会更新已保存的哈希信息。之后的查找使用当前 key 的哈希信息，可能走到另一个桶；即使桶位置碰巧相同，新旧哈希信息不一致也可能阻止匹配。“是不是同一个对象”的检查不能绕过这些条件。

这比“对象位置没变”更准确：未自动调整的是集合条目的组织及保存的哈希信息，不是在讨论学生对象的实际内存地址。

**扩容的成本**：元素增多时扩大桶数组，并根据已保存的哈希信息重新组织原有条目；这不只是追加空桶，也不需要重新调用所有 key 的 hashCode。扩容能缓解桶数量不足，不能解决所有 key 哈希相同的问题。精确阈值、扰动公式和树化细节未作为本轮学习要求。

### HashSet：复用 HashMap 的 key 唯一性

HashSet 内部借助 HashMap：成员作为 key，value 使用统一的内部占位对象。因此成员定位、判等与可变字段问题沿用哈希 key 的规则。

先加入 a，再加入与 a 相等的另一个对象 b：第一次新增成员；第二次返回 false，既不新增成员，也不替换原来保存的 a。

原因是 HashMap 对已有相等 key 的 put 更新 value，保留原 key 对象；HashSet 的 value 又只是同一个占位对象。因此不能把“Map 替换 value”误解成“Set 用新成员覆盖旧成员”。这段说明针对 HashSet / HashMap 的具体实现关系，不把全部 Map 实现都规定为同一种内部策略。

### 逻辑结构与内存组织是两个层次

Set 接口没有规定使用数组、链表还是树；树的逻辑关系也不必通过节点指针实现。

| 具体实现 | 内部组织 | 应记住的边界 |
| --- | --- | --- |
| ArrayList | 元素引用数组 | 引用槽位连续，不表示元素对象连续 |
| HashMap / HashSet | 桶数组及条目节点；桶内可能是链表或树 | 数组负责定位，节点和元素对象不保证相邻，整体不能简单归类为纯数组或纯链表 |
| TreeMap / TreeSet | 左右孩子、父节点等引用连接的红黑树节点 | 属于通过节点关系实现的搜索树 |
| PriorityQueue | 数组存储的二叉堆 | 用下标关系表达父子关系，不需要为每个元素保存孩子引用 |

上表描述本轮涉及的 Java 实现；不能据此推断所有语言、所有树或所有 Set 都采用同样表示。

### TreeMap / TreeSet：维持可搜索的排序关系

TreeMap 用红黑树组织条目，按 key 的自然顺序或 Comparator 比较。较小、较大的 key 分布在相应子树；一次比较可以排除另一侧分支。平衡机制避免退化成长链，基本查找、插入、删除为 O(log n)。按中序遍历可得到排序顺序，不需要每次插入后重新排序整个集合。

TreeSet 借助 TreeMap，把成员作为 key。Map / Set 决定保存映射还是成员；Hash / Tree 决定组织、定位和顺序特征。

比较结果为 0，TreeMap 视为同一个 key，TreeSet 视为同一个成员。TreeSet 不会再另用 equals 去决定是否保留两个比较为 0 的对象。比较规则应与 equals 一致，才能遵守 Map / Set 的通用相等契约；即使不一致，树仍按比较结果运行，可能与其他集合的相等语义冲突。

本轮理解搜索路径、排序来源和基本成本即可；红黑树旋转、染色与源码调整算法按需深入。

### PriorityQueue：局部堆顺序支持下一项优先

PriorityQueue 内部用数组存储二叉堆。按照比较器，父节点不大于其孩子，保证根节点是当前最小项；兄弟节点和数组整体不要求排序。

例如 `[1, 3, 2]` 是合法的小顶堆布局，却不是有序数组。读取队头为 O(1)；插入、取出需要进行 O(log n) 级别的堆调整，数组扩容另有成本。取出根后重新调整，使剩余项中的最小值来到根部。

- 连续 poll 按优先规则取出；迭代、直接打印不保证排序。
- 比较结果为 0 的多个元素都能保留，但它们的取出先后不保证。
- TreeSet 用比较结果参与唯一性判断；PriorityQueue 用它确定优先级。两者不能混为一谈。

### 成本理解的使用边界

复杂度描述规模增长趋势，不表示实际耗时相同。分配、缓存局部性、访问方式、比较或哈希的计算成本都会影响结果。O(1) 也不表示耗时严格固定。

本轮形成的是解释与选型框架，没有运行跨实现性能基准，不能把讨论中的性能倾向当成当前项目的实测结论。

## 第四轮总结：专用容器、配置与工具

### EnumMap / EnumSet：围绕同一种枚举表达数据

| 工具 | 解决的问题 | 例子与边界 |
| --- | --- | --- |
| EnumMap | 每个枚举值对应什么数据？ | 状态 → 人数；key 来自同一种枚举，遍历按枚举声明顺序 |
| EnumSet | 枚举成员中选中了哪些？ | READ / WRITE / DELETE 权限中拥有哪几项；成员来自同一种枚举 |

HashMap / HashSet 也能保存枚举，专用容器是可考虑的选择，并非强制要求。用户 ID、动态配置名称等不能预先列举为同一种枚举的 key，不适用这组专用容器。

EnumSet 可以与 C# 的 Flags 枚举在“组合多个选项”这一用途上对应理解；Java 这里操作的是集合，枚举常量仍是独立值，不要求像位标志一样手工分配数值。

使用枚举本身不要求学习内部数据结构。能判断这两类容器的用途和限制即可；专用表示的细节不作为本轮掌握要求。

### Properties：JDK 自带的简单字符串配置工具

Properties 位于 java.util，不需要额外依赖；支持读取和保存 `.properties` 等配置文本，配置项名称、意义由程序约定。

```properties
app.name=报名系统
retry.count=3
```

读取 `retry.count` 得到字符串 `"3"`，不会自动变成整数。常规使用保持字符串 key → 字符串 value，通过 setProperty / getProperty 操作；类型转换和有效性校验由程序或上层配置框架负责。

它适合简单、平铺的键值配置。Java 没有要求所有项目都采用这种格式，复杂嵌套结构也不一定适合用它表达。

**设计边界**：Properties 把键值容器、配置格式编解码、默认值查找集中在一个类中，底层来源和输出仍通过 Reader / Writer、输入输出流等 I/O 抽象提供。按职责拆分，也可以由普通 Map、独立编解码器和 I/O 组合完成；使用专用配置格式与把编解码绑定到容器，是可以分别评价的两个设计选择。

它继承 Hashtable<Object, Object>，暴露能写入非字符串的 put；这与字符串配置约定存在冲突，官方也不建议这样使用。可将其作为需要识别、会使用的传统工具，不必把这种继承设计当成新代码的范式。

### Collections：通用操作的查阅地图

Collection 是接口，Collections 是静态工具类，本身不保存一批元素。

| 用途 | 常用方法 | 需要留意什么 |
| --- | --- | --- |
| 排序与顺序调整 | sort、reverse、shuffle、swap、rotate | 修改传入列表的元素顺序，需要实例支持相关修改 |
| 查找与统计 | binarySearch、min、max、frequency | 二分查找要求预先按相同规则排序，方法不会代为排序 |
| 集合关系与子序列 | disjoint、indexOfSubList | 前者判断是否没有共同元素，后者找连续子列表位置 |
| 修改内容 | fill、replaceAll、addAll | 分别替换全部位置、替换相等项、批量添加；需要相应修改能力 |
| 写入已有目标列表 | copy(dest, src) | 复制元素引用到已有位置；目标 size 必须足够，仅 capacity 足够不行 |
| 创建简单集合 | emptyList / Set / Map、singletonList / singleton / singletonMap、nCopies | 这些结果不可修改；nCopies 重复的是同一元素引用 |
| 包装集合 | unmodifiable*、synchronized*、checked* | 分别提供不可修改、同步、运行时类型检查视图或包装；不等同于独立副本 |
| 构造比较器 | reverseOrder | 返回反向比较器，不直接反转列表 |

这里的 `emptyList / Set / Map` 分别指 emptyList、emptySet、emptyMap。具体重载按需查阅，不要求逐项记忆。

不可修改包装不能冻结元素对象。同步包装也不自动让任意复合操作整体原子化，遍历等仍需遵循其同步约定；checked 包装不能补救经原容器引用绕过检查的写入。这些包装的深入使用不作为本轮展开内容。

工具类中的方法修改边界不同：有的改原集合，有的返回新结果，有的只提供视图。使用前要确认这一点。

## 前两轮小测与巩固点

2026-11-04 做了五道逐题口头小测及针对性追问，没有运行代码测试：

| 题目 | 首次回答与反馈 |
| --- | --- |
| 最新报名资料、按学号查询、判断存在 | 正确选择 Map；补充 containsKey 不要求 value 非 null |
| source / view / snapshot，替换元素后修改原学生 | 独立答对李四、李四、王五，能区分列表变化和对象变化 |
| 同优先级学生分别加入 TreeSet、PriorityQueue | 首次误认为 TreeSet 保留 3 名，且不确定队列规则；解释后，改成优先级再按学号比较，正确答出均保留 3 名且顺序确定 |
| 正向下标循环删除连续两个 B | 独立答对最终为 A、B、C |
| 按学号判等的 HashSet，加入后修改学号 | 正确答出初始 size 为 1；首次误认为同一实例必能查到，经解释理解哈希查找边界，并答对仅修改无关姓名不影响查找 |

前两轮核心讲解在这次小测后结束，但提示后的理解不等同于全部独立答对。第三轮已用机制回访比较为 0 的含义，以及可变哈希 key 为什么可能查不到。用户随后能用自己的话概括“哈希值变了，条目未自动调整”，并总结接口、实现和工具的关系；其中“接口等于具体数据结构”“声明接口就可以不管实现”等表述已作校准。

以上代码均为讨论说明片段，不是完整程序；Student 构造器和访问方法按各例假设存在。四轮记录反映讲解、口头反馈与前两轮小测，没有为这些内容新增或运行 Java 实验，也没有为第三、四轮重新安排独立测验；不引用旧笔记的性能数据作为本轮验证结果。

## 当前进度与下次入口

| 轮次 | 当前状态 |
| --- | --- |
| 第一轮：能力与选择 | 核心讲解与相关小测已完成 |
| 第二轮：行为与边界 | 核心讲解及五题口头小测已完成；两个薄弱点已在第三轮通过机制回访 |
| 第三轮：机制与代价 | 已讨论主要实现、内存组织、哈希与判等、排序树及优先堆的机制与取舍；未展开完整算法或源码课程 |
| 第四轮：用途与边界补齐 | 已讨论 EnumMap、EnumSet、Properties、Collections，形成用途和方法查阅入口 |

**当前停留点**：四轮集合基础暂告一段落。用户已经自行总结接口、实现和辅助工具的关系，并确认以目前范围满足阶段性学习需要。这里的完成指计划范围的讨论结束，不表示已通过全面测试或掌握所有集合 API。

**后续入口**：可转向用户选择的其他主题；遇到具体集合需求时，从本文件的选型入口恢复，围绕实际疑问只补一个问题。不自动开启第五轮、API 清单或完整数据结构课程。

按需深入的内容包括红黑树调整、堆调整代码、精确扩容策略、哈希扰动和树化阈值、并发集合及线程安全组合操作、性能基准和完整 LRU 实现。它们属于后续选题，不是本阶段未完成的必做任务。

### 新窗口接续提示

可直接粘贴以下内容：

> D:\Learning 的 Java 集合四轮基础已讨论完，请先读 docs/index.md，再读 docs/Java集合学习记录.md，以后者为当前进度依据，旧 docs/Java集合.md 仅参考。目前停在能理解、判断和日常使用的阶段，不需要从第三轮重新开始，也不默认扩展成完整数据结构课程。用户有 C# 基础，具体 API 按需查询；恢复集合话题时，以当前实际疑问为入口，每次只推进一个小问题。前两轮有五题口头小测，第三、四轮以讨论和口头总结为主，没有新增 Java 运行实验或性能基准。

## 参考资料

- [廖雪峰：使用 List](https://liaoxuefeng.com/books/java/collection/list/index.html)：用户提供的当前教程入口。
- [廖雪峰：集合](https://liaoxuefeng.com/books/java/collection/index.html)：检查集合章节的知识覆盖。
- [Oracle：List](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/List.html)：位置、可选修改操作、不可修改列表的契约。
- [Oracle：Arrays.asList](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Arrays.html#asList(T...))：固定大小列表与原数组的共享关系。
- [Oracle：ArrayList(Collection)](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/ArrayList.html#%3Cinit%3E(java.util.Collection))：从集合元素创建新列表。
- [Oracle：Set](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Set.html)、[Map](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Map.html)：唯一性与键值对应的契约。
- [Oracle：ArrayList](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/ArrayList.html)、[LinkedList](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/LinkedList.html)、[ArrayDeque](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/ArrayDeque.html)：数组、链表和双端队列的实现定位与操作成本。
- [Oracle：HashMap](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/HashMap.html)、[HashSet](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/HashSet.html)：哈希分布、扩容与 Set 复用 Map 的实现关系。
- [OpenJDK：HashMap 源码](https://github.com/openjdk/jdk/blob/jdk-25%2B36/src/java.base/share/classes/java/util/HashMap.java)、[HashSet 源码](https://github.com/openjdk/jdk/blob/jdk-25%2B36/src/java.base/share/classes/java/util/HashSet.java)：节点保存哈希、已有 key 的 value 更新，以及 Set 的内部占位对象。
- [Oracle：TreeMap](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/TreeMap.html)、[TreeSet](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/TreeSet.html)：红黑树、比较规则和唯一性。
- [Oracle：PriorityQueue](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/PriorityQueue.html)：堆、优先取出与遍历顺序的边界。
- [Oracle：LinkedHashMap](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/LinkedHashMap.html)、[Linux：Linked Lists](https://docs.kernel.org/core-api/list.html)：哈希与链表组合、链表在实际软件中的用途。
- [Oracle：EnumMap](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/EnumMap.html)、[EnumSet](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/EnumSet.html)：面向枚举的专用映射和成员集合。
- [Oracle：Properties](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Properties.html)：字符串配置、流读写与继承带来的使用约束。
- [Oracle：Collections](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Collections.html)：通用方法、修改要求及包装类契约。

官方链接用于查阅契约，不表示本项目当前使用 JDK 25；`List.of` 从 Java 9 起可用。
