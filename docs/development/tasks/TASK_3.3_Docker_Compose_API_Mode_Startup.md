# TASK 3.3: Docker Compose API Mode Startup Alignment

Status: ✅ Completed (2025-11-03)
Priority: Medium
Owner: Immaculate Nyoni (with Chainguard builder/runtime)

## Summary
Ensure the FIN application starts in API mode when launched under Docker Compose using the fat JAR. Previously, the container passed the argument `api`, but the JAR's `Main-Class` was `fin.ConsoleApplication`, which ignored `api` and showed the interactive console menu. This task updates the main entrypoint to detect `api` and delegate to `ApiApplication` so `/api/v1/health` becomes reachable when running via Compose.

## Why
- Compose builds the fat JAR with `Main-Class: fin.ConsoleApplication` (see `app/build.gradle.kts`).
- `ConsoleApplication` did not interpret the `api` argument.
- Our Compose runtime image (`Dockerfile.compose`) correctly passed `api`, but the app still launched the console UI.

## Scope
- One code file: `app/src/main/java/fin/ConsoleApplication.java`
- No public API change; only startup behavior when `api` arg present.
- No database or Flyway changes.

## Changes
- ConsoleApplication: detect `"api"` or `"--api"` as first argument and delegate to `ApiApplication.main(args)`, returning immediately.

```java
// ConsoleApplication.main
if (args.length > 0 && ("api".equalsIgnoreCase(args[0]) || "--api".equalsIgnoreCase(args[0]))) {
    ApiApplication.main(args);
    return;
}
```

## Build & Quality Gates
- Checkstyle Inventory: Ran `./gradlew clean checkstyleMain --no-daemon ...` and saved `violations_inventory.txt`.
- Build: PASS — `./gradlew clean build` succeeded.
- SpotBugs: Non-blocking warnings remain (ignoreFailures=true for main); no new issues introduced by this change.

## Runtime Notes (Docker)
- Working Dockerfile for Compose: `Dockerfile.compose` (now using Chainguard builder/runtime). Entrypoint passes `api` by default.
- With this task, the app now respects `api` when using the fat JAR, so `/api/v1/health` should respond once containers are up.

## Verification Steps
1. Rebuild image: `docker compose build fin-app`.
2. Start stack: `docker compose up -d`.
3. Check health: `curl http://localhost:8080/api/v1/health` → expect JSON with success=true and DB status.

## Adjacent Cleanup (Not performed here)
- Consider setting `Main-Class` to a small delegator or `ApiApplication` for an API-only image variant.
- Optionally pin Chainguard image digests for reproducibility.

## References
- `Dockerfile.compose`: Chainguard-based builder/runtime, waits for `postgres`, CMD `["api"]`.
- `app/build.gradle.kts`: `application.mainClass = "fin.ConsoleApplication"` and fat JAR manifest.
- `fin.ApiApplication`: API entrypoint that boots `ApiServer`.
