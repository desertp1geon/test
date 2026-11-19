package com.example.hexmap.user;

import java.util.UUID;

public record ApplicationUser(UUID id, String displayName, boolean active) {
}
