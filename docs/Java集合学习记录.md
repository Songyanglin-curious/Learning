# Java 集合学习记录

更新日期：2026-11-03。

本文件记录当前采用的学习计划、已经讨论的内容与下一步入口。现有的《Java集合》笔记作为参考材料，其篇幅及原有“已完成”条目不代表本轮学习进度；恢复学习时以本文件为准。

## 学习方式

围绕同一组集合走三轮：**先建立全貌，再理解行为，最后用原理解释行为**。不要求第一次就把某个集合彻底研究完。

- 已有 C# 基础，共通的数据结构、判等和哈希概念快速带过，重点学习 Java 的具体规则、行为边界和容易误判之处。
- 每次只推进一个小问题，先说明它接在哪个已有认识上；不固定安排简单预测题。
- 用“操作前有哪些对象 → 执行什么 → 操作后发生什么”解释行为，不只给“浅拷贝”“活视图”等标签。
- 只有结果不容易凭直觉判断或存在疑问时，才做最小实验；说明代码本身不作为运行证据。
- 理解稳定后再整理笔记。教程用于检查知识覆盖，旧笔记用于参考，都不是必须逐条消化的清单。
- 暂时用不到的分支记为待办；同一个疑问最多深入两轮，仍未解决时记录问题和后续步骤。

## 三轮学习计划

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

### 主线稳定后补齐

`EnumMap`、`Properties`、`Collections` 工具类等按用途和边界补齐，不给每个类安排同等篇幅。旧笔记中的高级主题和实验不自动成为必做任务。

## 已学内容

### 学生报名场景中的选择

系统依次收到 S01、S02、S01 三次报名：

- `List` 保存三条提交记录，重复报名也有记录价值；下标从 0 开始。逐条追加时保留提交顺序，不会自动按学号排序。
- `Set<String>` 保存学号成员，只保留 S01、S02；可用 `contains("S01")` 判断是否报名。不凭 `Set` 接口推断遍历顺序。
- `Map<String, Student>` 保存“学号 → 学生资料”。key 唯一，value 可以重复；对同一个 key 再次 `put` 会替换对应值，不会保留该 key 的提交历史。
- 已有“学号 → 资料”的 `Map` 时，可以用 `containsKey` 判断是否报名，不必只为查重再维护一个 `Set`。

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

已经理解上述含义；本轮没有继续展开哈希定位过程或实现判等方法。

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

这里的独立只针对容器中的位置和元素引用。保存可修改的 `Student` 对象时，建立新列表不会自动创建一批新学生；这一层尚待接着展开。

以上代码为本次讨论的说明片段；没有为它们新建或运行实验，学生变量假设已存在。

## 当前进度与下次入口

| 轮次 | 当前状态 |
| --- | --- |
| 第一轮：能力与选择 | 已完成本轮讨论；已明确 List、Set、Map、Queue、Deque 的用途关系，用户确认基础概念大体理解 |
| 第二轮：行为与边界 | 进行中；已讨论判等含义、List 修改能力、Arrays.asList 数组共享及 ArrayList 容器副本 |
| 第三轮：机制与代价 | 尚未按本计划展开；旧笔记的原理内容仅作参考 |

**当前停留点**：已经讲到 `new ArrayList<>(view)` 复制的是元素引用，而不是复制元素对象。最近两次关于修改能力和数组共享的讲解尚未得到进一步理解反馈。

**下一步只推进一个问题**：把同一批可修改的 `Student` 放进原列表和副本后，分别执行“替换某个位置的学生”与“修改某个学生的姓名”，观察谁会看到变化。先确认这一层，再决定是否需要最小实验。

之后按疑问回访 `List.of`、`Collections.unmodifiableList` 的修改边界，再继续顺序规则与迭代器。`hashCode` 契约在进入哈希机制前补齐，不要求现在重复学习已熟悉的通用概念。

## 参考资料

- [廖雪峰：使用 List](https://liaoxuefeng.com/books/java/collection/list/index.html)：用户提供的当前教程入口。
- [廖雪峰：集合](https://liaoxuefeng.com/books/java/collection/index.html)：检查集合章节的知识覆盖。
- [Oracle：List](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/List.html)：位置、可选修改操作、不可修改列表的契约。
- [Oracle：Arrays.asList](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Arrays.html#asList(T...))：固定大小列表与原数组的共享关系。
- [Oracle：ArrayList(Collection)](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/ArrayList.html#%3Cinit%3E(java.util.Collection))：从集合元素创建新列表。
- [Oracle：Set](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Set.html)、[Map](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Map.html)：唯一性与键值对应的契约。

官方链接用于查阅契约，不表示本项目当前使用 JDK 25；`List.of` 从 Java 9 起可用。
