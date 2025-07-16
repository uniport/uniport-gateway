<!-- markdownlint-disable first-line-h1 -->

The JSON file for the static configuration is searched for at startup in the specified order:

1.  File specified via the Environment Variable `PORTAL_GATEWAY_JSON`
2.  File specified via the System Property `PORTAL_GATEWAY_JSON`
3.  File `uniport-gateway.json` in the `/etc/uniport-gateway/default/` directory
4.  File `uniport-gateway.json` in the current directory (Run Configuration "PortalGateway" := ./server/uniport-gateway)

??? abstract "Example of a static configuration"

    Additional configuration files can be referenced from this JSON file. For example, this configuration file uses the `file` Provider for dynamic configuration, with further files contained in the subdirectory `./dynamic-config/`.

    ```json
    {
        "entrypoints": [
            {
                "name": "http20000",
                "port": 20000,
                "middlewares": [
                {
                    "name": "customHeader",
                    "type": "headers",
                    "options": {
                        "customResponseHeaders": {
                            "customResponseHeader": "customResponseValue"
                        },
                        "customRequestHeaders": {
                            "customRequestHeader": "customRequestValue"
                        }
                    }
                }]
            }
        ],
        "providers": [
            {
                "name": "file",
                "directory": "./dynamic-config",
                "watch": false
            }
        ]
    }
    ```

    In this case, the referenced directory `./dynamic-config/` must contain at least one subdirectory (with any name). This subdirectory contains the other JSON files for configuring `routers`, `middlewares`, and `services`, e.g.:

    ```text
    uniport-gateway/server/src/main/resources/
    |-- uniport-gateway
    |   |-- dynamic-config
    |   |   |-- auth
    |   |   |   `-- config.json
    |   |   `-- general
    |   |       `-- config.json
    |   `-- uniport-gateway.json
    ```

    If there are multiple subdirectories with JSON files, JSON arrays under the same key are concatenated instead of overwritten. This allows configurations to be clearly structured. With the folder structure from the example above, routers can be defined and used simultaneously in `auth/config.json` and `general/config.json`.

#### Entrypoints

Entrypoints are the network entry points of Portal-Gateway. They define the port where packets are received. Entrypoints are part of the Static Configuration.

| Variable | Required | Type | Description |
| --- | --- | --- | --- |
| `name` | Yes | String | Name of the entrypoint |
| `port` | Yes | Integer | Port number |
| `sessionDisabled` | No | Boolean | Disables session handling (Default: `false`). As of version `8.0.0`, this variable **MUST NOT** be set anymore. Session is not active by default, it can only be activated if the Session Middleware is explicitly declared. |
| `middlewares` | No | List of [middlewares](../04-customization/index.md#entry-middlewares) | Middlewares can be attached to each entrypoint, which are first processed before a request is forwarded to the route-specific middlewares. |

#### Applications

Applications define the core logic of Portal-Gateway. They define which applications listen on which ports and paths.

| Variable                    | Required | Type   | Description                                                                         |
| --------------------------- | -------- | ------ | ----------------------------------------------------------------------------------- |
| `name`                      | Yes      | String | Name of the application                                                             |
| `entrypoint`                | Yes      | String | Reference to an existing entrypoint to listen for requests                          |
| `requestSelector.urlPrefix` | Yes      | String | Path that the application listens on (TODO Status: not implemented, default is `/`) |
| `provider`                  | Yes      | String | Class name of the application that processes requests                               |

#### Providers

The Portal-Gateway can be configured via Providers.

The following Provider types are supported:

- File (name = `file`)
- Docker Container (name = `docker`)

##### File Provider

The Portal-Gateway can be configured using a JSON file via the File Provider. Configuration via a single file or multiple files is supported.

!!! hint "Keep it DRY"

    The File Provider can be used for the reuse of configurations.

| Variable | Required | Type | Default | Description |
| --- | --- | --- | --- | --- |
| `name` | Yes | String | - | Type of the Provider, here `file` |
| `filename` | Yes or `directory` | String | - | Defines the path to a configuration file |
| `directory` | Yes or `filename` | String | - | Defines the path to the directory which contains the configuration files. It is important to understand how multiple configuration files are merged: In general, with a deep-merge (recursive) JSON objects are matched within the existing structure and all matching entries are replaced. JsonArrays are treated like any other entry, i.e., completely replaced. This pattern is applied to all files that are in the same directory. For more complex configurations, we offer a merge mechanism over subdirectories. Subdirectories are largely merged in the same way as described above, with the exception of JsonArrays. JsonArrays are concatenated without duplicates. The names of the subdirectories do not matter and can be used for organizational purposes. |
| `watch` | Yes | Boolean | - | Set watch option to `true` to automatically react to file changes. |

##### Docker Provider

With Docker, Container Labels can be used to configure routing in the Portal-Gateway.

| Variable | Required | Type | Default | Description |
| --- | --- | --- | --- | --- |
| `endpoint` | No | String | `unix:///var/run/docker.sock` | The Uniport-Gateway needs access to the Docker Socket to read the dynamic configuration. The Docker API endpoint can be defined via this variable. |
| `exposedByDefault` | No | Boolean | `true` | Exposes the container by default via the Uniport-Gateway. If set to `false`, containers without the `portal.enable=true` label are ignored. |
| `network` | No | String | `""` | Defines the Default Network that is used for connecting with the containers. |
| `defaultRule` | No | String | `Host('${name}')` | Defines which routing rule is applied to the container if the container does not define one. The rule must be a valid [StringSubstitutor](https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/StringSubstitutor.html). The Container Service Name can be retrieved via the `${name}` variable and the StringSubstitutor has access to any Labels that are defined for this container. |

###### IP/Port Detection

The private IP and port of a container are queried from the Docker API.

Network and IP selection works as follows:

The port is chosen as follows:

- If the container exposes **no** port, the container is ignored.
- If the container exposes **one** port, this port is used.
- If the container exposes **multiple** ports, the port must be manually set with the label `portal.http.service.<service-name>.server.port`.

!!! warning "Security Notice"

    Access to the Docker API without restrictions is [not recommended](https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html#rule-1-do-not-expose-the-docker-daemon-socket-even-to-the-containers) according to OWASP. If the Portal-Gateway is attacked, the attacker can gain access to the underlying host:

    ```text
    [...] only trusted users shouls be allowed to control your Docker dameon [...]
    ```
    [Source: Docker Daemon Attack Surface documentation](https://docs.docker.com/engine/security/#docker-daemon-attack-surface)

    !!! success "Solution"

        The Docker Socket can also be exposed via SSH. SSH is supported with [Docker > 18.09](https://docs.docker.com/engine/security/protect-access/).

##### Configuration Interval

Status: Functionality is available, but cannot yet be configured (the Default is used).

| Variable                    | Required | Type    | Default | Description                                                                               |
| --------------------------- | -------- | ------- | ------- | ----------------------------------------------------------------------------------------- |
| `providersThrottleDuration` | No       | Integer | `2000`  | Interval in milliseconds in which the configuration should be re-read from the Providers. |

In some cases, some Providers can publish many configuration changes at once. This would generate more change events in the Portal-Gateway than necessary. To circumvent this problem, this option can be set. It defines how long the Portal-Gateway waits after a re-configuration before it makes new changes. If multiple change events are registered during this time, only the most current one is considered and the rest ist ignored. This option can only be set globally for all Providers, but is applied individually for each Provider.

##### Provider Namespace

When specific resources are declared in the dynamic configuration, e.g., Middlewares and Services, they are located in their Provider's namespace. For example, if a Middleware is declared with a Docker label, the Middleware is in the Docker Provider's namespace.

If multiple Providers are used and such a resource is to be referenced that was declared by another Provider (e.g., a cross-Provider resource like a Middleware), the resource should be provided with the separator "@" and the Provider name.

```text
<resource-name>@<provider-name>
```
