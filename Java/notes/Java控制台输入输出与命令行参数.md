# Java 控制台输入输出与命令行参数

## 学习来源

- [廖雪峰：输入和输出](https://liaoxuefeng.com/books/java/quick-start/flow/input-output/index.html)
- [廖雪峰：命令行参数](https://liaoxuefeng.com/books/java/quick-start/array-op/args/index.html)

## 两种独立的输入入口

命令行参数在启动程序时提供，通过 `main(String[] args)` 接收；标准输入在程序运行时读取，通常通过 `System.in` 获取。两者互相独立。

执行 `java Main Alice`，`args[0]` 是 `"Alice"`。随后调用 `scanner.nextLine()`，仍然需要从标准输入另外读取，不会读到这个启动参数。

标准输入通常连接键盘，也可以通过重定向或管道连接文件、其他程序。标准输出通常连接终端，也可以重定向。

## 为什么需要 Scanner：与 C# 对照

```java
Scanner scanner = new Scanner(System.in);
```

`System.in` 是标准输入的字节流；`Scanner` 包装这个流，提供文本识别和数据解析能力，例如 `nextLine()` 读取一行文本，`nextInt()` 解析一个整数。

Java 可以直接读取 `System.in`，但需要自己处理更底层的字节和解析细节。`Scanner` 也可以读取文件等其他来源，并非键盘专用工具。

| 需求 | Java | C# |
| --- | --- | --- |
| 获取标准输入的字节流 | `System.in` | `Console.OpenStandardInput()` |
| 读取一行文本 | `scanner.nextLine()` | `Console.ReadLine()` |
| 读取并解析整数 | `scanner.nextInt()` | `int.Parse(Console.ReadLine())` |

C# 的 `Console.In` 是 `TextReader`，`Console.ReadLine()` 通过它读取文本；Java 的 `System.in` 提供字节流，需要另选文本读取工具。这里是标准库封装层次的差异，无须据此判断优劣。

## 命令行参数的本质

在 Java 程序中，命令行参数就是传给 `main` 的字符串数组。

例如执行：

```text
java Main --port 8080
```

得到两个字符串：`args[0]` 是 `"--port"`，`args[1]` 是 `"8080"`。`java` 和 `Main` 不在数组中；没有传参数时，数组长度为 `0`。

“端口选项”“整数 8080”等含义由程序或参数解析库赋予，Java 不会自动识别这些业务含义。需要数字时，必须显式解析，例如 `Integer.parseInt(args[1])`。

含空格的参数通常需要用引号分组，例如 `java Main "Alice Smith"`。正常情况下，程序收到一个字符串 `Alice Smith`，分组用的引号不属于字符串内容。

命令行参数适合指定启动选项、文件和运行模式，便于脚本调用；标准输入适合交互问答，也可接收管道数据。一个程序可以同时使用两者。

## 值得留意的行为

- `nextInt()` 读取下一个整数项，不负责消耗整行。输入整数并回车后，紧接着调用 `nextLine()`，可能读到当前行剩余的空内容。交互输入可以统一用 `nextLine()` 读取，再按需解析数字。
- 教程把 `nextInt()` 注释为“读取一行输入”，不够准确，应按“读取下一个整数项”理解。
- `print()` 输出后不自动换行，`println()` 输出后换行，`printf()` 按格式字符串输出。
- `printf("%.2f", value)` 控制显示的小数位数，不修改原值，也不消除浮点数的精度误差。格式占位符与参数类型不匹配，可能在运行时抛异常。
