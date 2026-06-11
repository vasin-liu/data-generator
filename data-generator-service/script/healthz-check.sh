#!/usr/bin/env bash
# HTTP liveness probe for keepalive, systemd, and manual checks.

set -euo pipefail

DG_BIN_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib/common.sh
source "${DG_BIN_DIR}/lib/common.sh"

dg_init_paths "$DG_BIN_DIR"
dg_load_config
dg_health_check
