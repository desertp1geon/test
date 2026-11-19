package com.example.hexmap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hexmap.security")
public record HexmapSecurityProperties(
        String issuer,
        String jwkSetUri,
        String audience
) {
}
