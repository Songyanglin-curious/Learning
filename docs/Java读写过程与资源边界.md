# Java 读写过程与资源边界

更新日期：2026-11-10。

本笔记总结 IO 第三条主线，覆盖实际读取数量、缓冲与包装、`write`／`flush`／`close` 的保证，以及资源归属和关闭异常。内容来自讲解与讨论，未执行读写实验；示例用于说明规则，不作为运行验证结果。

前置内容见 [Java文件与路径](Java文件与路径.md)、[Java字节与文字读写](Java字节与文字读写.md)，整体进度见 [JavaIO学习计划](JavaIO学习计划.md)。异常基础见 [Java异常处理](Java异常处理.md)。

## 一、实际读取数量与结束标志

先区分无参数读取与批量读取：

| 调用 | 正常返回什么 | 结束标志 |
| --- | --- | --- |
| `InputStream.read()` | 一个字节的值，以 `int` 表示，范围为 0～255 | `-1` |
| `InputStream.read(byte[] buffer)` | 本次实际读到的字节数，数据写进传入数组 | `-1` |

```java
byte[] buffer = new byte[8];

// 重复使用同一个数组，只处理本次实际读取的范围
try (InputStream input = Files.newInputStream(path)) {
    int count;
    while ((count = input.read(buffer)) != -1) {
        System.out.println("本次读取：" + count + " 字节");
        // 本次数据在 buffer[0] 到 buffer[count - 1] 中
    }
}
```

`buffer.length` 是容量，`count` 是本次实际数量。若容量为 8，本次返回 3，只有下标 0、1、2 是本次写入的数据；后面的元素不被本次读取修改，可能保留上次的数据。数组不会自动缩成长度 3，处理时不能每次都使用整个数组。

对长度非零的数组，`InputStream.read(buffer)` 正常返回正数或 `-1`。长度为零时可返回 0，不能把 0 当作这里的结束标志。上述约定针对 `InputStream`，不直接套用于所有其他 IO API。

## 二、文件短读与 TCP 的区别

本轮曾将“读不满”误解为“文件还没读完却返回了 `-1`”，随后已澄清：

- 返回正数：本次有数据，可能少于请求数量。
- 返回 `-1`：该流报告已经到达末尾，不是“数据暂时没准备好”。
- 读取失败通常报告异常，不是用 `-1` 代替所有错误。

对于大小稳定、未被修改的普通本地文件，使用正常大小同步读取时，常见表现是前面的批次读满，最后一批可能不足。假设文件 20 字节、缓冲容量 8，典型返回序列是 8、8、4、-1。返回 4 的那次仍成功交付了数据，下一次才报告已经没有数据。

Java 的文件流 API 只承诺读取最多请求数量，并未保证所有平台和环境都一定填满。底层读取的特殊条件、不同底层对象等都可能影响数量；例如 Linux 底层读取允许在已经读到部分数据后受到信号影响而返回已读数量。因此，通用代码仍按实际数量处理，并以 `-1` 判断结束。

不能把普通文件短读笼统解释为“磁盘慢，所以暂时只交出几个字节”。普通同步文件读取通常会等待所需 IO 完成；操作系统缓存未命中不等于必须向调用者返回短读。

阻塞式 TCP 输入流暂时没数据时通常等待。已接收数据读完，且对端正常结束发送方向后，才会读到末尾；`-1` 不是某一批网络数据还没到达的标志。本轮只用它作语义对照，不展开网络协议。

## 三、缓冲怎样减少小操作

缓冲是在业务与底层读写之间暂存一批数据，把多次小操作与较大批次的底层操作衔接起来。

输入缓冲会按需预读一批，暂存在内部数组中。业务逐字节调用外层 `read()` 时，只要缓冲还有数据，就从内部取；用完后再向底层读取。例如内部一次取得 8 字节，业务接下来的若干次单字节读取可以复用这批数据，不必每次都调用底层读取。

输出缓冲方向相反：先接收多次小写入，暂存在内部数组中，适时成批交给底层。并非所有大写入都必须先复制进内部缓冲，具体实现可能直接交给底层。

| 缓冲位置 | 谁管理 | 用途 |
| --- | --- | --- |
| 业务创建的 `byte[]`，配合 `read(buffer)` | 业务代码 | 主动按块读取与处理 |
| `BufferedInputStream` 的内部数组 | 包装流 | 字节预读，支持外部小量读取 |
| `BufferedReader` 的内部缓冲 | 字符包装流 | 字符预读、逐字符和逐行读取 |
| `BufferedOutputStream`／`BufferedWriter` 的内部缓冲 | 包装流 | 暂存小量输出，批量向下写出 |

缓冲减少的是相关的底层调用和处理开销，不能理解成“不加 Java 缓冲，每个字节都一定触发物理磁盘操作”：操作系统还有缓存。

业务本来就在较大批次读写时，再包一层不一定继续明显提速。默认缓冲大小通常可以先使用，不固定要求为每个任务调参。`Files.newBufferedReader` 已经提供带缓冲的字符读取入口。

## 四、包装是对象引用与调用的组合

包装是外层对象持有内层对象的引用，复用内层读写，并增加处理。变量指向外层，业务调用先进入外层；外层按需要再调用底层。两层处理同一条数据流，不是各自重新读取一遍文件。

| 包装类型 | 复用的内层 | 增加什么 |
| --- | --- | --- |
| `BufferedInputStream` | `InputStream` | 字节缓冲，仍提供字节流接口 |
| `InputStreamReader` | `InputStream` | 解码，提供字符流接口 |
| `BufferedReader` | `Reader` | 字符缓冲与逐行读取 |

给流加缓冲、保持原接口的包装属于装饰器思路；`InputStreamReader` 将字节接口转换为字符接口，更接近适配器。“包装”是较宽泛的组合描述，不等于所有包装都属于同一种模式。

对这些标准缓冲包装，构造后通过外层统一读写；绕开外层直接使用底层可能破坏预读、缓冲与顺序的配合。

## 五、write、flush 与数据所在层次

以 `BufferedOutputStream` 包装文件输出流为例：

| 操作 | 保证层次 |
| --- | --- |
| `write()` | 按当前流的约定接收数据；可能只存入 Java 缓冲，也可能已经交给底层 |
| `flush()` | 将这层待输出字节写给底层，并刷新下层；保持流可继续使用 |
| `close()` | 执行关闭收尾，释放资源；常见缓冲输出流会尝试先刷新 |

```java
// 小量写入可能先留在这层 Java 缓冲中
try (OutputStream out = new BufferedOutputStream(
        Files.newOutputStream(path), 8192)) {
    out.write("hello".getBytes(StandardCharsets.UTF_8));
    out.flush(); // 要求把待输出的数据继续交给底层
}
```

若 5 字节的 `hello` 还留在该 Java 对象内部，另一个进程即使能打开文件，也读不到这部分尚未提交的数据。缓冲流不会因为业务暂时没继续写，就自动判断应当立即刷新。

本轮对分层的认识是：调用者不能仅凭 `write()` 成功就判断数据已经到达文件系统。具体流实现知道自己执行了什么，不能把它理解为“流自己也不知道”。业务可以不关心内部细节，但需要关心 API 提供的完成保证。

`flush` 的“清空缓冲”应理解为先写给下层，再腾出待输出空间，不是丢弃数据。成功后内部数组通常仍保留供复用，不等于释放内存或把数组内容全部置零。

另需区分可见性与持久性：

- 普通本地文件的数据成功交给文件系统后，即使仍在操作系统缓存中，其他进程通常也可以通过正常文件读取看到它。
- `flush()` 不保证物理存储已完成写入，也不提供断电后仍保存的保证。更强的持久性要求按需研究，本轮不展开实现。
- 别的程序若保留自己内存中的旧内容，刷新输出不会强迫它重新加载。

## 六、多层流的关闭与资源归属

对常见标准包装流，关闭最外层会连带调用底层的关闭，这是具体类的实现约定，不是包装语法自动产生的效果。

例如字符输出的常见组合是：外层 `BufferedWriter` 持有 `OutputStreamWriter`，后者持有文件字节输出流。正常关闭时，外层先将字符交给内层，再逐层完成编码输出与关闭，最终释放文件句柄。

由当前代码打开并负责整个生命周期、且正常构造完成的组合，通常可以统一管理最外层。若先关闭底层，外层剩余数据再尝试写出就可能失败。

“谁调用谁的关闭”与“谁有权关闭”不同：自己打开且独占管理的文件流，可以自己负责结束；若方法只借用调用者传入的流，调用者后面还要使用，就不能擅自关闭它。即使只关闭自己创建的包装对象，也可能连带关闭借来的底层资源，必须依据关闭责任约定处理。

关闭后不应继续使用这条文件流链。关闭不等于文件被删除，也不等于已经完成可靠落盘。资源关闭与 Java 对象是否仍可达是不同事情，不应依赖 GC 替代明确的资源管理。

## 七、为什么关闭也会报错

`close()` 不只是把对象标记为关闭，还可能继续执行实际 IO。例如输出缓冲仍有数据，需要写出；磁盘空间不足、设备故障等可能使这一步抛出 `IOException`。

正文异常与关闭异常可以来自两个阶段：

1. 程序把文本存进输出缓冲，`write()` 返回。
2. 接下来解析业务数字，因非法内容抛出 `NumberFormatException`。
3. 异常退出正文，自动关闭资源时尝试把剩余缓冲写出。
4. 此时磁盘空间不足，关闭又抛出 `IOException`。

正文失败不意味着收尾工作无事可做，收尾工作也可能失败。`try-with-resources` 在这些正常控制流与异常退出路径中保证尝试关闭已管理资源，不保证外部设备配合成功，也不是回滚机制。已写出的数据不会因后续失败自动恢复。

## 八、主异常、suppressed 与堆栈信息

`try-with-resources` 按以下规则向外报告异常：

| 正文 | 关闭 | 向外报告 |
| --- | --- | --- |
| 失败 | 成功 | 正文异常 |
| 成功 | 失败 | 关闭异常 |
| 失败 | 失败 | 正文异常为主，关闭异常附加到其 suppressed 列表 |

刚才的例子中，外层接到 `NumberFormatException`，可以通过它的 `getSuppressed()` 取得关闭时的 `IOException`。`printStackTrace()` 会同时展示相关异常和各自堆栈，输出结构可能类似：

```text
NumberFormatException: 非法数字
    ...正文出错的位置...
    Suppressed: IOException: 磁盘空间不足
        ...关闭出错的位置...
```

这里要区分三种关系：

- 堆栈信息：某个异常发生时的调用位置与路径。
- suppressed 列表：主异常上关联的其他异常对象，每个对象也有自己的堆栈。
- cause：异常之间的原因关联，不等于本例中先后独立发生的正文错误与清理错误。

因此，不是把两个错误合成同一条调用栈，也不是简单的先进后出异常栈。保留正文异常为主，是为了保留首先使正文失败的信息，同时附带清理错误，避免后来发生的关闭错误遮住前面的失败。

## 九、多个资源与初始化边界

对分别声明在 `try (...)` 中的资源，按声明顺序初始化，按相反顺序关闭。资源初始化为非空并成功受该语句管理后，才参与自动关闭。

| 情况 | 规则 |
| --- | --- |
| 先初始化 A，再初始化 B，全部成功 | 退出时先关 B，再关 A |
| A 成功，B 初始化抛出异常 | 不进入正文，仍尝试关闭 A |
| 关闭 B 抛出异常 | 仍尝试关闭 A，保留相关异常 |
| 正文成功，但多个关闭都失败 | 按关闭顺序首先抛出的关闭异常为主，其余作为 suppressed |

初始化异常存在时，后续关闭异常同样附加在原异常上。

嵌套构造需要另外留意：`try (Outer out = new Outer(new Inner(...)))` 只有整个初始化成功返回后，外层资源才被登记。若内层已经取得资源、外层构造却失败，不能假定语法会自动找到并关闭内层。需要由构造器约定清理，或将需要保护的资源分别纳入管理。本轮只指出该边界，未展开复杂封装实现。

## 学习状态与后续范围

第三条主线已完成概念讨论，可先告一段落；多资源初始化与关闭顺序已给出规则，未做故障注入或真实文件实验。学习中已修正“未读完就可能因为暂时没有数据而返回 -1”的误解，并区分了数据分层、可见性与持久性。

本轮学习异常的顺序是先解释错误怎样产生，再看如何保留和交付，最后解释处理规则的理由。后续遇到类似机制问题，可以继续沿这一顺序讨论。

后续已补齐 Zip、classpath 资源、序列化与文本输出工具，分别见 [Java资源压缩与文本输出](Java资源压缩与文本输出.md)、[Java序列化与跨语言对比](Java序列化与跨语言对比.md)。本轮 IO 已阶段收尾，完整导航见 [JavaIO学习计划](JavaIO学习计划.md)。可靠落盘、复杂故障处理、具体缓冲调优和自定义包装实现按需深入；编码选择问题保留为随用回顾点。

## 参考入口

- [Java：InputStream](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/io/InputStream.html)、[FileInputStream](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/io/FileInputStream.html)：读取数量、结束标志与文件流约定。
- [Windows：ReadFile](https://learn.microsoft.com/en-us/windows/win32/api/fileapi/nf-fileapi-readfile)、[Linux：read(2)](https://man7.org/linux/man-pages/man2/read.2.html)：底层文件读取与短读边界。
- [Java：BufferedInputStream](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/io/BufferedInputStream.html)、[BufferedReader](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/io/BufferedReader.html)：内部缓冲与包装约定。
- [Java：BufferedOutputStream](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/io/BufferedOutputStream.html)、[OutputStream](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/io/OutputStream.html)：输出缓冲、刷新及其保证层次。
- [Java：FilterOutputStream](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/io/FilterOutputStream.html)、[BufferedWriter](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/io/BufferedWriter.html)：标准包装流的关闭与刷新。
- [Java 教程：try-with-resources](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html)、[语言规范 14.20.3](https://docs.oracle.com/javase/specs/jls/se25/html/jls-14.html#jls-14.20.3)：初始化、逆序关闭和 suppressed 异常。教程为较早版本，具体规则结合语言规范理解。

API 参考使用 Java SE 25 文档，具体方法可用性以实际 JDK 为准。
