# Calcite / Template V2 PF4J Plugin Packaging

## Goal

Define the minimum packaging contract for external Template V2 runtime plugins when PF4J is used as the external plugin framework.

This document is intentionally minimal. It describes the current repository contract, not a future marketplace or remote distribution design.

## Current Direction

External plugins should now be treated as PF4J-first.

Current recommendation:

- keep V2 runtime abstractions repository-local
- use PF4J as the primary external plugin loading and class-isolation layer
- keep the old `ServiceLoader + shared URLClassLoader` path only as fallback or temporary compatibility mode

## What A Plugin Contributes

An external plugin contributes one or more:

- `V2SourceFactory`
- `V2TransformFactory`
- `V2SinkFactory`

through the repository host contract:

- `Pf4jTemplateV2RuntimeExtension`
- `TemplateV2RuntimePluginProvider`
- `TemplateV2RuntimePlugin`

The plugin should not bypass these host abstractions.

## Required Files In The Jar

### 1. PF4J plugin descriptor

Path:

- `plugin.properties`

Minimum fields:

```properties
plugin.id=my-plugin
plugin.version=1.0.0
plugin.provider=my-team
plugin.requires=1.0.0
```

Notes:

- `plugin.id` must be unique across all installed plugins
- `plugin.version` is the plugin version
- `plugin.provider` identifies the publisher/team
- `plugin.requires` is currently used as the PF4J-side host/runtime compatibility declaration

### 2. Repository plugin descriptor

Path:

- `META-INF/data-generator/template-v2-plugin.properties`

Minimum example:

```properties
plugin.id=my-plugin
plugin.version=1.0.0
plugin.provider=my-team
plugin.host-version-range=current
plugin.capabilities=SOURCE:custom_source,TRANSFORM:custom_transform,SINK:custom_sink
```

Meaning:

- `plugin.id`: repository plugin id, should match PF4J `plugin.id`
- `plugin.version`: repository-visible plugin version
- `plugin.provider`: repository-visible provider
- `plugin.host-version-range`: current host compatibility marker
- `plugin.capabilities`: declared capabilities for conflict checking

Capability syntax:

- comma-separated
- format: `KIND:key`
- valid kinds:
  - `SOURCE`
  - `TRANSFORM`
  - `SINK`

Example:

```properties
plugin.capabilities=SOURCE:jdbc_extra,SINK:kafka_batch_v2
```

### 3. PF4J extension index

Path:

- `META-INF/extensions.idx`

Contents:

- one extension class per line

Example:

```text
com.example.plugin.MyTemplateV2Extension
```

## Required Extension Shape

The plugin should expose a PF4J extension implementing:

- `org.gensokyo.data.calcite.Pf4jTemplateV2RuntimeExtension`

The extension returns:

- `TemplateV2RuntimePluginProvider`

That provider returns:

- `TemplateV2RuntimePlugin`

The returned plugin should declare:

- its `TemplateV2RuntimePluginDescriptor`
- its source/transform/sink factories

## Minimal Example Shape

```java
@Extension
public class MyTemplateV2Extension implements Pf4jTemplateV2RuntimeExtension {
    @Override
    public TemplateV2RuntimePluginProvider provider() {
        return context -> new TemplateV2RuntimePlugin() {
            @Override
            public TemplateV2RuntimePluginDescriptor descriptor() {
                return TemplateV2RuntimePluginDescriptor.builder("my-plugin")
                        .version("1.0.0")
                        .hostVersionRange("current")
                        .provider("my-team")
                        .capability(TemplateV2PluginCapability.source("custom_source"))
                        .build();
            }

            @Override
            public List<V2SourceFactory> sourceFactories() {
                return List.of(new MySourceFactory());
            }
        };
    }
}
```

## Isolation Expectations

PF4J is now the preferred external plugin path because it provides plugin-level classloader boundaries.

What this means in practice:

- different plugins should not be loaded through one shared `URLClassLoader`
- plugin-private dependencies should stay plugin-scoped where possible
- plugin conflicts should be much less likely than under the old shared loader path

What this does not guarantee yet:

- full lifecycle-safe hot swap for in-flight tasks
- plugin dependency policy across all future plugin types
- remote plugin distribution governance

## Current Validation Rules

The host currently validates:

- duplicate plugin ids
- duplicate declared capabilities

A plugin will be rejected if:

- another loaded plugin declares the same `plugin.id`
- another loaded plugin declares the same capability key under the same capability kind

## Operational Notes

- plugin root comes from `pci.data.generator.v2-plugin-directories`
- current recommended framework is `PF4J`
- framework selection can be controlled by:
  - `pci.data.generator.v2-plugin-framework=PF4J`

## Current Status

The repository now has:

- PF4J-based external plugin wiring in service configuration
- repository plugin descriptor support
- runtime capability conflict validation
- tests proving PF4J path uses separate plugin classloaders while the old ServiceLoader path does not provide true plugin isolation
- tests proving PF4J plugins can contribute `SourceVO`, `TransformVO`, and `WriterVO` template subtypes into the shared parser path through plugin classloaders

## Repository Sample

A minimal external plugin skeleton is now available at:

- [`samples/template-v2-pf4j-plugin/README.md`](D:\Work\99_Code\data-generator\samples\template-v2-pf4j-plugin\README.md)

This sample currently focuses on:

- packaging structure
- PF4J extension shape
- repository descriptor contract
- one minimal runtime sink contribution

Template model subtype parsing is now validated through the PF4J path.

What remains unproven in this sample line is runtime execution of plugin-provided source/transform/sink factories inside a real V2 run.

## Related References

- [`docs/calcite-plugin-framework-evaluation.md`](D:\Work\99_Code\data-generator\docs\calcite-plugin-framework-evaluation.md)
- [`docs/calcite-implementation-status.md`](D:\Work\99_Code\data-generator\docs\calcite-implementation-status.md)
- [`docs/calcite-refactor-plan.md`](D:\Work\99_Code\data-generator\docs\calcite-refactor-plan.md)
