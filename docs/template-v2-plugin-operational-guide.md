# Template V2 Plugin Operational Guide

## Purpose

This document defines the operational guide for Template V2 plugins.

The repository already treats PF4J as the preferred external plugin lifecycle and classloading path. This guide turns that architectural direction into an operational model.

It answers six questions:

- how plugins should be packaged and introduced into the runtime
- how install, refresh, disable, rollback, and removal should work operationally
- what diagnostics operators should expect when plugin loading fails
- how plugin isolation, capability collisions, and refresh boundaries should be understood
- what minimum runbook should exist for support and production use
- what is still intentionally out of scope for the current plugin model

Related references:

- `docs/calcite-plugin-framework-evaluation.md`
- `docs/calcite-pf4j-plugin-packaging.md`
- `docs/template-v2-product-roadmap.md`
- `docs/template-v2-transformer-strategy.md`
- `docs/template-v2-datasource-and-secret-governance.md`
- `docs/template-v2-control-plane-requirements.md`
- `docs/calcite-implementation-status.md`

## Goal

The plugin path should be:

- class-isolated
- refreshable
- diagnosable
- governable
- safe for built-in and external capabilities to coexist

The guide is intentionally operational. It is about how the plugin runtime should be used and supported, not only how extension interfaces are designed.

## Plugin Scope

Plugins may contribute:

- source subtypes and source factories
- transformer subtypes and transform factories
- sink or writer subtypes and sink factories
- SQL UDFs
- future endpoint-family metadata where governance rules allow it

Plugins should not be treated as a shortcut for bypassing product governance.

## Core Operating Principles

- PF4J is the preferred external plugin loading and classloader isolation path
- built-in repository capabilities remain first-class and share the same host registry contract
- plugin refresh affects only later runs; in-flight runs keep the snapshot captured at run start
- capability collisions should fail fast or surface clear diagnostics rather than produce silent ambiguity
- plugin state should be visible in operational reports and error messages

## Plugin Lifecycle Model

Recommended operational lifecycle:

- `DISCOVERED`
- `LOADED`
- `RESOLVED`
- `STARTED`
- `DISABLED`
- `FAILED`
- `STOPPED`

Host-side interpretation:

- only started plugins should contribute active runtime capabilities
- failed plugins should not poison the last known good active registry
- disabled plugins should remain visible for diagnosis and rollback planning

## Packaging Requirements

Minimum expectations:

- plugin artifact packaged according to the PF4J-backed path used by the repository
- plugin metadata includes identity, version, host compatibility, and capability declarations
- plugin contains only the extension classes and dependencies it is expected to own

Recommended metadata fields:

- plugin id
- plugin version
- supported host version range
- declared capability kinds such as source, transformer, sink, UDF
- optional human-readable description

## Installation Flow

Recommended operational flow:

### Step 1. Intake

- operator or administrator obtains the plugin artifact
- artifact source and trust are verified according to environment policy

### Step 2. Placement

- plugin artifact is placed in the configured plugin root
- placement event is auditable

### Step 3. Discovery and load

- plugin manager discovers the artifact
- host validates metadata and compatibility
- plugin is loaded and started if valid

### Step 4. Registry rebuild

- host rebuilds subtype registry and runtime registry
- if rebuild succeeds, new capabilities become visible to later runs
- if rebuild fails, preserve last known good registry and surface diagnostics

## Refresh Flow

Refresh should be explicit and observable.

Recommended refresh sequence:

1. detect plugin directory change or receive explicit refresh request
2. stop and unload changed plugin artifacts as needed
3. reload plugin artifacts
4. rebuild subtype and runtime registries
5. activate the new registry only if the rebuild succeeds

Refresh rule:

- current in-flight runs keep the old snapshot
- only later runs see the refreshed plugin set

## Disable And Rollback Flow

### Disable

Use when:

- plugin is known bad
- plugin should remain installed but inactive

Expected behavior:

- plugin no longer contributes capabilities to new runs
- prior runs remain historically attributable to the plugin version they used

### Rollback

Use when:

- a newer plugin version introduced a regression

Expected behavior:

- disable or remove the new plugin artifact
- restore the prior known-good artifact
- refresh the registry
- confirm new runs bind to the restored plugin version

## Removal Flow

Removal should be treated as a controlled change.

Requirements:

- removal action must be auditable
- host should surface which template types or capability kinds may be affected
- removal should not invalidate historical run reports

## Operational Diagnostics

Plugin diagnostics should be grouped by failure phase.

### Discovery failures

Examples:

- plugin artifact not readable
- invalid location or file shape

### Metadata failures

Examples:

- missing plugin id
- unsupported host version range
- malformed declared capabilities

### Classloading failures

Examples:

- missing dependency
- linkage error
- class initialization failure

### Extension resolution failures

Examples:

- provider returns null
- expected extension class missing
- subtype registry registration failure

### Registry rebuild failures

Examples:

- duplicate subtype names
- duplicate capability collisions
- invalid factory contribution

### Runtime execution failures

Examples:

- plugin source/transformer/sink throws during execution
- plugin UDF evaluation failure

## Minimum Diagnostic Fields

Each important plugin diagnostic should include:

- plugin id
- plugin version
- capability kind
- extension class or factory class
- lifecycle phase
- host action such as load, refresh, validate, or run

## Capability Collision Policy

Capability collisions should be explicit.

Collision examples:

- duplicate subtype names
- duplicate transform or source type identity
- duplicate UDF names
- ambiguous capability claims across multiple plugins

Recommended policy:

- fail fast on collisions that make runtime resolution ambiguous
- include both plugin ids and both classes in diagnostics
- keep the last known good registry active if collision is introduced during refresh

## Isolation Expectations

Operationally, the plugin model should be treated as class-isolated.

Meaning:

- one plugin should not rely on another plugin's accidental classes unless dependency rules explicitly allow it
- plugin-level diagnostics should preserve attribution to the owning plugin
- the old shared ServiceLoader fallback path should not be treated as equivalent in isolation guarantees

## Control-Plane Integration

The control plane should expose plugin-aware visibility.

Recommended outputs:

- plugin list with state and version
- plugin capability inventory
- plugin snapshot included in run metadata
- plugin-aware validation and explain diagnostics
- refresh result report

## Datasource And Secret Governance Integration

Plugins must not bypass endpoint governance.

Requirements:

- plugin-defined endpoint families should declare secret-bearing fields
- plugin-defined connectors should participate in policy validation where applicable
- plugin failures in endpoint validation or secret resolution should surface as plugin-attributed diagnostics

## Support Runbook

The product should eventually provide a minimal operator runbook.

Recommended procedures:

### Procedure P1. Install a new plugin

- verify artifact source
- place artifact
- trigger or wait for refresh
- inspect plugin state
- run validate or explain on a representative template

### Procedure P2. Diagnose plugin load failure

- inspect lifecycle phase of failure
- inspect plugin id, version, and failing class
- inspect collision or missing dependency diagnostics
- verify last known good registry is still active

### Procedure P3. Roll back plugin regression

- disable or remove faulty plugin
- restore last known good artifact
- refresh plugin registry
- verify representative template execution

### Procedure P4. Investigate plugin runtime failure

- locate affected run id
- inspect plugin snapshot, extension class, and stage kind
- confirm whether failure is deterministic or data-dependent

## Recommended Delivery Plan

### Phase P0. Formalize operational vocabulary

- define plugin states and failure phases
- define install, refresh, disable, rollback, and remove semantics

### Phase P1. Improve operational reporting

- expose plugin state and refresh results
- surface plugin-aware diagnostics in validation and run reports

### Phase P2. Harden rollback and collision support

- ensure last known good registry behavior is stable
- improve duplicate subtype and capability diagnostics

### Phase P3. Expand operator guidance

- document runbooks
- document support expectations for plugin-defined endpoint families and custom transformers

## Acceptance Criteria

The operational guide is useful when:

- operators can describe how a plugin becomes active for new runs
- refresh boundaries are clear and safe
- collisions and load failures are diagnosable
- rollback is explicit instead of improvised
- plugin capabilities are visible in runtime and control-plane reporting

## Non-Goals

The near-term plugin operational model should avoid:

- treating the old shared-classloader fallback path as operationally equivalent to PF4J
- allowing plugins to bypass validation, secret policy, or audit
- assuming plugins can be refreshed mid-run without snapshot boundaries
- promising a full plugin marketplace before the current operational model is stable
