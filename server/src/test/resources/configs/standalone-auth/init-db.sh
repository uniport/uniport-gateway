#!/bin/bash
set -eEuo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
	CREATE USER keycloak WITH PASSWORD 'keycloak';
	CREATE DATABASE keycloak WITH OWNER keycloak ENCODING 'UTF8';
EOSQL
