# Uniport-Gateway Starter Kit

The following steps can help to get started with Uniport-Gateway. Every step contains a simple example showing a specific feature.

All examples are based on the same docker compose file [docker-compose.yml](./docker-compose.yml). The use just a different folder containing the Uniport-Gateway configuration.

## Steps

### Step 1

[Uniport-Gateway as a bare-bone reverse proxy](./step1/README.md) with two services.

### Step 2

[Uniport-Gateway with two entrypoint middlewares](./step2/README.md)for tracing & logging configured.

### Step 3

[Uniport-Gateway as a OIDC relying party](./step3/README.md) using Keycloak as OpenID Provider.

### Step 4

[Uniport-Gateway listens on two ports](./step4/README.md).

### Step 5

* Multiple sites with a shared Keycloak Session i.e. SSO session

### Step 6

* Organize your dynamic configurations

### Step 7

* [Back-channel logout](https://openid.net/specs/openid-connect-backchannel-1_0.html)

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
