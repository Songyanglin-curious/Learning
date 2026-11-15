# Java 字符串、编码与拼接

这三节主要认识文本的表示、文本与字节的转换，以及几种构造字符串的方式。具体方法按需查阅，重点掌握对象是否可变、比较规则和工具的适用场景。

| 类型或机制 | 用途 |
| --- | --- |
| `String` | 保存、共享和传递不可变的文本值 |
| 字符编码 | 按约定在文本与字节之间转换 |
| `StringBuilder` | 使用可变缓冲区逐步构造文本 |
| `StringJoiner` / `String.join()` | 连接多个元素，自动处理分隔符 |

## String 为什么不可变

字符串对象创建后，其内容不能被修改。多个位置可以放心引用同一个字符串，不需要担心某个调用方改掉内容，影响其他使用者。

```java
String name = "Alice";
String savedName = name;

name = name.replace("Alice", "Bob");
// name 引用 "Bob"，savedName 仍引用 "Alice"
```

这里改变的是变量 `name` 保存的引用，原来的字符串没有被改动。`replace()`、`toUpperCase()` 等方法需要使用返回结果，不能把它们当成原地修改。

不可变让字符串能够安全共享、复用相同字面量对应的对象，也让基于内容的哈希值保持稳定，适合用作哈希表的键。多个线程读取同一字符串内容，不会遇到其他线程同时修改其内容的问题；但保存字符串引用的共享变量仍可能被重新赋值，不能据此认为相关代码都线程安全。

代价是不能在原对象上追加或替换内容。循环中不断累积结果时，可能反复分配对象、复制已经积累的文本。这也是 `StringBuilder` 的用途之一。

不可变不代表每次调用方法都一定创建新对象。结果没有变化时，实现可能返回原对象；保证的是原内容不会被修改。

### 内容比较

Java 的字符串 `==` 比较是否引用同一个对象，`equals()` 比较内容。相同字面量可能共享对象，不能据此用 `==` 判断文本相同。

```java
String a = "hello";
String b = new String("hello");

boolean sameObject = a == b;       // false
boolean sameText = a.equals(b);    // true
```

这与 C# 不同：C# 字符串的 `==` 比较内容。

## 编码：如何解释字节

字符串表示文本，文件和网络传输的是字节。编码规定文字怎样转成字节，解码规定怎样把字节解释为文字。可以借助通信协议的经验理解：读到了字节，还要用约定的规则解释它。

```java
byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
String restored = new String(bytes, StandardCharsets.UTF_8);
```

这里假定已导入 `java.nio.charset.StandardCharsets`。第一行编码，第二行解码；读写双方需要使用约定一致的编码，不能把读取成功等同于解释正确。

字符串长度与字节数也不是一回事：

| 操作 | 统计内容 |
| --- | --- |
| `text.length()` | UTF-16 码元数量 |
| `text.codePointCount(0, text.length())` | Unicode 码点数量 |
| `text.getBytes(StandardCharsets.UTF_8).length` | UTF-8 编码后的字节数 |

例如 `"😀"` 的 `length()` 是 2，码点数量是 1，UTF-8 字节数是 4。用户看到的一个字符还可能由多个码点组成。处理长度限制或截断时，需要先确定统计单位。

## StringBuilder：逐步构造文本

`StringBuilder` 是可变的文本缓冲区，适合循环追加、插入、删除等构造过程，完成后通过 `toString()` 得到字符串。

```java
StringBuilder builder = new StringBuilder();
builder.append("姓名：").append("张三");
builder.append("，年龄：").append(18);
String text = builder.toString();
```

`append()` 修改 builder 并返回自身，因此可以链式调用。用途与 C# 的 `StringBuilder` 基本一致。

少量、固定的拼接直接使用 `+` 即可，不必看到字符串连接就改写。循环中反复积累文本时再考虑 builder。它不保证线程安全，通常作为方法内的局部对象使用。

## StringJoiner：封装分隔符处理

把多个名字拼成 `[张三, 李四]` 时，需要处理元素间的逗号、整体前后缀，还要避免末尾多出分隔符。`StringJoiner` 把这些常见逻辑封装起来。

```java
StringJoiner joiner = new StringJoiner(", ", "[", "]");
joiner.add("张三");
joiner.add("李四");
String text = joiner.toString(); // [张三, 李四]
```

这里假定已导入 `java.util.StringJoiner`，构造参数依次是分隔符、前缀、后缀。如果只想连接现有元素，可以直接使用：

```java
String text = String.join(", ", "张三", "李四");
// 张三, 李四
```

它没有引入新的字符串能力，是减少重复代码的便利工具类。严格说不是语法糖：它是普通标准库 API，不是编译器提供的新语法。

## 方法使用时的注意点

搜索、截取、大小写转换等常规 API 知道用途和查阅方式即可。`split()`、`replaceAll()` 使用正则表达式，`replace()` 可以做字面量替换，不要只凭方法名称把它们视为同一种操作。

教程中的 API 还需要留意版本，例如 `strip()`、`isBlank()` 从 Java 11 才提供，Java 8 不能直接使用。字符串内部采用什么数组存储属于具体版本的实现细节，不应把内部一定是 `char[]` 当成所有 JDK 的保证。

## 学习来源

- [廖雪峰：字符串和编码](https://liaoxuefeng.com/books/java/oop/core/string-encoding/index.html)
- [廖雪峰：StringBuilder](https://liaoxuefeng.com/books/java/oop/core/string-builder/index.html)
- [廖雪峰：StringJoiner](https://liaoxuefeng.com/books/java/oop/core/string-joiner/index.html)
