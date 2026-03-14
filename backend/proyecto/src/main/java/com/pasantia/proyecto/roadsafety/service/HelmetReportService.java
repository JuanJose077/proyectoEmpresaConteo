package com.pasantia.proyecto.roadsafety.service;

import com.pasantia.proyecto.roadsafety.util.SqlFilterUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class HelmetReportService {

    private final JdbcTemplate jdbc;

    public HelmetReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Cumple:
     * - Conductor usa casco = 'Sí'
     * Nota: tu regla real de casco puede extenderse luego si lo necesitas.
     */
    public List<Map<String, Object>> getHelmetStats(
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
              cr.clase_vehiculo AS claseVehiculo,

              SUM(CASE WHEN cr.excluido = 0 THEN 1 ELSE 0 END) AS total,

              SUM(
                CASE
                  WHEN cr.excluido = 0
                   AND cr.usa_casco_conductor = 'Sí'
                  THEN 1 ELSE 0
                END
              ) AS cumple

            FROM casco_registros cr
            JOIN v_formatos_general fg ON fg.id = cr.formato_general_id
            JOIN municipio m ON m.id = fg.municipio_id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        SqlFilterUtil.applyFilters(sql, params, departamento, municipioId, puntoId, fechaInicio, fechaFin, jornada, dia);

        sql.append("""
            GROUP BY cr.clase_vehiculo
            HAVING total > 0
            ORDER BY total DESC
        """);

        // Esto ya retorna [{"claseVehiculo","total","cumple"}...]
        // Tu ReportResponseBuilder ya agrega noCumple y %.
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    /**
     * Si ya lo usabas para date-range, déjalo igual (esto es opcional).
     * Si no lo necesitas, lo puedes borrar.
     */
    public Map<String, Object> getHelmetDateRange(Integer municipioId, Integer puntoId) {
        StringBuilder sql = new StringBuilder("""
            SELECT
              MIN(fg.device_created_at) AS minDate,
              MAX(fg.device_created_at) AS maxDate
            FROM casco_registros cr
            JOIN v_formatos_general fg ON fg.id = cr.formato_general_id
            WHERE cr.excluido = 0
        """);

        List<Object> params = new ArrayList<>();
        if (municipioId != null) {
            sql.append(" AND fg.municipio_id = ? ");
            params.add(municipioId);
        }
        if (puntoId != null) {
            sql.append(" AND fg.coordenada_id = ? ");
            params.add(puntoId);
        }

        Map<String, Object> row = jdbc.queryForMap(sql.toString(), params.toArray());
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("minDate", row.get("minDate"));
        res.put("maxDate", row.get("maxDate"));
        return res;
    }
}