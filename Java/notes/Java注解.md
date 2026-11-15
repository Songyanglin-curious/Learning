# Java 注解

## 核心理解：数据与处理者

注解是附着在类、字段、方法等代码元素上的结构化元数据。它可以只是标记，也可以携带参数。注解声明本身不包含自动执行的业务行为；编译器、工具或框架读取它后，才产生具体效果。

看到陌生注解，优先问：谁读取？什么时候读取？读取后做什么？什么条件下才生效？

例如 `@Range(max = 150)` 只表达范围约束。没有处理逻辑，字段仍然可以被赋值为 200；即使有处理逻辑，也要进入对应调用流程才会检查。校验某一时刻的值，也不等于拦截此后所有赋值。

## 定义与使用

以下是声明片段，不是独立运行程序：

```java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Range {
    // 声明允许的最小值，默认值为 0。
    int min() default 0;

    // 声明允许的最大值，使用时必须提供。
    int max();
}
```

使用时写 `@Range(max = 150)`，等价于 `@Range(min = 0, max = 150)`。

- `@interface` 声明特殊的注解接口，不是普通业务接口。
- `min()`、`max()` 是注解元素的声明，形式像无参方法，但没有校验方法体。通过注解对象调用它们时，读出的是配置值。
- `default` 指定省略该元素时的默认值，没有默认值的元素必须填写。
- 元素类型受限制：可用基本类型、String、Class、枚举、注解，以及这些类型的一维数组；不能任意传业务对象，不能用 null 作为元素值。

### value 的简写

只给名为 `value` 的元素赋值时，可以省略 `value =`：`@Label("用户")` 等价于 `@Label(value = "用户")`。其他元素如果有默认值并省略，也能使用这个简写。

这是语法糖，不改变处理方式。不是“只有一个元素就能省略名字”：`max` 不叫 `value`，所以 `@Range(150)` 不合法。

## 谁处理注解，是否需要反射

| 时机 | 处理方式 | 边界 |
| --- | --- | --- |
| 编译时 | 编译器识别内置注解，如 `@Override` | 可以进行检查，不必生成额外业务代码 |
| 编译时 | 配置好的注解处理器读取注解，生成源文件等产物 | 处理器需要有人实现；生成代码不代表自动执行 |
| 运行时 | 框架或自己的代码通过反射读取注解 | 通常要求 RUNTIME 保留策略，并有处理入口 |

标准 Java 注解处理器通常生成新的源文件等产物，不应理解为“随便给原来的方法插入一段代码”；修改现有语法树或字节码属于其他工具机制。

例如处理器生成 `PersonValidator`，其中直接读取字段并检查范围，运行时就可以执行普通代码，不再反射读取注解。但程序或框架仍要调用生成的校验器。

运行时校验的概念流程：取得字段描述 → 读取 Range 注解 → 读取 min/max → 读取目标对象的字段值 → 比较并决定如何处理。

`field.getAnnotation(Range.class)` 获取注解，`range.max()` 读取上限；比较与抛异常才是校验行为。JVM 不会看到自定义 Range 就自动安排这些步骤。

框架让“加上注解就生效”，是因为它已将处理逻辑放进请求处理、对象创建等流程。使用者不必亲自调用校验函数，但框架内部仍有调用入口。

## 元注解：位置与保留时间

元注解用于描述其他注解。

`@Target(ElementType.FIELD)` 限定只能标在字段上，写在不允许的位置会编译报错。允许多个位置可以写 `@Target({ElementType.FIELD, ElementType.PARAMETER})`。

允许写在参数上，不代表已有的字段校验器会处理参数；处理者还要实现对应的读取逻辑。

| Retention 策略 | 源码中 | 编译后的 class 中 | 运行时通过反射读取 |
| --- | --- | --- | --- |
| SOURCE | 有 | 无 | 不能 |
| CLASS | 有 | 有 | 不能 |
| RUNTIME | 有 | 有 | 能 |

不声明 `@Retention` 时默认是 CLASS。`@Override` 使用 SOURCE；运行时通过反射读取的 Range 需要 RUNTIME。RUNTIME 只是保证可读取，不会自动执行校验。

## 重写与注解继承是两件事

Java 方法是否重写，取决于是否满足语言的重写规则，不由 `@Override` 决定。删掉正确重写方法上的 `@Override`，仍会进行多态调用；加上它的价值是让编译器检查“确实发生了重写”，帮助发现方法名或签名写错等问题。

方法上的注解不会自动复制到子类重写的方法上。反射读取子类自己声明的方法，不会自动得到父类方法上的注解。框架可能自行沿父类或接口查找，这是框架额外的规则。

`@Inherited` 只影响类上的注解沿父类链的继承查找，不适用于方法，也不沿接口查找。不要把它理解成通用的注解继承开关。

## 与 C# Attribute 对比

二者核心角色接近：给代码附加元数据，由处理者赋予效果。定义机制不同，不能仅凭外观判断其作用。

| 对比点 | Java 注解 | C# Attribute |
| --- | --- | --- |
| 定义 | 特殊的 @interface | 继承 System.Attribute 的类 |
| 数据入口 | 注解元素与默认值 | 构造参数，以及命名的公共可写属性或字段 |
| 自定义行为 | 不能写普通方法体或构造方法 | 可以写构造方法、普通方法 |
| 使用位置限制 | @Target | AttributeUsage |
| 保留时间 | SOURCE / CLASS / RUNTIME | 没有直接对应的这套三档策略 |
| 运行时读取 | 反射取得注解表示并读取值 | 可反射实例化 Attribute，也可只读声明数据 |

C# 的 `[Range(150)]` 通常对应 `RangeAttribute` 类的构造参数，使用时可以省略 Attribute 后缀；Java 的 `@Range(max = 150)` 则给名为 max 的注解元素赋值。

写下 C# Attribute 不会在每次创建业务对象时执行其构造方法。编译器记录类型与参数等元数据；通过 GetCustomAttributes 等 API 获取 Attribute 实例时，会执行构造方法。CustomAttributeData 可以读取声明数据而不实例化 Attribute。

即使 Attribute 类有 Validate 方法，贴上标记也不会自动调用它；仍要由框架或自己的代码调用。

重写语法也要分开：Java 的 `@Override` 是检查注解；C# 的 `override` 是语言关键字。C# 普通实例方法默认不能被重写，需要父方法为 virtual、abstract 或可继续重写的 override；同名 new 方法是隐藏，调用规则不同。

### 如何评价取舍

Java 注解的行为边界较窄，但“能力受限”不等于“整套机制更简单”。特殊声明语法、保留策略和处理器仍有理解成本，部分复杂度放在了处理者一侧。

C# 复用类、构造方法和属性，对熟悉面向对象的人更统一，也更灵活；获取 Attribute 实例可能执行代码，需要留意副作用。

因此不能简单断言哪一个全面更好：比较的是元数据声明的约束、语言一致性和处理机制，而不是语法长短。

## 参考资料

- [廖雪峰：注解（6.1–6.3）](https://liaoxuefeng.com/books/java/annotation/index.html)：学习入口。
- [Java 语言规范第 9 章](https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6)：注解接口、元素、元注解与简写规则。
- [Oracle：注解概览](https://docs.oracle.com/javase/tutorial/java/annotations/)：区分编译期与运行时用途，页面使用 JDK 8 教程背景。
- [Microsoft：Attribute 与反射](https://learn.microsoft.com/en-us/dotnet/csharp/advanced-topics/reflection-and-attributes/)：C# 元数据声明与读取。
- [Microsoft：访问自定义 Attribute](https://learn.microsoft.com/en-us/dotnet/csharp/advanced-topics/reflection-and-attributes/accessing-attributes-by-using-reflection)：实例化与构造方法执行。
- [Microsoft：CustomAttributeData](https://learn.microsoft.com/en-us/dotnet/api/system.reflection.customattributedata)：不实例化 Attribute 的数据读取方式。

本笔记整理讨论与文档规则；代码为说明片段，未执行运行验证。Java 25 规范链接作为参考，不代表项目已指定或验证使用该 JDK。
