package com.pasantia.proyecto.roadsafety.service;

import com.pasantia.proyecto.roadsafety.util.SqlFilterUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OveroccupancyReportService {

    private final JdbcTemplate jdbc;

    public OveroccupancyReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * REGLA (estricta):
     * total = registros válidos (excluido=0)
     *
     * cumple si NO ocurre ninguno:
     * 1) ocupantes > capacidad_vehiculo
     *    donde ocupantes = 1 (conductor) + (acompañantes si tiene_acompanante='Sí')
     * 2) carga_sobredimensionada = 'Sí'
     * 3) (solo motos) ninos_en_motocicleta='Sí' y (ninos_sin_casco > 0 o ninos_brazos_piernas > 0)
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
              sr.clase_vehiculo AS claseVehiculo,

              -- total: solo registros válidos
              SUM(CASE WHEN sr.excluido = 0 THEN 1 ELSE 0 END) AS total,

              -- cumple: no sobreocupación + no carga sobredimensionada + no niños en condición peligrosa (moto)
              SUM(
                CASE
                  WHEN sr.excluido = 0
                   AND NOT (
                        -- (1) Sobreocupación: ocupantes > capacidad
                        (
                          (
                            1 + CASE
                                  WHEN sr.tiene_acompanante = 'Sí'
                                  THEN COALESCE(sr.cantidad_masculino, 0)
                                     + COALESCE(sr.cantidad_femenino, 0)
                                     + COALESCE(sr.cantidad_no_identificado, 0)
                                  ELSE 0
                                END
                          ) > COALESCE(sr.capacidad_vehiculo, 0)
                        )

                        OR
                        -- (2) Carga sobredimensionada
                        (sr.carga_sobredimensionada = 'Sí')

                        OR
                        -- (3) Niños en moto en condición peligrosa
                        (
                          sr.clase_vehiculo = 'Motocicleta'
                          AND sr.ninos_en_motocicleta = 'Sí'
                          AND (
                            COALESCE(sr.ninos_sin_casco, 0) > 0
                            OR COALESCE(sr.ninos_brazos_piernas, 0) > 0
                          )
                        )
                   )
                  THEN 1 ELSE 0
                END
              ) AS cumple

            FROM sobreocupacion_registros sr
            JOIN v_formatos_general fg ON fg.id = sr.formato_general_id
            JOIN municipio m ON m.id = fg.municipio_id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        SqlFilterUtil.applyFilters(sql, params, departamento, municipioId, puntoId, fechaInicio, fechaFin, jornada, dia);

        sql.append("""
            GROUP BY sr.clase_vehiculo
            HAVING total > 0
            ORDER BY total DESC
        """);

        return jdbc.queryForList(sql.toString(), params.toArray());
    }
}