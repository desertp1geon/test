package com.example.hexmap.subscription;

import java.time.Instant;

public record SubscriptionDto(
        String h3Index,
        String name,
        String regionName,
        double lat,
        double lng,
        Instant updatedAt
) {
}
