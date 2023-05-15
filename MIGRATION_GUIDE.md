# Migration Guide

## `7.*.*` -> `8.*.*`

### Session cookie
Breaking change if any of your client is dependent on the (now deprecated) session cookie name by default: `inventage-portal-gateway.session`. The new default for the session cookie name is: `uniport.session`.

Concretely: Change any occurrence of the old session cookie name to `uniport.session`.

### `sessionDisabled`
Breaking change if any of your static configuration file contains `sessionDisabled` in one of its entrypoint. Sessions are disabled per default and can be explicitly enabled by setting the session-middleware.

Concretely: Delete any occurrence of `sessionDisabled` in your static configuration file.

## `6.*.*` -> `7.*.*`

Breaking change if `bearerOnly` middleware is configured in your portal-gateway instance. From version `7.0.0+`, `bearerOnly` middleware expects in its `options`, an `publicKeys` key, instead of `publicKey` and `publicKeyAlgorithm`. The `publicKeys` arrays contains objects, and each object contains the keys `publicKey` and `publicKeyAlgorithm`. The underlying functionality remains the same, however it is now possible to allow multiple public keys for signature verification. Additionally, now all available public keys used for signing of a realms are loaded instead of only the main one.

Concretely: Change your `bearerOnly` configuration in your dynamic configuration file as follows:

Old:
```json
{
    "name": "...",
    "type": "bearerOnly",
    "options": {
        "publicKey": "http://portal-iam:8080/auth/realms/portal",
        "publicKeyAlgorithm": "RS256",
        "audience": ["..."],
        "issuer": "...",
        "optional": "...",
        "claims": ["..."]
    }
}
```

New:
```json
{
    "name": "...",
    "type": "bearerOnly",
    "options": {
        "publicKeys": [
            {
                "publicKey": "http://portal-iam:8080/auth/realms/portal",
                "publicKeyAlgorithm": "RS256"
            }
        ],
        "audience": ["..."],
        "issuer": "...",
        "optional": "...",
        "claims": ["..."]
    }
}
```

## `5.*.*` -> `6.*.*`

Breaking change if `sessionBag`-middleware is configured in your portal-gateway instance. From version `6.0.0+`, `sessionBag`-middleware needs to be configured in `portal-gateway.json` as `entryMiddleware`.

Concretely: Copy your `sessionBag` configuration from your dynamic configuration file and paste it in your `portal-gateway.json` file as the LAST entrymiddleware.

As example:
```json
{
    "entrypoints": [
        {
            "name": "...",
            "port": "...",
            "middlewares": [
                "...",
                {
                    "name": "...",
                    "type": "sessionBag",
                    "options": {
                        "whitelistedCookies": "..."
                    }
                }
            ]
        },
        "..."
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
            "name": "...",
            "port": "...",
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
        "..."
    ]
}
```
With this change, portal-gateway performs session handling identical to that in version `4.*.*`
