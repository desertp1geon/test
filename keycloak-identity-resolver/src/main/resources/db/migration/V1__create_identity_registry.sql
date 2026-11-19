CREATE TABLE identity_records
(
    id UUID PRIMARY KEY,

    identity_hash CHAR(64) NOT NULL,

    keycloak_user_id VARCHAR(255) NOT NULL,

    hash_key_version INTEGER NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_identity_records_hash
        UNIQUE (identity_hash, hash_key_version),

    CONSTRAINT uq_identity_records_keycloak_user
        UNIQUE (keycloak_user_id),

    CONSTRAINT chk_identity_hash_version
        CHECK (hash_key_version > 0)
);

CREATE TABLE identity_provider_links
(
    id UUID PRIMARY KEY,

    identity_record_id UUID NOT NULL,

    provider_type VARCHAR(32) NOT NULL,

    external_subject_hash CHAR(64) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    last_authenticated_at TIMESTAMPTZ,

    CONSTRAINT fk_provider_links_identity
        FOREIGN KEY (identity_record_id)
        REFERENCES identity_records (id)
        ON DELETE CASCADE,

    CONSTRAINT uq_provider_external_subject
        UNIQUE (provider_type, external_subject_hash),

    CONSTRAINT uq_identity_provider
        UNIQUE (identity_record_id, provider_type)
);

CREATE INDEX idx_identity_provider_links_identity
    ON identity_provider_links (identity_record_id);
