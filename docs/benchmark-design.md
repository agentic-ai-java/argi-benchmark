# 性能测试设计

最后验证日期：2026-09-03（Asia/Shanghai）

## 目标与边界

本项目只测试 `agentic-spring-ai`。LangChain4j 和 Google ADK Java 仅用于参考测试组织方式，不运行竞品代码、不输出跨框架排名。

核心问题如下：

1. Graph 在直线节点、条件循环和并行扇出规模增长时，延迟与分配量如何变化？
2. 图的冷路径 `compile()` 成本是多少？
3. `MemorySaver` checkpoint 相对无 checkpoint 的成本是多少？
4. ReAct 在排除网络和真实模型推理后，单模型回合和一次工具往返各引入多少框架开销？

所有热路径基准使用预构建对象。确定性 `ChatModel` 和本地工具均立即返回，避免网络抖动、限流和模型服务队列掩盖框架成本。

## 参考方案如何落地

LangChain4j 当前主分支没有仓库内 JMH 模块。其性能问题 [#4322](https://github.com/langchain4j/langchain4j/issues/4322) 提供了外置 JMH 结果，采用多档数据规模、同机基线、统一时间单位和 speedup；对应 [PR #4323](https://github.com/langchain4j/langchain4j/pull/4323) 只合入产品修复，没有把临时 benchmark 混入功能测试。

本项目继承这些原则，并增强为：

- JMH 独立 fork，避免同 JVM 污染；
- 预热与测量分离；
- 原始 JSON、运行日志和环境快照一并保留；
- 使用 `gc` profiler 记录 `gc.alloc.rate.norm`；
- 功能 smoke test 只校验语义，不用 `System.nanoTime()` 充当性能测试；
- Graph 与 ReAct 分模块、分结果目录。

Google ADK Java 的 `TestLlm`/workflow 测试采用预设响应隔离真实模型，本项目的 ReAct 假模型沿用这一“确定性替身”思路。

## 测试矩阵

| 套件 | 场景 | 参数 | 主要指标 |
| --- | --- | --- | --- |
| Graph | 直线图执行 | 节点数 1/10/50 | µs/op、bytes/op、相对直接 Java 基线 |
| Graph | 条件自循环 | 迭代 10/100/1000 | µs/op、每迭代增量、bytes/op |
| Graph | ALL_OF 并行扇出/汇合 | 分支 2/8/32 | µs/op、bytes/op |
| Graph | 图构建与编译 | 节点数 1/10/50 | µs/op、bytes/op |
| Graph | MemorySaver | 单节点、执行后释放 thread | µs/op、bytes/op |
| ReAct | 模型直调基线 | 单轮 | µs/op、bytes/op |
| ReAct | ReactAgent 单模型回合 | 单轮 | µs/op、bytes/op、相对模型直调开销 |
| ReAct | ReactAgent 工具往返 | 模型→工具→模型 | µs/op、bytes/op |

## 运行配置与验收

快速模式为 2 次预热、3 次测量、1 fork、每次 500 ms，只用于代码验证和趋势快照。完整模式为 5 次预热、8 次测量、3 forks、每次 1 s，才可作为发布结论。

完整结果的质量门槛：

- 所有场景零异常，smoke tests 全部通过；
- 每项至少 3 个独立 fork；
- 报告 JVM、OS、CPU 架构、上游与 benchmark 提交；
- 误差过大或热路径出现非单调突变时复跑并调查，不选择性删除异常值；
- 首次结果建立基线，不预设“更快即通过”；后续同机回归阈值建议为中位数退化不超过 10%。

## 不确定性

- JMH 测得的是当前机器、当前 JDK 下的框架开销，不代表真实 LLM 端到端时延。
- 并行节点没有人为 sleep，结果主要反映调度成本；并行收益应另用可控 CPU/IO 工作负载评估。
- quick 结果不能用于跨机器或版本的正式结论。
- 当前上游版本为开发提交，发布依赖树可能变化；每次数据必须固定提交重新生成。
