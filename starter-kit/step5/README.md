# Step 5 - Multiple sites with a shared SSO session

## Run

As a prerequisite a Keycloak and its Postgres database must be up & running. The file [docker-compose-auth.yml](../auth/docker-compose-auth.yml) for docker compose can be used for that:

```bash
docker compose -f ../auth/docker-compose-auth.yml up
```

Verify Keycloak is [set up correctly](../auth/README.md).

```bash
sed -i '' -e 's/step[0-9]/step5/g' ../docker-compose.yml
docker compose up
```

Then visit <http://whoami1.local.uniport.ch:20000> to login with `user1@mail.com`/`user1...` and then visit <http://whoami2.local.uniport.ch:20000/whoami1> to verify that there is a shared SSO session, as you are logged in.

**Disclaimer**: There is a trade-of to be made to make this possible. Best practice for OAuth, is to use the `responseMode=form_post` due to [several reasons](https://www.ietf.org/archive/id/draft-ietf-oauth-security-topics-29.html). However, to make `form_post` work, Keycloak returns some JavaScript to the Client after a successful authentication that issues the callback request to the Gateway. Since the JavaScript is loaded from one site`auth.local.uniport.ch` and the Gateway is on `whoami1.local.uniport.ch`, the browser does not send the `uniport.session` session cookie alongside with the callback request, but this is crucial. There is a solution for this:

* Use another `responseMode` than `form_post` i.e. `query` or `fragment`, or
* Set the `uniport.session` session cookie to `SameSite=LAX` (this has its own [security implications](https://owasp.org/www-community/SameSite))

Despite `responseMode=query` is not according to best practices, it is tolerable if it is used in combination with [PKCE](https://www.rfc-editor.org/rfc/rfc7636).
