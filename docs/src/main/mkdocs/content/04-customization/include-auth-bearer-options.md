<!-- markdownlint-disable first-line-h1 -->

| Variable | Required | Type | Description |
| --- | --- | --- | --- |
| `sessionScope` | Yes | id (referencing a session scope defined by an OAuth2 middleware) | The Session Scope determines which token should be set in the Auth Bearer Header. This can be either an ID token or an Access token. Per user, there is one ID token and zero or more Access tokens. |
