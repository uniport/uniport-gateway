# Step 6 - Organizing your dynamic configuration

## Run

```bash
sd 'step[0-9]+' 'step6' ../docker-compose.yml
docker compose up
```

The following URLs can be used:

- http://localhost:20000
- http://localhost:20000/whoami1
- http://localhost:20000/whoami2
- http://local.uniport.ch:20000

## Background

Defines the path to the directory which contains the configuration files. It is important to understand how multiple configuration files are merged: In general, with a deep-merge (recursive) JSON objects are matched within the existing structure and all matching entries are replaced. JsonArrays are treated like any other entry, i.e., completely replaced. This pattern is applied to all files that are in the same directory. For more complex configurations, we offer a merge mechanism over subdirectories. Subdirectories are largely merged in the same way as described above, with the exception of JsonArrays. JsonArrays are concatenated without duplicates. The names of the subdirectories do not matter and can be used for organizational purposes.

The directory name and the filename can be chosen to your liking.

In this example, config in the `whoami1` and `whoami` folders are appended. If there would be a `another.json` in the `whoami1` folder, the config in `config.json` and `another.json` would be replaced.

```text
.
├── dynamic-config
│   ├── whoami1
│   │   └── config.json
│   └── whoami2
│       └── config.json
├── uniport-gateway.json
```
