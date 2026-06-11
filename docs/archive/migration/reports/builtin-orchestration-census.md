# Built-in template migration census

Generated: 2026-05-29 (`BuiltinTemplateMigrationCensus`)

> Staging-free evidence for W3 orchestration blocking. Production `db-{id}` census is **M2**.

## Summary

- **Total templates:** 59
- **COMPATIBILITY_ONLY:** 2 (3%)

### By scenario family

- **multi_source:** 11
- **orchestration_legacy:** 2
- **synthetic:** 46

### By recommended path

- **compatibility_only:** 2
- **spel:** 43
- **sql:** 14

### By suggested class

- **ADAPTED:** 48
- **APPROXIMATE:** 9
- **COMPATIBILITY_ONLY:** 2

### Blocker signals (non-exclusive)

- **LOG:** 1
- **PAUSE:** 1

## Detail

| Path | Family | Class | Path | Blockers |
|------|--------|-------|------|----------|
| `demo/00_常规样例.yaml` | multi_source | ADAPTED | spel | — |
| `demo/01_带权重读取器样例.yaml` | synthetic | ADAPTED | sql | — |
| `demo/02_公平选择读取器样例.yaml` | synthetic | ADAPTED | sql | — |
| `demo/03_按顺序且只选取一次数据样例.yaml` | synthetic | APPROXIMATE | sql | — |
| `demo/04_按顺序且可重复选取数据样例.yaml` | synthetic | ADAPTED | sql | — |
| `demo/05_无序且只选取一次数据样例.yaml` | synthetic | APPROXIMATE | sql | — |
| `demo/06_最少N次最多M次选取数据样例.yaml` | synthetic | APPROXIMATE | sql | — |
| `demo/07_字段依赖样例.yaml` | multi_source | ADAPTED | spel | — |
| `demo/08_多个字段依赖样例.yaml` | multi_source | ADAPTED | spel | — |
| `demo/09_SQL参数样例.yaml` | multi_source | ADAPTED | spel | — |
| `demo/10_多个写入器样例.yaml` | multi_source | ADAPTED | spel | — |
| `demo/11_复杂阶段处理样例.yaml` | multi_source | ADAPTED | spel | — |
| `demo/12_内存驻留和非内存驻留样例.yaml` | synthetic | APPROXIMATE | sql | — |
| `demo/13_全局缓存数据获取样例.yaml` | synthetic | ADAPTED | sql | — |
| `demo/14_结果映射样例.yaml` | synthetic | ADAPTED | sql | — |
| `demo/15_条件判断样例.yaml` | synthetic | ADAPTED | sql | — |
| `demo/16_AI生成样例.yaml` | synthetic | ADAPTED | sql | — |
| `demo/17_时间迭代器样例.yaml` | synthetic | ADAPTED | spel | — |
| `demo/18_数据库查询迭代器样例.yaml` | orchestration_legacy | COMPATIBILITY_ONLY | compatibility_only | Template uses LOG orchestration stage; logging side effects are not migrated to V2 SQL. |
| `demo/19_Excel文件读取器样例.yaml` | synthetic | ADAPTED | sql | — |
| `demo/20_Json文件读取器样例.yaml` | synthetic | ADAPTED | sql | — |
| `demo/21_Csv文件读取器样例.yaml` | synthetic | ADAPTED | sql | — |
| `demo/22_Csv文件迭代器样例.yaml` | synthetic | ADAPTED | spel | — |
| `demo/23_Excel文件迭代器样例.yaml` | synthetic | ADAPTED | spel | — |
| `demo/24_Json文件迭代器样例.yaml` | synthetic | ADAPTED | spel | — |
| `demo/25_Csv文件写入器样例.yaml` | synthetic | ADAPTED | spel | — |
| `demo/26_Excel文件写入器样例.yaml` | synthetic | ADAPTED | spel | — |
| `demo/27_暂停阶段样例.yaml` | orchestration_legacy | COMPATIBILITY_ONLY | compatibility_only | Template uses PAUSE orchestration stage; V2 SQL migration cannot preserve pause semantics. |
| `demo/28_常量迭代器重复多次样例.yaml` | synthetic | ADAPTED | spel | — |
| `demo/98_常量JSON样例.yaml` | synthetic | ADAPTED | spel | — |
| `demo/99_综合样例.yaml` | multi_source | APPROXIMATE | spel | — |
| `idps/inet-cloud-control/01_VEHICLE_ALARM.yaml` | synthetic | ADAPTED | spel | — |
| `idps/inet-cloud-control/02_TRAFFIC_LIGHT_ALARM.yaml` | synthetic | ADAPTED | spel | — |
| `idps/inet-cloud-control/03_VEHICLE_OPERATION.yaml` | synthetic | ADAPTED | spel | — |
| `idps/inet-cloud-control/04_VEHICLE_ONLINE_FLOW.yaml` | synthetic | ADAPTED | spel | — |
| `idps/traffic-command/01_Q_USPP_WIT_DEVICE_LOCATION.yaml` | synthetic | APPROXIMATE | spel | — |
| `idps/traffic-command/02_GA_LOCATION_01.yaml` | synthetic | ADAPTED | spel | — |
| `idps/traffic-command/03_GA_LOCATION_02.yaml` | synthetic | ADAPTED | spel | — |
| `idps/traffic-ledger/01_OBJECT_GPS.yaml` | synthetic | ADAPTED | spel | — |
| `idps/traffic-ledger/02_TRAFFIC_EVENT.yaml` | synthetic | ADAPTED | spel | — |
| `idps/traffic-ledger/03_OBJECT_GPS_POINT.yaml` | synthetic | ADAPTED | spel | — |
| `idps/traffic-ledger/04_OBJECT_GPS_TRACK.yaml` | synthetic | ADAPTED | spel | — |
| `idps/traffic-vde-infohub/01_BASEDATA_ENTERPRISE.yaml` | synthetic | ADAPTED | spel | — |
| `idps/traffic-vde-infohub/02_BASEDATA_COMMERIAL_VEHICLE.yaml` | synthetic | ADAPTED | spel | — |
| `tocc/bus/01_bus_passenger_flow_hour_stat.yaml` | synthetic | ADAPTED | spel | — |
| `tocc/parking/01_car_detect_info.yaml` | synthetic | ADAPTED | spel | — |
| `tocc/parking/02_parking_user_vehicle.yaml` | synthetic | ADAPTED | spel | — |
| `tocc/parking/03_vehicle_break_rule_record.yaml` | multi_source | ADAPTED | spel | — |
| `tocc/parking/04_parking_space_reserve.yaml` | multi_source | ADAPTED | spel | — |
| `tocc/parking/05_parking_black_list.yaml` | synthetic | ADAPTED | spel | — |
| `tocc/parking/06_vehicle_gps_record.yaml` | synthetic | ADAPTED | spel | — |
| `tocc/parking/07_tourist_num_day_hour_stat.yaml` | synthetic | APPROXIMATE | spel | — |
| `tocc/parking/07_tourist_play_duration_day_stat.yaml` | synthetic | APPROXIMATE | spel | — |
| `tocc/parking/08_car_detect_info_kafka.yaml` | multi_source | ADAPTED | spel | — |
| `tocc/parking/09_parking_passing_record.yaml` | synthetic | ADAPTED | spel | — |
| `tocc/parking/10_archive_vehicle_info.yaml` | synthetic | ADAPTED | spel | — |
| `tocc/parking/11_parking_online_space_record.yaml` | synthetic | ADAPTED | spel | — |
| `utcs/site/01_UTCS_BAS_SITE_INFO.yaml` | multi_source | APPROXIMATE | spel | — |
| `utcs/site/02_BAS_SITE_FLOW.yaml` | synthetic | ADAPTED | spel | — |
