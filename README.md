# Portal Gateway

[![Main build](https://github.com/uniport/portal-gateway/actions/workflows/main.yaml/badge.svg)](https://github.com/uniport/portal-gateway/actions/workflows/main.yaml)

The Portal-Gateway acts as an reverse proxy for all requests in Uniport.

## Why yet another reverse proxy?

* Relying party as a reverse proxy with routing
* Handling parallel authentication requests
* Session bag - only the session cookie leaves the platform
* HA deployment on Kubernetes
* Maximum flexibility with minimal components

## Overview

The Portal-Gateway build on top of the concepts of `entrypoints`, `routers`, `middlewares`, `services` and `providers`:

* An `entrypoint` configures the port it is listening on
* A `router` configures a `rule` to route requests, e.g. based on the request's host or path
* A `router` may have `middlewares` to manipulate a request
* A `router` passes the request to a `service` that forward the request to the destination server
* A `provider` reads configuration, e.g. from a file, and provisions the `router`, `middlewares` and `services` accordingly.

![Concept](./docs/content/01-introduction/data/Concept.png)

## Configuration

The Portal-Gateway has two different types of configuration, a `static` configuration and `dynamic` configurations:

* The `static` configuration is the minimal configuration needed to start the Portal-Gateway and cannot be changed at runtime. It consists of `entrypoints` and `providers`.
* The `dynamic` configuration configures `routers`, `middlewares` and `services`. It can be dynamically updated and applied at runtime.

The simplest `provider` is the `file` provider. It reads the configuration from a JSON file and searches at the following locations:

1. File pointed at by the environment variable `PORTAL_GATEWAY_JSON`
2. File pointed at by the system property `PORTAL_GATEWAY_JSON`
3. File `portal-gateway.json` in the `/etc/portal-gateway/default/` directory
4. File `portal-gateway.json` in the current working directory

## Build

```bash
mvn clean install
```

**Note**: Your configuration at [~/.m2/settings.xml](http://maven.apache.org/settings.html#Servers) needs to exist with the following content:

```xml
<servers>
    <server>
        <id>inventage-portal-group</id>
        <username>username</username>
        <password>password</password>
    </server>
</servers>
```

(It is also possible to use [user tokens](https://help.sonatype.com/repomanager3/system-configuration/user-authentication/security-setup-with-user-tokens), instead of username/password)

## Launch

### IDE

A simple setup can be launched by first starting some background services with [docker compose](server/src/test/resources/configs/router-rules/docker-compose.yml), and then run the Portal-Gateway with the launch config `Launch (router-rules)` (VSCode) or the run config `PortalGateway` (IntelliJ).

```bash
docker compose -f server/src/test/resources/configs/router-rules/docker-compose.yml up
```

Then visit <http://localhost:20000>

> **Note**: To use the run config in IntelliJ, the plugin `net.ashald.envfile` has to be installed.

### Docker

Alternatively, a similar configuration can be launched by running [docker compose](starter-kit/docker-compose.yml).

```bash
docker compose -f starter-kit/docker-compose.yml up
```

Then visit <http://localhost:20000>

> **Important**: For the service discovery of the `docker` provider to work, the `/var/run/docker.sock` has to be available and have permissions set to `666`. There are [some security aspects](https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html#rule-1-do-not-expose-the-docker-daemon-socket-even-to-the-containers) involved.
