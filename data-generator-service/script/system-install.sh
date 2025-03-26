#!/bin/bash

#----------------------------------------------------------
# arg
#----------------------------------------------------------
base_dir=`cd $(dirname $0); pwd`

SERVICE_NAME="data-generator-service"
#----------------------------------------------------------

if [ -r "/etc/systemd/system/$SERVICE_NAME.service" ]; then
  echo "error: /etc/systemd/system/$SERVICE_NAME.service already exists"
else
cat > /etc/systemd/system/$SERVICE_NAME.service <<EOF
[Unit]
Description=$SERVICE_NAME
After=network-online.target
Wants=network-online.target

[Service]
Type=forking
WorkingDirectory=$base_dir
ExecStart=/bin/bash -c "source /etc/profile && /bin/sh $base_dir/run.sh start"
ExecStop=/bin/bash -c "source /etc/profile  && /bin/sh $base_dir/run.sh stop"
ExecStartPost=/usr/bin/sleep 5
Restart=always
RestartSec=10s

[Install]
WantedBy=multi-user.target
EOF

  echo "created /etc/systemd/system/$SERVICE_NAME.service"
  systemctl daemon-reload;
  # 修改为 restart 是无效的
  # 执行本脚本前先执行 ./shutdown.sh
  systemctl start $SERVICE_NAME.service;
  systemctl enable $SERVICE_NAME.service;
  echo "开机自启动功能:"
  systemctl is-enabled $SERVICE_NAME.service;
  echo "------------------------------------------------------------------------------------------------------------"
  echo "systemctl status $SERVICE_NAME.service"
  systemctl status $SERVICE_NAME.service;
fi
