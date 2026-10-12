#!/usr/bin/env bash
set -euo pipefail

benchmark_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
argi_source="${ARGI_SOURCE:-${benchmark_root}/..}"

if [[ ! -x "${argi_source}/mvnw" ]]; then
  echo "Cannot find ARGI Maven Wrapper: ${argi_source}/mvnw" >&2
  exit 1
fi

echo "Installing current ARGI artifacts from ${argi_source}"
"${argi_source}/mvnw" -B -f "${argi_source}/pom.xml" -pl :argi-agent-framework -am install -DskipTests
