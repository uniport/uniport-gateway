# Portal Gateway

Der Portal Gateway Server agiert als Reverse Proxy für alle Request des Portals.

![PortalGateway](.docs/PortalGateway.png)

Für die Konfiguration des Portal Gateway Servers wird eine JSON Datei verwendet. Diese wird in der angegebenen Reihenfolge gesucht:

1. Datei welche über die Environement Variable 'PORTAL_GATEWAY_JSON' angegeben wird
2. Datei welche über das System Property 'PORTAL_GATEWAY_JSON' angegeben wird
3. Datei 'portal-gateway.json' im '/etc/portal-gateway/' Verzeichnis
4. Datei 'portal-gateway.json' im aktuellen Verzeichnis (Run Configuration "PortalGateway" := ./server/portal-gateway)

## Build

```shell
./mvnw clean install
```

**Note**: You need to have your [~/.m2/settings.xml](http://maven.apache.org/settings.html#Servers) set up proberly with at least:

```xml
<servers>
    <server>
        <id>inventage-projectware</id>
        <username>username</username>
        <password>password</password>
    </server>
</servers>
```

(You can also use [user tokens](https://help.sonatype.com/repomanager3/system-configuration/user-authentication/security-setup-with-user-tokens) if you don't want to save your password in a file)

## Launch

Die Run Configuration `PortalGateway` startet den Portal Gateway Server aus der IDE. Dabei werden die beiden Property Dateien [portal-gateway.common.env](./docker-compose/src/main/resources/portal-gateway.common.env) und [portal-gateway.specific.env](./docker-compose/src/main/resources/portal-gateway.specific.env) zur Konfiguration verwendet.

Damit also die [Testkonfiguration](./server/src/test/resources/portal-gateway.json) unter `./server/src/test/resources/portal-gateway.json` verwendet wird, muss in der portal-gateway.specific.env Datei folgender Eintrag ergänzt werden:

```dotenv
PORTAL_GATEWAY_JSON=./src/test/resources/portal-gateway.json
```

Für den Start der verwendeten Backend Systeme, kann die Run Configuration `httpbin: docker-compose` verwendet werden.
