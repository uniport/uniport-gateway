# API

## Backend

### Middlewares

Das Portal-Gateway bietet die Möglichkeit via folgenden `middlewares` Typen das Verhalten des Gateway zu konfigurieren.

#### Control API - controlApi

Die `controlApi` Middleware analysiert die Header der Antworten der weitergeleiteten HTTP Anfragen um vordefinierte Aktionen auszuführen. Das Control API bietet folgenden Aktionen:

| Aktion              | Beschreibung                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| ------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `SESSION_TERMINATE` | Diese Aktion ruft den `end_session_endpoint` des verwendeten Identity Providers auf um dort die Session zu beenden. Parallel dazu wird die Session auf dem Gateway selbst gelöscht. _Bemerkung_: Der `end_session_endpoint` wird vom Identity Provider zur Verfügung gestellt und via `OpenID Connect Discovery` abgefragt. Beispiel für einen Logout Request: `http://portal-iam:8080/auth/realms/portal/protocol/openid-connect/logout?id_token_hint=eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ1Y0p4dWNXRDF...` |
| `SESSION_RESET`     | Diese Aktion entfernt alle Session Scopes (JWTs) und entfernt alle Cookies vom Session Bag des eingeloggten Benutzers, die nicht mit `KEYCLOAK_` in Namen anfangen. Mit dieser Aktion kann der Benutzer zwischen mehreren Organisationen wechseln, bei denen er verschiedene Rollen besitzt, ohne sich ausloggen zu müssen.                                                                                                                                                                                                |

Eine Aktion wird wie folgt konfiguriert:

```json

...
"middlewares": [
        ...
 {
    "name": "sessionTerminate",
    "type": "controlApi",
    "options": {
        "action": "SESSION_TERMINATE"
    }
}
        ...
]
```

Konfiguration des Headers:

Um eine Aktion zu triggern, muss auf der Antwort ein Cookie mit dem Key `IPS_GW_CONTROL` und der Aktion als Value z.B. `SESSION_TERMINATE` gesetzt sein. Es können auch mehrere Control API Cookies gesetzt werden. Nach der Verarbeitung werden diese Cookie aus der Session gelöscht.

!!! example "Beispiel"

    ```shell
    < HTTP/1.1 303 See Other
    < Set-Cookie: IPS_GW_CONTROL=SESSION_TERMINATE;Version=1
    < Content-Length: 0
    < Location: https://portal.com/cms/logged_out
    ```

## Monitoring

Über den Port 9090 unter `/metrics` werden verschiedenste Metriken bereitgestellt:

```text
# HELP vertx_http_client_responses_total Response count with codes
# TYPE vertx_http_client_responses_total counter
vertx_http_client_responses_total{code="200",method="POST",} 223.0
vertx_http_client_responses_total{code="404",method="GET",} 110.0
vertx_http_client_responses_total{code="204",method="POST",} 15.0
vertx_http_client_responses_total{code="303",method="GET",} 9.0
vertx_http_client_responses_total{code="304",method="GET",} 17.0
vertx_http_client_responses_total{code="200",method="GET",} 1371.0
# HELP vertx_pool_in_use Number of resources used
# TYPE vertx_pool_in_use gauge
vertx_pool_in_use{pool_type="worker",} 0.0
# HELP vertx_http_server_active_connections Number of opened connections to the server
# TYPE vertx_http_server_active_connections gauge
vertx_http_server_active_connections 0.0
# HELP vertx_http_client_errors_total Number of errors
# TYPE vertx_http_client_errors_total counter
vertx_http_client_errors_total 7.0
# HELP process_cpu_usage The "recent cpu usage" for the Java Virtual Machine process
# TYPE process_cpu_usage gauge
process_cpu_usage 8.968609865470852E-4
# HELP jvm_memory_committed_bytes The amount of memory in bytes that is committed for the Java virtual machine to use
# TYPE jvm_memory_committed_bytes gauge
jvm_memory_committed_bytes{area="heap",id="Tenured Gen",} 3.4013184E7
jvm_memory_committed_bytes{area="nonheap",id="CodeHeap 'profiled nmethods'",} 1.605632E7
jvm_memory_committed_bytes{area="heap",id="Eden Space",} 1.3893632E7
jvm_memory_committed_bytes{area="nonheap",id="Metaspace",} 4.0501248E7
jvm_memory_committed_bytes{area="nonheap",id="CodeHeap 'non-nmethods'",} 2555904.0
jvm_memory_committed_bytes{area="heap",id="Survivor Space",} 1703936.0
jvm_memory_committed_bytes{area="nonheap",id="Compressed Class Space",} 4521984.0
jvm_memory_committed_bytes{area="nonheap",id="CodeHeap 'non-profiled nmethods'",} 9830400.0
```
