#!/usr/bin/env bash
# Start/stop the main Data Generator coordinator (API + console) process.

set -euo pipefail

DG_BIN_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib/common.sh
source "${DG_BIN_DIR}/lib/common.sh"

dg_main "$@"
