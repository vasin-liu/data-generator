#!/bin/bash

cd "$(dirname "$0")" || exit 1

# Define default variables
DEFAULT_MODEL_NAME="data-generator-service"
DEFAULT_MODEL_DAEMON=1
#DEFAULT_MODEL_LOG="logs/${DEFAULT_MODEL_NAME}.log"  # Use a subdirectory for logs
DEFAULT_MODEL_LOG="/dev/null"
DEFAULT_SPRING_PROFILES_ACTIVE="dev"
DEFAULT_SLEEP_MIN=5 # 5 seconds

# Set variables from arguments or defaults
MODEL_NAME="${MODEL_NAME:-$DEFAULT_MODEL_NAME}"
MODEL_DAEMON="${MODEL_DAEMON:-$DEFAULT_MODEL_DAEMON}"
MODEL_LOG="${MODEL_LOG:-$DEFAULT_MODEL_LOG}"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-$DEFAULT_SPRING_PROFILES_ACTIVE}"
SLEEP_MIN="${SLEEP_MIN:-$DEFAULT_SLEEP_MIN}"

MODEL_JAR="$(pwd)/$(ls ${MODEL_NAME}*.jar)"
MODEL_VARS="--spring.config.location=../conf/ --spring.profiles.active=${SPRING_PROFILES_ACTIVE} --logging.config=../conf/logback-spring.xml"
PID_FILE=/var/run/${MODEL_NAME}.pid  # PID file in current directory

JAVA_CMD="${JAVA_HOME}/bin/java"
if [ -z "$JAVA_HOME" ]; then
    echo "JAVA_HOME is not set. Using 'java' from PATH."
    JAVA_CMD="java"
else
    JAVA_CMD="${JAVA_HOME}/bin/java"
fi

if [ "$ENV_NONDAEMON" != "true" ]; then
    JVM_VARS="-server -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=256M -Xms512M -Xmx1G -XX:+UseG1GC -Dskywalking.agent.service_name=${MODEL_NAME} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/suntek/service/jvmdump/${MODEL_NAME}.hprof"
else
    JVM_VARS="-server -XX:MaxRAMPercentage=95.0 -XX:+UseG1GC -Dskywalking.agent.service_name=${MODEL_NAME} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/suntek/service/jvmdump/${MODEL_NAME}.hprof"
fi

# Functions
print_usage() {
    echo ""
    echo "Usage:"
    echo "  h|H|help|HELP             - Print help information."
    echo "  start [daemon]            - Start the ${MODEL_NAME} server. Optional [daemon] to specify 0 for foreground, 1 for background."
    echo "  stop                      - Stop the ${MODEL_NAME} server."
    echo "  restart                   - Restart the ${MODEL_NAME} server."
    echo "  status                    - Check the status of the ${MODEL_NAME} server."
}

modelService_is_exist() {
    if [ -f "${PID_FILE}" ]; then
        local pid
        pid=$(cat "${PID_FILE}")
        if ps -p "${pid}" > /dev/null 2>&1; then
            echo "PID is ${pid}"
            return 0
        else
            echo "PID file exists but process is not running."
            return 1
        fi
    else
        return 1
    fi
}

model_start() {
    modelService_is_exist
    if [ $? -eq 0 ]; then
        echo "${MODEL_NAME} is already running."
        return 0
    else
        if [ "$MODEL_DAEMON" -eq 0 ]; then
            echo "Starting ${MODEL_NAME} ... foreground"
            ${JAVA_CMD} ${MODEL_OPTS} -jar ${JVM_VARS} ${MODEL_JAR} ${MODEL_VARS}
        else
            echo "Starting ${MODEL_NAME} ... background"
            mkdir -p "$(dirname "$MODEL_LOG")"  # Ensure log directory exists
            nohup ${JAVA_CMD} ${MODEL_OPTS} -jar ${JVM_VARS} ${MODEL_JAR} ${MODEL_VARS} >> "${MODEL_LOG}" 2>&1 &
            echo $! > "${PID_FILE}"  # Save the PID of the last background process
            sleep ${SLEEP_MIN}
            modelService_is_exist
            if [ $? -eq 0 ]; then
                echo "${MODEL_NAME} is running."
                return 0
            else
                echo "Failed to start ${MODEL_NAME}. Check the log at ${MODEL_LOG} for details."
                return 1
            fi
        fi
    fi
}

model_stop() {
    echo "Stopping ${MODEL_NAME} ..."
    modelService_is_exist
    if [ $? -eq 0 ]; then
        local pid
        pid=$(cat "${PID_FILE}")
        kill "${pid}"
        count=0
        while modelService_is_exist; do
            ((count++))
            echo "Stopping ${MODEL_NAME} ${count} ..."
            sleep 1
        done
        rm -f "${PID_FILE}"
        echo "${MODEL_NAME} stopped."
        return 0
    else
        echo "${MODEL_NAME} is not running!"
        return 1
    fi
}

model_restart() {
    model_stop
    model_start
}

model_status() {
    modelService_is_exist
    if [ $? -eq 0 ]; then
        echo "${MODEL_NAME} is running."
    else
        echo "${MODEL_NAME} is not running."
    fi
}

parse_para() {
    case "$1" in
        start)
            if [ -n "$2" ]; then
                MODEL_DAEMON="$2"
            fi
            model_start ;;
        stop) model_stop ;;
        status) model_status ;;
        restart) model_restart ;;
        *) echo "Invalid parameter: $1"; print_usage ;;
    esac
}

# Main
if [ $# -eq 0 ]; then
    print_usage
    exit 1
fi

parse_para "$1" "$2"
