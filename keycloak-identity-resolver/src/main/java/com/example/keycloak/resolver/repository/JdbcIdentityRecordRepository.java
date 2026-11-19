package com.example.keycloak.resolver.repository;

import com.example.keycloak.resolver.config.IdentityRegistryConfig;
import com.example.keycloak.resolver.exception.IdentityConflictException;
import com.example.keycloak.resolver.exception.IdentityRegistryException;
import com.example.keycloak.shared.crypto.IdentityHash;
import com.example.keycloak.shared.identity.IdentityProviderType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;

public final class JdbcIdentityRecordRepository
        implements IdentityRecordRepository {

    private static final String FIND_BY_HASH_SQL = """
            SELECT id, keycloak_user_id
            FROM identity_records
            WHERE identity_hash = ?
              AND hash_key_version = ?
            """;

    private static final String INSERT_IDENTITY_SQL = """
            INSERT INTO identity_records (
                id,
                identity_hash,
                keycloak_user_id,
                hash_key_version
            )
            VALUES (?, ?, ?, ?)
            ON CONFLICT (identity_hash, hash_key_version)
            DO NOTHING
            """;

    private static final String FIND_IDENTITY_FOR_UPDATE_SQL = """
            SELECT id, keycloak_user_id
            FROM identity_records
            WHERE identity_hash = ?
              AND hash_key_version = ?
            FOR UPDATE
            """;

    private static final String FIND_PROVIDER_LINK_SQL = """
            SELECT identity_record_id
            FROM identity_provider_links
            WHERE provider_type = ?
              AND external_subject_hash = ?
              AND external_subject_hash_key_version = ?
            """;

    private static final String FIND_IDENTITY_PROVIDER_LINK_SQL = """
            SELECT external_subject_hash,
                   external_subject_hash_key_version
            FROM identity_provider_links
            WHERE identity_record_id = ?
              AND provider_type = ?
            """;

    private static final String INSERT_PROVIDER_LINK_SQL = """
            INSERT INTO identity_provider_links (
                id,
                identity_record_id,
                provider_type,
                external_subject_hash,
                external_subject_hash_key_version
            )
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT DO NOTHING
            """;

    private final IdentityRegistryConfig config;

    public JdbcIdentityRecordRepository(
            IdentityRegistryConfig config
    ) {
        this.config = config;
    }

    @Override
    public Optional<IdentityRecord> findByIdentityHash(
            IdentityHash identityHash
    ) {
        try (
                Connection connection = openConnection();
                PreparedStatement statement =
                        connection.prepareStatement(FIND_BY_HASH_SQL)
        ) {
            statement.setString(1, identityHash.value());
            statement.setInt(2, identityHash.keyVersion());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(new IdentityRecord(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("keycloak_user_id")
                ));
            }

        } catch (SQLException exception) {
            throw new IdentityRegistryException(
                    "Failed to find identity record",
                    exception
            );
        }
    }

    @Override
    public IdentityRegistrationResult register(
            IdentityHash identityHash,
            IdentityHash externalSubjectHash,
            IdentityProviderType providerType,
            String keycloakUserId
    ) {
        if (identityHash == null ||
                externalSubjectHash == null ||
                providerType == null) {
            throw new IllegalArgumentException(
                    "Identity registration parameters are required"
            );
        }

        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            throw new IllegalArgumentException(
                    "Keycloak user ID is required"
            );
        }

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);

            try {
                boolean identityCreated = insertIdentity(
                        connection,
                        identityHash,
                        keycloakUserId
                );

                IdentityRecord identityRecord = findIdentityForUpdate(
                        connection,
                        identityHash
                );

                if (identityRecord == null) {
                    throw new IdentityRegistryException(
                            "Identity record was not found after insertion"
                    );
                }

               boolean providerLinkCreated = ensureProviderLink(
 		       	connection,
        		identityRecord.id(),
        		externalSubjectHash,
        		providerType
		); 

                connection.commit();

                return new IdentityRegistrationResult(
                        identityRecord.id(),
                        identityRecord.keycloakUserId(),
                        identityCreated,
                        true
                );

            } catch (Exception exception) {
                rollback(connection, exception);

                if (exception instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }

                throw new IdentityRegistryException(
                        "Identity registration failed",
                        exception
                );
            }

        } catch (SQLException exception) {
            throw new IdentityRegistryException(
                    "Unable to execute identity registration transaction",
                    exception
            );
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
                config.jdbcUrl(),
                config.username(),
                config.password()
        );
    }

    private boolean insertIdentity(
            Connection connection,
            IdentityHash identityHash,
            String keycloakUserId
    ) throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(INSERT_IDENTITY_SQL)) {

            statement.setObject(1, UUID.randomUUID());
            statement.setString(2, identityHash.value());
            statement.setString(3, keycloakUserId);
            statement.setInt(4, identityHash.keyVersion());

            return statement.executeUpdate() == 1;
        }
    }

    private IdentityRecord findIdentityForUpdate(
            Connection connection,
            IdentityHash identityHash
    ) throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             FIND_IDENTITY_FOR_UPDATE_SQL
                     )) {

            statement.setString(1, identityHash.value());
            statement.setInt(2, identityHash.keyVersion());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                return new IdentityRecord(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("keycloak_user_id")
                );
            }
        }
    }

   private boolean ensureProviderLink(
        Connection connection,
        UUID identityRecordId,
        IdentityHash externalSubjectHash,
        IdentityProviderType providerType
) throws SQLException {

    UUID existingIdentityId = findProviderLink(
            connection,
            externalSubjectHash,
            providerType
    );

    if (existingIdentityId != null) {
        if (!existingIdentityId.equals(identityRecordId)) {
            throw new IdentityConflictException(
                    "Provider account is already linked to another identity"
            );
        }

        return false; 
    }

    ExistingProviderLink existingProviderLink =
            findIdentityProviderLink(
                    connection,
                    identityRecordId,
                    providerType
            );

    if (existingProviderLink != null) {
        if (!existingProviderLink.hash().equals(
                externalSubjectHash.value()
        ) || existingProviderLink.keyVersion() !=
                externalSubjectHash.keyVersion()) {

            throw new IdentityConflictException(
                    "Identity already has another account for this provider"
            );
        }

        return false;
    }

    insertProviderLink(
            connection,
            identityRecordId,
            externalSubjectHash,
            providerType
    );

    UUID linkedIdentityId = findProviderLink(
            connection,
            externalSubjectHash,
            providerType
    );

    if (!identityRecordId.equals(linkedIdentityId)) {
        throw new IdentityConflictException(
                "Provider account could not be linked to the requested identity"
        );
    }

    return true;
} 

    private UUID findProviderLink(
            Connection connection,
            IdentityHash externalSubjectHash,
            IdentityProviderType providerType
    ) throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(FIND_PROVIDER_LINK_SQL)) {

            statement.setString(1, providerType.getAlias());
            statement.setString(2, externalSubjectHash.value());
            statement.setInt(3, externalSubjectHash.keyVersion());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                return resultSet.getObject(
                        "identity_record_id",
                        UUID.class
                );
            }
        }
    }

    private ExistingProviderLink findIdentityProviderLink(
            Connection connection,
            UUID identityRecordId,
            IdentityProviderType providerType
    ) throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             FIND_IDENTITY_PROVIDER_LINK_SQL
                     )) {

            statement.setObject(1, identityRecordId);
            statement.setString(2, providerType.getAlias());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                return new ExistingProviderLink(
                        resultSet.getString("external_subject_hash"),
                        resultSet.getInt(
                                "external_subject_hash_key_version"
                        )
                );
            }
        }
    }

    private void insertProviderLink(
            Connection connection,
            UUID identityRecordId,
            IdentityHash externalSubjectHash,
            IdentityProviderType providerType
    ) throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(INSERT_PROVIDER_LINK_SQL)) {

            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, identityRecordId);
            statement.setString(3, providerType.getAlias());
            statement.setString(4, externalSubjectHash.value());
            statement.setInt(5, externalSubjectHash.keyVersion());

            statement.executeUpdate();
        }
    }

    private void rollback(
            Connection connection,
            Exception originalException
    ) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }

    private record ExistingProviderLink(
            String hash,
            int keyVersion
    ) {
    }
}
