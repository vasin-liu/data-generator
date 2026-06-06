#!/usr/bin/env bash
# Periodic health check with optional auto-restart (Linux cron).

set -euo pipefail

DG_BIN_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib/common.sh
source "${DG_BIN_DIR}/lib/common.sh"

DG_KEEPALIVE_MAX_FAIL="${DG_KEEPALIVE_MAX_FAIL:-3}"
DG_KEEPALIVE_CRON="${DG_KEEPALIVE_CRON:-*/5 * * * *}"
FAIL_FILE="${DG_BIN_DIR}/.failcount"

dg_record_fail() {
  local err_code="$1"
  dg_init_paths "$DG_BIN_DIR"
  dg_load_config
  local fail=0
  local restart=""
  if [[ -s "$FAIL_FILE" ]]; then
    fail="$(tail -n 1 "$FAIL_FILE" | sed -n 's/.*fail:[[:space:]]*\([0-9]*\).*/\1/p')"
  fi
  if [[ "$err_code" == "0" ]]; then
    fail=0
  else
    fail=$((fail + 1))
  fi
  if [[ "$fail" -ge "$DG_KEEPALIVE_MAX_FAIL" ]]; then
    fail=0
    restart="[RESTART]"
    dg_log_warn "Keepalive: ${DG_KEEPALIVE_MAX_FAIL} consecutive failures; restarting ${DG_SERVICE_NAME}"
    dg_service_stop || true
    dg_service_start || true
  fi
  echo "[$(date '+%Y%m%d_%H%M%S')]${restart} fail: ${fail}" | tee -a "$FAIL_FILE"
  local lines
  lines="$(wc -l < "$FAIL_FILE" | tr -d ' ')"
  if [[ "$lines" -gt 3000 ]]; then
    tail -n 500 "$FAIL_FILE" > "${FAIL_FILE}.tmp"
    mv "${FAIL_FILE}.tmp" "$FAIL_FILE"
  fi
}

dg_cron_add() {
  local drop="${1:-}"
  local script_name
  script_name="$(basename "$0")"
  local conf="/tmp/dg-cron-${script_name}-$$.txt"
  echo 'MAILTO=""' > "$conf"
  sudo crontab -l 2>/dev/null | grep -v "${DG_BIN_DIR}/${script_name}" | grep -v '^MAILTO' | grep -v '^#' | grep -v '^$' >> "$conf" || true
  if [[ -z "$drop" ]]; then
    echo "${DG_KEEPALIVE_CRON} ${DG_BIN_DIR}/${script_name} check" >> "$conf"
  fi
  sudo crontab "$conf"
  rm -f "$conf"
  dg_log_info "Crontab updated:"
  sudo crontab -l
}

case "${1:-}" in
  check)
    dg_init_paths "$DG_BIN_DIR"
    dg_load_config
    if dg_health_check; then
      dg_record_fail 0
      exit 0
    fi
    dg_record_fail 1
    exit 1
    ;;
  cronAdd) dg_cron_add ;;
  cronDrop) dg_cron_add drop ;;
  *)
    dg_log_error "Usage: $0 {check|cronAdd|cronDrop}"
    exit 1
    ;;
esac
