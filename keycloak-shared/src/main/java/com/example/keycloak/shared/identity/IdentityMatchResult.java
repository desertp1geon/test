package com.example.keycloak.shared.identity;

import java.util.UUID;

public record IdentityMatchResult(
        MatchStatus status,
        UUID identityRecordId,
        String keycloakUserId
) {

    public IdentityMatchResult {
        if (status == null) {
            throw new IllegalArgumentException("Match status is required");
        }

        if (status == MatchStatus.MATCHED) {
            if (identityRecordId == null ||
                    keycloakUserId == null ||
                    keycloakUserId.isBlank()) {
                throw new IllegalArgumentException(
                        "Matched result must contain identity and Keycloak user IDs"
                );
            }
        } else if (identityRecordId != null || keycloakUserId != null) {
            throw new IllegalArgumentException(
                    "Unmatched result cannot contain identity or Keycloak user IDs"
            );
        }
    }

    public enum MatchStatus {
        MATCHED,
        NOT_FOUND
    }

    public static IdentityMatchResult matched(
            UUID identityRecordId,
            String keycloakUserId
    ) {
        return new IdentityMatchResult(
                MatchStatus.MATCHED,
                identityRecordId,
                keycloakUserId
        );
    }

    public static IdentityMatchResult notFound() {
        return new IdentityMatchResult(
                MatchStatus.NOT_FOUND,
                null,
                null
        );
    }

    public boolean isMatched() {
        return status == MatchStatus.MATCHED;
    }
}
