# Step 5

## Run

```bash
sed -i -E 's/step[0-9]+/step5/g' docker-compose.yml
docker compose -f auth/docker-compose-auth.yml up -d
docker compose up
```

Verify everything is [set up correctly](../auth/README.md).

Then visit <http://whoami1.local.uniport.ch:20000> to login with `user1@mail.com`/`user1...` and then visit <http://whoami2.local.uniport.ch:20000/whoami1> to verify that there is a shared SSO session, as you are logged in.

**Disclaimer**: There is a trade-of to be made to make this possible. Best practice for OAuth, is to use the `responseMode=form_post` due to [several reasons](https://www.ietf.org/archive/id/draft-ietf-oauth-security-topics-29.html). However, to make `form_post` work, Keycloak returns some JavaScript to the Client after a successful authentication that issues the callback request to the Gateway. Since the JavaScript is loaded from one domain `auth.local.uniport.ch` and the Gateway is on `whoami1.local.uniport.ch`, the browser does not send the `uniport.session` session cookie alongside with the callback request, but this is crucial. There are two possibilities to solve this:

* Use another `responseMode` than `form_post` i.e. `query` or `fragment`, or
* Set the `uniport.session` session cookie to `secure` and `SameSite=NONE` (this has its own [security implications](https://owasp.org/www-community/SameSite))

Both are not optimal, but make this setup work.
