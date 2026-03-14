package com.pasantia.proyecto.roadsafety.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PedestrianDistractionReportService {

    private final JdbcTemplate jdbc;

    public PedestrianDistractionReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static String picoExpr() {
        return """
          (
            TIME(fg.device_created_at) BETWEEN '05:30:00' AND '08:00:00'
            OR TIME(fg.device_created_at) BETWEEN '11:00:00' AND '13:00:00'
            OR TIME(fg.device_created_at) BETWEEN '16:30:00' AND '18:30:00'
          )
        """;
    }

    private void applyFilters(StringBuilder sql, List<Object> params,
                              Integer municipioId,
                              Integer puntoId,
                              LocalDate fechaInicio,
                              LocalDate fechaFin,
                              String jornada,
                              String dia) {

        if (municipioId != null) {
            sql.append(" AND fg.municipio_id = ? ");
            params.add(municipioId);
        }

        if (puntoId != null) {
            sql.append(" AND fg.coordenada_id = ? ");
            params.add(puntoId);
        }

        if (fechaInicio != null) {
            sql.append(" AND fg.device_created_at >= ? ");
            params.add(Date.valueOf(fechaInicio));
        }

        if (fechaFin != null) {
            sql.append(" AND fg.device_created_at < ? ");
            params.add(Date.valueOf(fechaFin.plusDays(1)));
        }

        if ("HABIL".equalsIgnoreCase(dia)) {
            sql.append(" AND DAYOFWEEK(fg.device_created_at) BETWEEN 2 AND 6 ");
        } else if ("NO_HABIL".equalsIgnoreCase(dia)) {
            sql.append(" AND DAYOFWEEK(fg.device_created_at) IN (1,7) ");
        }

        if ("PICO".equalsIgnoreCase(jornada)) {
            sql.append(" AND ").append(picoExpr());
        } else if ("VALLE".equalsIgnoreCase(jornada)) {
            sql.append(" AND NOT ").append(picoExpr());
        }
    }

    public List<Map<String, Object>> getStats(Integer municipioId,
                                              Integer puntoId,
                                              LocalDate fechaInicio,
                                              LocalDate fechaFin,
                                              String jornada,
                                              String dia) {

        StringBuilder sql = new StringBuilder("""
            SELECT
              'Peatón' AS claseVehiculo,
              COUNT(*) AS total,
              SUM(
                CASE
                  WHEN pr.excluido = 0 AND pr.ninguna = 'Sí'
                  THEN 1 ELSE 0
                END
              ) AS cumple
            FROM distraccion_peaton_registros pr
            JOIN v_formatos_general fg ON fg.id = pr.formato_general_id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        applyFilters(sql, params, municipioId, puntoId, fechaInicio, fechaFin, jornada, dia);

        return jdbc.queryForList(sql.toString(), params.toArray());
    }
}