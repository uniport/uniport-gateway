# Step &

## Run

```bash
sed -i -E 's/step[0-9]+/step7/g' docker-compose.yml
docker compose -f auth/docker-compose-auth.yml up -d
docker compose up
```

Verify everything is [set up correctly](../auth/README.md).

Then visit <http://localhost:20000> (unprotected) or <http://localhost:20000/whoami1> (protected) and use the test user to login `user1@mail.com`/`user1...`.

Then visit to <http://localhost:20000/auth>, login with `admin/admin` and navigate to `testrealm` > `sessions`, where you should see an existing session for `user1`.
On the three dots to the right, the user can be logged out. This also trigger a back-channel logout to the Uniport-Gateway and destroys the corresponding session.

## Background

Middleware Documentation: <https://uniport-gateway.netlify.app/04-customization/#middlewares>

This specification defines a logout mechanism that uses direct back-channel communication between the OIDC Provider (OP) and Relying Parties (RPs) being logged out.
This differs from front-channel logout mechanisms, which communicate logout requests from the OP to RPs via the User Agent.

Router with `backChannelLogout` middlewares:

```json
{
    "http": {
        "routers": [
            {
                "name": "backChannelLogout",
                "middlewares": [
                    "logoutRedirectRegex",
                    "backChannelLogout"
                ],
                "rule": "PathPrefix('/backchannellogout')",
                "service": "whoami1"
            }
        ],
        "middlewares": [
            // [..]
            {
                "name": "backChannelLogout",
                "type": "backChannelLogout",
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
                    "publicKeysReconciliation": {
                        "enabled": true,
                        "intervalMs": 3600000
                    }
                }
            },
            {
                "name": "logoutRedirectRegex",
                "type": "redirectRegex",
                "options": {
                    "regex": "^/logout(.*)$",
                    "replacement": "/whoami1"
                }
            }
        ],
        // [..]
    }
}
```

`keycloak` is required to be configured to work with the example configuration of the `uniport-gateway`. The imported realm
configuration should take care of everything.

For the sake of completeness: The `testclient` has to be configured with the `Backchannel logout URL` pointing to `http://gateway:20000/backchannellogout`.
