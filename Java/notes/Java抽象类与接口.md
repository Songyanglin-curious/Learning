# Java 抽象类与接口

## 两者共同提供约束

抽象类和接口都能约束具体实现，都可以作为多态调用的类型入口。不能简单用“类有实现、接口没有实现”区分，因为接口也可以提供默认方法。

“基类”是一种角色，“抽象类”是一种类的形式。普通类和抽象类都可以作为基类，不是三个并列选项。

## 抽象类：共享基础并留下实现要求

```java
abstract class Account {
    private int balance;

    // 初始化账户共有的余额状态
    public Account(int balance) {
        this.balance = balance;
    }

    // 返回账户余额
    public int getBalance() {
        return balance;
    }

    // 要求具体子类提供取款校验
    protected abstract boolean canWithdraw(int amount);
}
```

抽象方法只声明要求，不提供方法体；具体子类必须补齐继承下来的抽象方法，或者自身继续声明为抽象类。

抽象类不能直接实例化，但可以有实例字段、构造方法和已经实现的方法。创建具体子类对象时，父类构造方法仍会执行，初始化父类部分。

抽象类不一定要包含抽象方法；声明为 `abstract` 本身就禁止直接实例化。

## 接口：表达能力约定

```java
interface Withdrawable {
    // 约定实现者提供取款操作
    void withdraw(int amount);
}
```

类用 `implements Withdrawable` 声明实现这个接口。接口中的这种普通方法声明默认是 `public abstract`，实现方法必须是 `public`。

接口可以作为变量、参数和返回值的类型。通过接口类型使用对象时，直接可用的能力由该类型决定，具体执行哪个实现由实际对象决定。

类可以实现多个接口，接口也可以继承多个接口：

```java
interface BankService extends Withdrawable, Transferable {
}

abstract class Account implements Withdrawable, Transferable {
}
```

以上声明假定 `Transferable` 已定义。抽象类可以暂不实现接口方法，留给具体子类。

## 默认方法：约定之外提供通用行为

```java
interface Greeter {
    // 要求实现类提供姓名
    String getName();

    // 根据已有能力提供通用问候行为
    default void greet() {
        System.out.println("你好，" + getName());
    }
}

class Person implements Greeter {
    // 提供姓名，供默认问候方法使用
    @Override
    public String getName() {
        return "张三";
    }
}
```

`new Person().greet()` 可以直接使用默认实现，输出“你好，张三”。实现类也可以重写 `greet()`。

默认方法的一个重要用途是接口演进：新增行为若能基于已有能力提供合理实现，已有实现类通常不必逐一补写。但仍要考虑默认行为是否适合各实现，以及是否出现方法冲突。

默认方法使接口同时表达能力约定和默认策略。这是一种设计折中，并不意味着所有接口方法都应提供实现。

默认方法选择规则先了解：

- 类或父类已有匹配的公开实例方法实现时，类的实现优先。
- 子接口重写父接口的默认方法时，更具体的子接口优先。
- 不相关接口提供冲突的默认方法时，实现类需要显式重写，解决冲突。

## 能力边界与选择

| 对比点 | 抽象类 | 接口 |
| --- | --- | --- |
| 直接实例化 | 不可以 | 不可以 |
| 实例字段与构造方法 | 可以有 | 没有 |
| 未实现的方法 | 可以声明抽象方法 | 可以声明抽象方法 |
| 已实现的方法 | 可以有 | 可以有，例如 `default` 方法 |
| 访问控制 | 可使用类成员的访问控制机制 | 普通抽象方法为公开契约；不是所有方法都只能为 public，例如可有私有辅助方法 |
| 使用关系 | 类用 `extends`，只能直接继承一个类 | 类用 `implements`，可以实现多个接口 |

接口声明的字段是 `public static final` 常量，不是每个对象各自持有的实例状态。默认方法没有改变这一边界。

- 不相关类型需要共同遵守一组能力约定：考虑接口。
- 相关类型需要共享状态、初始化过程和实现：考虑抽象类。
- 同时需要稳定的对外约定与共用基础：接口和抽象类可以搭配。
- 没有明确的抽象需求：普通类即可，不必预先建立多层结构。

编译器只能检查签名等语言约束，不会保证实现正确履行业务承诺。接口也不能代替业务规则和行为验证。

## 与 C# 的必要对照

- C# 同样支持类实现多个接口、接口继承多个接口，语法统一使用 `:`；Java 分别使用 `implements` 和 `extends`。
- C# 8.0 起支持接口默认实现，但需要运行环境支持，传统 .NET Framework 不支持。
- C# 接口默认实现直接写方法体，不使用 Java 的 `default` 关键字。类没有自行实现该方法时，需要通过接口类型调用；Java 的默认方法可以通过实现类实例直接调用。
- C# 的 `partial class` 是把同一个类分开声明的代码组织方式，与 `abstract` 不在同一维度。两者可以组合为 `abstract partial class`；Java 没有直接对应的分部类机制。

## 后续按需补充

函数式接口与 Lambda、复杂的默认方法冲突等，结合后续内容再学。

## 教程来源

- [抽象类](https://liaoxuefeng.com/books/java/oop/basic/abstract-class/index.html)
- [接口](https://liaoxuefeng.com/books/java/oop/basic/interface/index.html)
