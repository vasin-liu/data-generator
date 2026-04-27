#!/bin/bash
baseDir=$(cd `dirname $0` && pwd)
cd $baseDir

# 健康检查脚本会在服务部署的目标机器上执行，因此ip地址可以直接指定为localhost
url="http://localhost:9876/healthz"
rc=`curl -I -m 5 -o /dev/null -s -w %{http_code} ${url}`
result=`curl -s  $url`
great=`echo $result | sed 's/,/\n/g' | sed 's/"//g' | grep -i opcode | awk -F':' '{print $2}'`
function healthCheck() {
    # 状态码自定义
    if [[ "$rc"x != "200"x ]];then
        echo "rc:$rc"
        exit 1
    fi
}

function healthz(){
    if [[ "$great" -eq 0 ]];then
        echo "应用启动成功"
    else
        echo "应用启动失败"
    fi
}

function sshCheck() {
    host=localhost
    port=9500
    timeout 3 telnet $host $port|grep -E -w "Connected to"
}

#-------------------------------------------------------------------
# main
#-------------------------------------------------------------------
healthCheck $?  && healthz $?
