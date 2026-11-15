# Java 类、方法、构造方法与重载

## 核心认识

类定义对象的结构和行为，里面可以有字段、方法和构造方法。通过 `new` 创建类的实例；构造方法用于初始化实例；方法和构造方法都可以根据参数列表重载。

这些基础概念与 C# 基本一致，先掌握整体关系，不必反复比较相同的语法。

## 一个完整示例

```java
class Person {
    private String name;

    // 创建实例时初始化姓名
    public Person(String name) {
        this.name = name;
    }

    // 未提供姓名时使用默认姓名
    public Person() {
        this("匿名");
    }

    // 返回自我介绍
    public String greet() {
        return "你好，我是" + this.name;
    }

    // 返回面向指定对象的问候语
    public String greet(String target) {
        return "你好，" + target + "，我是" + this.name;
    }
}
```

使用这个类：

```java
Person p = new Person("张三");
System.out.println(p.greet());
System.out.println(p.greet("李四"));
```

| 内容 | 示例 | 用途 |
| --- | --- | --- |
| 类 | `Person` | 定义实例的结构与行为 |
| 字段 | `name` | 保存实例的状态 |
| 方法 | `greet()` | 定义实例可以执行的操作 |
| 构造方法 | `Person(String name)` | 创建实例时完成初始化 |
| 实例化 | `new Person("张三")` | 创建一个具体对象 |
| 重载 | 两个 `Person`、两个 `greet` | 通过不同参数列表提供不同调用入口 |

`this.name` 表示当前实例的字段；`this("匿名")` 表示调用本类的另一个构造方法，共用初始化逻辑。

## 构造方法与重载的规则

- 构造方法与类同名，没有返回类型；写成 `void Person()` 会成为普通方法。
- 普通类完全没有声明构造方法时，编译器会提供默认无参构造方法；声明了构造方法后，如果还需要无参版本，就要自己声明。
- 重载通过参数数量、类型或类型顺序区分，不能仅靠返回类型或参数名区分。

以上规则对普通 C# 类也基本适用。

## 本次涉及的修饰符

| 写法 | 含义 |
| --- | --- |
| `public` | 公开访问，具体还受所在类型等访问边界约束 |
| `private` | 限制外部访问；常用于封装字段或限制构造入口 |
| `static` 字段或方法 | 属于类，可以通过类名访问 |
| `final class` | 类不能被继承，对应 C# 的 `sealed class` |

Java 没有 C# 那种 `static class`。只提供静态操作的工具类通常这样表达：

```java
public final class Tools {
    // 禁止外部通过构造方法创建实例
    private Tools() {
    }

    // 通过类名调用工具操作
    public static void run() {
    }
}
```

调用方式是 `Tools.run()`。这里三个限制各自独立：

- `final` 禁止继承，本身不禁止实例化。
- 私有构造方法阻止外部直接 `new Tools()`。
- `static` 让方法属于类，无须先创建实例。

Java 顶层类不能用 `static` 修饰。嵌套类可以使用 `static`，但含义不同，留到内部类单元再学。

还有一个默认行为值得先记住：普通类的成员没有写访问修饰符时，通常表示同包可访问，而不是 C# 类成员常见的默认 `private`。访问控制的完整规则后续单独学习。

## 教程来源

- [面向对象基础](https://liaoxuefeng.com/books/java/oop/basic/index.html)
- [方法](https://liaoxuefeng.com/books/java/oop/basic/method/index.html)
- [构造方法](https://liaoxuefeng.com/books/java/oop/basic/constructor/index.html)
- [方法重载](https://liaoxuefeng.com/books/java/oop/basic/overload/index.html)
