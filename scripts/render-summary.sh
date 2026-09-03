#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "用法: $0 <results/<run-id>>" >&2
  exit 2
fi

result_root="$1"
if ! command -v jq >/dev/null 2>&1; then
  echo "需要 jq 才能从 JMH JSON 生成摘要" >&2
  exit 1
fi

echo '| suite | benchmark | params | score | error | unit | bytes/op |'
echo '| --- | --- | --- | ---: | ---: | --- | ---: |'

for suite in graph react; do
  jq -r --arg suite "${suite}" '
    .[] |
    ($suite + "|" +
     (.benchmark | split(".") | last) + "|" +
     ((.params // {}) | to_entries | map(.key + "=" + .value) | join(", ")) + "|" +
     (.primaryMetric.score | tostring) + "|" +
     (.primaryMetric.scoreError | tostring) + "|" +
     .primaryMetric.scoreUnit + "|" +
     ((.secondaryMetrics["gc.alloc.rate.norm"].score // 0) | tostring))
  ' "${result_root}/${suite}/jmh.json" |
    while IFS='|' read -r row_suite benchmark params score error unit allocation; do
      printf '| %s | %s | %s | %.3f | %.3f | %s | %.1f |\n' \
        "${row_suite}" "${benchmark}" "${params:--}" "${score}" "${error}" "${unit}" "${allocation}"
    done
done
