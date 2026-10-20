# Java 继承与多态

## 学习主线：机制是手段

封装、继承、多态通常被称为面向对象的三大特征。它们是解决设计问题的手段，不是项目必须集齐的目标；职责清楚、规则得到维护、变化容易控制，才是使用这些机制的理由。

重写不是三大特征之一，而是实现这里讨论的运行时多态的一种机制。

## 封装：建立边界并维护规则

访问修饰符的语义由语言规定，业务规则由具体需求决定。例如，`private` 限制字段访问，而“库存不能为负”需要业务代码保证。

把字段设为 `private`，再提供不受限制的 setter，并不一定保护了业务规则。提供“扣减库存”等有明确含义的操作，可以把检查与状态修改集中在对象内部。

对外可以分别提供“是否允许操作”和“执行操作”的入口，但执行操作内部仍需校验：调用方可能跳过预检，状态也可能在两次调用之间变化。涉及并发时，仅重复校验还不够，需要适当的原子性保障；具体实现按需再学。

## 继承、重写与多态的关系

```java
class Person {
    // 输出通用的身份介绍
    public void introduce() {
        System.out.println("我是一个人");
    }
}

class Student extends Person {
    // 重写父类方法，提供学生的身份介绍
    @Override
    public void introduce() {
        System.out.println("我是一个学生");
    }
}
```

使用时：

```java
Person p = new Person();
p.introduce(); // 我是一个人

p = new Student();
p.introduce(); // 我是一个学生
```

- **继承**：`Student extends Person` 建立子类型关系，使 `Person` 变量可以引用学生对象。Java 类只能直接继承一个父类。
- **重写**：子类为父类的实例方法提供自己的实现。与重载不同，这里的方法名和参数列表相同。
- **多态**：通过父类型调用可重写的实例方法时，根据实际对象选择实现。

`@Override` 让编译器检查方法确实在重写父类型方法，不负责开启多态。Java 普通、可继承的实例方法通常默认支持重写，无须像 C# 那样先声明 `virtual`。

## 继承还意味着行为承诺

“生活分类上是一种”不足以证明继承设计合理。子类还应该能够遵守父类对调用方的约定。

例如，如果父类承诺“金额为正且余额充足就允许取款”，定期账户却额外要求到期，调用方原来依据的约定就不再够用。问题在于父类承诺与子类限制不一致，并不是账户不能有差异。

可以修正共同约定：允许账户存在不同的取款条件，通过统一校验入口表达；也可以把某种取款能力单独定义成接口。若只是想复用实现，可以考虑组合，即持有并调用另一个对象，不必建立父子类型关系。

编译器能检查类型和方法签名，不能完全检查业务承诺。合理的类型划分、明确的契约和共同的行为测试，可以减少对人工注意的依赖。

## 父类流程会调用子类重写的步骤

```java
class Account {
    // 提供示例中的基础取款条件
    protected boolean canWithdraw(int amount) {
        return amount > 0;
    }

    // 固定操作流程，根据实际对象的校验结果决定是否继续
    public final void withdraw(int amount) {
        if (!canWithdraw(amount)) {
            System.out.println("不允许取款");
            return;
        }
        System.out.println("执行取款");
    }
}

class FixedTermAccount extends Account {
    private boolean matured;

    // 初始化账户是否到期的状态
    public FixedTermAccount(boolean matured) {
        this.matured = matured;
    }

    // 在父类条件基础上增加到期条件
    @Override
    protected boolean canWithdraw(int amount) {
        return super.canWithdraw(amount) && matured;
    }
}
```

调用 `new FixedTermAccount(false).withdraw(100)` 时，执行父类的 `withdraw()`，但其中的 `canWithdraw()` 相当于 `this.canWithdraw()`，因此执行子类版本，输出“不允许取款”。

- 方法写在父类里，不代表它内部调用的方法固定使用父类实现。
- `final` 禁止重写 `withdraw()`，不妨碍其中的校验方法动态调用子类实现。
- `super.canWithdraw(...)` 显式调用父类版本，可在其结果上补充条件。

这类“父类固定流程，子类调整步骤”的组织方式通常称为模板方法模式。示例只演示调用关系，没有实现余额维护和实际扣款。

这里的动态调用针对可重写的实例方法；`private`、`static`、`final` 方法不能按相同方式被重写。

## 后续按需补充

父子类型转换、字段访问与方法调用的区别、父子类初始化顺序，遇到具体代码再进一步讨论。

## 教程来源

- [继承](https://liaoxuefeng.com/books/java/oop/basic/inherit/index.html)
- [多态](https://liaoxuefeng.com/books/java/oop/basic/polymorphic/index.html)
