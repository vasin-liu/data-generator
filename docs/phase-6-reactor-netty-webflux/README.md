# Phase 6 - Reactor / Netty / WebFlux Alignment

## Goal

Validate the reactive HTTP path on the Spring Boot 4.0.5 baseline and remove local overrides that are no longer required.

## Changes

- Removed the parent POM Reactor version override and returned `reactor-core` version management to the Spring Boot BOM.
- Simplified test JVM configuration in the parent POM:
  - removed `-Dio.netty.noUnsafe=true`
  - removed `-Xshare:off`
  - kept only the Mockito javaagent in Surefire/Failsafe `argLine`
- Updated the WebFlux client request body handling in `data-generator-reader-ai`:
  - switched `OllamaApi` request publishing from `.body(Mono.just(...), ...)` to `.bodyValue(...)`
  - fixed `streamingChat()` to send `ChatRequest` as `ChatRequest`, instead of incorrectly declaring it as `GenerateRequest`

## Validation

Validated with JDK 25 at `E:\Home\vasin.GENSOKYO\sdk\zulu-jdk25.0.1` and project-local Maven settings `.mvn/settings-jdk25.xml`.

Commands executed:

```powershell
.\mvnw.cmd -s .mvn\settings-jdk25.xml -pl data-generator-service,data-generator-reader\data-generator-reader-ai -am test
.\mvnw.cmd -s .mvn\settings-jdk25.xml -pl data-generator-service,data-generator-reader\data-generator-reader-ai -am -Dtest.jvm.args=-Xshare:off test
.\mvnw.cmd -s .mvn\settings-jdk25.xml -pl data-generator-service,data-generator-reader\data-generator-reader-ai -am -Dtest.jvm.args= test
```

Results:

- default configuration: passed
- without `io.netty.noUnsafe`: passed
- without any extra test JVM args: passed

Observed final summary:

- `BUILD SUCCESS`
- `Tests run: 25, Failures: 0, Errors: 0, Skipped: 2`
- skipped tests were the Ollama integration tests gated by local availability

## Conclusion

- The explicit Reactor override is no longer required on Boot 4.0.5.
- `-Dio.netty.noUnsafe=true` is not required for the validated reactive modules.
- `-Xshare:off` is also not required for the validated reactive modules.
- Current Boot 4 + WebFlux + Reactor path is stable for:
  - `data-generator-reader-ai`
  - `data-generator-service`

## Artifacts

- `docs/phase-6-reactor-netty-webflux/phase6-test-final.log`
- `docs/phase-6-reactor-netty-webflux/phase6-test-no-unsafe.log`
- `docs/phase-6-reactor-netty-webflux/phase6-test-no-extra-jvm-args.log`
