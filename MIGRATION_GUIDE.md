# Migration Guide

## `5.*.*` -> `6.*.*`

Breaking change if `sessionBag`-middleware is configured in your portal-gateway instance. From version `6.0.0+`, `sessionBag`-middleware needs to be configured in `portal-gateway.json` as `entryMiddleware`.

Concretely: Copy your `sessionBag` configuration from your dynamic configuration file and paste it in your `portal-gateway.json` file as the LAST entrymiddleware.

For example:
```json
{
    "entrypoints": [
        {
            "name": ...,
            "port": ...,
            "middlewares": [
                ...,
                {
                    "name": ...,
                    "type": "sessionBag",
                    "options": {
                        "whitelistedCookies": ...
                    }
                }
            ]
        },
        ...
    ]
}
```

## `4.*.*` → `5.*.*`

Session handling is not enabled per default. Needs to be explicitly configured in `portal-gateway.json`.

### Step-by-Step

To enable session handling in your portal-gateway instance you need to explicitly declare them in
the `portal-gateway.json` configuration file. The following changes need to be made:

```json
{
    "entrypoints": [
        {
            "name": ...,
            "port": ...,
            "middlewares": [
                {
                    "name": "responseSessionCookieRemoval",
                    "type": "responseSessionCookieRemoval",
                    "options": {}
                },
                {
                    "name": "session",
                    "type": "session",
                    "options": {}
                },
                {
                    "name": "requestResponseLogger",
                    "type": "requestResponseLogger",
                    "options": {}
                },
                {
                    "name": "replacedSessionCookieDetection",
                    "type": "replacedSessionCookieDetection",
                    "options": {}
                }
            ]
        },
        ...
    ]
}
```
With this change, portal-gateway performs session handling identical to that in version `4.*.*`



