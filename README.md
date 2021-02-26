# Portal Gateway

![PortalGateway](.docs/PortalGateway.png)

## Build

```shell
./mvnw clean install
```

## Launch

Die Run Configuration `PortalGateway` startet den Portal Gateway Server aus der IDE. Dabei werden die beiden Property Dateien [portal-gateway.common.env](./docker-compose/src/main/resources/portal-gateway.common.env) und [portal-gateway.specific.env](./docker-compose/src/main/resources/portal-gateway.specific.env) zur Konfiguration verwendet.

Für die Konfiguration des Portal Gateway Servers wird eine JSON Datei verwendet. Diese wird in der angegeben Reihenfolge wie folgt gelesen:

1. Datei welche über die Environement Variable 'PORTAL_GATEWAY_JSON' angegeben wird
2. Datei welche über das System Property 'PORTAL_GATEWAY_JSON' angegeben wird
3. Datei 'portal-gateway.json' im '/etc/portal-gateway/' Verzeichnis
4. Datei 'portal-gateway.json' im aktuellen Verzeichnis (= ./server)

Damit also die Testkonfiguration unter ./server/src/test/resources/portal-gateway.json verwendet wird, muss in der portal-gateway.specific.env Datei folgender Eintrag ergänzt werden:

```dotenv
PORTAL_GATEWAY_JSON=./src/test/resources/portal-gateway.json
```

Für den Start der verwendeten Backend Systeme, kann die Run Configuration `httpbin: docker-compose` verwendet werden.