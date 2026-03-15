package com.pasantia.proyecto.roadsafety.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import com.pasantia.proyecto.roadsafety.dto.MetaCellDto;
import com.pasantia.proyecto.roadsafety.dto.MetaUpdateRequest;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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

    public List<MetaCellDto> findMetasByMunicipio(int municipioId) {
        String sql = """
            SELECT id, municipio_id, behavior, clase_vehiculo, meta, activo
            FROM metas_conteo
            WHERE municipio_id = ?
            ORDER BY behavior, clase_vehiculo
        """;

        return localJdbc.query(sql, (rs, rowNum) -> new MetaCellDto(
                rs.getLong("id"),
                rs.getInt("municipio_id"),
                rs.getString("behavior"),
                rs.getString("clase_vehiculo"),
                rs.getLong("meta"),
                rs.getBoolean("activo")
        ), municipioId);
    }

    public int[] updateMetasBatch(List<MetaUpdateRequest> updates) {
        if (updates == null || updates.isEmpty()) return new int[0];

        String sql = "UPDATE metas_conteo SET meta = ?, activo = ? WHERE id = ?";

        return localJdbc.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                MetaUpdateRequest item = updates.get(i);
                ps.setLong(1, item.meta() == null ? 0L : item.meta());
                ps.setBoolean(2, item.activo() != null && item.activo());
                ps.setLong(3, item.id());
            }

            @Override
            public int getBatchSize() {
                return updates.size();
            }
        });
    }

    public int updateMetaById(long id, Long meta, Integer activo) {
        if (meta == null && activo == null) return 0;

        StringBuilder sql = new StringBuilder("UPDATE metas_conteo SET ");
        boolean first = true;

        if (meta != null) {
            sql.append("meta = ?");
            first = false;
        }
        if (activo != null) {
            if (!first) sql.append(", ");
            sql.append("activo = ?");
        }

        sql.append(" WHERE id = ?");

        if (meta != null && activo != null) {
            return localJdbc.update(sql.toString(), meta, activo, id);
        }
        if (meta != null) {
            return localJdbc.update(sql.toString(), meta, id);
        }
        return localJdbc.update(sql.toString(), activo, id);
    }
}
