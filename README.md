# ARGI Benchmark

This separate Maven project measures Graph and ReAct orchestration using deterministic, framework-neutral chat clients and tools. It does not call remote models or require provider SDKs. Historical September 2026 results retain their original names and are not measurements of the neutral API replacement.

## 目录

```text
graph-benchmarks/argi/                Graph scheduling, loops, parallel execution and checkpoints
react-benchmarks/argi/                Model turns, tool round trips, history and shared throughput
docs/benchmark-design.md              方法、矩阵、边界和验收标准
docs/evidence.csv                     调研证据与固定提交
scripts/                              上游准备、运行和摘要脚本
results/                              版本化原始数据
reports/                              人读性能结论与设备信息
```

## 环境

- JDK 17
- Maven 3.9+
- Uses the parent ARGI checkout by default; set `ARGI_SOURCE` to override
- 可选：`jq`，用于生成 Markdown 摘要

## 运行

```shell
./scripts/prepare-argi.sh
./scripts/run-benchmarks.sh quick
./scripts/run-benchmarks.sh full
./scripts/run-benchmarks.sh reference
./scripts/run-benchmarks.sh reference-quick
./scripts/render-summary.sh results/<run-id>
```

快速模式只用于验证。完整模式运行全部场景；`reference` 使用正式采样参数，专门运行状态宽度、并行交叉点、历史长度和多线程吞吐；`reference-quick` 用于提交前趋势检查。需要提交或发布性能结论时必须保留生成的 JMH JSON、日志和 `environment.txt`。

详细口径见 [性能测试设计](docs/benchmark-design.md)。

当前本机完整结果见 [2026-09-03 性能报告](reports/2026-09-03-full.md)，原始数据位于 `results/20260903T141000Z/`。

面向日常框架开发的扩展结果见 [2026-09-03 开发参考基线](reports/2026-09-03-development-reference.md)，原始数据位于 `results/20260903T065000Z/`。
