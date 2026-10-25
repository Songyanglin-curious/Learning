# Java 核心类

学习范围：[廖雪峰《Java 核心类》](https://liaoxuefeng.com/books/java/oop/core/index.html)。重点是这些类型解决什么问题、有什么行为边界，以及与 C#、JavaScript 的区别。常规 API 按需查阅。

字符串部分已单独整理为 [Java 字符串、编码与拼接](Java字符串编码与拼接.md)，本文保留入口，集中记录后续讨论。

## 能力概览

| 内容 | 解决的问题 |
| --- | --- |
| String、编码、拼接工具 | 保存文本、转换字节、构造字符串 |
| 包装类型 | 把基本值接入对象体系，并提供相关操作 |
| BigInteger | 保存超出固定整数类型范围的整数 |
| BigDecimal | 表达十进制数值，明确精度与舍入规则 |
| JavaBean | 让工具按统一约定识别属性及读写入口 |
| enum | 定义一组固定的、有类型约束的选项 |
| record | 简洁地声明数据载体，生成相等比较等配套代码 |
| 常用工具类 | 数学计算、十六进制转换、随机数生成 |

## 包装类型与装箱拆箱

### 为什么基本类型之外还有包装类型

Java 把基本类型与引用类型分开。`int` 直接表达整数，`Integer` 是保存整数值的对象类型；两者取值范围相同，包装不会扩大范围。

基本类型适合直接存储和运算，包装类型可以进入需要对象的位置，例如 `List<Integer>`，也可以实现接口、取值为 `null`。包装类还集中提供解析、进制转换等方法，但静态工具方法不要求先装箱，例如 `Integer.parseInt("123")`。

Java 早期保留基本类型，是效率与类型统一性之间的取舍。后来加入泛型时，又以兼容既有代码和类库为重要条件，采用类型擦除方案。这些选择解释了两套表示为何长期共存，并不意味着其他设计不可能。

### 泛型为什么使用 Integer

现有 Java 泛型的类型参数使用引用类型，不能直接写 `List<int>`。以没有显式上界的 `T` 为例，擦除后通常按 `Object` 处理：

```java
List<String> names = new ArrayList<>();
names.add("张三");
String name = names.get(0);
```

可以近似理解为：编译器先检查元素类型，执行代码中的读取则类似 `(String) names.get(0)`。通常不会为每种类型实参生成独立的 `ArrayList` 类，但声明中的部分泛型信息仍可供反射读取。

`int` 不属于引用类型体系，不能直接作为这种对象引用表示，所以通过 `Integer` 衔接。擦除并非在理论上必然排斥基本类型；自动包装或生成专门实现都可以另行设计，但涉及不同的语义、性能与兼容取舍。

### 装箱具体做了什么

装箱、拆箱是跨语言概念，连接值的直接表示与对象表示：

```java
int n = 123;
Integer boxed = n;     // 装箱，通常通过 Integer.valueOf(n)
int restored = boxed;  // 拆箱，通过 intValue() 取值
```

编译器插入转换调用，实际动作在运行时执行。“自动”表示少写转换代码，不表示没有成本。装箱可能复用缓存对象，不能把它一律理解成分配新对象。

包装对象不可变。`Integer n = 10; n++;` 涉及拆箱、加一、再装箱并赋值，不会修改原 Integer 对象。

使用时注意：

- `null` 拆箱会抛出 `NullPointerException`。
- 两个包装对象直接用 `==` 比较的是引用；比较内容可用 `equals()`，但还要注意空值与具体类型。例如 `Integer` 与 `Long` 不会因为数值同为 1 就被 `equals()` 判为相等。
- 使用自动装箱或 `valueOf()`，不要沿用 `new Integer(...)` 等旧写法。

### 与 C#、JavaScript 对照

| 语言 | 机制与区别 |
| --- | --- |
| Java | `int` 与 `Integer` 是不同类型，装箱连接基本类型与对象类型 |
| C# | `int` 就是 `System.Int32` 的别名，值类型本身已有方法；转换为 `object` 等场景才涉及装箱 |
| JavaScript | 原始值访问属性时采用包装对象相关语义，因而能使用 `toFixed()` 等方法；不必据此推定引擎每次都实际分配临时对象 |

C# 的 `List<int>` 可以直接使用值类型，不要求因加入列表而装箱；`List<object>` 接收整数时则会装箱。装箱也适用于自定义结构体。

JavaScript 的 `new Number(123)` 明确创建包装对象，`typeof` 是 `"object"`，与原始数字不同。日常通常不需要这种写法。例如 `new Boolean(false)` 在条件判断中仍为真，因为它是对象。

### 装箱与数值转换不是同一个动作

以 C# 为例：

```csharp
int n = 123;
double a = n; // 转换成 double 数值表示
object b = n; // 复制 int 值，形成装箱对象，b 保存引用
```

装箱后，原来的 `n` 保持原样；对象保存的是当时的值和相应的运行时类型信息，里面仍然是 `System.Int32`。`object` 只是变量 `b` 的声明类型。

```csharp
object boxed = 123;
int a = (int)boxed;    // 成功：按保存的类型拆箱
// long b = (long)boxed; // 运行时异常：里面不是 long
long c = (int)boxed;   // 先拆出 int，再做数值转换
```

拆箱会检查实际类型，不是从字节中猜类型。也可以用 `boxed is int value` 检查并取值。装箱属于语言定义的一类转换，但不会把整数转换成另一种数值类型。

## BigInteger 与 BigDecimal

### BigInteger：整数范围不够时使用

```java
BigInteger a = new BigInteger("999999999999999999999999");
BigInteger b = a.add(BigInteger.ONE);
```

它不受 `long` 的固定 64 位范围限制，但仍受内存和实现限制。对象不可变，运算通过 `add()`、`multiply()` 等方法返回结果，不支持直接使用 `+`、`*` 运算符。

用途类似 C# 的 `System.Numerics.BigInteger`。普通整数范围够用时，无须为此增加存储与计算成本。

### BigDecimal：十进制表示与舍入

```java
BigDecimal a = new BigDecimal("0.1");
BigDecimal b = new BigDecimal("0.2");
BigDecimal result = a.add(b); // 0.3
```

它适合需要十进制精确表示和明确舍入规则的计算，使用目的接近 C# `decimal`，但不是同样的固定精度类型。

- 优先从十进制字符串构造。`new BigDecimal(0.1)` 会接收那个 double 已有的二进制近似值；`valueOf(double)` 通常更符合常见意图，但不能恢复此前浮点运算已经损失的信息。
- 不是所有运算都有有限的十进制结果。例如 1 除以 3，要明确精度或小数位数与舍入方式；直接要求精确结果的 `divide()` 可能抛异常。
- `equals()` 同时比较数值与 scale，所以 `1.0`、`1.00` 不相等；只比较数值大小时用 `compareTo()`，返回 0 表示数值相等。
- 它也是不可变对象，运算需要接收返回值。

## 枚举：有限选项与解释权

```java
enum OrderStatus {
    CREATED, PAID, CANCELLED
}
```

枚举为一组固定选项提供独立类型，避免直接使用数字、字符串导致的误传或混用。Java 枚举常量是预先定义的对象实例，正常代码不能自行 `new`，可以直接用 `==` 比较。

| 对比 | Java enum | C# enum |
| --- | --- | --- |
| 表示方式 | 固定实例集合的类，引用类型 | 基于整数的值类型 |
| 业务编号 | 显式添加字段、构造方法等 | 可直接写 `Paid = 10` |
| 整数转换 | 不能直接强转为枚举 | 可显式转换出未命名的枚举值 |
| 附属能力 | 可定义字段、方法并实现接口 | 枚举体内不支持实例字段和方法，可在外部组织相关操作 |
| 多选标志 | 通常用 `EnumSet` | 常用 `[Flags]` 配合位运算 |

不要把 `ordinal()` 当作稳定的数据库编号或协议码。它表示声明顺序，调整顺序就会变化，需要稳定编号时应单独定义。

本次讨论的设计判断是：枚举定义有哪些选项，选项在具体场景中如何处理，可以由外部负责。固定编号与成员的对应关系属于定义的一部分；界面展示、退款、外部服务调用等解释则可能随上下文变化。

Java 允许把属性和行为放进枚举，没有强迫这样组织。这里保留的偏好是让枚举保持简单，需要时再增加明确属于该概念的内容。这是职责分配上的选择，不影响正常使用。

C# 允许未命名整数值，与它限制枚举体内能力，是两个独立设计选择，不能说前者是换取后者必须付出的代价。

## record：减少数据类的重复实现

```java
record Point(int x, int y) {}
```

编译器为组件生成私有 final 字段、对应构造方法、`x()` / `y()` 访问方法，以及 `equals()`、`hashCode()`、`toString()`。默认按同类型 record 的组件判断相等，减少手写配套代码的遗漏与不一致。

```java
Point a = new Point(10, 20);
Point b = new Point(10, 20);
boolean sameObject = a == b;      // false
boolean sameData = a.equals(b);  // true
```

Java record 是引用类型，隐式 final，不能继承其他业务类或被继承，但可以实现接口、添加方法和构造校验。组件字段不能重新赋值，不代表深度不可变；组件若引用可变列表，列表内容仍可能变化。

适合坐标、查询结果等数据载体，不必用于所有业务对象。Java 14、15 中是预览功能，Java 16 正式定稿，Java 8 不能使用。

### record 与 struct 是不同维度

`struct` 主要选择值类型语义，普通赋值复制值；`record` 为数据表达生成配套行为。按内容相等，不意味着是值类型。

| 声明 | 类型语义 | 相关特点 |
| --- | --- | --- |
| Java record | 引用类型 | 组件字段 final，以 equals() 比较组件，== 比较引用 |
| C# record / record class | 引用类型 | 默认生成内容相等比较及 ==；位置属性默认为 init-only，支持 record 类之间继承与 with 复制 |
| C# record struct | 值类型 | 生成相等比较等代码，位置属性默认可修改，可使用 readonly 限制 |
| C# 普通 struct | 值类型 | 默认已有按字段的 Equals()；通常需自行提供 ==、可读的 ToString() 等 |

两边都不会自动深拷贝引用成员。C# 的 `with` 可复制并调整部分数据；Java 的类似需求通常显式构造新实例。

### 大量坐标的例子：表示语义与存储布局

C# 的小型结构体数组可直接连续存放坐标数据，不必为每个点创建单独对象。普通 Java class 或 record 的对象数组存放引用，坐标对象分别存在；record 本身没有承诺结构体数组式的内嵌布局。

Java 可以改用 `double[] xs`、`double[] ys` 或交错的基本类型数组，避免每点一个对象，但需要自己维护坐标对应关系和索引。这里的差别在于，C# 可以自然地兼顾自定义 Point 类型与紧凑存储。

紧凑布局通常有助于减少对象头、引用、分配和 GC 工作；遍历时还能改善缓存利用、预取并减少间接访问。这不代表 C# 数值加法本身更快，也不是所有值类型都更快：大型结构体复制、装箱等同样有成本，具体收益要测量。

值类型不等于在栈上。C# 结构体字段可内嵌于堆上的类对象，结构体数组元素内嵌于堆上的数组；局部变量也可能被优化到寄存器。紧凑来自数据内嵌与布局，不是来自栈。结构体内若有引用字段，内嵌的是引用，不会自动内嵌它指向的对象。

## JavaBean：普通方法之上的识别约定

`getName()`、`setName(...)` 在语言层面是普通方法。JavaBean 的价值在于统一约定，使通用工具能够推断：这里有一个名为 name 的属性，具有哪些读写入口。

JavaBeans 原本是组件规范，涉及属性、方法、事件等；教程主要介绍属性部分。JDK 的 `Introspector` 可以按约定分析属性，无须为每个类单独编写适配代码。

与 C# 的属性语法相比，Java 用命名约定识别 getter/setter，C# 则直接在语言中表达属性。JavaBean 解决了工具识别问题，也留下了重复代码和依赖命名的成本。

使用时注意：

- 属性由读写方法表达，不必有同名字段，也可以是计算结果。
- 只有 getter 是只读属性，不必为符合约定而给每个字段提供 setter。
- 普通 getter 无参数、返回属性值，setter 接收属性值并返回 void。基本 boolean 常用 `isEnabled()`，包装 Boolean 通常用 `getEnabled()`。
- 无参构造、序列化等要求应结合具体工具判断，不能把它们当成所有属性识别场景的硬要求。
- JavaBean 不等于 Spring Bean。后者是容器管理的对象，不要求统一写成 getter/setter 数据类。
- setter 只是写入入口，不自动维护业务规则；需要时仍要校验或提供有业务含义的操作。

## 常用工具类

| 工具 | 用途与注意点 |
| --- | --- |
| Math | 静态数学方法，如绝对值、最大最小值、开方；三角函数使用弧度 |
| StrictMath | 对相关函数提供更严格的跨平台结果一致性约定，普通计算通常用 Math 即可 |
| HexFormat | 字节数组与十六进制文本互转，Java 17 才提供 |
| Random | 普通随机、模拟和可重复测试；相同种子与调用顺序产生相同序列，不适合安全用途 |
| SecureRandom | 密码学安全的随机数据，适用于安全令牌、密钥材料等需求 |

HexFormat 是字节的十六进制表示，不是按 UTF-8 等编码解释文本。例如字节 `0x0F` 格式化为两个字符 `0f`。

SecureRandom 可以采用安全的伪随机算法，“伪随机”不等于“不安全”。它也有接受种子的接口；安全使用时不能随意给固定、可预测的种子。教程“不能指定种子”的说法不准确。

## 参考链接

### 教程

- [Java 核心类总览](https://liaoxuefeng.com/books/java/oop/core/index.html)
- [字符串和编码](https://liaoxuefeng.com/books/java/oop/core/string-encoding/index.html)、[StringBuilder](https://liaoxuefeng.com/books/java/oop/core/string-builder/index.html)、[StringJoiner](https://liaoxuefeng.com/books/java/oop/core/string-joiner/index.html)
- [包装类型](https://liaoxuefeng.com/books/java/oop/core/wrapper/index.html)
- [BigInteger](https://liaoxuefeng.com/books/java/oop/core/biginteger/index.html)、[BigDecimal](https://liaoxuefeng.com/books/java/oop/core/bigdecimal/index.html)
- [枚举类](https://liaoxuefeng.com/books/java/oop/core/enum/index.html)、[记录类](https://liaoxuefeng.com/books/java/oop/core/record/index.html)
- [JavaBean](https://liaoxuefeng.com/books/java/oop/core/javabean/index.html)、[常用工具类](https://liaoxuefeng.com/books/java/oop/core/utils/index.html)

### 机制与跨语言对照

- [Java 装箱规则](https://docs.oracle.com/en/java/javase/26/docs/specs/jls/jls-5.html#jls-5.1.7)
- [C# 装箱与拆箱](https://learn.microsoft.com/en-us/dotnet/csharp/programming-guide/types/boxing-and-unboxing)、[内置类型与别名](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/builtin-types/built-in-types)
- [JavaScript 原始值与包装](https://developer.mozilla.org/en-US/docs/Glossary/Primitive)
- [Java record](https://docs.oracle.com/en/java/javase/21/language/records.html)、[C# record](https://learn.microsoft.com/en-us/dotnet/csharp/fundamentals/types/records)、[C# 值类型](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/builtin-types/value-types)
- [JavaBeans 属性约定](https://docs.oracle.com/javase/tutorial/javabeans/writing/index.html)
- [HexFormat](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/HexFormat.html)、[SecureRandom](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/security/SecureRandom.html)
