# Step 4 - Multiple entrypoints

In this example Uniport-Gateway listens on two ports: `20000` and also `20001`. The router for path `/single` is only available on port `20001`.

## Run

```bash
sed -i '' -e 's/step[0-9]/step4/g' ../docker-compose.yml
docker compose up
```

The following URLs can be used:


- http://localhost:20000
- http://localhost:20000/all
- http://localhost:20001/single
