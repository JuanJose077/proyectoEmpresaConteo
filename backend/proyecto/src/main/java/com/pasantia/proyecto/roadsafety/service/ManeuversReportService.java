package com.pasantia.proyecto.roadsafety.service;

import com.pasantia.proyecto.roadsafety.util.SqlFilterUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ManeuversReportService {

    private final JdbcTemplate jdbc;

    public ManeuversReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * cumple = ninguna='Sí'
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
              mr.clase_vehiculo AS claseVehiculo,

              SUM(CASE WHEN mr.excluido = 0 THEN 1 ELSE 0 END) AS total,

              SUM(
                CASE
                  WHEN mr.excluido = 0
                   AND mr.ninguna = 'Sí'
                  THEN 1 ELSE 0
                END
              ) AS cumple

            FROM maniobras_registros mr
            JOIN v_formatos_general fg ON fg.id = mr.formato_general_id
            JOIN municipio m ON m.id = fg.municipio_id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        SqlFilterUtil.applyFilters(sql, params, departamento, municipioId, puntoId, fechaInicio, fechaFin, jornada, dia);

        sql.append("""
            GROUP BY mr.clase_vehiculo
            HAVING total > 0
            ORDER BY total DESC
        """);

        return jdbc.queryForList(sql.toString(), params.toArray());
    }
}