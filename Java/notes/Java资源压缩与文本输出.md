# Java 资源、压缩与文本输出

更新日期：2026-11-10。

本笔记整理 IO 三条主线之后的 classpath 资源、ZIP 和 Print 系列。序列化单独见 [Java序列化与跨语言对比](Java序列化与跨语言对比.md)，本轮总入口与完成状态见 [JavaIO学习计划](JavaIO学习计划.md)。内容来自讲解与讨论，示例未运行，不作为实验结果。

## 一、classpath 资源：按资源名读取发布内容

classpath 除了查找类，也可以查找配置、图片、模板等资源。资源可以位于 classpath 目录，也可以是 JAR 内部条目。资源名描述查找范围内的名称，与按工作目录解释的普通相对文件路径不同。

以 Maven 默认构建约定为例：

| 阶段 | 配置资源的位置 |
| --- | --- |
| 项目源码 | `src/main/resources/config/default.properties` |
| 默认构建输出 | `target/classes/config/default.properties` |
| 普通 JAR 内部 | `config/default.properties` |

构建工具负责复制、打包，运行时 API 负责按资源名查找。`src/main/resources` 是 Maven 约定，不是 JVM 自动扫描的特殊目录；资源目录也可以通过构建配置调整。

```java
// 取得资源输入流，处理完后自动关闭
try (InputStream input =
        App.class.getResourceAsStream("/config/default.properties")) {
    if (input == null) {
        throw new IOException("找不到默认配置资源");
    }
    // 后续按普通输入流处理内容
}
```

假设 `App` 位于 `com.example` 包：

| 查找方式 | 资源名规则 |
| --- | --- |
| `App.class.getResourceAsStream("/config/default.properties")` | 根资源名 `config/default.properties` |
| `App.class.getResourceAsStream("config/default.properties")` | 补上类的包路径，查找 `com/example/config/default.properties` |
| `App.class.getClassLoader().getResourceAsStream("config/default.properties")` | 类加载器方式使用根资源名，不加开头的 `/` |

`Class` 方式的 `/` 不是磁盘根目录；不带 `/` 时相对的是该类的包，不是源码目录或工作目录。这里按普通 classpath 场景理解，命名模块和自定义类加载器的额外规则按需查。

找不到资源时可能返回 `null`。`App.class` 是查找上下文，不保证只查 App 所在的 JAR；普通 classpath 可以包含多个目录和 JAR，同名资源可能被遮蔽，具体受类加载器规则影响。

资源位于 JAR 内时，不是普通文件系统中的独立文件，不能假定资源 URL 一定能直接转换为普通本地 `File`。输入流使调用者可以统一读取内容，而不必绑定存储形式。

遇到“源码里有，运行找不到”，依次核对实际运行产物中有没有该资源、内部路径是什么、代码使用的查找方式与名称是否对应。仅检查源码目录不足以判断部署后的行为。

本轮只保留内置资源与外部文件的能力区别。默认配置覆盖外部配置、部署组织等属于业务设计，不作为本章继续展开的主线。

## 二、ZIP：条目内容、元信息与归档结构

ZIP 归档包含多个条目。每个条目有名称、压缩方式、大小、校验等元信息，以及文件内容；内容可以压缩，也可以不压缩。整个归档还有目录等组织信息。

| 类型 | 职责 |
| --- | --- |
| `ZipEntry` | 描述条目，如 `docs/a.txt`、目录条目 `empty/` |
| `ZipInputStream` | 从输入流顺序进入各条目，读取当前条目的解压后字节 |
| `ZipOutputStream` | 向输出流写入条目及其内容，完成 ZIP 结构 |
| `ZipFile` | 打开已有 ZIP 文件，可按条目名称查找并取得内容输入流 |

`ZipEntry` 本身不装着整份内容。条目名是包内名称，不会因被读取就自动创建磁盘文件或目录；读取 ZIP 内容也不要求先解压到磁盘。文本条目的内容仍需要按自身编码解码。

读取过程是“外层换条目，内层读当前内容”：

```java
// 顺序读取归档中的每个条目
try (InputStream file = Files.newInputStream(zipPath);
     ZipInputStream zip = new ZipInputStream(file)) {
    byte[] buffer = new byte[8192];
    ZipEntry entry;
    while ((entry = zip.getNextEntry()) != null) {
        System.out.println(entry.getName());
        if (!entry.isDirectory()) {
            int count;
            while ((count = zip.read(buffer)) != -1) {
                // 只处理本次前 count 个解压后的字节
            }
        }
        zip.closeEntry();
    }
}
```

- `getNextEntry()` 定位到下一条目的数据，返回 `null` 表示没有更多条目。
- `zip.read(...)` 返回 `-1` 表示当前条目的内容结束，不等于归档没有下一条目。
- `closeEntry()` 结束当前条目的读取，为下一条目做准备，不关闭整个归档流。
- 只处理实际返回数量以内的数据，不依赖缓冲区剩余位置的内容。

写入过程是“开始条目 → 写内容 → 结束条目”，多条目时重复此过程：

```java
// 写入一个文本条目，退出时完成归档并关闭底层流
try (OutputStream file = Files.newOutputStream(zipPath);
     ZipOutputStream zip = new ZipOutputStream(file)) {
    zip.putNextEntry(new ZipEntry("docs/a.txt"));
    zip.write("hello".getBytes(StandardCharsets.UTF_8));
    zip.closeEntry();
}
```

大内容可以分批写，不要求先整体读入内存。收尾方法有不同层次：

| 操作 | 作用 |
| --- | --- |
| `closeEntry()` | 完成当前条目，还能继续添加条目 |
| `finish()` | 完成整个 ZIP 结构，不关闭底层流 |
| `close()` | 完成 ZIP 并关闭底层流 |
| `flush()` | 刷新输出，不等于完成归档结构 |

JAR 基于 ZIP 格式，有额外的 Java 元信息约定。理解包与条目有助于理解 JAR，但不要求应用手动解压 JAR 才能加载类或资源。压缩算法、格式字段和调参不作为本轮必学内容。

## 三、PrintStream 与 PrintWriter：便利的文本输出层

两者都提供 `print`、`println`、`printf`／`format`，把数值、布尔值、对象等表示成文本，并提供换行、格式化。`System.out` 和 `System.err` 是 `PrintStream`，输出目标不限于控制台，取决于连接的底层资源。

| 类型 | 所属体系与能力 |
| --- | --- |
| `PrintStream` | 字节流体系；打印字符时编码成字节，也保留原始字节写入能力 |
| `PrintWriter` | 字符流体系；包装 `Writer`，或接收字节流并建立编码层，提供文本输出 |

```java
System.out.print(123);     // 文本“123”，不换行
System.out.println(true); // 文本“true”，再换行
System.out.printf("姓名：%s，年龄：%d%n", "张三", 20); // 格式化输出
```

`print(object)` 通常依据 `toString()`，不是对象序列化，不保证可以据此恢复对象。输出到字节目标仍涉及编码。

这类输出方法通常把 IO 失败记入内部错误状态，通过 `checkError()` 检查，不直接向外抛出 `IOException`；部分构造方法仍可能抛出 IO 异常。格式串错误等运行时异常不受“不抛 IO 异常”这条约定保护。

换行与自动刷新不同。自动刷新需要启用，规则也不完全相同：

- `PrintWriter` 的自动刷新由 `println`、`printf`、`format` 调用触发，单纯 `print("\n")` 不因包含换行而触发这条规则。
- `PrintStream` 的自动刷新触发还包括字节数组写入、换行字符／字节等情况，具体按方法约定判断。
- 需要明确交付下层时可调用 `flush()`，但仍不因此保证可靠落盘。关闭责任遵循已有流的归属规则。

与 C 的打印能力对照，根本用途相同。C 用 `printf` 输出标准输出、`fprintf` 指定 `FILE*`；Java 对具体流对象调用方法。C 的标准 IO 同样有缓冲与刷新。

Java 有 `print(int)` 等重载和对象文本表示；格式化调用借鉴 C，但规则不完全相同。例如 Java `%n` 是平台换行符，不能照搬 C 中同名转换的含义。格式与参数类型不匹配时，Java 通常运行时报告格式异常，C 的可变参数类型不匹配则可能产生未定义行为。具体占位符按需查阅。

## 本轮学习边界

上述内容已完成当前范围的能力与机制讨论，不默认扩展业务配置方案、部署架构、压缩算法或复杂格式实现。学习侧重 Java 自身的规则、限制和与已有知识的差异，常规 API 按需查。

## 参考入口

- [廖雪峰：读取 classpath 资源](https://liaoxuefeng.com/books/java/io/classpath/index.html)、[操作 ZIP](https://liaoxuefeng.com/books/java/io/zip/index.html)、[PrintStream 和 PrintWriter](https://liaoxuefeng.com/books/java/io/print/index.html)。
- [Java：Class.getResourceAsStream](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Class.html#getResourceAsStream(java.lang.String))、[ClassLoader](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/ClassLoader.html)：资源名与查找上下文。
- [Maven 标准目录](https://maven.apache.org/guides/introduction/introduction-to-the-standard-directory-layout.html)、[资源目录配置](https://maven.apache.org/plugins/maven-resources-plugin/examples/resource-directory.html)：构建工具的复制与打包约定。
- [Java：ZipInputStream](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/zip/ZipInputStream.html)、[ZipOutputStream](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/zip/ZipOutputStream.html)、[ZipFile](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/zip/ZipFile.html)：条目访问和归档完成边界。
- [Java：PrintStream](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/io/PrintStream.html)、[PrintWriter](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/io/PrintWriter.html)、[Formatter](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Formatter.html)：输出、自动刷新与错误报告。
- [Linux C 库 printf 文档](https://man7.org/linux/man-pages/man3/printf.3.html)：C 侧格式化输出对照。

Java API 参考使用 Java SE 25；具体方法与构建布局以项目实际版本、配置为准。
