package com.example.hexmap.hexagon;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class HexagonRepository {

    private static final String SELECT_HEXAGON = """
            SELECT h.h3_index, h.name, h.region_name, h.region_code,
                   h.center_lat, h.center_lng, h.boundary::text AS boundary
            FROM hexagons h
            """;

    private final JdbcClient jdbc;
    private final ObjectMapper objectMapper;

    public HexagonRepository(JdbcClient jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public List<HexagonDto> findAllActive() {
        return jdbc.sql(SELECT_HEXAGON + " WHERE h.is_active ORDER BY h.h3_index")
                .query(this::mapHexagon)
                .list();
    }

    public Optional<HexagonDto> findByH3Index(String h3Index) {
        return jdbc.sql(SELECT_HEXAGON + " WHERE h.h3_index = :h3 AND h.is_active")
                .param("h3", h3Index)
                .query(this::mapHexagon)
                .optional();
    }

    public List<HexagonDetailsDto.NeighbourDto> findNeighbours(String h3Index) {
        return jdbc.sql("""
                        SELECT n.h3_index, n.name, n.region_name
                        FROM hexagons h
                        JOIN hexagon_neighbours hn ON hn.hexagon_id = h.id
                        JOIN hexagons n            ON n.id = hn.neighbour_hexagon_id
                        WHERE h.h3_index = :h3 AND n.is_active
                        ORDER BY n.h3_index
                        """)
                .param("h3", h3Index)
                .query((rs, i) -> new HexagonDetailsDto.NeighbourDto(
                        rs.getString("h3_index"), rs.getString("name"), rs.getString("region_name")))
                .list();
    }

    private HexagonDto mapHexagon(ResultSet rs, int row) throws SQLException {
        try {
            return new HexagonDto(
                    rs.getString("h3_index"),
                    rs.getString("name"),
                    rs.getString("region_name"),
                    rs.getString("region_code"),
                    rs.getDouble("center_lat"),
                    rs.getDouble("center_lng"),
                    objectMapper.readValue(rs.getString("boundary"), double[][].class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupted hexagon boundary", e);
        }
    }
}
