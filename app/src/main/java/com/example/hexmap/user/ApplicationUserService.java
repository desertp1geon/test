package com.example.hexmap.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Maps a verified Keycloak identity to a local profile.
 * The only key is the JWT "sub" claim; e-mail is never used to identify or merge users.
 */
@Service
public class ApplicationUserService {

    private final JdbcClient jdbc;

    public ApplicationUserService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Returns the local profile for the token's subject, creating it on first use. */
    @Transactional
    public ApplicationUser resolve(Jwt jwt) {
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token has no subject");
        }

        ApplicationUser user = find(sub).orElseGet(() -> {
            jdbc.sql("""
                            INSERT INTO application_users (keycloak_user_id, display_name)
                            VALUES (:sub, :name)
                            ON CONFLICT (keycloak_user_id) DO NOTHING
                            """)
                    .param("sub", sub)
                    .param("name", jwt.getClaimAsString("preferred_username"))
                    .update();
            return find(sub).orElseThrow(() -> new IllegalStateException("User profile was not created"));
        });

        if (!user.active()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User profile is disabled");
        }
        return user;
    }

    private Optional<ApplicationUser> find(String keycloakUserId) {
        return jdbc.sql("SELECT id, display_name, is_active FROM application_users WHERE keycloak_user_id = :sub")
                .param("sub", keycloakUserId)
                .query(ApplicationUserService::map)
                .optional();
    }

    private static ApplicationUser map(ResultSet rs, int row) throws SQLException {
        return new ApplicationUser(
                rs.getObject("id", UUID.class),
                rs.getString("display_name"),
                rs.getBoolean("is_active"));
    }
}
