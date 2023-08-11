Paralleler Zugriff auf geschützte URLs innheralb der gleichen Session
=====================================================================

Eine Webseite verwendet meist einige Ressourcen, welche vom Browser nachgeladen werden. Wenn es sich dabei um Ressourcen aus geschützten URLs handelt, für welche in der aktuellen Portal-Gateway-Session noch kein JWT vorhanden ist, so löst der Portal-Gateway (als Relying Party (RP)) einen Authentication Request in Form eines Redirects aus.

Das folgende Beispiel zeigt eine solche Konstellation. Im [index.html](./index.html) File der `Report` Applikation werden zusätzlich zwei JavaScript Ressourcen aus der `Research` Applikation geladen.

Ausschnitt aus [index.html](./index.html):

```html
<!DOCTYPE html>
<html data-build-version="n/a">
  <head><base href="/report/"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Portal Report</title><link crossorigin="use-credentials" rel="icon" href="/base/assets/favicon/favicon.ico" sizes="any"/>
  </head>
  <body>
    ...
    <script type="module" crossorigin="use-credentials" src="/research/components/latest-articles.js"></script>
    <script type="module" crossorigin="use-credentials" src="/research/components/unread-articles-updater.js"></script>
  </body>
</html>
```

Der Browser ladet nun mit zwei Requests die Ressourcen für `/research/components/latest-articles.js` und für `/research/components/unread-articles-updater.js`. Diese beiden Requests (A + B) lösen im Portal-Gateway je einen Authentication Request Flow via Redirects zum Portal-IAM aus.

![Successful index.html load](./index.html_load_successful.png)

In dem obigen Beispiel sind die relevanten Request/Vorgänge ersichtlich:

1. Aufruf der geschützten Ressource und Redirect zum IAM
2. Aufruf des IAM (mit bestehender SSO Session) und Redirect zum Callback des RP
3. Aufruf der Callback-URL für Code-to-Token (zwischen RP und IAM) und Redirect zur geschützten Ressource
4. Erneuter Aufruf der geschützten Ressource

Weil bereits eine gültige Session im IAM besteht, verlangt dieses keine Login Credentials mehr, sondern antwortet auf den Authentication Request direkt mit einem Redirect auf die Callback-URL im RP.

Request A#1

```text
GET https://portal-uat.kundenplattform.albinkistler.ch/research/components/latest-articles.js
```

Response A#1

```text
302
Location: https://portal-uat.kundenplattform.albinkistler.ch/auth/realms/portal/protocol/openid-connect/auth?state=_9g8r5k0&redirect_uri=https%3A%2F%2Fportal-uat.kundenplattform.albinkistler.ch%3A443%2Fcallback%2Fresearch&scope=openid+Research&response_type=code&client_id=Portal-Gateway
```

Request#2

```text
https://portal-uat.kundenplattform.albinkistler.ch/auth/realms/portal/protocol/openid-connect/auth?state=_9g8r5k0&redirect_uri=https%3A%2F%2Fportal-uat.kundenplattform.albinkistler.ch%3A443%2Fcallback%2Fresearch&scope=openid+Research&response_type=code&client_id=Portal-Gateway
```

Response#2

```text
302
Location: https://portal-uat.kundenplattform.albinkistler.ch/callback/research?state=_9g8r5k0&session_state=b76ad3e7-14c1-420c-b3d2-49e91bdc9f09&code=4d48e7f5-7fb5-4a43-85a5-0411c66dc068.b76ad3e7-14c1-420c-b3d2-49e91bdc9f09.2a4d3a09-7f76-4a96-a4fe-42f682d4f086
```

Request#3

```text
https://portal-uat.kundenplattform.albinkistler.ch/callback/research?state=x833Cce5&session_state=b76ad3e7-14c1-420c-b3d2-49e91bdc9f09&code=26177213-260f-4ded-aa2b-27cc9a6ab20e.b76ad3e7-14c1-420c-b3d2-49e91bdc9f09.2a4d3a09-7f76-4a96-a4fe-42f682d4f086
```

Response#3

```text
302
/research/components/unread-articles-updater.js
```

Je nachdem wie die parallele Verarbeitung im Portal-Gateway geschieht, kann sich daraus die Situation ergeben, dass der spätere Request ein Cookie verwendet, für welches auf dem Portal-Gateway keine Session mehr vorhanden ist (regenerateId()).

Aktuell existiert im Portal-Gateway ein Fehler, sodass in diesen Fällen keine Antwort zurückgeliefert wird. Je nach Netzwerkinfrastruktur resultiert dann im Browser als Resultat `504 Gateway Time-out`.

Fälle

- ohne existierender Authentisierung, parallele Requests auf gleiche Applikation
  - eine Response mit Redirect für Authentication Flow
  - eine Response mit Redirect auf sich selber nach Ablauf der Wartezeit oder wenn Authentication Flow beendet ist
- ohne existierender Authentisierung, parallele Requests auf unterschiedliche Applikationen
  - eine Response mit Redirect auf AuthenticationFlow nach Ablauf der Wartezeit oder wenn Authentication Flow beendet ist
- mit existierender Authentisierung, parallele Requests auf eine andere Applikation
- mit existierender Authentisierung, parallele Requests auf zwei andere, unterschiedliche Applikationen


Der `state` Parameter des Authentication Requests wird genutzt, damit die ursprünglich aufgerufene URL und die dafür verwendete HTTP Methode über den ganzen Authentication Flow erhalten bleiben. Es kommt dafür im `RelyingPartyHandler` die Klasse `StateWithUri` zum Einsatz. Der Wert des `state` Parameters entspricht dem String von "base64(Zufallszahl:HTTP-Methode@base64(URI))"

### FIX: PORTAL-1184

Die notwendige Umgebung für lokales, exploratives Testing kann einfach über den Start der folgenden Run Configurations erreicht werden:

- Organisation: `organisation-service`
- Organisation: `hasura: docker-compose`
- Portal-IAM: `keycloak`
- Portal-Gateway: `PortalGateway`

### FIX: PORTAL-1417
