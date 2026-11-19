
#!/bin/bash

set -e

echo "Initializing PostgreSQL databases..."

psql \
    -v ON_ERROR_STOP=1 \
    --username "$POSTGRES_USER" \
    --dbname postgres \
    --set=kc_user="$KEYCLOAK_DB_USER" \
    --set=kc_password="$KEYCLOAK_DB_PASSWORD" \
    --set=registry_user="$REGISTRY_DB_USER" \
    --set=registry_password="$REGISTRY_DB_PASSWORD" \
    --set=app_user="$APP_DB_USER" \
    --set=app_password="$APP_DB_PASSWORD" <<'EOSQL'

SELECT format(
    'CREATE ROLE %I LOGIN PASSWORD %L',
    :'kc_user',
    :'kc_password'
) \gexec

SELECT format(
    'CREATE ROLE %I LOGIN PASSWORD %L',
    :'registry_user',
    :'registry_password'
) \gexec

SELECT format(
    'CREATE ROLE %I LOGIN PASSWORD %L',
    :'app_user',
    :'app_password'
) \gexec

SELECT format(
    'CREATE DATABASE %I OWNER %I',
    'keycloak',
    :'kc_user'
) \gexec

SELECT format(
    'CREATE DATABASE %I OWNER %I',
    'identity_registry',
    :'registry_user'
) \gexec

SELECT format(
    'CREATE DATABASE %I OWNER %I',
    'hexmap_app',
    :'app_user'
) \gexec

EOSQL

echo "PostgreSQL initialization completed."

