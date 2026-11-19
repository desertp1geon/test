ALTER TABLE identity_provider_links
    ADD COLUMN external_subject_hash_key_version INTEGER
    NOT NULL DEFAULT 1;

ALTER TABLE identity_provider_links
    DROP CONSTRAINT uq_provider_external_subject;

ALTER TABLE identity_provider_links
    ADD CONSTRAINT uq_provider_external_subject
    UNIQUE (
        provider_type,
        external_subject_hash,
        external_subject_hash_key_version
    );

ALTER TABLE identity_provider_links
    ADD CONSTRAINT chk_external_subject_hash_version
    CHECK (external_subject_hash_key_version > 0);
