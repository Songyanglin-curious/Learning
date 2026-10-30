# Java 日志

## 学习起点

已讨论日志的定义与用途、Java 各套方案的角色、桥接与输出冲突，以及与现代 .NET 的对照。当前重点是理解入口、转发与实现的关系；不逐套背配置项，具体依赖和配置按实际项目查阅。

## 日志的定义与能力

日志是程序运行过程中，对事件及相关上下文留下的记录，用来了解程序做了什么、还原过程、排查故障、监测运行情况，也可服务于审计。正式审计还需要额外的完整性和保留机制。

可以先理解成“可配置、可管理的规范化输出”：统一时间、来源、级别等信息，按级别或来源筛选，并管理控制台、文件或远程输出。日志不只记录错误，也不一定写入本地文件。

日志与异常的职责不同：异常参与控制流，日志留下观察记录。记录“失败”本身不会中断操作，也不等于完成异常处理。

日志不一定异步。异步是可选实现能力，通过队列交给其他线程处理，减少调用线程等待，但要考虑队列满、退出前写完及崩溃丢失等问题。

## API、门面与实现

- 日志 API：业务代码调用的入口，例如 logger.info(...)。
- 日志门面：提供统一 API，把实际处理交给其他实现，让业务代码与具体日志实现分开。
- 日志实现：执行级别过滤、格式化、控制台或文件输出、文件滚动等工作。

门面本身也是 API，两者不是互斥分类。一个日志项目也可以同时提供 API 和实现。

| 名称 | 主要角色 | 认识重点 |
| --- | --- | --- |
| JUL（java.util.logging） | JDK 自带的 API 和实现 | 不额外安装社区库也能记录日志 |
| Commons Logging（JCL） | 门面 | 阅读已有项目时可能遇到 |
| SLF4J | 门面 | 业务代码调用其 API，运行时接入 provider |
| Logback | 实现 | logback-classic 直接实现 SLF4J 接口 |
| Log4j 2 | 同时提供 API 和实现 | log4j-api 是门面，log4j-core 是实现；也可接入 SLF4J |

例如“业务代码 → SLF4J → Logback”与“业务代码 → SLF4J → Log4j Core（通过适配组件）”是可选组合，不需要把所有名称都装进项目。也可以直接使用 JUL，或使用 Log4j API 配合 Log4j Core。

门面的价值不仅是更换实现：应用及其依赖库可以使用统一入口，由最终应用决定日志如何输出。现实中不同库可能采用不同 API，因此还需要桥接组件。

## 使用方式：获取对象，再调用方法

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class OrderService {
    private static final Logger logger =
            LoggerFactory.getLogger(OrderService.class);

    void createOrder() {
        logger.info("订单创建成功");
    }
}
```

这仍是普通 Java 对象的使用方式。Logger 是接口，通过工厂获取实例，而非自己 new；OrderService.class 用于标识来源。static final 让类共用固定的 logger 引用。

看 import 可以判断调用哪套 API；看运行时依赖和配置，才能判断最终实现。只加入 slf4j-api 能编译，但缺少合适 provider 时，当前 SLF4J 会警告并回退到不输出日志的实现。

## 各套系统需要认识到什么程度

### JUL

JUL 可以独立工作，内部有 Logger、Level、Handler、Formatter 和 LogManager。其级别从高到低包括 SEVERE、WARNING、INFO、CONFIG、FINE、FINER、FINEST，与社区的 ERROR、WARN、INFO、DEBUG、TRACE 需要映射，不能直接视为完全相同。

Logger 有父子层级，默认可把记录交给父 Logger 的 Handler。当前 Logger 和父级同时挂输出 Handler 时，可能重复输出。Logger 与 Handler 都可能过滤级别，上游丢弃的记录不会被下游恢复。

教程中“进入 main 后无法修改 JUL 配置”的说法过于绝对：官方提供 LogManager.readConfiguration() 等能力，也可调整 Logger、Handler 的级别。

### Commons Logging

JCL 用 LogFactory 获取 Log，背后通过适配或发现机制对接日志实现。具体发现规则取决于版本和适配包，不背固定搜索顺序。Spring Boot 内部也使用 Commons Logging API，不能认为它的所有内部代码都直接调用 SLF4J。

### Log4j 2

log4j-api 和 log4j-core 分别提供入口与实现，可以直接组合，也可以通过适配组件承接 SLF4J 调用。Core 负责过滤、格式和输出等，常见配置文件是 log4j2.xml。Log4j 1.x 与 Log4j 2 的包名、依赖和配置不能直接混用。

### SLF4J 与 Logback

slf4j-api 提供入口，logback-classic 直接实现 SLF4J 接口，logback-core 提供基础组件。常见配置文件是 logback.xml；Spring Boot 项目可使用 logback-spring.xml 接入其配置扩展。

教程列出的 SLF4J 1.7、Logback 1.2 是示例版本，不作为新项目选型依据。SLF4J 1.x 与 2.x 的接入机制有差异，应使用兼容版本，并优先遵循项目的依赖管理。

## 桥接与冲突：检查日志最终走哪条路径

门面、桥接和实现属于不同职责：

- 门面：统一业务代码的调用入口。
- provider（旧版本常称 binding）：把 SLF4J 调用接到具体后端。
- 桥接：将其他 API 的日志转交过来。例如 jul-to-slf4j 提供 JUL Handler，把 JUL 记录转给 SLF4J。
- 实现：实际过滤、格式化和输出。

可以用“入口／转发”和“实际实现”两层建立认识，但门面本身并不等于桥接。桥接有方向，JUL → SLF4J 与 SLF4J → JUL 同时启用可能形成循环。

Spring Boot 默认 starter 使用 Logback，并包含适当路由来整合其他日志 API。JUL 与 SLF4J 共存本身不会自动冲突；自行设置 Handler、关闭父级传播、重置配置或改变依赖，都可能绕开默认安排。部署到外部 Servlet 容器时，JUL 的隔离与路由还存在不同边界，应查看对应部署说明。

曾遇到 JUL 封装与 Spring Boot 日志输出混乱，具体原因未通过项目配置验证，不能直接认定是哪一种。常见排查方向如下：

| 现象 | 检查方向 |
| --- | --- |
| 一条记录重复输出 | 自定义 Handler、桥接及父级输出是否同时生效；Appender 是否重复接收 |
| 部分日志格式不同 | 是否有独立 JUL 输出路径 |
| 开启下游 DEBUG 仍没有日志 | 是否已被上游 Logger 或 Handler 过滤 |
| 提示多个 SLF4J provider | 是否引入多套竞争实现 |
| 递归或栈溢出 | 是否存在往返桥接循环 |

分析时依次确定：代码调用哪套 API、经过哪些转发、最终由谁输出，以及沿途哪里会过滤或额外输出。不靠盲目增加日志依赖解决问题。

## 为什么官方和社区方案共存，以及与 .NET 的对照

JUL 到 JDK 1.4 才加入，社区已有方案；官方提供能力不代表既有生态会统一迁移。社区方案可以独立演进，各套入口和配置也因此长期共存。割裂不单是接口设计优劣，更与历史、兼容和生态采用有关。

现代 .NET 官方提供 Microsoft.Extensions.Logging，以 ILogger<T> 为常见入口，再接入 provider。微软有 Console、Debug、EventSource、Windows EventLog 等 provider；社区也有 Serilog、NLog、log4net 等框架，可通过适配接入官方入口。

两边都有多种实现。区别在于现代 .NET 官方抽象在框架中广泛采用，Java 则有 SLF4J 等社区门面参与承担统一入口。这里主要是进程内接口与抽象的整合问题，不必称为通信协议问题。

## 使用注意事项：与体系认识分开

这些属于日志实践，不要求为每套框架重复学习：

- 按事件性质选择级别，不为强行显示而把普通事件记成 ERROR。
- 记录定位所需上下文和完整异常。例如 logger.error("读取配置失败", e)，不要只保存 e.getMessage()。
- 避免同一失败层层重复记录；日志不能替代异常处理。
- 占位符不延迟普通参数计算：logger.debug("结果：{}", calculate()) 中 calculate() 仍先执行。昂贵计算需判断级别或使用延迟求值接口。
- 不记录密码、令牌等敏感数据；控制高频日志，配置滚动和保留期限。
- 关键业务事实保存在业务存储中，不能只依赖一条可能被过滤或丢失的日志。

## 官方资料

- [JUL 包说明](https://docs.oracle.com/en/java/javase/24/docs/api/java.logging/java/util/logging/package-summary.html)
- [SLF4J 手册](https://slf4j.org/manual.html)
- [Log4j API 与实现的区分](https://logging.apache.org/log4j/2.x/manual/api.html)
- [JUL LogManager API](https://docs.oracle.com/en/java/javase/25/docs/api/java.logging/java/util/logging/LogManager.html)
- [SLF4J 桥接与循环注意事项](https://www.slf4j.org/legacy.html)
- [Logback 架构](https://logback.qos.ch/manual/architecture.html)
- [Spring Boot 日志](https://docs.spring.io/spring-boot/reference/features/logging.html)
- [.NET 日志 provider](https://learn.microsoft.com/en-us/dotnet/core/extensions/logging-providers)

## 教程入口

- [廖雪峰：使用 JDK Logging](https://liaoxuefeng.com/books/java/exception/jdk-logging/index.html)
- [廖雪峰：使用 Commons Logging](https://liaoxuefeng.com/books/java/exception/commons-logging/index.html)
- [廖雪峰：使用 Log4j](https://liaoxuefeng.com/books/java/exception/log4j/index.html)
- [廖雪峰：使用 SLF4J 和 Logback](https://liaoxuefeng.com/books/java/exception/slf4j-logback/index.html)
