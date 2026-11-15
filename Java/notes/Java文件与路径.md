# Java 文件与路径

更新日期：2026-11-10。

本笔记总结 IO 第一条主线及讨论中涉及的工作目录、文件占用和格式识别。当前已完成概念与行为边界的讨论，未执行文件操作实验；文中代码是说明片段，不是运行验证结果。后续学习入口见 [JavaIO学习计划](JavaIO学习计划.md)。

## 一、路径、文件、读取结果各是什么

先区分三种东西：

- 路径对象：内存中对一个位置的描述，该位置可以不存在。
- 实际文件或目录：文件系统中对应位置的对象。
- 读取结果：例如从文件读出的 `String` 或 `byte[]`，是程序接下来处理的数据。

Java 有较早的 `File` 设计，以及后来配套使用的 `Path` 与 `Files`。它们不是三个依次执行的步骤。

| 类型 | 职责 | 常见能力 |
| --- | --- | --- |
| `java.io.File` | 表示文件或目录路径，同时提供部分文件系统操作 | 取得名称、查询存在与类型、创建、删除、列出目录 |
| `java.nio.file.Path` | 表示和处理路径 | 取得名称、父路径、组合路径、转换为绝对路径 |
| `java.nio.file.Files` | 根据路径操作文件系统，也提供内容读写入口 | 查询属性、创建、复制、移动、删除、遍历、读取和写入 |

```java
// 只构造路径，不创建或打开文件
Path path = Path.of("D:\\Learning\\data.txt");

// 查询文件系统，不读取正文
boolean exists = Files.exists(path);

// 读取全部文本，默认使用 UTF-8，内部负责打开和关闭文件
String content = Files.readString(path);
```

`new File(...)` 同样不会因构造对象就创建、打开或读取磁盘文件。`File` 没有直接读取全部文本的方法；`Files.readString` 则封装了这类读写过程。它返回的字符串是这次读取的结果，路径对象本身不保存文件内容。整体读取受可用内存限制；分批读写留到流的主线展开。

## 二、为什么后来有 Path 与 Files

旧 `File` 的一些操作无法充分表达失败原因。例如 `file.delete()` 返回 `false` 时，不能直接据此区分文件不存在、目录非空等原因；`Files.delete(path)` 则通过异常报告失败，便于处理和诊断。

新体系还把两类职责分开：

- `Path` 处理位置描述，例如 `directory.resolve("data.txt")`，组合路径不要求对应文件存在。
- `Files` 执行文件系统操作，处理权限、存在性和读写失败等问题，并根据路径所属的文件系统提供者执行操作。

`Path` 是接口，新体系支持不同文件系统实现。保留旧 API 则使已有代码仍可使用原来的方法约定。

要区分“API 能力的改进”与“职责的拆分”：更明确的异常报告并非只有拆成两个类型才能实现，不能把改进简单归因于把方法移到了工具类。

## 三、相对路径与工作目录

工作目录是进程解释相对路径时使用的默认基准目录。它与源文件目录、EXE／JAR 所在目录是不同概念，具体可能相同，也可能不同。

例如，先在 PowerShell 中切换到 `D:\Data`，再启动 `D:\Apps\demo.jar`，正常情况下工作目录是 `D:\Data`，相对路径 `config.txt` 指向 `D:\Data\config.txt`。不能根据 JAR 位置推断工作目录。

启动前可以先切换终端目录，或由 IDE 的运行配置、负责启动进程的程序指定工作目录。

| 操作 | Java | C# 对照 |
| --- | --- | --- |
| 查看工作目录 | 通常查看 `System.getProperty("user.dir")` | `Environment.CurrentDirectory` |
| 运行中修改当前进程工作目录 | 标准库没有与 C# 设置方式等价的可移植接口 | 可设置 `Environment.CurrentDirectory`，或调用 `Directory.SetCurrentDirectory(...)` |
| 指定业务文件的基准目录 | 用明确的基准 `Path` 配合 `resolve` | 用明确基准配合 `Path.Combine`；应用目录文件通常以 `AppContext.BaseDirectory` 为基准 |

`System.setProperty("user.dir", ...)` 只修改 Java 系统属性，不能作为可靠的工作目录切换方式：它不会因此修改操作系统的进程工作目录，路径实现还可能缓存初始化时的值。

`path.toAbsolutePath()` 返回新的路径对象，不改变原来的 `Path`，也不证明文件存在。若业务要求固定目录，显式构造基准更清楚：

```java
Path base = Path.of("D:\\Learning");
Path config = base.resolve("data").resolve("config.txt");
```

## 四、文件操作看行为约定即可

已有文件操作经验时，重点是知道入口和边界，具体 API 按需查阅，不要求逐个记忆。

以下复制、移动约定以两个不同路径的普通文件为例：

| 操作 | 主要约定 |
| --- | --- |
| `Files.createFile(path)` | 创建空文件；目标已存在会失败，不覆盖；不会自动创建父目录 |
| `Files.createDirectories(dir)` | 创建目录并补齐缺少的父目录；目标已是目录时可接受 |
| `Files.copy(source, target)` | 复制后源文件保留；默认不替换已存在的不同目标文件 |
| `Files.move(source, target)` | 移动后原位置不再保留该文件；默认不替换已存在的不同目标文件 |
| `StandardCopyOption.REPLACE_EXISTING` | 显式选择替换已有目标，可用于上述复制或移动操作 |
| `Files.delete(path)` | 不存在也视为失败，抛出异常 |
| `Files.deleteIfExists(path)` | 成功删除返回 `true`；不存在返回 `false` |

文件移动改变的是文件系统，不会改写原来的路径对象。成功执行 `Files.move(source, target)` 后，`source` 仍描述原位置；后续访问移动后的文件应使用 `target`。`Path` 是不可变的位置描述，不会跟踪文件移动。

删除目录要求目录为空；`delete` 与 `deleteIfExists` 都不会自动递归删除内容。删除整棵目录树需要先删除子项，再删除父目录，可用 `walkFileTree` 组织；本轮未展开实现。递归删除中途失败时，已经完成的删除不会自动回滚。

`deleteIfExists` 只接受“不存在”，不忽略其他失败：目录非空、权限不足、Windows 下的文件占用等仍可能导致异常。

## 五、检查结果不能保证下一步成功

文件系统可能被其他线程或进程改变。先 `Files.exists(path)` 再读取，是两个分开的操作；即使刚才存在，读取时也可能已经被删除、移动，或权限发生变化。

存在检查不会锁定文件，结果也可能立即过时。若需求就是读取内容，可以直接尝试读取，按业务需要处理 `NoSuchFileException` 或其他 `IOException`。状态查询适合展示和分支判断，操作成功与否仍以真正执行时的结果为准。

## 六、文件占用：界面打开与操作系统句柄不同

本节讨论 Windows 下的常见机制。句柄可以先理解为程序打开文件后获得的访问凭证，与表示位置的 `Path` 不同。

打开文件时，有两组要求：自己需要什么访问能力，以及允许其他打开者做什么。共享许可包括读、写、删除等；若新旧打开要求不兼容，Windows 可以拒绝访问。这类限制通常持续到对应句柄关闭。

持续持有句柄不必然阻止其他程序；还要看共享要求，也可能有额外的文件锁。

对 WPS 与 VS Code 的讨论，应保留以下区别：

- WPS 编辑期间其他程序无法访问，可能涉及持续持有句柄并限制共享，或文件锁，不能只根据界面状态确定具体机制。
- 文本编辑器可以读取内容后关闭读取句柄，继续编辑内存中的文本，保存时再打开、写入和关闭；界面标签页持续打开不要求持续排他占用磁盘文件。读取时也需要句柄，并非只有保存才需要。
- 本轮未检查 WPS／VS Code 的实际句柄，以上用于解释常见策略，不是对特定版本实现的跟踪结论。

共享兼容性是双方的：即使已有编辑程序允许别人读，压缩程序若要求读取期间禁止其他人写，也可能与已有写句柄冲突。因此，只读用途的 ZIP 打包也可能遭遇“文件被占用”。Java 访问这些文件同样受操作系统限制，文件锁的具体行为还具有平台差异。

## 七、属性查询与内容格式识别

| 层次 | 要回答的问题 |
| --- | --- |
| 路径信息 | 名称、父路径、路径组合是什么；如 `getFileName()` |
| 文件系统属性 | 是否为普通文件或目录、多少字节、何时修改 |
| 内容格式 | 文件内容是 PNG、PDF、Excel、ZIP 等哪种格式 |

常用属性入口是 `Files.isRegularFile`、`Files.isDirectory`、`Files.size`、`Files.getLastModifiedTime`。查询的是文件系统维护的信息，不要求把正文全部加载到内存。

- `isRegularFile` 的“普通文件”包括文本、图片、Excel 等，不会识别具体内容格式。
- `isDirectory` 返回 `false` 不意味着一定是普通文件：不存在、无法确定类型等也可能返回 `false`。
- `size` 单位是字节；对目录的结果由实现决定，不是递归累加里面所有文件的大小。
- 文件大小、修改时间等查询可能失败并抛出异常。创建时间、访问时间、隐藏属性、权限和所有者等按需查阅。

此前根据开头几个 byte 判断类型，属于文件头／魔数识别。它能辅助识别格式，但不一定足够区分具体格式或验证完整性。例如普通 `.xlsx` 是 ZIP 容器，仅识别 ZIP 文件头还不能确认它是 Excel，需进一步检查内部结构。

`Files.probeContentType(path)` 可尝试获得 MIME 类型，无法确定时可能返回 `null`。它使用具体探测器，可能依据名称、属性或内容字节，不保证检查文件头，不能直接作为严格的格式验证。

## 八、目录遍历与筛选

廖雪峰教程展示 `File.listFiles()`；使用 `Path` 体系时，本轮认识了以下入口：

| 方式 | 范围与行为 |
| --- | --- |
| `Files.newDirectoryStream(dir)` | 直接子项，不包含起点自身，不自动递归；返回可逐项遍历的目录资源 |
| `Files.walk(dir)` | 包含起点，深度优先递归，边遍历边提供 `Path` |
| `Files.walk(dir, maxDepth)` | 限制向下深度，起点为第 0 层；深度为 1 时包括起点及直接子项 |

`DirectoryStream` 与 `walk` 返回的流都应通过 `try-with-resources` 关闭。结果不保证按文件名排序；它们也不是文件系统的固定快照。`walk` 默认不沿符号链接继续遍历目录。遍历可能因权限或文件变化中途失败。

筛选的含义是对已经提供的路径进行判断，保留满足条件的结果。例如递归找出名称以 `.java` 结尾的普通文件：

```java
Path dir = Path.of("D:\\Learning");

// 遍历、筛选并打印路径，结束后自动关闭资源
try (Stream<Path> paths = Files.walk(dir)) {
    paths.filter(Files::isRegularFile)
         .filter(p -> p.getFileName().toString().endsWith(".java"))
         .forEach(System.out::println);
}
```

数据流是：遍历提供路径 → 只保留普通文件 → 只保留名称匹配者 → 打印。`filter` 的条件为 `true` 就保留，为 `false` 就过滤；这里的扩展名筛选不验证内容格式。`Stream` 和方法引用本身尚未在本轮系统展开。

筛选结果与控制遍历范围不同：过滤掉目录，不代表 `walk` 不会进入它。要跳过某棵子目录树等更复杂的控制，可按需学习 `walkFileTree`。

## 学习边界与接续

文件与路径这条主线当前可以告一段落。共通文件操作主要认识能力与行为约定，不继续逐项罗列 API。路径规范化、符号链接、详细权限、复杂遍历控制、锁实现和严格格式验证，在实际问题出现时再深入。

后续已完成字节与字符、读写过程及用途补齐的讨论，本轮 IO 已阶段收尾。完整入口与按需回顾范围见 [JavaIO学习计划](JavaIO学习计划.md)。

## 参考入口

- [廖雪峰：File 对象](https://liaoxuefeng.com/books/java/io/file/index.html)、[使用 Files](https://liaoxuefeng.com/books/java/io/files/index.html)：本轮教程入口。
- [Java：File](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/io/File.html)、[Path](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/Path.html)、[Files](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/Files.html)：职责、文件操作和异常约定。
- [Java：DirectoryStream](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/DirectoryStream.html)：目录遍历与关闭。
- [Java：System.getProperties](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/System.html#getProperties())：`user.dir` 及修改标准系统属性的限制。
- [Java：FileTypeDetector](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/spi/FileTypeDetector.html)：内容类型探测的实现边界。
- [Windows：CreateFileW](https://learn.microsoft.com/en-us/windows/win32/api/fileapi/nf-fileapi-createfilew)、[Java：FileLock](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/channels/FileLock.html)：共享模式与锁的平台边界。
- [微软：Environment.CurrentDirectory](https://learn.microsoft.com/en-us/dotnet/api/system.environment.currentdirectory)、[AppContext.BaseDirectory](https://learn.microsoft.com/en-us/dotnet/api/system.appcontext.basedirectory)：工作目录与应用基准目录的 C# 对照。
- [微软：SpreadsheetML 文档结构](https://learn.microsoft.com/en-us/office/open-xml/spreadsheet/structure-of-a-spreadsheetml-document)：XLSX 的 ZIP 容器与内部结构。

Java API 参考使用 Java SE 25 文档；具体方法可用性以实际 JDK 为准，例如 `Path.of`、`Files.readString` 从 Java 11 起提供。
