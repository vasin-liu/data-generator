#!/bin/sh
# 
MAX_FAIL=3                 #连续失败N次后触发模块重启
CRON_TIME="*/5 * * * *"    #定时周期，每5分钟
SERVICE_NAME=""            #默认为空，取keepalive.sh所在目录名

####################################################################################
# 模块的自检、自身保活：(该keepalive.sh为模板，请结合模块实况完善doCheck部分，并确保位于模块包的"./"目录下)
# 1）执行[cronAdd]：定时检测 (物理机部署可基于crond定时任务， 容器可直接利用healthcheck机制)
# 2）周期[CRON_TIME]：检测周期，如默认1分钟？ (该项为可配参数)
# 3）检测[doCheck]：检测细节，如检查所依赖的mysql是否可连，模块api/healthz状态等。
# （该项需又模块负责人编写细节，类似healthcheck.sh机制，无异常则返回0。需注意避免检测卡住、导致机器负载高或耗时超过检测周期导致检测任务堆积）
# 4）失败次数[MAX_FAIL]：连续失败N次后触发模块自身的重启。（该项需可配，避免偶发/抖动导致模块频繁重启）
# 5）脚本用法：./keepalive.sh cronAdd/cronDrop #修改doCheck, 添加/移除定时任务
            #  ./keepalive.sh check #手动执行检测 (模拟定时任务的调用)
			#  ./keepalive.sh #提示：please call with one param[check/cronAdd/cronDrop]
# 6）脚本日志：检测失败次数记在当前脚本目录下记录.failcount文件(超3000行时，只留500行)
####################################################################################
function doCheck(){
cd $(dirname $0)
sh healthz-check.sh
if [ $? -ne 0 ]; then
  return 1
fi
return 0
}

# sysd: 需要root;
function cronAdd(){
  cur=$(cd "$(dirname "$0")"; pwd)
  sudo systemctl start crond
  sudo systemctl enable crond
  script=$(basename $0)
  drop=$1
  conf=/tmp/cron-conf-$rq.txt
  echo 'MAILTO=""' > $conf 
  sudo crontab -l |grep -v "$cur/$script" |grep -v "^MAILTO" |grep -v "^#" |grep -v "^$" >> $conf
  test -z "$drop" && echo "$CRON_TIME sh $cur/$script check" >> $conf 
  sudo crontab $conf
  rm -f $conf
  sudo crontab -l #view
}

function maxFailAlive(){

  local errCode=$1
  # 初始0
  failfile=".failcount" #在当前脚本目录下记录.failcount文件(超3000行时，只留500行)
  test -s "$failfile" && fail=$(cat $failfile |tail -1 |cut -d':' -f2) || fail=0

  # 失败+1; 达MAX_FAIL/suc则置0
  RESTART=""
  test "0" == "$errCode" && fail=0 || fail=$(($fail+1))
  if [ "$fail" -gt "$(($MAX_FAIL-1))" ]; then
    fail=0
    RESTART="[RESTART]" #记录到failfile
    sh run.sh stop
    sh run.sh start
  fi

	# 记录值
  echo "[$rq]${RESTART}fail: $fail" |tee -a $failfile
	cnt=$(cat $failfile |wc -l) #超过3000行，则只留500行
	if [ "$cnt" -gt "3000" ]; then
		tail -500 $failfile > /tmp/.failcount_$rq
		cat /tmp/.failcount_$rq > $failfile; rm -f /tmp/.failcount_$rq
	fi	
}

rq=`date +%Y%m%d_%H%M%S`
case "$1" in
  check)
	  doCheck
          maxFailAlive "$?"
    ;;
  cronAdd)
    cronAdd
    ;;
  cronDrop)
    cronAdd "drop"
    ;;    
  *)
    echo "please call with one param[check/cronAdd/cronDrop]"
    exit 1
    ;;  
esac
