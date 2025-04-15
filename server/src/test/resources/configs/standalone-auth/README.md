# Standalone Portal-Gateway

## Run

Start all services:

```bash
docker-compose -f docker-compose.yml up -d
```

**NOTE**: This may take a couple of minutes and may even timeout, because `keycloak` is setting up all its database tables and importing the `testrealm`. Further,
*the `portal-gateway` service may also fail on creating its `oauth2` middleware, due to `keycloak` not being configured completely yet.

Once everything is running, visit <http://localhost:8080> and login to `keycloak` with `admin`/`admin`. Verify that there is:

* a realm called `testrealm`
* a user called `user1@mail.com` in `testrealm` (initial password `user1...`)
* client called `testclient`, `whoami<N>` in `testrealm`

Then visit <http://localhost:20000> (unprotected) or <http://localhost:20000/whoami1> (protected) and use the test user to login.

## Configuration

### Portal-Gateway

The `portal-gateway` is configured such that it has a single entrypoint on port `20000`. The entrypoint has the following middleware:

* OpenTelemetry - enables tracing
* Session - configures a session
* RequestResponseLogger - logs the request and resposnes of a request
* ShowSessionContent - shows the session store content of the current session

Further, there are 4 routes configured:

* `/` - Redirects all requests to `/whoami1`
* `/whoami1` - Serves all requests to `whoami1`, sets an Bearer Token for session scope `whoami2` if present
* `/whoami2` - Serves all requests to `whoami2`, requires authentcation, sets an Bearer Token for session scope `whoami2`
* `/whoami3` - Serves all requests to `whoami3`, requires authentcation, sets an Bearer Token for session scope `whoami2`

### Keycloak

`keycloak` is required to be configured to work with the example configuration of the `portal-gateway`. The imported realm
configuration should take care of everything. For the sake of completeness, the following list show the custom configuration that is applied by the import:

* Create realm `testrealm` (the following configuration has to be applied in this realm)
* Create user `testuser` (> `Users`)
  * Set password (> `Users` > `testuser` > `Credentials`)
* Create client with client ID `testclient` (> `Clients`)
  * Toggle `Client authentication`
  * Select `Standard flow`
  * Set `http://localhost:20000` as `home url`
  * Set `http://localhost:20000/*` as `redirect url`
* Copy client secret into the `portal-gateway` config of the `oauth2` middleware (> `Clients` > `testclient` > `Credentials`)
* Create client with client ID `whoami{1..3}`
  * ?
* Create client scope `whoami{1..3}` (> `Client scopes`)
  * Type `None`
  * Toggle `include in token scope`
  * Add mapper `Audience testclient` with `testclient` as `Included Client Audience` (> `Client scopes` > `whoamiX` > `Mappers`)
  * Add mapper `Audience whoamiX` with `whoamiX` as `Included Client Audience` (> `Client scopes` > `whoamiX` > `Mappers`)
* Add `whoami{1..3}` as optional client scopes to `testclient` (> `Clients` > `testclient` > `Client scopes`)

## Background

### Whoami

[whoami](https://github.com/traefik/whoami) is a tiny Go server that returns OS information and the received HTTP request as its HTTP response. It is a
convenient way, to inspect the request a backend service would receive by the `portal-gateway`.
