package com.pasantia.proyecto.roadsafety.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Repository
public class MetasRepository {

    private final JdbcTemplate localJdbc;

    public MetasRepository(@Qualifier("localJdbcTemplate") JdbcTemplate localJdbc) {
        this.localJdbc = localJdbc;
    }

    public Map<String, Long> findMetasByMunicipioAndBehavior(Integer municipioId, String behavior) {
        if (municipioId == null) return Map.of();

        String sql = """
            SELECT clase_vehiculo, SUM(meta) AS meta
            FROM metas_conteo
            WHERE municipio_id = ?
              AND behavior = ?
              AND activo = 1
            GROUP BY clase_vehiculo
        """;

        return localJdbc.query(sql, rs -> {
            Map<String, Long> out = new HashMap<>();
            while (rs.next()) {
                out.put(rs.getString("clase_vehiculo"), rs.getLong("meta"));
            }
            return out;
        }, municipioId, behavior);
    }

    public Map<String, Long> findMetasByMunicipiosAndBehavior(List<Integer> municipioIds, String behavior) {
        if (municipioIds == null || municipioIds.isEmpty()) return Map.of();

        StringJoiner sj = new StringJoiner(",", "(", ")");
        for (int i = 0; i < municipioIds.size(); i++) {
            sj.add("?");
        }

        String sql = """
            SELECT clase_vehiculo, SUM(meta) AS meta
            FROM metas_conteo
            WHERE municipio_id IN %s
              AND behavior = ?
              AND activo = 1
            GROUP BY clase_vehiculo
        """.formatted(sj);

        Object[] params = new Object[municipioIds.size() + 1];
        for (int i = 0; i < municipioIds.size(); i++) {
            params[i] = municipioIds.get(i);
        }
        params[municipioIds.size()] = behavior;

        return localJdbc.query(sql, rs -> {
            Map<String, Long> out = new HashMap<>();
            while (rs.next()) {
                out.put(rs.getString("clase_vehiculo"), rs.getLong("meta"));
            }
            return out;
        }, params);
    }
}