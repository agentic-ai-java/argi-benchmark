# agentic-spring-ai benchmark

这是 `agentic-spring-ai` 的独立 JMH 性能测试仓库。Graph 与 ReAct 分模块、分数据目录管理；LangChain4j 和 Google ADK Java 只作为测试设计参考，不参与性能排名。

## 目录

```text
graph-benchmarks/agentic-spring-ai/   Graph 调度、循环、并行、编译和 checkpoint
react-benchmarks/agentic-spring-ai/   单模型回合和一次工具往返
docs/benchmark-design.md              方法、矩阵、边界和验收标准
docs/evidence.csv                     调研证据与固定提交
scripts/                              上游准备、运行和摘要脚本
results/                              版本化原始数据
reports/                              人读性能结论与设备信息
```

## 环境

- JDK 17
- Maven 3.9+
- 同级目录中存在 `agentic-spring-ai`；也可设置 `AGENTIC_SPRING_AI_SOURCE`
- 可选：`jq`，用于生成 Markdown 摘要

## 运行

```shell
./scripts/prepare-agentic-spring-ai.sh
./scripts/run-benchmarks.sh quick
./scripts/run-benchmarks.sh full
./scripts/run-benchmarks.sh reference
./scripts/render-summary.sh results/<run-id>
```

快速模式只用于验证。完整模式运行全部场景；`reference` 使用相同的正式采样参数，专门运行状态宽度、并行交叉点、历史长度和多线程吞吐。需要提交或发布性能结论时必须保留生成的 JMH JSON、日志和 `environment.txt`。

详细口径见 [性能测试设计](docs/benchmark-design.md)。

当前本机完整结果见 [2026-09-03 性能报告](reports/2026-09-03-full.md)，原始数据位于 `results/20260903T141000Z/`。
