# Step 3

## Run

```bash
sed -i -E 's/step[0-9]+/step3/g' docker-compose.yml
docker compose -f auth/docker-compose-auth.yml up -d
docker compose up
```

**NOTE**: This may take a couple of minutes and may even timeout, because `keycloak` is setting up all its database tables and importing the `testrealm`. Further,
*the `portal-gateway` service may also fail on creating its `oauth2` middleware, due to `keycloak` not being configured completely yet.

Once everything is running, visit <http://localhost:8080> and login to `keycloak` with `admin`/`admin`. Verify that there is:

* a realm called `testrealm`
* a user called `user1@mail.com` in `testrealm` (initial password `user1...`)
* client called `testclient`, `whoami<N>` in `testrealm`

Then visit <http://localhost:20000> (unprotected) or <http://localhost:20000/whoami1> (protected) and use the test user to login.

## Background

Middleware Documentation: <https://docs.uniport.ch/customization/portal-gateway/#middlewares>

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
                        "idleTimeoutInMinute": 30
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
    "applications": [
        // [..]
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
                "rule": "Host('localdev.me')",
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

`keycloak` is required to be configured to work with the example configuration of the `portal-gateway`. The imported realm
configuration should take care of everything. For the sake of completeness, the following list shows the custom configuration that is applied by the import:

* Create realm `testrealm` (the following configuration has to be applied in this realm)
* Create user `testuser` (> `Users`)
  * Set password (> `Users` > `testuser` > `Credentials`)
* Create client with client ID `testclient` (> `Clients`)
  * Toggle `Client authentication`
  * Select `Standard flow`
  * Set `http://localhost:20000` as `home url`
  * Set `http://localhost:20000/*` as `redirect url`
* Copy client secret into the `portal-gateway` config of the `oauth2` middleware (> `Clients` > `testclient` > `Credentials`)
* Create client with client ID `whoami{1..2}`
  * ?
* Create client scope `whoami{1..2}` (> `Client scopes`)
  * Type `None`
  * Toggle `include in token scope`
  * Add mapper `Audience testclient` with `testclient` as `Included Client Audience` (> `Client scopes` > `whoamiX` > `Mappers`)
  * Add mapper `Audience whoamiX` with `whoamiX` as `Included Client Audience` (> `Client scopes` > `whoamiX` > `Mappers`)
* Add `whoami{1..2}` as optional client scopes to `testclient` (> `Clients` > `testclient` > `Client scopes`)
