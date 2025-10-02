# Step 2 - Telemetry with Tracing & Logging

## Run



```bash
sed -i '' -e 's/step[0-9]/step2/g' ../docker-compose.yml
docker compose up
```

## Background

Entry Middleware Documentation: <https://uniport-gateway.netlify.app/04-customization/#entry-middlewares>

Entrypoint with `openTelemetry` and `requestResponseLogger` middlewares:

```json
{
    "entrypoints": [
        {
            "name": "http20000",
            "port": 20000,
            "middlewares": [
                {
                    "name": "openTelemetry",
                    "type": "openTelemetry"
                },
                {
                    "name": "requestResponseLogger",
                    "type": "requestResponseLogger",
                    "options": {
                        "uriWithoutLoggingRegex": "/health.*|/ready.*",
                        "contentTypes": [
                        "text/plain",
                        "application/json",
                        "application/x-www-form-urlencoded"
                        ]
                    }
                }
            ]
        }
    ],
    "providers": [
        // [..]
    ]
}
```

Log:

```text
2025-04-10 12:57:03,128 DEBUG [-/-/3d689f8e6e63e518f8c21af6d8dd39af] c.i.p.g.p.m.proxy.ProxyMiddleware:155 - handleProxyRequest: lowercasing the host header name
```

Outgoing request HTTP header

```text
Traceparent: 00-3d689f8e6e63e518f8c21af6d8dd39af-807056b782f2d161-01
```

Outgoing response HTTP header:

```text
X-Uniport-Trace-Id: 3d689f8e6e63e518f8c21af6d8dd39af
```
