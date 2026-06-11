#!/usr/bin/env bash
# Install a systemd unit for the packaged Data Generator service (Linux, root required).

set -euo pipefail

if [[ "$(id -u)" -ne 0 ]]; then
  echo "[ERROR] Run as root: sudo $0" >&2
  exit 1
fi

BIN_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "${BIN_DIR}/.." && pwd)"
UNIT_NAME="${DG_SERVICE_NAME:-data-generator-service}"
UNIT_PATH="/etc/systemd/system/${UNIT_NAME}.service"
RUN_USER="${DG_SERVICE_USER:-${SVC_USER:-root}}"
ENV_FILE="${ROOT_DIR}/conf/service.env"

if [[ -f "$UNIT_PATH" ]]; then
  echo "[ERROR] Unit already exists: ${UNIT_PATH}" >&2
  echo "        Remove it first or use a different DG_SERVICE_NAME." >&2
  exit 1
fi

cat > "$UNIT_PATH" <<EOF
[Unit]
Description=Data Generator (${UNIT_NAME})
Documentation=file://${ROOT_DIR}/conf/service.env.example
After=network-online.target
Wants=network-online.target

[Service]
Type=forking
WorkingDirectory=${BIN_DIR}
Environment=DG_DAEMON=1
Environment=DG_JAVA_PROMPT=0
EnvironmentFile=-${ENV_FILE}
PIDFile=${ROOT_DIR}/${UNIT_NAME}.pid
ExecStart=/bin/bash ${BIN_DIR}/run.sh start 1
ExecStop=/bin/bash ${BIN_DIR}/run.sh stop
ExecReload=/bin/bash ${BIN_DIR}/run.sh restart
TimeoutStartSec=180
TimeoutStopSec=60
Restart=on-failure
RestartSec=10
User=${RUN_USER}

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable "${UNIT_NAME}.service"
echo "[INFO] Created ${UNIT_PATH}"
echo "[INFO] Edit ${ENV_FILE} (copy from service.env.example) then:"
echo "       systemctl start ${UNIT_NAME}"
echo "       systemctl status ${UNIT_NAME}"
echo "       journalctl -u ${UNIT_NAME} -f"
