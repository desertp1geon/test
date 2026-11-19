-- Application database (separate from Keycloak DB and identity_registry).
-- All timestamps are stored in UTC (TIMESTAMPTZ).

CREATE TABLE application_users
(
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_user_id VARCHAR(255) NOT NULL,   -- JWT claim "sub"
    display_name     VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_application_users_keycloak_user UNIQUE (keycloak_user_id)
);

CREATE TABLE hexagons
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    h3_index      VARCHAR(15)      NOT NULL,
    resolution    SMALLINT         NOT NULL,
    name          VARCHAR(255)     NOT NULL,
    region_name   VARCHAR(255)     NOT NULL,
    region_code   VARCHAR(10)      NOT NULL,  -- ISO 3166-2, e.g. UA-46
    district_name VARCHAR(255),               -- reserved for ADM2 data
    center_lat    DOUBLE PRECISION NOT NULL,
    center_lng    DOUBLE PRECISION NOT NULL,
    boundary      JSONB            NOT NULL,  -- [[lat,lng] x6], cell outline
    is_active     BOOLEAN          NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ      NOT NULL DEFAULT now(),

    CONSTRAINT uq_hexagons_h3_index UNIQUE (h3_index),
    CONSTRAINT chk_hexagons_resolution CHECK (resolution BETWEEN 0 AND 15)
);

CREATE INDEX idx_hexagons_region_code ON hexagons (region_code);

CREATE TABLE hexagon_neighbours
(
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hexagon_id            UUID NOT NULL REFERENCES hexagons (id) ON DELETE CASCADE,
    neighbour_hexagon_id  UUID NOT NULL REFERENCES hexagons (id) ON DELETE CASCADE,

    CONSTRAINT uq_hexagon_neighbours_pair UNIQUE (hexagon_id, neighbour_hexagon_id),
    CONSTRAINT chk_hexagon_neighbours_not_self CHECK (hexagon_id <> neighbour_hexagon_id)
);

CREATE INDEX idx_hexagon_neighbours_neighbour ON hexagon_neighbours (neighbour_hexagon_id);

-- Symmetry: if A is a neighbour of B, B must be a neighbour of A (checked at commit).
CREATE FUNCTION check_hexagon_neighbour_symmetry() RETURNS trigger AS
$$
BEGIN
    IF NOT EXISTS (SELECT 1
                   FROM hexagon_neighbours
                   WHERE hexagon_id = NEW.neighbour_hexagon_id
                     AND neighbour_hexagon_id = NEW.hexagon_id) THEN
        RAISE EXCEPTION 'Asymmetric hexagon neighbour link: % -> %', NEW.hexagon_id, NEW.neighbour_hexagon_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_hexagon_neighbour_symmetry
    AFTER INSERT
    ON hexagon_neighbours
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
EXECUTE FUNCTION check_hexagon_neighbour_symmetry();

CREATE TABLE user_hexagon_subscriptions
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES application_users (id) ON DELETE CASCADE,
    hexagon_id UUID        NOT NULL REFERENCES hexagons (id) ON DELETE RESTRICT,
    is_active  BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- One active primary subscription per user, enforced by PostgreSQL.
CREATE UNIQUE INDEX uq_user_active_subscription
    ON user_hexagon_subscriptions (user_id) WHERE is_active;

-- Fast "who is subscribed to hexagon X" lookups (needed for alerts later).
CREATE INDEX idx_subscriptions_hexagon_active
    ON user_hexagon_subscriptions (hexagon_id) WHERE is_active;
