<!-- GENERATED — do not edit by hand. -->
<!-- Source: .planning/test-matrix.yaml -->
<!-- Generator: scripts/generate-test-matrix-doc.ps1 -->

# Test feature matrix

This document is generated from `.planning/test-matrix.yaml`. Edit the YAML source of truth and re-run `scripts/generate-test-matrix-doc.ps1`.

| id | capability | adapter | test_types | owner_module | status | linked_tests |
|----|------------|---------|------------|--------------|--------|--------------|
| reader-ai | reader-ai | ai | integration | data-generator-reader-ai | pending |  |
| reader-csv | reader-csv | csv | unit, integration | data-generator-reader-csv | pending |  |
| reader-jdbc-basic | reader-jdbc | jdbc | integration | data-generator-test-fixtures | pending |  |
| reader-elasticsearch | reader-elasticsearch | elasticsearch | integration | data-generator-reader-elasticsearch | pending |  |
| reader-excel | reader-excel | excel | unit | data-generator-reader-excel | pending |  |
| reader-json | reader-json | json | unit | data-generator-reader-json | pending |  |
| reader-spel | reader-spel | spel | unit | data-generator-reader-spel | pending |  |
| writer-csv | writer-csv | csv | unit | data-generator-writer-csv | pending |  |
| writer-jdbc-basic | writer-jdbc | jdbc | integration | data-generator-test-fixtures | pending |  |
| writer-elasticsearch | writer-elasticsearch | elasticsearch | integration | data-generator-writer-elasticsearch | partial | ElasticsearchRowSinkAdapterHttpEmbeddedTests |
| writer-excel | writer-excel | excel | unit | data-generator-writer-excel | pending |  |
| writer-json | writer-json | json | unit | data-generator-writer-json | pending |  |
| writer-kafka | writer-kafka | kafka | integration | data-generator-writer-kafka | partial |  |
| iterator-constant | iterator-constant | constant | unit | data-generator-iterator-constant | pending |  |
| iterator-csv | iterator-csv | csv | unit | data-generator-iterator-csv | pending |  |
| iterator-database | iterator-database | jdbc | integration | data-generator-iterator-database | pending |  |
| iterator-number | iterator-number | number | unit | data-generator-iterator-number | pending |  |
| calcite-source-jdbc | calcite-source-jdbc | jdbc | integration | data-generator-calcite | pending |  |
| calcite-source-csv | calcite-source-csv | csv | integration | data-generator-calcite | pending |  |
| transform-sql-basic | transform-sql | calcite-sql | integration | data-generator-test-fixtures | pending |  |
| calcite-transform-js | calcite-transform-js | javascript | unit | data-generator-calcite | pending |  |
| calcite-sink-jdbc | calcite-sink-jdbc | jdbc | integration | data-generator-calcite | pending |  |
| calcite-sink-elasticsearch | calcite-sink-elasticsearch | elasticsearch | integration | data-generator-calcite | partial | ElasticsearchRowSinkAdapterHttpEmbeddedTests |
| calcite-pipeline-chunked | calcite-pipeline-chunked | chunked | integration | data-generator-calcite | partial | ChunkedPipelineTests |
| calcite-scenario-v2 | calcite-scenario-v2 | scenario | integration | data-generator-service | partial | V2ScenarioTemplateIT |
| generator-sync | generator-sync | sync | unit | data-generator-generator-sync | pending |  |
| generator-async | generator-async | async | unit | data-generator-generator-async | pending |  |
| scripter-javascript | scripter-javascript | javascript | unit | data-generator-scripter-javascript | pending |  |
| scripter-spel | scripter-spel | spel | unit | data-generator-scripter-spel | pending |  |
| faker-integration | faker-synthetic | datafaker | unit | data-generator-faker | pending |  |
| geo-synthetic | geo-synthetic | geojson | unit | data-generator-geo | pending |  |
| console-api-templates | console-api-templates | console-api | unit, e2e | data-generator-service | partial | ConsoleTemplateControllerTest |
| console-api-jobs | console-api-jobs | console-api | unit, e2e | data-generator-service | partial | ConsoleJobControllerTest |
| console-api-health | console-api-health | console-api | e2e | data-generator-service | pending |  |
| console-ui-template-edit | console-ui-template-edit | console-ui | e2e | data-generator-console-web | partial |  |
| console-ui-job-trigger | console-ui-job-trigger | console-ui | e2e | data-generator-console-web | pending |  |
| console-ui-navigation | console-ui-navigation | console-ui | e2e | data-generator-console-web | pending |  |
| console-ui-api-smoke | console-ui-api-smoke | console-ui | e2e | data-generator-console-web | pending |  |
| stage-pipeline-v1 | stage-pipeline-v1 | v1-stage | integration | data-generator-stage | pending |  |
| service-task-api | service-task-api | rest | integration | data-generator-service | pending |  |

