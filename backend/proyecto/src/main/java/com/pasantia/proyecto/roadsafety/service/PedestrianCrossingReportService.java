package com.pasantia.proyecto.roadsafety.service;

import com.pasantia.proyecto.roadsafety.util.SqlFilterUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PedestrianCrossingReportService {

    private final JdbcTemplate jdbc;

    public PedestrianCrossingReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }


    public List<Map<String, Object>> getStats(
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

              SUM(CASE WHEN pcr.excluido = 0 THEN 1 ELSE 0 END) AS total,

              SUM(
                CASE
                  WHEN pcr.excluido = 0
                   AND pcr.cruce_indebido <> 'Sí'
                  THEN 1 ELSE 0
                END
              ) AS cumple

            FROM peaton_cruce_registros pcr
            JOIN v_formatos_general fg ON fg.id = pcr.formato_general_id
            JOIN municipio m ON m.id = fg.municipio_id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        SqlFilterUtil.applyFilters(sql, params, departamento, municipioId, puntoId, fechaInicio, fechaFin, jornada, dia);

        return jdbc.queryForList(sql.toString(), params.toArray());
    }
}