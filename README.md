# data-generator

数据生成服务是通过给定的模板批量生成模拟数据，并且写入目标数据源的工具。

1. 读取数据源支持MySQL、Postgresql、Clickhouse、Elasticsearch；
2. 支持常量、动态表达式生成数据；
3. 写入数据源支持MySQL、Postgresql、Clickhouse、Elasticsearch、Kafka；
4. 支持Rest接口启动任务；
5. 支持带权重的数据生成；
6. 支持数据生成顺序控制；
7. 支持单个或者多个字段依赖；
8. 支持SQL参数化；
9. 支持多个写入数据源；
10. 支持全局缓存数据获取；
11. 支持复杂阶段处理；
12. 支持条件判断阶段；
13. 支持地理空间数据：V1/V2 合成 GEO 迭代器、GeoJSON/PostGIS 源、Template V2 SQL 地理函数（距离/谓词/近似 buffer），用法见 `docs/geospatial-phase1-usage.md`。

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
   替换为需要执行的模板名

### 模板规范

模板配置主要分以下几部分：

1. 模板公共部分；
2. 表数据集来源元数据配置，包含各个字段数据源读取、处理、选择、转换、映射等配置，可以包含虚拟字段，字段依赖等；
3. 表数据集写入器配置；

> **注意：模板校验文件路径为：src/resources/META-INF/template-schema.json**

样例配置如下：
```yaml
# 数据集名称，各个文件中唯一
name: demo_00
#生成数量
amount: 10
#批次数量
batchSize: 5
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

# 数据集元数据配置
table:
   fields:
      - name: ID #字段名称
        stages:  # 字段处理阶段
           - type: READ #读取阶段
             readers: # 读取器配置
                - type: SPEL # spel表达式读取器
                  content: "#faker.snowflake.next" # 表达式内容
      - name: DISTRICT_CODE
        stages:
           - type: READ
             inMemory: true # 驻留内存数据源
             readers:
                - type: JDBC # jdbc读取器
                  dataSourceId: 'system_manage' # 数据源编号，对应上面数据源配置
                  content: "SELECT CODE,NAME,PARENT_CODE FROM PC_DISTRICT WHERE PARENT_CODE = '440100' LIMIT 10" # SQL语句
           - type: SELECT # 上一阶段的结果作为输入，选择字段值（如果为单值或者Map类的值，则选择阶段无效，直接返回原值，如果为集合类型，则按配置的选择策略选择一个值）
           - type: SCRIPT # 上一阶段的结果作为输入，执行脚本，生成新值
             scriptType: SPEL # 脚本类型
             content: "#dataset.CODE" # 脚本内容
      - name: DISTRICT_NAME
        dependsOn: # 字段依赖集合
           - DISTRICT_CODE # 依赖字段名称，依赖该字段最后的生成结果作为输入
        stages:
           - type: SCRIPT # 上一阶段的结果作为输入（此时的输入值为依赖字段的生成结果），执行脚本，生成新值
             scriptType: SPEL # 脚本类型
             content: "#dataset.NAME" # 脚本内容
      - name: STATUS
        stages:
           - type: READ
             inMemory: true
             readers:
                - type: CONSTANT
                  content: [ 'S0A', 'S0X' ,'S0D', 'S0H' ]
           - type: SELECT
      - name: CREATE_TIME
        stages:
           - type: READ
             readers:
                - type: SPEL
                  content: >-
                     #faker.expression("#{date.past '1','DAYS','yyyy-MM-dd HH:mm:ss'}")

# 数据输出配置
output:
   writers:
      - type: CONSOLE # 控制台输出器
```

### 阶段处理

阶段处理主要分以下几类：
1. READ 读取阶段，主要用于从数据源读取数据；
2. SCRIPT 脚本阶段，主要用于执行脚本，生成新值；
3. SELECT 选择阶段，主要用于选择字段值；
4. MAPPING 映射阶段，主要用于字段值映射；
5. CONVERT 转换阶段，主要用于字段值类型转换；
6. CONDITION 条件判断阶段，主要用于根据字段值进行条件判断，使用不同的分支处理；
6. WRITE 写入阶段，主要用于写入数据；

> 一个字段的处理阶段可以有1...N个，核心处理逻辑是才有流水线的方式，上一个阶段的输出会作为下一阶段的输入。

### 脚本处理

>脚本处理目前支持两种方式：`javascript`和`spel`。

#### javascript

javascript脚本目前仅仅支持立即执行的脚本，支持以下三种方式加载脚本内容：

1. 支持内联脚本字符串；
2. 服务器上本地脚本文件；
3. 远程服务器脚本文件（http/https）；

脚本内容示例如下：

```javascript
(dataset, args) => {
    var data = [];
    for (var i = 0; i < dataset.length; i++) {
        data.push(arr[i].CODE);
    }
    return data;
}
```

对于上述脚本内容的，有两个个参数：`dataset`、`args`。

1. dataset 参数为当前脚本执行的数据集参数，通常来说是上一个阶段的输出值；
2. args 参数为脚本执行的附加参数，为数组类型，其中`args[0]`是当前所有驻留内存数据集的集合，其他的作为预留扩展参数；

#### spel

`SPEL` 请参考官方文档：https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#expressions

### 扩展

#### Stage

> 目前支持的`Stage`有：ConstantStage、DirectSpelStage、ElasticsearchStage、JdbcStage、KafkaStage、MappingStage、ReadStage、ScriptStage、SelectStage、WriteStage

如需新增`Stage`，则可以继承 `AbstractStage` 抽象类，实现`Stage`接口的`internalExecute`方法

```java
@FunctionalInterface
public interface Stage {

   /**
    * 省略其他方法...
    */

   /**
    * 执行处理阶段（实际内部处理方法）
    *
    * @param input 输入值
    * @return 输出值
    */
   Value internalExecute(Value input);
}
```

#### Reader

目前支持的`Reader`有：ConstantReader、JdbcReader、SpelReader

如果需要新增`Reader`，则可以实现`Reader`接口的`read`方法

```java
public interface Reader<T extends ReaderPO> {

   Value read(final ReaderContext<T> ctx, final Value input);
}
```

#### ReaderSelectStrategy

目前支持的`ReaderSelectStrategy`有：EqualReaderSelectStrategy、WeightReaderSelectStrategy

如果需要新增`ReaderSelectStrategy`，则可以实现`ReaderSelectStrategy`接口的`select`方法

```java
@FunctionalInterface
public interface ReaderSelectStrategy<T extends ReaderPO> {

   /**
    * 数据选择策略
    *
    * @param rpo 读取阶段信息
    * @return 选择结果
    */
   T select(final ReadStagePO rpo);
}
```

#### Writer

目前支持的`Writer`有：ClickHouseWriter、ElasticsearchWriter、JdbcWriter、KafkaWriter、MySQLWriter、PostgresWriter、ConsoleWriter

如果需要新增`Writer`，则可以实现`Writer`接口的`write`方法

```java
@FunctionalInterface
public interface Writer<T extends WriterPO> {

   /**
    * 写入数据集
    *
    * @param ctx     写入上下文
    * @param dataset 数据集
    * @return 写入数据量
    */
   long write(final WriterContext<T> ctx, final List<Map<String, Object>> dataset);
}
```

#### Converter

目前支持的`Converter`有：StringConverter

如果需要新增`Converter`，则可以实现 `Converter` 接口的`convert`方法

```java
@FunctionalInterface
public interface Converter {

   /**
    * 将输入值转换为指定的输出值
    *
    * @param input 输入值
    * @return 输出值
    */
   Value convert(Value input);
}
```

#### Script

目前支持的`Script`有：JsScript、SpelScript

如果需要新增`Script`，则可以实现 `Script` 接口的`eval`方法

```java
@FunctionalInterface
public interface Script {

   /**
    * 执行脚本
    *
    * @param spo     脚本阶段配置
    * @param dataset 输入数据集
    * @param args    输入参数列表
    * @return 脚本执行结果
    */
   Value eval(final ScriptStagePO spo, final Value dataset, Object... args);
}
```

#### ValueSelectStrategy

目前支持的`ValueSelectStrategy`有：RepeatRandomValueSelectStrategy、RepeatOrderValueSelectStrategy、OnceRandomValueSelectStrategy、OnceOrderValueSelectStrategy、MultipleOrderValueSelectStrategy

如果需要新增`ValueSelectStrategy`，则可以实现 `ValueSelectStrategy` 接口的`select`方法

```java
@FunctionalInterface
public interface ValueSelectStrategy {

    /**
     * 数据选择策略
     *
     * @param index         选择索引
     * @param selectedCount 已选择选择次数
     * @param spo           选择阶段参数
     * @param input         给定数据集
     * @return 选择结果
     */
    Value select(final AtomicInteger index, final AtomicInteger selectedCount, SelectStagePO spo, final Value input);
}
```