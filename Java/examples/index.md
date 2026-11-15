# Java 实验索引

这里保存用于观察行为、检验疑问的小程序，不是必须逐项完成的作业集。返回 [Java 学习入口](../index.md)。

| 资料 | 内容摘要 | 使用场景 |
| --- | --- | --- |
| [Hello](01-hello/Hello.java) | 第一个 Java 程序 | 检查最基本的编译、启动与输出过程 |
| [集合实验导航](../notes/Java集合.md) | 笔记中的配套实验表说明集合示例的用途和必要参数 | 按判等、只读与视图、容器实现和哈希机制等疑问定位代码 |
| [集合基础示例](02-collection-intro/Basics.java) | 数组与 List、Set、Map 的基本行为 | 对照几种容器的能力 |
| [只读与视图](02-collection-intro/ReadOnlyForms.java) | 固定大小列表、不可修改包装与视图行为 | 验证修改限制和底层共享 |
| [判等示例](02-collection-intro/EqualsDemo.java) | equals 与 hashCode 的不同组合 | 检查内容判等与哈希去重的关系 |
| [接口与代价](02-collection-intro/AbstractionTrap.java) | 同一 List 接口在不同实现上的访问代价 | 理解接口能力与实现成本的区别 |

运行前阅读对应代码和笔记。部分反射实验需要 `--add-opens=java.base/java.util=ALL-UNNAMED`；具体要求见主要笔记的实验表。性能数字受环境、预热和测量方法影响，不作为严谨基准结论。

历史记录中的运行结果不代表本次重跑；本次整理没有编译或执行这些程序。子目录沿用既有编号分组，只保存代码，不扩建空案例或评测目录。
