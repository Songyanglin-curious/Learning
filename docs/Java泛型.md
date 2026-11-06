# Java 泛型

## 核心理解

泛型把类型之间的关系交给编译器检查。例如 `List<String>` 约束放入的元素，并让读取结果的静态类型成为 String。它不仅用于集合，也用于类、接口和方法。

本文代码是说明片段，不是独立程序；集合类需导入 `java.util` 下的对应类型，反射示例需处理或声明受检异常。标为编译错误或运行异常的代码用于展示边界。

```java
List<String> names = new ArrayList<>();
names.add("小明");
String name = names.get(0);
// names.add(18); // 编译错误
```

右侧 `<>` 让编译器推断类型参数，不代表没有泛型约束。

## 自己定义泛型类和方法

```java
class Box<T> {
    private T value;

    // 保存指定类型的值。
    public void set(T value) {
        this.value = value;
    }

    // 返回保存的值。
    public T get() {
        return value;
    }
}
```

`T` 是自定义的类型参数名，关联字段、参数和返回值。使用 `Box<String>` 时，编译器按 String 检查这些位置；不会为此生成一份新的 Box 类。

两次 `new Box<>()` 创建两个独立对象，各自保存实例字段。共用同一个类不等于共用字段值，类型擦除也不会合并对象。

Java 的类型实参不能是基本类型：用 `Box<Integer>`，不能用 `Box<int>`。传入 `18` 时可以自动装箱。

泛型方法在返回类型前声明自己的参数：`static <T> T ...` 中，`<T>` 是声明，后面的 `T` 是返回类型。方法自己的类型参数也可以用于静态方法；静态成员不能直接使用所属类的类型参数。

## 类型擦除：擦掉什么，留下什么

```java
Object names = new ArrayList<String>();
Object ages = new ArrayList<Integer>();
System.out.println(names.getClass() == ages.getClass()); // true
```

两个对象的运行时类都是 ArrayList。普通 ArrayList 实例不记录创建时指定的 String 或 Integer，但容器里的字符串仍是 String 对象，整数包装对象仍是 Integer。

- 没有上界的 T 擦除为 Object。
- `T extends Number` 中的 T 擦除为 Number；多个上界时按最左上界擦除。
- 编译器进行类型检查，并在需要的位置插入类型转换；涉及重写时还可能生成桥接方法。

不能把它理解成“List<String> 变成 List<Object>”。这两种写法在编译期仍有不同约束，只是运行时没有各自独立的参数化类身份。

擦除是兼容性优先的历史取舍：Java 5 引入泛型时，让已有类库可以逐步增加泛型声明，并保持与旧代码的互操作。泛型本身并非必须用擦除实现。

“所有泛型信息都消失”也不准确。类的泛型父类、字段类型、方法签名等声明信息可以保存在 class 文件的签名元数据中，供反射读取。例如字段声明 `List<String> names` 的泛型类型可读取；仅拿到普通 `new ArrayList<String>()` 对象，不能从其运行时类还原创建时的 String。声明中的 T 也不自动等于实例使用时的具体类型。

## 混放、原始类型和运行时错误

需要混放对象时可以显式使用 `List<Object>`，读取时直接得到 Object。需要统一处理有共同父类型的对象时，也可以选择更具体的共同类型。

不写类型参数的 `List` 或 `ArrayList` 是原始类型（raw type），主要用于兼容旧代码。它可以绕过检查，但不等于安全：

```java
ArrayList<String> names = new ArrayList<>();
ArrayList raw = names; // 指向同一个容器
raw.add(123);          // unchecked 警告，运行时仍能写入
String name = names.get(0); // ClassCastException
```

容器没有按 String 检查写入。最后一行读取后，编译器插入的 String 转换发现实际对象是 Integer，才抛出异常。因此不能笼统说“运行时没有约束”。

## extends 与 super 通配符

虽然 Integer 是 Number 的子类，`List<Integer>` 却不是 `List<Number>` 的子类型。否则通过后者就能把 Double 放入整数列表。这个限制并非单纯由擦除造成，而是可读可写容器的类型安全要求。

`?` 表示某个具体但未知的类型。通配符让使用者在不确定具体类型时，只进行能证明安全的操作。

| 写法 | 可能指向的列表 | 安全读取为 | 安全写入 |
| --- | --- | --- | --- |
| `List<? extends Number>` | List<Integer>、List<Double>、List<Number> 等 | Number | 除 null 外，不能直接添加具体值 |
| `List<? super Integer>` | List<Integer>、List<Number>、List<Object> | Object | Integer 或其子类型的值；Integer 本身是 final |
| `List<?>` | 任意元素类型的列表 | Object | 除 null 外，不能直接添加具体值 |

表中的允许是编译期类型规则；具体集合仍可能禁止修改或禁止 null。

extends 的来源无论是哪种数，读取都能当 Number；写入整数却不安全，因为实际可能是 Double 列表。super 的目标无论是哪种，都能接收 Integer；读取却可能拿到 Object 列表中的字符串。

extends 不代表不可修改：通过这个引用仍可能调用 clear、remove；原始的具体类型引用也仍可添加元素。

```java
// 将来源列表中的元素追加到目标列表。
static <T> void copy(
        List<? extends T> source,
        List<? super T> destination) {
    for (T item : source) {
        destination.add(item);
    }
}
```

来源提供 T，目标接收 T，因此可以把 List<Integer> 的元素追加到 List<Number>。这就是常说的 PECS：生产者用 extends，消费者用 super。“生产”和“消费”描述的是方法如何使用参数。

## Class<T>：编译期关系与运行时凭证

```java
Class<String> type = String.class;
java.lang.reflect.Constructor<String> constructor = type.getDeclaredConstructor();
String value = constructor.newInstance();
```

左侧的 `<String>` 让编译器关联 Class、Constructor 和创建结果的类型。右侧的 `String.class` 是实际传入的类对象，携带 String 的运行时信息。

T 被擦除，不会抹掉 String.class 对象描述的类。因此反射仍知道该调用哪个构造方法。教程中的 `Class.newInstance()` 是旧 API；笔记采用获取 Constructor 后调用 newInstance 的方式。

## 泛型与数组的边界

数组在运行时知道元素类型，并检查写入：

```java
Object[] values = new String[2];
values[0] = "小明";
values[1] = 123; // ArrayStoreException：实际数组仍是 String[]
```

`new T[3]` 不合法，因为创建数组需要具体的元素类型，而运行时没有从 T 得到这个信息。将 Object[] 强转也不会把实际对象变成 String[]。

可以额外传入 Class<T>，告诉运行时要创建哪种数组：

```java
// 根据引用类型凭证创建数组；调用方应传入 String.class 等引用类型。
@SuppressWarnings("unchecked")
static <T> T[] createArray(Class<T> elementType, int length) {
    return (T[]) java.lang.reflect.Array.newInstance(elementType, length);
}
```

调用 `String[] names = createArray(String.class, 3)` 时，编译器由 Class<String> 推断返回 String[]；运行时由 String.class 创建真正的 String[]。Array.newInstance 返回 Object，所以这里需要转换。不要把 int.class 创建的 int[] 与 Integer[] 混淆。

可以声明 `Box<String>[]`，但不能直接 `new Box<String>[3]`：数组运行时无法检查 Box 的 String 类型实参。强转创建的泛型数组仍可能被其他引用污染；仅去掉一个原始引用不代表彻底安全，后续仍能通过 Object[] 等别名写入不匹配的 Box。

泛型可变参数 `T...` 底层也是数组。直接用字符串调用时可能生成 String[]，但从另一个泛型方法转调时可能生成 Object[]；将它作为 String[] 返回给调用者就可能报错。`@SafeVarargs` 和 `@SuppressWarnings` 不会增加运行时保护。此处了解风险即可，复杂的泛型数组和可变参数设计按需深入。

## 与 C# 的对照

| 能力 | Java | C# |
| --- | --- | --- |
| 编译期泛型检查 | 支持 | 支持 |
| 具体类型实参的运行时身份 | 普通实例经过擦除 | CLR 保留具体泛型类型 |
| 两种元素类型的列表比较运行时类型 | ArrayList<String> 与 ArrayList<Integer> 相同 | List<string> 与 List<int> 不同 |
| 基本类型作为类型实参 | 用 Integer 等包装类 | 可以用 int 等值类型 |
| 泛型数组创建 | 不能直接 new T[n]，可显式提供 Class<T> | 可以直接 new T[n] |
| 普通可读写列表的子类型关系 | List<Integer> 不能当 List<Number> | List<string> 不能当 List<object> |

C# 的不同封闭泛型类型有不同的运行时身份，不代表每一种都必须拥有完全独立的机器码；运行时可以共享部分实现代码。

Java 这里的复杂感来自两套信息要配合：泛型让编译器检查关系，Class<T> 在需要时把具体类型显式传给运行时。

## 参考资料

- [廖雪峰：泛型](https://liaoxuefeng.com/books/java/generics/index.html)：本次学习主线。
- [廖雪峰：泛型和反射](https://liaoxuefeng.com/books/java/generics/reflection/index.html)：反射 API、泛型数组及可变参数的边界。
- [Oracle：Type Erasure](https://docs.oracle.com/javase/tutorial/java/generics/erasure.html)：擦除、转换与桥接方法。
- [Microsoft：Generic types and methods](https://learn.microsoft.com/dotnet/csharp/fundamentals/types/generics)：C# 泛型及运行时类型信息。
- [Microsoft：Generics in the runtime](https://learn.microsoft.com/en-us/dotnet/csharp/programming-guide/generics/generics-in-the-run-time)：CLR 对值类型与引用类型泛型的处理。
