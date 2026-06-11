# Calcite / Template V2 Plugin Framework Evaluation

## Goal

Evaluate whether the current Template V2 plugin mechanism should:

- stay on the current lightweight custom path
- migrate fully to PF4J
- or adopt a hybrid model

This evaluation is for the V2 runtime only:

- `source`
- `transformer`
- `sink`
- runtime service extension
- external plugin discovery and hot reload

It is not a general repository-wide plugin policy.

## Current In-Repo Design

Current V2 runtime extension path:

- `TemplateV2RuntimePlugin`
- `TemplateV2RuntimePluginProvider`
- `TemplateV2RuntimeRegistryFactory`
- `TemplateV2RuntimeRegistryProvider`
- `RefreshableTemplateV2RuntimeRegistryProvider`
- `DirectoryAwareTemplateV2RuntimePluginProvider`
- `TemplateV2RuntimeWatcher`
- `TemplateV2RuntimeContext`
- `TemplateV2RuntimeServices`

Current behavior:

- Spring beans can contribute `V2SourceFactory`, `V2TransformFactory`, `V2SinkFactory`
- built-in runtime providers can contribute JDBC / Kafka / Elasticsearch capabilities
- plugin directories can be scanned for external jars
- external jars are loaded with `URLClassLoader`
- plugin implementations are discovered with `ServiceLoader`
- file change events on plugin directories trigger registry refresh

This means the repository already has a usable extension model for V2, but only a minimal external plugin runtime.

## What The Current Design Already Gets Right

The current custom design is already aligned with the business/runtime shape:

- extension surface is V2-native, not framework-native
- runtime context is explicit
- runtime services are explicit
- registry refresh is already part of the contract
- internal Spring-provided factories and future external factories can share one abstraction

These are important advantages. They should not be thrown away lightly.

In other words, the current problem is not the extension API itself. The weaker part is the external plugin packaging, lifecycle, isolation, and operational discipline.

## Current Gaps

Compared with a mature plugin runtime, the current custom layer still has these gaps:

### 1. Plugin metadata is weak

There is no first-class plugin descriptor for:

- plugin id
- version
- provider
- dependency graph
- minimum host version
- plugin capabilities

Today the host mainly discovers classes, not plugins as managed units.

### 2. Lifecycle is weak

There is no formal lifecycle model for:

- load
- resolve
- start
- stop
- disable
- unload

Current behavior is effectively "scan jars and rebuild registry".

### 3. Classloading is too coarse

Current external jars under plugin directories are placed into one `URLClassLoader`.

That is simple, but it means:

- weak isolation between plugins
- higher risk of dependency conflicts
- no plugin-level dependency graph
- no clean answer for unloading a single plugin

### 4. Hot reload is only registry refresh

Current watcher refreshes the entire runtime registry when a jar changes.

That is acceptable for early V2, but long term it is weaker than:

- plugin-level load/unload
- failure isolation
- disabled plugin retention
- operational visibility for plugin state

### 5. Packaging contract is under-specified

External authors do not yet have a strong contract for:

- how to package a plugin
- how to declare plugin metadata
- how to depend on host APIs
- how to evolve host/plugin compatibility safely

## PF4J Assessment

PF4J is a lightweight Java plugin framework focused on runtime plugin loading and extension discovery.

Relevant strengths from official PF4J documentation:

- plugins are managed as first-class units, not only discovered classes
- plugins have lifecycle states such as `CREATED`, `RESOLVED`, `STARTED`, `STOPPED`, `DISABLED`
- only started plugins contribute extensions
- each plugin normally gets its own `PluginClassLoader`
- plugin dependency and host-version compatibility can be declared in plugin metadata
- plugin manager APIs support load, start, stop, disable, enable, and delete style operations

For this repository, the practical value is:

- stronger external plugin isolation
- better plugin packaging discipline
- cleaner hot-load / hot-unload path
- clearer future support for remote distribution or plugin marketplace style delivery

## PF4J Strengths Mapped To V2 Needs

### Good fit

PF4J is a strong fit for:

- external `source` / `transformer` / `sink` jars
- plugin metadata and version governance
- plugin-level classloader isolation
- future plugin dependency management
- controlled hot load / stop / disable workflows

### Especially good for future goals

It fits the user's stated long-term direction well:

- pluginizable `source`, `transformer`, `sink`
- hot-loadable execution nodes
- dynamic datasource-aware runtime nodes
- AI / Kafka / Elasticsearch / JDBC all contributing through the same runtime model

If the repository eventually wants third parties to ship V2 runtime nodes independently, PF4J is materially stronger than the current `ServiceLoader + URLClassLoader` approach.

## PF4J Weaknesses / Costs

PF4J is not free. Adopting it introduces:

### 1. Additional host complexity

The repository would need:

- a plugin manager bean
- plugin root configuration
- plugin packaging conventions
- lifecycle integration with Spring Boot startup and shutdown
- mapping from PF4J extension instances into current V2 runtime registry

### 2. Spring integration complexity

The repository is Spring Boot 4 based. PF4J can integrate with Spring through `pf4j-spring`, but that is still another abstraction boundary to own and test.

For V2, the runtime already has its own registry/provider/context abstraction. Replacing that abstraction with PF4J-native concepts would likely be unnecessary churn.

### 3. Reload semantics still need repository-specific work

PF4J helps with plugin lifecycle, but the repository still needs to decide:

- how datasource/service handles are injected
- whether plugin factories are stateless or stateful
- what happens to in-flight tasks on plugin refresh
- whether sink/source instances are per-execution or singleton-like

PF4J improves the infrastructure layer, but it does not eliminate business/runtime design work.

### 4. Full migration now would slow current feature delivery

The current V2 path still needs:

- real Kafka sink/provider implementation
- real Elasticsearch sink/provider implementation
- source policy execution semantics
- AI source execution path
- multi-sink failure strategy

A full PF4J rewrite before those capabilities exist would likely slow the core migration path.

## Spring Plugin Assessment

Spring Plugin is useful for selecting and organizing plugins already present in the Spring application context.

It is weaker for this use case because it does not target:

- plugin jar packaging as first-class runtime units
- independent plugin classloaders
- runtime install/uninstall
- external jar hot deployment

It is a reasonable helper for in-container extension selection, but not a good answer for the repository's long-term external plugin and hot-load goals.

Conclusion:

- stronger than ad hoc lists for in-Spring selection
- weaker than PF4J for external runtime plugins
- not enough by itself for the target V2 architecture

## Full Options

### Option A. Keep the current custom runtime only

Pros:

- fastest short-term delivery
- no migration cost
- preserves current abstractions
- enough for internal/runtime-local extension

Cons:

- plugin packaging remains weak
- classloading isolation remains weak
- hot reload remains coarse
- future third-party plugin support will need rework

Best use:

- short-term continuation while V2 still lacks major built-in capabilities

### Option B. Replace the whole plugin model with PF4J now

Pros:

- one formal plugin runtime
- stronger external plugin lifecycle from the start
- better packaging and isolation

Cons:

- unnecessary churn in already-good V2 abstractions
- higher delivery risk now
- likely slows Kafka / Elasticsearch / AI / multi-sink work

Best use:

- only if external plugin delivery is immediately more important than completing V2 core features

### Option C. Hybrid model

Keep current V2 runtime abstractions as the host contract:

- `TemplateV2RuntimePlugin`
- `TemplateV2RuntimePluginProvider`
- `TemplateV2RuntimeContext`
- `TemplateV2RuntimeServices`
- `TemplateV2RuntimeRegistryProvider`

But allow the external plugin discovery/lifecycle layer to evolve from:

- current `ServiceLoader + URLClassLoader + WatchService`

to:

- PF4J-backed plugin manager and extension discovery

Pros:

- preserves the right business/runtime abstraction
- limits migration blast radius
- enables phased adoption
- lets built-in Spring beans and external plugins converge on one host API

Cons:

- two conceptual layers instead of one
- requires adapter code between PF4J and current runtime provider model

Best use:

- this repository's current situation

## Recommendation

The recommended direction is Option C: hybrid.

### Current decision

Do not replace the current V2 runtime abstraction layer.

Keep and continue investing in:

- `TemplateV2RuntimeContext`
- `TemplateV2RuntimeServices`
- `TemplateV2RuntimePluginProvider`
- `TemplateV2RuntimeRegistryProvider`

These types are already correctly shaped around the repository's execution model.

At the same time, the repository should now treat PF4J as the primary external plugin path.

Reason:

- real isolation validation now shows the old `ServiceLoader + shared URLClassLoader` path does not provide true plugin isolation
- the old path also exposes descriptor/resource attribution problems under multiple plugins
- PF4J path has now been wired into the service runtime and validated as the stronger external loading mode

### Runtime loading decision

For external plugins, the preferred runtime loading mechanism should now be:

- `Pf4jTemplateV2RuntimePluginProvider`

The old path:

- `DirectoryAwareTemplateV2RuntimePluginProvider`

should be treated as fallback or temporary compatibility mode, not the long-term default.

### Why this is the best tradeoff

Because the repository now has enough evidence to avoid keeping the old external loader as default, while still preserving the right host abstractions:

- Kafka V2 sink/provider is still not real
- Elasticsearch V2 sink/provider is still not real
- source policy is not executed yet
- AI source is not executed yet
- sink failure strategies are incomplete

The current custom abstraction is still good enough to finish these. PF4J should carry the external plugin lifecycle and isolation concerns.

## Proposed Host Contract For Future PF4J Adoption

If PF4J is introduced later, keep the extension boundary V2-native.

Recommended contract shape:

- PF4J extension point returns one or more `TemplateV2RuntimePluginProvider`
- provider receives `TemplateV2RuntimeContext`
- provider contributes `V2SourceFactory`, `V2TransformFactory`, `V2SinkFactory`
- host still composes one `TemplateV2RuntimeRegistry`

This preserves:

- one runtime registry model
- one execution path
- one service injection model

and avoids leaking PF4J types into most V2 runtime code.

## Recommended Next Implementation Steps

### Phase P1. Stabilize the current custom layer

- implement real Kafka sink/provider on current runtime abstractions
- implement real Elasticsearch sink/provider on current runtime abstractions
- complete sink failure policy wiring
- complete source policy wiring
- define plugin capability naming and collision rules

### Phase P2. Add stronger plugin metadata without PF4J first

Before full PF4J adoption, the current custom plugin path can be strengthened with a small descriptor contract:

- plugin id
- version
- host version range
- declared capabilities

This gives immediate governance value even if the loader is still custom.

### Phase P3. Introduce PF4J adapter behind the current provider contract

- add PF4J dependencies
- create a plugin manager wrapper bean
- add PF4J-backed provider that exports V2 runtime plugins
- keep the current directory watcher only as a trigger, not as the plugin loader itself

### Phase P4. Retire the coarse `URLClassLoader` path

Once PF4J path is proven:

- deprecate direct `ServiceLoader + URLClassLoader` external loading
- keep plain Spring/service-loaded built-ins for internal providers
- reserve PF4J for external runtime plugins

## Final Conclusion

The current custom V2 plugin abstraction should stay.

The current custom external plugin loader should probably not be the final answer.

PF4J is the strongest candidate for the external plugin lifecycle layer, but adopting it now as a full replacement would be premature.

The pragmatic path is:

1. finish V2 runtime capabilities on the current abstraction
2. keep strengthening plugin metadata and capability contracts
3. use PF4J as the default external plugin loading/lifecycle adapter
4. keep the coarse directory `URLClassLoader` path only as fallback until it can be retired

## References

- PF4J overview: https://pf4j.org/
- PF4J getting started: https://pf4j.org/doc/getting-started.html
- PF4J plugin lifecycle: https://pf4j.org/doc/plugin-lifecycle.html
- PF4J extensions: https://pf4j.org/doc/extensions.html
- PF4J class loading: https://pf4j.org/doc/class-loading.html
- PF4J Spring integration repository: https://github.com/pf4j/pf4j-spring
- Spring Plugin project page: https://spring.io/projects/spring-plugin
- Spring Plugin repository: https://github.com/spring-projects/spring-plugin
