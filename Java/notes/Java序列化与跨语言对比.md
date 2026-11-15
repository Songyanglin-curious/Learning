# Java 序列化与跨语言对比

更新日期：2026-11-10。

本笔记整理序列化机制、Java／.NET／JavaScript 对比、安全讨论和当前常见工具。内容来自讲解与官方资料核对，没有执行序列化、攻击或兼容性实验；代码片段及预期行为是机制说明，不是运行证据。本轮总入口见 [JavaIO学习计划](JavaIO学习计划.md)。

## 一、先区分格式与工具的能力

序列化把对象状态转换为可保存、传输的数据表示；反序列化根据约定恢复数据或对象。对简单数据传输，把类理解为数据结构的容器即可，日常 JSON 转换不必承担完整对象恢复机制的全部复杂度。

需要分清三个层次：

| 层次 | 提供什么 |
| --- | --- |
| JSON 格式 | 对象、数组、字符串、数字、布尔值、`null` 等数据表示 |
| JSON 对象映射工具 | 在 JSON 数据与程序指定的类型之间转换；可另有多态、引用、自定义转换等功能 |
| Java 原生对象序列化 | 用 Java 对象协议保存类型、实例状态和对象图，恢复期间可调用类的恢复逻辑 |

JSON 本身不定义 Java 类名、运行时方法、共享引用恢复或恢复回调。`$type`、`$ref` 等名称对普通 JSON 解析器只是属性；具体工具赋予它们额外语义后，才会产生类型恢复、引用恢复等能力。

因此，“Java 序列化能处理复杂对象图”不能归纳为“JSON 格式本身能力很强”。要看具体机制与配置给了输入多少控制权。

## 二、Java 原生序列化保存什么

`Serializable` 是标记接口，本身没有需要实现的方法；`ObjectOutputStream.writeObject` 写出对象，`ObjectInputStream.readObject` 恢复对象。这是 Java 对象序列化协议，不是 JSON。通常的 JSON 对象映射不要求实现 `Serializable`。

以下讨论普通 `Serializable` 类的默认字段机制，不把 record、枚举和自定义替换规则混在一起。

```java
class Student implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name = "张三";
    private int age = 20;
    private static String school = "第一中学";
    private transient String sessionToken = "temporary";
}
```

| 字段 | 是否参与默认实例状态保存 |
| --- | --- |
| `name`、`age` | 是，包括 `private` 字段 |
| `school` | 否，`static` 属于类的共享状态 |
| `sessionToken` | 否，`transient` 排除在默认保存范围外 |

`transient` 不使字段在正常运行时消失，只影响默认保存范围；`private` 也不意味着输出数据经过加密或保密。`serialVersionUID` 是写入类描述的特殊兼容性标识，不是普通实例字段。

普通类默认恢复时，会分配新实例，但不执行该可序列化类自己的构造器和实例字段初始化表达式；首个不可序列化父类的无参构造器参与初始化。保存的字段从数据恢复，未保存的字段默认得到相应零值，因此上例 `sessionToken` 是 `null`，不会重新执行 `= "temporary"`。

`school` 仍使用当前进程中的类静态值，未从旧对象恢复。若保存后将它改为“第二中学”，恢复出的对象访问到的仍是当前静态值。

这里已修正过一个理解：默认恢复不是 merge 到已有对象，而是按保存的状态重建实例，也不是把整个运行环境重建。普通类恢复通常得到新对象，不能沿用旧进程的内存地址；类的方法代码不会一起保存，读取方仍需能加载兼容的类定义。

## 三、对象引用关系也可以保存

Java 原生机制会沿需要保存的字段继续遍历，记录对象图中的共享引用和循环引用。例如班级对象的班长、值日生两个字段都引用同一个学生，恢复后仍共享同一个恢复出的学生对象，不会因为有两个字段就复制出两份学生。

被默认遍历到的对象也要支持序列化，否则可能报告 `NotSerializableException`。仅让最外层类实现接口不一定够；`transient` 排除的那条字段引用不会因此继续被保存。

这类对象图保持能力属于原生协议；不能直接推导普通 JSON 自动具有相同语义，具体 JSON 库需要另行配置或定义表示方式。

## 四、类变化后的兼容性

先检查 `serialVersionUID`：流中的标识与当前普通类不一致时，报告 `InvalidClassException`。未显式声明时，会根据类的信息计算默认值，类的某些修改会影响结果，因此通常显式声明。

标识相同只表示通过这项检查，不代表任意修改都兼容。假设其他条件兼容、默认恢复字段，旧数据用新版类读取时：

| 新类变化 | 默认处理 |
| --- | --- |
| 新增 `String email` | 旧数据没有，得到 `null` |
| 新增 `int level` | 得到 0 |
| 删除 `age` | 旧数据中的该字段被忽略 |
| `name` 改名为 `fullName` | 按旧字段删除、新字段增加处理，不自动搬值 |
| 基本类型 `int age` 改为 `long age` | 不兼容，不自动转换基本类型 |

能读出对象不等于恢复后仍满足要求。例如新增必填字段得到 `null`，可能不符合新版对象约束。UID 是兼容性检查标识，不是自动迁移工具；复杂兼容转换按需研究。

简单 JSON 数据同样可能有字段改名、类型或含义变化，但那是双方数据约定的演进，不必因此学习原生对象协议的全部规则。

## 五、反序列化漏洞为什么发生

典型原生对象反序列化会根据输入决定恢复哪些类型、字段和对象关系，恢复期间还可能调用 `readObject`、`readResolve` 等已有代码。这比单纯解析数据提供了更多输入影响程序行为的路径。

例如接收方写了：

```java
// readObject 返回后才进行 Student 类型转换
Student student = (Student) input.readObject();
```

若未提前限制类型，输入描述了一个环境中可恢复的其他类型，它的恢复回调可能已经执行，然后外层转换才失败。因此，事后强制转换不是恢复前的类型过滤。

讨论中的简化例子，是一个已有类在 `readObject` 中恢复字段后打印通知。打印本身不是漏洞，只说明代码调用发生在返回之前；若已有恢复代码可被输入驱动到敏感操作，就可能形成利用路径。多段已有方法组合出的路径常称为利用链（gadget chain）。

它通常不是把输入当作 Java 源代码执行，而是利用接收环境中已有的类型与代码。与 `eval` 相似的是输入影响执行行为，二者具体机制不同。即使没有代码执行链，输入还可能造成过大的对象图和内存、CPU 消耗。

## 六、Java、.NET 与 JavaScript 要比较同一层次

| 机制 | 输入的影响范围与边界 |
| --- | --- |
| Java `ObjectInputStream` | 原生对象图恢复，可能触发恢复逻辑 |
| .NET `BinaryFormatter` | 类似的运行时对象恢复风险，不能视为普通 JSON 映射 |
| 指定目标类型的 JSON 映射 | 程序规定目标类型；构造、成员设置和自定义转换仍可能执行代码 |
| 开放输入类型名的 JSON 恢复 | 输入进一步影响运行时类型选择，需要明确约束 |
| 普通 JS `JSON.parse()` | 得到普通对象、数组和基本值，不因 `$type` 等属性自动实例化业务类 |

.NET 也存在严重反序列化问题。微软明确认为 `BinaryFormatter` 无法被配置成安全机制；从 .NET 9 起，框架自带实现在使用时抛异常，之前允许启用的设置也被移除。此变化不代表旧 .NET Framework 项目已经自动安全。

“较少听说某种语言的漏洞”不能证明它的漏洞数量更少。本轮只比较机制与使用场景，没有做生态漏洞统计。日常若主要接触指定数据类型的 JSON 映射，就可能很少接触原生恢复机制的风险；这只是场景解释，不能推成语言安全排名。

### JS 示例：解析之后的使用也可能出问题

```javascript
const data = JSON.parse('{"name":"小明","__proto__":{"isAdmin":true}}');
const user = Object.assign({}, data); // 合并时触发目标对象继承的特殊 setter

console.log(Object.hasOwn(user, "isAdmin")); // 按标准行为预期为 false
console.log(user.isAdmin);                  // 按标准行为预期为 true
```

JSON 解析时 `__proto__` 只是普通属性；随后赋值到普通对象时，可触发特殊 setter，改变目标 `user` 的原型，属性读取又沿原型取得 `isAdmin`。若后续直接相信该值，就可能影响权限判断。本例只改变目标对象的原型，不是修改全局所有对象。

漏洞触发点在解析之后的合并与使用，不能简单称为 `JSON.parse` 自动执行输入代码。`reviver`、按输入选择构造器等扩展行为，同样要依据实际逻辑判断风险。

## 七、当前常见格式与工具

以下是截至本笔记日期的能力对照，不是使用率或性能排名。

| 格式／机制 | Java 侧入口 | .NET 侧入口 | 认识重点 |
| --- | --- | --- | --- |
| JSON | Jackson；也有 Gson、Fastjson2 | System.Text.Json、Newtonsoft.Json | 指定数据类型映射，按需配置成员与多态 |
| Protobuf | 官方运行库与生成代码 | Google.Protobuf；另有 protobuf-net | 明确的消息约定与字段演进，通常从 `.proto` 生成类型 |
| MessagePack | 相应 Java 实现 | MessagePack for C# | 紧凑二进制数据表示，按需了解 |
| XML | Jakarta XML Binding 等 | XmlSerializer、DataContractSerializer | 对接已有格式时按需学习 |
| 原生对象协议 | Serializable、ObjectOutputStream、ObjectInputStream | BinaryFormatter 等历史机制 | 了解状态、对象图、版本与安全限制 |

Spring Boot 4 系列默认使用 Jackson 3；已有教程可能基于 Jackson 2，包名和配置不能混用。现代 .NET 提供 System.Text.Json，其他项目也可能使用 Newtonsoft.Json。JSON 映射不要求 Java `Serializable` 或 C# `[Serializable]` 标记，具体成员映射由库规则决定。

Protobuf 是数据表示机制，gRPC 是通信框架，常结合使用但不是同一概念。JSON、Protobuf 等跨语言格式仍需要双方约定字段、数值、日期和兼容行为，不自动复制任意语言运行时的对象语义。

本轮没有要求逐一掌握各库 API：Java 的 JSON 以 Jackson 为主要认识对象，其他知道用途和边界即可；不转入完整选型或业务架构设计。

## 八、使用时关注什么

- 简单数据交换优先采用程序明确规定的数据类型和字段约定。
- 不随意开放由输入指定任意运行时类型的能力；确实需要多态时明确允许范围。
- 避免将不可信数据直接交给原生对象恢复机制；Java 的过滤器可约束类型、对象图规模，但不是任意输入都安全的开关；BinaryFormatter 不作为新用途的常规选择。
- 检查自定义转换、构造与成员设置逻辑，不把“指定了目标类型”当作绝对安全证明。
- JS 不盲目合并外部属性，按允许字段接收，敏感状态不直接信任输入；关注输入大小、嵌套深度和依赖版本。

当前学到能识别高风险能力、找到正确用法即可，不要求研究每条利用链。复杂对象图、自定义恢复和历史数据迁移遇到需求再深入。

## 参考入口

- [廖雪峰：序列化](https://liaoxuefeng.com/books/java/io/serialize/index.html)：本轮教程入口。
- [Java 序列化架构](https://docs.oracle.com/en/java/javase/25/docs/specs/serialization/serial-arch.html)、[对象恢复](https://docs.oracle.com/en/java/javase/25/docs/specs/serialization/input.html)、[版本演进](https://docs.oracle.com/en/java/javase/25/docs/specs/serialization/version.html)、[Serializable](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/io/Serializable.html)：字段、对象图、构造与兼容规则。
- [Oracle 安全编码指南](https://www.oracle.com/java/technologies/javase/seccodeguide.html)、[序列化过滤](https://docs.oracle.com/en/java/javase/25/core/serialization-filtering1.html)：原生恢复的安全边界。
- [微软 BinaryFormatter 安全说明](https://learn.microsoft.com/en-us/dotnet/standard/serialization/binaryformatter-security-guide)、[.NET 9 移除说明](https://learn.microsoft.com/en-us/dotnet/core/compatibility/serialization/9.0/binaryformatter-removal)：机制风险与版本状态。
- [System.Text.Json](https://learn.microsoft.com/en-us/dotnet/standard/serialization/system-text-json/overview)、[多态配置](https://learn.microsoft.com/en-us/dotnet/standard/serialization/system-text-json/polymorphism)、[Json.NET TypeNameHandling](https://www.newtonsoft.com/json/help/html/P_Newtonsoft_Json_JsonSerializerSettings_TypeNameHandling.htm)：JSON 映射与类型恢复。
- [ECMAScript JSON.parse](https://tc39.es/ecma262/multipage/structured-data.html#sec-json.parse)、[MDN 原型污染说明](https://developer.mozilla.org/en-US/docs/Web/Security/Attacks/Prototype_pollution)：JS 解析规则与后续合并行为。
- [Spring Boot JSON 文档](https://docs.spring.io/spring-boot/reference/features/json.html)、[Jackson](https://github.com/FasterXML/jackson)、[Gson](https://github.com/google/gson)、[Fastjson2](https://github.com/alibaba/fastjson2)：Java JSON 工具入口。
- [微软序列化方案比较](https://learn.microsoft.com/en-us/dotnet/standard/serialization/binaryformatter-migration-guide/choose-a-serializer)、[Protobuf](https://protobuf.dev/overview/)、[gRPC Java 教程](https://grpc.io/docs/languages/java/basics/)：格式与协议工具对照。

Java 机制参考使用 Java SE 25 文档；框架与库的现状可能变化，真正接入项目时核对所用版本。
