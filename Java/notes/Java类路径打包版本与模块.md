# Java 类路径、打包、版本与模块

这组内容解释源码编译以后，类放在哪里、怎样被找到、能在哪个运行环境中执行，以及组件怎样声明依赖和开放边界。

| 概念 | 负责什么 |
| --- | --- |
| classpath | 提供类与资源的查找位置 |
| JAR | 把 class 和资源按内部路径装进归档 |
| 版本兼容 | 判断语法、编译产物、API 和运行环境是否匹配 |
| 模块 | 声明依赖及包的开放边界，由编译器和运行时检查 |

## classpath：去哪里查找类

包名决定类的名称，classpath 指定查找它的根位置。例如文件位于：

```text
D:\demo\out\com\example\Main.class
```

运行时指定：

```powershell
java -cp D:\demo\out com.example.Main
```

运行环境把 `com.example.Main` 对应到 `com/example/Main.class`，再到 classpath 根位置查找。因此应指定 `out`，而不是 `out\com\example`。

classpath 可以包含多个目录和 JAR。Windows 使用分号分隔，在 PowerShell 中应给整个路径列表加引号：

```powershell
java -cp "D:\demo\out;D:\shared\classes;D:\demo\lib\mysql.jar" com.example.Main
```

程序需要的类可以分别存放在这些位置，不要求全部集中在入口类所在目录。

### 多个位置怎样查找

普通应用 classpath 通常按列表顺序查找，查找目标是完整类名，不是先选定某个包目录就只用它。

假设位置 A 有 `com/example/Main.class`，位置 B 有 `com/example/Helper.class`，两者可以共同提供 `com.example` 包中的不同类。如果两个位置都有 `com/example/Helper.class`，通常先找到的版本生效，不会合并两个类的内容。重复类容易导致加载旧版本，应尽量避免。

这只是普通 classpath 的模型。父级委派、自定义类加载器等还会影响实际查找，不能理解成所有 Java 类都严格按这个列表选择。

### 使用边界

- 编译时的 `javac -cp` 和运行时的 `java -cp` 分别配置，编译成功不保证运行时依赖齐全。
- 把 `lib` 目录加入 classpath，不会递归收集其中的 JAR。`"lib/*"` 可表示该目录中的 JAR，不包含子目录。
- 未指定 classpath 且没有设置 `CLASSPATH` 环境变量时，默认使用当前目录 `.`。显式指定后，需要当前目录就把 `.` 也列入。
- classpath 不下载依赖、不选择依赖版本。IDE、Maven、Gradle 等通常负责准备实际的编译和运行路径。

## JAR：保留路径的文件容器

最基本的 JAR 是 ZIP 格式的归档，把编译后的 `.class` 和资源按相对路径装进去，并可附加 Java 工具识别的元信息。

```text
app.jar
├── META-INF/MANIFEST.MF
├── com/example/Main.class
├── com/example/Helper.class
└── config.properties
```

打包本身不把多个类合并为一个类，不把字节码变成机器码，也不执行其中的程序。普通 ZIP 工具看到的是文件；Java 类加载器会把对应路径的 class 条目作为类型字节码读取。

打包前后可以这样对照：

```powershell
java -cp out com.example.Main
java -cp app.jar com.example.Main
```

变化的是 classpath 项：从目录换成 JAR；保留的是内部包路径，不是 classpath 字符串本身。

### 元信息和启动方式

`META-INF/MANIFEST.MF` 是清单文件，可以声明入口等信息。`jar` 工具通常默认生成清单，但不是所有 JAR 都必须声明程序入口。

| 启动方式 | 入口从哪里来 |
| --- | --- |
| `java -cp app.jar com.example.Main` | 命令中明确指定 |
| `java -jar app.jar` | 清单中的 `Main-Class` |

使用 `-jar` 时，命令行 `-cp` 不生效；其他依赖可通过清单的 `Class-Path` 或专门启动器等机制提供。“可执行 JAR”表示 Java 启动器能够找到入口，不表示它是原生 EXE，也不是任意 ZIP 改名就能运行。

### 容器与构建动作要分开

普通 JAR 不会自动下载、收集第三方依赖，也不会修改业务字节码。把一个依赖 JAR 放到另一个 JAR 内，普通 classpath 不会自动继续进去加载。

Maven、Gradle 或 Spring Boot 插件可以在打包流程中加入依赖、调整布局、生成元信息或启动器。Spring Boot 常见的整体打包方式属于这类额外处理，不能当成原始 JAR 格式自带的行为。

JAR 在分发用途上有点像 C# 的 DLL/EXE，但不等同于 .NET 程序集：JAR 主要是容器，其中的 class 文件各自保存字节码和类型信息。签名、服务发现、多版本 JAR 等先知道存在，用到再深入。

## 版本：运行环境是否覆盖程序要求

版本兼容需要分别看三个层面：

| 层面 | 判断内容 |
| --- | --- |
| 源码语法 | 目标版本是否支持所写语法 |
| class 文件格式 | 目标 JVM 是否支持编译产物的格式版本 |
| API 与依赖 | 使用的标准库及第三方库是否适用于目标环境 |

例如 JDK 17 默认编译出的 class 文件，JVM 8 不支持，即使源码只用了旧语法，也可能报 `UnsupportedClassVersionError`。

高版本编译器也可以面向其支持的旧版本生成产物。例如使用 JDK 17：

```powershell
javac --release 8 Main.java
```

`--release 8` 约束源码语法、生成的 class 版本和可使用的目标 Java SE API。第三方依赖是否支持 Java 8，仍需另行确认。

可以按“环境的支持范围是否覆盖程序要求”理解：新 JVM 通常能运行旧 class，但仍可能受库移除等变化影响；旧 JVM 不能识别超出其支持范围的新 class 格式。判断依据是最终产物和依赖要求，不只是使用了哪个版本的编译器。

C# 的基本兼容思路相同，也要区分语言版本、编译目标、运行时及 API。具体配置并不一一对应：C# 语言版本与目标框架可以分别设置，目标框架也不能简单等同于 IL 文件版本。

## 模块：声明依赖与组件边界

Java 9 引入的模块系统在包之上增加边界。模块描述文件是一份会被执行检查的声明，不只是文档，也不是 Maven、npm 那样的依赖下载和版本管理工具。

### 为什么只有包还不够

假设支付 SDK 有两个包：

```text
pay.api
  PaymentClient

pay.internal
  SignatureHelper
```

`PaymentClient` 要调用另一个包中的 `SignatureHelper`，后者需要具备相应的公开访问能力。但在传统 classpath 下，外部程序也可能直接使用这个公开的内部类型。

SDK 作者本来只想承诺 `PaymentClient` 的用法，签名细节则希望能够自由修改。如果使用者依赖了 `SignatureHelper`，内部调整就可能破坏外部代码。

模块描述可以只导出 API 包：

```java
module payment.sdk {
    exports pay.api;
}
```

SDK 内部仍能跨包访问公开的实现类型；普通外部模块则不能直接引用未导出的内部包，即使其中的类是 `public`。

这层边界区分了“向组件内部其他包公开”和“向组件使用者公开”。小程序未必需要模块，但一个组件包含多个包、需要保护内部实现时，这项能力有明确用途。

### 声明什么，检查什么

模块在源码根部的 `module-info.java` 中描述。例如：

```java
module com.example.bank {
    requires java.sql;
    exports com.example.bank.api;
}
```

- `requires` 声明依赖的模块，模块解析时检查必需模块是否存在。
- `exports` 以包为单位开放 API，不是逐个导出函数或类；包中的类型和成员仍需满足公开访问等条件。

使用方要有相应的模块读取关系，被使用的包要对其导出，类型和成员也要可访问。反射开放等细节留到实际需要时讨论。

可以类比 JavaScript 模块的依赖和导出声明，但不能等同于 npm 包管理：Java 模块声明不负责下载、安装或选择依赖版本。

### 与 classpath、JAR 的关系

- 包组织类型，JAR 装载文件，模块声明依赖与开放边界，三者不是同一概念。
- `module-path` 提供模块的查找位置；模块系统采用不同于传统 classpath 的解析规则。
- 模块可以打成包含 `module-info.class` 的 JAR，不是每个 JAR 都是显式模块。
- 模块依赖也帮助拆分 Java 平台，可用 `jlink` 组合应用所需的运行环境。
- 传统 classpath 仍可使用，不需要为了运行在新版 Java 上就立即把应用改成模块化项目。

## 学习来源

- [廖雪峰：classpath 和 jar](https://liaoxuefeng.com/books/java/oop/basic/classpath-jar/index.html)
- [廖雪峰：class 版本](https://liaoxuefeng.com/books/java/oop/basic/class-version/index.html)
- [廖雪峰：模块](https://liaoxuefeng.com/books/java/oop/basic/module/index.html)
