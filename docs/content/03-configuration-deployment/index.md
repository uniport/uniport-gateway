# Configuration & Deployment

The Portal-Gateway has two different types of configuration, a `static` configuration and `dynamic` configurations:

- The `static` configuration is the minimal configuration needed to start the Portal-Gateway and cannot be changed at runtime. It consists of `entrypoints` and `providers`.
- The `dynamic` configuration configures `routers`, `middlewares` and `services`. It can be dynamically updated and applied at runtime.

The simplest `provider` is the `file` provider. It reads the configuration from a JSON file and searches at the following locations:

1. File pointed at by the environment variable `PORTAL_GATEWAY_JSON`
2. File pointed at by the system property `PORTAL_GATEWAY_JSON`
3. File `portal-gateway.json` in the `/etc/portal-gateway/default/` directory
4. File `portal-gateway.json` in the current working directory

## Konfiguration

Im Container Image von Portal-Gateway befindet sich im Verzeichnis `/etc/portal-gateway/default/` bereits eine `portal-gateway.json` Datei. Es wird empfohlen die projektspezifische `portal-gateway.json` Datei in einem anderen Verzeichnis (z.B. `/etc/portal-gateway/<PROJEKTNAME>/`) abzulegen und die `PORTAL_GATEWAY_JSON` Environment Variable auf diesen Pfad zu setzen.

| Variable                                    | Beschreibung                                                                                                                                                                | Default                                           |
| ------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------- |
| `PORTAL_GATEWAY_JSON`                       | Absoluter Pfad der JSON Datei für die statische Konfiguration                                                                                                               | -                                                 |
| `PORTAL_GATEWAY_PUBLIC_PROTOCOL`            | HTTP Protokoll für die URL, welche ein Browser verwenden soll                                                                                                               | http                                              |
| `PORTAL_GATEWAY_PUBLIC_HOSTNAME`            | HTTP Host Name für die URL, welche ein Browser verwenden soll                                                                                                               | portal.minikube                                   |
| `PORTAL_GATEWAY_PUBLIC_PORT`                | HTTP Port für die URL, welche ein Browser verwenden soll                                                                                                                    | http: 80, https: 443                              |
| `PORTAL_GATEWAY_LOG_LEVEL`                  | Log Level: TRACE, DEBUG, INFO, WARN, ERROR, ALL or OFF                                                                                                                      | INFO                                              |
| `PORTAL_GATEWAY_LOGGING_CONFIG`             | Absoluter Pfad der `logback.xml` Datei, für die Logback Konfiguration                                                                                                       | /etc/portal-gateway/logback.xml                   |
| `PORTAL_GATEWAY_STRUCTURAL_LOGGING_ENABLED` | Log Output wird als strukturierte JSON ausgegeben.                                                                                                                          | false                                             |
| `PORTAL_GATEWAY_BEARER_TOKEN_PUBLIC_KEY`    | Pfad oder URL                                                                                                                                                               | http://portal-iam:8080/auth/realms/portal         |
| `PORTAL_GATEWAY_BEARER_TOKEN_ISSUER`        | Beschreibt den erwarteten Issuer im Access Token. Siehe `issuer` in [bearerOnly](../customization/portal-gateway.md#beareronly)                                             | http://ips.inventage.com:20000/auth/realms/portal |
| `PORTAL_GATEWAY_BEARER_TOKEN_OPTIONAL`      | Gibt an, ob der Proxy ein Access Token erwarten (und somit überprüfen) sollte. Siehe `optional` in [bearerOnly](../customization/portal-gateway.md#beareronly)              | false                                             |
| `PORTAL_GATEWAY_METRICS_PORT`               | HTTP Port für den Metrics Endpoint                                                                                                                                          | 9090                                              |
| `PORTAL_GATEWAY_METRICS_PATH`               | HTTP Path für den Metrics Endpoint                                                                                                                                          | /metrics                                          |
| `PORTAL_GATEWAY_CLUSTERED`                  | Switch, um den Portal-Gateway im Cluster Modus laufen zu lassen. Dafür muss auch die Anzahl deployten Instanzen (z.B. `replicas` in den Helm Value Files) angepasst werden. | false                                             |

Die 3 Variablen `PORTAL_GATEWAY_PUBLIC_PROTOCOL`, `PORTAL_GATEWAY_PUBLIC_HOSTNAME` und `PORTAL_GATEWAY_PUBLIC_PORT` ergeben zusammen die öffentliche URL, welche vom Portal-Gateway für Redirects verwendet wird. Dies betrifft sowohl Redirects von der OAuth2 Middleware zum Portal-IAM, a

### Default portal-gateway.json

Die `portal-gateway.json` Datei im `/etc/portal-gateway/default/` Verzeichnis dient als Beispiel für die Konfiguration des Portal-Gateways.

### Upstreams

| Variable                              | Beschreibung                                       | Default                   |
| ------------------------------------- | -------------------------------------------------- | ------------------------- |
| `PORTAL_GATEWAY_PORTAL_IAM_HOST`      | Hostname, um via HTTP auf Portal-IAM zuzugreifen   | portal-iam                |
| `PORTAL_GATEWAY_PORTAL_IAM_PORT`      | Port, um via HTTP auf Portal-IAM zuzugreifen       | 8080                      |
| `PORTAL_GATEWAY_BASE_HOST`            | Hostname, um via HTTP auf Base zuzugreifen         | base-proxy                |
| `PORTAL_GATEWAY_BASE_PORT`            | Port, um via HTTP auf Base zuzugreifen             | 20010                     |
| `PORTAL_GATEWAY_DASHBOARD_HOST`       | Hostname, um via HTTP auf Dashboard zuzugreifen    | dashboard-proxy           |
| `PORTAL_GATEWAY_DASHBOARD_PORT`       | Port, um via HTTP auf Dashboard zuzugreifen        | 20020                     |
| `PORTAL_GATEWAY_ORGANISATION_HOST`    | Hostname, um via HTTP auf Organisation zuzugreifen | organisation-proxy        |
| `PORTAL_GATEWAY_ORGANISATION_PORT`    | Port, um via HTTP auf Organisation zuzugreifen     | 20030                     |
| `PORTAL_GATEWAY_FILESTORAGE_HOST`     | Hostname, um via HTTP auf Filestorage zuzugreifen  | filestorage-proxy         |
| `PORTAL_GATEWAY_FILESTORAGE_PORT`     | Port, um via HTTP auf Filestorage zuzugreifen      | 20090                     |
| `PORTAL_GATEWAY_CONTENT_HOST`         | Hostname, um via HTTP auf Content zuzugreifen      | content-proxy             |
| `PORTAL_GATEWAY_CONTENT_PORT`         | Port, um via HTTP auf Content zuzugreifen          | 20110                     |
| `PORTAL_GATEWAY_PORTAL_KAFKA_UI_HOST` | Hostname, um via HTTP auf Kafka-UI zuzugreifen     | portal-kafka-ui           |
| `PORTAL_GATEWAY_PORTAL_KAFKA_UI_PORT` | Port, um via HTTP auf Kafka-UI zuzugreifen         | 80                        |
| `PORTAL_GATEWAY_PORTAL_PGADMIN_HOST`  | Hostname, um via HTTP auf PgAdmin zuzugreifen      | portal-pgadmin            |
| `PORTAL_GATEWAY_PORTAL_PGADMIN_PORT`  | Port, um via HTTP auf PgAdmin zuzugreifen          | 80                        |
| `PORTAL_GATEWAY_PORTAL_GRAFANA_HOST`  | Port, um via HTTP auf Grafana zuzugreifen          | portal-monitoring-grafana |
| `PORTAL_GATEWAY_PORTAL_GRAFANA_PORT`  | Port, um via HTTP auf Grafana zuzugreifen          | 3000                      |

### Logging

Das Log-Level kann auch auf Package Ebene einzeln konfiguriert werden, falls das global Log Level nicht ausreicht. Der Portal-Gateway verwendet [logback](https://github.com/qos-ch/logback) als Logging Backend und somit kann das Logging-Verhalten mittels der `logback.xml` Datei konfiguriert werden. Um das Log-Level auf Package Ebene anzupassen, muss die `logback.xml` Datei mit [logger erweitert](https://logback.qos.ch/manual/configuration.html#loggerElement) werden.

!!! example "Setzen des Log Levels für `com.inventage`"

    `<logger name="com.inventage" level="DEBUG"/>`

Analog zur Portal-Gateway Konfiguration gibt es eine Default Logback Konfiguration unter `/etc/portal-gateway/logback.xml`. Um die ergänzte `logback.xml` Datei zu verwenden, wird empfohlen die Default Konfiguration zu überschreiben. Alternativ kann auch eine zusätzliche Datei angelegt werden und die `PORTAL_GATEWAY_LOGGING_CONFIG` Environment Variable auf diesen Pfad zu setzen.

### OpenTelemetry

Wir setzen auf [OpenTelemetry SDK Autoconfiguration](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure). Unten aufgeführt sind Properties, deren Default Wert von [OpenTelemetry SDK Autoconfiguration](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure) abweichen können.

| Variable                | Beschreibung                                                          | Default                                             |
| ----------------------- | --------------------------------------------------------------------- | --------------------------------------------------- |
| `OTEL_SERVICE_NAME`     | Service Name, der eventuell in den exportierten Daten mitgegeben wird | Hängt von der Komponente ab (z.B. `portal-gateway`) |
| `OTEL_TRACES_EXPORTER`  | Liste von Exporters für Tracing, mit Kommas separiert                 | `none`                                              |
| `OTEL_METRICS_EXPORTER` | Exporter für Metric                                                   | `none`                                              |

## Deployment

Der Portal-Gateway kann in seinen Konfigurationsfiles auch Environmentvariablen verwenden. Falls dies der Fall ist müssen diese bei einem Deployment definiert werden. Das geschieht mittels des `portal-gateway.common.env` Files in einer Docker Umgebung und mittels des `values.yaml` Files in einer Kubernetes Umgebung.

### Observability

#### Traces

Die Tracing Daten werden zum OpenTelemetry Collector exportiert. Es werden folgende Spans erstellt:

- Span Kind = `server` für eingehende Request/Response
- Span Kind = `client` für ausgehende Request/Response

Falls die `openTelemetry` Middleware Komponente als Entrypoint Middleware konfiguriert ist, so wird in jeder zum Client zurückgegebenen HTTP Antwort der HTTP Header `X-Uniport-Trace-Id` gesetzt. Als Wert ist die OpenTelemetry Trace-Id enthalten.

#### Metrics

Für die Bereitstellung der Metriken wird die [Vert.x Micrometer Implementation](https://vertx.io/docs/vertx-micrometer-metrics/java/) verwendet. Sie basiert auf [Micrometer](http://micrometer.io/) und stellt 2 Arten von Metriken zur Verfügung:

- [Vert.x Core](https://vertx.io/docs/vertx-micrometer-metrics/java/#_vert_x_core_tools_metrics)
- [Vert.x Pool](https://vertx.io/docs/vertx-micrometer-metrics/java/#_vert_x_pool_metrics)

#### Logs

Die Log Einträge werden via Promtail zum OpenTelemetry Collector exportiert.
