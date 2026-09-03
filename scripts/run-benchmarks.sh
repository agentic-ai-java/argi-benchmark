#!/usr/bin/env bash
set -euo pipefail

benchmark_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
agentic_source="${AGENTIC_SPRING_AI_SOURCE:-${benchmark_root}/../agentic-spring-ai}"
mode="${1:-quick}"
run_id="${RUN_ID:-$(date -u +%Y%m%dT%H%M%SZ)}"
result_root="${benchmark_root}/results/${run_id}"

case "${mode}" in
  quick)
    # 快速验证用于发现回归和生成非权威快照。
    jmh_options=(-wi 2 -i 3 -w 500ms -r 500ms -f 1)
    ;;
  full)
    # 完整模式使用源码注解中的 5 次预热、8 次测量和 3 个 fork。
    jmh_options=()
    ;;
  *)
    echo "用法: $0 [quick|full]" >&2
    exit 2
    ;;
esac

mkdir -p "${result_root}/graph" "${result_root}/react"

{
  echo "run_id=${run_id}"
  echo "mode=${mode}"
  echo "started_at_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "benchmark_commit=$(git -C "${benchmark_root}" rev-parse HEAD 2>/dev/null || echo uncommitted)"
  echo "agentic_spring_ai_commit=$(git -C "${agentic_source}" rev-parse HEAD)"
  echo "os=$(uname -a)"
  java -version
  mvn -version
} >"${result_root}/environment.txt" 2>&1

mvn -B -q -f "${benchmark_root}/pom.xml" verify

java -jar "${benchmark_root}/graph-benchmarks/agentic-spring-ai/target/benchmarks.jar" \
  "${jmh_options[@]}" -prof gc -rf json -rff "${result_root}/graph/jmh.json" \
  >"${result_root}/graph/jmh.log"

java -jar "${benchmark_root}/react-benchmarks/agentic-spring-ai/target/benchmarks.jar" \
  "${jmh_options[@]}" -prof gc -rf json -rff "${result_root}/react/jmh.json" \
  >"${result_root}/react/jmh.log"

echo "completed_at_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)" >>"${result_root}/environment.txt"
echo "结果目录: ${result_root}"
