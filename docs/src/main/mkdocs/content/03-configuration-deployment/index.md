# Configuration & Deployment

The Portal-Gateway has two different types of configuration, a `static` configuration and `dynamic` configurations:

- The `static` configuration is the minimal configuration needed to start the Portal-Gateway and cannot be changed at runtime. It consists of `entrypoints` and `providers`.
- The `dynamic` configuration configures `routers`, `middlewares` and `services`. It can be dynamically updated and applied at runtime.

The simplest `provider` is the `file` provider. It reads the configuration from a JSON file and searches at the following locations:

1. File pointed at by the environment variable `PORTAL_GATEWAY_JSON`
2. File pointed at by the system property `PORTAL_GATEWAY_JSON`
3. File `uniport-gateway.json` in the `/etc/uniport-gateway/default/` directory
4. File `uniport-gateway.json` in the current working directory

---

## Configuration

The Uniport-Gateway container image already includes a `uniport-gateway.json` file in the `/etc/uniport-gateway/default/` directory. We recommend storing your project-specific `uniport-gateway.json` file in a different directory (e.g., `/etc/uniport-gateway/<PROJECTNAME>/`) and setting the `PORTAL_GATEWAY_JSON` environment variable to this path.

| Variable                                    | Description                                                                                                                                                  | Default                                                                                                |
| :------------------------------------------ | :----------------------------------------------------------------------------------------------------------------------------------------------------------- | :----------------------------------------------------------------------------------------------------- |
| `PORTAL_GATEWAY_JSON`                       | Absolute path of the JSON file for static configuration                                                                                                      | -                                                                                                      |
| `PORTAL_GATEWAY_PUBLIC_PROTOCOL`            | HTTP protocol for the URL that a browser should use                                                                                                          | http                                                                                                   |
| `PORTAL_GATEWAY_PUBLIC_HOSTNAME`            | HTTP Host Name for the URL that a browser should use                                                                                                         | portal.minikube                                                                                        |
| `PORTAL_GATEWAY_PUBLIC_PORT`                | HTTP Port for the URL that a browser should use                                                                                                              | http: 80, https: 443                                                                                   |
| `PORTAL_GATEWAY_LOG_LEVEL`                  | Log Level: TRACE, DEBUG, INFO, WARN, ERROR, ALL or OFF                                                                                                       | INFO                                                                                                   |
| `PORTAL_GATEWAY_LOGGING_CONFIG`             | Absolute path of the `logback.xml` file for Logback configuration                                                                                            | /etc/uniport-gateway/logback.xml                                                                       |
| `PORTAL_GATEWAY_STRUCTURAL_LOGGING_ENABLED` | Log output is formatted as structured JSON.                                                                                                                  | false                                                                                                  |
| `PORTAL_GATEWAY_BEARER_TOKEN_PUBLIC_KEY`    | Path or URL                                                                                                                                                  | http://portal-iam:8080/auth/realms/portal                                                              |
| `PORTAL_GATEWAY_BEARER_TOKEN_ISSUER`        | Describes the expected issuer in the access token. See `issuer` in [bearerOnly](../04-customization/index.md#beareronly)                                     | [http://ips.inventage.com:20000/auth/realms/portal](http://ips.inventage.com:20000/auth/realms/portal) |
| `PORTAL_GATEWAY_BEARER_TOKEN_OPTIONAL`      | Indicates whether the proxy should expect (and thus validate) an access token. See `optional` in [bearerOnly](../04-customization/index.md#beareronly)       | false                                                                                                  |
| `PORTAL_GATEWAY_METRICS_PORT`               | HTTP Port for the Metrics Endpoint                                                                                                                           | 9090                                                                                                   |
| `PORTAL_GATEWAY_METRICS_PATH`               | HTTP Path for the Metrics Endpoint                                                                                                                           | /metrics                                                                                               |
| `PORTAL_GATEWAY_CLUSTERED`                  | Switch to run the Uniport-Gateway in cluster mode. This also requires adjusting the number of deployed instances (e.g., `replicas` in the Helm Value Files). | false                                                                                                  |

The three variables `PORTAL_GATEWAY_PUBLIC_PROTOCOL`, `PORTAL_GATEWAY_PUBLIC_HOSTNAME`, and `PORTAL_GATEWAY_PUBLIC_PORT` collectively form the public URL that the Portal-Gateway uses for redirects. This applies to redirects from the OAuth2 Middleware to Portal-IAM.

---

### Default portal-gateway.json

The `uniport-gateway.json` file in the `/etc/uniport-gateway/default/` directory serves as an example for the Uniport-Gateway's configuration.

---

### Upstreams

| Variable                              | Description                              | Default                   |
| :------------------------------------ | :--------------------------------------- | :------------------------ |
| `PORTAL_GATEWAY_PORTAL_IAM_HOST`      | Hostname to access Portal-IAM via HTTP   | portal-iam                |
| `PORTAL_GATEWAY_PORTAL_IAM_PORT`      | Port to access Portal-IAM via HTTP       | 8080                      |
| `PORTAL_GATEWAY_BASE_HOST`            | Hostname to access Base via HTTP         | base-proxy                |
| `PORTAL_GATEWAY_BASE_PORT`            | Port to access Base via HTTP             | 20010                     |
| `PORTAL_GATEWAY_DASHBOARD_HOST`       | Hostname to access Dashboard via HTTP    | dashboard-proxy           |
| `PORTAL_GATEWAY_DASHBOARD_PORT`       | Port to access Dashboard via HTTP        | 20020                     |
| `PORTAL_GATEWAY_ORGANISATION_HOST`    | Hostname to access Organization via HTTP | organisation-proxy        |
| `PORTAL_GATEWAY_ORGANISATION_PORT`    | Port to access Organization via HTTP     | 20030                     |
| `PORTAL_GATEWAY_FILESTORAGE_HOST`     | Hostname to access Filestorage via HTTP  | filestorage-proxy         |
| `PORTAL_GATEWAY_FILESTORAGE_PORT`     | Port to access Filestorage via HTTP      | 20090                     |
| `PORTAL_GATEWAY_CONTENT_HOST`         | Hostname to access Content via HTTP      | content-proxy             |
| `PORTAL_GATEWAY_CONTENT_PORT`         | Port to access Content via HTTP          | 20110                     |
| `PORTAL_GATEWAY_PORTAL_KAFKA_UI_HOST` | Hostname to access Kafka-UI via HTTP     | portal-kafka-ui           |
| `PORTAL_GATEWAY_PORTAL_KAFKA_UI_PORT` | Port to access Kafka-UI via HTTP         | 80                        |
| `PORTAL_GATEWAY_PORTAL_PGADMIN_HOST`  | Hostname to access PgAdmin via HTTP      | portal-pgadmin            |
| `PORTAL_GATEWAY_PORTAL_PGADMIN_PORT`  | Port to access PgAdmin via HTTP          | 80                        |
| `PORTAL_GATEWAY_PORTAL_GRAFANA_HOST`  | Hostname to access Grafana via HTTP      | portal-monitoring-grafana |
| `PORTAL_GATEWAY_PORTAL_GRAFANA_PORT`  | Port to access Grafana via HTTP          | 3000                      |

---

### Logging

The log level can also be configured individually at the package level if the global log level is insufficient. The Portal-Gateway uses [logback](https://github.com/qos-ch/logback) as its logging backend, allowing logging behavior to be configured via the `logback.xml` file. To adjust the log level at the package level, the `logback.xml` file must be [extended with a logger](https://logback.qos.ch/manual/configuration.html#loggerElement).

!!! example "Setting the Log Level for `com.inventage`"

    `<logger name="com.inventage" level="DEBUG"/>`

Analogous to the Uniport-Gateway configuration, there is a default Logback configuration under `/etc/uniport-gateway/logback.xml`. To use an extended `logback.xml` file, it is recommended to overwrite the default configuration. Alternatively, an additional file can be created, and the `PORTAL_GATEWAY_LOGGING_CONFIG` environment variable can be set to its path.

---

### OpenTelemetry

We rely on [OpenTelemetry SDK Autoconfiguration](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure). Listed below are properties whose default values may differ from [OpenTelemetry SDK Autoconfiguration](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure).

| Variable                | Description                                            | Default                                           |
| :---------------------- | :----------------------------------------------------- | :------------------------------------------------ |
| `OTEL_SERVICE_NAME`     | Service name that may be included in the exported data | Depends on the component (e.g., `portal-gateway`) |
| `OTEL_TRACES_EXPORTER`  | Comma-separated list of exporters for tracing          | `none`                                            |
| `OTEL_METRICS_EXPORTER` | Exporter for metrics                                   | `none`                                            |

---

## Deployment

The Portal-Gateway can also use environment variables in its configuration files. If this is the case, these must be defined during deployment. This is done using the `portal-gateway.common.env` file in a Docker environment and the `values.yaml` file in a Kubernetes environment.

---

### Observability

#### Traces

Tracing data is exported to the OpenTelemetry Collector. The following spans are created:

- Span Kind = `server` for incoming Request/Response
- Span Kind = `client` for outgoing Request/Response

If the `openTelemetry` middleware component is configured as an entrypoint middleware, the HTTP header `X-Uniport-Trace-Id` will be set in every HTTP response returned to the client. The value will contain the OpenTelemetry Trace-Id.

---

#### Metrics

The [Vert.x Micrometer Implementation](https://vertx.io/docs/vertx-micrometer-metrics/java/) is used for providing metrics. It is based on [Micrometer](http://micrometer.io/) and provides two types of metrics:

- [Vert.x Core](https://vertx.io/docs/vertx-micrometer-metrics/java/#_vert_x_core_tools_metrics)
- [Vert.x Pool](https://vertx.io/docs/vertx-micrometer-metrics/java/#_vert_x_pool_metrics)

---

#### Logs

Log entries are exported via Promtail to the OpenTelemetry Collector.
