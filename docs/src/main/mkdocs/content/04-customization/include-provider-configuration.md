<!-- markdownlint-disable first-line-h1 -->

#### File

##### General

The good old configuration file. This is the least magical way to configure the Portal-Gateway. Nothing is done automatically here, and everything must be defined manually.

The basic structure of the configuration file is:

```json
{
    "http": {
        "routers": [],
        "middlewares": [],
        "services": []
    }
}
```

Environment variables can also be used in all parts of the configuration file.

??? example "Example of a configuration file for the Organization Microservice"

    ```json
    {
        "http": {
            "routers": [
                {
                    "name": "organisation-graphql",
                    "rule": "PathPrefix('/v1/')",
                    "middlewares": ["bearerOnly"],
                    "service": "organisation-graphql"
                },
                {
                    "name": "organisation-frontend",
                    "rule": "PathPrefix('/')",
                    "middlewares": ["bearerOnly"],
                    "service": "organisation-frontend"
                }
            ],

            "middlewares": [
                {
                    "name": "bearerOnly",
                    "type": "bearerOnly",
                    "options": {
                        "publicKey": "${PROXY_BEARER_TOKEN_PUBLIC_KEY}",
                        "publicKeyAlgorithm": "RS256",
                        "optional": "${PROXY_BEARER_TOKEN_OPTIONAL}"
                    }
                }
            ],

            "services": [
                {
                    "name": "organisation-graphql",
                    "servers": [
                        {
                            "host": "organisation-graphql",
                            "port": "20031"
                        }
                    ]
                },
                {
                    "name": "organisation-frontend",
                    "servers": [
                        {
                            "host": "organisation-frontend",
                            "port": "20035"
                        }
                    ]
                }
            ]
        }
    }
    ```

##### Routers

```json
{
    "name": "testRouter",
    "middlewares": ["md1", "md2", "md3"],
    "rule": "Path('/')",
    "priority": 42,
    "service": "testService"
}
```

##### Middlewares

```json
{
    "name": "testMiddleware",
    "type": "authorizationBearer",
    "options": {
        "sessionScope": "testScope"
    }
}
```

##### Services

```json
{
    "name": "testService",
    "servers": [
        {
            "host": "example.com",
            "port": 4242
        }
    ]
}
```

#### Docker

##### General

The Portal-Gateway creates a corresponding Router and Service for each container.

A server instance is automatically attached to the Service, and the Default Rule is assigned to the Router if no Routing Rule has been defined in the Labels.

!!! warning "Docker Service Discovery"

    For Docker Container Service Discovery to work, `/var/run/docker.sock` must be mounted in the Portal-Gateway. It is important that `docker.sock` has permission 666 (`sudo chmod 666 /var/run/docker.sock`). In this regard, there are [some security aspects](https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html#rule-1-do-not-expose-the-docker-daemon-socket-even-to-the-containers) to consider.

??? example "Example of a Dockerfile for the Organization Microservice"

    ```dockerfile
    FROM ${docker.pull.registry}/com.inventage.portal.gateway.portal-gateway:${uniport-gateway.version}

    COPY target/docker-context/organisation-proxy-config/ /etc/uniport-gateway/organisation/

    # labels used for the service discovery by the portal-gateway
    LABEL portal.enable="true"
    LABEL portal.http.routers.organisation-proxy.rule="PathPrefix('/organisation')"
    LABEL portal.http.routers.organisation-proxy.middlewares="organisationRedirectRegex, organisationOauth2@file, organisationAuthBearer@file, organisationReplacePath"
    LABEL portal.http.middlewares.organisationRedirectRegex.redirectRegex.regex="^(/organisation)\$"
    LABEL portal.http.middlewares.organisationRedirectRegex.redirectRegex.replacement="\$1/"
    LABEL portal.http.middlewares.organisationReplacePath.replacePathRegex.regex="/organisation/(.*)"
    LABEL portal.http.middlewares.organisationReplacePath.replacePathRegex.replacement="/\$1"
    LABEL portal.http.services.organisation-proxy.servers.port="20030"
    ```

##### Service Definition

In general, when configuring a Service that is assigned to one (or more) Router(s), it must also be defined. However, when using label-based configurations, there are some exceptions:

- If a Label defines a Router (e.g., through a Router Rule) and a Label defines a Service (e.g., through a Server Port), but the Router does not specify a Service, then this Service is automatically assigned to the Router.
- If a Label defines a Router (e.g., through a Router Rule), but no Service is defined, then a Service is automatically created and assigned to the Router.

##### Routers

To update the configuration of the Router automatically attached to the container, add Labels starting with `portal.http.routers.<name-of-your-router>.`, followed by the option to be changed. Available options are `rule`, `priority`, `entrypoints`, `middlewares`, and `service`.

!!! example

    Add this Label `portal.http.routers.test-router.rule=Host('example.com')` to change the rule.

##### Middlewares

Middlewares can be declared by using Labels starting with `portal.http.middlewares.<name-of-your-middleware>.`, followed by the Middleware Type/Options. Examples and detailed explanations are available under "[Customization > Middlewares](#middlewares)".

##### Services

To update the configuration of the Service automatically attached to the container, add Labels starting with `portal.http.services.<name-of-your-service>.`, followed by the option to be changed. Available options are `server.host` and `server.port`.

##### Specific Provider Options

You can instruct Portal-Gateway to consider (or not consider) the container by setting `portal.enable` to true or false. This option overrides the value of `exposedByDefault`.

```yaml
labels:
    - "portal.enable=true"
```

Overrides the default Docker network used for connections with the container. If a container is connected to multiple networks, make sure the correct network name is set, otherwise the container will be ignored.

```yaml
labels:
    - "portal.docker.network=test-network"
```
