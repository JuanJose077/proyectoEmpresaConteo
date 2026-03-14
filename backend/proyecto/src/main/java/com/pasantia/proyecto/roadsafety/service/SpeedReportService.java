package com.pasantia.proyecto.roadsafety.service;

import com.pasantia.proyecto.roadsafety.util.SqlFilterUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SpeedReportService {

    private final JdbcTemplate jdbc;

    public SpeedReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

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
              vr.clase_vehiculo AS claseVehiculo,

              SUM(CASE WHEN vr.excluido = 0 THEN 1 ELSE 0 END) AS total,

              SUM(
                CASE
                  WHEN vr.excluido = 0
                   AND vr.velocidad_registrada <= CAST(v.nombre AS UNSIGNED)
                  THEN 1 ELSE 0
                END
              ) AS cumple

            FROM velocidad_registros vr
            JOIN v_formatos_general fg
              ON fg.id = vr.formato_general_id
            JOIN municipio m
              ON m.id = fg.municipio_id

            JOIN (
                SELECT
                    formato_id,
                    municipio_id,
                    coordenada_id,
                    clima_id,
                    periodo_id,
                    MAX(velocidad_id) AS velocidad_id
                FROM formato_velocidad_general
                GROUP BY
                    formato_id,
                    municipio_id,
                    coordenada_id,
                    clima_id,
                    periodo_id
            ) fvg
              ON fvg.formato_id = fg.formato_id
             AND fvg.municipio_id = fg.municipio_id
             AND fvg.coordenada_id = fg.coordenada_id
             AND fvg.clima_id = fg.clima_id
             AND fvg.periodo_id = fg.periodo_id

            JOIN velocidad v
              ON v.id = fvg.velocidad_id

            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        SqlFilterUtil.applyFilters(sql, params, departamento, municipioId, puntoId, fechaInicio, fechaFin, jornada, dia);

        sql.append("""
            GROUP BY vr.clase_vehiculo
            HAVING total > 0
            ORDER BY total DESC
        """);

        return jdbc.queryForList(sql.toString(), params.toArray());
    }
}