# Cheat Sheets

## Kubernetes

Especially with deployments where the pod name changes with every restart, it is tedious to always either find out the new pod name or to hope for command line completion features. There is a `kubectl` plugin called [stern](https://github.com/stern/stern) that can tail multiple pods, handle multiple containers in the same pod, and also works across pod restarts/scaling.

!!! example "Using stern"

    ```bash
    $ kubectl stern uniport-gateway
    ```

---

## Structured Logs

For monitoring purposes, some microservices log structured log statements. This simplifies the parsing of log statements in a monitoring instance, but makes reading raw logs rather tiring for a developer. To better read structured logs, it is recommended to use [jq](https://github.com/jqlang/jq) for parsing log statements. The same tool can also be used for filtering log statements.

!!! tip "Combination of stern and jq"

    These two tools can be combined excellently.

!!! example "Example of using jq"

    Before:

    ```bash
    $ kubectl stern uniport-gateway
    {"timestamp":"2023-09-05 14:39:14,096","level":"DEBUG","logger":"ch.uniport.gateway.proxy.middleware.log.RequestResponseLoggerMiddleware","line":"142","method":"logResponseDebug","message":"requestResponseLogger outgoing Response '\"GET /readiness \" 500 Internal Server Error 1003 \" in 1003 ms \"' \nHeaders 'connection: close - content-length: 147 - Content-Security-Policy: style-src https://fonts.gstatic.com 'unsafe-inline' 'self' https://cdn.jsdelivr.net https://fonts.googleapis.com; connect-src 'self' https://fonts.googleapis.com; script-src 'unsafe-inline' 'self' blob: https://cdn.jsdelivr.net 'unsafe-eval'; img-src 'self' https: data:; report-to /csp-reports; default-src 'self'; report-uri /csp-reports; font-src https://fonts.gstatic.com 'self' https://cdn.jsdelivr.net https://fonts.googleapis.com data: - content-type: application/json;charset=UTF-8 - set-cookie: uniport.session-lifetime=1693919353; Path=/ - X-Uniport-Trace-Id: c6af96c98d473f1c899fdad71d28fb1b - '","userId":"","sessionId":"","traceId":"c6af96c98d473f1c899fdad71d28fb1b","stacktrace":""}
    ```

    After:

    ```bash
    $ kubectl stern uniport-gateway | jq -R "fromjson?"
    {
        "timestamp": "2023-09-05 14:39:14,096",
        "level": "DEBUG",
        "logger": "ch.uniport.gateway.proxy.middleware.log.RequestResponseLoggerMiddleware",
        "line": "142",
        "method": "logResponseDebug",
        "message": "requestResponseLogger outgoing Response '\"GET /readiness \" 500 Internal Server Error 1003 \" in 1003 ms \"' \nHeaders 'connection: close - content-length: 147 - Content-Security-Policy: style-src https://fonts.gstatic.com 'unsafe-inline' 'self' https://cdn.jsdelivr.net https://fonts.googleapis.com; connect-src 'self' https://fonts.googleapis.com; script-src 'unsafe-inline' 'self' blob: https://cdn.jsdelivr.net 'unsafe-eval'; img-src 'self' https: data:; report-to /csp-reports; default-src 'self'; report-uri /csp-reports; font-src https://fonts.gstatic.com 'self' https://cdn.jsdelivr.net https://fonts.googleapis.com data: - content-type: application/json;charset=UTF-8 - set-cookie: uniport.session-lifetime=1693919353; Path=/ - X-Uniport-Trace-Id: c6af96c98d473f1c899fdad71d28fb1b - '",
        "userId": "",
        "sessionId": "",
        "traceId": "c6af96c98d473f1c899fdad71d28fb1b",
        "stacktrace": ""
    }
    ```
