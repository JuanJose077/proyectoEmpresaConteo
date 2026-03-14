package com.pasantia.proyecto.roadsafety.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CatalogValidationService {

    private final JdbcTemplate jdbc;

    public CatalogValidationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean puntoPerteneceAMunicipio(int puntoId, int municipioId) {
        Integer exists = jdbc.queryForObject(
                """
                SELECT 1
                FROM v_formatos_general
                WHERE coordenada_id = ?
                  AND municipio_id = ?
                LIMIT 1
                """,
                Integer.class,
                puntoId, municipioId
        );
        return exists != null;
    }
}