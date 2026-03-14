package com.pasantia.proyecto.roadsafety.service;

import com.pasantia.proyecto.roadsafety.util.SqlFilterUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ChildRestraintReportService {

    private final JdbcTemplate jdbc;

    public ChildRestraintReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Unidad: NIÑOS
     * total = suma categorías de niños (solo excluido=0)
     * cumple = niños en silla de retención (solo excluido=0)
     */
    public List<Map<String, Object>> byVehicle(
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
              rr.clase_vehiculo AS claseVehiculo,

              SUM(
                CASE WHEN rr.excluido = 0 THEN
                  COALESCE(rr.cantidad_nino_silla_retencion, 0)
                  + COALESCE(rr.cantidad_nino_brazos_piernas, 0)
                  + COALESCE(rr.cantidad_nino_con_cinturon, 0)
                  + COALESCE(rr.cantidad_nino_sin_retencion, 0)
                ELSE 0 END
              ) AS total,

              SUM(
                CASE WHEN rr.excluido = 0 THEN
                  COALESCE(rr.cantidad_nino_silla_retencion, 0)
                ELSE 0 END
              ) AS cumple

            FROM retencion_registros rr
            JOIN v_formatos_general fg ON fg.id = rr.formato_general_id
            JOIN municipio m ON m.id = fg.municipio_id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        SqlFilterUtil.applyFilters(sql, params, departamento, municipioId, puntoId, fechaInicio, fechaFin, jornada, dia);

        sql.append("""
            GROUP BY rr.clase_vehiculo
            HAVING total > 0
            ORDER BY total DESC
        """);

        return jdbc.queryForList(sql.toString(), params.toArray());
    }
}