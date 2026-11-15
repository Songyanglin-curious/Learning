# Java 反射与动态代理

## 学习范围

对应教程 5.1–5.6：Class、字段访问、方法调用、构造方法调用、继承关系与动态代理。重点理解运行机制、描述对象与业务实例的区别，以及适用边界；常规 API 写法按需查阅。

## 反射为什么能够工作

运行中的 Java 程序需要区分三类内容：

| 内容 | 例子 |
| --- | --- |
| 对象数据 | 小明的 age 是 20，小红的 age 是 30 |
| 方法代码 | getAge 方法读取当前对象的 age 并返回 |
| 类型元数据 | 类名、字段名称和类型、方法名称和参数、继承关系等 |

元数据就是“描述程序结构的数据”。class 文件既有字节码，也有类型结构信息；JVM 加载类后建立相应的运行时结构。反射查询这些信息，不需要读取或重新理解源码。

JVM 本来就有访问字段、执行方法、创建对象的机制。Java 标准库与 JVM 配合，提供查询类型和执行操作的标准入口。接口背后还需要成员查找、权限检查、参数适配等支持，不能理解成没有额外工作的简单转发。

概念上的字段访问过程：字段名 → 查询类型元数据 → 找到字段描述 → 检查访问条件 → 定位指定对象中的字段 → 读取或写入。具体内存布局与底层调用实现由 JVM 实现决定，不必为理解反射先深入源码。

反射是跨语言的编程概念。Java 字节码与 CIL 是中间代码格式，JVM 与 CLR 是运行时；不要把两组层级混在一起。原生机器码程序也可以携带元数据和操作入口，实现反射。C/C++ 通常需要自行注册或生成描述信息及访问函数；C++ RTTI 的类型识别能力不等于完整的成员反射。C++ 的静态反射与这里讨论的运行时反射也需区分。

- 内省：查看自身的类型与成员结构。
- 反射操作：根据描述创建实例、读写字段、调用方法。

能识别类型不代表能枚举字段或调用任意方法；能力取决于保留和开放了哪些信息。反射也不能恢复完整源码、注释、所有局部变量等内容。

## 5.1 Class：用对象表示类型

| 写法 | 含义 |
| --- | --- |
| Person | 源码中的类型名称 |
| new Person(20) | 一个具体业务实例，有自己的数据 |
| Person.class | 表示 Person 类型的 Class 对象，是查询类型信息的入口 |

Class 对应 C# 中的 Type。Class 对象不是 Person 实例，也不是完整的 JVM 内部元数据本体，而是 Java 代码访问类型信息的入口。

```java
Class<?> a = Person.class;             // 代码中已知类型。
Class<?> b = person.getClass();        // 手上已有对象，取得实际类型。
Class<?> c = Class.forName("demo.Person"); // 只有完整类名字符串。
```

Class<?> 表示“描述某个类型，但这里没有确定具体是什么类型”。

getClass() 取决于对象的实际类型，不取决于变量声明为 Object 还是 Person。同一个运行时类型对应同一个 Class 对象；不同类加载器加载的同名类可以是不同类型。

类加载与初始化不同：Person.class 本身不触发类初始化；单参数 Class.forName 通常会触发目标类初始化。

## 5.2–5.4：成员描述、目标实例、实际参数

这些操作共享同一模型：先确定成员，再提供执行所需的对象与参数。

以下片段假设 Person 是可访问的 public 类，有 private int age、public Person(int age) 构造方法，以及 public String introduce(String prefix) 方法。省略异常处理，片段用于对照 API，不是独立运行程序。

### 字段：描述没有绑定实例

```java
var field = Person.class.getDeclaredField("age");
field.setAccessible(true);

int age = field.getInt(person);
field.setInt(person, 30);
```

field 表示 Person 的 age 字段；person 决定读取或修改哪个人的年龄。同一个 Field 可以访问多个兼容实例。称它为“字段描述”比“访问规则”更准确。

静态字段属于类，目标参数通常传 null。通用的 get 返回 Object，基本类型值会装箱；getInt 是针对 int 的读取方式。

### 方法：查找时传类型，执行时传值

```java
var method = Person.class.getMethod("introduce", String.class);
Object result = method.invoke(person, "今年");
```

| 内容 | 决定什么 |
| --- | --- |
| introduce 与 String.class | 哪个方法、哪个重载 |
| person | 在哪个对象上执行 |
| 今年 | 本次执行传入的参数值 |

Method 与 Field 一样，没有绑定某个业务实例。Method 是方法描述，不是已经绑定目标的函数。

查找方法时参数类型需要匹配签名：int.class 与 Integer.class 不可互换。查找阶段不会按普通调用的自动装箱规则替你选择重载。

执行时仍遵守正常的实例方法重写与动态分派规则。静态方法通常用 method.invoke(null, 参数) 调用。invoke 返回 Object，基本类型结果装箱，void 方法结果为 null。

目标方法抛出的异常被包装为 InvocationTargetException，通过 getCause() 取得原始异常；查找失败、访问失败、参数不合适则属于反射操作本身的问题。

### 构造方法：正常创建实例的反射入口

```java
var constructor = Person.class.getConstructor(int.class);
Person person = constructor.newInstance(20);
```

对应 new Person(20)：先找到构造方法，再传值创建对象。正常执行构造方法，没有额外的特殊魔法；价值在于类型与构造方法可以到运行时才确定。

抽象类不能实例化，参数和访问权限等约束仍然存在。使用 Constructor.newInstance，避免已经弃用的 Class.newInstance。

### 查找范围与访问权限分开

| API 家族 | 查找范围 |
| --- | --- |
| getField / getMethod | 公共成员，包括继承的公共成员 |
| getDeclaredField / getDeclaredMethod | 当前类自己声明的成员，包括非公共成员，不搜索父类 |
| getConstructor | 当前类的公共构造方法，构造方法不继承 |
| getDeclaredConstructor | 当前类声明的构造方法，包括非公共构造方法 |

复数形式用于枚举成员。Declared 表示“当前类声明”，不表示已经取得访问私有成员的权限。

setAccessible(true) 请求抑制语言访问检查，仍受模块封装等限制，可能失败。绕过访问检查也可能破坏对象通过正常方法维护的不变量，不应作为日常业务设计习惯。

## 5.5 查询继承关系有什么用

getSuperclass() 返回直接父类，getInterfaces() 返回直接接口。通用代码常用类型兼容性判断，比自己遍历继承链更直接：

```java
Object.class.isAssignableFrom(Person.class); // true
Person.class.isAssignableFrom(Object.class); // false
Person.class.isInstance(obj);                // 判断具体对象是否兼容。
```

固定读法：左边.isAssignableFrom(右边)，问“右边类型的值能赋给左边类型的变量吗？”isInstance 类似把 instanceof 的目标类型改为运行时取得的 Class。

例如插件类型来自配置：先 Class.forName(className)，再 Plugin.class.isAssignableFrom(type)，确认它实现了插件接口，才继续创建和使用。类型已经写死时通常交给编译器检查；类型在运行时才确定时，这些 API 才体现价值。

## 5.6 动态代理：自动生成接口实现并统一接收调用

### 先区分装饰行为与动态代理

在调用前后打印日志，用 JS 高阶函数包装、手写 Java 代理类都能实现，并不依赖反射。

JDK 动态代理额外解决的问题是：运行时根据接口生成满足 Java 类型要求的实现类，把接口的各个方法统一接到 InvocationHandler。Java 中一个通用函数不能直接充当实现了多个方法的接口对象。

### 创建与调用

假设 Calculator 是可访问的接口，声明 int add(int a, int b)，target 是它的真实实现对象：

```java
Calculator calculator = (Calculator) java.lang.reflect.Proxy.newProxyInstance(
    Calculator.class.getClassLoader(),
    new Class<?>[]{Calculator.class},
    // 统一记录调用，并转发给真实对象；保留目标异常。
    (proxy, method, args) -> {
        System.out.println("开始调用 " + method.getName());
        try {
            return method.invoke(target, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause();
        } finally {
            System.out.println("调用结束");
        }
    }
);
```

三个参数分别指定类加载器、需要实现的接口列表、统一的调用处理器。上述 lambda 实现 InvocationHandler。

调用 calculator.add(2, 3) 时：

1. 接收调用的是 JDK 生成的代理对象，它实现了 Calculator。
2. 生成的 add 方法转交给处理器。
3. 处理器收到 proxy（代理对象）、method（add 的 Method 描述）、args（装箱后的参数数组）。
4. 本例打印日志，再通过 method.invoke(target, args) 调用真实对象。
5. 返回结果经过代理方法的类型适配，回到调用者。

生成类负责把具体的接口调用转为统一的“方法描述 + 参数”。处理器负责决定怎么执行。

### 是否需要匹配，是否需要真实对象

不一定需要自己按方法名匹配。所有方法统一转发时直接 method.invoke(target, args)；不同方法需要不同处理时才分支。

真实对象也不是必需的。处理器可以直接返回结果、读缓存或发网络请求。动态代理用 Method 描述本次调用，但业务处理不一定通过反射执行。

因此要区分两种 invoke：InvocationHandler.invoke 是处理调用的统一入口；Method.invoke 是通过方法描述执行目标方法的反射 API。

### 必要边界

- JDK 动态代理实现接口，代理对象不是业务实现类的子类。
- 返回值和异常必须符合接口约定；不符合约定的受检异常可能被包装为 UndeclaredThrowableException。
- equals、hashCode、toString 也会进入处理器，需要明确其语义。本例将这些调用同样转发给 target，仅用于说明机制，不保证合适的代理相等性语义。
- 不要用 method.invoke(proxy, args) 向代理自身转发，会再次进入处理器并造成递归。

## 使用判断与学习结论

类型或成员到运行时才确定时，反射适用于插件、对象映射、框架等通用机制。可以直接调用时通常直接调用，保留编译期检查与重构支持；接口调用已能解决的问题，不必改成字符串查找。

核心应理解：运行时信息来自哪里，描述对象与实例如何区分，查找和执行如何分开，动态代理如何把调用转换为数据。普通 API 名称按需查阅；底层实现和性能优化遇到实际问题再深入。

## 参考资料

- [廖雪峰：反射 5.1–5.6](https://liaoxuefeng.com/books/java/reflection/index.html)
- [JVM 规范：方法区与运行时结构](https://docs.oracle.com/javase/specs/jvms/se25/html/jvms-2.html#jvms-2.5.4)
- [Java 反射包：能力与模型边界](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/reflect/package-summary.html)
- [Class：类型描述、成员查找与兼容性判断](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Class.html)
- [Method：调用、动态分派与异常](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/reflect/Method.html)
- [AccessibleObject：访问检查与模块边界](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/reflect/AccessibleObject.html)
- [Proxy：动态代理与调用处理器](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/reflect/Proxy.html)

官方 API 链接使用 Java 25 文档作为本次参考，不代表项目指定或已验证使用该 JDK。
