# Getting Started

The Portal-Gateway can be built & launched as follows.

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

A simple setup can be launched by first starting some background services with `docker compose` and then run the Portal-Gateway with the launch config `Launch (router-rules)` (VSCode) or the run config `PortalGateway` (IntelliJ).

```bash
docker compose -f server/src/test/resources/configs/router-rules/docker-compose.yml up
```

Then visit <http://localhost:20000>

> **Note**: To use the run config in IntelliJ, the plugin `net.ashald.envfile` has to be installed.

### Docker

Alternatively, a similar configuration can be launched by running [docker compose](.docs/starter-kit/docker-compose.yml).

```bash
docker compose -f .docs/starter-kit/docker-compose.yml up
```

Then visit <http://localhost:20000>

> **Important**: For the service discovery of the `docker` provider to work, the `/var/run/docker.sock` has to be available and have permissions set to `666`. There are [some security aspects](https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html#rule-1-do-not-expose-the-docker-daemon-socket-even-to-the-containers) involved.
