# Getting Started

The Portal-Gateway can be built & launched as follows.

---

## Build

```bash
mvn clean install
```

!!! note "Authentication for dependencies"

    Your configuration at [~/.m2/settings.xml](http://maven.apache.org/settings.html#Servers) needs to exist with the following content:

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

---

## Launch

### IDE

A simple setup can be launched by first starting some background services with `docker compose` and then run the Portal-Gateway with the launch config `Launch (router-rules)` (VSCode) or the run config `PortalGateway` (IntelliJ).

```bash
docker compose -f server/src/test/resources/configs/router-rules/docker-compose.yml up
```

Then visit <http://localhost:20000>

!!! note

    To use the run config in IntelliJ, the plugin `net.ashald.envfile` has to be installed.

---

### Docker

Alternatively, a similar configuration can be launched by running `docker compose`.

=== "Command"

    ```bash
    docker compose -f starter-kit/docker-compose.yml up
    ```

=== "docker-compose.yml"

    ```yaml
    services:
        gateway:
            image: uniportcr.artifacts.inventage.com/com.inventage.portal.gateway.uniport-gateway:10.0.0-202507090956-185-a370e5a
            environment:
            - "UNIPORT_GATEWAY_JSON=/config/uniport-gateway.json"
            - "UNIPORT_GATEWAY_LOG_LEVEL=INFO"
            - "OTEL_TRACES_EXPORTER=none"
            - "OTEL_METRICS_EXPORTER=none"
            volumes:
            - ./config:/config/
            ports:
            - "20000:20000"

        # example resource server
        whoami1:
            image: traefik/whoami
            expose:
            - "80"

        # example resource server
        whoami2:
            image: traefik/whoami
            expose:
            - "80"
    ```

=== "config/uniport-gateway.json"

    ```json
    {
        "entrypoints": [
            {
                "name": "http20000",
                "port": 20000
            }
        ],
        "providers": [
            {
                "name": "file",
                "filename": "./dynamic-config/config.json",
                "watch": true
            }
        ]
    }
    ```

=== "config/dynamic-config/config.json"

    ```json
    {
        "http": {
            "routers": [
                {
                    "name": "root",
                    "middlewares": [
                        "rootRedirectregex"
                    ],
                    "rule": "Path('/')",
                    "service": "whoami1"
                },
                {
                    "name": "whoami1",
                    "rule": "PathPrefix('/whoami1')",
                    "service": "whoami1"
                },
                {
                    "name": "whoami2",
                    "rule": "PathPrefix('/whoami2')",
                    "service": "whoami2"
                },
                {
                    "name": "whoami2-host",
                    "rule": "Host('local.uniport.ch.')",
                    "priority": 50,
                    "service": "whoami2"
                }
            ],
            "middlewares": [
                {
                    "name": "rootRedirectregex",
                    "type": "redirectRegex",
                    "options": {
                        "regex": "^/$",
                        "replacement": "/whoami1"
                    }
                }
            ],
            "services": [
                {
                    "name": "whoami1",
                    "servers": [
                        {
                            "host": "whoami1",
                            "port": 80
                        }
                    ]
                },
                {
                    "name": "whoami2",
                    "servers": [
                        {
                            "host": "whoami2",
                            "port": 80
                        }
                    ]
                }
            ]
        }
    }`
    ```

Then visit <http://localhost:20000>, or <http://local.uniport.ch:20000> (resolves to `127.0.0.1` and showcases the host-based routing):

```text
Hostname: cff2d034ac36
IP: 127.0.0.1
IP: ::1
IP: 172.26.0.2
RemoteAddr: 172.26.0.4:50064
GET /whoami1 HTTP/1.1
Host: whoami1
User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:139.0) Gecko/20100101 Firefox/139.0
Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
Accept-Encoding: gzip, deflate, br, zstd
Accept-Language: en-US,en;q=0.5
Cookie: CSRF-Token-XX7NAT3=GPtkAi3vvofrxGfwnpWb5ra4kdJFmUCsgkLbiAeKiKmJr4SU7hvo5awzsnz3maDY
Dnt: 1
Priority: u=0, i
Referer: http://127.0.0.1:8000/
Sec-Fetch-Dest: document
Sec-Fetch-Mode: navigate
Sec-Fetch-Site: cross-site
Sec-Fetch-User: ?1
Sec-Gpc: 1
Traceparent: 00-8c68596fc18fea908248c17fad03c8a6-acf39563e3661374-01
Upgrade-Insecure-Requests: 1
X-Forwarded-For: 192.168.65.1:39357
X-Forwarded-Host: localhost:20000
X-Forwarded-Port: 20000
X-Forwarded-Proto: http
```

!!! warning "Docker provider permissions"

    For the service discovery of the `docker` provider to work, the `/var/run/docker.sock` has to be available and have permissions set to `666`.
    There are [some security aspects](https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html#rule-1-do-not-expose-the-docker-daemon-socket-even-to-the-containers) involved.
