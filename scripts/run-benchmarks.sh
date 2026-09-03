#!/usr/bin/env bash
set -euo pipefail

benchmark_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
agentic_source="${AGENTIC_SPRING_AI_SOURCE:-${benchmark_root}/../agentic-spring-ai}"
mode="${1:-quick}"
run_id="${RUN_ID:-$(date -u +%Y%m%dT%H%M%SZ)}"
result_root="${benchmark_root}/results/${run_id}"
user_home="${HOME:-}"

record_hardware() {
  if command -v system_profiler >/dev/null 2>&1; then
    system_profiler SPHardwareDataType |
      awk -F ': ' '/Model Name:|Model Identifier:|Model Number:|Chip:|Total Number of Cores:|Memory:/ {
        key=$1; value=$2; gsub(/^[[:space:]]+|[[:space:]]+$/, "", key); gsub(/ /, "_", key);
        print "hardware_" tolower(key) "=" value
      }'
  fi
  if command -v sw_vers >/dev/null 2>&1; then
    sw_vers | awk -F ':[[:space:]]*' '{ key=$1; value=$2; gsub(/ /, "_", key); print "os_" tolower(key) "=" value }'
  fi
}

record_power() {
  pmset -g batt | awk '{ gsub(/\(id=[0-9]+\)/, ""); output = output (NR > 1 ? " " : "") $0 } END { print output }'
}

sanitize_result_file() {
  local result_file="$1"
  if [[ -n "${user_home}" ]]; then
    BENCHMARK_USER_HOME="${user_home}" perl -pi -e 's/\Q$ENV{BENCHMARK_USER_HOME}\E/<HOME>/g' "${result_file}"
  fi
}

case "${mode}" in
  quick)
    # 快速验证用于发现回归和生成非权威快照。
    jmh_options=(-wi 2 -i 3 -w 500ms -r 500ms -f 1)
    graph_include='.*'
    react_include='.*'
    ;;
  full)
    # 显式传参，避免不同 Bash 版本对空数组展开的行为差异。
    jmh_options=(-wi 5 -i 8 -w 1s -r 1s -f 3)
    graph_include='.*'
    react_include='.*'
    ;;
  reference)
    # 日常开发参考矩阵：状态宽度、并行交叉点、历史长度和多线程吞吐。
    jmh_options=(-wi 5 -i 8 -w 1s -r 1s -f 3)
    graph_include='.*(StateWidthGraphBenchmark|ParallelCrossoverGraphBenchmark|GraphThroughputBenchmark).*'
    react_include='.*(ReactHistoryBenchmark|ReactThroughputBenchmark).*'
    ;;
  reference-quick)
    # 提交前趋势检查；正式结论仍以 reference 为准。
    jmh_options=(-wi 2 -i 3 -w 500ms -r 500ms -f 1)
    graph_include='.*(StateWidthGraphBenchmark|ParallelCrossoverGraphBenchmark|GraphThroughputBenchmark).*'
    react_include='.*(ReactHistoryBenchmark|ReactThroughputBenchmark).*'
    ;;
  *)
    echo "用法: $0 [quick|full|reference|reference-quick]" >&2
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
  echo "os=$(uname -srvmp)"
  record_hardware
  if command -v pmset >/dev/null 2>&1; then
    echo "power_at_start=$(record_power)"
  fi
  java -version
  mvn -version
} >"${result_root}/environment.txt" 2>&1

mvn -B -q -f "${benchmark_root}/pom.xml" verify

java -jar "${benchmark_root}/graph-benchmarks/agentic-spring-ai/target/benchmarks.jar" \
  "${graph_include}" "${jmh_options[@]}" -prof gc -rf json -rff "${result_root}/graph/jmh.json" \
  >"${result_root}/graph/jmh.log"

java -jar "${benchmark_root}/react-benchmarks/agentic-spring-ai/target/benchmarks.jar" \
  "${react_include}" "${jmh_options[@]}" -prof gc -rf json -rff "${result_root}/react/jmh.json" \
  >"${result_root}/react/jmh.log"

for result_file in \
  "${result_root}/environment.txt" \
  "${result_root}/graph/jmh.json" \
  "${result_root}/graph/jmh.log" \
  "${result_root}/react/jmh.json" \
  "${result_root}/react/jmh.log"; do
  sanitize_result_file "${result_file}"
done

{
  if command -v pmset >/dev/null 2>&1; then
    echo "power_at_end=$(record_power)"
  fi
  echo "completed_at_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
} >>"${result_root}/environment.txt"
echo "结果目录: ${result_root}"
