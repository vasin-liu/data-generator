#!/usr/bin/env bash
# Shared runtime helpers for Data Generator packaged deployments (Linux/macOS).
# Sourced by bin/run.sh, run-worker.sh, healthz-check.sh, keepalive.sh.

set -euo pipefail

dg_now() {
  date '+%Y-%m-%d %H:%M:%S'
}

dg_log() {
  local level="$1"
  shift
  printf '[%s] [%s] %s\n' "$(dg_now)" "$level" "$*" >&2
}

dg_log_info() { dg_log "INFO" "$@"; }
dg_log_warn() { dg_log "WARN" "$@"; }
dg_log_error() { dg_log "ERROR" "$@"; }

dg_ensure_dir() {
  local dir="$1"
  if [[ ! -d "$dir" ]]; then
    mkdir -p "$dir" || {
      dg_log_error "Failed to create directory: $dir"
      return 1
    }
  fi
}

# Initializes BIN_DIR, ROOT_DIR, and default paths from the calling script directory.
dg_init_paths() {
  local bin_dir="$1"
  BIN_DIR="$(cd "$bin_dir" && pwd)"
  ROOT_DIR="$(cd "${BIN_DIR}/.." && pwd)"
  LOG_DIR="${DG_LOG_DIR:-${ROOT_DIR}/logs}"
  CONF_DIR="${ROOT_DIR}/conf"
  LIB_DIR="${ROOT_DIR}/lib"
  JVMDUMP_DIR="${DG_JVMDUMP_DIR:-${ROOT_DIR}/jvmdump}"
  DG_SERVICE_NAME="${DG_SERVICE_NAME:-data-generator-service}"
  DG_SERVICE_ROLE="${DG_SERVICE_ROLE:-coordinator}"
  LOG_FILE="${DG_LOG_FILE:-${LOG_DIR}/${DG_SERVICE_NAME}.log}"
  PID_FILE="${DG_PID_FILE:-${ROOT_DIR}/${DG_SERVICE_NAME}.pid}"
  DG_SERVER_PORT="${DG_SERVER_PORT:-9876}"
  DG_HEALTH_URL="${DG_HEALTH_URL:-http://127.0.0.1:${DG_SERVER_PORT}/healthz}"
  DG_START_WAIT_SEC="${DG_START_WAIT_SEC:-5}"
  DG_STOP_TIMEOUT_SEC="${DG_STOP_TIMEOUT_SEC:-30}"
  DG_DAEMON="${DG_DAEMON:-1}"
  DG_JAVA_MIN_VERSION="${DG_JAVA_MIN_VERSION:-25}"
  DG_JAVA_PROMPT="${DG_JAVA_PROMPT:-0}"
  DG_MAIN_CLASS="${DG_MAIN_CLASS:-org.gensokyo.data.DataGeneratorWorkerApplication}"
  dg_apply_role_defaults
}

dg_apply_role_defaults() {
  if [[ "${DG_SERVICE_ROLE}" == "worker" ]]; then
    if [[ -n "${DG_WORKER_SERVICE_NAME:-}" ]]; then
      DG_SERVICE_NAME="${DG_WORKER_SERVICE_NAME}"
    elif [[ "${DG_SERVICE_NAME}" == "data-generator-service" ]]; then
      DG_SERVICE_NAME="data-generator-worker"
    fi
    LOG_FILE="${DG_LOG_FILE:-${LOG_DIR}/${DG_SERVICE_NAME}.log}"
    PID_FILE="${DG_PID_FILE:-${ROOT_DIR}/${DG_SERVICE_NAME}.pid}"
  fi
}

dg_load_config() {
  local env_file="${ROOT_DIR}/conf/service.env"
  if [[ -f "$env_file" ]]; then
    dg_log_info "Loading configuration: ${env_file}"
    set -a
    # shellcheck disable=SC1090
    source <(sed 's/\r$//' "$env_file")
    set +a
    dg_init_paths "${BIN_DIR}"
  fi
  dg_apply_role_defaults
}

dg_java_major_version() {
  local java_cmd="$1"
  "$java_cmd" -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F. '{print ($1 == "1") ? $2 : $1}' | head -n 1
}

dg_java_version_ok() {
  local java_cmd="$1"
  local min_version="$2"
  local major
  major="$(dg_java_major_version "$java_cmd")"
  [[ -n "$major" && "$major" -ge "$min_version" ]]
}

dg_resolve_java() {
  local java_cmd=""
  local candidate=""

  if [[ -n "${DG_JAVA_HOME:-}" && -x "${DG_JAVA_HOME}/bin/java" ]]; then
    candidate="${DG_JAVA_HOME}/bin/java"
    if dg_java_version_ok "$candidate" "$DG_JAVA_MIN_VERSION"; then
      java_cmd="$candidate"
    else
      dg_log_warn "DG_JAVA_HOME Java version < ${DG_JAVA_MIN_VERSION}: ${candidate}"
    fi
  fi

  if [[ -z "$java_cmd" && -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
    candidate="${JAVA_HOME}/bin/java"
    if dg_java_version_ok "$candidate" "$DG_JAVA_MIN_VERSION"; then
      java_cmd="$candidate"
    else
      dg_log_warn "JAVA_HOME Java version < ${DG_JAVA_MIN_VERSION}: ${candidate}"
    fi
  fi

  if [[ -z "$java_cmd" ]] && command -v java >/dev/null 2>&1; then
    candidate="$(command -v java)"
    if dg_java_version_ok "$candidate" "$DG_JAVA_MIN_VERSION"; then
      java_cmd="$candidate"
    fi
  fi

  if [[ -z "$java_cmd" && "${DG_JAVA_PROMPT}" == "1" && -t 0 ]]; then
    dg_log_warn "JDK ${DG_JAVA_MIN_VERSION}+ not found in DG_JAVA_HOME, JAVA_HOME, or PATH."
    while [[ -z "$java_cmd" ]]; do
      read -r -p "Enter JDK ${DG_JAVA_MIN_VERSION}+ home path: " input_home
      if [[ -x "${input_home}/bin/java" ]] && dg_java_version_ok "${input_home}/bin/java" "$DG_JAVA_MIN_VERSION"; then
        java_cmd="${input_home}/bin/java"
        dg_log_info "Using interactive Java: ${java_cmd}"
      else
        dg_log_error "Invalid JDK path or version: ${input_home}"
      fi
    done
  fi

  if [[ -z "$java_cmd" ]]; then
    dg_log_error "JDK ${DG_JAVA_MIN_VERSION}+ is required."
    dg_log_error "Set DG_JAVA_HOME in conf/service.env (see conf/service.env.example)."
    return 1
  fi

  JAVA_CMD="$java_cmd"
  dg_log_info "Java: $("$JAVA_CMD" -version 2>&1 | head -n 1)"
}

dg_find_app_jar() {
  local pattern="${DG_SERVICE_NAME}-*.jar"
  APP_JAR="$(find "${BIN_DIR}" -maxdepth 1 -type f -name "$pattern" 2>/dev/null | sort -V | tail -n 1)"
  if [[ -z "$APP_JAR" ]]; then
    dg_log_error "Application JAR not found in ${BIN_DIR} (pattern: ${pattern})"
    return 1
  fi
  dg_log_info "Application JAR: ${APP_JAR}"
}

dg_build_jvm_opts() {
  JVM_OPTS=()
  if [[ "${DG_DAEMON}" == "0" ]]; then
    JVM_OPTS+=(-XX:MaxRAMPercentage=95.0)
  else
    JVM_OPTS+=(-Xms"${DG_HEAP_MIN:-512m}" -Xmx"${DG_HEAP_MAX:-1g}")
    JVM_OPTS+=(-XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=256m)
  fi
  JVM_OPTS+=(-XX:+UseG1GC)
  JVM_OPTS+=(-XX:+HeapDumpOnOutOfMemoryError)
  JVM_OPTS+=(-XX:HeapDumpPath="${JVMDUMP_DIR}/${DG_SERVICE_NAME}.hprof")
  if [[ -n "${DG_JVM_OPTS:-}" ]]; then
    # shellcheck disable=SC2206
    JVM_OPTS+=(${DG_JVM_OPTS})
  fi
}

dg_build_spring_args() {
  SPRING_ARGS=(--spring.config.location="${CONF_DIR}/")
  if [[ -n "${DG_SPRING_PROFILES_ACTIVE:-}" ]]; then
    SPRING_ARGS+=(--spring.profiles.active="${DG_SPRING_PROFILES_ACTIVE}")
  fi
  if [[ -f "${CONF_DIR}/logback-spring.xml" ]]; then
    SPRING_ARGS+=(--logging.config="${CONF_DIR}/logback-spring.xml")
  fi
  if [[ -n "${DG_SERVER_PORT:-}" ]]; then
    SPRING_ARGS+=(--server.port="${DG_SERVER_PORT}")
  fi
  if [[ -n "${DG_SPRING_ARGS:-}" ]]; then
    # shellcheck disable=SC2206
    SPRING_ARGS+=(${DG_SPRING_ARGS})
  fi
}

dg_read_pid() {
  if [[ -f "$PID_FILE" ]]; then
    tr -d '[:space:]' < "$PID_FILE"
  fi
}

dg_is_running() {
  local pid="$1"
  [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null
}

dg_service_status() {
  local pid
  pid="$(dg_read_pid)"
  if dg_is_running "$pid"; then
    dg_log_info "${DG_SERVICE_NAME} is running (pid=${pid}, role=${DG_SERVICE_ROLE})"
    dg_log_info "Log file: ${LOG_FILE}"
    dg_log_info "Health URL: ${DG_HEALTH_URL}"
    return 0
  fi
  if [[ -n "$pid" ]]; then
    dg_log_warn "Stale PID file removed: ${PID_FILE} (pid=${pid})"
    rm -f "$PID_FILE"
  fi
  dg_log_info "${DG_SERVICE_NAME} is not running"
  return 1
}

dg_service_start() {
  local pid
  pid="$(dg_read_pid)"
  if dg_is_running "$pid"; then
    dg_log_info "${DG_SERVICE_NAME} already running (pid=${pid})"
    return 0
  fi
  [[ -n "$pid" ]] && rm -f "$PID_FILE"

  dg_ensure_dir "$LOG_DIR"
  dg_ensure_dir "$JVMDUMP_DIR"
  dg_resolve_java
  dg_find_app_jar
  dg_build_jvm_opts
  dg_build_spring_args

  local -a cmd
  if [[ "${DG_SERVICE_ROLE}" == "worker" ]]; then
    dg_log_info "Launch mode: classpath main ${DG_MAIN_CLASS}"
    cmd=("$JAVA_CMD" "${JVM_OPTS[@]}" -cp "${CONF_DIR}:${LIB_DIR}/*:${APP_JAR}" "$DG_MAIN_CLASS" "${SPRING_ARGS[@]}")
  else
    dg_log_info "Launch mode: java -jar"
    cmd=("$JAVA_CMD" "${JVM_OPTS[@]}" -jar "$APP_JAR" "${SPRING_ARGS[@]}")
  fi

  if [[ "${DG_DAEMON}" == "0" ]]; then
    dg_log_info "Starting ${DG_SERVICE_NAME} in foreground (role=${DG_SERVICE_ROLE}) ..."
    dg_log_info "Press Ctrl+C to stop."
    exec "${cmd[@]}"
  fi

  dg_log_info "Starting ${DG_SERVICE_NAME} in background (role=${DG_SERVICE_ROLE}) ..."
  dg_log_info "Stdout/stderr -> ${LOG_FILE}"
  nohup "${cmd[@]}" >> "$LOG_FILE" 2>&1 &
  echo $! > "$PID_FILE"
  dg_log_info "Started with pid=$(cat "$PID_FILE")"

  local waited=0
  while [[ "$waited" -lt "${DG_START_WAIT_SEC}" ]]; do
    sleep 1
    waited=$((waited + 1))
    if dg_health_check_quiet; then
      dg_log_info "Health check passed: ${DG_HEALTH_URL}"
      return 0
    fi
  done
  dg_log_warn "Process started but health check not yet OK (${DG_HEALTH_URL})"
  dg_log_warn "Tail logs: tail -f ${LOG_FILE}"
}

dg_service_stop() {
  local pid
  pid="$(dg_read_pid)"
  if ! dg_is_running "$pid"; then
    dg_log_info "${DG_SERVICE_NAME} is not running"
    rm -f "$PID_FILE"
    return 0
  fi

  dg_log_info "Stopping ${DG_SERVICE_NAME} (pid=${pid}) ..."
  kill -TERM "$pid" 2>/dev/null || true
  local waited=0
  while dg_is_running "$pid" && [[ "$waited" -lt "${DG_STOP_TIMEOUT_SEC}" ]]; do
    sleep 1
    waited=$((waited + 1))
  done
  if dg_is_running "$pid"; then
    dg_log_warn "Graceful stop timed out after ${DG_STOP_TIMEOUT_SEC}s; sending SIGKILL"
    kill -KILL "$pid" 2>/dev/null || true
  fi
  rm -f "$PID_FILE"
  dg_log_info "${DG_SERVICE_NAME} stopped"
}

dg_service_restart() {
  dg_service_stop
  dg_service_start
}

dg_health_check_quiet() {
  local code
  code="$(curl -s -o /dev/null -w '%{http_code}' --connect-timeout 3 --max-time 8 "${DG_HEALTH_URL}" 2>/dev/null || echo "000")"
  [[ "$code" == "200" ]]
}

dg_health_check() {
  dg_load_config
  dg_log_info "Checking ${DG_HEALTH_URL} ..."
  local tmp body code
  tmp="$(mktemp)"
  code="$(curl -s -o "$tmp" -w '%{http_code}' --connect-timeout 5 --max-time 10 "${DG_HEALTH_URL}" 2>/dev/null || echo "000")"
  if [[ "$code" != "200" ]]; then
    dg_log_error "Health check failed: HTTP ${code} (${DG_HEALTH_URL})"
    rm -f "$tmp"
    return 1
  fi
  body="$(cat "$tmp")"
  rm -f "$tmp"
  if ! grep -Eq '"opcode"[[:space:]]*:[[:space:]]*0' <<< "$body"; then
    dg_log_error "Health check failed: response missing opcode=0"
    dg_log_error "Body: ${body}"
    return 1
  fi
  dg_log_info "Health check OK: ${body}"
  return 0
}

dg_print_usage() {
  cat <<EOF

Data Generator runtime script
  Package root : ${ROOT_DIR}
  Service      : ${DG_SERVICE_NAME} (${DG_SERVICE_ROLE})
  Config file  : ${ROOT_DIR}/conf/service.env (optional, see service.env.example)

Commands:
  start [0|1]   Start service (1=background default, 0=foreground)
  stop          Stop service gracefully
  restart       Restart service
  status        Show running state
  health        Run HTTP health check
  help          Show this help

Environment overrides (also set in conf/service.env):
  DG_JAVA_HOME, DG_SERVER_PORT, DG_SPRING_PROFILES_ACTIVE, DG_DAEMON, DG_LOG_DIR

EOF
}

dg_parse_start_daemon_arg() {
  if [[ "${1:-}" == "0" || "${1:-}" == "1" ]]; then
    DG_DAEMON="$1"
  fi
  if [[ "${DG_FOREGROUND:-0}" == "1" ]]; then
    DG_DAEMON=0
  fi
}

dg_main() {
  local action="${1:-help}"
  dg_init_paths "$DG_BIN_DIR"
  dg_load_config
  dg_parse_start_daemon_arg "${2:-}"

  case "$action" in
    start) dg_service_start ;;
    stop) dg_service_stop ;;
    restart) dg_service_restart ;;
    status) dg_service_status ;;
    health) dg_health_check ;;
    help|-h|--help) dg_print_usage ;;
    *)
      dg_log_error "Unknown command: ${action}"
      dg_print_usage
      return 1
      ;;
  esac
}
