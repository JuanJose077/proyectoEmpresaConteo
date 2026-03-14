package com.pasantia.proyecto.roadsafety.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/puntos")
public class PointController {

    private final JdbcTemplate jdbc;

    public PointController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<Map<String, Object>> getPuntos(
            @RequestParam Integer municipioId
    ) {
        return jdbc.queryForList("""
            SELECT id, nombre, latitud, longitud, municipio_id
            FROM coordenada
            WHERE municipio_id = ?
            ORDER BY nombre
        """, municipioId);
    }
}