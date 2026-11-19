package com.example.hexmap.hexagon;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Public, read-only map data. */
@RestController
@RequestMapping("/api/hexagons")
public class HexagonController {

    private static final Pattern H3_INDEX = Pattern.compile("^[0-9a-f]{15}$");

    private final HexagonRepository repository;

    public HexagonController(HexagonRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<HexagonDto>> all() {
        // Geometry changes only with a migration, so a short client cache is safe.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                .body(repository.findAllActive());
    }

    @GetMapping("/{h3Index}")
    public HexagonDetailsDto details(@PathVariable String h3Index) {
        if (!H3_INDEX.matcher(h3Index).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid H3 index");
        }
        HexagonDto hexagon = repository.findByH3Index(h3Index)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hexagon not found"));
        return new HexagonDetailsDto(hexagon, repository.findNeighbours(h3Index));
    }
}
