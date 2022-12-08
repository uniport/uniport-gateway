# Changelog

All notable changes to this project will be documented in this file. The changes should be categorized under one of these sections: Added, Changed, Deprecated, Removed, Fixed or Security.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

[JIRA](https://issue.inventage.com/projects/PORTAL/versions/???) -
[Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~???~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D???)

### Removed

- Removed all configuration variables for Document microservice.

## [5.1.0]-202212061653-531-6945ffd 2022-12-06

[JIRA](https://issue.inventage.com/projects/PORTAL/versions/17778) -
[Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~5.1.0-202212061653-531-6945ffd~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D5.1.0-202212061653-531-6945ffd)

### Changed

- EntryMiddlewares need to be declared explicitly ([PORTAL-1109](https://issue.inventage.com/browse/PORTAL-1109)).

## [5.0.0]-202212060911-525-5e0cfa8 - 2022-12-06

[JIRA](https://issue.inventage.com/projects/PORTAL/versions/17668) -
[Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~5.0.0-202212060911-525-5e0cfa8~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D5.0.0-202212060911-525-5e0cfa8)

### Added

- Middlewares can be attached to each entrypoint, which are first traversed before a request is forwarded to the route-specific middlewares. ([PORTAL-895](https://issue.inventage.com/browse/PORTAL-895)).
- Setup IQ Evaluation and Checkstyle ([PORTAL-967](https://issue.inventage.com/browse/PORTAL-967)).
- Keycloak Mockserver + OAuth2Auth Middleware tests for PORTAL-512 and PORTAL-513
- Enable PKCE in OIDC flow ([PORTAL-512](https://issue.inventage.com/browse/PORTAL-512)).
- PKCE and Authorization code are sent in the request body ([PORTAL-513](https://issue.inventage.com/browse/PORTAL-513)).
- Configuration for Conversation Microservice added ([PORTAL-1026](https://issue.inventage.com/browse/PORTAL-1026)).
- Configuration for Notification Microservice added ([PORTAL-951](https://issue.inventage.com/browse/PORTAL-951)).

### Changed

- **BREAKING** Session handling middlewares (if required) need to be explicitly declared in portal-gateway.json (see [migration guide](/MIGRATION.md)) 
- **BREAKING** Upgraded `portal-helm-chart-library` to `4.0.0` (see migration guide in [portal-helm-chart-library](https://git.inventage.com/projects/PORTAL/repos/portal-helm-chart-library/browse/CHANGELOG.md)).
- Do not log unhandled (ignored) URLs in `ShowSessionContentMiddleware`. This should reduce noise for cases we are not interested in.

### Fixed

- Added missing `PORTAL_GATEWAY_BEARER_TOKEN_OPTIONAL` variable to Helm values with its default value `false`.

### Removed

- Removed unused `PROXY_BEARER_TOKEN_ISSUER` variable from Helm values we provide.

## [4.3.0]-202208121035-483-5b7b069 - 2022-08-12

[JIRA](https://issue.inventage.com/projects/PORTAL/versions/17574) -
[Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~4.3.0-202208121035-483-5b7b069~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D4.3.0-202208121035-483-5b7b069)

### Changed

-  Typo in variable name. Backward compatibility for configuration files that contain the typo is still provided. From `whithelistedCookies` to `whitelistedCookies` ([PORTAL-620](https://issue.inventage.com/browse/PORTAL-620)).

### Added

- `SESSION_RESET` action in `ControlApiMiddelware` added ([PORTAL-747](https://issue.inventage.com/browse/PORTAL-747)).
- Added possibility to configure logback by providing a `logback.xml` and pointing `PORTAL_GATEWAY_LOGGING_CONFIG` to it ([PORTAL-741](https://issue.inventage.com/browse/PORTAL-741)).

### Removed

- Removed `versiontiger` in favor of the [versions-maven-plugin](https://www.mojohaus.org/versions-maven-plugin/index.html) for version management ([PORTAL-743](https://issue.inventage.com/browse/PORTAL-743)).

## [4.2.0]-202207191024-463-b0dd7e7 - 2022-07-19

[JIRA](https://issue.inventage.com/projects/PORTAL/versions/17561) -
[Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~4.2.0-202207191024-463-b0dd7e7~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D4.2.0-202207191024-463-b0dd7e7)

### Changed

- Remove old parent pom module ([PORTAL-799](https://issue.inventage.com/browse/PORTAL-799)).

## [4.1.0]-202207080655-447-ed21a2a - 2022-07-08

[JIRA](https://issue.inventage.com/projects/PORTAL/versions/17484) -
[Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~4.1.0-202207080815-448-ed21a2a~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D4.1.0-202207080815-448-ed21a2a)

### Added

- Added multi-architecture builds of docker image ([PORTAL-745](https://issue.inventage.com/browse/PORTAL-745)).
- Added structural logging ([PORTAL-741](https://issue.inventage.com/browse/PORTAL-741)).
- Update to Eclipse-Temurin JRE v17 for Docker-Desktop cgroup v2 support & MaxRAMPercentage=50.0 ([PORTAL-453](https://issue.inventage.com/browse/PORTAL-453)).

  **IMPORTANT**: The `portal-gateway` process needs 256 MB memory when running in a container (`docker-comose` = `mem_limit: '256m'`, `kubernetes` = `rescources: / limits: / memory: "256Mi"`). This also applies when `portal-gateway` is used as a proxy in a microservice. The JVM inside the container image is configured in a way that the maximum amount of memory allocated for the heap is 50%.

## [4.0.0]-202205231117-426-efc4093 - 2022-05-23

[JIRA](https://issue.inventage.com/projects/PORTAL/versions/17350) -
[Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~4.0.0-202205231117-426-efc4093~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D4.0.0-202205231117-426-efc4093)

### Added

- Add custom claim check for prefix urls `/ips/portal-database`and `/ips/portal-messaging`. Only JWT with valid claims can access those urls.
([PORTAL-626](https://issue.inventage.com/browse/PORTAL-626))
- Extend the claim verification in the bearer only middleware. It should be possible to verify arbitrary claims.
([PORTAL-654](https://issue.inventage.com/browse/PORTAL-654))
- OpenTelemetry: traces and spans are created using OpenTelemetry SDK and extension of Vert.x. The configuration of the OpenTelemetry logic occurs using [OpenTelemetry SDK Autoconfigure](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure) ([PORTAL-642](https://issue.inventage.com/browse/PORTAL-642)).
- HTTP header `X-Forwarded-Port` is set for outgoing requests to services ([PORTAL-713](https://issue.inventage.com/browse/PORTAL-713)).
- Add route to portal-monitoring UI ([PORTAL-695](https://issue.inventage.com/browse/PORTAL-695))
- Allow literal dot `.` characters in `Path` and `PathPrefix` and `Host` rule values in the router factory.

### Removed

- **BREAKING**: Disabled the native image build. The added OpenTelemetry features lead to errors during health checks on Kubernetes.

### Changed

- Changed `X-IPS-Request-Id` header name to `X-IPS-Trace-Id`.

## [3.3.0]-202203091113-353-e988dae - 2022-03-09

[Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~3.3.0-202203091113-353-e988dae~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D3.3.0-202203091113-353-e988dae)

### Fixed

- Fix timezone on server and native docker image ([PORTAL-606](https://issue.inventage.com/browse/PORTAL-606)).
- Fix user handling in Vert.x context ([PORTAL-647](https://issue.inventage.com/browse/PORTAL-647)).

## [3.2.0]-202202200055-343-81de06f - 2022-02-21

[Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~3.2.0-202202200055-343-81de06f~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D3.2.0-202202200055-343-81de06f)

### Added

- Session idle timeout is configurable ([PORTAL-610](https://issue.inventage.com/browse/PORTAL-610)).

## [3.1.0]-202202071726-335-0975202 - 2022-02-07

[Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~3.1.0-202202071726-335-0975202~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D3.1.0-202202071726-335-0975202)

### Fixed

- Native build and tagging fixed.

### Changed

- The File Config Provider for dynamic configurations allows arbitrary directory names instead of the hard-coded `auth` and `general` ones ([PORTAL-205](https://issue.inventage.com/browse/PORTAL-205)).

## [3.0.0]-202202071120-328-c8f8caa - 2022-02-07

[Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~3.0.0-202202071120-328-c8f8ca~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D3.0.0-202202071120-328-c8f8ca)

### Added

- Created helm values for minikube environment
- Memory requirements and limits in Kubernetes added ([PORTAL-570](https://issue.inventage.com/browse/PORTAL-570)).
- Language Cookie Handler Middleware added ([PORTAL-590](https://issue.inventage.com/browse/PORTAL-590)).
- Handling of parallel authentication requests ([PORTAL-563](https://issue.inventage.com/browse/PORTAL-563)).
- Logging with contextual data ([PORTAL-578](https://issue.inventage.com/browse/PORTAL-578)).

### Changed

- **BREAKING** Switched from `navigation` microservice to `base`. You will need to have at least version `1.0.0` of the base microservice deployed if you want to use this version as a portal proxy.
- Upgraded portal-helm-chart-library
- Pulling images from Nexus instead of Docker Hub
- Use SCREAMING_SNAKE_CASE for environment variables
- Replaced `com.spotify.dockerfile-maven-plugin` with `io.fabric8.docker-maven-plugin` for building docker images ([PORTAL-571](https://issue.inventage.com/browse/PORTAL-571)).

## [2.7.0]-202112080733-264-ce3614d - 2021-12-08

[Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~2.7.0-202112080733-264-ce3614d~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D2.7.0-202112080733-264-ce3614d)

### Added

- Added memory limits to the services defined in Docker Compose files using `mem_limit`. This also means using [Docker Compose files version `2.4`](https://docs.docker.com/compose/compose-file/compose-versioning/#version-24) which has support for this feature.
- Use PKCE for OIDC ([PORTAL-512](https://issue.inventage.com/browse/PORTAL-512))
- Prevent access to various /auth* URLs

### Changed

- SecureFlag set for `inventage-portal-gateway.session` cookie

## [2.6.0]-202111100757-232-3f0c716 - 2021-11-10

[Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~2.6.0-202111100757-232-3f0c716~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D2.6.0-202111100757-232-3f0c716)

### Removed
- OpenShift deployment removed

## [2.5.0]-202110181145-223-2a0ba2a

[Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~2.5.0-202110181145-223-2a0ba2a~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D2.5.0-202110181145-223-2a0ba2a)

### Changed

- Version updated of portal-helm-chart-library.version dependency

## [2.4.1]-202110041214-219-5351207 - 2021-10-04

### Changed

- disable tracing because of runtime problems in native mode

## [2.4.0]-202110040810-216-85d8f83 - 2021-10-04

[JIRA](https://issue.inventage.com/projects/PORTAL/versions/17097) - [Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~2.4.0-202110040810-216-85d8f83~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D2.4.0-202110040810-216-85d8f83)

### Added

- Added `ControlApiMiddleware` (PORTAL-195). Handles control api actions provided as values from a "IPS_GW_CONTROL" cookie. Supported actions:
  - SESSION_TERMINATE: invalidates the session and calls "end_session_endpoint" on Keycloak
- Tracing enabled ([PORTAL-418](https://issue.inventage.com/browse/PORTAL-418))
- Ports can now also be defined by env variables ([PORTAL-417](https://issue.inventage.com/browse/PORTAL-417))

## [2.3.0]-202109100643-197-6a6ac10 - 2021-09-10

[JIRA](https://issue.inventage.com/projects/PORTAL/versions/17079) - [Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~2.3.0-202109100643-197-6a6ac10~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D2.3.0-202109100643-197-6a6ac10)

### Added

- Added `X-Forwarded-Proto` to headers that are forwarded to the target service to enable proper redirects. Required for ([PORTAL-353](https://issue.inventage.com/browse/PORTAL-353)).
- `RequestResponseLogger` logs now the status code of the response

## [2.2.0]-202108231142-190-927586e - 2021-08-23

[JIRA](https://issue.inventage.com/projects/PORTAL/versions/17064) - [Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~2.2.0-202108231142-190-927586e~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D2.2.0-202108231142-190-927586e)

### Added

- `docker-compose.jar` is built with and without Maven variable substitution ([PORTAL-360](https://issue.inventage.com/browse/PORTAL-360))
- health route and check ([PORTAL-255](https://issue.inventage.com/browse/PORTAL-255))

### Changed

- Updated `helm-chart-library` to version `1.3.0` (`1.3.0-202108231107-49-5f559db`).

## [2.1.0]-202108060813-181-e0baeec - 2021-08-09

[JIRA](https://issue.inventage.com/projects/PORTAL/versions/16990) - [Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~2.1.0-202108060813-181-e0baeec~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D2.1.0-202108060813-181-e0baeec)

### Added

- Support for Refresh tokens in `AuthorizationBearer` middleware ([PORTAL-298](https://issue.inventage.com/browse/PORTAL-298))
- `BearerOnly` middleware ([PORTAL-341](https://issue.inventage.com/browse/PORTAL-341))
- Routing for microservice FileStorage added ([PORTAL-352](https://issue.inventage.com/browse/PORTAL-352))
- `docker-compose.jar` is built with and without Maven variable substitution ([PORTAL-360](https://issue.inventage.com/browse/PORTAL-360))

## [2.0.0]-202107261151-161-ad6799e - 2021-07-26

### Added

- Build as Uber/fat jar artifact
- Build as native image
- Option to disable session management

### Changed

- Use proxy for each microservice

## [1.3.0]-202107070850-141-913856c - 2021-07-07

### Added

- Kubernetes pods defer their start until all dependencies are up and running.
- Pom module

### Fixed

- Version tiger

### Changed

- Path of default gateway configuration to `/etc/portal-gateway/default`
- Docker-compose artifacts contains only the base `docker-compose.yml` and `*.common.env` files.

### Removed

- Remove suffix `-public` from proxy hostnames

## [1.2.0]-202106141557-113-657c581 - 2021-06-14

### Added

- Session Bag ([PORTAL-194](https://issue.inventage.com/browse/PORTAL-194))
- Configuration documentation ([PORTAL-206](https://issue.inventage.com/browse/PORTAL-206))
- Helm charts ([PORTAL-216](https://issue.inventage.com/browse/PORTAL-216))
- Cookie configuration feature ([PORTAL-246](https://issue.inventage.com/browse/PORTAL-246))

### Changed

- Artifact `docker-compose.jar` contains the files from the `src/main/resources` folder with and without any variable substitution ([PORTAL-260](https://issue.inventage.com/browse/PORTAL-260))

## [1.1.0]-202104301516-89-6ea3315 - 2021-04-30

### Added

- Session information page for development/debug purposes ([PORTAL-204](https://issue.inventage.com/browse/PORTAL-204))
- Tests

## [1.0.0]-202104161309-72-2861749 - 2021-04-16

### Added

- Portal-Gateway providing reverse proxy functionality within the Inventage Portal Solution ([PORTAL-89](https://issue.inventage.com/browse/PORTAL-89)).

[Unreleased]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?sourceBranch=refs%2Fheads%2Fmaster&targetBranch=refs%2Ftags%2F5.1.0
[5.1.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?targetBranch=refs%2Ftags%2F5.0.0&sourceBranch=refs%2Ftags%2F5.1.0
[5.0.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?targetBranch=refs%2Ftags%2F4.3.0&sourceBranch=refs%2Ftags%2F5.0.0
[4.3.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?targetBranch=refs%2Ftags%2F4.2.0&sourceBranch=refs%2Ftags%2F4.3.0
[4.2.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?targetBranch=refs%2Ftags%2F4.1.0&sourceBranch=refs%2Ftags%2F4.2.0
[4.1.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?targetBranch=refs%2Ftags%2F4.0.0&sourceBranch=refs%2Ftags%2F4.1.0
[4.0.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?targetBranch=refs%2Ftags%2F3.3.0&sourceBranch=refs%2Ftags%2F4.0.0
[3.3.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?targetBranch=refs%2Ftags%2F3.2.0&sourceBranch=refs%2Ftags%2F3.3.0
[3.2.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?targetBranch=refs%2Ftags%2F3.1.0&sourceBranch=refs%2Ftags%2F3.2.0
[3.1.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?targetBranch=refs%2Ftags%2F3.0.0&sourceBranch=refs%2Ftags%2F3.1.0
[3.0.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?targetBranch=refs%2Ftags%2F2.7.0&sourceBranch=refs%2Ftags%2F3.0.0
[2.7.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?targetBranch=refs%2Ftags%2F2.6.0&sourceBranch=refs%2Ftags%2F2.7.0
[2.6.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?targetBranch=refs%2Ftags%2F2.5.0&sourceBranch=refs%2Ftags%2F2.6.0
[2.5.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?sourceBranch=refs%2Ftags%2F2.5.0&targetBranch=refs%2Ftags%2F2.4.1
[2.4.1]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?sourceBranch=refs%2Ftags%2F2.4.1&targetBranch=refs%2Ftags%2F2.3.0
[2.4.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?sourceBranch=refs%2Ftags%2F2.4.0&targetBranch=refs%2Ftags%2F2.3.0
[2.3.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?sourceBranch=refs%2Ftags%2F2.3.0&targetBranch=refs%2Ftags%2F2.2.0
[2.2.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?sourceBranch=refs%2Ftags%2F2.2.0&targetBranch=refs%2Ftags%2F2.1.0
[2.1.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?sourceBranch=refs%2Ftags%2F2.1.0&targetBranch=refs%2Ftags%2F2.0.0
[2.0.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?sourceBranch=refs%2Ftags%2F2.0.0&targetBranch=refs%2Ftags%2F1.3.0
[1.3.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?sourceBranch=refs%2Ftags%2F1.3.0&targetBranch=refs%2Ftags%2F1.2.0
[1.2.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?sourceBranch=refs%2Ftags%2F1.2.0&targetBranch=refs%2Ftags%2F1.1.0
[1.1.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?sourceBranch=refs%2Ftags%2F1.1.0&targetBranch=refs%2Ftags%2F1.0.0
[1.0.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/commits?until=1.0.0
