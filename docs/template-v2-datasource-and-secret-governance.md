# Template V2 Datasource And Secret Governance

## Purpose

This document defines how Template V2 should manage datasource-style runtime endpoints, inline connection definitions, secrets, and hot reload behavior.

It turns the current "Seatunnel-style source direction" into a concrete governance plan.

It answers six questions:

- when templates may reference managed connections versus define inline connections
- how JDBC, Kafka, Elasticsearch, AI providers, and future plugin endpoints should fit one governance model
- how secrets should be represented and resolved
- how hot load and refresh should behave
- how validation, audit, and policy should work
- how this governance layer should evolve without blocking the current V2 runtime

Related references:

- `docs/calcite-refactor-plan.md`
- `docs/calcite-implementation-status.md`
- `docs/template-v2-product-roadmap.md`
- `docs/template-v2-transformer-strategy.md`
- `docs/calcite-plugin-framework-evaluation.md`
- `docs/jdbc-resolver-ownership.md` — JDBC resolver ownership (catalog vs execute-path)

## Goal

Template V2 should support both:

- centrally managed reusable connections
- template-owned inline endpoint definitions where policy permits

The final product should make this safe, observable, and hot-reloadable without forcing every runtime endpoint to be pre-wired as a Spring bean.

## Design Principles

- support Seatunnel-style inline source configuration where it improves template portability
- keep centrally managed connections for shared infrastructure and stronger governance
- do not treat JDBC as the only connection family; Kafka, Elasticsearch, AI providers, and plugin-defined endpoints need the same governance pattern
- do not store long-lived production secrets as plain text in template payloads
- use snapshot-based runtime refresh so in-flight tasks do not see half-applied connection changes
- prefer one governance model with family-specific adapters over separate ad hoc policies per connector

## Governance Scope

This document covers runtime endpoint families that can require connection, authentication, or shared client configuration.

In scope:

- JDBC query sources
- JDBC sinks
- Kafka sinks and future Kafka sources
- Elasticsearch sinks and future Elasticsearch sources
- AI provider endpoints
- future plugin-provided source / sink / provider endpoints

Not in scope:

- local file paths such as CSV / JSON / Excel inputs unless a future remote file connector is introduced
- low-level Spring bean lifecycle details
- cluster sizing and infrastructure capacity management outside the product

## Connection Ownership Modes

Template V2 should support three governance modes.

### 1. Managed connection mode

The template references a centrally managed connection object.

Examples:

- `dataSourceId: customerDb`
- `connectionRef: kafka-prod`
- `providerRef: ollama-shared`

Strengths:

- strongest governance
- centralized credential rotation
- shared health visibility
- easier audit and permissions

Best for:

- production shared infrastructure
- sensitive environments
- high-reuse endpoints

### 2. Inline connection mode

The template carries its own endpoint definition.

Examples:

- JDBC URL, username, secret reference, and driver hints directly in the source
- Kafka bootstrap servers and auth directly in the sink
- Elasticsearch cluster config directly in the sink
- AI provider host/model/auth directly in the source

Strengths:

- high portability
- close to Seatunnel-style authoring
- easier self-contained templates

Risks:

- weaker governance if uncontrolled
- secret leakage risk if plain text is allowed
- duplication of shared endpoint configuration

Best for:

- development and isolated integration cases
- project-owned endpoints
- migration and experimentation

### 3. Hybrid mode

The template references a managed connection and applies a limited override layer.

Examples:

- managed JDBC connection plus template-local query timeout
- managed Kafka cluster plus template-local topic and headers
- managed AI provider plus template-local model or parser settings

Strengths:

- shared governance with local flexibility

Risks:

- can become confusing if overrides are too broad

Recommendation:

- support only narrow, explicit overrides
- do not allow template-local override of every transport and security property

## Recommended Product Policy Modes

The product should expose environment-level governance policy.

### Policy A. Managed only

- templates may reference managed connections only
- inline endpoints are rejected

Best for:

- strict production environments

### Policy B. Hybrid default

- managed connections preferred
- inline endpoints allowed only for approved families or workspaces
- secret references required for sensitive fields

Best for:

- most enterprise deployments

### Policy C. Inline allowed

- inline endpoints allowed broadly
- still audited and validated

Best for:

- development labs
- embedded or standalone deployments

Recommended default:

- `Hybrid default`

## Recommended Endpoint Model Direction

The product should gradually evolve toward a family-neutral endpoint model.

Short-term compatibility:

- keep current connector-local fields such as `dataSourceId` where they already exist

Medium-term direction:

- introduce a shared reference concept such as `connectionRef` or `endpointRef`
- allow connector-local inline endpoint blocks where needed

Illustrative direction:

```yaml
sources:
  orders:
    type: query
    connectionRef: customer-db
    sql: "select * from orders"

  orders_inline:
    type: query
    endpoint:
      family: jdbc
      url: jdbc:mysql://localhost:3306/demo
      username: demo
      passwordSecretRef: secrets/db/demo
    sql: "select * from orders"
```

The same idea should apply to Kafka, Elasticsearch, and AI providers.

## Secret Model

Secrets should not be represented as one ad hoc field per connector forever.

### Supported secret input forms

Recommended forms:

- `secretRef`
- environment-backed indirection
- temporary plain text for local development only where policy allows it

Examples:

```yaml
endpoint:
  family: jdbc
  url: jdbc:postgresql://localhost:5432/demo
  username: app
  passwordSecretRef: secrets/db/app
```

```yaml
provider:
  type: OPENAI
  apiKeySecretRef: secrets/ai/openai
```

### Secret policy rules

- production policy should reject plain-text secrets by default
- secret references must be auditable and resolvable before runtime
- secret resolution failures should fail validation or preflight, not only task execution
- resolved secrets must never be logged or returned in preview APIs

## Hot Load And Refresh Model

Hot load is required for the future V2 direction, but it must be safe.

### Refresh sources

Potential refresh triggers:

- managed connection updates
- secret rotation
- plugin install or refresh
- template update with inline endpoint changes

### Runtime rule

- an in-flight run keeps the connection and plugin snapshot captured at run start
- refresh affects only later runs

This matches the current registry snapshot direction already used elsewhere in V2.

### Failure rule

- if a refresh build fails, keep the last known good registry
- surface actionable diagnostics with endpoint family, reference, and failing phase

## Validation Requirements

Connection validation should exist at more than one level.

### Structural validation

- connector family supported
- required connection fields present
- illegal combinations of `connectionRef` and inline endpoint rejected unless explicitly supported
- secret reference format valid

### Policy validation

- environment policy allows inline endpoints or overrides
- connector family allowed in the current workspace or environment
- secret handling policy satisfied

### Reachability validation

- optional preflight connection test where the family supports it
- health or credential failures surfaced with family-specific diagnostics

### Security validation

- plain-text secret blocked where policy forbids it
- unsafe override fields blocked in hybrid mode

## Audit And Permissions

Datasource governance is also an authorization problem.

Minimum requirements:

- permission to create or update managed connections
- permission to reference a managed connection in a template
- permission to define inline endpoints where policy allows it
- audit trail for connection changes, secret reference changes, and refresh actions
- run logs should record which endpoint reference or inline definition snapshot was used

## Family-Specific Guidance

### JDBC

Requirements:

- support managed and inline endpoint modes
- keep dynamic datasource resolution independent of compile-time Spring bean wiring
- allow family-specific options such as driver hints, fetch size, or query timeout through explicit fields or namespaced options

### Kafka

Requirements:

- support cluster-level managed connection plus template-local topic and producer behavior
- allow inline cluster definition where policy allows it
- keep auth, serializer, and security properties under governance

### Elasticsearch

Requirements:

- support managed and inline cluster definitions
- treat auth and SSL material as secret-backed config
- allow template-local target index, routing, and write semantics separately from endpoint governance

### AI providers

Requirements:

- support managed provider profiles
- allow template-local prompt and parser choices
- keep host, auth, and sensitive provider options under the same secret and policy model

### Plugin-defined endpoint families

Requirements:

- plugins may declare additional endpoint families
- plugin families must provide:
  - validation rules
  - secret field metadata
  - preflight behavior if supported
  - audit-safe redaction behavior

## Recommended Delivery Plan

### Phase D0. Document and stabilize the model

- choose the managed / inline / hybrid governance policy model
- choose the shared endpoint reference vocabulary
- define the secret reference format

### Phase D1. Implement policy and secret resolution

- add secret reference resolution
- add environment policy gates for inline endpoint usage
- add audit-safe redaction behavior

### Phase D2. Implement hot reload for managed connections

- add refresh hooks for managed endpoint updates
- preserve snapshot-based in-flight behavior
- add last-known-good fallback on refresh failure

### Phase D3. Expand family coverage and plugin path

- align JDBC, Kafka, Elasticsearch, and AI families under the same governance rules
- add plugin metadata path for new endpoint families

## Acceptance Criteria

The governance design is successful when:

- templates can use managed or inline endpoints according to policy
- secrets are handled without leaking into logs or persisted template output
- hot reload is safe and observable
- JDBC, Kafka, Elasticsearch, and AI providers all fit the same governance model
- plugin-defined endpoint families can participate without special-case governance code

## Non-Goals

The near-term design should avoid:

- forcing every inline endpoint into a permanent managed catalog before first use
- allowing unrestricted plain-text secrets in production
- letting template-local overrides bypass connection governance
- tying all runtime endpoint creation to pre-declared Spring beans only
