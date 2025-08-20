# Step 8 - JWT Authorization

## Run

```bash
sed -i -E 's/step[0-9]+/step8/g' docker-compose.yml
docker compose -f auth/docker-compose-auth.yml up -d
docker compose up
```

Verify everything is [set up correctly](../auth/README.md).

Request an access token via the `Client Credentials Grant` (can alternatively also be an access token acquired by a user with the `oauth` middleware):

```bash
ACCESS_TOKEN="$(curl -sSf -X POST \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=client_credentials&client_id=exampleclient&client_secret=**********&scope=whoami2' \
  http://localhost:20000/auth/realms/testrealm/protocol/openid-connect/token \
  | jq --raw-output '.access_token')"
echo "$ACCESS_TOKEN"
```

Before we use the access token, lets check the protected routes first. <http://localhost:20000/whoami1> is unprotected and <http://localhost:20000/whoami2> is protected:

```bash
➜ curl -sSf localhost:20000/whoami1
Hostname: 9fa5025212a7
IP: 127.0.0.1
IP: ::1
IP: 172.27.0.2
RemoteAddr: 172.27.0.6:43640
GET /whoami1 HTTP/1.1
Host: whoami1
User-Agent: curl/8.15.0
Accept: */*
Traceparent: 00-376646c3aec3daab9d80ea91eea6c8fa-f8a310f922a584b6-01
X-Forwarded-For: 192.168.65.1:52902
X-Forwarded-Host: localhost:20000
X-Forwarded-Port: 20000
X-Forwarded-Proto: http

➜ curl -sSf localhost:20000/whoami2
curl: (22) The requested URL returned error: 401
```

Then issue an API call with the access token as the bearer token in the `Authorization` header:

```bash
curl -sSfv -H "Authorization: Bearer $ACCESS_TOKEN" localhost:20000/whoami2
```

## Background

Middleware Documentation: <https://uniport-gateway.netlify.app/04-customization/#middlewares>

The `bearerOnly` middleware ensures that JWT provided as bearer token in the Authorization header is

* has a valid signature and is signed with a trusted private key
* has a trusted issuer
* is intended for the uniport-gateway i.e. has a correct audience
* optionally, has matching custom claims i.e. authorization

Router with `bearerOnly` middleware:

```json
{
    "http": {
        "routers": [
            // [..]
        ],
        "middlewares": [
            // [..]
            {
                "name": "whoami2BearerOnly",
                "type": "bearerOnly",
                "options": {
                    "publicKeys": [
                        {
                            "publicKey": "http://keycloak:8080/auth/realms/testrealm"
                        }
                    ],
                    "audience": [
                        "testclient"
                    ],
                    "issuer": "http://localhost:20000/auth/realms/testrealm",
                    "optional": "false",
                    "claims": [
                        {
                            "claimPath": "$['resource_access']['whoami2']['roles']",
                            "operator": "CONTAINS",
                            "value": [
                                "ADMIN"
                            ]
                        }
                    ],
                    "publicKeysReconciliation": {
                        "enabled": true,
                        "intervalMs": 3600000
                    }
                }
            }
        ],
        "services": [
            // [..]
        ]
    }
}
```

`keycloak` is required to be configured to work with the example configuration of the `uniport-gateway`. The imported realm
configuration should take care of everything.

For the sake of completeness:

* `exampleclient` client
* on the `whoami2` client is a role named `ADMIN`
* on the `whoami2` client scope is a `User Client Role` mapper that maps the user roles to the `resource_access.${client_id}.roles` claim in an access token
* the `service-account-exampleclient` user has role mapping `whoami2`/`ADMIN` assigned
