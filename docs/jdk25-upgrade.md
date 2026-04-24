# JDK 25 Upgrade Notes

## Current baseline

- Runtime JDK used for validation: `E:\Home\vasin.GENSOKYO\sdk\zulu-jdk25.0.1`
- Compiler target remains Java `17`
- Maven wrapper version: `3.9.11`

## Verified commands

Use the repo-local helper script:

```powershell
.\mvnw-jdk25.ps1 -v
.\mvnw-jdk25.ps1 test
.\mvnw-jdk25.ps1 -U -DskipTests clean package
```

Or run Maven directly with a temporary process-local JDK:

```powershell
$env:JAVA_HOME='E:\Home\vasin.GENSOKYO\sdk\zulu-jdk25.0.1'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -s .mvn\settings-jdk25.xml test
.\mvnw.cmd -s .mvn\settings-jdk25.xml -U -DskipTests clean package
```

## Important constraints

- Do not change global environment variables.
- Do not modify global Maven settings.
- Use `.mvn/settings-jdk25.xml` for this repository because internal Nexus still uses HTTP.

## Current status

- `test`: passed on JDK 25
- `clean package`: passed on JDK 25
- AI tests are skipped automatically when Ollama is unavailable on `localhost:11434`

## Remaining warnings

The remaining JDK 25 warning is from Maven's own embedded Guice dependency:

- `sun.misc.Unsafe::staticFieldBase`

This is not caused by the project code or test configuration.

## Project-side warning mitigations already applied

- `.mvn/jvm.config`
  - enables native access for Maven process
- Surefire/Failsafe test JVM args
  - preloads Mockito as `javaagent`
  - disables CDS for test JVM
  - disables Netty unsafe path during tests
- `ClassGraph` test dependency upgraded to reduce JDK 25 warning noise
