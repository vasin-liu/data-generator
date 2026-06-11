# data-generator

面向测试数据、样例数据与批量数据管道的 **Java 数据生成平台**。通过 YAML 模板定义 **读取 → 转换 → 写入** 流程，支持 JDBC、Elasticsearch、Kafka、文件等多种数据源，可由 **运营控制台** 或 **REST API** 触发执行。

当前主线版本：**3.0.0-SNAPSHOT**（**Template V2**）。Legacy V1 字段阶段模型已退役，任务执行仅支持 V2 模板。

**文档语言：** 中文 · [English](README.en.md) · [快速入门](docs/quickstart.md)

---

## 核心能力

| 能力 | 说明 |
|------|------|
| **Template V2** | 声明式 `sources` / `transform` / `sink`；支持 SQL（Calcite）、SpEL、JavaScript 转换 |
| **工作流与计算块** | L2 `workflow` 步骤（日志、暂停、分支、共享作用域）+ L1 `computeBlocks` / `transformGraph` DAG |
| **多源读取** | JDBC 查询、迭代器、CSV/Excel/JSON、Elasticsearch、GeoJSON/PostGIS、AI 生成等 |
| **多目标写入** | MySQL、PostgreSQL、ClickHouse、Elasticsearch、Kafka、CSV/Excel/JSON、控制台 |
| **运营控制台** | React SPA：模板编辑、数据源管理、任务历史、Cron 调度、审计 |
| **任务治理** | 草稿 / 发布生命周期、密钥引用、运行报告与指标 |
| **定时调度** | 基于 Cron 的模板定时触发（可开关） |
| **分布式执行** | 协调器 / Worker 队列、租约与心跳（可开关） |
| **地理空间** | 合成 GEO 迭代器、GeoJSON/PostGIS 源、V2 地理 SQL 函数 |
| **插件扩展** | PF4J 自定义 Transformer；模块化 Reader / Writer / Iterator |

---

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言与构建 | Java **25**、Maven 多模块、`mvnw` / `mvnw-jdk25.ps1` |
| 运行时 | Spring Boot **4.x**（`data-generator-service`） |
| 控制台前端 | React 19、Vite、Ant Design（`data-generator-console-web`） |
| 转换引擎 | Apache Calcite、GraalJS、SpEL、DataFaker |
| 数据访问 | 动态 JDBC（Druid）、Elasticsearch、Kafka |
| 序列化 | Jackson 3.x、YAMLBeans、JSON Schema |

---

## 项目结构

```
data-generator/
├── data-generator-service/          # Spring Boot 可执行服务（REST + 控制台静态资源）
├── data-generator-console-web/      # 运营控制台 React 源码
├── data-generator-calcite/          # Template V2 运行时与 SQL 引擎
├── data-generator-common/           # 公共模型与工具
├── data-generator-datasource/       # 数据源抽象
├── data-generator-stage/            # 阶段处理（V1 遗留模块，运行时已退役）
├── data-generator-reader/           # 读取器（JDBC、CSV、Excel、ES、AI…）
├── data-generator-writer/           # 写入器（JDBC、Kafka、ES、文件…）
├── data-generator-iterator/         # 迭代器（数值、日期、GEO、数据库…）
├── data-generator-scripter/         # 脚本引擎（GraalJS、SpEL、Velocity）
├── data-generator-geo/              # 地理空间工具与谓词
├── data-generator-faker/            # 假数据集成
├── data-generator-converter/        # 类型转换
├── data-generator-generator/        # 生成编排
├── data-generator-dependencies/     # 依赖 BOM
├── docs/                            # 设计说明、迁移与专题文档
└── samples/                         # 示例插件等
```

---

## 快速开始

手把手入门见 **[docs/quickstart.md](docs/quickstart.md)**。

### 环境要求

- **JDK 25**
- **Node.js 22+**（仅本地开发控制台前端时需要）
- Maven 使用仓库内配置：`.mvn/settings-jdk25.xml`（内网 Nexus 可能为 HTTP）

### 构建与运行

`data-generator-service` 在 `process-classes` 阶段会把 `data-generator-console-web/target/console-dist` 嵌入 JAR。**单独打包 service 前必须先构建控制台前端**，否则会报 `console-dist does not exist`。

```powershell
# 查看 Maven / Java 版本
.\mvnw-jdk25.ps1 -v

# 推荐：一次构建前端 + 服务（跳过测试）
.\mvnw-jdk25.ps1 -pl data-generator-console-web,data-generator-service -am -DskipTests package

# 仅启动服务（需已 package 成功）
.\mvnw-jdk25.ps1 -pl data-generator-service spring-boot:run
```

仅改后端、不需要 `/console/` 静态资源时，可临时跳过嵌入（**不推荐**用于验证控制台）：

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-service -am -DskipTests -Dskip.console.frontend=true package
```

### 访问入口

| 入口 | 地址 |
|------|------|
| 运营控制台 | http://localhost:9876/console/ |
| 控制台 API | http://localhost:9876/api/… |
| 遗留 REST | `/template/…`、`/task/…`、`/datasource/…` |
| H2 控制台（开发） | http://localhost:9876/h2 |

### 本地前端热更新

后端保持在 **9876** 端口，另开终端：

```powershell
cd data-generator-console-web
npm install
npm run dev
```

浏览器打开 http://localhost:5173/console/ ，Vite 会将 `/api` 代理到后端。

### 验证

```powershell
# 全量测试：先构建 console-dist，再跑全仓库 test
.\mvnw-jdk25.ps1 -pl data-generator-console-web -DskipTests package
.\mvnw-jdk25.ps1 test

# 或一步（console-web + service 及其依赖）
.\mvnw-jdk25.ps1 -pl data-generator-console-web,data-generator-service -am test

# 控制台切片（更快；含前端构建 + 控制台相关 Java 测试）
.\scripts\verify-console-unit.ps1 -IncludeWebBuild
```

更多构建说明见 [`docs/jdk25-upgrade.md`](docs/jdk25-upgrade.md)。

---

## 运营控制台

控制台用于 **Template V2** 全生命周期管理，不再提供 V1 模板或迁移工作台。

推荐流程：

1. **数据源** — 登记 JDBC 连接（支持常用驱动预设）
2. **模板** — 向导创建：General → Sources → Transform → Sinks → Execution
3. **校验与发布** — Review 页 Validate → Save → Publish（`PUBLISHED` 状态方可正式运行）
4. **执行** — 手动 Run 或配置 Cron 调度
5. **任务** — 查看运行报告、错误样本与分布式作业信息

详细操作说明：[`docs/operator-console-usage.md`](docs/operator-console-usage.md)

---

## Template V2 简介

V2 以 **数据源（sources）→ 转换（transform / transformers / transformGraph）→ 输出（sink）** 为主线。复杂场景可使用 **workflow + computeBlocks** 组合多个计算块。

### 最小示例（合成数据 + SQL 投影 + 控制台输出）

```yaml
name: demo-synthetic
sources:
  seed:
    type: iterator
    iterator:
      type: number
      from: 1
      to: 5
      step: 1
transform:
  type: sql
  sql: SELECT value AS id, value * 10 AS score FROM seed
sink:
  writers:
    - type: console
```

更多场景样例位于 `data-generator-service/src/main/resources/template/v2-scenarios/`（如 `scenario-a-synthetic.yaml`、`scenario-b-lookup-join.yaml`）。

### 能力分层（简表）

| 层级 | 用途 | YAML 位置 |
|------|------|-----------|
| **线性管道** | 单条 sources → transform → sink | 根级 `sources` / `transform` / `sink` |
| **L1 转换 DAG** | 多节点变换图 | `transformGraph` 或 `computeBlocks[].transformGraph` |
| **L2 工作流** | 步骤编排、分支、暂停 | 根级 `workflow.steps` + `computeBlocks` |

专题文档：

- 工作流编写：[`docs/template-v2-workflow-authoring-guide.md`](docs/template-v2-workflow-authoring-guide.md)
- 转换策略（SQL / SpEL / JS）：[`docs/template-v2-transformer-strategy.md`](docs/template-v2-transformer-strategy.md)
- 场景目录：[`docs/template-v2-scenario-template-catalog.md`](docs/template-v2-scenario-template-catalog.md)
- JDBC 分块执行：[`docs/template-v2-jdbc-chunked-execution-guide.md`](docs/template-v2-jdbc-chunked-execution-guide.md)
- 流式执行：[`docs/template-v2-streaming-execution-guide.md`](docs/template-v2-streaming-execution-guide.md)

---

## 数据源配置

服务使用 **动态多数据源**。每个数据源以唯一 **编号（name）** 注册，模板中通过 `dataSourceId` 或内联 `dataSource` 引用。

### JDBC（内置元数据库 + 业务库）

```yaml
spring:
  datasource:
    dynamic:
      primary: data-generator
      datasource:
        data-generator:
          url: jdbc:h2:file:./db/data-generator
          username: sa
          password: ""
          driver-class-name: org.h2.Driver
          type: com.alibaba.druid.pool.DruidDataSource
          init:
            schema: classpath:db/schema.sql
        my-mysql:
          url: jdbc:mysql://localhost:3306/demo?useSSL=false&serverTimezone=UTC
          username: demo
          password: "${DB_PASSWORD}"
          driver-class-name: com.mysql.cj.jdbc.Driver
          type: com.alibaba.druid.pool.DruidDataSource
```

支持的 JDBC 方言包括 **MySQL、PostgreSQL、ClickHouse、H2、达梦（DM）、人大金仓、瀚高等**（以各 `data-generator-writer-database` 模块为准）。

生产环境请通过控制台 **密钥引用** 或环境变量注入密码，避免在模板 YAML 中写明文密码。见 [`docs/template-v2-datasource-and-secret-governance.md`](docs/template-v2-datasource-and-secret-governance.md)。

### Elasticsearch

```yaml
spring:
  elasticsearch:
    multiple:
      primary: es1
      clusters:
        es1:
          uris:
            - https://localhost:9200
          username: elastic
          password: "${ES_PASSWORD}"
```

### Kafka

```yaml
spring:
  kafka:
    multiple:
      primary: kafka1
      clusters:
        kafka1:
          bootstrap-servers:
            - localhost:9092
```

控制台也可在 **数据源** 页面动态登记 JDBC，无需改 `application.yaml`。

---

## 运行任务

### 方式一：运营控制台（推荐）

在模板 Review 页或列表页点击 **Run**，跳转至任务详情查看 `SUCCESS` / `FAILED` 状态与结构化运行报告。

### 方式二：REST API

```http
# 按模板 ID 启动（POST 推荐）
POST /task/run/{templateId}

# 按名称启动（名称唯一时）
GET  /task/runByName/{templateName}

# 查询模板列表
GET  /task/list
```

当 `data.generator.governance.require-published-for-task-run=true`（默认）时，仅 **已发布** 模板可通过 `/task/run` 执行；编辑器内草稿运行走控制台专用接口。

---

## 运行时配置

常用 `application.yaml` 片段（`data.generator` 前缀）：

```yaml
data:
  generator:
    # V1 执行路径已退役，保留配置项仅为兼容
    v1-execution:
      enabled: false
    governance:
      require-published-for-task-run: true
      reject-plaintext-passwords-in-templates: true
    schedule:
      enabled: false          # true 时启用 Cron 调度轮询
      poll-delay-ms: 60000
    distributed:
      enabled: false          # true 时启用分布式队列
      worker-enabled: false
      lease-seconds: 30
    preview-max-rows: 100
    v2-plugin-directories: [] # PF4J 插件目录
```

分布式部署说明：[`docs/staging-distributed-deployment.md`](docs/staging-distributed-deployment.md)（默认关闭；协调器 / Worker 双 JVM，见文档中的 Spring profile）

并行多 Sink、JDBC 方言选项：[`docs/template-v2-jdbc-sink-guide.md`](docs/template-v2-jdbc-sink-guide.md)

运行时标志可通过 `GET /api/console/runtime` 查询，控制台首页会展示调度 / 分布式模式状态。

---

## 地理空间

支持合成点生成、GeoJSON/PostGIS 读取，以及在 Calcite SQL 中使用内置 `V2_GEO_*` 函数（距离、包含、缓冲等）。

- 总览：[`docs/geospatial-overview.md`](docs/geospatial-overview.md)
- 用法：[`docs/geospatial-phase1-usage.md`](docs/geospatial-phase1-usage.md)

---

## 扩展开发

平台采用 **可插拔模块** 设计，扩展点包括：

| 扩展点 | 模块位置 | 说明 |
|--------|----------|------|
| Reader | `data-generator-reader-*` | 实现 `Reader` 接口 |
| Writer | `data-generator-writer-*` | 实现 `Writer` 接口 |
| Iterator | `data-generator-iterator-*` | 注册迭代器类型 |
| Transformer 插件 | PF4J | 见 `docs/template-v2-pf4j-custom-transform-guide.md` |
| Calcite UDF | `data-generator-calcite` | SQL 函数注册 |

示例 PF4J 插件：`samples/template-v2-pf4j-plugin/`

AI 读取器（Ollama 等）在本地无模型端点时会自动跳过相关测试。

---

## 文档索引

| 主题 | 文档 |
|------|------|
| 快速入门 | [`docs/quickstart.md`](docs/quickstart.md) |
| 控制台使用 | [`docs/operator-console-usage.md`](docs/operator-console-usage.md) |
| Template V2 产品路线图 | [`docs/template-v2-product-roadmap.md`](docs/template-v2-product-roadmap.md) |
| Calcite 实现状态 | [`docs/calcite-implementation-status.md`](docs/calcite-implementation-status.md) |
| V1 → V2 映射 | [`docs/calcite-v1-v2-mapping.md`](docs/calcite-v1-v2-mapping.md) |
| 嵌入式测试约定 | [`docs/testing-embedded-components.md`](docs/testing-embedded-components.md) |
| JDK 25 升级说明 | [`docs/jdk25-upgrade.md`](docs/jdk25-upgrade.md) |
| V1 迁移归档 | [`docs/archive/migration/`](docs/archive/migration/) |
| AI 贡献者指南 | [`AGENTS.md`](AGENTS.md) |

---

## 许可证与归属

Copyright © 2021–2026 PCI Technology Group Co.,Ltd. All Rights Reserved.

内部 Maven 制品与 SCM 配置见根 `pom.xml`。
