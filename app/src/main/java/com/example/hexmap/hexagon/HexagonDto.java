package com.example.hexmap.hexagon;

/** Map cell. {@code boundary} is [[lat, lng] x 6]. */
public record HexagonDto(
        String h3Index,
        String name,
        String regionName,
        String regionCode,
        double lat,
        double lng,
        double[][] boundary
) {
}
