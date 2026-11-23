# Java 实验索引

这里保存用于观察行为、检验疑问的小程序，不是必须逐项完成的作业集。返回 [Java 学习入口](../index.md)。

| 资料 | 内容摘要 | 使用场景 |
| --- | --- | --- |
| [Hello](01-hello/Hello.java) | 第一个 Java 程序 | 检查最基本的编译、启动与输出过程 |
| [纯 JDK 测试入口](03-unit-test/CalculatorTest.java)与[业务代码](03-unit-test/Calculator.java) | 不使用 IDE、构建工具或 JUnit，由 main 调用测试并用 if / throw 验证结果 | 在该目录执行 `javac -encoding UTF-8 Calculator.java CalculatorTest.java`，再执行 `java -cp . CalculatorTest`；观察业务调用、断言失败与程序退出 |
| [JUnit 命令行环境](03-unit-test/运行说明.md) | 独立 JAR、无 main 的测试类、原始编译与运行命令 | 不用 IDE 或构建工具，对照 JUnit 怎样组织测试；JDK 17 下由 AI 实测两个测试通过 |
| [集合实验导航](../notes/Java集合.md) | 笔记中的配套实验表说明集合示例的用途和必要参数 | 按判等、只读与视图、容器实现和哈希机制等疑问定位代码 |
| [集合基础示例](02-collection-intro/Basics.java) | 数组与 List、Set、Map 的基本行为 | 对照几种容器的能力 |
| [只读与视图](02-collection-intro/ReadOnlyForms.java) | 固定大小列表、不可修改包装与视图行为 | 验证修改限制和底层共享 |
| [判等示例](02-collection-intro/EqualsDemo.java) | equals 与 hashCode 的不同组合 | 检查内容判等与哈希去重的关系 |
| [接口与代价](02-collection-intro/AbstractionTrap.java) | 同一 List 接口在不同实现上的访问代价 | 理解接口能力与实现成本的区别 |
| [线程启动与等待](04-thread-start-join/Main.java) | 用 `start()` 启动子线程，用 `join()` 等待子线程结束 | 按[运行说明](04-thread-start-join/运行说明.md)执行，观察两个线程的输出顺序 |
| [线程安全实验](05-thread-safety/运行说明.md) | 丢失更新、坐标中间状态、普通字段与 volatile 字段对照 | 按问题选择对应的 Java 文件运行 |

运行前阅读对应代码和笔记。部分反射实验需要 `--add-opens=java.base/java.util=ALL-UNNAMED`；具体要求见主要笔记的实验表。性能数字受环境、预热和测量方法影响，不作为严谨基准结论。

历史记录中的运行结果不代表本次重跑；本次整理没有编译或执行这些程序。子目录沿用既有编号分组，只保存代码，不扩建空案例或评测目录。
