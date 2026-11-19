package com.example.hexmap.hexagon;

import java.util.List;

public record HexagonDetailsDto(HexagonDto hexagon, List<NeighbourDto> neighbours) {

    public record NeighbourDto(String h3Index, String name, String regionName) {
    }
}
