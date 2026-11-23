# Uniport Gateway

Java 21 / Vert.x reverse proxy gateway with plugin-based middleware. Maven multi-module reactor.

## Modules

- `api` – Middleware interfaces (no server deps)
- `server` – Core gateway (entrypoints, routers, 30+ middlewares, proxy)
- `extensions` – Example custom middleware plugin (SPI-based)
- `config-schemas` – JSON schema generation from Java models
- `docs` – MkDocs documentation (Node `20.9.0` via `frontend-maven-plugin`)
- `helm` – Kubernetes Helm chart
- `starter-kit` – Step-by-step example configs (one feature per step, shared docker-compose)

## Commands

```bash
# Build
mvn clean install
mvn clean install -pl '!helm' -Dpublic=true  # without private deps

# Test
mvn test                                       # unit tests (JUnit 5 + AssertJ)
mvn test -pl server -am -Dtest=CorsMiddlewareTest -Dsurefire.failIfNoSpecifiedTests=false  # single test

# Code quality
mvn spotless:apply                             # format code
mvn initialize checkstyle:check                # checkstyle
mvn initialize spotbugs:check                  # static analysis

# Local dev
docker compose -f server/src/test/resources/configs/router-rules/docker-compose.yml up
# Then: http://localhost:20000
```

## Architecture

Three-layer router system:
1. **Entrypoint** – HTTP listeners (per port/protocol), entrypoint-level middlewares
2. **Glue Router** – Replaceable on dynamic config reload (no verticle restart)
3. **Route-specific** – Per-route middlewares → proxy to backend service

Key classes:
- `GatewayLauncher` – Custom Vert.x launcher (metrics, clustering)
- `GatewayVerticle` – Main verticle, reads config, builds entrypoints
- `ConfigurationWatcher` / `ProviderAggregator` – Dynamic config reload (throttle `2000ms`)

## Adding a Middleware

Pattern: `server/.../proxy/middleware/<name>/` with `<Name>Middleware`, `<Name>MiddlewareFactory`, `Abstract<Name>MiddlewareOptions` (Immutables).

1. Register the factory in `server/src/main/resources/META-INF/services/ch.uniport.gateway.proxy.middleware.MiddlewareFactory`
2. Add a builder method to test helper `MiddlewareServerBuilder`
3. Regenerate schemas with `mvn package -pl config-schemas -am` and commit the updated JSON in `config-schemas/src/main/resources/schema/`
4. Add a CHANGELOG entry

## Gotchas

- **Middleware order matters** – chain is sequential; changing order changes behavior
- **Can't modify request URI mid-chain** – Vert.x limitation; path changes deferred to proxy phase
- **Spotless ratchet** – only checks files changed vs `origin/main` (not all files)
- **Pre-commit hook** – auto-installed via `git-build-hook-maven-plugin`; skipped in CI
- **Immutables** – Config models use `@org.immutables.value`; generated POJOs with validation
- **Full git history required** – CI checkout needs full history for `spotless-maven-plugin`
- **CHANGELOG check** – PRs to `main` need an entry under `[Unreleased]` (Keep a Changelog sections). Bypass with the `no changelog` label.

## Code Style

- Checkstyle + SpotBugs enforced in CI
- Spotless for formatting (run `mvn spotless:apply` before committing)
- Config in `.code-style-settings/`

## Configuration

**Static** (startup): env vars or JSON file (`UNIPORT_GATEWAY_JSON` → `/etc/uniport-gateway/default/uniport-gateway.json` → `./uniport-gateway.json`)
**Dynamic** (runtime): routers, services, middlewares — hot-reloaded via `ConfigurationWatcher`

Key env vars: `UNIPORT_GATEWAY_JSON`, `UNIPORT_GATEWAY_LOG_LEVEL`, `UNIPORT_GATEWAY_METRICS_PORT` (default `9090`)
