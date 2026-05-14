#!/bin/bash
source /etc/profile
# Ensure the script is executed from the correct directory
cd "$(dirname "$0")" || exit 1

# Define directories based on project root and bin directory
ROOT_DIR="$(pwd)/.."  # Project root directory
BIN_DIR="${ROOT_DIR}/bin"  # bin directory
LOG_DIR="${ROOT_DIR}/logs"  # logs directory
CONF_DIR="${ROOT_DIR}/conf"  # conf directory
LIB_DIR="${ROOT_DIR}/lib"  # lib directory
JVMDUMP_DIR="${ROOT_DIR}/jvmdump"  # jvmdump directory
CLASSPATH=".:${CONF_DIR}:${LIB_DIR}/*:${MODEL_JAR}"

# Ensure directory exists function
ensure_directory() {
    local dir="$1"
    if [ ! -d "$dir" ]; then
        mkdir -p "$dir" || { echo "Failed to create directory: $dir"; exit 1; }
    fi
}

# Ensure log and jvmdump directories exist
ensure_directory "$LOG_DIR"
ensure_directory "$JVMDUMP_DIR"

# Define default variables
DEFAULT_MODEL_NAME="data-generator-service"
DEFAULT_MODEL_DAEMON=1
DEFAULT_MODEL_LOG="${LOG_DIR}/${DEFAULT_MODEL_NAME}.log"  # Log file in the logs directory
DEFAULT_SPRING_PROFILES_ACTIVE="dev"
DEFAULT_SLEEP_MIN=5  # 5 seconds

# Set variables from arguments or defaults
MODEL_NAME="${MODEL_NAME:-$DEFAULT_MODEL_NAME}"
MODEL_DAEMON="${MODEL_DAEMON:-$DEFAULT_MODEL_DAEMON}"
MODEL_LOG="${MODEL_LOG:-$DEFAULT_MODEL_LOG}"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-$DEFAULT_SPRING_PROFILES_ACTIVE}"
SLEEP_MIN="${SLEEP_MIN:-$DEFAULT_SLEEP_MIN}"

# Correct the JAR file path to be in the bin directory
MODEL_JAR=$(find "${BIN_DIR}" -type f -name "${MODEL_NAME}-*.jar" | sort -V | tail -n 1)
MODEL_VARS="--spring.config.location=../conf/ --spring.profiles.active=${SPRING_PROFILES_ACTIVE} --logging.config=../conf/logback-spring.xml"

# PID file is placed in the root directory
PID_FILE="${ROOT_DIR}/${MODEL_NAME}.pid"

#run as non-daemon
for i in "$@"; do
    if [ $i = "--non-daemon" ]
     then
    	 MODEL_DAEMON=0
    fi
done

# JVM parameters
if [ "$ENV_NONDAEMON" != "true" ]; then
    JVM_VARS="-server -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=256M -Xms512M -Xmx1G -XX:+UseG1GC -Dskywalking.agent.service_name=${MODEL_NAME} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${JVMDUMP_DIR}/${MODEL_NAME}.hprof"
else
    JVM_VARS="-server -XX:MaxRAMPercentage=95.0 -XX:+UseG1GC -Dskywalking.agent.service_name=${MODEL_NAME} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${JVMDUMP_DIR}/${MODEL_NAME}.hprof"
fi

# Check if JAVA_VERSION is valid
check_java_version() {
    JAVA_VERSION=$("$1" -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F. '{print ($1 == "1") ? $2 : $1}')
    if [ "$JAVA_VERSION" -lt 25 ]; then
        echo "Java version is lower than 25. Please set JAVA_HOME to point to JDK 25."
        return 1
    fi
    return 0
}

# Validate if the JDK path is correct and contains 'bin/java'
validate_java_home() {
    if [ -d "$1" ] && [ -f "$1/bin/java" ]; then
        echo "Valid JDK found at $1."
        return 0
    else
        echo "Invalid JDK path: $1. Make sure it contains 'bin/java'."
        return 1
    fi
}

# Initialize JAVA_CMD
JAVA_CMD=""

# If DG_JAVA_HOME/JAVA_HOME is set, validate and use it
if [ -n "$DG_JAVA_HOME" ]; then
    JAVA_CMD="${DG_JAVA_HOME}/bin/java"
    if check_java_version "$JAVA_CMD"; then
        echo "Using Java from DG_JAVA_HOME: $JAVA_CMD"
    fi
else
  if [ -n "$JAVA_HOME" ]; then
    JAVA_CMD="${JAVA_HOME}/bin/java"
    if check_java_version "$JAVA_CMD"; then
        echo "Using Java from JAVA_HOME: $JAVA_CMD"
        DG_JAVA_HOME="$JAVA_HOME"
    else
        echo "Java version from DG_JAVA_HOME/JAVA_HOME does not meet the requirement. Please specify another JDK path."
        DG_JAVA_HOME=""
    fi
  fi
fi

# If JAVA_HOME is not set, prompt the user to enter it
while [ -z "$DG_JAVA_HOME" ]; do
    echo "Please enter the path to JDK 25:"
    read -r DG_JAVA_HOME

    if validate_java_home "$DG_JAVA_HOME"; then
        JAVA_CMD="${DG_JAVA_HOME}/bin/java"
        echo "Using Java from: $JAVA_CMD"
    else
        echo "Invalid Java path or version. Please enter a valid JDK 25 path."
    fi
done

# Function definitions
print_usage() {
    echo ""
    echo "Usage:"
    echo "  h|H|help|HELP             - Display help information."
    echo "  start [daemon]            - Start the ${MODEL_NAME} service. Optionally, [daemon] can be 0 for foreground or 1 for background."
    echo "  stop                      - Stop the ${MODEL_NAME} service."
    echo "  restart                   - Restart the ${MODEL_NAME} service."
    echo "  status                    - Check the status of the ${MODEL_NAME} service."
}

# Start the service
model_start() {
    if [ -f "${PID_FILE}" ]; then
        local pid
        pid=$(cat "${PID_FILE}")
        # Check if the process is running
        if ps -p "${pid}" > /dev/null 2>&1; then
            echo "${MODEL_NAME} is already running."
            return 0
        else
            # If the process is not running, delete the PID file
            echo "PID file exists, but the process is not running. Removing PID file."
            rm -f "${PID_FILE}"
        fi
    fi

    if [ "$MODEL_DAEMON" -eq 0 ]; then
        echo "Starting ${MODEL_NAME} ... in foreground"
        ${JAVA_CMD} -cp "${CLASSPATH}" ${JVM_VARS} -jar ${MODEL_JAR} ${MODEL_VARS}
    else
        echo "Starting ${MODEL_NAME} ... in background"
        nohup ${JAVA_CMD} -cp "${CLASSPATH}" ${JVM_VARS} -jar ${MODEL_JAR} ${MODEL_VARS} >> "${MODEL_LOG}" 2>&1 &
        echo $! > "${PID_FILE}"  # Save the background process PID
        sleep ${SLEEP_MIN}
    fi
}

# Stop the service
model_stop() {
    echo "Stopping ${MODEL_NAME} ..."
    if [ -f "${PID_FILE}" ]; then
        local pid
        pid=$(cat "${PID_FILE}")
        kill "${pid}"
        rm -f "${PID_FILE}"
        echo "${MODEL_NAME} stopped."
    else
        echo "PID file not found, ${MODEL_NAME} might not be running."
    fi
}

# Restart the service
model_restart() {
    model_stop
    model_start
}

# Service status
model_status() {
    if [ -f "${PID_FILE}" ]; then
        local pid
        pid=$(cat "${PID_FILE}")
        if ps -p "${pid}" > /dev/null 2>&1; then
            echo "${MODEL_NAME} is running."
        else
            echo "${MODEL_NAME} is not running."
        fi
    else
        echo "${MODEL_NAME} is not running."
    fi
}

# Parse command line arguments
parse_para() {
    case "$1" in
        start)
          if [ -n "$2" ]; then
            MODEL_DAEMON="$2"
          fi
          model_start ;;
        stop) model_stop ;;
        restart) model_restart ;;
        status) model_status ;;
        *) echo "Invalid argument: $1"; print_usage ;;
    esac
}

# Main function
if [ $# -eq 0 ]; then
    print_usage
    exit 1
fi

parse_para "$1" "$2"
