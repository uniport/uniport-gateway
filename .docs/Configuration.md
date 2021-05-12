# 1. Configuration

The Portal-Gateway is a Reverse Proxy (or Edge Router) inspired by [traefik](https://doc.traefik.io/traefik/). It receives requests and finds out which components are responsible for handling them. It automatically disovers the infrastracture and finds the right configuration to forward each request to the right service.

- [1. Configuration](#1-configuration)
  - [1.1. Configurations](#11-configurations)
    - [1.1.1. Static Configuration](#111-static-configuration)
      - [1.1.1.1. Configuration file](#1111-configuration-file)
    - [1.1.2. Dynamic Configuration](#112-dynamic-configuration)
  - [1.2. Providers](#12-providers)
    - [1.2.1. Orchestrators](#121-orchestrators)
    - [1.2.2. Provider Namespaces](#122-provider-namespaces)
    - [1.2.3. Supported Providers](#123-supported-providers)
      - [1.2.3.1. Docker](#1231-docker)
        - [1.2.3.1.1. IP/Port Detection](#12311-ipport-detection)
        - [1.2.3.1.2. Docker API Access](#12312-docker-api-access)
        - [1.2.3.1.3. Provider Configuration](#12313-provider-configuration)
      - [1.2.3.2. [TODO] Kubernetes](#1232-todo-kubernetes)
      - [1.2.3.3. File](#1233-file)
        - [1.2.3.3.1. Provider Configuration](#12331-provider-configuration)
    - [1.2.4. [TODO] Configuration Reload Frequency](#124-todo-configuration-reload-frequency)
  - [1.3. Routing](#13-routing)
    - [1.3.1. Entrypoints](#131-entrypoints)
    - [1.3.2. Applications](#132-applications)
    - [1.3.3. Routers](#133-routers)
    - [1.3.4. Services](#134-services)
  - [1.4. Middlewares](#14-middlewares)
    - [1.4.1. Proxy](#141-proxy)
    - [1.4.2. Session Bag](#142-session-bag)
    - [1.4.3. Headers](#143-headers)
    - [1.4.4. OAuth2](#144-oauth2)
    - [1.4.5. Authorization Bearer](#145-authorization-bearer)
    - [1.4.6. Redirect Regex](#146-redirect-regex)
    - [1.4.7. Replace Path Regex](#147-replace-path-regex)
    - [1.4.8. Show session content](#148-show-session-content)
  - [1.5. Providers](#15-providers)

## 1.1. Configurations

The portal-gateway knows two types of configurations: **static** and **dynamic** configuration.

### 1.1.1. Static Configuration

The static configuration is known on startup. It specifies entrypoints, applications and providers.

There are three different, **mutually exclusive** (e.g. you can use only on at the same time), ways to define static configuration in Portal-Gateway:

- In a config file
- [TODO] With CLI arguments
- As envionment variables

These ways are evaluated in the order listed above (a later definition overwrites the previous).

#### 1.1.1.1. Configuration file

At startup, Portal-Gateway searches for a file named `portal-gateway.json` in:

- `PORTAL_GATEWAY_JSON`
- `/etc/portal-gateway/`
- `.` (the current working directory)
- [TODO] defined by the CLI argument `--configFile`

### 1.1.2. Dynamic Configuration

The dynamic configuration contains everything that defines how the requests are handled by the system. This configuration can change at runtime.

## 1.2. Providers

Configuration discovery is achieved through Providers. The Portal-Gateway queries the provider APIs in order to find relevant information about routing and when changes are detected, the routes are dynamically updated.

### 1.2.1. Orchestrators

The Portal-Gateway supports three types of providers:

- Label-based: each deployed container has a set of labels attached to it
- Annotation-based: a seperate object, with annotations, defines the characteristics of the container
- File-based: uses files to define configuration

### 1.2.2. Provider Namespaces

When certain objects are declared in the dynamic configuration, such as middleware, services, they reside in their provider's namespace. For example, if you declare a middleware using a Docker label, it resides in the Docker provider namespace.

If you use multiple providers and wish to reference such an object declared in another provider (e.g. referencing a cross-provider object like middleware), then the object should be suffixed by the `@` seperator and the provider name-

```
<resource-name>@<provider-name>
```

### 1.2.3. Supported Providers

Currently supported providers are:

- Docker Containers (published container ports to the host)
- [TODO] Kubernetes Services

#### 1.2.3.1. Docker

With Docker, container labels are used to retrieve its routing configuration.

##### 1.2.3.1.1. IP/Port Detection

The private IP and port of containers are retrieved from the Docker API

Network i.e IP selection works as follows

- 

Port detection works as follows:

- If a container exposes **no port**, then this container is ignored.
- If a container exposes a **single port**, then this port is used for private communication
- If a container exposes **multiple ports**, then a port must be manually set using the label `portal.http.service.<service-name>.server.port

##### 1.2.3.1.2. Docker API Access

The Portal-Gateway requires access to the docker socket to get its dynmica configuration. The Docker API Endpoint, that should be used, can be specified with the directive `endpoint`.

**Security Note**: Accessing the Docker API without any restrictions is a security concern and [not recommended by OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html#rule-1-do-not-expose-the-docker-daemon-socket-even-to-the-containers): If the Portal-Gateway is attacked, then the attacker might get access to the underlying host:

```
[...] only trusted users shouls be allowed to control your Docker dameon [...]
```
[Source: Docker Daemon Attack Surface documentation](https://docs.docker.com/engine/security/#docker-daemon-attack-surface)

**Solution**: Expose the Docker socket over SSH, instead of the default Unix socket file. SSH is supported with [Docker > 18.09](https://docs.docker.com/engine/security/protect-access/).

##### 1.2.3.1.3. Provider Configuration

- `endpoint` (*Required, Default="unix:///var/run/docker.sock"*): See [Docker API Access](#docker-api-access)
- `exposedByDefault` (*Optional, Default=true*): Expose containers by default through Portal-Gateway. If set to `false`, containers that do not have a `portal.enable=true` label are ignored from the resulting routing configuration.
- `network` (*Optional, Default=""*): Defines a default docker network to use for connections to all containers.
- `defaultRule` (*Optional, Default="Host('${name}')*): Defines what routing rule to apply to a container if no rule is defined by a label. It must be a valid [StringSubstitutor](https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/StringSubstitutor.html). The container service name can be accessed with the `name` identifier, and the StringSubstitutor has access to all the labels defined on this container.
- `watch` (*Optiona, Default=true*): Watch Docker events.

#### 1.2.3.2. [TODO] Kubernetes

- TODO: endpoints, namespace, token, etc.

#### 1.2.3.3. File

The file provider lets you define the dynamic configuration in a JSON file.

It supports providing configuration throught single configuration file or multiple seperate files.

**Tip**: The file provider can be a good solution for reusing common elements from other providers.

##### 1.2.3.3.1. Provider Configuration

- `filename`: Defines the path to the configuration file.
- `directory`: Defines the path to the directory that contains the configuration files. It is important to undestand how multiple configuration files are assembled: Generally, a deep merge (recursive) matches (sub) JSON objects in the existing tree and replaces all matching entries. JsonArrays are treated like any other entry, i.e. replaced entirely. This pattern is applied to all files being in the same directory. At the moment two sub-directories are supported: `general/` and `auth/`. Sub-directories are mostly merged the same way as described above with the exception of JsonArrays. JsonArrays are concatenated without duplicates.
- `watch`: Set the watch option to `true` to automatically watch for file changes.

**Note**: The ``filename`` and ``directory`` are mutually exlusive.

### 1.2.4. [TODO] Configuration Reload Frequency

Status: Functionality exists, but not yet configurable (default applies).

```
providers.providersThrottleDuration
Optional, Default: 2s
```

In some cases, some providers might undergo a sudden burst of changes, which would generate a lot of configuration change events. If all of them are taken into account, more configuration reloads would be triggered than is necessary or even useful.

In order to mitigate that, this option can be set. It is the duration that Portal-Gateway waits for, after a configuration reload, before taking into account any new configuration refresh event. If multiple events occur within this time, only the most recent one is taken into account, and all others are discarded.

This option cannot be set per provider, but the throttling algorithm applies to each of them independently.

The value should be provided in seconds.

## 1.3. Routing

### 1.3.1. Entrypoints

Entrypoints are the network entry points into Portal-Gateway. They define the port which will receive packets.  Entrypoints are part of the static configurations.

```
name
Required, String, Name of the entrypoint
```

```
port
Required, Integer, Port number
```

### 1.3.2. Applications

Applications define the core logic of the Portal-Gateway. They define what application listens on which port and path.

Name of the Application

```
name
Required, String,
```

Reference to an existing entrypoint to listen for requests

```
entrypoint
Required, String,
```

TODO: Path the application is listen on

```
requestSelector.urlPrefix
Required, String
```

Classname of the application handling requests

```
provider
Required, String
```

### 1.3.3. Routers

A router is in charge of connecting incoming requests to the services that can handle them. In the process, routers may use middlewares to update the request or act before forwarding the request to the service.

Name of the router

```
name
Required, String
```

If not specified, the router will accept requests from all defined entry points. If you want to limit the router scope to a set of entrypoints, set the entrypoints option.

```
entrypoints
Optional, List of Strings
```

A rule is a matcher configured with values, that determine if a particular request matches specifig criteria. If the rule is verified, the router becomes active, calls middlewares and then forwards the request to the service.

```
rule
Optional, Path('/example')|PathPrefix('/example')|Host('example.com'),
```

To avoid path overlap, routers are sorted by default, in descending order using the rules length. The priority is directly equals the length of the rule and so the longest length has the highest priority.

```
priority
Optional, Integer
```

A router can have a list of middlewares attached. The middlewares will take effect only if the rule matches and before forwarding the request to the service.

**Note**: The charachter `@``is not authorized in the middleware name.

**Note**: Middleware are applied in the same order as their declaration in the router.

```
middlewares
Optional, List of Strings
```

Each request must eventually be handled by a service, which is why each router definition should include a service target, which is basically where the request will be passed along to.

In general, a service assigned to a router should have been defined, but there there are exceptions for label-based providers. See the specifig docker documentation.
```
service
Required, String
```

### 1.3.4. Services

Services are responsible for configuring how to reach the actual services that will eventually handle the oncoming requests.

Name of the service
**Note**: The character @ is not authorized in the service name.

```
name
Required, String
```

Servers declare a single instance of your program. The host and port option point to a specific instance.

```
servers
Required, List of tuple of host (String) and port (String or Integer)
```

## 1.4. Middlewares

Attached to the routers, middlewares tweak requests before they are sent to a service (or before the answer from a service is sent back to the client).

Middlewares can be combined in chains to fit every scenario.

### 1.4.1. Proxy

The Proxy is implemented as a middleware but it cannot be dynamically set. It is always the last middleware in the chain and forwards the incoming request to the service.

### 1.4.2. Session Bag

The SessionBag is also implemented as a middleware but cannot be set dynamically. It is always the first middleware in the chain and is responsible for the cookie handling. The user agent in general won't see any cookie but Vert.x session cookie. The SessionBag manages all cookies related to this session. It intercepts responses from services, removes them and stores them. In future requests of the same session, it will then set those cookie again so that the services won't see any difference.

One exception to this rule is the Keycloak session cookie for the Master realm. This is the only cookie, apart from the Vert.x session cookie, that will be passed to the user agent. This is required for some Keycloak login logic.

### 1.4.3. Headers

The Headers middleware manages the headers of incoming requests and outgoing responses.

```
customRequestHeaders
Optional, Pairs of header name and value
```

```
customResponseHeaders
Optional, Pairs of header name and value
```

The following example adds the `X-Script-Name` header to the proxied request and the `X-Custom-Response-Header` header to the response (label-based notation).

```yaml
labels:
  - "portal.http.middlewares.testHeader.headers.customrequestheaders.X-Script-Name=test"
  - "portal.http.middlewares.testHeader.headers.customresponseheaders.X-Custom-Response-Header=value"
```

### 1.4.4. OAuth2

The OAuth2 provides authentication (no authorization) by a Keycloak server instance. It intercepts all requests and redirects for authentication if needed. It stores the ID token and access token under the given session scope after successful authentication.

Provider client id

```
clientId
Required, String
```

Provider client secret

```
clientSecret
Required, String
```

Provider discover url. Usually, `https://keycloak.ch/auth/realms/<your-realm>`

```
discoveryUrl
Required, URL
```

```
sessionScope
Required, String
```

Example

```yaml
labels:
  - "portal.http.middlewares.test-oauth2.oauth2.clientId=testclient"
  - "portal.http.middlewares.test-oauth2.oauth2.clientSecret=testsecret"
  - "portal.http.middlewares.test-oauth2.oauth2.discoverUrl=https://keycloak.ch/auth/realms/testrealm"
  - "portal.http.middlewares.test-oauth2.oauth2.sessionScope=testScope"
```

### 1.4.5. Authorization Bearer

The AuthorizationBearer set a token in the HTTP header `Authorization: Bearer <token>` depending on the session scope. It is thightly coupled to the OAuth2 middleware as it uses tokens acquired by the authentication process. 

The session scope defines what token should be set in the Auth Bearer header. This could either be an ID token or a access token. Per user there is one ID token and zero or more access tokens.

```
sessionScope
Required, id|<referencing a session scope defined by a OAuth2 middleware>
```

Example

```yaml
# sessionScope should be 'id' or reference a sessionScope defined by a OAuth2 middleware
labels:
  - "portal.http.middlewares.test-auth-bearer.authorizationBearer.sessionScope=testScope"
```

### 1.4.6. Redirect Regex

The RedirectRegex redirects a request using regex matching and replacement.

The regex option is the regular expression to match and capture elements from the request URL.

```
regex
Required, String
```

The replacement option defines how to modify the URL to have the new target URL.

```
replacement
Required, String
```

Example

```yaml
# Redirect with domain replacement
# Note: all dollar signs need to be doubled for escaping in a docker-compose.yml.
labels:
  - "portal.http.middlewares.test-redirectregex.redirectregex.regex=^http://localhost/(.*)"
  - "portal.http.middlewares.test-redirectregex.redirectregex.replacement=http://mydomain/$${1}"
```

### 1.4.7. Replace Path Regex

The ReplacePathRegex replaces the path of an URL using regex matching and replacement.

TODO: The ReplacePathRegex will store the original path in a `X-Replaced-Path` header.

The regex option is the regular expression to match and capture the path from the request URL.

```
regex
Required, String
```

The replacement option defines the replacement path format, which can include captured variables.

```
replacement
Required, String
```

Example

```yaml
# Replace path with regex
labels:
  - "portal.http.middlewares.test-replacepathregex.replacepathregex.regex=^/foo/(.*)"
  - "portal.http.middlewares.test-replacepathregex.replacepathregex.replacement=/bar/$$1"
```

### 1.4.8. Show session content

The ShowSessionContent is intended for **development** only. It will show the current session content. This includes the session ID, any Cookies and, if logged in, the ID token (encoded and decoded), any access tokens (encoded and decoded). To trigger the session middleware, add `_session_` to the request path of a router with this middleware that will handle the request.

Example

```yaml
# TODO: currently not configurable by labels
```

## 1.5. Providers

TODO