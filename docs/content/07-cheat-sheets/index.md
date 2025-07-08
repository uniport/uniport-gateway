# Cheat Sheets

## Kubernetes

Gerade bei Deployments, wo der Pod Name mit jedem Restart wechselt, ist es mühsam immer entweder den neuen Pod Namen herauszufinden oder auf Command Line Completion Features zu hoffen. Es gibt ein `kubectl` Plugin names [stern](https://github.com/stern/stern), das mehrere Pods tailen kann, mit mehreren Contrainer im selben Pod umgehen kann und auch über Pod restarts/scaling funktioniert.

!!! example "Beispiel einer Anwendung von stern"

    ```bash
    $ kubectl stern portal-gateway
    ```

## Structured Logs

Zu Monitoring-Zwecken loggen einige Microservices structured Log Statement. Dies vereinfacht, das Parsen von Log Statements in einer Monitoring Instanz, macht das Lesen der Logs in seiner rohen Form für einen Entwickler eher müde Augen. Um structured Logs besser lesen zu können, empfiehlt sich die Verwendung von [jq](https://github.com/jqlang/jq) für das Parsen der Logs Statements. Das selbe Tool kann auch für das Filtern von Log Statement verwendet werden.

!!! hint "Kombination stern und jq"

    Die beiden Tools lassen sich hervorragend kombinieren.

!!! example "Beispiel einer Anwendung von jq"

    Vorher:

    ```bash
    $ kubectl stern portal-gateway
    {"timestamp":"2023-09-05 14:39:14,096","level":"DEBUG","logger":"com.inventage.portal.gateway.proxy.middleware.log.RequestResponseLoggerMiddleware","line":"142","method":"logResponseDebug","message":"requestResponseLogger outgoing Response '\"GET /readiness \" 500 Internal Server Error 1003 \" in 1003 ms \"' \nHeaders 'connection: close - content-length: 147 - Content-Security-Policy: style-src https://fonts.gstatic.com 'unsafe-inline' 'self' https://cdn.jsdelivr.net https://fonts.googleapis.com; connect-src 'self' https://fonts.googleapis.com; script-src 'unsafe-inline' 'self' blob: https://cdn.jsdelivr.net 'unsafe-eval'; img-src 'self' https: data:; report-to /csp-reports; default-src 'self'; report-uri /csp-reports; font-src https://fonts.gstatic.com 'self' https://cdn.jsdelivr.net https://fonts.googleapis.com data: - content-type: application/json;charset=UTF-8 - set-cookie: uniport.session-lifetime=1693919353; Path=/ - X-Uniport-Trace-Id: c6af96c98d473f1c899fdad71d28fb1b - '","userId":"","sessionId":"","traceId":"c6af96c98d473f1c899fdad71d28fb1b","stacktrace":""}
    ```

    ```bash
    $ kubectl stern portal-gateway | jq -R "fromjson?"
    {
        "timestamp": "2023-09-05 14:39:14,096",
        "level": "DEBUG",
        "logger": "com.inventage.portal.gateway.proxy.middleware.log.RequestResponseLoggerMiddleware",
        "line": "142",
        "method": "logResponseDebug",
        "message": "requestResponseLogger outgoing Response '\"GET /readiness \" 500 Internal Server Error 1003 \" in 1003 ms \"' \nHeaders 'connection: close - content-length: 147 - Content-Security-Policy: style-src https://fonts.gstatic.com 'unsafe-inline' 'self' https://cdn.jsdelivr.net https://fonts.googleapis.com; connect-src 'self' https://fonts.googleapis.com; script-src 'unsafe-inline' 'self' blob: https://cdn.jsdelivr.net 'unsafe-eval'; img-src 'self' https: data:; report-to /csp-reports; default-src 'self'; report-uri /csp-reports; font-src https://fonts.gstatic.com 'self' https://cdn.jsdelivr.net https://fonts.googleapis.com data: - content-type: application/json;charset=UTF-8 - set-cookie: uniport.session-lifetime=1693919353; Path=/ - X-Uniport-Trace-Id: c6af96c98d473f1c899fdad71d28fb1b - '",
        "userId": "",
        "sessionId": "",
        "traceId": "c6af96c98d473f1c899fdad71d28fb1b",
        "stacktrace": ""
    }
    ```
