# API

## Backend

### Middlewares

The Uniport-Gateway offers the possibility to configure the behavior of the gateway via the following `middlewares` types.

#### Control API - controlApi

The `controlApi` middleware analyzes the headers of the responses from forwarded HTTP requests to execute predefined actions. The Control API offers the following actions:

| Action | Description |
| --- | --- |
| `SESSION_TERMINATE` | This action calls the `end_session_endpoint` of the used Identity Provider to terminate the session there. In parallel, the session on the Gateway itself is deleted. |
| `SESSION_RESET` | This action removes all session scopes (JWTs) and all cookies from the session bag of the logged-in user that do not start with `KEYCLOAK_` in their name. With this action, the user can switch between multiple organizations where they have different roles, without having to log out. |

!!! note

    The `end_session_endpoint` is provided by the Identity Provider and queried via `OpenID Connect Discovery`. Example for a logout request: `http://portal-iam:8080/auth/realms/portal/protocol/openid-connect/logout?id_token_hint=eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ1Y0p4dWNXRDF...` |

An action is configured as follows:

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

Header Specification:

To trigger an action, a cookie with the key `IPS_GW_CONTROL` and the action as its value, e.g., `SESSION_TERMINATE`, must be set in the response. Multiple Control API cookies can also be set. After processing, these cookies are deleted from the session.

!!! example

    ```shell
    < HTTP/1.1 303 See Other
    < Set-Cookie: IPS_GW_CONTROL=SESSION_TERMINATE;Version=1
    < Content-Length: 0
    < Location: https://portal.com/cms/logged_out
    ```

---

## Monitoring

Various metrics are provided via port 9090 under `/metrics`:

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
