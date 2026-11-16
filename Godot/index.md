# Godot .NET：2D 独立开发学习

以 Godot 官方文档为主线，建立独立制作小体量 2D 游戏的能力。主要语言为 C#，重心是引擎机制、使用判断与实际验证，不是重学编程基础或遍历引擎功能。

## 工作区入口

| 资料 | 内容摘要 | 什么时候读取 |
| --- | --- | --- |
| [学习计划](plan.md) | 目标、边界、能力依赖、阶段实践与测评、近期三个单元设计 | 理解路线、调整范围或准备教学 |
| [进度快照](progress.md) | 已确认背景、本机环境证据、当前状态、缺口与接续位置 | 跨会话继续、判断能否压缩或需要补强 |

已开始入门讲解，实际操作与理解尚待反馈，当前接续见[进度快照](progress.md)。没有实践代码、课后笔记或评测产物，因此不预建其他目录。

## 官方资料地图与导航

核读日期：2026-11-15。已访问用户指定的[介绍入口](https://docs.godotengine.org/zh-cn/4.x/getting_started/introduction/index.html)，并以官方随页提供的[正文源文本及直属目录](https://docs.godotengine.org/zh-cn/4.x/_sources/getting_started/introduction/index.rst.txt)校对范围，避免把全站侧栏当作本节内容。源文本为英文；中文页面是主要阅读入口。

### 指定入口的实际范围

它回答“引擎适不适合我、有什么能力、怎样工作、怎样使用界面与学习资料”。直属六页如下，已核读正文：

| 页面 | 实际作用与取舍 |
| --- | --- |
| [Godot 简介](https://docs.godotengine.org/zh-cn/4.x/getting_started/introduction/introduction_to_godot.html) | 引擎用途、编辑器工具、语言选择与编程前提；保留引擎认知，不补教已有的 OOP 基础 |
| [学习用 GDScript 编程](https://docs.godotengine.org/zh-cn/4.x/getting_started/introduction/learn_to_code_with_gdscript.html) | 面向编程初学者，指向 Learn GDScript From Zero；不作为本路线的前置课程 |
| [Godot 关键概念概述](https://docs.godotengine.org/zh-cn/4.x/getting_started/introduction/key_concepts_overview.html) | 场景、节点、场景树、信号的第一层模型；需要学，不能由 C# 经验替代 |
| [初识 Godot 界面](https://docs.godotengine.org/zh-cn/4.x/getting_started/introduction/first_look_at_the_editor.html) | 项目管理器、工作区、场景/文件系统/检查器面板、运行入口和内置类参考；以实际版本定位，非背按钮 |
| [学习新特性](https://docs.godotengine.org/zh-cn/4.x/getting_started/introduction/learning_new_features.html) | 区分用户手册与类参考，使用搜索、提出带错误与场景树证据的问题；不要求接入社区或第三方工具 |
| [Godot 的设计理念](https://docs.godotengine.org/zh-cn/4.x/getting_started/introduction/godot_design_philosophy.html) | 面向对象与场景组合、一体化工具、开源、编辑器自身运行于引擎、独立 2D/3D；重点保留设计与组织方式 |

这些页面不是完整的 2D 游戏制作教程，也不是 C# 集成手册。场景实例、生命周期、物理、完整玩法与导出需继续导航。

### 后续主线与补充资料

| 资料 | 已核读的范围 | 学习中如何使用 |
| --- | --- | --- |
| [渐进式教程](https://docs.godotengine.org/zh-cn/4.x/getting_started/step_by_step/index.html) | 总览、节点与场景、创建实例、第一个脚本、信号正文；目录还包含语言选择与输入 | 建立可运行场景、复用、脚本和通信；不是按小节照抄。第一个脚本及信号已有 C# 代码页签 |
| [你的第一个 2D 游戏](https://docs.godotengine.org/zh-cn/4.x/getting_started/first_2d_game/index.html) | 总览及前置要求、玩家代码、主场景、HUD、收尾正文；设置项目/玩家场景/敌人页仅确认目录，教学前再读正文 | 用 Dodge the Creeps 检查完整流程；整合移动、生成、得分、失败、重玩、音效；不是最终独立能力证据 |
| [C#/.NET](https://docs.godotengine.org/zh-cn/4.x/tutorials/scripting/c_sharp/index.html)与[C# 基础](https://docs.godotengine.org/zh-cn/4.x/tutorials/scripting/c_sharp/c_sharp_basics.html) | .NET 版本要求、SDK、VS Code 配置、工程生成、命名和重建注意事项 | 只学习 Godot 集成层，不重讲 C#；VS Code 说明在“C# 基础”页内，不另设不存在的外部编辑器专页 |
| [C# 信号](https://docs.godotengine.org/zh-cn/4.x/tutorials/scripting/c_sharp/c_sharp_signals.html) | 内置信号事件、自定义委托、发射、自动断连及其例外 | 结合节点生命周期教学，不能把普通 C# 事件经验直接当作 Godot 信号管理能力 |
| [物理介绍](https://docs.godotengine.org/zh-cn/4.x/tutorials/physics/physics_introduction.html) | 四种碰撞对象、形状、物理步进、层与掩码、移动方法边界 | 区分检测与阻挡，理解 Area2D / CharacterBody2D / StaticBody2D / RigidBody2D 的选型；纠正把教程节点泛化为全部游戏规则 |
| [场景组织](https://docs.godotengine.org/zh-cn/4.x/tutorials/best_practices/scene_organization.html) | 独立子场景、依赖注入、祖先协调通信、按存在依赖组织树 | 在有实际复用问题后回读，不一开始灌输框架或全局单例 |

按需补读但本次未全文核读：[Node 类参考](https://docs.godotengine.org/zh-cn/4.x/classes/class_node.html)用于生命周期精确定义，[C# API 差异](https://docs.godotengine.org/zh-cn/4.x/tutorials/scripting/c_sharp/c_sharp_differences.html)、[导出属性](https://docs.godotengine.org/zh-cn/4.x/tutorials/scripting/c_sharp/c_sharp_exports.html)用于语言绑定，[2D 手册](https://docs.godotengine.org/zh-cn/4.x/tutorials/2d/index.html)、[调试手册](https://docs.godotengine.org/zh-cn/4.x/tutorials/debug/index.html)、[导出项目](https://docs.godotengine.org/zh-cn/4.x/tutorials/export/exporting_projects.html)用于实际开发问题。教学前读取对应正文，不凭链接标题补造内容。

## 通用引擎知识与语言绑定

节点、场景实例、场景树、生命周期、资源、输入、物理和信号机制属于引擎知识；C# 的 partial 节点类、回调签名、Export/Signal 特性、事件订阅、程序集构建及 API 命名属于绑定知识。普通 C# 语法与 OOP 已有基础，按需对照，不另排基础课程。

示例优先使用官方 C# 页签；只有 GDScript 时，先解释引擎行为再给等价 C#。涉及实践时须说明脚本挂载节点、所需子节点、属性和连接方式，并通过构建与运行观察验证；不能只替换大小写或把 AI 生成代码当成证据。

## 版本与范围限制

`4.x` 是会更新的文档分支入口，不代表固定的 4.7.2 快照。本机实际版本和核验边界只维护在[进度快照](progress.md#环境核验)。具体 API 以实际引擎、对应版本参考和可运行实验交叉确认；中文内容滞后或含糊时核对官方英文同页。SDK 版本、项目目标框架和 Godot.NET.Sdk 版本是不同概念，不因 SDK 为 9 就擅自改项目目标框架。

当前官方 C# 页面仍明确说明 Godot 4 C# 项目不能导出 Web，移动平台另有实验性限制。因此初始实践以本机 Windows 桌面为工作假设，最终发布平台仍待后续确认；若目标改为浏览器，应先重新评估路线，不能直接承诺支持。
