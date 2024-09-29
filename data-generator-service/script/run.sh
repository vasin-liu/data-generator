#!/bin/bash
#根目录
baseDir=$(cd $(dirname $0)/..; pwd)
#执行脚本目录
cd ${baseDir}/bin
echo "===> baseDir=$baseDir"

CLASSPATH=$baseDir
CLASSPATH=$CLASSPATH:$baseDir/conf/

CONF_PATH=-Xbootclasspath/p:$baseDir/conf/

#-------------------------------------------------------------------
# 定义变量
#-------------------------------------------------------------------

# 模块名
MODEL_NAME="data-generator"
#pid 文件路径
pidfile=/var/run/${MODEL_NAME}.pid

# 选项
MODEL_OPTS=""

# 运行包名
MODEL_JAR=`pwd`'/'`echo ${MODEL_NAME}*.jar`

# 运行参数
MODEL_VARS="--spring.config.location=../conf/application.yaml --logging.config=../conf/logback-spring.xml"

# JVM参数
JVM_VARS="-server -Xms256m -Xmx1g -XX:+UseG1GC  -Djava.awt.headless=true -XX:MaxMetaspaceSize=256m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/suntek/service/jvmdump/${MODEL_NAME}"

# 前台/后台: 0-前台， 1-后台
MODEL_DAEMON=0

# 日志 '&-':表示关闭标准输出日志
MODEL_LOG="./error.log"
for i in "$@"; do
    if [ $i = "--non-daemon" ]
     then
    	 MODEL_DAEMON=0
    fi
done


#-------------------------------------------------------------------
# 以下内容请不要修改
#-------------------------------------------------------------------

SLEEP_MIN=5

# model info can be define here
MODEL_SYMBOL=${MODEL_NAME}
GREP_KEY="Diname="${MODEL_SYMBOL}


#----------------------------------------------------------
# function print usage
#----------------------------------------------------------

print_usage()
{
    echo ""
    echo "h|H|help|HELP             ---Print help information."
    echo "start                     ---Start the ${MODEL_NAME} server."
    echo "stop                      ---Stop the ${MODEL_NAME} server."
    echo "restart                   ---Restart the ${MODEL_NAME} server."
    echo "status                    ---Status the ${MODEL_NAME} server."
}

#-------------------------------------------------------------------
# function model_is_exist (兼容alpine)
#-------------------------------------------------------------------

modelService_is_exist()
{
    if [ -e $pidfile ]; then
        localServerId=$(cat $pidfile)
        echo "file $pidfile exists. The content of the file is $localServerId"
        #存在文件，检测是否进程还存在
        if kill -0 "$localServerId" 2>/dev/null; then
            #检查进程是否存在，
            echo "the $localServerId process exists"
            return 0
        else
            echo "the $localServerId process does not exist"
            return 1
        fi
    else
        localServerId=0
        #文件不存在
        echo "file $pidfile does not exist"
        return 1
    fi
}

# modelService_is_exist()
# {
# localServerId=`ps -ef |grep -w "${GREP_KEY}" | grep -v grep | awk '{print $2}'`
# if [ -z "${localServerId}" ]
# then
# return 1
# else
# expr ${localServerId} + 0 &>/dev/null
# if [ $? -ne 0 ]
# then
# localServerId=`ps -ef |grep -w "${GREP_KEY}" | grep -v grep | awk '{print $1}'`
# fi
# echo "pid is ${localServerId}"
# return 0
# fi
# }

#-------------------------------------------------------------------
# function check_user_id
# return 0 ---- supper user
# return 1 ---- normal user
#-------------------------------------------------------------------

# check_user_id ()
# {
# localMyId=$(id|awk '{print $1}')
# localMyId=${localMyId:4:1}
# if [ "${localMyId}" -eq "0" ]
# then
# return 0
# else
# return 1
# fi
# }

#-------------------------------------------------------------------
# function model_start
#-------------------------------------------------------------------

model_start ()
{
    modelService_is_exist

    if [ $? -eq "0" ]; then
        echo "${MODEL_NAME} is running yet. pid ${localServerId}."
        return 0
    else
        if [ $MODEL_DAEMON = 0 ]; then
            echo "try to start ${MODEL_NAME} ... foreground"
            java -${GREP_KEY} ${MODEL_OPTS} -jar ${JVM_VARS} ${MODEL_JAR} ${MODEL_VARS} ${CONF_PATH}
        else
            echo "try to start ${MODEL_NAME} ... backgroud"
            nohup java -${GREP_KEY} ${MODEL_OPTS} -jar ${JVM_VARS} ${MODEL_JAR} ${MODEL_VARS} ${CONF_PATH} 1>&- 2>>${MODEL_LOG} & echo $! > $pidfile
            sleep $SLEEP_MIN
            modelService_is_exist
            if [ $? -eq "0" ]; then
                echo "${MODEL_NAME} is running. pid ${localServerId}."
                touch /var/lock/subsys/$MODEL_NAME
                return 0
            else
                echo "failed to start ${MODEL_NAME}! see the output log for more details."
                rm -f $pidfile
                rm -f /var/lock/subsys/$MODEL_NAME
                return 1
            fi
        fi
    fi
}

#-------------------------------------------------------------------
# function model_stop
#-------------------------------------------------------------------

model_stop()
{
    echo "try to stop ${MODEL_NAME} ..."
    modelService_is_exist

    if [ $? -eq 0 ]; then
        kill -9 ${localServerId}
        if [ $? -ne 0 ]; then
            echo "failed to stop ${MODEL_NAME}!"
            return 1
        else
            echo "${MODEL_NAME} stopped."
            rm -f $pidfile
            rm -f /var/lock/subsys/$MODEL_NAME
            return 0
        fi
    else
        echo "${MODEL_NAME} is not running!"
        return 1
    fi
}

model_restart()
{
    model_stop

    model_start
}

#-------------------------------------------------------------------
# function model_status
#-------------------------------------------------------------------

model_status()
{
    modelService_is_exist
    if [ $? -eq 0 ]; then
        echo "${MODEL_NAME} is running. pid ${localServerId}."
    else
        echo "${MODEL_NAME} is not running."
    fi
}

#-------------------------------------------------------------------
#
#-------------------------------------------------------------------

#-------------------------------------------------------------------
# function parse_para
#-------------------------------------------------------------------

parse_para()
{
    case "$1" in
        start) model_start;;
        stop) model_stop;;
        restart) model_restart;;
        status) model_status;;
        *) echo "illage parameter : $1";print_usage;;
    esac
}

#-------------------------------------------------------------------
# main
#-------------------------------------------------------------------

parse_para $1

