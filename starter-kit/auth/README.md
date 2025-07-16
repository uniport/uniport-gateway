# Keycloak

## Run

```bash
docker compose -f auth/docker-compose-auth.yml up -d
```

**NOTE**: This may take a couple of minutes and may even timeout, because `keycloak` is setting up all its database tables and importing the `testrealm`. Further,
the `uniport-gateway` service may also fail on creating its `oauth2` middleware, due to `keycloak` not being configured completely yet.

Once everything is running, visit <http://localhost:8080> and login to `keycloak` with `admin`/`admin`. Verify that there is:

* a realm called `testrealm`
* a user called `user1@mail.com` in `testrealm` (initial password `user1...`)
* client called `testclient`, `whoami<N>` in `testrealm`

## Background

`keycloak` is required to be configured to work with the example configuration of the `uniport-gateway`. The imported realm
configuration should take care of everything. For the sake of completeness, the following list shows the custom configuration that is applied by the import:

* Create realm `testrealm` (the following configuration has to be applied in this realm)
* Create user `testuser` (> `Users`)
  * Set password (> `Users` > `testuser` > `Credentials`)
* Create client with client ID `testclient` (> `Clients`)
  * Toggle `Client authentication`
  * Select `Standard flow`
  * Set `http://localhost:20000`, `http://whoami1.local.uniport.ch` and `http://whoami2.local.uniport.ch`  as `redirect`
  * Set `http://localhost:20000` for `home_url`
* Copy client secret into the `uniport-gateway` config of the `oauth2` middleware (> `Clients` > `testclient` > `Credentials`)
* Create client with client ID `whoami{1..2}`
  * ?
* Create client scope `whoami{1..2}` (> `Client scopes`)
  * Type `None`
  * Toggle `include in token scope`
  * Add mapper `Audience testclient` with `testclient` as `Included Client Audience` (> `Client scopes` > `whoamiX` > `Mappers`)
  * Add mapper `Audience whoamiX` with `whoamiX` as `Included Client Audience` (> `Client scopes` > `whoamiX` > `Mappers`)
* Add `whoami{1..2}` as optional client scopes to `testclient` (> `Clients` > `testclient` > `Client scopes`)
