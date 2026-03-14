package com.pasantia.proyecto.roadsafety.util;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

public final class SqlFilterUtil {

    private SqlFilterUtil() {}

    public static String picoExpr() {
        return """
          (
            TIME(fg.device_created_at) BETWEEN '05:30:00' AND '08:00:00'
            OR TIME(fg.device_created_at) BETWEEN '11:00:00' AND '13:00:00'
            OR TIME(fg.device_created_at) BETWEEN '16:30:00' AND '18:30:00'
          )
        """;
    }

    /**
     * Aplica filtros siguiendo prioridad:
     * 1) puntoId (coordenada_id)
     * 2) municipioId
     * 3) departamento (municipio.departamento)
     */
    public static void applyFilters(
            StringBuilder sql,
            List<Object> params,
            String departamento,
            Integer municipioId,
            Integer puntoId,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String jornada,
            String dia
    ) {

        // 1) Punto
        if (puntoId != null) {
            sql.append(" AND fg.coordenada_id = ? ");
            params.add(puntoId);
        }

        // 2) Municipio o 3) Departamento
        if (municipioId != null) {
            sql.append(" AND fg.municipio_id = ? ");
            params.add(municipioId);
        } else if (departamento != null && !departamento.isBlank()) {
            sql.append(" AND m.departamento = ? ");
            params.add(departamento.trim());
        }

        // Fechas (día completo)
        if (fechaInicio != null) {
            sql.append(" AND fg.device_created_at >= ? ");
            params.add(Date.valueOf(fechaInicio));
        }
        if (fechaFin != null) {
            sql.append(" AND fg.device_created_at < ? ");
            params.add(Date.valueOf(fechaFin.plusDays(1)));
        }

        // Día: TODOS | HABIL | NO_HABIL
        if ("HABIL".equalsIgnoreCase(dia)) {
            // MySQL: 1=Dom, 7=Sáb => 2..6 Lun..Vie
            sql.append(" AND DAYOFWEEK(fg.device_created_at) BETWEEN 2 AND 6 ");
        } else if ("NO_HABIL".equalsIgnoreCase(dia)) {
            sql.append(" AND DAYOFWEEK(fg.device_created_at) IN (1,7) ");
        }

        // Jornada: GENERAL | PICO | VALLE
        if ("PICO".equalsIgnoreCase(jornada)) {
            sql.append(" AND ").append(picoExpr());
        } else if ("VALLE".equalsIgnoreCase(jornada)) {
            sql.append(" AND NOT ").append(picoExpr());
        }
    }
}