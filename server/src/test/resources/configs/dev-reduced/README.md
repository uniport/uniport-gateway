Portal-Gateway `dev-reduced`
===

This folder contains a simple, minimal Portal-Gateway example configuration with two services: `portal-iam` and `organisation-proxy`.

After a successful start of the Portal-Gateway the session can be inspected by [the `_session_` middleware](http://localhost:20000/_session_).

The configuration is done within 3 files: [auth.json](./config/dynamic-config/auth/auth.json), [organisation.json](./config/dynamic-config/organisation/organisation.json) and [portal-iam](./config/dynamic-config/portal-iam/portal-iam.json).

The values for the host name and the port are taken from the environment variables `PORTAL_GATEWAY_PORTAL_IAM_HOST` and `PORTAL_GATEWAY_PORTAL_IAM_PORT` for the `portal-iam` service and `PORTAL_GATEWAY_ORGANISATION_HOST` and `PORTAL_GATEWAY_ORGANISATION_PORT` for the `organisation-proxy` service. These variables are defined in [uniport-gateway.env](./uniport-gateway.env).

auth.json
---

organisation.json
---

portal-iam.json
---
