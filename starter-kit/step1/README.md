# Step 1 - Bare-bone

In this example Uniport-Gateway serves as a reverse proxy for two services: `whoami1` and `whoami2`. Both services have their own URL path: `/whoami` and `/whoami2`. Additionally, the root path is also mapped to the `whoami1` service and the `whoami2` service is also mapped to the Host name `local.uniport.ch`.

## Run

```bash
sed -i '' -e 's/step[0-9]/step1/g' ../docker-compose.yml
docker compose up
```

The following URLs can be used:

- http://localhost:20000
- http://localhost:20000/whoami1
- http://localhost:20000/whoami2
- http://local.uniport.ch:20000
