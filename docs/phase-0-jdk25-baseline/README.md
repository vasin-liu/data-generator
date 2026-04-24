# Phase 0 JDK 25 Baseline

Generated on `2026-04-24`.

## Purpose

These files freeze the last known-good repository state before starting the incremental Spring Boot 4.0 and dependency-train upgrade.

Current validated baseline:

- Build JDK: `25.0.1`
- Compiler target: Java `17`
- Spring Boot baseline: `3.5.13`
- Maven wrapper: `3.9.11`

## Artifacts

- `build-dependency-tree.txt`
  - full repository dependency tree under the JDK 25 baseline
- `service-effective-pom.xml`
  - effective POM for `data-generator-service`
- `jdk25-test.log`
  - full repository test log
- `jdk25-clean-package.log`
  - full repository `clean package` log with tests skipped

## Validation results

- `test`
  - `BUILD SUCCESS`
  - `Tests run: 25, Failures: 0, Errors: 0, Skipped: 2`
- `clean package`
  - `BUILD SUCCESS`

## Remaining warning boundary

The remaining JDK 25 warning during Maven execution is from Maven's embedded Guice dependency:

- `sun.misc.Unsafe::staticFieldBase`

This is a Maven runtime issue, not a project dependency regression.

## Recommended use during Boot 4.0 work

- Compare dependency drift against `build-dependency-tree.txt`
- Compare service build model changes against `service-effective-pom.xml`
- Re-run `test` and `clean package` after each phase and compare log tails/results
