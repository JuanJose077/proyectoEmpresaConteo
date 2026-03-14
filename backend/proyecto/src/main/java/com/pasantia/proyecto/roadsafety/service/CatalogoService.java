package com.pasantia.proyecto.roadsafety.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogoService {

    private final JdbcTemplate jdbc;

    public CatalogoService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<String> getDepartamentosConDatos() {
        String sql = """
    SELECT DISTINCT TRIM(m.departamento) AS departamento
    FROM municipio m
    JOIN v_formatos_general fg ON fg.municipio_id = m.id
    WHERE m.departamento IS NOT NULL AND TRIM(m.departamento) <> ''
    ORDER BY TRIM(m.departamento)
""";

        return jdbc.queryForList(sql, String.class);
    }
    public List<String> getTodosLosDepartamentos() {
        String sql = """
        SELECT DISTINCT TRIM(m.departamento) AS departamento
        FROM municipio m
        WHERE m.departamento IS NOT NULL
          AND TRIM(m.departamento) <> ''
        ORDER BY TRIM(m.departamento)
    """;

        return jdbc.queryForList(sql, String.class);
    }

    public List<MunicipioItem> getMunicipiosConDatosPorDepartamento(String departamento) {
        String sql = """
            SELECT DISTINCT m.id, m.nombre
            FROM municipio m
            JOIN v_formatos_general fg ON fg.municipio_id = m.id
            WHERE m.departamento = ?
            ORDER BY m.nombre
        """;

        return jdbc.query(sql, (rs, rowNum) ->
                        new MunicipioItem(rs.getInt("id"), rs.getString("nombre")),
                departamento
        );
    }

    public record MunicipioItem(int id, String nombre) {}
}