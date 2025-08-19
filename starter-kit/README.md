# Uniport-Gateway Starter Kit

## Steps

### Step 1

* Bare-bone

### Step 2

* Telemetry (Tracing & Logging)

### Step 3

* OIDC/OAuth2

### Step 4

* Multiple entrypoints

### Step 5

* Multiple hosts with a shared Keycloak Session i.e. SSO session

### Step 6

* Organize your dynamic configurations

### Step 7

* [Backchannel-logout](https://openid.net/specs/openid-connect-backchannel-1_0.html)

### Step 8

* JWT bearer token authorization

### Future Step Ideas

* env vars in config
* ha proxy tls termination
* cluster mode

## Background

### Whoami

[whoami](https://github.com/traefik/whoami) is a tiny Go server that returns OS information and the received HTTP request as its HTTP response. It is a
convenient way, to inspect the request a backend service would receive by the `uniport-gateway`.
