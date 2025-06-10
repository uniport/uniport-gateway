# Step 4

## Run

```bash
docker compose up
```

**Note**: Make sure, both ports `20000` and `20001` are mapped to the host:

```yaml
    ports:
      - "20000:20000"
      - "20001:20001"
```
