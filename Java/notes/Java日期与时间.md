# Java 日期与时间

整理日期：2026-11-17。依据本轮讨论整理；示例用于说明语义，未运行验证。当前深度已由学习者确认足够，特殊规则和具体 API 按需查阅。

## 1. 核心认识

**先准确解读时间数据携带的信息；未携带的信息按明确的业务约定补齐，不擅自附加时区或转换。**

有偏移量／时区就按其解释；没有则按业务约定。业务约定可以是北京时间，但不等于当前机器的默认时区。不是只有跨国业务才涉及解释规则，也不是所有系统都必须存 UTC 或时间戳。

单一中国业务可以统一约定输入、存储和显示都采用北京时间的日期时间字段：输入八点就保留八点。关键是数据库映射、接口和序列化配置保持一致，不能让中间组件无意转换。

## 2. 时间戳与当地字段不是同一种东西

对于确定的事件时刻，可以用 Epoch 时间作为统一坐标。Unix 时间戳以 `1970-01-01T00:00:00Z` 为起点，通常以秒或毫秒计数，不计入闰秒；本轮不深入计时尺度细节。

同一时刻可以有不同地区的表示：

```text
北京时间：2026-11-17 08:00:00 +08:00
UTC：     2026-11-17 00:00:00 Z
```

换算关系：**当地日期时间 = UTC 日期时间 + 偏移量**。因此北京时间八点转 UTC 要减八小时，不是加八小时。

时间戳是这个时刻的数字坐标，本身不需要为北京时间额外加八小时；选择时区是解释当地字段或显示时刻时的操作。

但并非所有时间对象都存时间戳：`LocalDateTime` 只保存日期时间字段，没有偏移量／时区。解析成它，不等于已经确定一个时刻。“每天上午九点”也是当地规则，不能由某一天的单个时间戳代替。

## 3. 文本中的空格、T、Z

```text
2026-11-17 08:00:00        没有偏移量，按业务约定解释
2026-11-17T08:00:00       同样没有偏移量，T 仅分隔日期和时间
2026-11-17T08:00:00+08:00 明确是 UTC+08:00 的八点
2026-11-17T08:00:00Z      明确是 UTC 八点，对应北京时间十六点
```

`T` 来自 ISO 8601 写法，只是分隔符，不触发时区转换。空格格式仍可使用，只要解析格式匹配。

`Z` 表示 UTC 零偏移，等价于 `+00:00`。**不能直接在北京时间八点后加 Z，却期望仍表示同一个时刻。**

## 4. 类型选择速查

| 类型 | 表达什么 | 常见用途与边界 |
| --- | --- | --- |
| `LocalDate` | 年月日，不带时区 | 生日、业务日期；不必硬凑零点 |
| `LocalTime` | 一天中的时间，不带日期／时区 | 开门钟表时间；完整周期规则还需日期及地区等上下文 |
| `LocalDateTime` | 年月日时分秒等字段，不带时区 | 原样保存当地日期时间，不自动转 UTC |
| `Instant` | 时间线上的确定时刻 | 事件时刻、时间戳；以 Epoch 秒及纳秒部分表达 |
| `OffsetDateTime` | 日期时间 + 固定 UTC 偏移量 | 接口中的 `+08:00` 等表示，可转成确定时刻 |
| `ZonedDateTime` | 日期时间 + 地区时区规则 | 跨地区显示、按地区日历规则运算 |
| `Period` | 日历上的年月日数量 | 一个月后、一年后 |
| `Duration` | 以秒及纳秒表达的时间长度 | 两个时刻的间隔、经过半小时 |

以上现代核心类型不可变：修改方法返回新对象，不修改原对象。与 C# `DateTime.AddHours()` 类似，结果需要接住。

## 5. 常用操作

下列片段使用 `java.time.*`、`java.time.format.*`；旧 API 衔接另用 `java.util.Date`。片段不是完整可执行程序。

### 原样解析、格式化和计算

```java
DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

// 按约定格式保存日期时间字段，不做时区转换。
LocalDateTime time =
        LocalDateTime.parse("2026-11-17 08:00:00", formatter);
String text = time.format(formatter); // 2026-11-17 08:00:00

// 返回新对象，原来的 time 仍是八点。
LocalDateTime later = time.plusHours(2);
```

`LocalDateTime.parse("2026-11-17T08:00:00")` 可直接使用默认 ISO 格式。默认输出可能省略零秒，得到 `2026-11-17T08:00`，这不是数据改变。

### 需要时才转换为确定时刻

```java
// 按 UTC+08:00 解释八点，对应 UTC 零点。
Instant instant = time.toInstant(ZoneOffset.ofHours(8));

// 将同一时刻按上海时区转换为当地字段。
LocalDateTime beijing =
        LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Shanghai"));

// 时间戳必须明确单位，秒与毫秒不能混用。
long seconds = instant.getEpochSecond();
long millis = instant.toEpochMilli();
Instant restored = Instant.ofEpochMilli(millis);
```

转为毫秒会丢弃不足一毫秒的精度。`Instant` 默认文本使用 UTC 的 `Z` 表示。

### 携带偏移量或地区

```java
OffsetDateTime offsetTime =
        OffsetDateTime.parse("2026-11-17T08:00:00+08:00");
Instant sameInstant = offsetTime.toInstant();

ZonedDateTime shanghai = time.atZone(ZoneId.of("Asia/Shanghai"));

// 保持同一时刻，只改变地区显示。
ZonedDateTime london =
        shanghai.withZoneSameInstant(ZoneId.of("Europe/London"));
```

`ZoneOffset` 是固定偏移量；`ZoneId` 可以代表一套随日期变化的地区规则，不能把地区时区永远当作固定偏移量。夏令时切换时，当地日期时间可能不存在或重复；实际遇到再查解析与调整策略。

## 6. 运算与格式的必要边界

- 月份不是固定天数。`2026-01-31` 加一个月会调整到 `2026-02-28`。
- 对带地区时区的时间，加一个日历日与经过 24 小时在夏令时附近可能不同。
- `Duration.between(startInstant, endInstant)` 可计算时刻间隔；测量程序耗时通常用 `System.nanoTime()` 的差值，它不是 Epoch 时间戳。
- 比较前先确认比较当地字段还是实际时刻；需要比较实际时刻可转成 `Instant`，不要把不同地区的钟表数字直接当全球先后。
- `MM` 是月份，`mm` 是分钟；`HH` 是 24 小时制，`hh` 是 12 小时制，通常要搭配上午／下午。
- 普通 ISO 日期格式优先考虑 `uuuu`；`yyyy` 是纪元内年份；`YYYY` 是周所属年份，跨年附近可能不同，不要误用。
- 严格拒绝非法日期时，可使用 `formatter.withResolverStyle(ResolverStyle.STRICT)`；不必背完整格式符表。
- `DateTimeFormatter` 不可变且线程安全，可以复用。系统默认时区、语言地区和解析策略应与实际需求一致，不要只凭格式相似就认为语义相同。

## 7. 旧 API：会识别和衔接即可

`Date` 实际表达一个时刻，以 Epoch 毫秒为基础；它不是只有年月日。`Date.toString()` 按系统默认时区显示，显示不同不代表内部时间戳变了。

`Calendar` 是旧日期时间运算工具，月份从零编号。`SimpleDateFormat` 是旧格式器，非线程安全，不应随意跨线程共享。新代码优先考虑 `java.time`。

```java
// 旧 Date 与现代 Instant 互转，转为 Date 只保留毫秒精度。
Instant instant = date.toInstant();
Date legacyDate = Date.from(instant);
```

## 8. 本轮结论与后续边界

**明确字段含义、解释约定和时间戳单位；需要转换时再转换，而不是看到时间就先转 UTC。**

用户已有 Epoch 统一模型和解释规则的口头总结，对本地时间自述无问题，并确认当前深度足够。其余类型和操作主要为讲解证据，没有本轮运行实验或独立应用评测。具体 API、复杂夏令时处理、`Clock` 注入、区域化显示等按需查阅，不作为继续学习的门槛。

## 参考与导航

- [廖雪峰 Java 教程：日期与时间](https://liaoxuefeng.com/books/java/datetime/index.html)：主要学习参考。
- [Java 官方文档：java.time](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/time/package-summary.html)：核对具体类型和行为；实际使用以项目 JDK 为准。
- [专题计划与讨论记录](Java日期与时间学习计划.md)：查看覆盖范围和教学接续背景。
- [进度](../进度.md)：继续学习的统一入口。
