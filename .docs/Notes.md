# Portal Gateway

Das Portal Gateway kann von einer der folgenden Konfigurationsarten initialisiert werden:

- über die Konfigurationsdatei: portal-gateway.json
- über CLI Argumente
- über Environment Variablen

Die oben aufgeführte Reihenfolge definiert wie die Werte ausgewertet werden (eine spätere Definition überschreibt vorherige).

## Konfigurationsdatei

Die Konfigurationsdatei wird von einem der folgenden Orten gelesen:

- `/etc/portal-gateway/`
- `~/.config/`  
- `.` (aktuelles Verzeichnis)
- definiert durch das CLI Argument `--configFile`

## Debugging

- io.vertx.ext.web.handler.impl.AuthenticationHandlerImpl
    - handle
- io.vertx.ext.web.handler.impl.OAuth2AuthHandlerImpl
    - parseCredentials
    - setupCallback: im Callback wird der OAuth2 Code bei Keycloak gegen ein Token ausgetauschen und dann im RoutingContext als User gesetzt (OAuth2AuthHandlerImpl.java:335).