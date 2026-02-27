# proxy-config.examples

## TLS setup (mkcert)

Uniport-Gateway configuration for TLS:

```json
{
    "entrypoints": [
        {
            "name": "http20000",
            "port": 20000,
            "tls": {
                "certFile": "helm/src/main/resources/proxy-config.mcp/localhost.pem",
                "keyFile": "helm/src/main/resources/proxy-config.mcp/localhost-key.pem"
            },
            ...
         }
    ]
}
```

Both entrypoints use locally-trusted TLS certificates generated with [mkcert](https://github.com/FiloSottile/mkcert).

Generate the certificates:

```bash
mkcert -cert-file localhost.pem -key-file localhost-key.pem localhost 127.0.0.1 ::1
```

To have them trusted automatically by browsers and other tools:

```bash
mkcert -install
```

The `.pem` files are gitignored and must be regenerated locally.
