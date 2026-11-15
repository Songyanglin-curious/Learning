# Java 程序的编译与运行

## 文件与工具的职责

| 名称 | 本质与职责 |
| --- | --- |
| `.java` 文件 | 源码文本，按 Java 语法描述程序要执行的逻辑，供人阅读和修改 |
| `.class` 文件 | 编译生成的二进制文件，包含 JVM 字节码及类的结构等信息，供 JVM 加载和执行 |
| `javac.exe` | Java 编译器，读取 `.java`，检查语法和类型等规则，生成 `.class` |
| `java.exe` | Java 启动器，启动 JVM，由 JVM 加载类并执行程序 |

`.java` 与 `.class` 是程序在不同阶段的表示：前者是源码，后者是编译产物。`.class` 不是普通文本，也不是 CPU 可以直接执行的本机机器码。

## 程序如何运行

```mermaid
flowchart LR
    A["源码文件<br/>Hello.java"] -->|javac.exe 编译| B["字节码文件<br/>Hello.class"]
    B -->|java.exe 启动 JVM，由 JVM 加载并执行| C(["运行状态<br/>程序逻辑正在执行"])
```

本例先由 `javac.exe` 把源码编译成字节码，再由 `java.exe` 启动 JVM。JVM 加载 `Hello` 类，从 `main` 方法开始执行，按代码逻辑输出问候。

执行时，JVM 可以解释执行字节码，也可以将部分字节码即时编译成机器码执行，最终由 CPU 完成运算。
