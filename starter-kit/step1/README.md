# Step 1

In this example Uniport-Gateway serves as a reverse proxy for two services: `whoami1` and `whoami`. Both services have their own URL path: `/whoami` and `/whoami2`. Additionally, the root path is also mapped to the `whoami1` service and the `whoami2` is also mapped to the Host name `local.uniport.ch`.

## Run

```bash
docker compose up
```
