# Phase 6: Datasource Platform Core — Pattern Map

**Generated:** 2026-06-24
**Purpose:** Analog files and integration seams for planners/executors.

## Files to Create/Modify

| Role | Path | Data flow |
|------|------|-----------|
| API contracts | `data-generator-datasource/data-generator-datasource-api/` | Catalog interfaces consumed by calcite + service |
| JDBC adapter | `data-generator-datasource/data-generator-datasource-jdbc/` | Resolves JDBC → `DataSource` / routing keys |
| Kafka adapter | `data-generator-datasource/data-generator-datasource-kafka/` | Resolves cluster → `KafkaTemplate` |
| ES adapter | `data-generator-datasource/data-generator-datasource-elasticsearch/` | Resolves cluster → `RestClient` |
| Service catalog | `data-generator-service/.../ConnectionCatalogImpl` | Delegates CRUD to existing services; resolve-only Catalog |
| Calcite runtime | `TemplateV2RuntimeServices`, sink/source factories | Consume `ConnectionCatalog` via api module only |

## Analog: JDBC endpoint resolution (migrate from)

`DefaultRuntimeJdbcEndpointResolver` — managed id first, inline `InlineDataSourceVO` fallback, `SecretResolver` for passwords:

```java
// data-generator-service/.../DefaultRuntimeJdbcEndpointResolver.java
if (StrKit.isNotBlank(source.getDataSourceId())) {
    return source.getDataSourceId();
}
return ensureInlineDataSource(source.getDataSource(), source.getDataSourceId());
```

## Analog: Kafka/ES registry (relocate to adapters)

`DynamicKafkaTemplateRegistry.template(cluster)` — blank cluster → primary:

```java
// data-generator-core/.../DynamicKafkaTemplateRegistry.java
public KafkaTemplate<String, String> template(String cluster) {
    // primary fallback when cluster blank
}
```

## Analog: Runtime service bundle (evolve)

`TemplateV2RuntimeServices` currently holds registries directly — Phase 6 injects `ConnectionCatalog`:

```java
// data-generator-calcite/.../TemplateV2RuntimeServices.java
public record TemplateV2RuntimeServices(
    NamedParameterJdbcTemplate jdbcTemplate,
    DynamicKafkaTemplateRegistry kafkaTemplateRegistry,
    ...
)
```

## Analog: Maven empty aggregator (populate)

`data-generator-datasource/pom.xml` — `packaging pom`, empty `<modules>` — add api/jdbc/kafka/elasticsearch submodules.

## Analog: Service bootstrap

`DataSourceBootstrap` + `MessagingClusterConfigService.@PostConstruct` — register yaml/DB entries at startup into Catalog.

## Test patterns

- Unit: per adapter module (`*Tests.java` suffix)
- Service regression: `DataSourceConfigServiceTests`, `ConsoleDataSourceControllerTest`
- Integration: `V2ScenarioTemplateIT`, `KafkaSinkFactoryTests`, `ElasticsearchSinkFactoryTests`
- Calcite embedded: `InMemoryCatalog` test bean pattern (similar to `ChunkedJdbcParitySupport`)

## PATTERN MAPPING COMPLETE
