<!-- GENERATED 鈥?do not edit by hand. -->
<!-- Source: .planning/test-matrix.yaml -->
<!-- Generator: scripts/generate-test-matrix-doc.ps1 -->

# Test feature matrix

This document is generated from `.planning/test-matrix.yaml`. Edit the YAML source of truth and re-run `scripts/generate-test-matrix-doc.ps1`.

| id | capability | adapter | test_types | owner_module | status | tier | linked_tests |
|----|------------|---------|------------|--------------|--------|------|--------------|
| reader-ai | reader-ai | ai | integration | data-generator-reader-ai | pending | P2 |  |
| reader-csv | reader-csv | csv | unit, integration | data-generator-reader-csv | pending | P2 |  |
| reader-jdbc-basic | reader-jdbc | jdbc | integration | data-generator-test-fixtures | partial | P1 | FixtureReaderJdbcExampleTests |
| reader-elasticsearch | reader-elasticsearch | elasticsearch | integration | data-generator-reader-elasticsearch | pending | P2 |  |
| reader-excel | reader-excel | excel | unit | data-generator-reader-excel | pending | P2 |  |
| reader-json | reader-json | json | unit | data-generator-reader-json | pending | P2 |  |
| reader-spel | reader-spel | spel | unit | data-generator-reader-spel | pending | P2 |  |
| writer-csv | writer-csv | csv | unit | data-generator-writer-csv | pending | P2 |  |
| writer-jdbc-basic | writer-jdbc | jdbc | integration | data-generator-test-fixtures | partial | P1 | FixtureWriterJdbcExampleTests |
| writer-elasticsearch | writer-elasticsearch | elasticsearch | integration | data-generator-calcite | partial | P2 | ElasticsearchRowSinkAdapterHttpEmbeddedTests |
| writer-excel | writer-excel | excel | unit | data-generator-writer-excel | pending | P2 |  |
| writer-json | writer-json | json | unit | data-generator-writer-json | pending | P2 |  |
| writer-kafka | writer-kafka | kafka | integration | data-generator-calcite | partial | P2 |  |
| iterator-constant | iterator-constant | constant | unit | data-generator-iterator-constant | pending | P2 |  |
| iterator-csv | iterator-csv | csv | unit | data-generator-iterator-csv | pending | P2 |  |
| iterator-database | iterator-database | jdbc | integration | data-generator-iterator-database | pending | P2 |  |
| iterator-number | iterator-number | number | unit | data-generator-iterator-number | pending | P2 |  |
| calcite-source-jdbc | calcite-source-jdbc | jdbc | integration | data-generator-calcite | pending | P2 |  |
| calcite-source-csv | calcite-source-csv | csv | integration | data-generator-calcite | pending | P2 |  |
| transform-sql-basic | transform-sql | calcite-sql | integration | data-generator-test-fixtures | partial | P1 | FixtureTransformSqlExampleTests |
| transform-json | transform-json | calcite-json | integration | data-generator-test-fixtures | covered | P0 | FixtureTransformJsonExampleTests |
| transform-mask | transform-mask | calcite-mask | integration | data-generator-test-fixtures | covered | P0 | FixtureTransformMaskExampleTests |
| transform-lookup | transform-lookup | calcite-lookup | integration | data-generator-test-fixtures | covered | P0 | FixtureTransformLookupExampleTests |
| calcite-transform-js | calcite-transform-js | javascript | unit | data-generator-calcite | pending | P2 |  |
| calcite-sink-jdbc | calcite-sink-jdbc | jdbc | integration | data-generator-calcite | pending | P2 |  |
| calcite-sink-elasticsearch | calcite-sink-elasticsearch | elasticsearch | integration | data-generator-calcite | partial | P2 | ElasticsearchRowSinkAdapterHttpEmbeddedTests |
| calcite-pipeline-chunked | calcite-pipeline-chunked | chunked | integration | data-generator-calcite | partial | P1 | ChunkedPipelineTests |
| calcite-scenario-v2 | calcite-scenario-v2 | scenario | integration | data-generator-service | covered | P0 | V2ScenarioTemplateIT |
| generator-sync | generator-sync | sync | unit | data-generator-generator-sync | pending | P2 |  |
| generator-async | generator-async | async | unit | data-generator-generator-async | pending | P2 |  |
| scripter-javascript | scripter-javascript | javascript | unit | data-generator-scripter-javascript | pending | P2 |  |
| scripter-spel | scripter-spel | spel | unit | data-generator-scripter-spel | pending | P2 |  |
| faker-integration | faker-synthetic | datafaker | unit | data-generator-faker | pending | P2 |  |
| geo-synthetic | geo-synthetic | geojson | unit | data-generator-geo | pending | P2 |  |
| console-api-templates | console-api-templates | console-api | unit, e2e | data-generator-service | partial | P1 | ConsoleTemplateControllerTest |
| console-api-jobs | console-api-jobs | console-api | unit, e2e | data-generator-service | partial | P1 | ConsoleJobControllerTest |
| console-api-udf | console-api-udf | console-api | unit | data-generator-service | covered | P1 | ConsoleUdfControllerTest |
| console-api-transforms | console-api-transforms | console-api | unit | data-generator-service | covered | P1 | ConsoleTransformCatalogControllerTest |
| console-api-health | console-api-health | console-api | e2e | data-generator-service | pending | P2 |  |
| console-ui-template-edit | console-ui-template-edit | console-ui | e2e | data-generator-console-web | partial | P2 |  |
| console-ui-job-trigger | console-ui-job-trigger | console-ui | e2e | data-generator-console-web | partial | P2 |  |
| console-ui-navigation | console-ui-navigation | console-ui | e2e | data-generator-console-web | pending | P2 |  |
| console-ui-api-smoke | console-ui-api-smoke | console-ui | e2e | data-generator-console-web | pending | P2 |  |
| stage-pipeline-v1 | stage-pipeline-v1 | v1-stage | integration | data-generator-stage | pending | P2 |  |
| service-task-api | service-task-api | rest | integration | data-generator-service | pending | P2 |  |
| udf-sql | udf-sql | calcite-sql | unit | data-generator-service | covered | P0 | UdfPublishServiceTests, UdfConsoleTemplateBindingE2ETests |
| udf-script | udf-script | javascript | unit | data-generator-calcite | covered | P0 |  |
| udf-java-plugin | udf-java-plugin | pf4j | unit | data-generator-service | covered | P0 | UdfGovernanceSupportTests, UdfConsoleTemplateBindingE2ETests |
| v2-streaming-csv | v2-streaming-csv | csv | unit, integration | data-generator-calcite | covered | P0 |  |
| v2-streaming-json | v2-streaming-json | json | unit, integration | data-generator-calcite | covered | P0 |  |
| v2-jdbc-upsert-pg-mysql | v2-jdbc-upsert | jdbc | integration, unit | data-generator-calcite | covered | P0 |  |
| v2-dialect-dameng | v2-dialect-dameng | jdbc | unit | data-generator-calcite | covered | P0 | JdbcSinkSqlBuilderTests |
| v2-dialect-kingbase | v2-dialect-kingbase | jdbc | integration, unit | data-generator-calcite | covered | P0 |  |
| v2-dialect-highgo | v2-dialect-highgo | jdbc | integration, unit | data-generator-calcite | covered | P0 |  |
| v2-dialect-postgres | v2-dialect-postgres | jdbc | integration | data-generator-calcite | covered | P0 | ChunkedPipelinePostgresUpsertTests |
| v2-dialect-clickhouse | v2-dialect-clickhouse | jdbc | integration, unit | data-generator-calcite | covered | P0 |  |
| dist-multi-jvm-worker | distributed-multi-jvm-worker | host-jvm | integration | data-generator-service | covered | P1 |  |
| exec-http-managed-catalog | http-execute-managed-catalog | rest | integration | data-generator-service | covered | P1 | ManagedJdbcCatalogHttpExecuteIT |
| exec-http-postgres-dialect | http-execute-postgres-dialect | rest | integration | data-generator-service | covered | P1 | ManagedJdbcCatalogHttpPostgresUpsertIT |
| rbac-enable-path | console-rbac-enable | console-api | integration, unit | data-generator-service | covered | P1 |  |

