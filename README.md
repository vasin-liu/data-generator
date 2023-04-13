# data-generator

数据生成服务是通过给定的模板批量生成模拟数据，并且写入目标数据源的工具。

1. 读取数据源支持MySQL、Postgresql、Clickhouse、Elasticsearch；
2. 支持常量、动态表达式生成数据；
3. 写入数据源支持MySQL、Postgresql、Clickhouse、Elasticsearch、Kafka；
4. 支持Rest接口启动任务、查看任务、查看模板、重新加载模板；
5. 支持前置、后置脚本对生成数据进行处理；

### 数据源配置

elasticsearch

```yaml
spring:
  elasticsearch:
    multiple:
      #默认数据源
      primary: es1
      clusters:
        #es1 即为属于编号，后续模板中均用此编号来识别
        es1:
          uris:
            - https://localhost:9200
          username: elastic
          password: PCI@suntek#123
        es2:
          uris:
            - 172.25.22.60:9401
            - 172.25.20.217:9401
            - 172.25.20.218:9401
          username: elastic
          password: Suntek123
```

kafka

```yaml
spring:
  kafka:
    multiple:
      #默认数据源
      primary: kafka1
      clusters:
        #kafka1 即为属于编号，后续模板中均用此编号来识别
        kafka1:
          bootstrap-servers:
            - localhost:9092
        kafka2:
          bootstrap-servers:
            - 172.25.21.29:9092
```

database

```yaml
spring:
  datasource:
    dynamic:
      #默认数据源
      primary: reader
      datasource:
        #内置内存数据库，用来保存任务信息
        data-generator:
          url: jdbc:h2:mem:data-generator
          #url: jdbc:h2:file:./db/data-generator
          username: sa
          password:
          driver-class-name: org.h2.Driver
          type: com.alibaba.druid.pool.DruidDataSource
          init:
            schema: classpath:db/schema.sql
          druid:
            filters: stat,slf4j
        #omof 即为属于编号，后续模板中均用此编号来识别
        omof:
          url: jdbc:mysql://172.25.20.175:3306/omof_gzsj?useUnicode=true&autoReconnect=true&characterEncoding=utf8&useSSL=false&allowMultiQueries=true&serverTimezone=GMT%2B8&allowLoadLocalInfile=true
          username: videoweb
          password: suntek
          driver-class-name: com.mysql.cj.jdbc.Driver
          type: com.alibaba.druid.pool.DruidDataSource
          druid:
            filters: stat,slf4j
        reader:
          url: jdbc:mysql://localhost:3306/reader?useUnicode=true&autoReconnect=true&characterEncoding=utf8&useSSL=false&allowMultiQueries=true&serverTimezone=GMT%2B8&allowLoadLocalInfile=true
          username: root
          password: PCI@suntek#123
          driver-class-name: com.mysql.cj.jdbc.Driver
          type: com.alibaba.druid.pool.DruidDataSource
          druid:
            filters: stat,slf4j
        writer:
          url: jdbc:mysql://localhost:3306/writer?useUnicode=true&autoReconnect=true&characterEncoding=utf8&useSSL=false&allowMultiQueries=true&serverTimezone=GMT%2B8&allowLoadLocalInfile=true
          username: root
          password: PCI@suntek#123
          driver-class-name: com.mysql.cj.jdbc.Driver
          type: com.alibaba.druid.pool.DruidDataSource
          druid:
            filters: stat,slf4j
        writer2:
          url: jdbc:postgresql://localhost:5432/pd_dts
          username: jiadu
          password: PCI@suntek#123
          driver-class-name: org.postgresql.Driver
          type: com.alibaba.druid.pool.DruidDataSource
          druid:
            filters: stat,slf4j
        writer3:
          url: jdbc:clickhouse://localhost:8123/default
          username: default
          password: PCI@suntek#123
          driver-class-name: com.clickhouse.jdbc.ClickHouseDriver
          type: com.alibaba.druid.pool.DruidDataSource
          druid:
            filters: stat,slf4j
        writer4:
          url: jdbc:postgresql://172.25.21.18:25308/pd_dts_gz
          username: pd_dts_gz
          password: suntek@123
          driver-class-name: org.postgresql.Driver
          type: com.alibaba.druid.pool.DruidDataSource
          druid:
            filters: stat,slf4j
        pd_dicmanage:
          url: jdbc:mysql://172.25.21.29:3306/pd_dicmanage?useUnicode=true&autoReconnect=true&characterEncoding=utf8&useSSL=false&allowMultiQueries=true&serverTimezone=GMT%2B8&allowLoadLocalInfile=true
          username: videoweb
          password: suntek
          driver-class-name: com.mysql.cj.jdbc.Driver
          type: com.alibaba.druid.pool.DruidDataSource
          druid:
            filters: stat,slf4j
        md_device_mgr:
          url: jdbc:mysql://172.25.21.29:3306/md_device_mgr?useUnicode=true&autoReconnect=true&characterEncoding=utf8&useSSL=false&allowMultiQueries=true&serverTimezone=GMT%2B8&allowLoadLocalInfile=true
          username: videoweb
          password: suntek
          driver-class-name: com.mysql.cj.jdbc.Driver
          type: com.alibaba.druid.pool.DruidDataSource
          druid:
            filters: stat,slf4j
      druid:
        filters: stat,slf4j
```

### 启动任务

1. 启动数据生成服务，确认启动成功；
2. 启动任务：`http://ip:port/task/run/{your-template-name}`，其中`{your-template-name}`
   替换为需要执行的模板名，执行成功后会返回`taskId`，该编号后续可以用于查询任务进度；
3. 查看任务进度：`http://localhost:8080/task/view/{your-task-id}` ，其中`{your-task-id}`替换为第2步中返回的任务编号；
4. 任务完成后检查写入的目标数据源，确认是否已经正确地写入数据；

### 模板规范

模板配置主要分以下几部分：

1. 模板公共部分；
2. 模板全局配置，含全局线程池配置、全局读取器配置；
3. 表数据集来源元数据配置，包含各个字段数据源读取、处理、输出配置，可以包含虚拟字段，字段依赖等；
4. 表数据集写入器配置；

**注意：模板校验文件路径为：src/resources/META-INF/template-schema.json**

```yaml
# 数据集名称，各个文件中唯一，启动任务时根据该名称进行调用模板
name: car_detect_info_hkmo_yisa_postgres
#生成的总数据量
amount: 100
#每个生成批次数量，即每次批量提交至写入数据源的数量
batchSize: 10
#全局配置
global:
  #线程池配置
  executor:
    #核心线程数
    coreSize: 8
    #最大线程数
    maxSize: 16
    #队列大小
    queueCapacity: 16

  #全局数据集配置，通常是需要后面的配置中共享的数据，即 table.fields 中各个字段共享的数据，如果仅仅是字段之间依赖的可以直接在
  #table.fields 中配置字段直接的依赖关系即可，不需要在全局共享数据中进行配置
  readers:
    #单个数据集读取配置
    - #数据集编号，可供后续在脚本中获取对应的数据集
      dataSetId: global_ds_1
      #数据源读取器类型，支持类型有：JDBC、ELASTICSEARCH、CONSTANT、SPEL、DIRECT_SPEL
      # JDBC，常规需要从数据库中读取的数据集，一般不建议搜索太大的数据集，因为这个数据集再数据生成过程中会驻留内存，数据太多会导致内存溢出
      # CONSTANT，常量数据，即可以在配置模板中固定数据范围的数据内容，以数组的方式进行配置，例如：[ 1,2,3,4,5,6,7 ]
      # ELASTICSEARCH，常规需要从数据库中读取的数据集，一般不建议搜索太大的数据集，因为这个数据集再数据生成过程中会驻留内存，数据太多会导致内存溢出
      # SPEL，通过内置的数据生成函数或者自定义的数据生成函数进行数据生成，该类型会将生成的数据缓存值内存，供后续数据生成时随机抽取生成结果值
      # DIRECT_SPEL，通过内置的数据生成函数或者自定义的数据生成函数进行数据生成，该类型不会生成数据缓存在内存中，而是后续数据生成时再执行表达式生成结果值（即延迟处理）
      type: JDBC
      #数据源编号，该编号唯一，且与 数据源配置 这个章节中配置的数据源名称（KEY）相同
      #注意：JDBC/ELASTICSEARCH/KAFKA这三种模式下该字段不能为空
      dataSourceId: 'reader'
      #数据集，允许字符串或者数组，当且仅当 type 字段的值为 CONSTANT 时才能为数组，其他情况均为字符串
      #JDBC对应的为查询SQL，ELASTICSEARCH对应的为查询DSL，SPEL和DIRECT_SPEL对应的为数据生成表达式，
      #可以采用内置的数据生成函数或者Faker模块
      dataSet: "SELECT CODE FROM SYS_DICT WHERE PARENT_ID = 92"
      #后置数据集处理脚本
      postScript:
        # 后置处理脚本类型，支持两种类型：JAVASCRIPT、SPEL。默认为：SPEL。
        type: SPEL
        #假如 type 为 JAVASCRIPT，则 content 支持三种模式：1）服务器脚本文件路径；2）网络脚本文件路径；3）内联脚本字符串；
        #假如 type 为 SPEL，则 content 仅支持表达式字符串模式，此处SPEL支持两种模式：1）原生SPEL；2）数据生成服务自定义SPEL；
        #注意无论那种模式，当前数据集的上下文关键字均为 dataset
        #以下实例表示使用SPEL的集合投影（expressions-collection-projection）将List<Map> 转换为 List<String>
        content: "#dataset.![CODE]"

# 数据集元数据配置
table:
  # 字段配置列表
  fields:
    #字段配置
    - #虚拟字段用来处理依赖关系字段的数据集选择
      name: virtual_filed
      readers:
        - dataSetId: virtual_filed_ds
          #常量数据集，固定值的数据集，数据生成服务会从中随机选取一个值
          type: CONSTANT
          # 1 普通小车；2 普通大车；3 香港车；4 澳门车；5 教练车；6 警车；7 挂车；
          dataSet: [ 3,4 ]
    - #字段名 信息主键编号
      #字段名，如果该字段的值是需要写入目标数据源的，则需要跟实际目标数据源的字段名保持一致
      name: INFO_ID
      #字段值转换器，默认为字符串转换器
      #通常来说，如果采用批量模式（无论是基于数据库还是ES、Kafka等，基本上不需要特殊的格式），即日期格式通常不需要转换
      #使用标准日期格式的字符串即可（或者是特定的时间戳）
      converter: org.gensokyo.data.generator.converter.LongConverter
      #数据集列表
      readers:
        #单个数据集读取配置
        - #数据集类型
          dataSetId: INFO_ID
          #根据数据生成服务自定义SPEL来生成数据
          type: DIRECT_SPEL
          #自定义SPEL表达式，使用 Faker 的表达式来随机生成雪花算法编号
          #示例：#faker.expression(\"#{date.past '15','DAYS','yyyy-MM-dd hh:mm:ss'}\")[10]
          #上面示例自定义SPEL表达式，使用 Faker 的表达式来随机生成过去 15 天，格式为 yyyy-MM-dd hh:mm:ss 的时间，总共生成 10 条数据
          dataSet: "#faker.snowflake.next"
    - #字段名 车辆类型（二次识别）
      name: CLLX
      #后置脚本处理，根据 readers 中读取的数据集随机选取一个后再获取对象中指定的属性
      postScript:
        content: "#dataset.CODE"
      #数据集列表
      readers:
        #单个数据集读取配置
        - #数据集类型
          dataSetId: f_ds_1
          type: JDBC
          #数据源编号
          dataSourceId: 'pd_dicmanage'
          #数据集，会将查询结果缓存在内存中，然后生成数据时随机从中选取一个，选取结果根据后置脚本类型不同，上下文对象有所不同
          #SPEL脚本：选取结果的上下文对象为 dataset，即选取对象的某个属性为 => #dataset.xxx
          #JAVASCRIPT：选取结果的上下文对象为 context, dataset, arg，其中 context 脚本上下文对象，包含全局数据集，dataset 为当前字段
          #所产生的数据选取结果（同SPEL中的dataset），arg 为附加参数
          dataSet: 'SELECT CODE FROM SYS_DICT WHERE PARENT_ID = 40'
    - #字段名 设备编号
      name: DEVICE_ID
      postScript:
        content: "#dataset.SXJSBBM"
      #数据集列表
      readers:
        #单个数据集读取配置
        - #数据集类型
          dataSetId: DEVICE_ID
          type: JDBC
          #数据源编号
          dataSourceId: 'omof'
          #数据集
          dataSet: >-
            SELECT OVBI.KKBH,OVBI.KKMC,OVBI.KKDZ,OVBI.KKMC,OVBI.XZQH,OVBI.JSDW,OVBI.FXMS,OVBI.ZPFXLX,
            OVBI.DLMC,OVBI.JD,OVBI.WD,OLI.SXJSBBM,OLI.CDBH,OLI.CDLX 
            FROM OMOF_VEHICLE_BARRIER_INFO OVBI INNER JOIN OMOF_LANE_INFO OLI ON OVBI.KKBH = OLI.KKBH
    - #字段名 抓拍时间
      name: JGSK
      converter: org.gensokyo.data.generator.converter.LongConverter
      #数据集列表
      readers:
        #单个数据集读取配置
        - #数据集类型
          dataSetId: JGSK
          #直接表达式
          type: DIRECT_SPEL
          #数据集，使用 Faker 的表达式来随机生成过去 1 天，格式为 yyMMddHHmmss 的时间
          dataSet: >-
            #faker.expression("#{date.past '1','DAYS','yyMMddHHmmss'}")
    - #字段名 号牌号码（二次识别）
      name: HPHM
      #依赖虚拟字段值进行数据生成
      dependsOn:
        - virtual_filed
      #前置脚本处理
      preScript:
        #根据虚拟字段选择的结果值生成号牌号码，vehicleCN为自定义的数据生成器VehicleProvider，plate(String type)为号牌生成方法
        content: "#faker.vehicleCN.plate(#dataset['virtual_filed'])"
    - #字段名 号牌颜色（二次识别）
      name: HPYS
      converter: org.gensokyo.data.generator.converter.LongConverter
      #依赖字段，依赖虚拟字段，根据虚拟字段随机选择的结果值进行映射，通常用于有关联关系的字段，即 字段1 的值会约束 字段2 的值
      dependsOn:
        - virtual_filed
      resultMapper:
        #无法找到映射值时的默认值
        defaultDataset: [ 0 ]
        #字段映射值，此处的 key 值即是依赖字段 virtual_filed 选择结果值
        mappers:
          - key: "1"
            dataset: [ 0 ] #蓝色
          - key: "2"
            dataset: [ 2 ] #黄色
          - key: "3"
            dataset: [ 1 ] #黑色
          - key: "4"
            dataset: [ 1 ] #黑色
          - key: "5"
            dataset: [ 2 ] #黄色
          - key: "6"
            dataset: [ 3 ] #白色
          - key: "7"
            dataset: [ 2 ] #黄色
    - #字段名 全景图URL
      name: PIC_ABBREVIATE
      #数据集列表
      readers:
        #单个数据集读取配置
        - #数据集类型
          dataSetId: PIC_ABBREVIATE
          type: CONSTANT
          #数据集
          dataSet:
            - "http://172.25.21.133:8088/g1/M00/00031003/20210610/rBkVhWDB2GWIEZx-AAdwNGUSeOwAAAAWgAAAAAAB3BM397.jpg"
            - "http://172.25.21.133:8088/g1/M00/00031003/20210528/rBkVhWCwn7GIHz71AADogyvcEjMAAAADAA4xEEAAOib567.jpg"
            - "http://172.25.21.133:8088/simulation/pic/motorvehicle_20200225_16_44050100000034010001_cimya9.Jpeg"
            - "http://172.25.21.133:8088/g1/M00/00031003/20210528/rBkVhWCwn6WISyHPAAB_niC42DUAAAADAA4RIsAAH-2098.jpg"
            - "http://172.25.21.133:8088/simulation/pic/motorvehicle_20200225_16_44050100000031020002_5646rg.Jpeg"
    - #字段名 车身长度，单位：厘米
      name: VEHICLE_LENGTH
      converter: org.gensokyo.data.generator.converter.LongConverter
      #数据集列表
      readers:
        #单个数据集读取配置
        - #数据集类型
          dataSetId: VEHICLE_LENGTH
          type: DIRECT_SPEL
          #数据集，随机生成 3900-5000 的数字
          dataSet: "#faker.number.numberBetween(3900,5000)"
    - #字段名 车道编号
      name: LANE_NO
      converter: org.gensokyo.data.generator.converter.LongConverter
      #依赖字段的结果值
      dependsOn:
        - DEVICE_ID
      #获取依赖字段的结果值的CDBH属性值作为本字段的值
      postScript:
        content: "#dataset.CDBH"
    - #字段名 号牌归属地（省市）
      name: HPGS
      converter: org.gensokyo.data.generator.converter.LongConverter
      #数据集列表
      dependsOn:
        - HPHM
      #根据号牌号码生成号牌归属字典
      postScript:
        content: "#faker.vehicleCN.plateProvince(#dataset)"
    - #字段名 车身区域
      name: CAR_RECT
      #数据集列表
      readers:
        #单个数据集读取配置
        - #数据集类型
          dataSetId: CAR_RECT
          type: CONSTANT
          #数据集
          dataSet: [ "{'X':0,'Y':0,'Width':0,'Height':0}" ]
    - #字段名 车辆抓拍视图库标识
      name: VIID_OBJECT_ID
      #依赖两个字段的结果值
      dependsOn:
        - DEVICE_ID
        - JGSK
      #前置处理脚本，将设备信息对象和抓拍时间值重新组成数组
      preScript:
        content: "{ #dataset['DEVICE_ID'],#dataset['JGSK'] }"
      #后置脚本处理，根据摄像机编号和抓拍时刻生成编号
      postScript:
        content: "#faker.snowflake.viid(#dataset[0].SXJSBBM,'02',#dataset[1],'02')"
    - #字段名 对象关联信息
      name: RELATED_LIST
      #数据集列表
      readers:
        - dataSetId: RELATED_LIST
          type: CONSTANT
          #写入空值
          dataSet: [ ]
  writer:
    #目标数据集类型，支持JDBC、MYSQL、POSTGRES、CLICKHOUSE、ELASTICSEARCH、KAFKA
    #JDBC：支持标准的JDBC驱动，并且以批量插入的方式提交生成的结果集
    #MYSQL，以文件加载的方式批量写入数据
    #POSTGRES，以文件加载的方式批量写入数据
    #CLICKHOUSE，以文件加载的方式批量写入数据
    #ELASTICSEARCH，通过Bulk方式批量写入数据
    #KAFKA，默认单条发送数据
    type: POSTGRES
    #数据源编号，该编号唯一，且与 数据源配置 这个章节中配置的数据源名称（KEY）相同
    dataSourceId: 'writer4'
    # 表名/索引名/主题名
    target: 'car_detect_info_hkmo_yisa'
    #数据写入模板，Elasticsearch和Kafka通常是生成数据模板，以占位符的方式替换数值
    #KAFKA模板：'{"f_string": "${f_string}","f_date": "${f_date}","f_float": ${f_float},"f_integer": ${f_integer}}'
    #ELASTICSEARCH模板：'{"f_string": "${f_string}","f_date": "${f_date}","f_float": ${f_float},"f_integer": ${f_integer}}'
    #数据库模板：字段名以英文逗号","分隔即可，需要注意的是，该模板中的字段名必须与 table.fields 中配置的字段名完全一致
    template: >-
      INFO_ID,CLLX,DEVICE_ID,JGSK,HPHM,HPYS,PIC_ABBREVIATE,VEHICLE_LENGTH,LANE_NO,HPGS,CAR_RECT,VIID_OBJECT_ID,RELATED_LIST
```

### 前后置脚本处理

> 前后置脚本处理目前支持两种方式：`javascript`和`spel`。

#### javascript

javascript脚本目前仅仅支持立即执行的脚本，支持以下三种方式加载脚本内容：

1. 支持内联脚本字符串；
2. 服务器上本地脚本文件；
3. 远程服务器脚本文件（http/https）；

脚本内容示例如下：

```javascript
(context, dataset, args) => {
    var data = [];
    for (var i = 0; i < dataset.length; i++) {
        data.push(arr[i].CODE);
    }
    return data;
}
```

对于上述脚本内容的，有三个参数：`context`、`dataset`、`args`。

1. context 参数包含全局数据集，获取指定数据集方式为如下，其中`global_ds_1`为数据集编号。

```javascript
var ds1 = context.global('global_ds_1');
```

2. dataset 参数为当前脚本执行的数据集参数，通常来说，对于Reader上下文中该数据集通常为单一的数据集，即数组；对于表字段上下文中该数据集为对象，其中可能包含多个Reader的数据集；

```javascript
//Reader 中单一数据集则直接为数组对象，可以直接使用，如下所示
var data = dataset;
//对于表字段上下文的数据集则为对象，可以根据数据集编号来获取对应Reader的数据集，如下所示
var data_2 = dataset['current_ds_2'];
```

3. args 参数为脚本执行的附加参数，目前为预留扩展；

#### spel

`SPEL`表达式目前支持两种方式：原生和自定义。

1. 原生SPEL，文档见：https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#expressions
2. 自定义SPEL，主要是在原生SPEL基础上包装了一层，方便批量执行表达式，后面的`[10]`表示按照前面的表达式执行10次，生成10次结果

```spel
#faker.expression(\"#{date.past '15','DAYS','yyyy-MM-dd hh:mm:ss'}\")[10]
```

### 扩展

#### Reader

目前支持的`Reader`有：ConstantReader、DirectSpelReader、ElasticsearchReader、JdbcReader、KafkaReader、SpelReader

如果需要新增`Reader`，则可以继承 `AbstractReader` 抽象类，实现`Reader`接口的`read`方法

```java
public interface Reader {

    Dataset read(final Context ctx);
}
```

#### Writer

目前支持的`Writer`有：ClickHouseWriter、ElasticsearchWriter、JdbcWriter、KafkaWriter、MySQLWriter、PostgresWriter

如果需要新增`Writer`，则可以继承 `AbstractWriter` 抽象类，实现`Writer`接口的`write`方法

```java
public interface Writer {

   long write(final List<Map<String, Object>> data);
}
```

#### Converter

目前支持的`Converter`有：DateConverter、FloatConverter、LongConverter、StringConverter

如果需要新增`Converter`，则可以实现 `Converter` 接口的`convert`方法

```java
@FunctionalInterface
public interface Converter<S, T> {
    
   @Nullable
   T convert(S source);

   default <U> Converter<S, U> andThen(Converter<? super T, ? extends U> after) {
      Assert.notNull(after, "'after' Converter must not be null");
      return (S s) -> {
         T initialResult = convert(s);
         return (initialResult != null ? after.convert(initialResult) : null);
      };
   }

}
```

#### Script

目前支持的`Script`有：JsScript、SpelScript

如果需要新增`Script`，则可以实现 `Script` 接口的`eval`方法

```java
public interface Script extends AutoCloseable {

   default Object eval(String script, Object dataset, Object... args) {
      return eval(dataset, args);
   }

   Object eval(Object dataset, Object... args);
}
```