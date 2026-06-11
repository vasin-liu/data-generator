# Template V2 Custom Transform Plugins (Operator Guide)

## When to use a plugin

Use a PF4J runtime plugin when built-in transforms (`sql`, `spel`, `js`) are not enough and you need team-owned Java logic in the L1 transform layer.

V2 does **not** load V1 stage types. Register a `V2TransformFactory` through the Template V2 runtime plugin SPI instead.

## What you ship

1. A PF4J plugin JAR with `plugin.properties` and a `TemplateV2RuntimePlugin` implementation.
2. One or more `V2TransformFactory` beans that recognize your `TransformVO` subtype.
3. Optional console documentation for operators (transform `type` string and YAML fields).

## Packaging reference

See [calcite-pf4j-plugin-packaging.md](./calcite-pf4j-plugin-packaging.md) for:

- required `plugin.properties` fields
- host classloader isolation rules
- repository `TemplateV2RuntimePlugin` descriptor layout
- sample module under `samples/template-v2-pf4j-plugin/`

## Authoring in YAML

After the plugin is installed on the service host:

```yaml
transformGraph:
  transforms:
    enrich:
      type: my_custom_transform
      optionA: value
  nodes:
    - id: n1
      transformId: enrich
```

The console editor does not generate custom transform forms automatically; use the YAML tab or extend the console when a transform becomes a first-class product feature.

## Verification

1. Place the plugin JAR on the configured PF4J plugin path.
2. Restart the service and confirm the plugin appears in runtime diagnostics.
3. Validate the template (`POST /api/templates/draft/validate`).
4. Run preview with `executionPolicy.mode: IN_MEMORY` before scheduling production runs.
