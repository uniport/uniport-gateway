# Uniport-Gateway

## Steps

### Step 1

* Bare-bone

### Step 2

* Telemetry (Tracing & Logging)

### Step 3

* OAuth2

### Step 4

* Multiple entrypoints

### Step 5

* Multiple hosts with a shared Keycloak Session i.e. SSO session

### Step 6

* Organize your dynamic configurations

### Future Step Ideas

* env vars in config
* ha proxy tls termination
* backchannel logout
* cluster mode

## Background

### Whoami

[whoami](https://github.com/traefik/whoami) is a tiny Go server that returns OS information and the received HTTP request as its HTTP response. It is a
convenient way, to inspect the request a backend service would receive by the `portal-gateway`.
