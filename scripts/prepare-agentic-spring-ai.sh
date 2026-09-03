#!/usr/bin/env bash
set -euo pipefail

# 默认使用本仓库同级的 agentic-spring-ai，也可通过环境变量指定源码位置。
benchmark_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
agentic_source="${AGENTIC_SPRING_AI_SOURCE:-${benchmark_root}/../agentic-spring-ai}"

if [[ ! -x "${agentic_source}/mvnw" ]]; then
  echo "找不到 agentic-spring-ai Maven Wrapper: ${agentic_source}/mvnw" >&2
  exit 1
fi

echo "准备上游制品: ${agentic_source}"
git -C "${agentic_source}" status --short --branch
git -C "${agentic_source}" rev-parse HEAD

"${agentic_source}/mvnw" -q \
  -f "${agentic_source}/pom.xml" \
  -pl :agentic-spring-ai-agent-framework \
  -am install \
  -DskipTests \
  -Dspring-javaformat.skip=true \
  -Dspotless.check.skip=true \
  -Dcheckstyle.skip=true \
  -Dlicense.skip=true \
  -Denforcer.skip=true
