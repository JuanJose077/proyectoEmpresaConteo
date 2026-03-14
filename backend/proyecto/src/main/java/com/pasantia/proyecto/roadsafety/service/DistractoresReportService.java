package com.pasantia.proyecto.roadsafety.service;

import com.pasantia.proyecto.roadsafety.util.SqlFilterUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class DistractoresReportService {

    private final JdbcTemplate jdbc;

    public DistractoresReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Devuelve 2 filas:
     * - "Conductor"
     * - "Peatón"
     *
     * total = excluido=0
     * cumple = ninguna='Sí'
     */
    public List<Map<String, Object>> resumen(
            String departamento,
            Integer municipioId,
            Integer puntoId,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String jornada,
            String dia
    ) {
        List<Map<String, Object>> out = new ArrayList<>();
        out.addAll(conductores(departamento, municipioId, puntoId, fechaInicio, fechaFin, jornada, dia));
        out.addAll(peatones(departamento, municipioId, puntoId, fechaInicio, fechaFin, jornada, dia));
        return out;
    }

    private List<Map<String, Object>> conductores(
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
          dc.clase_vehiculo AS claseVehiculo,

          SUM(CASE WHEN dc.excluido = 0 THEN 1 ELSE 0 END) AS total,

          SUM(
            CASE
              WHEN dc.excluido = 0
               AND dc.ninguna = 'Sí'
              THEN 1 ELSE 0
            END
          ) AS cumple

        FROM distraccion_conductor_registros dc
        JOIN v_formatos_general fg ON fg.id = dc.formato_general_id
        JOIN municipio m ON m.id = fg.municipio_id
        WHERE 1=1
    """);

        List<Object> params = new ArrayList<>();
        SqlFilterUtil.applyFilters(sql, params, departamento, municipioId, puntoId, fechaInicio, fechaFin, jornada, dia);

        sql.append("""
        GROUP BY dc.clase_vehiculo
        HAVING total > 0
        ORDER BY total DESC
    """);

        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    private List<Map<String, Object>> peatones(
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
              'Peatón' AS claseVehiculo,

              SUM(CASE WHEN dp.excluido = 0 THEN 1 ELSE 0 END) AS total,

              SUM(
                CASE
                  WHEN dp.excluido = 0
                   AND dp.ninguna = 'Sí'
                  THEN 1 ELSE 0
                END
              ) AS cumple

            FROM distraccion_peaton_registros dp
            JOIN v_formatos_general fg ON fg.id = dp.formato_general_id
            JOIN municipio m ON m.id = fg.municipio_id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        SqlFilterUtil.applyFilters(sql, params, departamento, municipioId, puntoId, fechaInicio, fechaFin, jornada, dia);

        return jdbc.queryForList(sql.toString(), params.toArray());
    }
}