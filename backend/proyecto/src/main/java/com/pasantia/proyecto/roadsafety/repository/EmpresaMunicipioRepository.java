package com.pasantia.proyecto.roadsafety.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EmpresaMunicipioRepository {

    private final JdbcTemplate empresaJdbc;

    public EmpresaMunicipioRepository(@Qualifier("empresaJdbcTemplate") JdbcTemplate empresaJdbc) {
        this.empresaJdbc = empresaJdbc;
    }

    public List<Integer> findMunicipioIdsByDepartamento(String departamento) {
        String sql = """
            SELECT DISTINCT id
            FROM municipio
            WHERE departamento = ?
            ORDER BY id
        """;

        return empresaJdbc.query(
                sql,
                (rs, rowNum) -> rs.getInt("id"),
                departamento
        );
    }
}