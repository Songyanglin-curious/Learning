# Java 异常处理

学习入口：[廖雪峰《异常处理》](https://liaoxuefeng.com/books/java/exception/index.html)。本文记录目前已讨论的部分，重点保留 Java 与 C#、JavaScript 的差异，以及失败如何传递、由谁处理。

## 学习范围与进度

- 第一批（4.1～4.6）：已讨论异常分类、捕获与传播、手动抛出、异常包装、自定义异常，以及 finally 和资源自动关闭。
- 第一批待继续：NullPointerException 专题、断言 assert；finally 覆盖原结果、资源关闭失败的细节尚未展开。
- 第二批（4.7～4.10）：JDK Logging、Commons Logging、Log4j、SLF4J 与 Logback，尚未学习。

## 异常从哪里来，发生后怎样执行

异常表示当前操作无法正常完成。发现失败的可能是 JVM（例如对 null 调用实例方法）、库代码（例如把 abc 解析成整数），也可能是自己的业务判断。异常机制不会自动识别业务规则，需要代码明确判断。

抛出异常后，当前正常流程中断，沿调用链寻找匹配的 catch。失败语句后面的代码会跳过；catch 处理完后继续执行整个 try-catch 后面的代码，不会自动返回失败位置重试。若一直没有处理者，异常会到达当前线程的未捕获异常处理边界；Web 等框架也可能在外层统一处理。

```java
// 校验年龄；负数不符合要求时抛出参数异常。
static void checkAge(int age) {
    if (age < 0) {
        throw new IllegalArgumentException("年龄不能为负数：" + age);
    }
    System.out.println("年龄有效");
}
```

调用片段：

```java
try {
    checkAge(-5);
    System.out.println("校验结束");
} catch (IllegalArgumentException e) {
    System.out.println("校验失败：" + e.getMessage());
}
System.out.println("继续执行");
```

按控制流推导，会输出失败说明和“继续执行”，跳过“年龄有效”和“校验结束”。本文示例用于说明语义，本次未作为独立程序编译运行。

## throw 与 throws：实际动作和方法声明

```java
// 模拟加载失败，将 I/O 异常交给调用方。
static void load() throws IOException {
    throw new IOException("读取失败");
}
```

| 写法 | 作用 |
| --- | --- |
| static | 静态方法，与异常机制无关 |
| void | 正常完成时不返回值 |
| throws IOException | 方法声明：可能向外传递 IOException，调用方需要捕获或继续声明 |
| new IOException("读取失败") | 创建包含失败说明的异常对象 |
| throw | 实际抛出异常，中断正常执行流程 |

throws 不会主动制造异常，也不会把其他异常转换成 IOException。方法可以声明 throws IOException 却正常完成；如果内部发生 NullPointerException，向外传播的仍然是 NullPointerException。

返回类型也不因异常而改变：String 方法成功时返回字符串，抛出异常时则不正常返回。

## 异常类型与受检规则

Throwable 是 Java 可抛出对象的共同基类，其下主要分为 Error 与 Exception；IOException、RuntimeException 都继承自 Exception，NullPointerException、NumberFormatException 则属于 RuntimeException 的子类。

| 类别 | 例子 | 编译器是否强制捕获或声明 |
| --- | --- | --- |
| 受检异常 | IOException | 是 |
| 非受检异常：RuntimeException 及其子类 | IllegalArgumentException、NullPointerException | 否 |
| 非受检异常：Error 及其子类 | OutOfMemoryError | 否 |

“受检”检查的是代码有没有交代处理责任，异常仍发生在运行时。非受检异常也能捕获，但 Error 通常不适合当作普通业务失败处理。

throws 列表不一定穷尽方法可能发生的异常，因为非受检异常不要求声明。多个受检异常可以用逗号声明，例如 throws IOException, ParseException。

## 捕获多种失败，以及应该在哪层捕获

catch (IOException e) 中，IOException 是匹配类型，e 是本次异常对象的变量名。它能匹配 IOException 及其子类，不能匹配其他分支的异常。

下面读取端口号的示例需要导入 java.io.IOException、java.nio.file.Files、java.nio.file.Paths 和 java.nio.charset.StandardCharsets。

```java
// 从文件读取端口号；读取失败或数字格式错误时向外传播异常。
static int readPort() throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get("port.txt"));
    String text = new String(bytes, StandardCharsets.UTF_8);
    return Integer.parseInt(text.trim());
}
```

调用方可以区分处理：

```java
try {
    int port = readPort();
    System.out.println(port);
} catch (IOException e) {
    System.out.println("配置文件读取失败");
} catch (NumberFormatException e) {
    System.out.println("端口配置不是整数");
}
```

一次异常进入第一个类型匹配的 catch。若同时捕获父类和子类，子类应放在前面，否则会因后面的分支不可达而编译失败。

判断 API 可能抛什么异常，查看 IDE 方法声明和文档的 Throws；定位实际故障时，结合异常类型、消息、堆栈与原因链。

**在有能力决定失败后怎么办的那一层捕获。** 读取方法可能只负责读取，启动方法才知道能否使用默认配置；请求处理边界可以把失败转换为 HTTP 响应。处理也可以是终止操作，不一定要恢复执行。

如果当前层无法决定后续行为，可以不捕获，让异常直接传播；也可以捕获后补充业务含义并再次抛出。仅为消除编译报错而捕获、打印后继续提示成功，会掩盖失败。打印日志本身不等于完成了失败处理。

## 包装异常与堆栈保留

```java
// 读取配置；失败时补充启动语义，并保存原始原因。
static String readConfig() {
    try {
        byte[] bytes = Files.readAllBytes(Paths.get("config.txt"));
        return new String(bytes, StandardCharsets.UTF_8);
    } catch (IOException e) {
        throw new IllegalStateException("无法加载启动配置", e);
    }
}
```

第二个参数 e 将原异常保存为 cause。外层异常有包装位置的堆栈，原异常保留最初失败的堆栈。打印时通常通过 Caused by 展示原因链；“... n more”表示省略重复调用帧，不是丢失这些帧。

只写 new IllegalStateException(e.getMessage()) 会丢掉与原异常对象的关联，无法从新异常恢复原类型、堆栈和原因链。

常用排查入口：getMessage() 读取说明，getClass() 查看实际类型，getCause() 读取原始原因，printStackTrace() 打印堆栈和原因链。

Java 中 throw e; 重新抛出同一个异常对象，通常保留其原堆栈；没有 C# 的裸 throw; 语法。C# 中保留原堆栈的普通重新抛出写法是 throw;，throw ex; 会更新堆栈起点。

## finally 与资源自动关闭

```java
// 演示异常传播前仍执行 finally。
static void loadWithCleanup() throws IOException {
    try {
        throw new IOException("读取失败");
    } finally {
        System.out.println("执行清理");
    }
}
```

finally 负责离开代码块时执行清理，不代表捕获异常。正常结束、return 或异常离开时通常都会执行，但 JVM 被强制终止等情况不能保证。finally 中 return 或抛出新异常可能覆盖原结果，这部分待继续展开。

文件等资源优先使用 try-with-resources。下面示例另需导入 java.io.BufferedReader。

```java
// 读取第一行；离开 try 时自动关闭已成功创建的 reader。
static String readFirstLine() throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(
            Paths.get("config.txt"), StandardCharsets.UTF_8)) {
        return reader.readLine();
    }
}
```

try 后圆括号中的资源需要实现 AutoCloseable；正常返回或异常离开时自动调用 close()。这里可以没有 catch，关闭后异常仍能向外传播。关闭本身也可能失败，其处理机制待继续学习。

| Java | C# |
| --- | --- |
| try (资源声明) { ... } | using (资源声明) { ... } |
| AutoCloseable | IDisposable |
| close() | Dispose() |

可以把 Java 的这种写法对应到 C# using 来理解。垃圾回收主要管理对象内存，不能替代文件句柄、连接等资源的及时关闭。

## 自定义异常

自定义异常通过类型表达稳定的失败含义，使调用方无需解析错误文字就能区分处理。

```java
// 表示账户余额不足，无法完成扣款。
class InsufficientBalanceException extends RuntimeException {

    // 创建余额不足异常，保存失败说明。
    public InsufficientBalanceException(String message) {
        super(message);
    }
}

// 校验余额；不足以支付时抛出业务异常。
static void checkBalance(int balance, int amount) {
    if (amount > balance) {
        throw new InsufficientBalanceException("余额不足，请充值");
    }
}
```

这里是类定义与方法片段，checkBalance 需要放入使用它的类中。super(message) 调用父类构造方法保存说明，调用方可用 catch (InsufficientBalanceException e) 专门处理。

- 继承 Exception：属于受检异常，调用方必须捕获或声明。
- 继承 RuntimeException：属于非受检异常，不强制捕获或声明。

选择的关键是是否要强制调用方在代码中交代此类失败，而不是“业务异常固定继承哪一种”。需要包装原异常时，自定义异常也应提供接收 cause 的构造方法并传给父类。

无需为每条错误消息建立异常类。调用方需要区分处理，或失败本身有明确稳定的含义时，自定义类型才更有价值；普通参数错误通常可用 IllegalArgumentException。

## 与 C#、JavaScript 对照

| 对比点 | Java | C# | JavaScript |
| --- | --- | --- | --- |
| 类型捕获 | catch (IOException e) | catch (IOException e) | catch (e)，内部自行判断类型 |
| 受检异常 | 有，要求捕获或声明 | 无此语言规则 | 无此语言规则 |
| throws 声明 | 有，不是运行时抛出动作 | 无对应的语言声明 | 无对应的语言声明 |
| 抛出值 | Throwable 的实例 | Exception 派生类型实例 | 任意值，通常使用 Error |
| 原异常重新抛出 | throw e; | throw; | throw e; |

不必背异常类型大全与构造方法。需要理解控制流、处理责任、受检规则，以及包装后是否保留原始原因。

## 参考资料

- [Java 官方：捕获或声明要求](https://docs.oracle.com/javase/tutorial/essential/exceptions/catchOrDeclare.html)：受检异常与非受检异常。
- [Java 官方：声明抛出异常](https://docs.oracle.com/javase/tutorial/essential/exceptions/declaring.html)：throws 的作用。
- [Java 8 Throwable API](https://docs.oracle.com/javase/8/docs/api/java/lang/Throwable.html)：堆栈、cause 与异常链。
- [Java 官方：try-with-resources](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html)：资源自动关闭。
- [C# 官方：异常处理](https://learn.microsoft.com/en-us/dotnet/csharp/fundamentals/exceptions/)：与 C# 对照。
- [MDN：try...catch](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Statements/try...catch)：与 JavaScript 对照。
