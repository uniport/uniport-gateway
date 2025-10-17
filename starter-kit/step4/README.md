# Step 4 - Multiple entrypoints

In this example Uniport-Gateway listens on two ports: `20000` and also `20001`. The router for path `/single` is only available on port `20001`.

## Run

```bash
sd 'step[0-9]+' 'step4' ../docker-compose.yml
docker compose up
```

The following URLs can be used:

- http://localhost:20000
- http://localhost:20000/all
- http://localhost:20001/single


```bash
curl -sSfL localhost:20000
```

```bash
curl -sSf localhost:20000/all
```

```bash
curl -sSf localhost:20001/single
```

<details><summary>Response:</summary><pre>
Hostname: b312ee6aa416
IP: 127.0.0.1
IP: ::1
IP: 172.18.0.6
RemoteAddr: 172.18.0.5:48916
GET /single HTTP/1.1
Host: whoami1
User-Agent: curl/8.7.1
Accept: */*
Router: B
Traceparent: 00-2f5bdf59ea6504159f083121f5616d9a-4e09a4c978e4fbe0-01
X-Forwarded-For: 192.168.65.1:44643
X-Forwarded-Host: localhost:20001
X-Forwarded-Port: 20001
X-Forwarded-Proto: http
</pre></details>

```bash
curl -sSf localhost:20000/single
```

<details><summary>Response:</summary>
<pre>curl: (22) The requested URL returned error: 404</pre>
</details>
