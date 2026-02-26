# Uniport-Gateway

Vert.x-based reverse proxy for the Uniport/Inventage Portal Solution. Traefik-inspired architecture with entrypoints, routers, middlewares, services, and providers.

## Build & Dev Commands

```bash
# Full build (includes Docker image)
mvn clean install

# Build without Helm chart (no private registry deps)
mvn clean install -pl '!helm' -Ppublic

# Run tests only
mvn test

# Run single test class
mvn test -pl server -Dtest=RouterFactoryTest

# Code formatting (spotless, ratcheted to origin/main)
mvn spotless:apply

# Code quality checks
mvn checkstyle:check spotbugs:check

# Run via Docker Compose (starter-kit)
docker compose -f starter-kit/docker-compose.yml up
```

## Prerequisites

- Java 21
- Maven 3.9.2+
- Maven credentials in `~/.m2/settings.xml` for Nexus (unless using `-Ppublic`)

## Project Structure

```
api/              - Shared interfaces/models
server/           - Main application (entry point: GatewayLauncher.java)
config-schemas/   - JSON Schema generation for config validation
extensions/       - Custom middleware extension point (JAR loading)
helm/             - Kubernetes Helm chart (OCI registry)
docs/             - MkDocs documentation site (Node.js based)
starter-kit/      - 10-step progressive tutorial with docker-compose examples
```

## Architecture

- **Static config** (`uniport-gateway.json`): Entrypoints + providers. Set at startup, not hot-reloaded.
- **Dynamic config**: Routers, middlewares, services. Hot-reloaded via file/docker/aggregator providers.
- **Middleware chain**: Entrypoint middlewares (all requests) → Router middlewares (matched routes). Order matters.
- **Glue router pattern**: Three-tier routing for dynamic middleware updates without entrypoint changes.

### Key Entry Points

- `server/.../GatewayLauncher.java` - JVM entry, Vert.x init, metrics/tracing
- `server/.../core/GatewayVerticle.java` - Main verticle, deploys entrypoints
- `server/.../core/entrypoint/Entrypoint.java` - HTTP listener per entrypoint
- `server/.../proxy/router/RouterFactory.java` - Builds router chains from dynamic config
- `server/.../proxy/provider/ProviderAggregator.java` - Merges config from providers

## Code Style

- **Spotless** enforces formatting (ratcheted to origin/main) - run `mvn spotless:apply` before committing
- **Checkstyle** rules in `.code-style-settings/checkstyle/config.xml`
- **SpotBugs** exclusions in `.code-style-settings/spotbugs/exclude.xml`
- Pre-commit hooks enforce style

## Testing

- JUnit 5 + Vert.x test utilities + AssertJ
- Test configs in `server/src/test/resources/configs/` (6 scenarios: auth-only, dev-reduced, standalone-auth, extensions, entrypoints, router-rules)
- SmallRye JWT for test token generation
- 79+ test files in server module

## Gotchas & Non-Obvious Patterns

- Sessions are **NOT** enabled by default - must be explicitly configured via middleware
- Invalid/malformed router configs are **silently ignored** (fail-soft)
- Session cookies are **filtered** from backend requests for security
- Config lookup order: env `UNIPORT_GATEWAY_JSON` → system property → `/etc/uniport-gateway/default/uniport-gateway.json` → `./uniport-gateway.json`
- Dynamic config supports `${ENV_VAR}` substitution
- Config changes are throttled (default 2000ms interval)
- Hazelcast uses Kubernetes DNS for cluster discovery (headless service)
- Docker image runs as non-root `appuser`
- Multi-arch Docker builds (amd64 + arm64) via `-DmultiArchBuild=true`

## Docker

- Default ports: 20000 (API), 9090 (Prometheus), 20009 (debug), 9010 (JMX)
- JVM uses 50% of container memory limit (`MaxRAMPercentage=50`)
- Config volume: `/etc/uniport-gateway`

## CI/CD

- GitHub Actions: build.yml (Maven + Docker), release.yml, deploy-docs.yml
- Build profile `-Pci` for CI (disables git hooks, version checks)
- Artifacts: Docker images + Helm charts to OCI registry, Maven to Nexus
