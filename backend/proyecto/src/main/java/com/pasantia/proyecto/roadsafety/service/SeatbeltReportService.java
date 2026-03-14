package com.pasantia.proyecto.roadsafety.service;

import com.pasantia.proyecto.roadsafety.util.SqlFilterUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SeatbeltReportService {

    private final JdbcTemplate jdbc;

    public SeatbeltReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * total = registros válidos (excluido=0)
     * cumple:
     * - si tiene acompañante = 'Sí' => conductor 'Sí' y acompañante 'Sí'
     * - si no => conductor 'Sí'
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
              cr.clase_vehiculo AS claseVehiculo,

              SUM(CASE WHEN cr.excluido = 0 THEN 1 ELSE 0 END) AS total,

              SUM(
                CASE
                  WHEN cr.excluido = 0 AND (
                    (cr.tiene_acompanante = 'Sí'
                      AND cr.usa_cinturon_conductor = 'Sí'
                      AND cr.usa_cinturon_acompanante = 'Sí')
                    OR
                    (cr.tiene_acompanante <> 'Sí'
                      AND cr.usa_cinturon_conductor = 'Sí')
                  )
                  THEN 1 ELSE 0
                END
              ) AS cumple

            FROM cinturon_registros cr
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

        return jdbc.queryForList(sql.toString(), params.toArray());
    }
}