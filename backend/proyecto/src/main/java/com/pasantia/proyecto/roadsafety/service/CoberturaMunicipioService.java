package com.pasantia.proyecto.roadsafety.service;

import com.pasantia.proyecto.roadsafety.util.SqlFilterUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CoberturaMunicipioService {

    private final JdbcTemplate jdbc;

    public CoberturaMunicipioService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ResumenCobertura getResumenPorDepartamento(
            String departamento,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String jornada,
            String dia
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                COUNT(*) AS totalMunicipios,
                SUM(CASE WHEN x.conteo > 0 THEN 1 ELSE 0 END) AS municipiosConDatos,
                SUM(CASE WHEN x.conteo = 0 THEN 1 ELSE 0 END) AS municipiosSinDatos,
                ROUND(
                    (SUM(CASE WHEN x.conteo > 0 THEN 1 ELSE 0 END) * 100.0) / COUNT(*),
                    1
                ) AS cobertura
            FROM (
                SELECT
                    m.id,
                    COUNT(fg_filtrado.id) AS conteo
                FROM municipio m
                LEFT JOIN (
                    SELECT fg.id, fg.municipio_id, fg.device_created_at
                    FROM v_formatos_general fg
                    JOIN municipio m ON m.id = fg.municipio_id
                    WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        SqlFilterUtil.applyFilters(sql, params, departamento, null, null, fechaInicio, fechaFin, jornada, dia);

        sql.append("""
                ) fg_filtrado
                    ON fg_filtrado.municipio_id = m.id
                WHERE TRIM(m.departamento) = TRIM(?)
                GROUP BY m.id
            ) x
        """);

        params.add(departamento);

        return jdbc.queryForObject(sql.toString(), (rs, rowNum) ->
                        new ResumenCobertura(
                                rs.getInt("totalMunicipios"),
                                rs.getInt("municipiosConDatos"),
                                rs.getInt("municipiosSinDatos"),
                                rs.getDouble("cobertura")
                        ),
                params.toArray()
        );
    }

    public List<DetalleMunicipio> getDetallePorDepartamento(
            String departamento,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String jornada,
            String dia
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                m.id AS municipioId,
                m.nombre AS municipio,
                COUNT(fg_filtrado.id) AS conteo,
                CASE
                    WHEN COUNT(fg_filtrado.id) > 0 THEN true
                    ELSE false
                END AS tieneDatos,
                MAX(fg_filtrado.device_created_at) AS ultimaToma
            FROM municipio m
            LEFT JOIN (
                SELECT fg.id, fg.municipio_id, fg.device_created_at
                FROM v_formatos_general fg
                JOIN municipio m ON m.id = fg.municipio_id
                WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        SqlFilterUtil.applyFilters(sql, params, departamento, null, null, fechaInicio, fechaFin, jornada, dia);

        sql.append("""
            ) fg_filtrado
                ON fg_filtrado.municipio_id = m.id
            WHERE TRIM(m.departamento) = TRIM(?)
            GROUP BY m.id, m.nombre
            ORDER BY m.nombre
        """);

        params.add(departamento);

        return jdbc.query(sql.toString(), (rs, rowNum) -> {
                    Timestamp ts = rs.getTimestamp("ultimaToma");
                    LocalDateTime ultimaToma = (ts != null) ? ts.toLocalDateTime() : null;

                    return new DetalleMunicipio(
                            rs.getInt("municipioId"),
                            rs.getString("municipio"),
                            rs.getBoolean("tieneDatos"),
                            rs.getInt("conteo"),
                            ultimaToma
                    );
                },
                params.toArray()
        );
    }

    public List<RankingMunicipio> getRankingMasConteos(
            String departamento,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String jornada,
            String dia,
            int limite
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                m.id AS municipioId,
                m.nombre AS municipio,
                COUNT(fg_filtrado.id) AS conteo
            FROM municipio m
            LEFT JOIN (
                SELECT fg.id, fg.municipio_id, fg.device_created_at
                FROM v_formatos_general fg
                JOIN municipio m ON m.id = fg.municipio_id
                WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        SqlFilterUtil.applyFilters(sql, params, departamento, null, null, fechaInicio, fechaFin, jornada, dia);

        sql.append("""
            ) fg_filtrado
                ON fg_filtrado.municipio_id = m.id
            WHERE TRIM(m.departamento) = TRIM(?)
            GROUP BY m.id, m.nombre
            HAVING COUNT(fg_filtrado.id) > 0
            ORDER BY conteo DESC, m.nombre ASC
            LIMIT ?
        """);

        params.add(departamento);
        params.add(limite);

        return jdbc.query(sql.toString(), (rs, rowNum) ->
                        new RankingMunicipio(
                                rs.getInt("municipioId"),
                                rs.getString("municipio"),
                                rs.getInt("conteo")
                        ),
                params.toArray()
        );
    }

    public List<RankingMunicipio> getRankingMenosConteos(
            String departamento,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String jornada,
            String dia,
            int limite
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                m.id AS municipioId,
                m.nombre AS municipio,
                COUNT(fg_filtrado.id) AS conteo
            FROM municipio m
            LEFT JOIN (
                SELECT fg.id, fg.municipio_id, fg.device_created_at
                FROM v_formatos_general fg
                JOIN municipio m ON m.id = fg.municipio_id
                WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        SqlFilterUtil.applyFilters(sql, params, departamento, null, null, fechaInicio, fechaFin, jornada, dia);

        sql.append("""
            ) fg_filtrado
                ON fg_filtrado.municipio_id = m.id
            WHERE TRIM(m.departamento) = TRIM(?)
            GROUP BY m.id, m.nombre
            HAVING COUNT(fg_filtrado.id) > 0
            ORDER BY conteo ASC, m.nombre ASC
            LIMIT ?
        """);

        params.add(departamento);
        params.add(limite);

        return jdbc.query(sql.toString(), (rs, rowNum) ->
                        new RankingMunicipio(
                                rs.getInt("municipioId"),
                                rs.getString("municipio"),
                                rs.getInt("conteo")
                        ),
                params.toArray()
        );
    }

    public record ResumenCobertura(
            int totalMunicipios,
            int municipiosConDatos,
            int municipiosSinDatos,
            double cobertura
    ) {}

    public record DetalleMunicipio(
            int municipioId,
            String municipio,
            boolean tieneDatos,
            int conteo,
            LocalDateTime ultimaToma
    ) {}

    public record RankingMunicipio(
            int municipioId,
            String municipio,
            int conteo
    ) {}
}