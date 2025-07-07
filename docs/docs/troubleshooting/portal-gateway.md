# Portal-Gateway

## Cluster mode

Wenn der Portal-Gateway im Cluster Mode betrieben wird, werden die Key-Value Pairs im Session Store über die Instanzen synchronisiert. Der Session Store wird hauptsächlich für user-spezifische Daten bzgl. Authentisierung wie ID und Access Tokens sowie für Cookies benutzt. Mit der Beispiel-Konfiguration wird üblicherweise der Inhalt des Session Store unter `/organisation/_session_` ersichtlich gemacht. Wenn der Portal-Gateway im Cluster Mode betrieben wird, ist allerdings nicht klar auf welcher Instanz man landet. Als Hilfsmittel ist die aktuelle Instanz, auf welche man zugreift, auf der Seite ersichtlich, allerdings möchte man manchmal die Session Store Inhalte aller Instanzen sehen. Dazu braucht es ein wenig manuelle Arbeit:

Wir gehen im Folgenden von 2 Portal-Gateway Instanzen aus.

- Die beiden Instanzen werden mittels port-forward auf die lokalen Ports `:8000` und `:8001` erreichbar gemacht.

    ```bash
    kubectl port-forward portal-gateway-0 8000:20000
    kubectl port-forward portal-gateway-1 8001:20000
    ```

- Der aktuelle Wert des Session Cookies `uniport.session` muss aus dem Browser kopiert werden.

    ```plain
    uniport.session:"1af17763441b19582a2a26764050322dbd743a98260e8c83bda74c3b60dd16c1"
    ```

- Mittel `curl` können nun die beiden Session Store Inhalte eingesehen werden.

    ```bash
    curl -v --cookie "uniport.session=1af17763441b19582a2a26764050322dbd743a98260e8c83bda74c3b60dd16c1" http://127.0.0.1:8000/organisation/_session_
    curl -v --cookie "uniport.session=1af17763441b19582a2a26764050322dbd743a98260e8c83bda74c3b60dd16c1" http://127.0.0.1:8001/organisation/_session_
    ```

**Note**: We cannot simply forward the respective instances and access them via the browser, because we then access them with the `localhost` domain, instead of the true domain. Hence, the wrong session cookie will be sent along the request by the browser.
