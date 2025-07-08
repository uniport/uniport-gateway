# Uniport-Gateway Documentation

[Start here](./content)

## Requirements

- Node 16.x
- Docker

## Development

### Setup

```shell
npm login https://nexus3.inventage.com/repository/inventage-portal-npm-group/
docker login docker-registry.inventage.com:10094
```

### Useful commands

#### Npm

- `npm run lint` — Checks for codestyle errors using various **linters**
- `npm run format` — Runs automatic code **formatters**
- `npm start` – Starts the MkDocs container showing **live-preview** of the rendered documentation
- `npm run build` – **Builds** the MkDocs documentation
- `npm run serve:build` – **Serves** the built MkDocs documentation

IntelliJ launch configurations are available for the build and live preview scenarios.

### Documentation Principles

- Written in Markdown format
- A Markdown file contains only **one (1)** `h1` header
- It is written in English
- Capitalization
    - For product names, we use the spelling as on the product website
        - Jira, not JIRA
        - Kafka, not KAFKA
        - Avro, not AVRO
        - Keycloak, not KeyCloak
    - File formats: ZIP, PDF
- Filenames: no spaces, i.e., `Uniport_Overview.png` instead of `Uniport Overview.png`
- Table names, table attributes, code in general marked with backticks.

Here's the English translation of the provided German text, maintaining the original formatting and meaning:

### Useful Links

- [tree.nathanfriend.io](https://tree.nathanfriend.io/) – For generating ASCII directory trees
- Broken links checkers: https://github.com/untitaker/hyperlink and https://github.com/lycheeverse/lychee
- [`squidfunk/mkdocs-material`](https://squidfunk.github.io/mkdocs-material/)
    - [Images](https://squidfunk.github.io/mkdocs-material/reference/images/)
    - [Icons & Emojis](https://squidfunk.github.io/mkdocs-material/reference/icons-emojis)
        - [Octicons](https://primer.style/octicons/)
    - [Admonitions](https://squidfunk.github.io/mkdocs-material/reference/admonitions)
