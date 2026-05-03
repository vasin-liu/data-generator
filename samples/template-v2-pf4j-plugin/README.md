# Template V2 PF4J External Plugin Sample

## Goal

Provide a minimal repository-local skeleton for a Template V2 external plugin when `PF4J` is used as the external plugin framework.

This sample is intentionally small. It demonstrates:

- the current external plugin jar structure
- the PF4J extension shape
- the repository runtime plugin descriptor contract
- one minimal `V2SinkFactory` contribution
- one minimal SQL UDF contribution

## Important Current Boundary

As of the current V2 host/runtime implementation:

- PF4J external plugins are class-isolated at the plugin classloader level
- PF4J external plugins can contribute runtime `source/transform/sink` factories and SQL UDFs
- the service-side YAML/JSON parser does **not** yet hot-load new `SourceVO` / `TransformVO` / `WriterVO` model subtypes from PF4J plugins

That means this sample is currently best treated as:

- a packaging and extension skeleton for external runtime plugins
- a reference for future pluginization work

It is **not yet** a complete end-to-end example for introducing a brand-new template model subtype purely through plugin deployment.

## Directory Layout

```text
samples/template-v2-pf4j-plugin
├─ pom.xml
└─ src
   └─ main
      ├─ java
      │  └─ org/example/datagenerator/plugin/sample
      │     ├─ SampleLoggingSinkFactory.java
      │     └─ SampleTemplateV2Extension.java
      └─ resources
         ├─ META-INF
         │  ├─ data-generator
         │  │  └─ template-v2-plugin.properties
         │  └─ extensions.idx
         └─ plugin.properties
```

## What The Sample Contributes

The sample contributes one sink capability and one SQL transform/UDF capability:

- `SINK:sample_logging`
- `TRANSFORM:sql`

Its runtime behavior is intentionally simple:

- accept writers whose `type` is `sample_logging`
- print schema and row payloads to standard output
- expose `V2_SAMPLE_WRAP(left, value)` as a SQL function that returns `left + value + left`

This keeps the sample focused on the plugin contract itself rather than business behavior.

## Build

The sample is not part of the main reactor on purpose.

Build it separately after the main repository artifacts are installed locally:

```powershell
.\mvnw-jdk25.ps1 -DskipTests install
.\mvnw-jdk25.ps1 -f samples\template-v2-pf4j-plugin\pom.xml -DskipTests package
```

Output jar:

- `samples/template-v2-pf4j-plugin/target/template-v2-pf4j-plugin-sample-1.0.0-SNAPSHOT.jar`

## Deploy

Copy the built jar into one configured plugin directory:

```properties
pci.data.generator.v2-plugin-framework=PF4J
pci.data.generator.v2-plugin-directories[0]=plugins
```

Then place the jar under:

```text
plugins/
  template-v2-pf4j-plugin-sample-1.0.0-SNAPSHOT.jar
```

## Current Next Step

To make external plugins fully end-to-end for brand-new template node types, the next architecture step is:

- add a plugin-aware model subtype registration path for `SourceVO`, `TransformVO`, and later `WriterVO`
- decide whether that registration stays on `ServiceLoader` or converges on the same PF4J plugin lifecycle

Until that lands, PF4J external plugins should be viewed as:

- runtime capability extensions first
- full template-model extensions later
