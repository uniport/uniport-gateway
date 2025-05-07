# Migration Guide

## `9.*.*` -> `10.*.*`

### Environment variable

The following environment variables are converted to screaming snake-case.

* `development` to `DEVELOPMENT`
* `verticle.instances` to `VERTICLE_INSTANCES`

### Static Configuration

The configuration key `applications` in the static configuration was removed without replacement. It is no longer required. Please remove this entry in your configuration.

### Language Cookie Middleware

The detection of the deprecated language cookie name `ips.language` was removed. Please use its replacement `uniport.language` or configure the language cookie middleware accordingly.

## Session Middleware

* Rename the configuration key `clusteredSessionStoreRetryTimeoutInMiliseconds` to `clusteredSessionStoreRetryTimeoutInMilliseconds`.
* Rename the configuration key `idleTimeoutInMinute` to `idleTimeoutInMinutes`.

## `9.*.*` -> `9.3.*`

### Clustered Portal-Gateway

With the upgrade to vertx 4.5.8, the hazelcast version was updated from 4.2 to 5.3. In general, the updated Portal-Gateway instance can be deployed in a rolling
update. However, if the following error message appears, stop all instance and start them again. **Caution**: This clears the session store.

```json
{
  "timestamp": "2024-06-04 15:19:48,242",
  "level": "ERROR",
  "logger": "com.hazelcast.security",
  "line": "69",
  "method": "log",
  "message": "[10.244.4.210]:5701 [portal-gateway-ha] [5.3.5] Reason of failure for node join: Joining node's version 5.3.5 is not compatible with cluster version 4.2 [...]",
}
```

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
