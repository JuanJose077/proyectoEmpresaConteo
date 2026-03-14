package com.pasantia.proyecto.roadsafety.api;


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/municipios")
public class MunicipioController {

    private final JdbcTemplate jdbc;

    public MunicipioController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<Map<String, Object>> getMunicipios(
            @RequestParam(required = false) String departamento
    ) {
        if (departamento == null || departamento.isBlank()) {
            return jdbc.queryForList("""
                SELECT id, nombre, departamento
                FROM municipio
                ORDER BY departamento, nombre
            """);
        }

        return jdbc.queryForList("""
            SELECT id, nombre, departamento
            FROM municipio
            WHERE departamento = ?
            ORDER BY nombre
        """, departamento);
    }
}