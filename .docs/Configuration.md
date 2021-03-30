# Configuration

The Portal-Gateway is a Reverse Proxy (or Edge Router) inspired by [traefik](https://doc.traefik.io/traefik/). It receives requests and finds out which components are responsible for handling them. It automatically disovers the infrastracture and finds the right configuration to forward each request to the right service.

- [Configuration](#configuration)
  - [Configurations](#configurations)
    - [Static Configuration](#static-configuration)
      - [Configuration file](#configuration-file)
    - [Dynamic Configuration](#dynamic-configuration)
  - [Providers](#providers)
    - [Orchestrators](#orchestrators)
    - [Provider Namespaces](#provider-namespaces)
    - [Supported Providers](#supported-providers)
      - [Docker](#docker)
        - [Port Detection](#port-detection)
        - [Docker API Access](#docker-api-access)
        - [Provider Configuration](#provider-configuration)
      - [[WIP] Kubernetes](#wip-kubernetes)
      - [File](#file)
        - [Provider Configuration](#provider-configuration-1)
    - [[WIP] Configuration Reload Frequency](#wip-configuration-reload-frequency)
  - [Routing](#routing)
    - [TODO Doc](#todo-doc)
    - [Entrypoints](#entrypoints)
    - [Applications](#applications)
    - [Routers](#routers)
    - [Middlewares](#middlewares)
      - [Authoriuation Header](#authoriuation-header)
      - [Headers](#headers)
      - [OAuth2](#oauth2)
      - [Redirext Regex](#redirext-regex)
      - [Replace Path Regex](#replace-path-regex)
    - [Services](#services)
    - [Providers](#providers-1)

## Configurations

The portal-gateway knows two types of configurations: **static** and **dynamic** configuration.

### Static Configuration

The static configuration is known on startup. It specifies entrypoints, applications and providers.

There are three different, **mutually exclusive** (e.g. you can use only on at the same time), ways to define static configuration in Portal-Gateway:

- In a config file
- [WIP] With CLI arguments
- As envionment variables

These ways are evaluated in the order listed above (a later definition overwrites the previous).

#### Configuration file

At startup, Portal-Gateway searches for a file named `portal-gateway.json` in:

- `PORTAL_GATEWAY_JSON`
- `/etc/portal-gateway/`
- `.` (the current working directory)
- [WIP] defined by the CLI argument `--configFile`

### Dynamic Configuration

The dynamic configuration contains everything that defines how the requests are handled by the system. This configuration can change at runtime.

## Providers

Configuration discovery is achieved through Providers. The Portal-Gateway queries the provider APIs in order to find relevant information about routing and when changes are detected, the routes are dynamically updated.

### Orchestrators

The Portal-Gateway supports three types of providers:

- Label-based: each deployed container has a set of labels attached to it
- Annotation-based: a seperate object, with annotations, defines the characteristics of the container
- File-based: uses files to define configuration

### Provider Namespaces

When certain objects are declared in the dynamic configuration, such as middleware, services, they reside in their provider's namespace. For example, if you declare a middleware using a Docker label, it resides in the Docker provider namespace.

If you use multiple providers and wish to reference such an object declared in another provider (e.g. referencing a cross-provider object like middleware), then the object should be suffixed by the `@` seperator and the provider name-

```
<resource-name>@<provider-name>
```

### Supported Providers

Currently supported providers are:

- Docker Containers (published container ports to the host)
- [WIP] Kubernetes Services

#### Docker

With Docker, container labels are used to retrieve its routing configuration.

##### Port Detection

The private IP and port of containers are retrieved from the Docker API

Port detection works as follows:

- If a container exposes **no port**, then this container is ignored.
- If a container exposes a **single port**, then this port is used for private communication
- If a container exposes **multiple ports**, then a port must be manually set using the label `portal.http.service.<service-name>.server.port

##### Docker API Access

The Portal-Gateway requires access to the docker socket to get its dynmica configuration. The Docker API Endpoint, that should be used, can be specified with the directive `endpoint`.

**Security Note**: Accessing the Docker API without any restrictions is a security concern and [not recommended by OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html#rule-1-do-not-expose-the-docker-daemon-socket-even-to-the-containers): If the Portal-Gateway is attacked, then the attacker might get access to the underlying host:

```
[...] only trusted users shouls be allowed to control your Docker dameon [...]
```
[Source: Docker Daemon Attack Surface documentation](https://docs.docker.com/engine/security/#docker-daemon-attack-surface)

**Solution**: Expose the Docker socket over SSH, instead of the default Unix socket file. SSH is supported with [Docker > 18.09](https://docs.docker.com/engine/security/protect-access/).

##### Provider Configuration

- `endpoint` (*Required, Default="unix:///var/run/docker.sock"*): See [Docker API Access](#docker-api-access)
- `defaultRule` (*Optional, Default="Host('${name}')*): Defines what routing rule to apply to a container if no rule is defined by a label. It must be a valid [StringSubstitutor](https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/StringSubstitutor.html). The container service name can be accessed with the `name` identifier, and the StringSubstitutor has access to all the labels defined on this container.
- `watch` (*Optiona, Default=true*): Watch Docker events.

#### [WIP] Kubernetes

- TODO: endpoints, namespace, token, etc.

#### File

The file provider lets you define the dynamic configuration in a JSON file.

It supports providing configuration throught single configuration file or multiple seperate files.

**Tip**: The file provider can be a good solution for reusing common elements from other providers.

##### Provider Configuration

- `filename`: Defines the path to the configuration file.
- `directory`: Defines the path to the directory that contains the configuration files.
- `watch`: Set the watch option to `true` to automatically watch for file changes.

**Note**: The ``filename`` and ``directory`` are mutually exlusive.

### [WIP] Configuration Reload Frequency

```
providers.providersThrottleDuration
Optional, Default: 2s
```

In some cases, some providers might undergo a sudden burst of changes, which would generate a lot of configuration change events. If all of them are taken into account, more configuration reloads would be triggered than is necessary or even useful.

In order to mitigate that, this option can be set. It is the duration that Traefik waits for, after a configuration reload, before taking into account any new configuration refresh event. If multiple events occur within this time, only the most recent one is taken into account, and all others are discarded.

This option cannot be set per provider, but the throttling algorithm applies to each of them independently.

The value should be provided in seconds.

## Routing

### TODO Doc

  - Entrypoints (per Application)
  - Applications
  - Routers (Http, Rule, Rule Priority, Middlewares (order), Services)
  - Middlewares

    - Authorization Header
    - Headers
    - OAuth2
    - Redirect Regex
    - Replace Path Regex

  - Services (No loadbalancing at the moment)
  - Providers

### Entrypoints

### Applications

### Routers

### Middlewares

#### Authoriuation Header

#### Headers

#### OAuth2

#### Redirext Regex

#### Replace Path Regex

### Services

**Note**: The character @ is not authorized in the service name.

### Providers
