# Changelog

All notable changes to this project will be documented in this file. The changes should be categorized under one of these sections: Added, Changed, Deprecated, Removed, Fixed or Security.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 9.1.0-[Unreleased] - ???

[Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~???) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D???)

### Changed

- Replaced Portal-Kowl with Portal-Kafka-UI on `/ips/portal-messaging` ([PORTAL-2005](https://issue.inventage.com/browse/PORTAL-2005)).
- Modifying oauth2 flow --> Set prompt=none for accept headers that do not allow text/html ([PORTAL-2004](https://issue.inventage.com/browse/PORTAL-2004)).
- Upgraded Vert.X from `4.4.4` to `4.4.9` ([PORTAL-2027](https://issue.inventage.com/browse/PORTAL-2027)).
- Make clustered session store retry timeout in `session` middleware configurable ([PORTAL-2027](https://issue.inventage.com/browse/PORTAL-2027)).
- Make max redirect retries in `replacedSessionCookieDetection` middleware configurable ([PORTAL-2027](https://issue.inventage.com/browse/PORTAL-2027)).
- Version upgrade for Portal-Code-Style-Settings to `1.4.0-202404081537-54-54f1eba`.
- Version upgrade for Portal-Helm-Chart-Library to `4.7.0-202404231356-316-dc0a409`.

### Fixed

- Correctly detect a request with an expired session in the `replacedSessionCookieDetection` middleware ([PORTAL-2027](https://issue.inventage.com/browse/PORTAL-2027)).

## [9.0.0]-202401150956-1070-d0103604 - 2024-01-15

[Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~9.0.0-202401150956-1070-d0103604) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D9.0.0-202401150956-1070-d0103604)

### Changed

- Added `SameSite` attribute with value `STRICT` to `uniport.session-lifetime` cookie ([PORTAL-1942](https://issue.inventage.com/browse/PORTAL-1942)).
- Remove disabled tests ([PORTAL-1731](https://issue.inventage.com/browse/PORTAL-1731)).
- Update Step 'Dev Deployment' to use Jenkinslib method ([PORTAL-1623](https://issue.inventage.com/browse/PORTAL-1623)).
- Do not use any conditional logic in logback configuration files ([PORTAL-1562](https://issue.inventage.com/browse/PORTAL-1562)).
- **BREAKING**: configuration of security context for gateway container. Note that this is breaking due to changes in `values.yaml`. The security context is now enabled by default.

### Fixed

- JWT must be decoded with base64url ([PORTAL-1838](https://issue.inventage.com/browse/PORTAL-1838)).
- Update helm maven plugin to version `6.11.1` which fixes the wrong binary download on Apple Silicon machines ([PORTAL-1824](https://issue.inventage.   com/browse/PORTAL-1824)).

### Added

- Configure security context for pod & containers ([PORTAL-1379](https://issue.inventage.com/browse/PORTAL-1379))
- Every middleware creates its own trace span ([PORTAL-1617](https://issue.inventage.com/browse/PORTAL-1617))


## [8.3.0]-202309071344-1005-8468f9f1 - 2023-09-07

[Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~8.3.0-202309071344-1005-8468f9f1) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D8.3.0-202309071344-1005-8468f9f1)

### Added

-  Add support to run the Portal-Gateway in cluster mode ([PORTAL-1572](https://issue.inventage.com/browse/PORTAL-1572)).
-  Add matomo middleware for autologin feature ([PORTAL-1718](https://issue.inventage.com/browse/PORTAL-1718)).

### Changed

- Publishing of the chart to the maven repository has been discontinued. Please download the chart from the helm repository instead.
- Updated `portal-helm-chart-library` to `4.5.0-202309061145-260-8c797e8`
- Updated `code-style-settings` to `1.2.0-202308041303-1-08f26fa`

### Fixed

- Read Control API Cookie from Response ([PORTAL-1716](https://issue.inventage.com/browse/PORTAL-1716)).
- Default logback.xml added to container image ([PORTAL-1717](https://issue.inventage.com/browse/PORTAL-1717)).

## [8.2.0]-202308021352-962-750876b6 - 2023-08-02

[JIRA](https://issue.inventage.com/projects/PORTAL/versions/???) - [Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~8.2.0-202308021352-962-750876b6) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D8.2.0-202308021352-962-750876b6)

### Changed

- Build docker server image with dependencies and app on different layers ([PORTAL-1650](https://issue.inventage.com/browse/PORTAL-1650)).

### Fixed

- RoutingContext cannot be used in the Interceptors ([PORTAL-1663](https://issue.inventage.com/browse/PORTAL-1663)).
- Middleware chain is traversed in the wrong order for responses ([PORTAL-1664](https://issue.inventage.com/browse/PORTAL-1664)).
- CSP Middleware supports directives without values (fixes Dashboard loading in Portal-Monitoring) ([PORTAL-1667](https://issue.inventage.com/browse/PORTAL-1667)).

## [8.1.0]-202307270944-948-1cb69438 - 2023-07-27

[JIRA](https://issue.inventage.com/projects/PORTAL/versions/18089) - [Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~8.1.0-202307270944-948-1cb69438~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D8.1.0-202307270944-948-1cb69438)

### Changed

- Using sessionScope "Login" for /login route (instead of "Dashboard") ([PORTAL-1558](https://issue.inventage.com/browse/PORTAL-1558)).
- Informs Keycloak on Session-Reset to delete its user state ([PORTAL-1422](https://issue.inventage.com/browse/PORTAL-1422)).
- Upgrade to Java 17 ([PORTAL-1575](https://issue.inventage.com/browse/PORTAL-1575)).
- Upgrade the Vert.x stack to `4.4.4` ([PORTAL-1454](https://issue.inventage.com/browse/PORTAL-1454)).
- Upgrade all plugins and dependencies to their latest version.
- Changed user in docker containers to non-root ([PORTAL-1342](https://issue.inventage.com/browse/PORTAL-1342)).
- Update `portal-helm-chart-library` from 4.3.0-202305220818-197-8c61d10 to 4.4.0-202307251410-244-fc44010.

### Added

- Middleware `claimToHeader` for setting an HTTP header from a JWT claim value ([PORTAL-1483](https://issue.inventage.com/browse/PORTAL-1483)).
- Support for additional issuers. Can be defined in the configuration with `additionalIssuers` ([PORTAL-1331](https://issue.inventage.com/browse/PORTAL-1331)).
- Configuring the CSP middleware at route level. It is now possible to define basic CSP policies on the entry-middleware and specific/more restrictive CSP policies on each specific route ([PORTAL-1230](https://issue.inventage.com/browse/PORTAL-1230)).
- Support for periodical public keys refreshs for `bearerOnly` and `passAuthorization` middlewares. Can be configured with `publicKeysReconcilation.enabled` and `publicKeysReconcilation.intervalMs` in the middleware options ([PORTAL-1020](https://issue.inventage.com/browse/PORTAL-1020)).
- Created middleware `bodyHandler`, that is required for the csrf middleware ([PORTAL-1497](https://issue.inventage.com/browse/PORTAL-1497)).
- Enhanced configuration for CSP-middleware. It is possible to define how external/incoming CSP policies should be merged with the middleware policies ([PORTAL-1470](https://issue.inventage.com/browse/PORTAL-1470)).
- Language cookie name can be configured ([PORTAL-1636](https://issue.inventage.com/browse/PORTAL-1636)).
- `contentTypes`, `loggingRequestEnabled` and `loggingResponseEnabled` options added for the `requestResponseLogger` middleware ([PORTAL-1341](https://issue.inventage.com/browse/PORTAL-1341)).
- Add a CSP violation reporting server middleware ([PORTAL-1241](https://issue.inventage.com/browse/PORTAL-1241)).

### Fixed

- Fixed native image build ([PORTAL-764](https://issue.inventage.com/browse/PORTAL-764)).
- Registered CORSMiddlewareFactory. Before it was not possible to use the CORSMiddleware.

## [8.0.0]-202305240847-789-bdd58cd9 - 2023-05-24

[JIRA](https://issue.inventage.com/projects/PORTAL/versions/17887) - [Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~8.0.0-202305240847-789-bdd58cd9~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D8.0.0-202305240847-789-bdd58cd9)

### Fixed

- Bug when authentication request is not initiated by Portal-Gateway ([PORTAL-1417](https://issue.inventage.com/browse/PORTAL-1417)).

### Added

- Maven Dependency that generates an SBom with all dependencies for CLM Analysis ([PORTAL-1017](https://issue.inventage.com/browse/PORTAL-1017)).
- Added SharedCodeStyle Plugin ([PORTAL-919](https://issue.inventage.com/browse/PORTAL-919)).
- "httpsOptions" configuration to define the outgoing HTTPS connection ([PORTAL-1296](https://issue.inventage.com/browse/PORTAL-1296)).
- New "config-schemas" module that creates new json schemas for gateway/proxy's configuration files ([PORTAL-407](https://issue.inventage.com/browse/PORTAL-407)).
- OpenTelemetryMiddleware for adding traceId and sessionId as early as possible to the logging contextual data ([PORTAL-1416](https://issue.inventage.com/browse/PORTAL-1416)).
- New optional `uriWithoutSessionTimeoutReset` parameter in session middleware added. There will be no session timeout reset if a request contains one of the configured paths ([PORTAL-1321](https://issue.inventage.com/browse/PORTAL-1321))
- Add stacktrace to logback configuration ([PORTAL-1243](https://issue.inventage.com/browse/PORTAL-1243)).
- Process exits with code 0 if any middleware initialization fails ([PORTAL-1016](https://issue.inventage.com/browse/PORTAL-1016)).
- `uriWithoutLoggingRegex` option for RequestResponseLoggerMiddleware allowing selective logging of requests based on their URI ([PORTAL-1418](https://issue.inventage.com/browse/PORTAL-1418))
- Fail build when example configs are invalid ([PORTAL-1461](https://issue.inventage.com/browse/PORTAL-1461)).

### Changed

- **BREAKING**: Removed entrypoint configuration `sessionDisabled`, that was deprecated in `5.1.0` ([PORTAL-1459](https://issue.inventage.com/browse/PORTAL-1459)).
- **BREAKING**: HTTP response header `X-IPS-Trace-Id` is not written anymore, please use `OpenTelemetryMiddleware` as entrypoint middleware instead ().
- **BREAKING**: Changed cookie name `ips.language` to `uniport.language`, `inventage-portal-gateway.session` to `uniport.session`, and `ipg.state` to `uniport.state` ([PORTAL-718](https://issue.inventage.com/browse/PORTAL-718)).
- **BREAKING**: Removed configuration session bag middleware configuration `whithelistedCookies`, that was deprecated in `4.3.0` ([PORTAL-620](https://issue.inventage.com/browse/PORTAL-620)).
- The filter that determines which requests refresh the session has been updated to exclude polling requests from Conversation [PORTAL-1409](https://issue.inventage.com/browse/PORTAL-1409)).
- Change route ordering. `/health` route is hard-configured as first route to be considered ([PORTAL-859](https://issue.inventage.com/browse/PORTAL-859)).
- Support mounting of gateway-routing-config files via configMap. The directory `proxy-config.examples` contains a selected set of example config files for each microservice. Consumers of this chart should selectively copy these files and adapt according to their specific environment ([PORTAL-1290](https://issue.inventage.com/browse/PORTAL-1290)).
- Update portal-helm-chart-library to version `4.3.0-202305220818-197-8c61d10`

### Fixed

- JWT must be decoded with base64url ([PORTAL-1838](https://issue.inventage.com/browse/PORTAL-1838)).

## [7.0.2]-202304141241-4-576b35e4 - 2023-04-17

[JIRA](https://issue.inventage.com/projects/PORTAL/versions/18068) - [Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~7.0.2-202304141241-4-576b35e4~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D7.0.2-202304141241-4-576b35e4)

### Fixed

- [PORTAL-1396](https://issue.inventage.com/browse/PORTAL-1396)

## [7.0.1]-202303091216-1-ee2c2a1 - 2023-03-09

[JIRA](https://issue.inventage.com/projects/PORTAL/versions/17885) - [Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~7.0.1-202303091216-1-ee2c2a1~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=keyword%3D7.0.1-202303091216-1-ee2c2a1)

### Fixed

- Logout triggers 5 redirect requests fixed ([PORTAL-864](https://issue.inventage.com/browse/PORTAL-864)).
- "Unknown algorithm RSA" bug fixed ([PORTAL-1092](https://issue.inventage.com/browse/PORTAL-1092)).

## [7.0.0]-202303080728-648-fe6be73 - 2023-03-08

[JIRA](https://issue.inventage.com/projects/PORTAL/versions/17863) - [Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~7.0.0-202303080728-648-fe6be73~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D7.0.0-202303080728-648-fe6be73)

### Added

- Added a custom implementation of the relying party (based on vertx's implementation) ([PORTAL-1146](https://issue.inventage.com/browse/PORTAL-1146)).
- Add a CSP middleware ([PORTAL-665](https://issue.inventage.com/browse/PORTAL-665)).
- Add a CSRF middleware ([PORTAL-666](https://issue.inventage.com/browse/PORTAL-666)).
- Support for HTTPS for backend connections ([PORTAL-1292](https://issue.inventage.com/browse/PORTAL-1292)).
- SessionMiddleware can return session lifetime information ([PORTAL-1174](https://issue.inventage.com/browse/PORTAL-1174)).
- Logging incoming request and outgoing response. Logging more or less depending on log level defined in logback.xml ([PORTAL-1133](https://issue.inventage.com/browse/PORTAL-1133)).

### Changed

- **BREAKING**: docker-compose artefact is no longer provided ([PORTAL-1251](https://issue.inventage.com/browse/PORTAL-1251)).
- **BREAKING**: the `bearerOnly` middleware expects now a key with `publicKeys` with an array containing multiple objects, each with a public key ([PORTAL-1092](https://issue.inventage.com/browse/PORTAL-1092)).
- Updated the Vert.x stack to `4.3.7` ([PORTAL-1146](https://issue.inventage.com/browse/PORTAL-1146)).
- Updated the Vert.x stack to `4.3.8` ([PORTAL-1236](https://issue.inventage.com/browse/PORTAL-1236)).
- Updated usage of `io.vertx.json.schema` to newest concepts ([PORTAL-1146](https://issue.inventage.com/browse/PORTAL-1146)).
- Use `io.vertx.vertx-http-proxy` instead of our own fork ([PORTAL-1146](https://issue.inventage.com/browse/PORTAL-1146)).
- Use `proxyInterceptor` hooks in `proxy` middleware to apply custom logic on the request and responses instead of patching them directly ([PORTAL-1146](https://issue.inventage.com/browse/PORTAL-1146)).
- Patched issuer URL of JWT returned by the OIDC flow to the publicly facing URL ([PORTAL-1146](https://issue.inventage.com/browse/PORTAL-1146)).
- Inherit from public class `JWTAuthHandler` (and its ecosystem) to apply custom claim checks instead of copying the whole Vert.x JWT ecosystem ([PORTAL-1146](https://issue.inventage.com/browse/PORTAL-1146)).
- `options` in middleware configurations should be optional, if no options are needed ([PORTAL-1240](https://issue.inventage.com/browse/PORTAL-1240)).
- Fast failing if any middleware creation fails ([PORTAL-1106](https://issue.inventage.com/browse/PORTAL-1106)).

### Removed

- Code Hygiene: Removed dead code ([PORTAL-1146](https://issue.inventage.com/browse/PORTAL-1146)).

### Fixed

- Code: Hygiene: Usage of various deprecated functionalities ([PORTAL-1146](https://issue.inventage.com/browse/PORTAL-1146)).
- HTTP `HOST` header in backend request is now set to configured services.servers.host value ([PORTAL-1295](https://issue.inventage.com/browse/PORTAL-1295)).

## [6.0.0]-202302060855-584-ac9bd6a - 2023-02-06

[Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~6.0.0-202302060855-584-ac9bd6a~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D6.0.0-202302060855-584-ac9bd6a)

### Added

- Enabling versions with `helm ls` in uniport projects ([PORTAL-1128](https://issue.inventage.com/browse/PORTAL-1128)).
- Value for OIDC response mode is now configurable in `oauth2` middleware ([PORTAL-1196](https://issue.inventage.com/browse/PORTAL-1196)).
- Middleware `checkRoute` for triggering authentication for a specific route ([PORTAL-1198](https://issue.inventage.com/browse/PORTAL-1198)).

### Changed

- Use image pull secrets from service account ([PORTAL-847](https://issue.inventage.com/browse/PORTAL-847)).
- Pull busybox image over nexus.
- **BREAKING**: `sessionBag`-middleware needs to be declared as `entry-middleware` in `portal-gateway.json` (see [migration guide](./MIGRATION_GUIDE.md)). ([PORTAL-988](https://issue.inventage.com/browse/PORTAL-988)).
- Helm-Chart-Library version upgraded to 4.1.0-202302011108-163-b4dc038.

### Removed

- Removed all configuration variables for Document microservice.

### Fixed

- Changed log statements from `debug` to `warn` for JWT verification failures ([PORTAL-1130](https://issue.inventage.com/browse/PORTAL-1130)).
- Upgrade org.apache.commons:commons-text version to 1.10.0 ([PORTAL-1110](https://issue.inventage.com/browse/PORTAL-1110)).
- Initial URI added to `state` parameter of authentication flow ([PORTAL-1184](https://issue.inventage.com/browse/PORTAL-1184)).

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

- **BREAKING** Session handling middlewares (if required) need to be explicitly declared in portal-gateway.json (see [migration guide](./MIGRATION_GUIDE.md))
- **BREAKING** Upgraded `portal-helm-chart-library` to `4.0.0` (see migration guide in [portal-helm-chart-library](https://github.com/uniport/portal-helm-chart-library/blob/master/CHANGELOG.md)).
- Do not log unhandled (ignored) URLs in `ShowSessionContentMiddleware`. This should reduce noise for cases we are not interested in.

### Fixed

- Added missing `PORTAL_GATEWAY_BEARER_TOKEN_OPTIONAL` variable to Helm values with its default value `false`.

### Removed

- Removed unused `PROXY_BEARER_TOKEN_ISSUER` variable from Helm values we provide.

## [4.3.0]-202208121035-483-5b7b069 - 2022-08-12

[JIRA](https://issue.inventage.com/projects/PORTAL/versions/17574) -
[Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~4.3.0-202208121035-483-5b7b069~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D4.3.0-202208121035-483-5b7b069)

### Changed

- Typo in variable name. Backward compatibility for configuration files that contain the typo is still provided. From `whithelistedCookies` to `whitelistedCookies` ([PORTAL-620](https://issue.inventage.com/browse/PORTAL-620)).

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
- Prevent access to various /auth\* URLs

### Changed

- SecureFlag set for `inventage-portal-gateway.session` cookie

## [2.6.0]-202111100757-232-3f0c716 - 2021-11-10

[Nexus2](https://nexus.inventage.com/#nexus-search;gav~~~2.6.0-202111100757-232-3f0c716~~) - [Nexus3](https://nexus3.inventage.com/#browse/search=version%3D2.6.0-202111100757-232-3f0c716)

### Removed

- OpenShift deployment removed

## [2.5.0]-202110181145-223-2a0ba2a - 2021-10-18

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

[unreleased]: https://github.com/uniport/portal-gateway/compare/9.0.0...master
[9.0.0]: https://github.com/uniport/portal-gateway/compare/8.3.0...9.0.0
[8.3.0]: https://github.com/uniport/portal-gateway/compare/8.2.0...8.3.0
[8.2.0]: https://github.com/uniport/portal-gateway/compare/8.1.0...8.2.0
[8.1.0]: https://github.com/uniport/portal-gateway/compare/8.0.0...8.1.0
[8.0.0]: https://github.com/uniport/portal-gateway/compare/7.0.2...8.0.0
[7.0.2]: https://github.com/uniport/portal-gateway/compare/7.0.1...7.0.2
[7.0.1]: https://github.com/uniport/portal-gateway/compare/7.0.0...7.0.1
[7.0.0]: https://github.com/uniport/portal-gateway/compare/6.0.0...7.0.0
[6.0.0]: https://github.com/uniport/portal-gateway/compare/5.1.0...6.0.0
[5.1.0]: https://github.com/uniport/portal-gateway/compare/5.0.0...5.1.0
[5.0.0]: https://github.com/uniport/portal-gateway/compare/4.3.0...5.0.0
[4.3.0]: https://github.com/uniport/portal-gateway/compare/4.2.0...4.3.0
[4.2.0]: https://github.com/uniport/portal-gateway/compare/4.1.0...4.2.0
[4.1.0]: https://github.com/uniport/portal-gateway/compare/4.0.0...4.1.0
[4.0.0]: https://github.com/uniport/portal-gateway/compare/3.3.0...4.0.0
[3.3.0]: https://github.com/uniport/portal-gateway/compare/3.2.0...3.3.0
[3.2.0]: https://github.com/uniport/portal-gateway/compare/3.1.0...3.2.0
[3.1.0]: https://github.com/uniport/portal-gateway/compare/3.0.0...3.1.0
[3.0.0]: https://github.com/uniport/portal-gateway/compare/2.7.0...3.0.0
[2.7.0]: https://github.com/uniport/portal-gateway/compare/2.6.0...2.7.0
[2.6.0]: https://github.com/uniport/portal-gateway/compare/2.5.0...2.6.0
[2.5.0]: https://github.com/uniport/portal-gateway/compare/2.4.1...2.5.0
[2.4.1]: https://github.com/uniport/portal-gateway/compare/2.3.0...2.4.1
[2.4.0]: https://github.com/uniport/portal-gateway/compare/2.3.0...2.4.1
[2.3.0]: https://github.com/uniport/portal-gateway/compare/2.2.0...2.3.0
[2.2.0]: https://github.com/uniport/portal-gateway/compare/2.1.0...2.2.0
[2.1.0]: https://github.com/uniport/portal-gateway/compare/2.0.0...2.1.0
[2.0.0]: https://github.com/uniport/portal-gateway/compare/1.3.0...2.0.0
[1.3.0]: https://github.com/uniport/portal-gateway/compare/1.2.0...1.3.0
[1.2.0]: https://github.com/uniport/portal-gateway/compare/1.1.0...1.2.0
[1.1.0]: https://github.com/uniport/portal-gateway/compare/1.0.0...1.1.0
[1.0.0]: https://github.com/uniport/portal-gateway/compare/49fb083d...1.0.0
