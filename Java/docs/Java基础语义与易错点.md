# Java 基础语义与易错点

范围：教程 2.3.1—2.5.3。已有其他语言基础，重点理解运行语义和与 C# 的差异，常规语法按需查阅。参考定位：[Java 8 基础语法与运行语义](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/index.html)、[数组](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/arrays.html)、[字符串](https://docs.oracle.com/javase/tutorial/java/data/strings.html)、[传统 switch](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/switch.html)。

## 赋值、引用与数组

赋值复制变量的值。基本类型复制数据，引用类型复制引用，不会复制对象。“引用”可以借助地址理解，但 Java 不提供直接操作该地址的能力。

- 修改 `y[0]`，改变的是数组元素；让 `y` 引用新数组，改变的是变量的引用。
- `final` 限制变量不能重新赋值。`final` 数组的引用不能更换，但元素仍可修改。
- `Arrays.sort()` 修改原数组。先用 `Arrays.copyOf()` 复制，再排序，才能保留原数组的顺序。
- 二维数组是“数组的数组”，每行可以长度不同，也可以为 `null`。复制外层数组属于浅拷贝，内部各行仍然共享。
- 增强 `for` 把元素值赋给循环变量。给循环变量重新赋值不会替换原元素；若元素是对象引用，仍可通过引用修改对象。需要替换数组元素时，用下标明确赋值。

## 数值表达式先计算，再赋值转换

对这里的普通数值表达式，先按右侧操作数的类型和运算规则计算，再转换为左侧变量的类型。左侧类型无法挽回右侧已经发生的截断或溢出。

```java
double a = 5 / 2;            // 2.0：先做整数除法
double b = (double) 5 / 2;   // 2.5：先转为 double，再做除法
double c = (double) (5 / 2); // 2.0：除法完成后才转换

int n = 2_000_000_000;
long x = n + n;              // -294967296：int 运算已溢出
long y = (long) n + n;       // 4000000000：先提升为 long
```

`byte`、`short`、`char` 参与普通整数加法时，会先提升为 `int`。编译器通常按类型规则检查赋值，不根据普通变量当前的实际值放宽限制；编译期常量是一个例外，可以在结果不超出范围时隐式缩窄。

```java
byte a = 10;
byte b = 20;
// byte c = a + b; // 编译失败：结果类型是 int
byte d = 10 + 20;  // 可以：编译期常量 30 在 byte 范围内
```

浮点数有精度误差，除零也不会像整数那样抛异常：非零有限数除以零可能得到无穷大，零除以零得到 `NaN`。`NaN` 与自身用 `==` 比较也为 `false`，判断时使用 `Double.isNaN()`。没有异常，不代表结果是正常的有限数值。

## 相等判断：特别注意字符串

Java 引用类型的 `==` 判断是否为同一个对象；`equals()` 的规则由类的实现决定。

| 情况 | Java | C# |
| --- | --- | --- |
| 两个字符串使用 `==` | 比较引用 | 两侧为 string 类型时比较内容 |
| 字符串的 `equals/Equals` | 比较内容 | 比较内容 |
| 普通类未自定义相等规则 | 默认比较引用 | 默认比较引用 |

Java 不允许类重载 `==`；C# 允许。不能将 C# 字符串的 `==` 使用习惯直接带入 Java。

```java
String a = "hello";
String b = new String("hello");

// a == b 为 false，a.equals(b) 为 true
```

`Object.equals()` 默认比较引用，`String` 重写了它来比较内容。数组没有重写这一规则，比较数组元素内容使用 `Arrays.equals()`。

## char 与 String

`char` 是基本类型，保存一个 16 位 UTF-16 代码单元，用单引号表示；`String` 是引用类型，表示不可变的文本，用双引号表示，可以为空字符串或 `null`。

一个可见字符不一定只占一个 `char`。例如 `"😀".length()` 为 `2`；`length()` 统计代码单元，`charAt()` 也只取一个代码单元。截断文本或限制字数时，需要明确计数规则。

字符串拼接、替换不会修改原对象，要接收返回的结果。简单拼接用 `+` 即可；循环中持续追加通常用 `StringBuilder`。不要把“字符串不可变”理解成“每个 `+` 都必定创建一个新对象”，编译器可能优化拼接。这些行为与 C# 大体一致。

## Java 8 的 switch

传统 `switch` 匹配分支后会继续执行，直到遇到 `break` 等退出语句，不会重新判断后续 `case` 是否匹配。普通分支明确写 `break`，避免意外贯穿。

当前使用 Java 8；教程中的 `var`、文本块、新版 `switch` 等语法不适用于这个环境，阅读时注意版本边界。
