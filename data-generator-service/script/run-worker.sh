#!/usr/bin/env bash
# Start/stop the distributed worker process (profile distributed-worker).

set -euo pipefail

export DG_SERVICE_ROLE=worker
export DG_SPRING_PROFILES_ACTIVE="${DG_SPRING_PROFILES_ACTIVE:-distributed-worker}"

DG_BIN_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib/common.sh
source "${DG_BIN_DIR}/lib/common.sh"

dg_main "$@"
