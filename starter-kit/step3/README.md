# Step 3 - OIDC/Oauth2

In this example Uniport-Gateway serves as a reverse proxy and acts as the central relying party to Keycloak.

## Run

As a prerequisite a Keycloak and its Postgres database must be up & running. The file [docker-compose-auth.yml](../auth/docker-compose-auth.yml) for docker compose can be used for that:

```bash
docker compose -f ../auth/docker-compose-auth.yml up
```

Verify Keycloak is [set up correctly](../auth/README.md).

Then the Uniport-Gateway can be started:

```bash
sed -i '' -e -r 's/step[0-9]+/step3/g' ../docker-compose.yml
docker compose up
```

The following URLs can be used:

- <http://localhost:20000/whoami1> (unprotected)
- <http://localhost:20000/whoami2> (protected: use the test user to login `user1@mail.com`/`user1...`)

## Background

Middleware Documentation: <https://uniport-gateway.netlify.app/04-customization/#middlewares>

Entrypoint with `session` and `_session_` middlewares:

```json
{
    "entrypoints": [
        {
            "name": "http20000",
            "port": 20000,
            "middlewares": [
                // [..]
                {
                    "name": "session",
                    "type": "session",
                    "options": {
                        "idleTimeoutInMinutes": 30
                    }
                },
                {
                    "name": "ShowSessionContentMiddleware",
                    "type": "_session_"
                },
                // [..]
            ]
        },
    ],
    "providers": [
        // [..]
    ]
}
```

Router with `oauth2` and `authorizationBearer` middlewares:

```json
{
    "http": {
        "routers": [
            // [..]
            {
                "name": "whoami1",
                "rule": "PathPrefix('/whoami1')",
                "middlewares": [
                    "whoami1AuthBearer"
                ],
                "service": "whoami1"
            },
            {
                "name": "whoami2",
                "rule": "Host('local.uniport.ch.')",
                "priority": 50,
                "middlewares": [
                    "whoami2Oauth",
                    "whoami2AuthBearer"
                ],
                "service": "whoami2"
            }
            // [..]
        ],
        "middlewares": [
            // [..]
            {
                "name": "whoami2Oauth",
                "type": "oauth2",
                "options": {
                "clientId": "testclient",
                "clientSecret": "**********",
                "discoveryUrl": "http://keycloak:8080/auth/realms/testrealm",
                "sessionScope": "whoami2"
                }
            },
            {
                "name": "whoami1AuthBearer",
                "type": "authorizationBearer",
                "options": {
                    "sessionScope": "whoami2"
                }
            },
            {
                "name": "whoami2AuthBearer",
                "type": "authorizationBearer",
                "options": {
                    "sessionScope": "whoami2"
                }
            }
        ],
        "services": [
            // [..]
        ]
    }
}
```

Everything is routed through the Gateway

```json
{
    "http": {
        "routers": [
            {
                "name": "auth",
                "rule": "PathPrefix('/auth')",
                "service": "iam"
            },
            // [..]
        ],
        "middlewares": [
            // [..]
        ],
        "services": [
            // [..]
            {
                "name": "iam",
                "servers": [
                    {
                        "host": "keycloak",
                        "port": "8080"
                    }
                ]
            }
        ]
    }
}
```

`keycloak` is required to be configured to work with the example configuration of the `uniport-gateway`. The imported realm
configuration should take care of everything. For the sake of completeness, the following list shows the custom configuration that is applied by the import:

- Create realm `testrealm` (the following configuration has to be applied in this realm)
- Create user `testuser` (> `Users`)
  - Set password (> `Users` > `testuser` > `Credentials`)
- Create client with client ID `testclient` (> `Clients`)
  - Toggle `Client authentication`
  - Select `Standard flow`
  - Set `http://localhost:20000` as `home url`
  - Set `http://localhost:20000/*` as `redirect url`
- Copy client secret into the `uniport-gateway` config of the `oauth2` middleware (> `Clients` > `testclient` > `Credentials`)
- Create client with client ID `whoami{1..2}`
  - ?
- Create client scope `whoami{1..2}` (> `Client scopes`)
  - Type `None`
  - Toggle `include in token scope`
  - Add mapper `Audience testclient` with `testclient` as `Included Client Audience` (> `Client scopes` > `whoamiX` > `Mappers`)
  - Add mapper `Audience whoamiX` with `whoamiX` as `Included Client Audience` (> `Client scopes` > `whoamiX` > `Mappers`)
- Add `whoami{1..2}` as optional client scopes to `testclient` (> `Clients` > `testclient` > `Client scopes`)
