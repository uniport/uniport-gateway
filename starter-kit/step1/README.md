# Step 1 - Bare-bone

In this example Uniport-Gateway serves as a reverse proxy for two services: `whoami1` and `whoami2`. Both services have their own URL path: `/whoami` and `/whoami2`. Additionally, the root path is also mapped to the `whoami1` service and the `whoami2` service is also mapped to the Host name `local.uniport.ch`.

## Run

```bash
sed -i '' -r -e 's/step[0-9]+/step1/g' ../docker-compose.yml
docker compose up
```

The following URLs can be used:

- http://localhost:20000/whoami1
- http://localhost:20000/whoami2
- http://localhost:20000
- http://local.uniport.ch:20000

### /whoami1

```bash
curl -sSf localhost:20000/whoami1
```

<details><summary>Response:</summary><pre>
Hostname: b312ee6aa416
IP: 127.0.0.1
IP: ::1
IP: 172.18.0.4
RemoteAddr: 172.18.0.6:50412
GET /whoami1 HTTP/1.1
Host: whoami1
User-Agent: curl/8.7.1
Accept: */*
Traceparent: 00-43f94f102326037186cc9611143cb3de-8c7d4931fdd85342-01
X-Forwarded-For: 192.168.65.1:31927
X-Forwarded-Host: localhost:20000
X-Forwarded-Port: 20000
X-Forwarded-Proto: http
</pre></details>

### /whoami2

```bash
curl -sSf localhost:20000/whoami2
```

<details><summary>Response:</summary><pre>
Hostname: d40cb59af807
IP: 127.0.0.1
IP: ::1
IP: 172.18.0.5
RemoteAddr: 172.18.0.6:34382
GET /whoami2 HTTP/1.1
Host: whoami2
User-Agent: curl/8.7.1
Accept: */*
Traceparent: 00-521ddbff836d4d0ea5ded0133ac5c3cc-043cd98d481cdea7-01
X-Forwarded-For: 192.168.65.1:17007
X-Forwarded-Host: localhost:20000
X-Forwarded-Port: 20000
X-Forwarded-Proto: http
</pre></details>

### With `curl` following redirect for `/`:

```bash
curl -sSfL localhost:20000
```

<details><summary>Response:</summary><pre>
Hostname: b312ee6aa416
IP: 127.0.0.1
IP: ::1
IP: 172.18.0.4
RemoteAddr: 172.18.0.6:33064
GET /whoami1 HTTP/1.1
Host: whoami1
User-Agent: curl/8.7.1
Accept: */*
Traceparent: 00-805d63c04262f0a21dff1b743e87bd9c-d2f6a028c9d33ce0-01
X-Forwarded-For: 192.168.65.1:55854
X-Forwarded-Host: localhost:20000
X-Forwarded-Port: 20000
X-Forwarded-Proto: http
</pre></details>

### local.uniport.ch:20000

```bash
curl -sSfL local.uniport.ch:20000
```

<details><summary>Response:</summary><pre>
Hostname: b312ee6aa416
IP: 127.0.0.1
IP: ::1
IP: 172.18.0.4
RemoteAddr: 172.18.0.6:45400
GET /whoami1 HTTP/1.1
Host: whoami1
User-Agent: curl/8.7.1
Accept: */*
Traceparent: 00-4099924f36148c9f53c5c3c81cfeb6dd-a083e6b0aeef23d7-01
X-Forwarded-For: 192.168.65.1:40430
X-Forwarded-Host: local.uniport.ch:20000
X-Forwarded-Port: 20000
X-Forwarded-Proto: http
</pre></details>
