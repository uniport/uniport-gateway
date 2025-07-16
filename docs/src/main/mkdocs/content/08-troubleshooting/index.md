# Troubleshooting

## Cluster mode

When the Uniport-Gateway is operated in cluster mode, the key-value pairs in the Session Store are synchronized across instances. The Session Store is primarily used for user-specific data related to authentication, such as ID and Access Tokens, as well as for cookies. With the example configuration, the content of the Session Store is usually made visible under `/organisation/_session_`. However, when the Uniport-Gateway is operated in cluster mode, it is not clear which instance you land on. As a tool, the current instance being accessed is visible on the page, but sometimes you want to see the Session Store contents of all instances. This requires a little manual work:

We assume 2 Uniport-Gateway instances below.

1. The two instances are made accessible via port-forward to the local ports `:8000` and `:8001`.

    ```bash
    kubectl port-forward uniport-gateway-0 8000:20000
    kubectl port-forward uniport-gateway-1 8001:20000
    ```

2. The current value of the session cookie `uniport.session` must be copied from the browser.

    ```plain
    uniport.session:"1af17763441b19582a2a26764050322dbd743a98260e8c83bda74c3b60dd16c1"
    ```

3. The contents of both Session Stores can now be viewed using `curl`.

    ```bash
    curl -v --cookie "uniport.session=1af17763441b19582a2a26764050322dbd743a98260e8c83bda74c3b60dd16c1" http://127.0.0.1:8000/organisation/_session_
    curl -v --cookie "uniport.session=1af17763441b19582a2a26764050322dbd743a98260e8c83bda74c3b60dd16c1" http://127.0.0.1:8001/organisation/_session_
    ```

!!! note

    We cannot simply forward the respective instances and access them via the browser, because we then access them with the `localhost` domain, instead of the true domain. Hence, the wrong session cookie will be sent along the request by the browser.
