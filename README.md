# Flash Idea 闪念

![Flash Idea 闪念项目封面](docs/assets/readme-cover.png)

Flash Idea 是一款面向“灵感捕捉、AI 整理、知识关联、行动转化”的 Android 应用。它不是普通笔记本，也不是单一聊天机器人，而是一个把用户的零散想法转化为可验证计划、关联图谱和可导出结论的 Agent 工作台。

当前仓库是 Flash Idea 复赛最终版的干净交付包：包含 Android 源码、可安装 APK、源码压缩包，以及面向演示和二次开发的说明。

## 一句话定位

把转瞬即逝的灵感，变成可整理、可追踪、可关联、可执行的个人智能工作流。

## 核心问题

很多创意型用户、学生和开发者并不缺想法，真正的问题是：

- 灵感出现得很快，但记录入口太重，容易丢失。
- 记录之后缺少整理，笔记很快变成信息坟场。
- 不同想法之间的关联不明显，难以形成系统性思考。
- 通用 AI 对话和本地笔记割裂，AI 很难真正理解用户自己的上下文。
- 多模型能力分散，用户不知道何时用端侧、何时用云端、何时用本地规则兜底。

Flash Idea 的产品目标，是把“记录”升级为“个人灵感 Agent 系统”。

## 核心能力

### 1. 低摩擦灵感捕捉

应用提供轻量笔记入口，用于快速记录 idea、prompt、观察、任务和片段。记录不是终点，而是后续 Agent 整理、标签生成、图谱连接和洞察输出的上下文来源。

### 2. Agent 工作台

Agent 对话页是产品的核心界面。用户可以把笔记主动交给 Agent，也可以直接提出问题，让 Agent 基于本地记忆和当前输入生成：

- 行动计划
- 验证步骤
- 风险检查
- 标签与摘要
- 延伸思考
- 可导出的结论

代码入口：`app/src/main/java/com/flashidea/app/ai/agent/`

### 3. 多模型统一路由

Flash Idea 支持把多种模型能力接入同一个 Agent：

- vivo 端侧模型接口预留
- vivo 云端大模型接口
- 用户自定义 OpenAI-compatible 第三方模型
- 本地规则兜底模型

产品策略是“端侧优先、云端增强、本地保底”。用户可以按隐私、网络、模型能力和任务类型进行切换，但对 Agent 来说，它们统一进入同一套工作流。

代码入口：

- `app/src/main/java/com/flashidea/app/ai/model/ModelRouter.kt`
- `app/src/main/java/com/flashidea/app/ai/model/ModelSelectionPolicy.kt`
- `app/src/main/java/com/flashidea/app/ai/model/cloud/CloudChatProvider.kt`
- `app/src/main/java/com/flashidea/app/ai/model/custom/CustomOpenAiProvider.kt`
- `app/src/main/java/com/flashidea/app/ai/model/local/RuleBasedFallbackProvider.kt`

### 4. 本地记忆与知识图谱

Flash Idea 会把笔记、标签、关联和 Agent 运行记录保存到本地数据库。知识图谱的 MVP 规则是：两个节点存在相同标签时自动建立关联，并在图谱界面中用连线表达。

这让用户看到“我过去写过的东西之间有什么关系”，而不是只看到一串孤立笔记。

代码入口：

- `app/src/main/java/com/flashidea/app/ai/memory/TagLinkBuilder.kt`
- `app/src/main/java/com/flashidea/app/ui/graph/GraphScreen.kt`
- `app/src/main/java/com/flashidea/app/ui/graph/GraphViewModel.kt`

### 5. Air：可交互 Agent IP

Air 是 Flash Idea 的像素云 Agent 形象。它不是装饰图标，而是产品里的可视化 Agent 资产：

- Idle：待机浮动与眨眼
- Receive：接住用户输入
- Thinking：思考时释放像素闪电
- Pipeline：处理不同阶段时显示小型阶段反馈
- Done：完成时跳动与高光反馈

Air 让 Agent 从抽象能力变成“住在产品里的灵感伙伴”，也是 Flash Idea 与普通笔记应用、普通 AI 对话页拉开差异的重要设计。

代码入口：`app/src/main/java/com/flashidea/app/ui/aichat/AirMascot.kt`

## 产品差异化

Flash Idea 的差异不在于“有 AI”，而在于把 AI 放进了一个完整的灵感工作流：

1. 从捕捉开始，而不是从聊天开始。
2. 从用户自己的笔记出发，而不是只依赖一次性 prompt。
3. 从多模型选择出发，但统一到一个 Agent 框架里。
4. 从结果文本出发，进一步形成标签、图谱、洞察和导出。
5. 用 Air 建立产品记忆点，让 Agent 能力具象化。

## 技术架构

```text
FlashIdea/
├── app/src/main/java/com/flashidea/app/
│   ├── ai/
│   │   ├── agent/        # Agent 计划、工具、工作流、运行记录
│   │   ├── memory/       # 本地记忆、图谱构建、标签关联
│   │   └── model/        # 多模型 Provider、路由、策略、兜底
│   ├── data/
│   │   ├── local/        # Room 实体与 DAO
│   │   └── repository/   # 数据访问封装
│   ├── service/          # 快捷入口、系统能力接入
│   └── ui/               # Compose 界面、Air、图谱、笔记、设置
├── release/
│   ├── FlashIdea-debug.apk
│   └── FlashIdea-source.zip
└── scripts/
    └── build.ps1
```

技术栈：

- Kotlin
- Jetpack Compose
- Room
- Hilt
- Retrofit / OkHttp
- Android Shortcut / Quick Settings / Accessibility 相关能力
- OpenAI-compatible Chat API 形式的模型接入

## 快速体验

APK：

```text
release/FlashIdea-debug.apk
```

源码包：

```text
release/FlashIdea-source.zip
```

建议体验路径：

1. 安装 APK。
2. 新建几条带有共同主题的灵感笔记。
3. 打开 Agent 工作台，观察 Air 的状态反馈。
4. 切换模型策略，体验端侧/云端/自定义/本地兜底的统一入口。
5. 让 Agent 基于笔记生成行动计划或验证步骤。
6. 打开知识图谱，观察相同标签笔记之间的连线。
7. 到设置页查看导出、模型配置和本地优先策略。

## 构建

要求：

- JDK 17
- Android SDK，compile SDK 34

运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

或：

```powershell
.\gradlew.bat assembleDebug
```

## AI 配置

复制：

```text
local.properties.example -> local.properties
```

然后填写本机 SDK 路径和可选模型凭据。

`local.properties` 已被 Git 忽略。不要把 AppID、API Key、模型密钥提交到仓库。

## 给开发者和 Agent 的阅读路径

如果你要快速理解项目，用这个顺序：

1. 先读本 README，理解产品定位和核心卖点。
2. 看 `app/src/main/java/com/flashidea/app/ui/aichat/`，理解 Agent 对话页和 Air。
3. 看 `app/src/main/java/com/flashidea/app/ai/agent/`，理解 Agent 工作流。
4. 看 `app/src/main/java/com/flashidea/app/ai/model/`，理解多模型路由。
5. 看 `app/src/main/java/com/flashidea/app/ai/memory/` 和 `ui/graph/`，理解知识图谱。
6. 安装 APK 真机体验，再开始写 PPT 或录制视频。

## 宣传材料关键词

产品宣传时请优先强调：

- 闪念捕捉
- Agent 工作台
- 多模型统一接入
- 本地优先与隐私边界
- 知识图谱关联
- Air 像素云 Agent IP
- 从灵感到计划、验证、结论的闭环
