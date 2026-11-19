# Hexmap application

Interactive hexagonal map of Ukraine (H3, resolution 5, ~2 470 cells) where an
authenticated user can pick **one** monitoring zone. Authentication is delegated
entirely to the existing Keycloak; this service is an OAuth2 resource server.

Scope of this iteration: map, zone subscription, Keycloak integration.
**Not implemented yet:** alert events, notifications, SignalR/real-time updates,
admin test panel (the `/api/admin/**` role rule is already in place).

## Run

```bash
cp .env.example .env                      # first time only
mvn -pl keycloak-shared,keycloak-broker-common,keycloak-diia-provider,keycloak-bankid-provider,keycloak-identity-resolver -am package -DskipTests
docker compose up -d --build              # postgres + keycloak (imports realm "hexmap")

cd app
export APP_DB_PASSWORD=local_app_password # value of APP_DB_PASSWORD from .env
mvn spring-boot:run                       # http://localhost:8081
```

PostgreSQL init scripts run only on an **empty** volume. For an existing volume
create the database once:

```bash
docker exec -i keycloak-postgres psql -U postgres -c "CREATE ROLE hexmap_user LOGIN PASSWORD 'local_app_password'" \
  -c "CREATE DATABASE hexmap_app OWNER hexmap_user"
```

Dev users (realm `hexmap`, from `infrastructure/keycloak/hexmap-realm.json`):
`demo-user` / `demo-user` (role `user`), `demo-admin` / `demo-admin` (roles `user`, `admin`).
**Dev only — remove these users and the passwords before any shared deployment.**

## API

| Method | Path | Access |
|---|---|---|
| GET | `/api/hexagons` | public: all cells with outlines |
| GET | `/api/hexagons/{h3}` | public: cell + neighbours |
| GET | `/api/me` | role `user`: profile, roles, current subscription |
| GET / PUT / DELETE | `/api/subscription` | role `user`: read / create-or-change / remove own zone |

The user is always taken from the verified JWT (`sub`). No endpoint accepts a user id.
The token is validated for signature (JWKS), expiry, issuer and audience `hexmap-api`.

## Data model (`db/migration`)

`application_users`, `hexagons`, `hexagon_neighbours`, `user_hexagon_subscriptions`.
Guarantees in PostgreSQL: unique `keycloak_user_id`, unique `h3_index`, **one active
subscription per user** (partial unique index), no self-neighbour, unique and
**symmetric** neighbour pairs (deferred constraint trigger).

## Geography

`V2__seed_hexagons.sql` is generated, not hand-written:

```bash
pip install h3 shapely
python3 tools/generate_hexagons.py --geojson ne_10m_admin_1_states_provinces.geojson --resolution 5 \
  --out app/src/main/resources/db/migration/V2__seed_hexagons.sql
```

* Boundaries: Natural Earth 1:10m Admin-1 (public domain), regions selected by
  ISO 3166-2 `UA-*`, i.e. internationally recognised borders (includes Crimea and Sevastopol).
* A cell belongs to the region that contains its centre. Neighbours come from H3 `grid_ring(1)`
  restricted to cells in the set (coastal/border cells have fewer than 6).
* `district_name` is empty: no district-level (ADM2) source has been added yet.
* To change resolution: regenerate and add a new migration; `resolution` is stored per row.
* PostGIS is intentionally not used yet (outlines are stored as JSONB). Switch the compose
  image to `postgis/postgis` if spatial queries become necessary.
