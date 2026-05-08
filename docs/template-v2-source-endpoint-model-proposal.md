# Template V2 Source Endpoint Model Proposal

## Purpose

This document proposes a concrete model direction for Template V2 source and endpoint definitions.

It turns the datasource governance and product roadmap decisions into a more implementation-oriented model proposal.

It answers six questions:

- how managed and inline endpoints should be represented in the template model
- how JDBC, Kafka, Elasticsearch, AI providers, and future plugin-defined families can share one endpoint shape
- how secret references should be represented
- how endpoint resolution should interact with runtime services and refresh
- what should remain family-specific versus shared
- how the proposal can be introduced without breaking the current V2 path all at once

Related references:

- `docs/template-v2-datasource-and-secret-governance.md`
- `docs/template-v2-policy-to-runtime-mapping-guide.md`
- `docs/template-v2-product-roadmap.md`
- `docs/calcite-templatev2-model-design.md`
- `docs/calcite-implementation-status.md`

## Goal

Template V2 should gradually evolve from connector-specific reference fields such as `dataSourceId` toward a more consistent endpoint model that supports:

- managed references
- inline endpoint definitions
- narrow override layers
- family-specific options where needed

The proposal should preserve the current path while creating a cleaner long-term model.

## Design Principles

- keep the first evolution additive where practical
- do not force all connector families into identical fields if their operational models differ materially
- still provide one common structural pattern across families
- keep secret-bearing values explicit and reference-friendly
- keep endpoint identity visible in explain, preview, and run reports
- let plugin-defined families participate through declared metadata instead of hard-coded branches

## Current Baseline

Today the repository uses connector-specific fields in many places.

Examples:

- `dataSourceId` for JDBC-style resolution
- Kafka runtime service resolution
- Elasticsearch runtime service resolution
- AI provider-specific configuration under source models

This works for the current phase, but the long-term issues are:

- endpoint ownership rules are inconsistent across families
- inline versus managed configuration is not described through one shared structure
- secret-bearing properties are family-specific and harder to validate consistently

## Proposed Endpoint Ownership Modes

The model should support three explicit ownership modes.

### Mode 1. Managed reference

The template points to a centrally managed endpoint.

### Mode 2. Inline endpoint

The template carries the endpoint definition directly.

### Mode 3. Managed reference with explicit override

The template references a managed endpoint and applies a narrow set of allowed overrides.

## Proposed Shared Endpoint Concepts

Not every endpoint family needs the same fields, but they should share the same conceptual structure.

Recommended shared concepts:

- endpoint family
- endpoint reference
- inline endpoint definition
- optional override block
- secret reference fields
- endpoint options map for family-specific non-core properties

## Recommended Model Direction

### Option A. Keep family-specific fields only

Strengths:

- lowest short-term change cost

Weaknesses:

- hard to scale governance and refresh consistently
- keeps policy and validation scattered

Recommendation:

- not ideal as the final direction

### Option B. Introduce one neutral endpoint block gradually

Strengths:

- clearer long-term structure
- easier shared validation and control-plane reporting
- easier plugin-family participation

Weaknesses:

- requires transitional coexistence with old fields

Recommendation:

- preferred direction

## Proposed Core Model Shapes

### Shared endpoint reference block

Illustrative direction:

```java
public class EndpointRefVO implements Serializable {
    private String family;
    private String ref;
    private Map<String, Object> overrides;
}
```

Intent:

- `family` identifies the endpoint family such as `jdbc`, `kafka`, `elasticsearch`, `ai`
- `ref` identifies the managed endpoint
- `overrides` is intentionally narrow and policy-controlled

### Shared inline endpoint block

Illustrative direction:

```java
public class EndpointVO implements Serializable {
    private String family;
    private Map<String, Object> properties;
    private Map<String, String> secretRefs;
}
```

Intent:

- `properties` holds non-secret endpoint properties
- `secretRefs` carries secret indirection by logical property name

### Shared endpoint selection wrapper

Illustrative direction:

```java
public class EndpointBindingVO implements Serializable {
    private EndpointRefVO managed;
    private EndpointVO inline;
}
```

Validation rule:

- exactly one of `managed` or `inline` should be present in the baseline proposal
- a later revision may allow `managed + overrides` through the managed block

## Recommended Source Model Integration

The first adoption target should be source models, not every sink family at once.

### Query source direction

Illustrative direction:

```java
public class QuerySourceVO extends SourceVO {
    private EndpointBindingVO endpoint;
    private String sql;
    private List<ParamVO> params;
    private RowSchema schema;
    private SourcePolicyVO policy;
}
```

Transitional rule:

- if `dataSourceId` exists and `endpoint` does not exist, normalize into `endpoint.managed`

### AI source direction

Illustrative direction:

```java
public class AiSourceVO extends SourceVO {
    private EndpointBindingVO endpoint;
    private AiProviderVO provider;
    private String prompt;
    private String parser;
    private RowSchema schema;
    private SourcePolicyVO policy;
}
```

Note:

- provider-specific model data may still exist, but transport and auth endpoint identity should converge on the endpoint model over time

## Family-Specific Endpoint Property Guidance

The shared model should not erase family-specific semantics.

### JDBC family

Likely inline properties:

- `url`
- `driverClassName`
- `database`
- `readOnly`
- `fetchSize`

Likely secret refs:

- `username`
- `password`

### Kafka family

Likely inline properties:

- `bootstrapServers`
- `securityProtocol`
- `clientId`

Likely secret refs:

- `saslUsername`
- `saslPassword`
- SSL keystore or truststore credentials where relevant

### Elasticsearch family

Likely inline properties:

- `uris`
- `cloudId`
- `ssl`

Likely secret refs:

- `username`
- `password`
- `apiKey`

### AI family

Likely inline properties:

- `baseUrl`
- `providerType`
- `model`

Likely secret refs:

- `apiKey`
- provider auth token fields

## Overrides Model Guidance

Overrides should stay narrow and intentional.

Allowed override examples:

- query timeout
- fetch size
- model selection
- non-sensitive producer or parser settings

Avoid allowing overrides for:

- transport identity
- secret-bearing auth fields
- low-level security configuration that should stay in the managed endpoint

## Runtime Resolution Proposal

The model should resolve into one runtime endpoint snapshot before execution.

Recommended resolution flow:

1. normalize old connector-specific fields into the new binding shape where applicable
2. validate family, policy, and secret usage
3. resolve managed reference or inline endpoint into a runtime snapshot
4. apply allowed overrides
5. hand the resolved snapshot to the relevant runtime service or factory

## Plugin Participation Proposal

Plugin-defined families should not require core code changes for every new endpoint shape.

Recommended plugin metadata responsibilities:

- declare endpoint family id
- declare supported properties
- declare secret-bearing property names
- declare validation rules
- declare whether managed references, inline endpoints, or both are supported

## Control-Plane Visibility Proposal

Explain and run reports should surface endpoint identity consistently.

Recommended visible fields:

- endpoint family
- endpoint mode: managed or inline
- managed reference id where applicable
- redacted summary of inline endpoint identity
- applied override keys

Never expose:

- resolved secret values
- low-level credential contents

## Validation Rules

Baseline validation rules:

- exactly one endpoint mode present unless hybrid override is explicitly supported
- endpoint family known or plugin-resolved
- secret refs valid for the chosen family
- forbidden inline use rejected by governance policy
- forbidden override keys rejected by policy

## Backward Compatibility Strategy

The proposal should be introduced gradually.

### Phase E0. Document and normalize

- keep existing connector-specific fields
- add normalization logic into an internal endpoint binding model

### Phase E1. Add new endpoint fields to selected source families

- start with `QuerySourceVO`
- optionally add `AiSourceVO` next

### Phase E2. Expand to sink families

- align Kafka, Elasticsearch, and JDBC sink endpoint handling

### Phase E3. Formalize plugin-family support

- allow plugin-defined endpoint families through declared metadata

## Recommended First Implementation Slice

The first practical implementation slice should be:

- internal `EndpointBinding` normalization for query sources
- governance validation over normalized shape
- explain/report visibility for managed versus inline endpoint usage

This gets immediate value without forcing a large external model rewrite all at once.

## Open Questions

These questions should be settled during implementation design:

- whether `family` should always be explicit or inferred from the source/sink type in some paths
- whether `secretRefs` should remain a flat map or become typed subfields for common families
- whether endpoint overrides deserve a typed model per family instead of a generic map
- whether sink endpoint convergence should happen together with source convergence or one family at a time

## Acceptance Criteria

The proposal is useful when:

- implementers can see a clean path from `dataSourceId`-style fields to a normalized endpoint model
- governance rules can be expressed against one common structure
- explain and run reports can show endpoint identity consistently
- plugin-defined endpoint families have a clear participation model

## Non-Goals

This proposal should avoid:

- forcing every connector into one overly rigid endpoint schema immediately
- removing current connector-specific fields before a migration path exists
- solving every sink family in the first implementation slice
