package com.example.hexmap.subscription;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SubscriptionService {

    private final JdbcClient jdbc;

    public SubscriptionService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public Optional<SubscriptionDto> current(UUID userId) {
        return jdbc.sql("""
                        SELECT h.h3_index, h.name, h.region_name, h.center_lat, h.center_lng, s.updated_at
                        FROM user_hexagon_subscriptions s
                        JOIN hexagons h ON h.id = s.hexagon_id
                        WHERE s.user_id = :user AND s.is_active
                        """)
                .param("user", userId)
                .query(SubscriptionService::map)
                .optional();
    }

    /**
     * Creates the user's primary subscription or moves it to another hexagon.
     * A single atomic statement backed by the partial unique index
     * uq_user_active_subscription, so concurrent requests can never produce two
     * active subscriptions.
     */
    @Transactional
    public SubscriptionDto subscribe(UUID userId, String h3Index) {
        int rows = jdbc.sql("""
                        INSERT INTO user_hexagon_subscriptions (user_id, hexagon_id)
                        SELECT :user, h.id FROM hexagons h WHERE h.h3_index = :h3 AND h.is_active
                        ON CONFLICT (user_id) WHERE is_active
                        DO UPDATE SET hexagon_id = EXCLUDED.hexagon_id, updated_at = now()
                        """)
                .param("user", userId)
                .param("h3", h3Index)
                .update();
        if (rows == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Hexagon not found");
        }
        return current(userId).orElseThrow(() -> new IllegalStateException("Subscription was not saved"));
    }

    /** Idempotent: deactivates the active subscription if there is one. */
    @Transactional
    public void unsubscribe(UUID userId) {
        jdbc.sql("""
                        UPDATE user_hexagon_subscriptions
                        SET is_active = FALSE, updated_at = now()
                        WHERE user_id = :user AND is_active
                        """)
                .param("user", userId)
                .update();
    }

    private static SubscriptionDto map(ResultSet rs, int row) throws SQLException {
        return new SubscriptionDto(
                rs.getString("h3_index"),
                rs.getString("name"),
                rs.getString("region_name"),
                rs.getDouble("center_lat"),
                rs.getDouble("center_lng"),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant());
    }
}
