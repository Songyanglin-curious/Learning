# C# 节点脚本与帧更新

适用 Godot 4.x .NET；2026-11-17 整理。记录最小脚本接入与旋转示例的机制，不表示完整生命周期已学完。

## 脚本怎样接入节点

给 Sprite2D 挂载继承 Sprite2D 的 C# 节点脚本后，脚本扩展该节点的行为。脚本中的 this 代表带脚本的节点，可直接使用其 Name、Rotation 等继承属性，不需要自己编写 Main 来每帧调用它。

脚本文件名与类名保持一致；使用 partial，供 Godot 的源生成器补充绑定代码。partial 在类声明中，不在帧回调方法上。

```csharp
using Godot;

public partial class SpinDemo : Sprite2D
{
    public override void _Ready()
    {
        GD.Print($"节点 {Name} 已就绪");
    }

    public override void _Process(double delta)
    {
        Rotation += Mathf.Pi * (float)delta;
    }
}
```

此例需挂在有 Texture、位于运行可见区域的 Sprite2D 上。保存并构建后运行该场景。C# 源码修改需要编译生效，安装 SDK 不等于项目已经构建成功。

## 回调与时间

_Ready 是就绪阶段回调，不等同于 C# 构造函数；父子生命周期与重入树行为尚待单独展开。正常启用处理时，_Process 每帧由引擎调用。

delta 是本次帧更新的时间间隔（秒），不是启动以来的累计时间。恒定角速度的增量更新为：

`新角度 = 当前角度 + 角速度 × 本帧时间`

Rotation 的单位为弧度，描述节点相对父节点坐标系的旋转；RotationDegrees 提供同一旋转的角度制访问。π 弧度/秒即 180°/秒，若本帧 delta 为 0.1 秒，本次增加 18°。从初始 30° 开始，前三次同样更新后是 48°、66°、84°。

- `Rotation += ω * delta`：累加本帧增量。
- `Rotation = ω * delta`：直接设置本帧角度，delta 恒定时会停在固定角度，不是持续旋转。

乘 delta 用于把“每秒的速度”换算为“这一帧的变化量”，避免简单固定每帧增量造成速度随帧率改变。此处是普通视觉旋转，不是物理移动或碰撞响应的通用实现。

## 开启与关闭帧处理

`SetProcess(bool)` 与 `IsProcessing()` 定义在 Node 上，Sprite2D 等节点继承后可直接调用。`SetProcess(true)` 启用处理，之后每个绘制帧会收到处理通知，若重写了 `_Process` 就会被调用；`IsProcessing()` 返回当前是否启用。

如果类中重写了 `_Process`，引擎会在 `_Ready` 之前自动启用处理，所以初始 `IsProcessing()` 为 true。

边界：

- 只影响 `_Process`，不影响 `_PhysicsProcess` 等回调；要整个节点停下应改 `ProcessMode`，而不是只调 SetProcess(false)。
- 关闭处理不影响信号回调。按钮的 pressed 仍会触发，因为它走输入与信号分发，不经过 `_Process`；这正好区分“帧更新通道”与“事件通道”。
- `SetProcess(!IsProcessing())` 是二元切换，可作为暂停/恢复开关。

依据：本机随附 GodotSharp 4.7.2 的 XML 文档（Godot.Node.SetProcess / IsProcessing）。

## 导出属性：代码默认值与场景配置

给脚本属性添加 [Export]，即可由检查器编辑并将值保存到场景。公开属性本身不等于导出：

```csharp
[Export]
public float DegreesPerSecond { get; set; } = 180.0f;

public override void _Process(double delta)
{
    RotationDegrees += DegreesPerSecond * (float)delta;
}
```

此段是在同一个节点脚本类中的成员示意，不能与原有同签名 _Process 重复保留。速度单位为度/秒；90 比 180 慢一半，负值改变方向。

代码初始化提供默认值。场景加载时可应用保存的导出属性值，所以源码写 180，不代表运行时必然为 180。可在 _Ready 中打印 DegreesPerSecond 来观察本次实际值；构造函数中不能假定场景导出值已应用完成。完整生命周期另行讨论。

新增或修改 C# 导出属性后，保存源码并构建项目。若检查器提示 “This inspector might be out of date. Please build the C# project.”，先构建，再重新选中挂脚本的节点。属性通常出现在脚本分区，以 Degrees Per Second 显示，不在 Node2D 的变换分区。构建失败先处理首条构建错误，不必先重挂脚本或重建场景。

## GDScript 与 C#：不只是语法不同

两者扩展同一套 Godot 节点，场景、物理、生命周期等核心引擎机制并不因此换一套实现。区别主要在执行环境、绑定、编辑器工作流与部署边界。

- GDScript 是 Godot 专用语言，不是 Python；支持渐进式类型，与内置脚本编辑器、节点及信号提示紧密集成。
- C# 经过编译，由 .NET 执行，经 C# 绑定访问引擎；可用成熟 IDE、静态类型工具与兼容的 .NET 库。库能否用于目标平台仍须单独核对。
- C# 存在托管 GC，不能因此把 Godot 原生 Node 的释放交给普通托管引用生命周期推断。GDScript 没有 .NET 式追踪 GC，部分对象引用计数，Node 仍有自己的释放规则。
- 脚本自身的大量计算中 C# 通常更有优势；如果主要调用底层 C++ 引擎，改语言不一定明显提速，先定位实际瓶颈。
- 核读时官方文档仍注明 Godot 4 C# 不能导出 Web；此为版本相关限制，选平台时复查。

对已有 C# 开发经验的学习者，继续使用 C#、重点学习绑定和引擎边界是合理路线。GDScript 可先做到能阅读教程；同项目混用受支持，但会引入互操作和类型转换问题，不需为了学习刻意混用。

本节为已讲解的选型边界，未据此认定用户已掌握跨语言互操作。

## 当前证据与接续

用户自述“现在旋转起来了”，支持最小脚本挂载、构建生效和帧回调运行；后续截图显示旧版 Ready 就绪日志与节点脚本挂载；未另读实际工程，VS Code 断点未验证。用户曾询问 partial 位置与 delta 时间基准，已讲解，尚无迁移性解释证据。

[Export] 属性一度未显示，截图含需重新构建提示；用户在构建指导后回复“没问题了”，记录为显示障碍已解决（用户反馈），不额外推定已验证 90 度/秒或新的速度日志。导出属性子单元暂告收束；接续为构造、入树与就绪时机。完整节点生命周期、节点引用及物理帧回调尚未展开，不在本笔记中假定掌握。

## 来源

- [官方脚本语言说明](https://docs.godotengine.org/en/stable/getting_started/step_by_step/scripting_languages.html)：语言执行环境、集成、性能与平台取舍，2026-11-17 核读。

- [官方首个脚本教程（含 C#）](https://docs.godotengine.org/en/stable/getting_started/step_by_step/scripting_first_script.html)：脚本挂载、继承、构建与帧旋转示例。stable 是动态文档分支，具体行为需结合实际版本。
