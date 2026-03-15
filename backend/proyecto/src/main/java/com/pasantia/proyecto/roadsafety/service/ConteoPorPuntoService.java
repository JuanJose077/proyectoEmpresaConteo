package com.pasantia.proyecto.roadsafety.service;

import com.pasantia.proyecto.roadsafety.util.SqlFilterUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConteoPorPuntoService {

    private final JdbcTemplate jdbc;

    public ConteoPorPuntoService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ConteoPorPuntoItem> getRankingPorPunto(
            String departamento,
            Integer municipioId,
            Integer puntoId,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String jornada,
            String dia,
            Integer limite
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                c.id AS puntoId,
                c.nombre AS punto,
                COUNT(fg.id) AS cantidad
            FROM v_formatos_general fg
            JOIN coordenada c ON c.id = fg.coordenada_id
            JOIN municipio m ON m.id = fg.municipio_id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        SqlFilterUtil.applyFilters(sql, params, departamento, municipioId, puntoId, fechaInicio, fechaFin, jornada, dia);

        sql.append("""
            GROUP BY c.id, c.nombre
            HAVING COUNT(fg.id) > 0
            ORDER BY cantidad DESC, c.nombre ASC
        """);

        if (limite != null && limite > 0) {
            sql.append(" LIMIT ? ");
            params.add(limite);
        }

        return jdbc.query(sql.toString(), (rs, rowNum) ->
                        new ConteoPorPuntoItem(
                                rs.getInt("puntoId"),
                                rs.getString("punto"),
                                rs.getLong("cantidad")
                        ),
                params.toArray()
        );
    }

    public List<ActividadDiariaItem> getActividadDiaria(
            String departamento,
            Integer municipioId,
            Integer puntoId,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String jornada,
            String dia
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                DATE(fg.device_created_at) AS fecha,
                COUNT(fg.id) AS cantidad
            FROM v_formatos_general fg
            JOIN municipio m ON m.id = fg.municipio_id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        SqlFilterUtil.applyFilters(sql, params, departamento, municipioId, puntoId, fechaInicio, fechaFin, jornada, dia);

        sql.append("""
            GROUP BY DATE(fg.device_created_at)
            ORDER BY fecha
        """);

        return jdbc.query(sql.toString(), (rs, rowNum) ->
                        new ActividadDiariaItem(
                                rs.getDate("fecha").toLocalDate(),
                                rs.getLong("cantidad")
                        ),
                params.toArray()
        );
    }

    public record ConteoPorPuntoItem(
            int puntoId,
            String punto,
            long cantidad
    ) {}

    public record ActividadDiariaItem(
            LocalDate fecha,
            long cantidad
    ) {}
}
